import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class acts as a manager over all the existing peer connections, and ensures that connections are
 * created within working limits and closed properly before exiting. It delegates the main 'download file'
 * responsibility among its peer connections.
 *
 * @authors Von Kenneth Quilon & Alex Loh
 * @date 07/31/2013
 * @version 1.0
 */
public class PeerConnectionManager extends Thread{
    List<PeerConnection> activeConnections;

    ArrayList<String> peers;
    TorrentFile torrentFile;
    byte[] peerID;
    FileManager file;

    int lowServerSocketPortRange;
    int highServerSocketPortRange;
    private boolean running;
    private boolean ready;
    String fileName;

    /**
     * Constructor for the manager, which handles all coordination of the connections
     * @param peers list of all peers
     * @param torrentFile the file
     * @param peerID
     * @param fileName
     */
    public PeerConnectionManager(int lowServerSocketPortRange, int highServerSocketPortRange, ArrayList<String> peers, TorrentFile torrentFile, byte[] peerID, String fileName) throws IOException {
        this.peers = peers;
        activeConnections = Collections.synchronizedList(new ArrayList<PeerConnection>());
        this.lowServerSocketPortRange = lowServerSocketPortRange;
        this.highServerSocketPortRange = highServerSocketPortRange;
        this.peerID = peerID;
        this.torrentFile = torrentFile;
        ready = true;
        this.fileName = fileName;
        file = new FileManager(torrentFile.getFileSize(), torrentFile.getNumberOfPieces(), fileName);
        try {
            file.loadBitfield("bitfield.txt");
            System.out.println("Successfully read bitfield from file.");
        }
        catch(IOException e) {
            System.out.println("Unable to read bitfield from file, so starting fresh.");
        }
    }

    /**
     * start downloading from the peers
     */
    @Override
    public void run() {
        startDownloading();
    }

    /**
     * create PeerConnections from server sockets and sockets to peers, then
     * handles their creations/deletions and releases resources upon the
     * ultimate shutdown of the client
     */
    public void startDownloading() {
        ready = false;

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
            if(splitted[0].equals("128.6.171.3") || splitted[0].equals("128.6.171.4")) {
                validPeers.add(splitted[0]);
                validPeerPorts.add(splitted[1]);

            }
        }
        running = true;
        boolean closedAllDownloadConnectionsYet = false;
        long interval = 60000;
        long initTime = System.currentTimeMillis();
        while(running){
            if(System.currentTimeMillis()-initTime > interval/4) {
                try {
                    URLConnection trackerCommunication = null;
                    trackerCommunication = Functions.makeURL(torrentFile.getAnnounce(), peerID, torrentFile.getInfoHashBytes(), PeerUploadConnection.uploadedBytes, PeerDownloadConnection.downloadedBytes, torrentFile.getFileSize(), "empty").openConnection();
                    trackerCommunication.connect();
                }catch(IOException e) {
                    System.out.println("Warning: Unable to contact tracker.");
                }
            }
            if(file.isDoneDownloading(torrentFile.getNumberOfPieces())) {
                if(!closedAllDownloadConnectionsYet) {
                    System.out.println("File completely downloaded, closing any download connections.");
                    closeAllDownloadConnections();
                    closedAllDownloadConnectionsYet = true;
                }
                continue;
            }
            if(activeConnections.size() > 2) {
                continue;
            }
            int peerNumber = Functions.generateRandomInt(validPeerPorts.size());
            boolean alreadyConnected = false;
            for (int i = 0; i < activeConnections.size(); i++) {
                PeerConnection peerConnection = activeConnections.get(i);
                String ipAddress = peerConnection.getIPAddress();
                if (ipAddress != null && ipAddress.contains(validPeers.get(peerNumber))) {
                    alreadyConnected = true;
                }
            }
            if(!alreadyConnected){
                try {
                    //since there are 2 or fewer connections (only 3 connections allowed at a time) try connecting to a peer


                    PeerConnection peerConnection = new PeerConnection(new Socket(validPeers.get(peerNumber), Integer.parseInt(validPeerPorts.get(peerNumber))), activeConnections, torrentFile, peerID, file);
                    activeConnections.add(peerConnection);
                    System.out.println("Connected to peer at " + validPeers.get(peerNumber) + ".");
                    peerConnection.start();

                } catch (IOException e) {
                    System.out.println("Warning: unable to connect to host " + validPeers.get(peerNumber) + " on port " + validPeerPorts.get(peerNumber) + ".");
                }
            }
        }

        //Contacts tracker that downloading has stopped
        URLConnection trackerCommunication = null;
        try {
            trackerCommunication = Functions.makeURL(torrentFile.getAnnounce(), peerID, torrentFile.getInfoHashBytes(), PeerUploadConnection.uploadedBytes, PeerDownloadConnection.downloadedBytes, torrentFile.getFileSize(), "stopped").openConnection();
            trackerCommunication.connect();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        try {
            file.close();
            file.writeBitfield("bitfield.txt");
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        closeAllConnections();
        long time = System.currentTimeMillis();
        while(activeConnections.size() > 0) {
             if(System.currentTimeMillis()-time > 1000) {
                 closeAllConnections();
                 time = System.currentTimeMillis();
             }
        }
        ready = true;
    }

    /**
     * When called, closes all the connections.
     */
    private synchronized void closeAllConnections() {
        for (int i = 0; i < activeConnections.size(); i++) {
            try {
                PeerConnection peerConnection = activeConnections.get(i);
                peerConnection.close();
            }
            catch(IndexOutOfBoundsException e) {
                //try the same connection again just in case; if it doesn't work this time, it'll break out
                i--;
            }
        }
    }
    /**
     * When called, closes all the download connections.
     */
    private synchronized void closeAllDownloadConnections() {
        for (int i = 0; i < activeConnections.size(); i++) {
            try {
                if(activeConnections.get(i).serverSocket == null) {
                    PeerConnection peerConnection = activeConnections.get(i);
                    peerConnection.close();
                }
            }
            catch(IndexOutOfBoundsException e) {
                //try the same connection again just in case; if it doesn't work this time, it'll break out
                i--;
            }
        }
    }

    /**
     * Tells all of the sockets to stop their connections and clear resources, then exit.
     */
    public synchronized void stopDownloading() {
        running = false;
    }
}