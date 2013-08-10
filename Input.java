import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.BitSet;


public class Input implements Runnable{
	
	private volatile boolean stopped = false;
	private PeerConnection connection;
	private InputStream in;
	
	public Input(PeerConnection connection, InputStream in) {
		this.connection = connection;
		this.in = in;
	}

	@Override
	public void run() {
		try {
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
					processBitfield(BitSet.valueOf(bitfield), ConnectionManager.pieces.size());
			}
			while(!stopped) {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					//do nothing
				}
			}
		} catch (IOException e) {
			System.out.println("Could not get data from " + connection.IPAddress);
		}
	}
	
	private void read(byte[] buffer) throws IOException {
		for(int size = 0; size != buffer.length;)
			size += in.read(buffer, size, buffer.length-size);
	}
	
	private void processBitfield(BitSet bitfield, int size) {
		for(int i = 0; i < size; i++) {
			if(bitfield.get(i)) {
				ConnectionManager.pieces.get(i).peers.add(connection.IPAddress);
				ConnectionManager.pieces.get(i).occurrences++;
			}
		}
		connection.done = true;
	}
}
