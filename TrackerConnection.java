import java.net.MalformedURLException;
import java.net.URL;


public class TrackerConnection implements Runnable{

	private boolean stopped = false;
	private TorrentInfo torrentInfo;
	
	public TrackerConnection(TorrentInfo torrentInfo) {
		this.torrentInfo = torrentInfo;
	}
	
	public void run() {
		while(!stopped) {
			//URL url = makeURL()
		}
	}
	
	/**
     * Private helper method that generates a URL object.
     *
     * @param announce      The announce string data from the torrent file
     * @param peerID        
     * @param infoHashBytes
     * @param uploaded      Bytes uploaded
     * @param downloaded    Total bytes downloaded including header data
     * @param left          Bytes left to download
     * @param event         {started, stopped, completed}
     * @return URL 			The created URL object
     * @throws MalformedURLException Bad URL
     */
    private URL makeURL(String announce, byte[] peerID, byte[] infoHashBytes, int uploaded,
    		int downloaded, int left, String event) throws MalformedURLException {
        StringBuilder urlSb = new StringBuilder();
        urlSb.append(announce);
        urlSb.append("?info_hash=");
        urlSb.append(hexStringToURL(bytesToHex(infoHashBytes)));
        urlSb.append("&peer_id=");
        urlSb.append(new String(peerID));
        urlSb.append("&port=9593");

        if (uploaded >= 0)
            urlSb.append("&uploaded=" + Integer.toString(uploaded));
        if (downloaded >= 0)
            urlSb.append("&downloaded=" + Integer.toString(downloaded));
        if (left >= 0)
            urlSb.append("&left=" + Integer.toString(left));
        if (event != null)
            urlSb.append("&event=" + event);

        return new URL(urlSb.toString());
    }
    
    /**
     * Private helper method that converts a hex string
     * into a URL encoded string.
     *
     * @param hexString
     * @return String The URL encoded hex string
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
     * Private helper method that converts a byte[] into a hex String.
     *
     * @param bytes The byte[] to be converted
     * @return hexString
     */
    private String bytesToHex(byte[] bytes) {
        String hexString = "";

        for (byte byteObject : bytes)
            hexString += String.format("%02X", byteObject & 0xff);

        return hexString;
    }
}