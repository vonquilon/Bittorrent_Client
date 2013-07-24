
public class FileManager {

	byte[] file;
	
	public FileManager(int size) {
		
		file = new byte[size];
		
	}
	
	public byte[] getFile() {
		
		return file;
		
	}
	
	public synchronized void putPieceInFile(int index, byte[] piece, int pieceSize) {

		//pieceSize*index = byte offset in file
    	System.arraycopy(piece, 0, file, pieceSize * index, piece.length);

    }
}
