import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

/**
 * RUBTClient is a simple BitTorrent client that parses a torrent file
 * and downloads an image file from one peer.
 * 
 * @authors Von Kenneth Quilon & Alex Loh
 * @date 07/12/2013
 * @version 1.0
 */
public class RUBTClient {

	/**
	 * CONSTRUCTOR
	 */
    public RUBTClient() {

    }

    /**
     * This method returns a new TorrentFile class.
     * 
     * @param torrentFilename Name of torrent file
     * @return TorrentFile
     */
    public static TorrentFile readInTorrentFile(String torrentFilename) {
    	
        return new TorrentFile(torrentFilename);
        
    }

    /**
     * This method returns an Object[] that contains a Map,
     * which is filled with torrent information, and a ByteBuffer
     * of the info hash.
     * 
     * @param torrentFile
     * @return Object[] Holds torrent information
     */
    public static Object[] parseTorrentFile(TorrentFile torrentFile) {
    	
        return torrentFile.parseTorrent();
        
    }

    /**
     * This method contacts the tracker and obtains a response from
     * the tracker.
     * 
     * @param torrentInfo Contains torrent information
     * @param torrentFile
     * @param peerID 
     * @return byte[] Tracker response in bytes
     */
    public static byte[] contactTracker(Object[] torrentInfo, TorrentFile torrentFile, byte[] peerID) {
    	
        TrackerConnection trackerConnection = new TrackerConnection();
        return trackerConnection.getTrackerResponse(torrentInfo, torrentFile, peerID);
        
    }

    /**
     * Parses the torrent file, gets tracker response, obtain list of peers,
     * downloads file, and writes file to the disk.
     * 
     * @param args args[0]: torrent file
     * 			   args[1]: name of file to save data to
     */
    public static void main(String args[]) {
    	
        try {
            byte[] peerID = Functions.generatePeerID();
            TorrentFile torrentFile = readInTorrentFile(args[0]);

            Object[] torrentInfo = parseTorrentFile(torrentFile);
            byte[] trackerResponse = contactTracker(torrentInfo, torrentFile, peerID);

            ArrayList<String> peers = TrackerConnection.getPeersFromTrackerResponse(trackerResponse);
            PeerConnection.downloadFile(peers, torrentFile, peerID, args[1]);

            //saveDownloadedFile(args[1], file);
        } catch (Exception e) {
        	System.err.println("Could not save to " + args[1]);
        }
        
    }

    /**
     * Private helper method that saves the downloaded data into the disk.
     * 
     * @param downloadedFilename Name of file to save to
     * @param fileData Data to be saved in bytes
     */
    private static void saveDownloadedFile(String downloadedFilename, byte[] fileData) {
    	
        try (FileOutputStream fileWriter = new FileOutputStream(downloadedFilename)) {
            fileWriter.write(fileData);
            System.out.println("\nSaved file as " + downloadedFilename);
        } catch (IOException e) {
            System.err.println("Could not save to " + downloadedFilename);
        }
        
    }
}