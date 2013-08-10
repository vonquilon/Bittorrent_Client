import java.io.IOException;
import java.io.OutputStream;

public class Output implements Runnable{

	private volatile boolean stopped = false;
	private PeerConnection connection;
	private OutputStream out;
	
	public Output(PeerConnection connection, OutputStream out) {
		this.connection = connection;
		this.out = out;
	}

	@Override
	public void run() {
		while(!stopped) {
			if(connection.outputQueue.size() > 0) {
				try {
					out.write(connection.outputQueue.remove(0).array());
				} catch (IOException e) {
					System.out.println("Could not send data to " + connection.IPAddress);
				}
			}
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				//do nothing
			}
		}
	}
}
