import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.util.*;

/**
 * ConnectionManager manages the download and upload connections. It also manages
 * the rarest first algorithm.
 * 
 * @author Von Kenneth Quilon & Alex Loh
 * @date 08/11/2013
 * @version 1.0
 */
public class ConnectionManager implements Runnable{

	private volatile boolean stopped = false;
	private boolean trackerDone = false;
	private boolean bitfieldDone = false;
	private boolean sorted = false;
	private final Random RANDOM = new Random();
	private int peersOffset = 0;
	private int activeDownloadConnections = 0;
	private int activeUploadConnections = 0;
	private FileManager fileManager;
	private Upload upload = null;
	private ArrayList<Object> downloadQueue = new ArrayList<Object>();
	private static ArrayList<Piece> sortedPieces;
	private static volatile boolean isSorting = false;
	private static volatile boolean checkingSortedPieces = false;
	public static TorrentInfo torrentInfo;
	public static volatile HashMap<String, Piece> downloading = new HashMap<String, Piece>(10);
	public static volatile HashMap<String,PeerConnection> choked = new HashMap<String,PeerConnection>(10);
	public static volatile HashMap<String,PeerConnection> unchoked = new HashMap<String,PeerConnection>(10);
	public static volatile HashMap<Integer, Piece> pieces;
	public static volatile ArrayList<Integer> donePieces;
	
	/**
     * Constructs this connection manager.
     * 
     * @param torrentInfo - the decoded info from the torrent file
     * @param fileManager - the file manager that handles all of the writing/reading to the file
     */
	public ConnectionManager(TorrentInfo torrentInfo, FileManager fileManager) {
		ConnectionManager.torrentInfo = torrentInfo;
		this.fileManager = fileManager;
		pieces = new HashMap<Integer, Piece>(torrentInfo.piece_hashes.length*2);
		donePieces = new ArrayList<Integer>(torrentInfo.piece_hashes.length);
		for(int i = 0; i < torrentInfo.piece_hashes.length; i++)
			pieces.put(i, new Piece(i));
	}
	
	/**
     * Runs the connection manager, which then downloads then seeds the file.
     */
	@Override
	public void run() {
        int throttleCounter = 0;
		while(!stopped) {
			if(TrackerResponse.peers != null && !trackerDone)
				startConnections();
			if(!bitfieldDone)
				checkBitfieldProcess();
			else {
				if(!sorted) {
					sort(); sorted = true;
				} else {
					if(!isSorting) {
						checkingSortedPieces = true;
						for(int i = 0; i < 4; i++) {
							if(sortedPieces.size() == 0 || isSorting)
								break;
							if(!isSorting) {
								int offset = 0;
								while(offset < sortedPieces.size()-1) {
									if(sortedPieces.get(offset).occurrences != sortedPieces.get(offset+1).occurrences)
										break;
									offset++;
								}
								int index;
								int random = RANDOM.nextInt(offset+1);
								if(random == 0)
									index = random;
								else
									index = random-1;
								findPeer(sortedPieces.remove(index), false);
							}
						}//end for
						checkingSortedPieces = false;
					}//end if
					checkDownloadQueue();
					updateActiveConnections();
					if( ((choked.size()+unchoked.size()) < 10) && (peersOffset < TrackerResponse.peers.size()))
						getConnections(10 - (choked.size()+unchoked.size()) );
				}//end else
                if(throttleCounter == 15) {
                    throttlePeers();
                    throttleCounter = 0;
                }
				if(ClientInfo.left == 0) {
					stopped = true;
					try {
						URLConnection trackerCommunication = TrackerConnection.makeURL(torrentInfo.announce_url.toExternalForm(), ClientInfo.PEER_ID, ClientInfo.port,
								torrentInfo.info_hash, ClientInfo.uploaded, ClientInfo.downloaded, ClientInfo.left, "completed").openConnection();
						TrackerConnection.resetTimer();
						trackerCommunication.connect();
						System.out.println("Contacted tracker with event completed.");
					} catch (MalformedURLException e) {
						System.out.println("Unknown URL protocol!");
					} catch (IOException e) {
						System.out.println("Could not contact tracker.");
					}
					System.out.println("Download complete.");
				}
			}//end else
			try {
				Thread.sleep(200);
                throttleCounter++;
			} catch (InterruptedException e) {
				//do nothing
			}
		}//end while
		System.out.println("\nSeeding....");
		upload = new Upload(fileManager);
		new Thread(upload).start();
	}

    private void throttlePeers() {
        long lowestRate = Long.MAX_VALUE;
        String key = null;
        for(Map.Entry<String, PeerConnection> entry : unchoked.entrySet()){
            PeerConnection peerConnection = entry.getValue();
            long rate = peerConnection.getDataRate();
            if(rate < lowestRate) {
                lowestRate = rate;
                key = entry.getKey();
            }
        }
        if(key == null) {
            return;
        }
        PeerConnection slowestPeer = unchoked.remove(key);
        slowestPeer.outputQueue.add(Message.createChoke());
        choked.put(key,slowestPeer);

        System.out.println("Choked " + slowestPeer.IPAddress + " based on poor speed.");

        for(Map.Entry<String, PeerConnection> entry : choked.entrySet()){
            key = entry.getKey();
            break;
        }
        PeerConnection peerConnection = choked.remove(key);

        peerConnection.outputQueue.add(Message.createUnchoke());
        unchoked.put(key,peerConnection);

        System.out.println("Unchoked " + slowestPeer.IPAddress + ".");
    }

    /**
	 * Opens the connections to 10 peers in the peers list.
	 */
	private void startConnections() {
		try {
			URLConnection trackerCommunication = TrackerConnection.makeURL(torrentInfo.announce_url.toExternalForm(), ClientInfo.PEER_ID, ClientInfo.port,
					torrentInfo.info_hash, ClientInfo.uploaded, ClientInfo.downloaded, ClientInfo.left, "started").openConnection();
			TrackerConnection.resetTimer();
			trackerCommunication.connect();
			System.out.println("Contacted tracker with event started.");
		} catch (MalformedURLException e) {
			System.out.println("Unknown URL protocol!");
		} catch (IOException e) {
			System.out.println("Could not contact tracker.");
		}
		System.out.println("\nDownload Process:");
		System.out.println("Initiating... May take up to a minute.");
		getConnections(10);
		trackerDone = true;
	}
	
	/**
	 * Starts connections to a specified number of peers.
	 * 
	 * @param peers - number of peers to connect to
	 */
	private void getConnections(int peers) {
		int size;
		if((TrackerResponse.peers.size()-peersOffset) > peers)
			size = peers;
		else
			size = TrackerResponse.peers.size()-peersOffset;
		for(int i = 0; i < size; i++,peersOffset++) {
			String[] IPandPort = TrackerResponse.peers.get(peersOffset).split(":");
			if(!IPandPort[0].equals(ClientInfo.IPAddress)) {
				PeerConnection peer = new PeerConnection(IPandPort[0], IPandPort[1], false, fileManager);
				choked.put(IPandPort[0], peer);
				peer.outputQueue.add(Message.handshake);
				new Thread(peer).start();
			}
		}
	}
	
	/**
	 * Checks if the connections are done processing the peers' bitfields.
	 */
	private void checkBitfieldProcess() {
		Set<String> peers = choked.keySet();
		Iterator<String> iterator = peers.iterator();
		while(iterator.hasNext()) {
			if(choked.get(iterator.next()).done)
				bitfieldDone = true;
			else {
				bitfieldDone = false; break;
			}
		}
	}
	
	/**
	 * Finds a peer to download a piece from.
	 * 
	 * @param piece - the piece to download
	 * @param restarted - if it is looking for another peer
	 */
	private void findPeer(Piece piece, boolean restarted) {
			if(activeDownloadConnections < 4 && activeUploadConnections <= 2 && choked.size() != 0) {
				for(int j = 0; j < piece.peers.size(); j++) {
					String peer = piece.peers.get(j);
					if(choked.containsKey(peer)) {
						choked.get(peer).outputQueue.add(Message.createInterested());
						addToQueue(piece, peer, restarted);
						downloading.put(peer, piece);
						break;
					}
				}
			} else {
				ArrayList<String> activePeers = new ArrayList<String>(activeDownloadConnections);
				for(int j = 0; j < piece.peers.size(); j++) {
					String peer = piece.peers.get(j);
					if(unchoked.containsKey(peer))
						activePeers.add(peer);
				}
				if(activePeers.size() == 0)
					addToQueue(piece, piece.peers.get(RANDOM.nextInt(piece.peers.size()-1)), restarted);
				else
					addToQueue(piece, activePeers.get(RANDOM.nextInt(activePeers.size()-1)), restarted);
			}
	}
	
	/**
	 * Adds a piece and peer to the download queue.
	 * 
	 * @param piece - the piece to download
	 * @param peer - the peer to download from
	 * @param restarted - if it looked for another peer
	 */
	private void addToQueue(Piece piece, String peer, boolean restarted) {
		if(restarted) {
			downloadQueue.add(0, peer); downloadQueue.add(1, piece); 
		}
		else {
			downloadQueue.add(peer); downloadQueue.add(piece);
		}
	}
	
	/**
	 * Initiates the download of a piece in front of a queue.
	 */
	private void checkDownloadQueue() {
		if(downloadQueue.size() != 0) {
			String peer = (String) downloadQueue.get(0);
			if(choked.containsKey(peer)) {
				Set<String> peers = unchoked.keySet();
				Iterator<String> iterator = peers.iterator();
				while(iterator.hasNext()) {
					String unchokedPeer = iterator.next();
					if(downloading.containsKey(unchokedPeer)) {
						//do nothing
					}
					else {
						unchoked.get(unchokedPeer).outputQueue.add(Message.createNotInterested());
						choked.get(peer).outputQueue.add(Message.createInterested());
						downloading.put(peer, (Piece) downloadQueue.get(1));
						break;
					}
				}//end while
			} else if(unchoked.containsKey(peer)) {
				downloadQueue.remove(0);
				Piece piece = (Piece) downloadQueue.remove(0);
				int size;
				if(piece.index == torrentInfo.piece_hashes.length-1)
					size = torrentInfo.file_length - (torrentInfo.piece_hashes.length-1)
						* torrentInfo.piece_length;
				else
					size = torrentInfo.piece_length;
				unchoked.get(peer).outputQueue.add(Message.createRequest(piece.index, 0, size));
			} else {
				//do nothing
			}
		}
	}
	
	/**
	 * Updates the number of active connections.
	 */
	private void updateActiveConnections() {
		activeDownloadConnections = 0;
		activeUploadConnections = 0;
		Set<String> peers = unchoked.keySet();
		Iterator<String> iterator = peers.iterator();
		while(iterator.hasNext()) {
			String unchokedPeer = iterator.next();
			if(unchoked.get(unchokedPeer).isUpload)
				activeUploadConnections++;
			else
				activeDownloadConnections++;
		}
	}
	
	/**
     * Sorts the pieces according to number of occurrences for use in determining
     * the least common piece, which is the one we should request.
     */
	public static synchronized void sort() {
		isSorting = true;
		while(checkingSortedPieces);
		sortedPieces = new ArrayList<Piece>(pieces.values());
		Collections.sort(sortedPieces, new PieceComparable());
		isSorting = false;
	}
	
	/**
	 * Closes all connections.
	 */
	public void close() {
		stopped = true;
		if(choked.size() != 0) {
			Set<String> peers = choked.keySet();
			Iterator<String> iterator = peers.iterator();
			while(iterator.hasNext()) {
				choked.get(iterator.next()).close();
			}
		}
		if(unchoked.size() != 0) {
			Set<String> peers = unchoked.keySet();
			Iterator<String> iterator = peers.iterator();
			while(iterator.hasNext()) {
				unchoked.get(iterator.next()).close();
			}
		}
		if(upload != null)
			upload.close();
	}
	
	/**
	 * A custom Comparator class used for sorting the pieces.
	 * 
	 * @author Von Kenneth Quilon
	 */
	private static class PieceComparable implements Comparator<Piece>{

		@Override
		public int compare(Piece o1, Piece o2) {
			return (o1.occurrences<o2.occurrences ? -1 : (o1.occurrences==o2.occurrences ? 0 : 1));
		}
	} 
}