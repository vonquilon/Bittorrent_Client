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
	public String IPAddress;
	private int port;
	//public boolean isHandshakeVerified = false;
	public volatile ArrayList<ByteBuffer> outputQueue;
	public boolean done = false;
	
	public PeerConnection(String IPAddress, String port) {
		this.IPAddress = IPAddress;
		this.port = Integer.parseInt(port);
		outputQueue = new ArrayList<ByteBuffer>();
	}
	
	@Override
	public void run() {
		try {
			Socket socket = new Socket(IPAddress, port);
			Output out = new Output(this, socket.getOutputStream());
			Input in = new Input(this, socket.getInputStream());
			new Thread(out).start();
			new Thread(in).start();
			while(!stopped) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					//do nothing
				}
			}
			socket.close();
		} catch (IOException e) {
			System.out.println("Could no connect to " + IPAddress);
		}
	}	
}