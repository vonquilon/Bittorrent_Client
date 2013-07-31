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
    //list of pieces still in progress of downloading
	ArrayList<Integer> downloading;
    //list of pieces that have been fully downloaded. is a '1' if downloaded, '0' if not.
	char[] bitfield = {'0', '0', '0', '0', '0', '0', '0', '0'};

    /**
     * Constructor for this manager
     * @param size the size of the file
     * @param numberOfPieces the number of pieces in this file
     * @param fileName the name of this file
     * @throws IOException when unable to read/write to the disk
     */
	public FileManager(int size, int numberOfPieces, String fileName) throws IOException {
		
		try {
			file = new RandomAccessFile(fileName, "rw");
		} catch (FileNotFoundException e) {
			file = new RandomAccessFile(new File(fileName), "rw");
		}
		file.setLength(size);
		downloading = new ArrayList<Integer>(numberOfPieces);
		
	}

    /**
     * moves this piece from 'downloading' to 'complete'
     * @param index the index of the piece
     */
	public synchronized void insertIntoBitfield(int index) {
		
		downloading.remove((Integer) index);
		bitfield[index] = '1';
		
	}

    /**
     * sees whether a piece can be downloaded or not
     * @param index index of the piece
     * @return whether this piece is not in the progress of downloading or has been downloaded
     */
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
     * @param pieceSize the size of the piece
	 * @throws java.io.IOException if unable to read/write from file
     */
	public synchronized void putPieceInFile(int index, byte[] piece, int pieceSize) throws IOException {

		//pieceSize*index = byte offset in file
		file.seek(pieceSize*index);
    	file.write(piece);

    }

    /**
     * Gets piece data from the file
     * @param index the piece we're going to download from
     * @param begin the offset within the piece; this will give us the exact seek position
     * @param pieceSize the size of the piece
     * @param length the length of the piece
     * @return the piece data requested
     * @throws IOException if unable to read/write from file
     */
    public synchronized byte[] getPieceFromFile(int index, int begin, int pieceSize, int length) throws IOException {
        byte[] piece = new byte[length];
        file.seek(pieceSize*index);
        file.read(piece,begin,length);
        return piece;
    }

    /**
     * sets this piece as currently downloading
     * @param index the piece to set the 'downloading' statuc to
     */
    public synchronized void startDownloading(int index) {
        downloading.add(index);
    }

    /**
     * gets a randomly chosen piece out of the currently existing ones available to download
     * @param numberOfPieces the number of pieces to download
     * @return the index of the chosen piece
     */
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

    /**
     * closes this file, and saves all changes
     * @throws IOException if unable to read/write to disk
     */
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

    /**
     * get the bitfield data from disk, which tells us which pieces we should download and which pieces are already downloaded
     * @param filename the filename of the bitfield
     * @throws IOException if unable to read/write to disk
     */
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

    /**
     * writes the bitfield to disk so that it can be retrieved later
     * @param filename the name of the file to store the bitfield in
     * @throws IOException if unable to read/write to disk
     */
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
