import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;


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
     * @param messageID Message identifier
     *
     */
    public static byte[] createMessage(int lengthPrefix, byte messageID) {
        ByteBuffer buffer = ByteBuffer.allocate(5);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(lengthPrefix);
        buffer.put(messageID);
        return buffer.array();
    }

    /**
     * Method that creates a message for a peer.
     *
     * @param lengthPrefix Length of message in bytes excluding lengthPrefix
     * @param messageID    Message identifier
     * @param payload      The payload of this message
     * @return message       The created message in byte[] form
     */
    public static byte[] createMessage(int lengthPrefix, byte messageID, byte[] payload) {
        byte[] message = createMessage(lengthPrefix, messageID);
        if (payload == null || payload.length == 0) {
            return message;
        }
        return concat(payload, message);
    }

    public static byte[] concat(byte[] b1, byte[] b2) {
        byte[] totalMessage = Arrays.copyOf(b2, b2.length + b1.length);
        System.arraycopy(b1, 0, totalMessage, b2.length, b1.length);
        return totalMessage;
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

    /**
     * Gets the length of a message as specified in its format
     *
     * @param message the message to parse
     * @return the length as specified by the message
     */
    public static int lengthOfMessage(byte[] message) {
        message = Arrays.copyOfRange(message, 0, 4);
        return ByteBuffer.wrap(message).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    /**
     * Method that takes an entire peer message and returns its payload
     *
     * @param message message from a peer, including the length and id fields
     * @param length  length of the message
     * @return payload of message
     */
    public static byte[] payloadOfMessage(byte[] message, int length) {
        return Arrays.copyOfRange(message, 5, length+4);
    }

    /**
     * Method that takes an entire peer message and returns its payload
     *
     * @param message message from a peer, including the length and id fields
     * @return payload of message
     */
    public static byte[] payloadOfMessage(byte[] message) {
        return payloadOfMessage(message, lengthOfMessage(message));
    }

    /**
     * Method that compresses the FileManager's representation of the bitfield (a char[]) into a byte array for sending to a peer
     *
     * @param bitfield the FileManager's representation of the bitfield
     * @return the compressed bitfield
     */
    public static byte[] compressBitfield(char[] bitfield) {
        int byteNumber = 0;
        byte currentByte = 0;
        byte[] compressedBitfield = new byte[(bitfield.length+7)/8];
        for(int i = 0; i < bitfield.length; i++) {
            int byteIndex = i % 8;
            if(byteIndex == 0 && i != 0) {
                compressedBitfield[byteNumber] = currentByte;
                byteNumber++;
                currentByte = 0;
            }
            //01234567
            if(bitfield[i] == '1') {
                currentByte |= 1 << (7-byteIndex);
            }
            else if(bitfield[i] == '0') {

            }
            else {
                throw new IllegalArgumentException();
            }
        }
        compressedBitfield[byteNumber] = currentByte;

        return compressedBitfield;
    }

    /**
     * Method that decompresses the network's representation of the bitfield (a byte[]) into a char array for easier reading
     *
     * @param bitfield the network's representation of the bitfield
     * @return the compressed bitfield
     */
    public static char[] decompressBitfield(byte[] bitfield) {
        ArrayList<Character> newBitfield = new ArrayList<>();
        //convert the bitfield to a binary string, then to a char array, then add all the chars in the array to the bitfield
        for(byte b : bitfield) {
            String decompressedBitfield = String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
            for(char c : decompressedBitfield.toCharArray()) {
                newBitfield.add(c);
            }
        }
        //remove the 0 bits used as padding at the end of the byte[] bitfield
        for(int i = newBitfield.size()-1; i >= 0; i++) {
            if(newBitfield.get(i) == '0') {
                newBitfield.remove(i);
            }
            else {
                break;
            }
        }
        //get the raw char data from the arraylist and put it into the char[] to return
        char[] charArrayBitfield = new char[newBitfield.size()];
        for (int i = 0; i < newBitfield.size(); i++) {
            charArrayBitfield[i] = newBitfield.get(i);
        }
        return charArrayBitfield;
    }

}