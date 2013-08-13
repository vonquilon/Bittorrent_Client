import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class FileManager {

	private RandomAccessFile file;

    /**
     * Constructor for this file manager
     * @param size the size of the file
     * @param fileName the filename to save the file under
     */
	public FileManager(int size, String fileName) {
		try {
			file = new RandomAccessFile(fileName, "rw");
		} catch (FileNotFoundException e) {
			try {
				file = new RandomAccessFile(new File(fileName), "rw");
			} catch (FileNotFoundException e1) {
				System.out.println("File not found or could not be created on the hard disk.");
			}
		}
		try {
			file.setLength(size);
		} catch (IOException e) {
			System.out.println("Not enough disk space!");
		}
	}

    /**
     * Gets data from a file
     * @param index the index of the piece to start at
     * @param begin the offset within the piece
     * @param length the length of data to get
     * @param pieceSize the maximum size of the pieces in this file
     * @return the data in the file as a byte[]
     * @throws IOException if unable to read from disk
     */
	public synchronized byte[] getFromFile(int index, int begin, int length, int pieceSize) throws IOException {
		file.seek(index*pieceSize+begin);
		byte[] result = new byte[length];
		int readSize = file.read(result);
		if(readSize == length)
			return result;
		else
			throw new IOException();
	}

    /**
     * Puts data in a file
     * @param index the index of the piece to start at
     * @param begin the offset within the piece
     * @param block the data to put in the file
     * @param pieceSize the maximum size of the pieces in this file
     * @throws IOException if unable to read from disk
     */
	public synchronized void putInFile(int index, int begin, byte[] block, int pieceSize) throws IOException {
		file.seek(index*pieceSize+begin);
		file.write(block);
	}
}