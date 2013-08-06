package PeerConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 8/2/13
 * Time: 4:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class PeerConnection {
    ArrayList<PeerConnection> activeConnections;

    String peerIPAddress;
    int peerPort;
    int hostPort;

    boolean closed;


    public PeerConnection(String peerIPAddress, int peerPort, ArrayList<PeerConnection> activeConnections) {
        hostPort = 0;

        this.peerPort = peerPort;
        this.peerIPAddress = peerIPAddress;
        this.activeConnections = activeConnections;
    }

    public PeerConnection(int hostPort, ArrayList<PeerConnection> activeConnections) {
        this.hostPort = hostPort;

        this.activeConnections = activeConnections;
    }

    public void run() {

        while(!closed) {

        }
    }
}
