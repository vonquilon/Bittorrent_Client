package old;

import java.io.InputStream;


public class Input extends Thread {

	private InputStream fromPeer;
	
	public Input(InputStream fromPeer) {
		this.fromPeer = fromPeer;
	}
	
	public void run() {
		
	}
}