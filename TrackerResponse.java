import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;

/**
 * TrackerResponse holds information from a tracker's response.
 * 
 * @author Von Kenneth Quilon
 * @date 08/01/2013
 * @version 1.0
 */
public class TrackerResponse {
	
	/**
	 * The time in seconds that the client should wait between sending regular requests
	 * to the tracker.
	 */
	public static int interval;
	
	/**
	 * The minimum announce interval in seconds.
	 */
	public static int minInterval;
	
	/**
	 * The list of peers.
	 */
	public static ArrayList<String> peers;
	
	/**
	 * Sets the public static fields of this class.
	 * 
	 * @param responseMap
	 */
	public static void setFields(@SuppressWarnings("rawtypes") Map responseMap) {
		interval = (Integer) responseMap.get(ByteBuffer.wrap("interval".getBytes()));
		minInterval = (Integer) responseMap.get(ByteBuffer.wrap("min interval".getBytes()));
		ByteBuffer peersBuffer = (ByteBuffer) responseMap.get(ByteBuffer.wrap("peers".getBytes()));
		peers = decodeCompressedPeers(peersBuffer);
	}
	
	 /**
     * Obtains the peers from a ByteBuffer and puts them in an ArrayList.
     *
     * @param peers - Contains the peers
     * @return peerURLs - The ArrayList of peers
     */
    private static ArrayList<String> decodeCompressedPeers(ByteBuffer peers) {
        ArrayList<String> peerURLs = new ArrayList<String>();
        try {
            while (true) {
                String ip = String.format("%d.%d.%d.%d",
                        peers.get() & 0xff,
                        peers.get() & 0xff,
                        peers.get() & 0xff,
                        peers.get() & 0xff);
                int firstByte = (0x000000FF & ((int)peers.get()));
                int secondByte = (0x000000FF & ((int)peers.get()));
                int port  = (firstByte << 8 | secondByte);
                //int port = peers.get() * 256 + peers.get();
                peerURLs.add(ip + ":" + port);
            }
        } catch (BufferUnderflowException e) {
            //done
        }
        return peerURLs;
    }
}