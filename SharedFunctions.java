import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;


public class SharedFunctions {
	
	 /**
     * Method that creates a handshake message for a peer.
     *
     * @param infoHashBytes
     * @param peerID
     * @return handshakeMessage
     */
    public static byte[] createHandshake(byte[] infoHashBytes, byte[] peerID) {

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
     * Method that obtains the response data from a peer.
     *
     * @param fromPeer InputStream to get data from peer
     * @param size     Length of expected data from peer
     * @return messageFromPeer
     * @throws IOException Failed to get message from peer
     */
    public static byte[] responseFromPeer(InputStream fromPeer, int size, String peerIPAddress) throws IOException {

        int messageLength;
        int offset = 0;
        int counter = 0;

        //stops when messageLength > size or messageLength == size or had tried 10 million times
        while ((messageLength = fromPeer.available()) < size && counter < 10000000) {
            offset = messageLength;
            counter++;
        }

        if (counter == 10000000)
            throw new IOException("\nCould not get any messages from peer: " + peerIPAddress);

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
     * Method that verifies if the info hash from the
     * peer's handshake matches that of the created handshake.
     *
     * @param toPeer   Handshake given to peer
     * @param fromPeer Handshake received from peer
     * @return boolean true if verified, false if not
     */
    public static boolean verifyInfoHash(byte[] toPeer, byte[] fromPeer) {

        //info hash is 20 bytes long
        for (int i = 0; i < 20; i++) {
            //info hash is offset by 28 bytes
            if (toPeer[i + 28] != fromPeer[i + 28])
                return false;
        }

        return true;

    }
    
    /**
     * Method that creates a message for a peer.
     *
     * @param lengthPrefix Length of message in bytes excluding lengthPrefix
     * @param id           Message identifier
     * @param index        Piece index
     * @param begin        Byte offset in piece
     * @param length       Size of requested block
     * @param byteSize     Size of expected message
     * @return message 	   The created message in byte[] form
     */
    public static byte[] createMessage(int lengthPrefix, int id, int index, int begin, int length, int byteSize) {

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
     * Method that decodes a byte[] message into
     * a readable string.
     *
     * @param message The byte[] message to be decoded
     * @return String The readable message
     */
    public static String decodeMessage(byte[] message) {

        String[] messages = {"choke", "unchoke", "interested", "not interested", "have",
                "bitfield", "request", "piece", "cancel"};

        if (message.length < 4)
            return null;
        else if (message.length == 4)
            return "keep-alive";
        else
            return messages[message[4]];

    }

}