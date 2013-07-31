import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * A class representing the file that is written to and read from in the process of downloading from the peers
 * @authors Von Kenneth Quilon & Alex Loh
 * @date 07/31/2013
 * @version 1.0
 */
public class FileManager{

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

    public synchronized byte[] getPieceFromFile(int index, int begin, int pieceSize, int length) throws IOException {
        byte[] piece = new byte[length];
        file.seek(pieceSize*index);
        file.read(piece,begin,length);
        return piece;
    }

    public synchronized void startDownloading(int index) {
        downloading.add(index);
    }

    public synchronized int getRandomDownloadableIndex(int numberOfPieces) {
        ArrayList<Integer> allIndices = new ArrayList<>();
        for(int i = 0; i < bitfield.length && i < numberOfPieces; i++) {
            if(bitfield[i] == '0' && !downloading.contains(i)) {
                allIndices.add(i);
            }
        }
        if(allIndices.size() == 0) {
            return -1;
        }
        return allIndices.get(Functions.generateRandomInt(allIndices.size() - 1));
    }

    public synchronized void close() throws IOException {
        file.close();
    }

    /**
     * Tells whether the file is completely done downloading or not
     * @param numberOfPieces
     * @return
     */
    public synchronized boolean isDoneDownloading(int numberOfPieces) {
        for (int i = 0; i < bitfield.length && i < numberOfPieces; i++) {
            char aBitfield = bitfield[i];
            if (aBitfield == '0') {
                return false;
            }
        }
        return true;

    }

    /**
     * Tell the file that you've completed downloading this particular piece
     * @param index the index of the piece
     */
    public synchronized void completedDownloading(Integer index) {
        downloading.remove(index);
    }

    public void loadBitfield(String filename) throws IOException {
        InputStream inputStream = new FileInputStream(filename);
        char[] newBitfield = new char[bitfield.length];
        int size = inputStream.available();
        for(int i = 0; i < size; i++) {
            newBitfield[i] = (char)inputStream.read();
        }
        inputStream.close();
        bitfield = newBitfield;
    }

    public void writeBitfield(String filename) throws IOException {
        byte[] bytes = new byte[bitfield.length];
        for(int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) bitfield[i];
        }
        OutputStream outputStream = new FileOutputStream(filename);
        for(int i = 0; i < bytes.length; i++) {
            outputStream.write(bytes[i]);
        }
        outputStream.close();

    }
}
