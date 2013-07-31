import java.io.*;
import java.util.ArrayList;

public class FileManager implements Serializable {

    transient RandomAccessFile file;
	transient ArrayList<Integer> downloading;
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

    private void serialize(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeLong(file.getFilePointer());
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

    public synchronized byte[] getPieceFromFile(int index, int begin, int pieceSize, int length) throws IOException {
        byte[] piece = new byte[length];
        file.seek(pieceSize*index);
        file.read(piece,begin,length);
        return piece;
    }

    public synchronized void loadFile(String filename) {

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

    public synchronized boolean doneDownloading(int numberOfPieces) {
        for (int i = 0; i < bitfield.length && i < numberOfPieces; i++) {
            char aBitfield = bitfield[i];
            if (aBitfield == '0') {
                return false;
            }
        }
        return true;

    }


    public synchronized void completedDownloading(Integer index) {
        downloading.remove(index);
    }
}