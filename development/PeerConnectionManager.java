package development;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 7/28/13
 * Time: 2:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class PeerConnectionManager {
    List<PeerConnection> activeConnections;

    TorrentFile torrentFile;
    byte[] peerID;
    FileManager file;

    /**
     * Constructor for the manager, which handles all coordination of the connections
     * @param peers list of all peers
     * @param torrentFile the file
     * @param peerID
     * @param fileName
     */
    public PeerConnectionManager(int lowServerSocketPortRange, int highServerSocketPortRange, ArrayList<String> peers, TorrentFile torrentFile, byte[] peerID, String fileName) throws IOException {
        serverSockets = new ArrayList<>();
        for(int i = lowServerSocketPortRange; i <= highServerSocketPortRange; i++) {
            try {
                serverSockets.add(new ServerSocket(i));
            } catch (IOException e) {
                System.out.println("Warning: unable to create socket on port " + i + '.');
            }
        }
        activeConnections = new ArrayList<>();
        FileManager fileManager = new FileManager(torrentFile.getFileSize(), torrentFile.getNumberOfPieces(), fileName);
    }

    public void startDownloading() {

    }
}