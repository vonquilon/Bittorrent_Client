import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class Upload implements Runnable{

	private volatile boolean stopped = false;
	private ServerSocket serverSocket;
	private Socket socket;
	private FileManager fileManager;
	
	public Upload(FileManager fileManager) {
		this.fileManager = fileManager;
		try {
			serverSocket = new ServerSocket(ClientInfo.port);
		} catch (IOException e) {
			System.out.println("Seeding failed!");
		}
	}
	
	@Override
	public void run() {
		while(!stopped) {
			try {
				serverSocket.setSoTimeout(1000);
				while((socket = serverSocket.accept()) != null) {
					if( (ConnectionManager.choked.size()+ConnectionManager.unchoked.size()) <= 10) {
						PeerConnection peer = new PeerConnection(socket, true, fileManager);
						ConnectionManager.choked.put(socket.getInetAddress().getHostAddress(),
								peer);
						new Thread(peer).start();
					}
				}
			} catch (SocketException e) {
				//do nothing
			} catch (IOException e) {
				System.out.println("Seeding failed!");
			}
		}
	}
}