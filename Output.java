import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;


public class Output extends Thread {
	
	private OutputStream toPeer;
	private boolean stopped  = false;
	private static ArrayList<byte[]> outputQueue;
	
	public Output(OutputStream toPeer) {
		this.toPeer = toPeer;
		outputQueue = new ArrayList<byte[]>();
	}
	
	public void run() {
		while(!stopped) {
			if(outputQueueSize() != 0) {
				try {
					toPeer.write(getFromOutputQueue());
				} catch (IOException e) {
					System.out.println("Unable to send");
				}
			}//end if
		}//end while
	}
	
	public static synchronized void addToOutputQueue(byte[] output) {
    	outputQueue.add(output);
    }
    
    private static synchronized byte[] getFromOutputQueue() {
    	return outputQueue.remove(0);
    }
    
    private static synchronized int outputQueueSize() {
    	return outputQueue.size();
    }
	
	public void halt() {
		stopped = true;
	}
}