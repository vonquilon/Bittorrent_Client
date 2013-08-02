import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * TrackerConnection sends HTTP request messages to the tracker. The first request obtains
 * a tracker response. The next requests informs the tracker of the client's progress.
 * 
 * @author Von Kenneth Quilon
 * @date 08/01/2013
 * @version 1.0
 */
public class TrackerConnection implements Runnable{

	private boolean stopped = false;
	private TorrentInfo torrentInfo;
	
	/**
	 * Creates a TrackerConnection.
	 * 
	 * @param torrentInfo - contains torrent information
	 */
	public TrackerConnection(TorrentInfo torrentInfo) {
		this.torrentInfo = torrentInfo;
	}
	
	/**
	 * Starts the HTTP GET request process.
	 */
	@SuppressWarnings("rawtypes")
	public void run() {
		try {
			URL url = makeURL(torrentInfo.announce_url.toExternalForm(), ClientInfo.PEER_ID, ClientInfo.port,
					torrentInfo.info_hash, ClientInfo.uploaded, ClientInfo.downloaded, ClientInfo.left, null);
			System.out.println("Request sent to tracker");
	
			InputStream is = url.openStream();
			byte[] response = new byte[is.available()];
			is.read(response);
			is.close();
			
			TrackerResponse.setFields((Map) Bencoder2.decode(response));
			System.out.println("\nTracker Response Information:");
			System.out.println("Interval: " + TrackerResponse.interval + " secs");
			System.out.println("Minimum interval: " + TrackerResponse.minInterval + " secs");
			System.out.println("Peers: " + TrackerResponse.peers + "\n");
		} catch (MalformedURLException e) {
			System.err.println("Unknown URL protocol!");
		} catch (IOException e) {
			System.err.println("I/O error!");
		} catch (BencodingException e) {
			System.err.println(e.getMessage());
		}
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
    private URL makeURL(String announce, byte[] peerID, int port, ByteBuffer infoHashBytes, int uploaded,
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
    private String hexStringToURL(String hexString) {
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
    private String bytesToHex(ByteBuffer bytes) {
        String hexString = "";
     
        for (int i = 0; i < bytes.capacity(); i++)
            hexString += String.format("%02X", bytes.get(i) & 0xff);

        return hexString;
    }
}