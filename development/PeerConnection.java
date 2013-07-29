package development;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

class PeerConnection extends Thread {

    Socket connectionSocket = null;
    ServerSocket serverSocket = null;

    PeerDownloadConnection downloadConnection = null;
    PeerUploadConnection uploadConnection = null;

    List<PeerConnection> allConnections;

    boolean connectedToPeer;

    //initializes this connection as a socket already connected to a peer
    public PeerConnection(Socket socketToPeer, List<PeerConnection> allConnections) {
        connectionSocket = socketToPeer;
        this.allConnections = allConnections;
        connectedToPeer = true;
    }
    //initializes this connecton as a server socket waiting for a peer to connect to it
    public PeerConnection(ServerSocket socket, List<PeerConnection> allConnections) {
        serverSocket = socket;
        this.allConnections = allConnections;
        connectedToPeer = true;
    }

    public void run() {
        try{
            while(connectedToPeer) {
                boolean peerInitiated = false;
                if(connectionSocket == null && serverSocket != null) {
                    connectionSocket = serverSocket.accept();
                    peerInitiated = true;
                }
                //3 minute timeout
                connectionSocket.setSoTimeout(60 * 3000);

                InputStream fromPeer = connectionSocket.getInputStream();
                OutputStream toPeer = connectionSocket.getOutputStream();

                downloadConnection = new PeerDownloadConnection(toPeer);
                uploadConnection = new PeerUploadConnection(toPeer);

                downloadConnection.run();
                uploadConnection.run();

                //send handshake to peer

                //send bitfield to peer

                //wait for handshake from peer

                //validate peer's handshake

                //wait for bitfield from peer

                //validate peer's bitfield



                //read in the first 4 bytes from the input stream

                //based on that first 4 bytes (the length field) get the rest of the message

                //get the type of the message from its id

                //if the id is keep-alive, then skip this message

                //elif the id belongs to download (choke, unchoke, piece) then insert it into downloadConnection.incomingMessageQueue

                //elif the id belongs to upload (interested, uninterested, request) then insert it into uploadConnection.incomingMessageQueue

                //else the message is unidentified; forget about it
            }
        }
        catch(Exception e) {
            e.printStackTrace(); //To change body of catch statement use File | Settings | File Templates.
        }
        finally {
            downloadConnection.running = false;
            uploadConnection.running = false;
            try {
                downloadConnection.join();
                uploadConnection.join();
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }


    }
}

class PeerDownloadConnection extends Thread {
    OutputStream toPeer;
    Queue<byte[]> incomingMessageQueue;

    boolean choked;
    boolean interested;

    boolean running;

    public PeerDownloadConnection(OutputStream toPeer) {
        this.toPeer = toPeer;
        incomingMessageQueue = new PriorityQueue<byte[]>();
        choked = true;
        interested = false;
    }

    public void run() {
        running = true;
        while(running) {

        }

    }
}

class PeerUploadConnection extends Thread {
    OutputStream toPeer;
    Queue<byte[]> incomingMessageQueue;

    boolean choking;
    boolean interested;

    boolean running;

    public PeerUploadConnection(OutputStream toPeer) {
        this.toPeer = toPeer;
        incomingMessageQueue = new PriorityQueue<byte[]>();
        choking = true;
        interested = false;
    }

    public void run() {
        running = true;
        while(running) {

        }

    }

}