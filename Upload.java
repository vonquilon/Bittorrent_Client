import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Upload manages an upload connection to a peer.
 * 
 * @author Von Kenneth Quilon
 * @date 08/12/2013
 * @version 1.0
 */
public class Upload implements Runnable{

	private volatile boolean stopped = false;
	private ServerSocket serverSocket;
	private Socket socket = null;
	private FileManager fileManager;
	
	/**
	 * Constructs an upload.
	 * 
	 * @param fileManager
	 */
	public Upload(FileManager fileManager) {
		this.fileManager = fileManager;
		try {
			serverSocket = new ServerSocket(ClientInfo.port);
		} catch (IOException e) {
			System.out.println("Seeding failed!");
		}
	}
	
	/**
	 * Starts to accept incoming connections.
	 */
	@Override
	public void run() {
		try {
			serverSocket.setSoTimeout(5000);
		} catch (SocketException e1) {
			//do nothing
		}
		while(!stopped) {
			try {
				socket = serverSocket.accept();
				if(socket != null) {
					if( (ConnectionManager.choked.size()+ConnectionManager.unchoked.size()) <= 10) {
						PeerConnection peer = new PeerConnection(socket, true, fileManager);
						ConnectionManager.choked.put(socket.getInetAddress().getHostAddress(),
								peer);
						new Thread(peer).start();
					}
					socket = null;
				}
			} catch (IOException e) {
				//do nothing
			}
		}
	}
	
	/**
	 * Closes an upload connection.
	 * 
	 * throws IOException if unable to close
	 */
	public void close() {
		stopped = true;
		try {
			serverSocket.close();
		} catch (IOException e) {
			System.out.println("Could not close seeding connection.");
		}
	}
}