package old;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class FileManager {

	RandomAccessFile file;
	ArrayList<Integer> downloading;
	char[] bitfield = {'0', '0', '0', '0', '0', '0', '0', '0'};
	
	public FileManager(int size, int numberOfPieces, String fileName) throws IOException {
		
		try {
			file = new RandomAccessFile(fileName, "rw");
		} catch (FileNotFoundException e) {
			file = new RandomAccessFile(new File(fileName), "rw");
		}
		file.setLength(size);
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
	
	public synchronized byte getBitfield() {
		
		String bitfieldString = String.valueOf(bitfield);
		return (byte) Integer.parseInt(bitfieldString, 2);
		
	}
	
	/**
     * Method that assembles the piece into the file.
     *
     * @param index     When multiplied by pieceSize, indicates the location
     *                  of the piece in the file
     * @param piece     Piece to be assembled into file
     * @param pieceSize
	 * @throws java.io.IOException
     */
	public synchronized void putPieceInFile(int index, byte[] piece, int pieceSize) throws IOException {

		//pieceSize*index = byte offset in file
		file.seek(pieceSize*index);
    	file.write(piece);

    }
}