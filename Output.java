import java.io.IOException;
import java.io.OutputStream;

/**
 * Output manages the sending of data to a peer.
 * 
 * @author Von Kenneth Quilon & Alex Loh
 * @date 08/10/2013
 * @version 1.0
 */
public class Output implements Runnable{

	private volatile boolean stopped = false;
	private PeerConnection connection;
	private OutputStream out;

    /**
     * Constructor for this output thread.
     * 
     * @param connection - the connection to the peer
     * @param out - an output stream connected to an active Socket
     */
	public Output(PeerConnection connection, OutputStream out) {
		this.connection = connection;
		this.out = out;
	}

	/**
     * Runs the output thread.
     */
	@Override
	public void run() {
		while(!stopped) {
			if(connection.outputQueue.size() > 0) {
				try {
					out.write(connection.outputQueue.remove(0).array());
					if(!connection.isUpload)
						connection.resetTimer();
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
	
	/**
     * Closes the output stream and exits this thread.
     * 
     * @throws IOException if unable to close the output stream
     */
	public void close() throws IOException {
		stopped = true; out.close();
	}
}