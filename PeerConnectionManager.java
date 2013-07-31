import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 7/28/13
 * Time: 2:01 PM
 * To change this template use File | Settings | File Templates.
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

    @Override
    public void run() {
        startDownloading();
    }

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
            assert splitted.length == 2;
            if(splitted[0].equals("128.6.171.3") || splitted[0].equals("128.6.171.4")) {
                validPeers.add(splitted[0]);
                validPeerPorts.add(splitted[1]);

            }
        }
        running = true;
        boolean closedAllYet = false;
        while(running){
            if(file.isDoneDownloading(torrentFile.getNumberOfPieces())) {
                if(!closedAllYet) {
                    System.out.println("File completely downloaded, closing any download connections.");
                    closeAllConnections();
                    closedAllYet = true;
                    running = false;
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
        try {
            file.close();
            file.writeBitfield("bitfield.txt");
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        if(!closedAllYet) {
            closeAllConnections();
        }
        long time = System.currentTimeMillis();
        while(activeConnections.size() > 0) {
             //give all threads 3 seconds to shutdown
             if(System.currentTimeMillis()-time > 3000) {
                for(int i = 0; i < activeConnections.size(); i++) {
                    try {
                        activeConnections.get(i).interrupt();
                    } catch (IndexOutOfBoundsException e) {
                        i--;
                    }
                }
             }
        }
        ready = true;
    }

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

    public synchronized void stopDownloading() {
        running = false;
    }

    public synchronized boolean readyToClose() {
        return ready;
    }
}