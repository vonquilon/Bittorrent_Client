import java.io.*;
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
        connectionSocket = new ServerSocket(serverPort);
        this.file = file;
        this.torrentInfo = torrentInfo;
        this.peerID = peerID;
        this.indexes = indexes;
        active = false;
    }
    @Override
    public void run() {
        try{
            active = true;
            while(active) {
                connectionSocket = new ServerSocket(port);
                Socket socket = connectionSocket.accept();
                boolean choking = true;
                boolean peerInterested = false;
                InputStream fromPeer = socket.getInputStream();
                OutputStream toPeer = socket.getOutputStream();

            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.

        }
    }


    /**
     * Stops the thread of execution, although perhaps not immediately, and frees all resources
     */
    public void close() {
        active = false;
    }
}