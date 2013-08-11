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

public class PeerConnection implements Runnable{

	private volatile boolean stopped = false;
	private Socket socket;
	private Output out;
	private Input in;
	private int port;
	public String IPAddress;
	//public boolean isHandshakeVerified = false;
	public volatile ArrayList<ByteBuffer> outputQueue;
	public boolean done = false;
	public boolean isUpload;
	
	public PeerConnection(String IPAddress, String port, boolean isUpload) {
		this.IPAddress = IPAddress;
		this.port = Integer.parseInt(port);
		this.isUpload = isUpload;
		outputQueue = new ArrayList<ByteBuffer>();
	}
	
	@Override
	public void run() {
		try {
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
			}
		} catch (IOException e) {
			System.out.println("Could no connect to " + IPAddress);
		}
	}
	
	public void close() {
		stopped = true;
		try {
			in.close(); out.close(); socket.close(); 
		} catch (IOException e) {
			System.out.println("Could not close connection with " + IPAddress);
		}
	}
}