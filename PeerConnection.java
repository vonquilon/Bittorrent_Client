import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * This class connects to a peer, communicates with the peer,
 * downloads pieces, and puts the pieces together.
 * 
 * @authors Von Kenneth Quilon & Alex Loh
 * @date 07/12/2013
 * @version 1.0
 */
public class PeerConnection{
    //Socket connectionSocket; made in PeerDownloadConnectin.java
    //PeerDownloadConnection fromPeer;
    PeerUploadConnection toPeer;
    static ConnectionState state;


    /**
     * Constructs a PeerConnection object connected to the machine with the specified IP address and port
     * @param ipAddress IP address of the peer, in the form "xxx.xxx.xxx.xxx" (ie. "255.255.255.255")
     * @param port port that we're connecting to
     * @throws IOException if unable to connect
     */
    public PeerConnection(/*String ipAddress, int port, byte[] file*/) throws IOException {
        //connectionSocket = new Socket(ipAddress, port); made in PeerDownloadConnectin.java
        //fromPeer = new PeerDownloadConnection(state, file);
        /*toPeer = new PeerUploadConnection(connectionSocket, state, file);
         *    should have a server socket, since it needs to listen
         */
    }
    
    public static byte[] getFileFromPeer(ArrayList<String> peers, TorrentFile torrentFile, byte[] peerID) throws IOException {

    	Byte[] file = new Byte[torrentFile.getFileSize()];
    	AtomicReferenceArray fileReference = new AtomicReferenceArray(file);
    	String[] peerIPAddresses = {"128.6.171.3", "128.6.171.4"};
    	int numberOfPeers = peerIPAddresses.length;
    	int numberOfPieces = torrentFile.getNumberOfPieces();
    	int piecesPerPeer = (int) Math.ceil(numberOfPieces/numberOfPeers);
    	boolean isLeftOver = false;
    	if(numberOfPieces % numberOfPeers > 0)
    		isLeftOver = true;
    	ArrayList<PeerDownloadConnection> downloads = new ArrayList<PeerDownloadConnection>(numberOfPeers);
    	for(int i = 0; i < numberOfPeers; i++) {
    		String[] selectedPeer = getAPeer(peers, peerIPAddresses[i]);
    		String IPaddress = selectedPeer[0];
            int port = Integer.parseInt(selectedPeer[1]);
            ArrayList<Integer> indexes;
            if(isLeftOver && i == numberOfPeers - 1)
            	indexes = getIndexes(piecesPerPeer - 1);
            else
            	indexes = getIndexes(piecesPerPeer);
            downloads.add(new PeerDownloadConnection(IPaddress, port, state, torrentFile, peerID, fileReference indexes));
    	}
    	
    }

    /**
     * Closes the connection to this peer
     */
    public void close() throws InterruptedException {
        //fromPeer.close();
        toPeer.close();
        //fromPeer.join();
        toPeer.join();
    }
    
    /**
     * Private helper method that gets a desired peer
     * from an ArrayList of peers.
     *
     * @param peers         ArrayList of available peers
     * @param peerIPAddress IP address of desired peer
     * @return String[] 	Contains the selected peer's
     * 						IP address and port number
     */
    private static String[] getAPeer(ArrayList<String> peers, String peerIPAddress) {

        for (String peer : peers) {
            String[] selectedPeer = peer.split(":");
            if (selectedPeer[0].equals(peerIPAddress))
                return selectedPeer;
        }

        return null;

    }
    
    /**
     * Private helper method that obtains possible indexes for the pieces.
     *
     * @param numberOfPieces
     * @return indexes
     */
    private static ArrayList<Integer> getIndexes(int numberOfPieces) {

        ArrayList<Integer> indexes = new ArrayList<Integer>(numberOfPieces);

        for (int i = 0; i < numberOfPieces; i++)
            indexes.add(i);

        return indexes;

    }
}