package development;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
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
	
	private String torrent, fileName;
	private JFrame window = new JFrame();
	private JPanel leftPanel = new JPanel();
	private JPanel rightPanel = new JPanel();
	private JButton start;
	private JButton exit; 

	/**
	 * CONSTRUCTOR
	 */
    public RUBTClient(String torrent, String fileName) {
    	this.torrent = torrent;
    	this.fileName = fileName;
    	
    	window.getContentPane().removeAll();
        window.setPreferredSize(new Dimension(320, 100));
        JSplitPane splitPane = new JSplitPane();
        window.getContentPane().add(splitPane);
        
        leftPanel.setPreferredSize(new Dimension(160, 100));
        createStartButton();
        leftPanel.add(start);
        splitPane.setLeftComponent(leftPanel);
        
        rightPanel.setPreferredSize(new Dimension(160, 100));
        createExitButton();
        rightPanel.add(exit);
        splitPane.setRightComponent(rightPanel);
        
        window.pack();
        start.setVisible(true);
        exit.setVisible(true);
        window.setVisible(true);
    }
    
    @SuppressWarnings("serial")
	private void createStartButton() {
    	start = new JButton( new AbstractAction("Start") {
            @Override
            public void actionPerformed( ActionEvent e ) {
            	try {
                    byte[] peerID = Functions.generatePeerID();
                    TorrentFile torrentFile = readInTorrentFile(torrent);

                    Object[] torrentInfo = parseTorrentFile(torrentFile);
                    byte[] trackerResponse = contactTracker(torrentInfo, torrentFile, peerID);

                    ArrayList<String> peers = TrackerConnection.getPeersFromTrackerResponse(trackerResponse);
                    PeerConnection.downloadFile(peers, torrentFile, peerID, fileName);
                } catch (Exception exception) {
                	System.err.println("Could not save to " +fileName);
                }
            }
        });
    }
    
    @SuppressWarnings("serial")
	private void createExitButton() {
    	exit = new JButton( new AbstractAction("Exit") {
            @Override
            public void actionPerformed( ActionEvent e ) {
            	window.dispose();
            	System.exit(0);
            }
        });
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
    	RUBTClient rubtClient = new RUBTClient(args[0], args[1]);
        
        
    }
}