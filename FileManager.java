import java.util.ArrayList;

public class FileManager {

	byte[] file;
	ArrayList<Integer> downloading;
	char[] bitfield = {'0', '0', '0', '0', '0', '0', '0', '0'};
	
	public FileManager(int size, int numberOfPieces) {
		
		file = new byte[size];
		downloading = new ArrayList<Integer>(numberOfPieces);
		
	}
	
	public synchronized void insertIntoBitfield(int index) {
		
		downloading.remove((Integer) index);
		bitfield[index] = '1';
		
	}
	
	public synchronized boolean isDownloadable(int index) {
		
		if(!downloading.contains(index) && bitfield[index]=='0') {
			downloading.add(index);
			return true;
		}
		else
			return false;
		
	}
	
	public byte[] getFile() {
		
		return file;
		
	}
	
	/**
     * Method that assembles the piece into the file.
     *
     * @param index     When multiplied by pieceSize, indicates the location
     *                  of the piece in the file
     * @param piece     Piece to be assembled into file
     * @param file      Where piece is assembled into
     * @param pieceSize
     */
	public synchronized void putPieceInFile(int index, byte[] piece, int pieceSize) {

		//pieceSize*index = byte offset in file
    	System.arraycopy(piece, 0, file, pieceSize * index, piece.length);

    }
}