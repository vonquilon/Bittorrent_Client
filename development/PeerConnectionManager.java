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
    List<ServerSocket> serverSockets;
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
        for(ServerSocket serverSocket : serverSockets) {
            ServerSocketAcceptThread acceptThread = new ServerSocketAcceptThread(serverSocket,fileManager,peerID,torrentFile,activeConnections);
            acceptThread.run();
        }
    }

    public void startDownloading() {

    }
}

class ServerSocketAcceptThread extends Thread{
    ServerSocket socket;
    PeerConnection newConnection;

    TorrentFile torrentFile;
    byte[] peerID;
    FileManager file;

    ServerSocketAcceptThread(ServerSocket socket, FileManager file, byte[] peerID, TorrentFile torrentFile) {
        this.socket = socket;
        this.file = file;
        this.peerID = peerID;
        this.torrentFile = torrentFile;
        newConnection = null;
    }

    @Override
    public void run() {
        try {
            Socket acceptedSocket = socket.accept();
            newConnection = new PeerConnection(acceptedSocket, torrentFile, peerID, file, true);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
