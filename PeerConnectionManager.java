import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 8/2/13
 * Time: 4:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class PeerConnectionManager extends Thread{
    ArrayList<String> peerAddresses;

    ArrayList<PeerConnection> activeConnections;
    HashMap<String, PeerConnection> addressToConnection;
    TorrentInfo info;

    boolean closed;
    boolean paused;
    public void run() {
        while(!closed) {
            if(paused) {
                System.out.println("Pausing peer manager.");
                while(paused) {

                }
            }

            connectToAllPeers();

        }
    }

    /**
     * Connects to all the peers provided in the peerlist by the tracker. Ensures that each peer is only connected to once.
     */
    private void connectToAllPeers() {
        for(String address : peerAddresses) {
            String[] splittedAddress = address.split(":");
            String ipAddress = splittedAddress[0];
            if(!addressToConnection.containsKey(ipAddress)) {
                int port = Integer.parseInt(splittedAddress[1]);
                PeerConnection newConnection = new PeerConnection(ipAddress,port, activeConnections, addressToConnection, info);
                newConnection.start();
            }
        }
    }

    public synchronized void updateConnection() {

    }

    public void closeAllConnections() throws IOException {
    }

}
