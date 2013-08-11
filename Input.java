import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class Input implements Runnable{
	
	private volatile boolean stopped = false;
	private PeerConnection connection;
	private InputStream in;
	private final Timer TIMER = new Timer();
	private TimerTask task = null;
	private long delay;
	
	public Input(PeerConnection connection, InputStream in) {
		this.connection = connection;
		this.in = in;
		if(connection.isUpload)
			delay = 2500;
		else
			delay = 2000;
	}

	@Override
	public void run() {
		try {
			if(Message.verifyHandshake(handshakeProcess())) {
				scheduleTask();
				while(!stopped) {
					byte[] messageLength = new byte[4];
					read(messageLength);
					byte[] message = new byte[ByteBuffer.wrap(messageLength).getInt()];
					read(message);
					String messageType = Message.parseMessage(ByteBuffer.wrap(message));
					switch(messageType) {
						case "keep-alive": break;
						case "choke": break;
						case "unchoke": break;
						case "interested": break;
						case "not interested": break;
						case "have": break;
						case "request": break;
						case "piece": break;
						case "cancel": break;
						case "port": break;
					}
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						//do nothing
					}
				}//end while
			}//end if
		} catch (IOException e) {
			System.out.println("Could not get data from " + connection.IPAddress);
		}
		connection.close();
	}
	
	private void read(byte[] buffer) throws IOException {
		for(int size = 0; size != buffer.length;)
			size += in.read(buffer, size, buffer.length-size);
	}
	
	private void processBitfield(BitSet bitfield) {
		Set<Integer> pieces = ConnectionManager.pieces.keySet();
		Iterator<Integer> iterator = pieces.iterator();
		while(iterator.hasNext()) {
			int piece = iterator.next();
			if(bitfield.get(piece)) {
				ConnectionManager.pieces.get(piece).peers.add(connection.IPAddress);
				ConnectionManager.pieces.get(piece).occurrences++;
			}
		}
		connection.done = true;
	}
	
	private ByteBuffer handshakeProcess() throws IOException {
		byte[] handshake = new byte[68];
		read(handshake);
		if(in.available() > 0) {
			byte[] bitfieldLength = new byte[4];
			byte[] bitfieldMessage;
			read(bitfieldLength);
			bitfieldMessage = new byte[ByteBuffer.wrap(bitfieldLength).getInt()];
			read(bitfieldMessage);
			ByteBuffer bitfieldBuffer = ByteBuffer.allocate(bitfieldLength.length+bitfieldMessage.length);
			bitfieldBuffer.put(bitfieldLength); bitfieldBuffer.put(bitfieldMessage);
			ByteBuffer bitfield = ByteBuffer.allocate(bitfieldMessage.length-1);
			bitfield.put(bitfieldMessage, 1, bitfieldMessage.length-1);
			bitfield.clear();
			if(Message.parseMessage(bitfieldBuffer).equals("bitfield"))
				processBitfield(BitSet.valueOf(bitfield));
		}
		return ByteBuffer.wrap(handshake);
	}
	
	private void scheduleTask() {
		if(connection.isUpload) {
			task = new TimerTask() {
				public void run() {
					connection.close();
				}
			};
		} else {
			task = new TimerTask() {
				public void run() {
					connection.outputQueue.add(Message.createKeepAlive());
				}
			};
		}
		TIMER.schedule(task, delay, delay);
	}
	
	private void resetTimer() {
		task.cancel(); TIMER.purge();
		scheduleTask();
	}
	
	private void cancelTimer() {
		if(task != null)
			task.cancel();
		TIMER.cancel();
	}
	
	public void close() throws IOException {
		stopped = true; in.close(); cancelTimer();
	}
}