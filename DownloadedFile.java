import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 8/10/13
 * Time: 10:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class DownloadedFile {
    RandomAccessFile file;
    boolean[] downloading;
    boolean[] bitfield;
    int fileSize;
    int pieceSize;
    int numberOfPieces;


    public DownloadedFile(int fileSize, int pieceSize, String filename) {
        this.fileSize = fileSize;
        this.pieceSize = pieceSize;
        numberOfPieces = (fileSize+pieceSize-1)/pieceSize;
        File baseFile = new File(filename);
        try {
            file = new RandomAccessFile(baseFile, "rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        downloading = new boolean[numberOfPieces];
        Arrays.fill(downloading, false);
        bitfield = new boolean[numberOfPieces];
        Arrays.fill(downloading,false);
    }

    public synchronized void writeBytes(byte[] bytes, int pieceIndex, int begin) throws IOException {
        int position = pieceIndex*pieceSize+begin;
        file.seek(position);
        file.write(bytes);
    }

    public synchronized byte[] readBytes(int length, int pieceIndex, int begin) throws IOException {
        int position = pieceIndex*pieceSize+begin;
        byte[] bytes = new byte[length];
        file.read(bytes,position,length);
        return bytes;
    }

    public synchronized void setDownloading(int index, boolean value) {
        downloading[index] = value;
    }

    public synchronized void setBitfield(int index, boolean value) {
        bitfield[index] = value;
    }

    public synchronized void close() throws IOException {
        file.close();
    }
}
