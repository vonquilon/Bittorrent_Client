import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Created with IntelliJ IDEA.
 * User: al
 * Date: 7/22/13
 * Time: 6:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class PeerUploadConnection extends Thread{

    ServerSocket connectionSocket;
    boolean active;
    int port;
    byte[] peerID;
    FileManager file;
    TorrentFile torrentInfo;
    ArrayList<Integer> indexes;

    public PeerUploadConnection(int serverPort, TorrentFile torrentInfo, byte[] peerID, FileManager file, ArrayList<Integer> indexes) throws IOException {
        this.port = serverPort;
        this.file = file;
        this.torrentInfo = torrentInfo;
        this.peerID = peerID;
        this.indexes = indexes;
        active = true;
    }
    @Override
    public void run() {
        try{
            connectionSocket = new ServerSocket(port);
            while(active) {

            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.

        }
    }

    public static void sendMessage()

    /**
     * Stops the thread of execution, although perhaps not immediately, and frees all resources
     */
    public void close() {
        active = false;
    }
}