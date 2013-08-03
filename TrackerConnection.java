import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * TrackerConnection sends HTTP request messages to the tracker. The first request obtains
 * a tracker response. The next requests informs the tracker of the client's progress.
 * 
 * @author Von Kenneth Quilon
 * @date 08/01/2013
 * @version 1.0
 */
public class TrackerConnection implements Runnable{

	private static TorrentInfo torrentInfo;
	private final static Timer TIMER = new Timer();
	private static long delay;
	private static TimerTask task;
	
	/**
	 * Creates a TrackerConnection.
	 * 
	 * @param torrentInfo - contains torrent information
	 */
	public TrackerConnection(TorrentInfo torrentInfo) {
		TrackerConnection.torrentInfo = torrentInfo;
	}
	
	/**
	 * Starts the HTTP GET request process.
	 */
	@SuppressWarnings("rawtypes")
	public void run() {
		try {
			URL url = makeURL(torrentInfo.announce_url.toExternalForm(), ClientInfo.PEER_ID, ClientInfo.port,
					torrentInfo.info_hash, ClientInfo.uploaded, ClientInfo.downloaded, ClientInfo.left, null);
			System.out.println("Request sent to tracker.\n");
	
			InputStream is = url.openStream();
			byte[] response = new byte[is.available()];
			is.read(response);
			is.close();

			TrackerResponse.setFields((Map) Bencoder2.decode(response));
			delay = ((TrackerResponse.interval - TrackerResponse.minInterval)/2
					+ TrackerResponse.minInterval) * 1000;
			System.out.println("Tracker Response Information:");
			System.out.println("Interval: " + TrackerResponse.interval + " secs");
			System.out.println("Minimum interval: " + TrackerResponse.minInterval + " secs");
			System.out.println("Peers: " + TrackerResponse.peers + "\n");
		} catch (MalformedURLException e) {
			System.err.println("Unknown URL protocol!");
		} catch (IOException e) {
			System.err.println("Could not contact tracker.");
		} catch (BencodingException e) {
			System.err.println(e.getMessage());
		}
		scheduleTask();
	}
	
	/**
     * Generates a URL object.
     *
     * @param announce - The announce string data
     * @param peerID - The client's 20 bytes peer ID
     * @param infoHashBytes - The SHA-1 encoded info bytes
     * @param uploaded - The number of bytes the client has uploaded
     * @param downloaded - The number of bytes the client has downloaded
     * @param left - The number of bytes has left to download
     * @param event - {started, stopped, completed}
     * @return URL - The created URL object
     * @throws MalformedURLException - Unknown URL protocol
     */
    private static URL makeURL(String announce, byte[] peerID, int port, ByteBuffer infoHashBytes, int uploaded,
    		int downloaded, int left, String event) throws MalformedURLException {
        StringBuilder urlSb = new StringBuilder();
        urlSb.append(announce);
        urlSb.append("?info_hash=");
        urlSb.append(hexStringToURL(bytesToHex(infoHashBytes)));
        urlSb.append("&peer_id=");
        urlSb.append(new String(peerID));
        urlSb.append("&port=");
        urlSb.append(port);

        if (uploaded >= 0)
            urlSb.append("&uploaded=" + uploaded);
        if (downloaded >= 0)
            urlSb.append("&downloaded=" + downloaded);
        if (left >= 0)
            urlSb.append("&left=" + left);
        if (event != null)
            urlSb.append("&event=" + event);
        
        return new URL(urlSb.toString());
    }
    
    /**
     * Converts a hex string into a URL encoded string.
     *
     * @param hexString
     * @return String - The URL encoded hex string
     */
    private static String hexStringToURL(String hexString) {
        int length = hexString.length();
        char[] urlEncoded = new char[length + length / 2];

        for (int i = 0, j = 0; j < length; i++, j++) {
            urlEncoded[i] = '%';
            i++;
            urlEncoded[i] = hexString.charAt(j);
            i++;
            j++;
            urlEncoded[i] = hexString.charAt(j);
        }

        return new String(urlEncoded);
    }

    /**
     * Converts a ByteBuffer into a hex String.
     *
     * @param bytes - The ByteBuffer to be converted
     * @return hexString - The hex string representation
     */
    private static String bytesToHex(ByteBuffer bytes) {
        String hexString = "";
     
        for (int i = 0; i < bytes.capacity(); i++)
            hexString += String.format("%02X", bytes.get(i) & 0xff);

        return hexString;
    }
    
    /**
     * Schedules a task that periodically contacts the tracker.
     */
    private static void scheduleTask() {
    	task = new TimerTask() {
    		public void run() {
				try {
					URLConnection connection = makeURL(torrentInfo.announce_url.toExternalForm(),
							ClientInfo.PEER_ID, ClientInfo.port, torrentInfo.info_hash, ClientInfo.uploaded,
							ClientInfo.downloaded, ClientInfo.left, null).openConnection();
					connection.connect();
					System.out.println("Periodic request sent to tracker.\n");
				} catch (MalformedURLException e) {
					System.err.println("Unknown URL protocol!");
				} catch (IOException e) {
					System.err.println("Could not contact tracker.");
				}
    		}
    	};
    	TIMER.schedule(task, delay, delay);
    }
    
    /**
     * Resets the task that periodically contacts the tracker. It contacts the tracker
     * every (interval - min interval)/2 + min interval seconds.
     */
    public static void resetTimer() {
    	task.cancel(); TIMER.purge();
    	scheduleTask();
    }
    
    /**
     * Cancels the task that periodically contacts the tracker.
     */
    public void cancelTimer() {
    	task.cancel(); TIMER.cancel();
    }
}