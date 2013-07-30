import java.io.IOException;
import java.util.ArrayList;

/**
 * This class connects to a peer, communicates with the peer,
 * downloads pieces, and puts the pieces together.
 * 
 * @authors Von Kenneth Quilon & Alex Loh
 * @date 07/12/2013
 * @version 1.0
 */
public class PeerConnection{
    
    PeerUploadConnection toPeer;
    


    /**
     * Constructs a PeerConnection object connected to the machine with the specified IP address and port
     * @param ipAddress IP address of the peer, in the form "xxx.xxx.xxx.xxx" (ie. "255.255.255.255")
     * @param port port that we're connecting to
     * @throws IOException if unable to connect
     */
    public PeerConnection() throws IOException {
        
    }
    
    public static void downloadFile(ArrayList<String> peers, byte[] peerID, String fileName) throws IOException {

    	FileManager fileManager = new FileManager(TorrentFile.getFileSize(), TorrentFile.getNumberOfPieces(), fileName);
    	String[] peerIPAddresses = {"128.6.171.3", "128.6.171.4"};
    	int numberOfPeers = peerIPAddresses.length;
    	ArrayList<PeerDownloadConnection> downloads = new ArrayList<PeerDownloadConnection>(numberOfPeers);
    	for(int i = 0; i < numberOfPeers; i++) {
    		String[] selectedPeer = getAPeer(peers, peerIPAddresses[i]);
    		String IPaddress = selectedPeer[0];
            int port = Integer.parseInt(selectedPeer[1]);
            downloads.add(new PeerDownloadConnection(IPaddress, port, peerID, fileManager));
    	}
    	
    	System.out.println("Download Process: ");
    	for(int i = 0; i < downloads.size(); i++)
    		downloads.get(i).start();
    	for(int i = 0; i < downloads.size(); i++) {
			try {
				downloads.get(i).join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	}
    	System.out.println("\nSaved file as " + fileName);
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
    private static ArrayList<Integer> getIndexes(int numberOfPieces, int offset) {

        ArrayList<Integer> indexes = new ArrayList<Integer>(numberOfPieces);
        int maxIndex = numberOfPieces + offset;

        for (int i = offset; i < maxIndex; i++)
            indexes.add(i);

        return indexes;

    }
}