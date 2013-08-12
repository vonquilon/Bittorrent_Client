import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class FileManager {

	private RandomAccessFile file;
	
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
	
	public synchronized byte[] getFromFile(int index, int begin, int length, int pieceSize) throws IOException {
		file.seek(index*pieceSize+begin);
		byte[] result = new byte[length];
		int readSize = file.read(result);
		if(readSize == length)
			return result;
		else
			throw new IOException();
	}
	
	public synchronized void putInFile(int index, int begin, byte[] block, int pieceSize) throws IOException {
		file.seek(index*pieceSize+begin);
		file.write(block);
	}
}