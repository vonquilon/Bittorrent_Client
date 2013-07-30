import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;


public class Input extends Thread {

	private InputStream fromPeer;
	private boolean stopped = false;
	private static ArrayList<byte[]> inputQueue;
	private static int messageLength = 4;
	
	public Input(InputStream fromPeer) {
		this.fromPeer = fromPeer;
		inputQueue = new ArrayList<byte[]>();
	}
	
	public void run() {
		while(!stopped) {
			try {
				int length = fromPeer.available();
				if(length != 0) {
					while((length = fromPeer.available()) < messageLength) {
						byte[] message = new byte[length];
						fromPeer.read(message);
						addToInputQueue(message);
					}
				}
			} catch (IOException e) {
				System.out.println("Unable to send");
			}
			if(inputQueueSize() != 0) {
				
			}
		}
	}
	
	private static synchronized void addToInputQueue(byte[] input) {
    	inputQueue.add(input);
    }
    
    public static synchronized byte[] getFromInputQueue() {
    	return inputQueue.remove(0);
    }
    
    private static synchronized int inputQueueSize() {
    	return inputQueue.size();
    }
    
    public static synchronized void setMessageLength(int length) {
    	messageLength = length;
    }
    
    public void halt() {
    	stopped = true;
    }
}