import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * RUBTClient is a simple BitTorrent client that parses a torrent file
 * and downloads a file specified in the torrent meta info.
 * 
 * @authors Von Kenneth Quilon & Alex Loh
 * @date 08/01/2013
 * @version 1.2
 * 
 * UPDATES:
 * -New GUI
 * -Now picks a port between 6881 and 6889
 */
public class RUBTClient {
	
	private JFrame frame;
	private JTextArea textArea;
	private JPanel bottomPanel;
	private JButton start, exit;
	private JLabel program, progress, rateLabel, rate;
	private String torrentName, fileName;
	private boolean started = false;
	TorrentInfo torrentInfo = null;
	private TrackerConnection trackerConnection = null;
	private ConnectionManager connection = null;
	
	/**
	 * Creates a RUBTClient with the specified torrent file name and the
	 * name of the file to be saved into the hard disk.
	 * 
	 * @param torrentName - the torrent file name
	 * @param fileName - the name of the file to be saved
	 */
	public RUBTClient(String torrentName, String fileName) {
		frame = new JFrame("RUBTClient");
		textArea = new JTextArea(30, 50);
		bottomPanel = new JPanel();
		createStartButton();
		createExitButton();
		program = new JLabel("Program: ");
		progress = new JLabel("Not started"); progress.setPreferredSize(new Dimension(100, 10));
		rateLabel = new JLabel("Rate: ");
		rate = new JLabel();
		
		this.torrentName = torrentName;
		this.fileName = fileName;
	}

	/**
	 * Creates a start/pause button that when pressed will start the process of downloading a file
	 * from a torrent and when pressed again, will pause the program.
	 */
	private void createStartButton() {
    	start = new JButton(new ImageIcon("pause_start icon.png")); 
    	start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	if(!started) {
            		started = true; progress.setText("Started");
            		try {
            			if(ClientInfo.port == null) {
            				ClientInfo.setPort();
            				System.out.println("Using port: " + ClientInfo.port + "\n");
            			}
            		} catch (NoSuchElementException exception) {
            			System.err.println(exception.getMessage() + "\n");
            		}
            	
            		if(torrentInfo == null) {
            			torrentInfo = TorrentParser.parseTorrent(torrentName);
            			ClientInfo.setLeft(torrentInfo.file_length);
            			Message.setHandshake(torrentInfo.info_hash);
            		}
            		if(trackerConnection == null) {
            			trackerConnection = new TrackerConnection(torrentInfo);
            			new Thread(trackerConnection).start();
            		}
            		if(connection == null) {
            			connection = new ConnectionManager(torrentInfo);
            			new Thread(connection).start();
            		}
            	} else {
            		started = false; progress.setText("Paused");
            	}
            }
        });
    }
	
	/**
	 * Creates an exit button that when pressed will close all alive threads, close all
	 * open connections, closes frame processes, and exits the program.
	 */
	private void createExitButton() {
    	exit = new JButton(new ImageIcon("exit icon.png"));
    	exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	if(trackerConnection != null)
            		trackerConnection.cancelTimer();
            	frame.dispose();
            	System.exit(0);
            }
        });
    }
	
	/**
	 * Organizes and builds the GUI. System.out and System.err are rerouted to the GUI's JTextArea.
	 */
	public void build() {
		PrintStream printStream = new PrintStream(new TextAreaOutputStream());
		System.setOut(printStream);
		System.setErr(printStream);
		
		frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		JScrollPane scroll = new JScrollPane(textArea);
		frame.getContentPane().add(scroll, BorderLayout.CENTER);
		
		textArea.setEditable(false);
		textArea.setBackground(Color.black);
		textArea.setForeground(Color.white);
		textArea.append("/**\n");
		textArea.append(" *  Welcome to RUBTClient!\n");
		textArea.append(" */\n\n");
		
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.LINE_AXIS));
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		bottomPanel.add(Box.createHorizontalGlue());
		bottomPanel.add(program);
		bottomPanel.add(progress);
		bottomPanel.add(rateLabel);
		bottomPanel.add(rate);
		bottomPanel.add(Box.createRigidArea(new Dimension(180, 0)));
		bottomPanel.add(start);
		bottomPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		bottomPanel.add(exit);
		frame.getContentPane().add(bottomPanel, BorderLayout.PAGE_END);
		
		frame.pack();
		frame.setVisible(true);
	}
	
	/**
	 * This class implements a TextAreaOutputStream that reroutes the OutputStream
	 * to the GUI's JTextArea.
	 * 
	 * @author Von Kenneth Quilon
	 * @date 08/01/2013
	 * @version 1.0
	 */
	private class TextAreaOutputStream extends OutputStream {
		
		/**
		 * Creates a TextAreaOutputStream.
		 */
		public TextAreaOutputStream() {
			
		}
		
		/**
		 * Overridden write method from the OutputStream class. Reroutes the OutputStream
		 * to this GUI's JTextArea.
		 * 
		 * @param b - the data to be printed
		 */
		@Override
		public void write(int b) throws IOException {
			textArea.append(String.valueOf((char) b));
			textArea.setCaretPosition(textArea.getDocument().getLength());
		}
	}
	
	/**
	 * Runs the RUBTClient.
	 * 
	 * @param args - args[0]: the torrent file name
	 * 				 args[1]: the file name to save to
	 */
	public static void main(String[] args) {
		RUBTClient client = new RUBTClient(args[0], args[1]);
		client.build();
	}
}