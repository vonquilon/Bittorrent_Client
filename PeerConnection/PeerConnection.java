package PeerConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 8/2/13
 * Time: 4:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class PeerConnection {
    Socket socketToPeer;
    ServerSocket serverSocketToPeer;
    PeerDownloadConnection downloadConnection;
    PeerUploadConnection uploadConnection;
    ConnectionData data;
    boolean serverSocketActive;

    public PeerConnection(Socket socketToPeer) {
        this.socketToPeer = socketToPeer;
        this.serverSocketToPeer = null;
        this.downloadConnection = null;
        this.uploadConnection = null;
        this.data = new ConnectionData();
    }

    public PeerConnection() {
        serverSocketActive = serverSocketToPeer != null;
    }


    public void run() {
        do {

        }while(serverSocketActive);
    }

    public void close() throws IOException {
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
