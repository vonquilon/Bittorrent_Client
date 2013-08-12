import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

public class ConnectionManager implements Runnable{

	private volatile boolean stopped = false;
	private boolean trackerDone = false;
	private boolean bitfieldDone = false;
	private boolean sorted = false;
	private final Random RANDOM = new Random();
	private int activeDownloadConnections = 0;
	private int activeUploadConnections = 0;
	private FileManager fileManager;
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
	
	public ConnectionManager(TorrentInfo torrentInfo, FileManager fileManager) {
		ConnectionManager.torrentInfo = torrentInfo;
		this.fileManager = fileManager;
		pieces = new HashMap<Integer, Piece>(torrentInfo.piece_hashes.length*2);
		donePieces = new ArrayList<Integer>(torrentInfo.piece_hashes.length);
		for(int i = 0; i < torrentInfo.piece_hashes.length; i++)
			pieces.put(i, new Piece(i));
	}
	
	@Override
	public void run() {
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
							if(!isSorting)
								findPeer(sortedPieces.remove(0), false);
						}
						checkingSortedPieces = false;
					}//end if
					checkDownloadQueue();
					updateActiveConnections();
				}//end else
			}//end else
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				//do nothing
			}
		}//end while
	}
	
	private void startConnections() {
		try {
			TrackerConnection.makeURL(torrentInfo.announce_url.toExternalForm(), ClientInfo.PEER_ID, ClientInfo.port,
					torrentInfo.info_hash, ClientInfo.uploaded, ClientInfo.downloaded, ClientInfo.left, "started");
		} catch (MalformedURLException e) {
			System.out.println("Unknown URL protocol!");
		}
		System.out.println("\nDownload Process:");
		int size;
		if(TrackerResponse.peers.size() > 10)
			size = 10;
		else
			size = TrackerResponse.peers.size();
		for(int i = 0; i < size; i++) {
			String[] IPandPort = TrackerResponse.peers.get(i).split(":");
			if(!IPandPort[0].equals(ClientInfo.IPAddress)) {
				PeerConnection peer = new PeerConnection(IPandPort[0], IPandPort[1], false, fileManager);
				choked.put(IPandPort[0], peer);
				peer.outputQueue.add(Message.handshake);
				new Thread(peer).start();
			}
		}
		trackerDone = true;
	}
	
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
	
	private void findPeer(Piece piece, boolean restarted) {
			if(activeDownloadConnections < 4) {
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
			/*
			String peer = piece.peers.get(j);
			if(downloading.containsKey(peer) && j != piece.peers.size()-1)
				continue;
			else {
				if(choked.containsKey(peer)) {
					if(unchoked.size() < 4) {
						choked.get(peer).outputQueue.add(Message.createInterested());
						addToQueue(piece, peer, restarted);
						downloading.put(peer, piece);
					} else {
						addToQueue(piece, peer, restarted);
					}
					break;
				} else {
					addToQueue(piece, peer, restarted);
					break;
				}
			}
			*/
	}
	
	private void addToQueue(Piece piece, String peer, boolean restarted) {
		if(restarted) {
			downloadQueue.add(0, peer); downloadQueue.add(1, piece); 
		}
		else {
			downloadQueue.add(peer); downloadQueue.add(piece);
		}
	}
	
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
				//downloadQueue.remove(0);
				//findPeer((Piece) downloadQueue.remove(1), true);
			}
		}
	}
	
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
	
	public static synchronized void sort() {
		isSorting = true;
		while(checkingSortedPieces);
		sortedPieces = new ArrayList<Piece>(pieces.values());
		Collections.sort(sortedPieces, new PieceComparable());
		isSorting = false;
	}
	
	private static class PieceComparable implements Comparator<Piece>{

		@Override
		public int compare(Piece o1, Piece o2) {
			return (o1.occurrences>o2.occurrences ? -1 : (o1.occurrences==o2.occurrences ? 0 : 1));
		}
	} 
}