import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * PeerConnection manages a peer's connection.
 * 
 * @author Von Kenneth Quilon & Alex Loh
 * @date 08/11/2013
 * @version 1.0
 */
public class PeerConnection implements Runnable{

	private volatile boolean stopped = false;
	private Socket socket = null;
	private Output out = null;
	private Input in = null;
	private int port;
	private final Timer TIMER = new Timer();
	private TimerTask task = null;
	private long delay;
	
	/**
	 * The IP address of the peer.
	 */
	public String IPAddress;
	
	/**
	 * The file manager.
	 */
	public FileManager fileManager;
	
	/**
	 * The output queue.
	 */
	public volatile ArrayList<ByteBuffer> outputQueue;
	
	/**
	 * If bitfield processing is done.
	 */
	public boolean done = false;
	
	/**
	 * If this is an upload connection.
	 */
	public boolean isUpload;
	
	/**
	 * Constructs a peer connection based on the peer's IP address and port number.
	 * 
	 * @param IPAddress - the peer's IP address
	 * @param port - the peer's port number
	 * @param isUpload - if it's an upload connection
	 * @param fileManager
	 */
	public PeerConnection(String IPAddress, String port, boolean isUpload, FileManager fileManager) {
		this.IPAddress = IPAddress;
		this.port = Integer.parseInt(port);
		this.isUpload = isUpload;
		this.fileManager = fileManager;
		outputQueue = new ArrayList<ByteBuffer>();
		if(isUpload)
			delay = 2500;
		else
			delay = 2000;
	}
	
	/**
	 * Constructs a peer connection based on a socket.
	 * 
	 * @param socket - the peer's socket
	 * @param isUpload - if it's an upload connection
	 * @param fileManager
	 */
	public PeerConnection(Socket socket, boolean isUpload, FileManager fileManager) {
		this.socket = socket;
		this.IPAddress = socket.getInetAddress().getHostAddress();
		this.port = socket.getPort();
		this.isUpload = isUpload;
		this.fileManager = fileManager;
		outputQueue = new ArrayList<ByteBuffer>();
		if(isUpload)
			delay = 2500;
		else
			delay = 2000;
	}
	
	/**
	 * Runs a peer connection.
	 */
	@Override
	public void run() {
		try {
			if(socket == null)
				socket = new Socket(IPAddress, port);
			out = new Output(this, socket.getOutputStream());
			in = new Input(this, socket.getInputStream());
			new Thread(out).start();
			new Thread(in).start();
			while(!stopped) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					//do nothing
				}
				if(ClientInfo.left == 0)
					close();
			}
		} catch (IOException e) {
			System.out.println("Could not connect to " + IPAddress);
			close();
		}
	}

    public long getDataRate() {
        return in.dataRate();
    }


	/**
	 * Schedules a timer to handle keep-alive messages.
	 */
	public void scheduleTask() {
		if(isUpload) {
			task = new TimerTask() {
				public void run() {
					close();
				}
			};
		} else {
			task = new TimerTask() {
				public void run() {
					outputQueue.add(Message.createKeepAlive());
				}
			};
		}
		TIMER.schedule(task, delay, delay);
	}
	
	/**
	 * Resets the timer.
	 */
	public void resetTimer() {
		if(task != null) {
			task.cancel(); TIMER.purge();
			scheduleTask();
		}
	}
	
	/**
	 * Cancels the timer.
	 */
	private void cancelTimer() {
		if(task != null)
			task.cancel();
		TIMER.cancel();
	}
	
	/**
	 * Closes a peer connection.
	 * 
	 * throws IOException if connection could not be closed
	 */
	public void close() {
		stopped = true;
		try {
			if(ConnectionManager.unchoked.containsKey(IPAddress))
				ConnectionManager.unchoked.remove(IPAddress);
			if(ConnectionManager.choked.containsKey(IPAddress))
				ConnectionManager.choked.remove(IPAddress);
			if(ConnectionManager.downloading.containsKey(IPAddress))
				ConnectionManager.downloading.remove(IPAddress);
			cancelTimer();
			if(in != null)
				in.close(); 
			if(out != null)
				out.close();
			if(socket != null)
				socket.close();
		} catch (IOException e) {
			System.out.println("Could not close connection with " + IPAddress);
		}
	}
}