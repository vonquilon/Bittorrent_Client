
public class FileManager {

	byte[] file;
	
	public FileManager(int size) {
		
		file = new byte[size];
		
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