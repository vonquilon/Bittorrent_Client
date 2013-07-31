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

    ArrayList<String> peers;
    TorrentFile torrentFile;
    byte[] peerID;
    FileManager file;

    int lowServerSocketPortRange;
    int highServerSocketPortRange;

    /**
     * Constructor for the manager, which handles all coordination of the connections
     * @param peers list of all peers
     * @param torrentFile the file
     * @param peerID
     * @param fileName
     */
    public PeerConnectionManager(int lowServerSocketPortRange, int highServerSocketPortRange, ArrayList<String> peers, TorrentFile torrentFile, byte[] peerID, String fileName) throws IOException {
        this.peers = peers;
        activeConnections = new ArrayList<>();
        this.lowServerSocketPortRange = lowServerSocketPortRange;
        this.highServerSocketPortRange = highServerSocketPortRange;
        this.peerID = peerID;
        this.torrentFile = torrentFile;
        file = new FileManager(torrentFile.getFileSize(), torrentFile.getNumberOfPieces(), fileName);
    }

    public void startDownloading() {
        for(int i = lowServerSocketPortRange; i <= highServerSocketPortRange; i++) {
            try {
                PeerConnection peerConnection = new PeerConnection(new ServerSocket(i), activeConnections, torrentFile, peerID, file);
                activeConnections.add(peerConnection);
                System.out.println("Server socket created on port " + i + ".");
                peerConnection.start();
            } catch (IOException e) {
                System.out.println("Warning: unable to create server socket on port " + i + '.');
            }
        }
        ArrayList<String> validPeers = new ArrayList<>();
        ArrayList<String> validPeerPorts = new ArrayList<>();
        for(String peer : peers) {
            String[] splitted = peer.split(":");
            assert splitted.length == 2;
            if(splitted[0].equals("128.6.171.3") /*|| splitted[0].equals("128.6.171.4")*/) {
                validPeers.add(splitted[0]);
                validPeerPorts.add(splitted[1]);

            }
        }
        while(!file.doneDownloading()){
            if(activeConnections.size() > 1) {
                continue;
            }
            int peerNumber = Functions.generateRandomInt(validPeerPorts.size()-1);
            try {
                //since there are 1 or fewer connections (only 2 connections allowed at a time) try connecting to a peer
                PeerConnection peerConnection = new PeerConnection(new Socket(validPeers.get(peerNumber), Integer.parseInt(validPeerPorts.get(peerNumber))), activeConnections, torrentFile, peerID, file);
                activeConnections.add(peerConnection);
                System.out.println("Connected to peer at " + validPeers.get(peerNumber) + ".");
                peerConnection.start();

            } catch (IOException e) {
                System.out.println("Warning: unable to connect to host " + validPeers.get(peerNumber) + " on port " + validPeerPorts.get(peerNumber) + ".");
            }
        }
        try {
            file.close();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}