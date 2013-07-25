import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
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

                byte[] handshake = new byte[68];
                fromPeer.read(handshake);
                byte[] protocol = "BitTorrent protocol".getBytes();
                if(handshake[0] != 19 || !Arrays.equals(Arrays.copyOfRange(handshake,1,protocol.length+1),protocol)) {
                    //first condition: first byte must be equal to 19.
                    //second condition: the next bytes must be the string "BitTorrent protocol" in bytes
                    //if either of the two conditions aren't met, then the data is unexpected and we kill the connection
                    connectionSocket.close();
                    continue;
                }

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