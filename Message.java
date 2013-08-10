import java.nio.ByteBuffer;

public class Message {
	
	private final static ByteBuffer KEEP_ALIVE = ByteBuffer.wrap(new byte[] {0,0,0,0});
	private final static String[] MESSAGES = {"choke", "unchoke", "interested", "not interested", "have", "bitfield",
		"request", "piece", "cancel", "port"};
	public static ByteBuffer handshake;
	
	public static String parseMessage(ByteBuffer message) {
		if(KEEP_ALIVE.equals(message))
			return "keep-alive";
		else if(message.capacity() > 4)
			return MESSAGES[message.get(4)];
		else
			return null;
	}
	
	public static void setHandshake(ByteBuffer infoHash) {
		handshake = ByteBuffer.allocate(68);
		handshake.put((byte) 19);
		handshake.put("BitTorrent protocol".getBytes());
		handshake.put(new byte[] {0,0,0,0,0,0,0,0});
		handshake.put(infoHash);
		handshake.put(ClientInfo.PEER_ID);
	}
	
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
}