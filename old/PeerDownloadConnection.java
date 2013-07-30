package old;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: al
 * Date: 7/22/13
 * Time: 6:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class PeerDownloadConnection extends Thread{
	
    Socket connectionSocket;
    String peerIPAddress;
    int port;
    byte[] peerID;
    FileManager fileManager;

    public PeerDownloadConnection(String peerIPAddress, int port, byte[] peerID, FileManager fileManager) throws IOException {
        this.peerIPAddress = peerIPAddress;
        this.port = port;
        this.fileManager = fileManager;
        this.peerID = peerID;
    }

    
    public void run(){
    	System.out.println("Contacting peer at " + peerIPAddress +
                " on port " + Integer.toString(port));
    	try{
	        connectionSocket = new Socket(peerIPAddress, port);
	        OutputStream toPeer = connectionSocket.getOutputStream();
	        InputStream fromPeer = connectionSocket.getInputStream();
	        byte[] message = SharedFunctions.createHandshake(TorrentFile.getInfoHashBytes(), peerID);
	        toPeer.write(message);
	        byte[] messageFromPeer = SharedFunctions.responseFromPeer(fromPeer, message.length + 6, peerIPAddress);
	        ArrayList<byte[]> handshakeAndBitfield = detachMessage(messageFromPeer, 68);
	        ArrayList<Integer> indexes = getIndexes(handshakeAndBitfield.get(1), TorrentFile.getNumberOfPieces());
	        if (SharedFunctions.verifyInfoHash(message, messageFromPeer)) {
	        	System.out.println("Handshake verified from " + peerIPAddress);
	            //creates an "interested" message
	            message = SharedFunctions.createMessage(1, 2, -1, -1, -1, 5);
	            toPeer.write(message);
	            messageFromPeer = SharedFunctions.responseFromPeer(fromPeer, message.length, peerIPAddress);
	            if (!SharedFunctions.decodeMessage(messageFromPeer).equals("unchoke"))
	                throw new IOException("Peer denied interested message!");
	            System.out.println("Connection unchoked from " + peerIPAddress);
	            //Contacts tracker that downloading has started
	            URLConnection trackerCommunication = Functions.makeURL(TorrentFile.getAnnounce(), peerID, TorrentFile.getInfoHashBytes(), 0, 0, TorrentFile.getFileSize(), "started").openConnection();
	            trackerCommunication.connect();
	            int numberOfPieces = indexes.size();
	            for (int i = 0; i < numberOfPieces; i++) {
	                //gets random index number
	                int index = indexes.get(Functions.generateRandomInt(indexes.size() - 1));
	                indexes.remove((Integer) index);
	                if(fileManager.isDownloadable(index)) {
	                	int pieceLength;
		                //if the piece at the end of the file
		                if (index == TorrentFile.getNumberOfPieces() - 1)
		                    //Ex: 151709-32768*(5-1) = 20637 bytes = piece at end of file
		                    pieceLength = TorrentFile.getFileSize() - TorrentFile.getPieceSize() * (TorrentFile.getNumberOfPieces() - 1);
		                else
		                    pieceLength = TorrentFile.getPieceSize();
		                //creates a "request" message
		                message = SharedFunctions.createMessage(13, 6, index, 0, pieceLength, 17);
		                ArrayList<byte[]> pieceAndHeader = getPieceAndHeader(message, fromPeer, toPeer, pieceLength + 13, index, TorrentFile.getPieceHashes());
		                fileManager.putPieceInFile(index, pieceAndHeader.get(1), TorrentFile.getPieceSize());
		                //creates a "have" message
		                toPeer.write(SharedFunctions.createMessage(5, 4, index, -1, -1, 9));
		                fileManager.insertIntoBitfield(index);
		                System.out.println("Piece " + Integer.toString(index + 1) + ": " +
		                        Integer.toString(pieceLength) + " bytes downloaded from " + peerIPAddress);
	                }//end if
	            }//end for
	            //Contacts tracker that download has completed, Header length = 13 bytes
	            trackerCommunication = Functions.makeURL(TorrentFile.getAnnounce(), peerID, TorrentFile.getInfoHashBytes(), 0, TorrentFile.getFileSize() + TorrentFile.getNumberOfPieces() * 13, 0, "completed").openConnection();
	            trackerCommunication.connect();
	        }//end if
	        else
                throw new IOException("Unknown info hash from peer's handshake!");
    	}catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
    
    /**
     * Private helper method that obtains data from a peer. The data
     * contains the header information and the piece.
     *
     * @param message     "request" message
     * @param fromPeer    InputStream to get data from peer
     * @param toPeer      OutputSteam to write data to peer
     * @param size        Expected size of requested data
     * @param index       Used for assembling piece into file
     * @param pieceHashes
     * @return pieceAndHeader
     */
    private ArrayList<byte[]> getPieceAndHeader(byte[] message, InputStream fromPeer, 
    		OutputStream toPeer, int size, int index, ArrayList<byte[]> pieceHashes) throws IOException {

        ArrayList<byte[]> pieceAndHeader = new ArrayList<byte[]>(2);
        byte[] response;
        int counter = -1;

        do {
            toPeer.write(message);
            response = SharedFunctions.responseFromPeer(fromPeer, size, peerIPAddress);
            pieceAndHeader = detachMessage(response, 13);
            counter++;
            //stops when valid header and piece are obtained or had tried more than 10 times
        } while ((!verifyPieceHash(pieceAndHeader.get(1), index, pieceHashes) ||
                !( SharedFunctions.decodeMessage(pieceAndHeader.get(0)).equals("piece") )) && counter < 10);

        if (counter == 10)
            throw new IOException("\nFailed to obtain a valid piece or header.");

        return pieceAndHeader;

    }
    
    /**
     * Private helper method that separates a data that contains two different information.
     *
     * @param attachedMessage The data to be separated
     * @param message1Length  Used for obtaining message2Length:
     *                        message2Length=attachedMessage.length-message1Length
     */
    private static ArrayList<byte[]> detachMessage(byte[] attachedMessage, int message1Length) {

        byte[] message1 = new byte[message1Length];
        int message2Length = attachedMessage.length - message1Length;
        byte[] message2 = new byte[message2Length];
        ArrayList<byte[]> detachedData = new ArrayList<byte[]>(2);

        //copies header data into message byte[]
        System.arraycopy(attachedMessage, 0, message1, 0, message1Length);
        detachedData.add(message1);
        //copies piece data into piece byte[]
        System.arraycopy(attachedMessage, message1Length, message2, 0, message2Length);
        detachedData.add(message2);

        return detachedData;

    }
    
    /**
     * Private helper method that verifies the downloaded piece hash
     * with one of the hashes given in the torrent file.
     *
     * @param piece       
     * @param index       Used for locating the corresponding piece in the
     *                    ArrayList of pieceHashes
     * @param pieceHashes
     * @return boolean true if verified, false if not
     */
    private static boolean verifyPieceHash(byte[] piece, int index, ArrayList<byte[]> pieceHashes) {

        byte[] pieceHash = Functions.encodeToSHA1(piece);
        if (Arrays.equals(pieceHash, pieceHashes.get(index)))
            return true;
        else
            return false;

    }
    
    private static ArrayList<Integer> getIndexes(byte[] bitfieldMessage, int numberOfPieces) throws IOException {
    	
    	if(SharedFunctions.decodeMessage(bitfieldMessage).equals("bitfield")) {
    		ArrayList<Integer> indexes = new ArrayList<Integer>(numberOfPieces);
    		String bitfield = Integer.toBinaryString(bitfieldMessage[5] & 0xFF);
    		for(int i = 0; i < numberOfPieces; i++) {
    			if(bitfield.charAt(i) == '1')
    				indexes.add(i);
    		}
    		return indexes;
    	}
    	else
    		throw new IOException("Invalid bitfield message!");
    	
    }
}