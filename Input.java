import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Set;

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
			if(Message.verifyHandshake(handshakeProcess())) {
				System.out.println("Connected to: " + connection.IPAddress);
				connection.scheduleTask();
				while(!stopped) {
					byte[] messageLength = new byte[4];
					read(messageLength);
					byte[] message = null;
					int size;
					if(in.available() > 0) {
						message = new byte[ByteBuffer.wrap(messageLength).getInt()];
						read(message);
						size = messageLength.length+message.length;
					} else
						size = messageLength.length;
					ByteBuffer fullMessage = ByteBuffer.allocate(size);
					fullMessage.put(messageLength);
					if(message != null)
						fullMessage.put(message);
					String messageType = Message.parseMessage((ByteBuffer) fullMessage.clear());
					try{
						if(messageType != null)
							processMessage(messageType, message);
					} catch(IOException e) {
						System.out.println("Hard disk error!");
					}
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						//do nothing
					}
				}//end while
			}//end if
		} catch (IOException e) {
			if(ClientInfo.left != 0)
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
		try {
			while(iterator.hasNext()) {
				int piece = iterator.next();
				if(bitfield.get(bitfield.length()-1-piece)) {
					ConnectionManager.pieces.get(piece).peers.add(connection.IPAddress);
					ConnectionManager.pieces.get(piece).occurrences++;
				}
			}
		} catch(IndexOutOfBoundsException e) {
			//do nothing
		}
		connection.done = true;
	}
	
	private BitSet arrayToBitSet(byte[] bytes) {
		BitSet bits = new BitSet();
	    for (int i=0; i<bytes.length*8; i++) {
	        if ((bytes[bytes.length-i/8-1]&(1<<(i%8))) > 0) {
	            bits.set(i);
	        }
	    }
	    return bits;
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
			bitfield.put(bitfieldMessage, 1, bitfieldMessage.length-1).clear();
			byte[] bytes = bitfield.array();
			if(Message.parseMessage(bitfieldBuffer).equals("bitfield"))
				processBitfield(arrayToBitSet(bytes));
		} else
			connection.done = true;
		return ByteBuffer.wrap(handshake);
	}
	
	private void processMessage(String messageType, byte[] message) throws IOException {
		switch(messageType) {
			case "keep-alive": 	if(connection.isUpload)
									connection.resetTimer();
								break;
			case "choke": 	if(connection.isUpload)
								connection.resetTimer();
							choke();
							break;
			case "unchoke": if(connection.isUpload)
								connection.resetTimer();
							unchoke();
							break;
			case "interested": 	if(connection.isUpload)
									connection.resetTimer();
								connection.outputQueue.add(Message.createUnchoke());
								break;
			case "not interested": 	if(connection.isUpload)
										connection.resetTimer();
									connection.outputQueue.add(Message.createChoke());
									break;
			case "have": 	if(connection.isUpload)
								connection.resetTimer();
							have(message);
							break;
			case "request": if(connection.isUpload)
								connection.resetTimer();
							request(message);
							break;
			case "piece": 	if(connection.isUpload)
								connection.resetTimer();
							piece(message);
							ConnectionManager.downloading.remove(connection.IPAddress);
							break;
			case "cancel": break;
			case "port": break;
			default: break;
		}
	}
	
	private void choke() {
		if(ConnectionManager.unchoked.containsKey(connection.IPAddress))
			ConnectionManager.choked.put(connection.IPAddress, 
					ConnectionManager.unchoked.remove(connection.IPAddress));
	}
	
	private void unchoke() {
		if(ConnectionManager.choked.containsKey(connection.IPAddress))
			ConnectionManager.unchoked.put(connection.IPAddress, 
					ConnectionManager.choked.remove(connection.IPAddress));
	}
	
	private void have(byte[] message) {
		ByteBuffer indexBuffer = ByteBuffer.allocate(message.length-1);
		indexBuffer.put(message, 1, message.length-1).clear();
		Integer pieceIndex = indexBuffer.getInt();
		if(!ConnectionManager.donePieces.contains(pieceIndex)) {
			Piece piece = ConnectionManager.pieces.get(pieceIndex);
			if(!piece.peers.contains(connection.IPAddress)) {
				piece.peers.add(connection.IPAddress);
				piece.occurrences++;
			}
		}
		ConnectionManager.sort();
	}
	
	private void request(byte[] message) throws IOException {
		ByteBuffer requestBuffer = ByteBuffer.allocate(message.length-1);
		requestBuffer.put(message, 1, message.length-1).clear();
		try{
			int index = requestBuffer.getInt();
			int begin = requestBuffer.getInt();
			int length = requestBuffer.getInt();
			byte[] block = connection.fileManager.getFromFile
					(index, begin, length, ConnectionManager.torrentInfo.piece_length);
			connection.outputQueue.add(Message.createPiece(index, begin, block));
			ClientInfo.uploaded += block.length;
		} catch(BufferUnderflowException e) {
			//do nothing
		}
	}
	
	private void piece(byte[] message) throws IOException {
		ByteBuffer pieceBuffer = ByteBuffer.allocate(message.length-1);
		pieceBuffer.put(message, 1, message.length-1).clear();
		try{
			int index = pieceBuffer.getInt();
			int begin = pieceBuffer.getInt();
			byte[] block = new byte[pieceBuffer.remaining()];
			pieceBuffer.get(block);
			if(verifyPiece(block, ConnectionManager.torrentInfo.piece_hashes[index])) {
				connection.fileManager.putInFile(index, begin, block,
						ConnectionManager.torrentInfo.piece_length);
				connection.outputQueue.add(Message.createHave(index));
				ConnectionManager.donePieces.add((Integer) index);
				ConnectionManager.pieces.remove((Integer) index);
				ConnectionManager.downloading.remove(connection.IPAddress);
				ClientInfo.downloaded += block.length; ClientInfo.left -= block.length;
				System.out.println("Downloaded piece " + (index+1) +" from " + connection.IPAddress);
			}
		} catch(BufferUnderflowException | NoSuchAlgorithmException e) {
			//do nothing
		}
	}
	
	private boolean verifyPiece(byte[] piece1, ByteBuffer encodedPiece2) throws NoSuchAlgorithmException {
		MessageDigest encoder = MessageDigest.getInstance("SHA-1");
		ByteBuffer encodedPiece1 = ByteBuffer.wrap(encoder.digest(piece1));
		if(encodedPiece1.equals(encodedPiece2))
			return true;
		else
			return false;
	}
	
	public void close() throws IOException {
		stopped = true; in.close();
	}
}