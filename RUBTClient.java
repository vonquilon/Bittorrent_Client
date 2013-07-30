import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

/**
 * RUBTClient is a simple BitTorrent client that parses a torrent file
 * and downloads an image file from one peer.
 * 
 * @authors Von Kenneth Quilon & Alex Loh
 * @date 07/12/2013
 * @version 1.0
 */
public class RUBTClient {
	
	private String fileName;
	private JFrame window = new JFrame();
	private JPanel leftPanel = new JPanel();
	private JPanel rightPanel = new JPanel();
	private JButton start;
	private JButton exit; 
	private TorrentFile torrentFile;
	private TrackerConnection trackerConnection;

	/**
	 * CONSTRUCTOR
	 */
    public RUBTClient(String torrent, String fileName) {
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
        
        torrentFile = new TorrentFile(torrent);
    }
    
    @SuppressWarnings("serial")
	private void createStartButton() {
    	start = new JButton( new AbstractAction("Start") {
            @Override
            public void actionPerformed( ActionEvent e ) {
            	try {
                    byte[] peerID = Functions.generatePeerID();
                    torrentFile.start();
                    torrentFile.join();
                    
                    trackerConnection = new TrackerConnection(peerID);
                    trackerConnection.start();
                    trackerConnection.join();
                    
                    ArrayList<String> peers = trackerConnection.getPeers();
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