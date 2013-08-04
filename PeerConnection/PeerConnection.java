package PeerConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 8/2/13
 * Time: 4:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class PeerConnection {
    //all of the active peerconnections; when a connection dies, it's expected to remove itself from this list
    ArrayList<PeerConnection> peerConnections;
    //the socket connected to the peer; is expected to be not null for
    Socket socketToPeer;
    ServerSocket serverSocketToPeer;
    PeerDownloadConnection downloadConnection;
    PeerUploadConnection uploadConnection;
    ConnectionData data;
    boolean serverSocketActive;

    /**
     * constructs this peer connection as a socket that we connected to a peer ourselves
     * @param socketToPeer the socket to the peer
     */
    public PeerConnection(Socket socketToPeer) {
        this.socketToPeer = socketToPeer;
        this.serverSocketToPeer = null;
        this.downloadConnection = null;
        this.uploadConnection = null;
        this.data = new ConnectionData(socketToPeer.getInetAddress().toString());
        serverSocketActive = serverSocketToPeer != null;
    }


    public void run() {
        do {
        }while(serverSocketActive);
        //we're not running anymore, so remove this connection from the list of connections
        peerConnections.remove(this);
    }

    public void closeConnections() throws IOException {
        closeStreams();
        closeSockets();
    }

    private void closeStreams() throws IOException {
        if(downloadConnection != null) {
            downloadConnection.close();
        }
        if(uploadConnection != null) {
            uploadConnection.close();
        }
    }

    private void closeSockets() throws IOException {
        if(serverSocketToPeer != null && !serverSocketToPeer.isClosed()) {
            serverSocketToPeer.close();
        }
        if(socketToPeer != null && !socketToPeer.isClosed()) {
            socketToPeer.close();
        }
    }
}
