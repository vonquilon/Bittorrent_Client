import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;

/**
 * This class represents a connection between the tracker and
 * this computer. The tracker must be contacted to obtain the peer 
 * list and to send updates when downloading data from peers.
 *
 * @version 1.0
 * @authors Von Kenneth Quilon & Alex Loh
 * @date 07/12/2013
 */
public class TrackerConnection {
	
    /**
     * Initializes the class, which is empty.
     */
    public TrackerConnection() {
    	
    }

    /**
     * This method obtains the response from the tracker after an HTTP GET request.
     * 
     * Pre-conditions:  torrentInfo must contain two valid objects.
     * Post-conditions: An HTTP GET request will be initiated and the
     * 					response will be returned.
     *
     * @param torrentInfo Object[] that holds torrent information
     * @param torrentFile the torrent file
     * @param peerID
     * @return response   Tracker response in a byte[]
     * @throws java.io.IOException Failed to get a response
     */
    public byte[] getTrackerResponse(Object[] torrentInfo, TorrentFile torrentFile, byte[] peerID) {

        byte[] response = null;
        torrentFile.getFileInfo(torrentInfo);

        try {

            URL url = Functions.makeURL(torrentFile.getAnnounce(), peerID, torrentFile.getInfoHashBytes(), 0, 0, torrentFile.getFileSize(), null);
            System.out.println("HTTP GET\n" + url.toString());
            InputStream is = url.openStream();
            response = new byte[is.available()];
            is.read(response);
            is.close();
            System.out.println("DONE\n");

        } catch (IOException e) {
            System.err.println("Failed I/O! Could not get tracker response.");
            System.exit(1);
        }

        return response;
    }

    /**
     * This method parses the tracker response and returns an ArrayList of peers.
     * 
     * Pre-conditions:  response must contain peer information.
     * Post-conditions: The ArrayList of peers will be returned.
     *
     * @param response byte[] of tracker response
     * @return peers
     * @throws BencodingException Bencoding error
     */
    @SuppressWarnings("rawtypes")
    public static ArrayList<String> getPeersFromTrackerResponse(byte[] response) {

        Map responseMap = null;

        try {
            responseMap = (Map) Bencoder2.decode(response);
        } catch (BencodingException e) {
            e.printStackTrace();
            System.exit(1);
        }

        ArrayList<String> peers = decodeCompressedPeers(responseMap);
        System.out.println("Available peers:\n" + peers.toString() + "\n");

        return peers;

    }

    /**
     * Private helper method that obtains the peers from a Map
     * and puts them in an ArrayList.
     *
     * @param map 		Contains the peers
     * @return peerURLs ArrayList of peers
     */
    private static ArrayList<String> decodeCompressedPeers(@SuppressWarnings("rawtypes") Map map) {

        ByteBuffer peers = (ByteBuffer) map.get(ByteBuffer.wrap("peers".getBytes()));
        ArrayList<String> peerURLs = new ArrayList<String>();
        try {
            while (true) {
                String ip = String.format("%d.%d.%d.%d",
                        peers.get() & 0xff,
                        peers.get() & 0xff,
                        peers.get() & 0xff,
                        peers.get() & 0xff);
                int port = peers.get() * 256 + peers.get();
                peerURLs.add(ip + ":" + port);
            }
        } catch (BufferUnderflowException e) {
            //done
        }

        return peerURLs;

    }
}