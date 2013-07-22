import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class connects to a peer, communicates with the peer,
 * downloads pieces, and puts the pieces together.
 * 
 * @authors Von Kenneth Quilon & Alex Loh
 * @date 07/12/2013
 * @version 1.0
 */
public class PeerConnection {
    
	/**
     * Empty constructor for the peer connection.
     */
    public PeerConnection() {
    	
    }

    /**
     * This method contacts a single peer, communicates with the peer,
     * obtain pieces of the file, and puts the pieces together
     * to form the file.
     * 
     * Pre-conditions:  128.6.171.3 must be in the peers list.
     * Post-conditions: The peer will be contacted for its pieces of
     * 					the file, and the pieces will be assembled.
     *
     * @param peers       The list of available peers
     * @param torrentFile The loaded and parsed torrent file
     * @param peerID
     * @return file 	  The image file as a byte[]
     * @throws IOException Communication errors
     */
    @SuppressWarnings("resource")
    public static byte[] getFileFromPeer(ArrayList<String> peers, TorrentFile torrentFile, byte[] peerID) {

        byte[] file = new byte[torrentFile.getFileSize()];

        try {

            String peerIPAddress = "128.6.171.3";
            String[] selectedPeer = getAPeer(peers, peerIPAddress);
            if (selectedPeer == null)
                throw new IOException("Peer with IP Address " + peerIPAddress + " not found");

            System.out.println("Contacting peer at " + selectedPeer[0] +
                    " on port " + selectedPeer[1]);

            Socket clientSocket = new Socket(selectedPeer[0], Integer.parseInt(selectedPeer[1]));
            OutputStream toPeer = clientSocket.getOutputStream();
            InputStream fromPeer = clientSocket.getInputStream();
            System.out.print("\nHandshake verified");
            byte[] message = createHandshake(torrentFile.getInfoHashBytes(), peerID);
            toPeer.write(message);
            byte[] messageFromPeer = responseFromPeer(fromPeer, message.length);

            if (verifyInfoHash(message, messageFromPeer)) {

                System.out.print(".......................DONE\n");
                //creates an "interested" message
                message = createMessage(1, 2, -1, -1, -1, 5);
                int counter = -1;
                System.out.print("Connection unchoked");

                do {
                    toPeer.write(message);
                    messageFromPeer = responseFromPeer(fromPeer, message.length);
                    counter++;
                    //stops when received message == "unchoke" or had tried more than 10 times
                } while (!decodeMessage(messageFromPeer).equals("unchoke") && counter < 10);

                System.out.print("......................DONE\n");

                if (counter == 10)
                    throw new IOException("Peer denied interested message!");

                ArrayList<Integer> indexes = getIndexes(torrentFile.getNumberOfPieces());

                //Contacts tracker that downloading has started
                URLConnection trackerCommunication = Functions.makeURL(torrentFile.getAnnounce(), peerID, torrentFile.getInfoHashBytes(), 0, 0, torrentFile.getFileSize(), "started").openConnection();
                trackerCommunication.connect();

                System.out.println("\nDownload Started");

                for (int i = 0; i < torrentFile.getNumberOfPieces(); i++) {

                    int pieceLength;
                    //gets random index number
                    int index = indexes.get(Functions.generateRandomInt(indexes.size() - 1));
                    indexes.remove((Integer) index);
                    //if the piece at the end of the file
                    if (index == torrentFile.getNumberOfPieces() - 1)
                        //Ex: 151709-32768*(5-1) = 20637 bytes = piece at end of file
                        pieceLength = torrentFile.getFileSize() - torrentFile.getPieceSize() * (torrentFile.getNumberOfPieces() - 1);
                    else
                        pieceLength = torrentFile.getPieceSize();

                    System.out.print("Piece " + Integer.toString(index + 1) + ": " +
                            Integer.toString(pieceLength) + " bytes");

                    //creates a "request" message
                    message = createMessage(13, 6, index, 0, pieceLength, 17);
                    ArrayList<byte[]> pieceAndHeader = getPieceAndHeader(message, fromPeer, toPeer, pieceLength + 13, index, torrentFile.getPieceHashes());
                    putPieceInFile(index, pieceAndHeader.get(1), file, torrentFile.getPieceSize());
                    //creates a "have" message
                    toPeer.write(createMessage(5, 4, index, -1, -1, 9));

                    System.out.print(".....................DONE\n");

                }//end for

                //Contacts tracker that download has completed, Header length = 13 bytes
                trackerCommunication = Functions.makeURL(torrentFile.getAnnounce(), peerID, torrentFile.getInfoHashBytes(), 0, torrentFile.getFileSize() + torrentFile.getNumberOfPieces() * 13, 0, "completed").openConnection();
                trackerCommunication.connect();

                System.out.println("Download Complete\n");

            }//end if
            else
                throw new IOException("Unknown info hash from peer's handshake!");

            toPeer.close();
            fromPeer.close();
            clientSocket.close();

        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        return file;

    }

    /**
     * Private helper method that assembles the piece into the file.
     *
     * @param index     When multiplied by pieceSize, indicates the location
     *                  of the piece in the file
     * @param piece     Piece to be assembled into file
     * @param file      Where piece is assembled into
     * @param pieceSize
     */
    private static void putPieceInFile(int index, byte[] piece, byte[] file, int pieceSize) {

        								//pieceSize*index = byte offset in file
        System.arraycopy(piece, 0, file, pieceSize * index, piece.length);

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
}