import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Created with IntelliJ IDEA.
 * User: al
 * Date: 7/22/13
 * Time: 6:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class PeerDownloadConnection extends Thread{
	
    Socket connectionSocket;
    //ConnectionState connectionState;
    boolean active;
    String peerIPAddress;
    int port;
    byte[] peerID;
    FileManager file;
    TorrentFile torrentInfo;
    ArrayList<Integer> indexes;

    public PeerDownloadConnection(String peerIPAddress, int port, TorrentFile torrentInfo, byte[] peerID, FileManager file, ArrayList<Integer> indexes) throws IOException {
        //this.connectionSocket = connectionSocket;
        //connectionState = state;
        this.peerIPAddress = peerIPAddress;
        this.port = port;
        this.file = file;
        this.torrentInfo = torrentInfo;
        this.peerID = peerID;
        this.indexes = indexes;
        active = true;
    }

    
    public void run(){
    	System.out.println("Contacting peer at " + peerIPAddress +
                " on port " + Integer.toString(port));
    	try{
	        connectionSocket = new Socket(peerIPAddress, port);
	        OutputStream toPeer = connectionSocket.getOutputStream();
	        InputStream fromPeer = connectionSocket.getInputStream();
	        byte[] message = createHandshake(torrentInfo.getInfoHashBytes(), peerID);
	        toPeer.write(message);
	        byte[] messageFromPeer = responseFromPeer(fromPeer, message.length);
	
	        if (verifyInfoHash(message, messageFromPeer)) {
	        	
	        	System.out.println("Handshake verified from " + peerIPAddress);
	            //creates an "interested" message
	            message = createMessage(1, 2, -1, -1, -1, 5);
	            int counter = -1;
	
	            do {
	                toPeer.write(message);
	                messageFromPeer = responseFromPeer(fromPeer, message.length);
	                counter++;
	                //stops when received message == "unchoke" or had tried more than 10 times
	            } while (!decodeMessage(messageFromPeer).equals("unchoke") && counter < 10);
	
	            System.out.println("Connection unchoked from " + peerIPAddress);
	
	            if (counter == 10)
	                throw new IOException("Peer denied interested message!");
	
	            //Contacts tracker that downloading has started
	            URLConnection trackerCommunication = Functions.makeURL(torrentInfo.getAnnounce(), peerID, torrentInfo.getInfoHashBytes(), 0, 0, torrentInfo.getFileSize(), "started").openConnection();
	            trackerCommunication.connect();
	
	            System.out.println("Download Started from " + peerIPAddress);
	            
	            int numberOfPieces = indexes.size();
	            for (int i = 0; i < numberOfPieces; i++) {
	
	                int pieceLength;
	                //gets random index number
	                int index = indexes.get(Functions.generateRandomInt(indexes.size() - 1));
	                indexes.remove((Integer) index);
	                //if the piece at the end of the file
	                if (index == torrentInfo.getNumberOfPieces() - 1)
	                    //Ex: 151709-32768*(5-1) = 20637 bytes = piece at end of file
	                    pieceLength = torrentInfo.getFileSize() - torrentInfo.getPieceSize() * (torrentInfo.getNumberOfPieces() - 1);
	                else
	                    pieceLength = torrentInfo.getPieceSize();
	
	                //creates a "request" message
	                message = createMessage(13, 6, index, 0, pieceLength, 17);
	                ArrayList<byte[]> pieceAndHeader = getPieceAndHeader(message, fromPeer, toPeer, pieceLength + 13, index, torrentInfo.getPieceHashes());
	                file.putPieceInFile(index, pieceAndHeader.get(1), torrentInfo.getPieceSize());
	                //creates a "have" message
	                toPeer.write(createMessage(5, 4, index, -1, -1, 9));
	
	                System.out.println("Piece " + Integer.toString(index + 1) + ": " +
	                        Integer.toString(pieceLength) + " bytes downloaded from " + peerIPAddress);
	
	            }//end for
	
	            //Contacts tracker that download has completed, Header length = 13 bytes
	            trackerCommunication = Functions.makeURL(torrentInfo.getAnnounce(), peerID, torrentInfo.getInfoHashBytes(), 0, torrentInfo.getFileSize() + torrentInfo.getNumberOfPieces() * 13, 0, "completed").openConnection();
	            trackerCommunication.connect();
	
	            System.out.println("Download Complete from " + peerIPAddress);
	
	        }//end if
	        else
                throw new IOException("Unknown info hash from peer's handshake!");
    	}catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }


    /**
     * Stops the thread of execution, although perhaps not immediately, and frees all resources
     */
    public void close() {
        active = false;
    }
    
    /**
     * Private helper method that creates a handshake message for a peer.
     *
     * @param infoHashBytes
     * @param peerID
     * @return handshakeMessage
     */
    private static byte[] createHandshake(byte[] infoHashBytes, byte[] peerID) {

        byte[] handshakeMessage = new byte[68];
        handshakeMessage[0] = 19;

        int offset = 1;

        byte[] protocol = "BitTorrent protocol".getBytes();
        System.arraycopy(protocol, 0, handshakeMessage, offset, protocol.length);
        offset += protocol.length + 8;

        System.arraycopy(infoHashBytes, 0, handshakeMessage, offset, infoHashBytes.length);
        offset += infoHashBytes.length;

        System.arraycopy(peerID, 0, handshakeMessage, offset, peerID.length);

        return handshakeMessage;

    }
    
    /**
     * Private helper method that obtains the response data from a peer.
     *
     * @param fromPeer InputStream to get data from peer
     * @param size     Length of expected data from peer
     * @return messageFromPeer
     * @throws IOException Failed to get message from peer
     */
    private static byte[] responseFromPeer(InputStream fromPeer, int size) throws IOException {

        int messageLength;
        int offset = 0;
        int counter = 0;

        //stops when messageLength > size or messageLength == size or had tried 10 million times
        while ((messageLength = fromPeer.available()) < size && counter < 10000000) {
            offset = messageLength;
            counter++;
        }

        if (counter == 10000000)
            throw new IOException("\nCould not get any messages from peer. Check internet connection!");

        byte[] messageFromPeer = new byte[messageLength];
        fromPeer.read(messageFromPeer);

        //deletes any unexpected bytes at the beginning of the data
        if (messageLength > size) {
            byte[] result = new byte[size];
            System.arraycopy(messageFromPeer, offset, result, 0, size);
            return result;
        } else
            return messageFromPeer;

    }
    
    /**
     * Private helper method that verifies if the info hash from the
     * peer's handshake matches that of the created handshake.
     *
     * @param toPeer   Handshake given to peer
     * @param fromPeer Handshake received from peer
     * @return boolean true if verified, false if not
     */
    private static boolean verifyInfoHash(byte[] toPeer, byte[] fromPeer) {

        //info hash is 20 bytes long
        for (int i = 0; i < 20; i++) {
            //info hash is offset by 28 bytes
            if (toPeer[i + 28] != fromPeer[i + 28])
                return false;
        }

        return true;

    }
    
    /**
     * Private helper method that creates a message for a peer.
     *
     * @param lengthPrefix Length of message in bytes excluding lengthPrefix
     * @param id           Message identifier
     * @param index        Piece index
     * @param begin        Byte offset in piece
     * @param length       Size of requested block
     * @param byteSize     Size of expected message
     * @return message 	   The created message in byte[] form
     */
    private static byte[] createMessage(int lengthPrefix, int id, int index, int begin, int length, int byteSize) {

        byte[] message = new byte[byteSize];
        int offset = 0;

        if (lengthPrefix >= 0) {
            byte[] temp = new byte[]{0, 0, 0, (byte) lengthPrefix};
            System.arraycopy(temp, 0, message, offset, temp.length);
            offset += 4;
        }

        if (id >= 0) {
            message[offset] = (byte) id;
            offset++;
        }

        if (index >= 0) {
            byte[] temp = ByteBuffer.allocate(4).putInt(index).array();
            System.arraycopy(temp, 0, message, offset, temp.length);
            offset += 4;
        }

        if (begin >= 0) {
            byte[] temp = ByteBuffer.allocate(4).putInt(begin).array();
            System.arraycopy(temp, 0, message, offset, temp.length);
            offset += 4;
        }

        if (length >= 0) {
            byte[] temp = ByteBuffer.allocate(4).putInt(length).array();
            System.arraycopy(temp, 0, message, offset, temp.length);
        }

        return message;

    }
    
    /**
     * Private helper method that decodes a byte[] message into
     * a readable string.
     *
     * @param message The byte[] message to be decoded
     * @return String The readable message
     */
    private static String decodeMessage(byte[] message) {

        String[] messages = {"choke", "unchoke", "interested", "not interested", "have",
                "bitfield", "request", "piece", "cancel"};

        if (message.length < 4)
            return null;
        else if (message.length == 4)
            return "keep-alive";
        else
            return messages[message[4]];

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
    private static ArrayList<byte[]> getPieceAndHeader(byte[] message, InputStream fromPeer,
                                                       OutputStream toPeer, int size, int index, ArrayList<byte[]> pieceHashes) throws IOException {

        ArrayList<byte[]> pieceAndHeader = new ArrayList<byte[]>(2);
        byte[] response;
        int counter = -1;

        do {
            toPeer.write(message);
            response = responseFromPeer(fromPeer, size);
            pieceAndHeader = detachMessageAndPiece(response, 13);
            counter++;
            //stops when valid header and piece are obtained or had tried more than 10 times
        } while ((!verifyPieceHash(pieceAndHeader.get(1), index, pieceHashes) ||
                !decodeMessage(pieceAndHeader.get(0)).equals("piece")) && counter < 10);

        if (counter == 10)
            throw new IOException("\nFailed to obtain a valid piece or header. Connection closed!");

        return pieceAndHeader;

    }
    
    /**
     * Private helper method that separates the header data and the piece data.
     *
     * @param pieceAndHeader The data to be separated
     * @param headerLength    Used for obtaining the pieceLength:
     *                        pieceLength=pieceAndMessage.length-headerLength
     */
    private static ArrayList<byte[]> detachMessageAndPiece(byte[] pieceAndHeader, int headerLength) {

        byte[] message = new byte[headerLength];
        int pieceLength = pieceAndHeader.length - headerLength;
        byte[] piece = new byte[pieceLength];
        ArrayList<byte[]> detachedData = new ArrayList<byte[]>(2);

        //copies header data into message byte[]
        System.arraycopy(pieceAndHeader, 0, message, 0, headerLength);
        detachedData.add(message);
        //copies piece data into piece byte[]
        System.arraycopy(pieceAndHeader, headerLength, piece, 0, pieceLength);
        detachedData.add(piece);

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
}