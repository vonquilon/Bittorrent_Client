import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.NoSuchElementException;
import javax.swing.JButton;
import javax.swing.JFrame;
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
	private JButton start, pause, exit;
	private String torrentName, fileName;
	
	/**
	 * Creates a RUBTClient with the specified torrent file name and the
	 * name of the file to be saved into the hard disk.
	 * 
	 * @param torrentName - the torrent file name
	 * @param fileName - the name of the file to be saved
	 */
	public RUBTClient(String torrentName, String fileName) {
		frame = new JFrame("RUBTClient");
		textArea = new JTextArea(10, 50);
		bottomPanel = new JPanel(new BorderLayout());
		createStartButton();
		createPauseButton();
		createExitButton();
		
		this.torrentName = torrentName;
		this.fileName = fileName;
	}

	/**
	 * Creates a start button that when pressed will start the process of downloading a file
	 * from a torrent.
	 */
	private void createStartButton() {
    	start = new JButton("Start"); 
    	start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	try {
            		ClientInfo.setPort();
            		System.out.println("Using port: " + ClientInfo.port + "\n");
            		TorrentInfo torrentInfo = TorrentParser.parseTorrent(torrentName);
            	} catch (NoSuchElementException exception) {
            		System.err.println(exception.getMessage() + "\n");
            	}
            }
        });
    }
	
	/**
	 * Creates a pause button that when pressed will pause the client's progress.
	 */
	private void createPauseButton() {
    	pause = new JButton("Pause");
    	pause.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent e ) {
            	
            }
        });
    }
	
	/**
	 * Creates an exit button that when pressed will close all alive threads, close all
	 * open connections, closes frame processes, and exits the program.
	 */
	private void createExitButton() {
    	exit = new JButton("Exit");
    	exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent e ) {
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
		
		start.setPreferredSize(new Dimension(200, 20));
		pause.setPreferredSize(new Dimension(200, 20));
		exit.setPreferredSize(new Dimension(200, 20));
		
		bottomPanel.add(start, BorderLayout.LINE_START);
		bottomPanel.add(pause, BorderLayout.CENTER);
		bottomPanel.add(exit, BorderLayout.LINE_END);
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