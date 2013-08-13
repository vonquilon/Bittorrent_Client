import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class PeerConnection implements Runnable{

	private volatile boolean stopped = false;
	private Socket socket = null;
	private Output out = null;
	private Input in = null;
	private int port;
	private final Timer TIMER = new Timer();
	private TimerTask task = null;
	private long delay;
	public String IPAddress;
	public FileManager fileManager;
	public volatile ArrayList<ByteBuffer> outputQueue;
	public boolean done = false;
	public boolean isUpload;

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
	
	public void resetTimer() {
		if(task != null) {
			task.cancel(); TIMER.purge();
			scheduleTask();
		}
	}
	
	private void cancelTimer() {
		if(task != null)
			task.cancel();
		TIMER.cancel();
	}
	
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