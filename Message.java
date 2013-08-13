import java.nio.ByteBuffer;

/**
 * A class that encapsulates a single peer message from one peer to another - for example, a handshake message or a choking message.
 */
public class Message {
	
	private final static ByteBuffer KEEP_ALIVE = ByteBuffer.wrap(new byte[] {0,0,0,0});
	private final static String[] MESSAGES = {"choke", "unchoke", "interested", "not interested", "have", "bitfield",
		"request", "piece", "cancel", "port"};
	public static ByteBuffer handshake;

    /**
     * Decode the message in the bytebuffer into a string that describes the type of this message
     * @param message the message inside a ByteBuffer
     * @return a String in MESSAGES if it's able to be decoded into that type, otherwise null
     */
	public static String parseMessage(ByteBuffer message) {
		if(KEEP_ALIVE.equals(message))
			return "keep-alive";
		else if(message.capacity() > 4)
			return MESSAGES[message.get(4)];
		else
			return null;
	}

    /**
     * Creates a handshake message and sets it as a data member
     * @param infoHash the info hash of the message
     */
	public static void setHandshake(ByteBuffer infoHash) {
		handshake = ByteBuffer.allocate(68);
		handshake.put((byte) 19);
		handshake.put("BitTorrent protocol".getBytes());
		handshake.put(new byte[] {0,0,0,0,0,0,0,0});
		handshake.put(infoHash);
		handshake.put(ClientInfo.PEER_ID);
	}

    /**
     * Verify that the data in the handshake is correct
     * @param peerHandshake the ByteBuffer containing the handshake message
     * @return true if valid, otherwise false
     */
	public static boolean verifyHandshake(ByteBuffer peerHandshake) {
		if(peerHandshake.capacity() < 68)
			return false;
		else {
			for(int i = 0; i < 20; i++) {
				if(handshake.get(i+28) != peerHandshake.get(i+28))
					return false;
			}
			return true;
		}
	}

    /**
     * Creates a keep alive message
     * @return a keep alive message
     */
	public static ByteBuffer createKeepAlive() {
		return KEEP_ALIVE;
	}

    /**
     * Creates a choking message
     * @return a choking message
     */
	public static ByteBuffer createChoke() {
		ByteBuffer result = ByteBuffer.allocate(5);
		result.putInt(1);
		return result.put((byte) 0);
	}


    /**
     * Creates an unchoking message
     * @return an unchoking message
     */
	public static ByteBuffer createUnchoke() {
		ByteBuffer result = ByteBuffer.allocate(5);
		result.putInt(1);
		return result.put((byte) 1);
	}


    /**
     * Creates an interested message
     * @return an interested message
     */
	public static ByteBuffer createInterested() {
		ByteBuffer result = ByteBuffer.allocate(5);
		result.putInt(1);
		return result.put((byte) 2);
	}

    /**
     * Creates an uninterested message
     * @return an uninterested message
     */
	public static ByteBuffer createNotInterested() {
		ByteBuffer result = ByteBuffer.allocate(5);
		result.putInt(1);
		return result.put((byte) 3);
	}


    /**
     * Creates a have message
     * @param index the index of the piece specified in the message
     * @return a have message
     */
	public static ByteBuffer createHave(int index) {
		ByteBuffer result = ByteBuffer.allocate(9);
		result.putInt(5);
		result.put((byte) 4);
		return result.putInt(index);
	}


    /**
     * Creates a request message
     * @param index the index of the piece specified in the message
     * @param begin the beginning offset of the piece specified in the message
     * @param length the length of the data to request specified in the message
     * @return a request message
     */
	public static ByteBuffer createRequest(int index, int begin, int length) {
		ByteBuffer result = ByteBuffer.allocate(17);
		result.putInt(13);
		result.put((byte) 6);
		result.putInt(index);
		result.putInt(begin);
		return result.putInt(length);
	}


    /**
     * Creates a piece message
     * @param index the index of the piece specified in the message
     * @param begin the beginning offset of the piece specified in the message
     * @param block the file data to be sent to the peer
     * @return a piece message
     */
	public static ByteBuffer createPiece(int index, int begin, byte[] block) {
		ByteBuffer result = ByteBuffer.allocate(13+block.length);
		result.putInt(9+block.length);
		result.put((byte) 7);
		result.putInt(index);
		result.putInt(begin);
		return result.put(block);
	}
}