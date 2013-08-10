import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class ConnectionManager implements Runnable{

	private volatile boolean stopped = false;
	private boolean trackerDone = false;
	private boolean bitfieldDone = false;
	public static volatile HashMap<String,PeerConnection> choked = new HashMap<String,PeerConnection>(10);
	public static volatile HashMap<String,PeerConnection> unchoked = new HashMap<String,PeerConnection>(10);
	public static volatile HashMap<Integer, Piece> pieces;
	
	public ConnectionManager(TorrentInfo torrentInfo) {
		pieces = new HashMap<Integer, Piece>(torrentInfo.piece_hashes.length*2);
		for(int i = 0; i < torrentInfo.piece_hashes.length; i++)
			pieces.put(i, new Piece());
	}
	
	@Override
	public void run() {
		while(!stopped) {
			if(TrackerResponse.peers != null && !trackerDone) {
				startConnections();
			}
			if(!bitfieldDone) {
				checkBitfieldProcess();
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				//do nothing
			}
		}//end while
	}
	
	private void startConnections() {
		int size;
		if(TrackerResponse.peers.size() > 10)
			size = 10;
		else
			size = TrackerResponse.peers.size();
		for(int i = 0; i < size; i++) {
			String[] IPandPort = TrackerResponse.peers.get(i).split(":");
			if(!IPandPort[0].equals(ClientInfo.IPAddress)) {
				PeerConnection peer = new PeerConnection(IPandPort[0], IPandPort[1]);
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
}