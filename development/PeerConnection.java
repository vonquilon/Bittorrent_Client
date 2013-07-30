package development;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

class PeerConnection extends Thread {
    FileManager fileManager;

    TorrentFile torrentFile;
    byte[] myPeerID;

    Socket connectionSocket = null;
    ServerSocket serverSocket = null;

    PeerDownloadConnection downloadConnection = null;
    PeerUploadConnection uploadConnection = null;

    List<PeerConnection> allConnections;

    boolean connectedToPeer;

    //initializes this connection as a socket already connected to a peer
    public PeerConnection(Socket socketToPeer, List<PeerConnection> allConnections, TorrentFile torrentFile, byte[] myPeerID, FileManager fileManager) {
        connectionSocket = socketToPeer;
        this.allConnections = allConnections;
        connectedToPeer = true;
        this.torrentFile = torrentFile;
        this.fileManager = fileManager;
    }
    //initializes this connecton as a server socket waiting for a peer to connect to it
    public PeerConnection(ServerSocket socket, List<PeerConnection> allConnections, TorrentFile torrentFile, byte[] myPeerID, FileManager fileManager) {
        serverSocket = socket;
        this.allConnections = allConnections;
        connectedToPeer = true;;
        this.torrentFile = torrentFile;
        this.fileManager = fileManager;
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
                byte[] handshakeMessageToSend = SharedFunctions.createHandshake(torrentFile.getInfoHashBytes(), myPeerID);
                toPeer.write(handshakeMessageToSend);
                //send bitfield to peer
                byte[] byteBitfield = SharedFunctions.compressBitfield(fileManager.bitfield);
                //length = 1 + ceil(number of pieces/8)
                int messageLength = 1 + (fileManager.bitfield.length + 7) / 8;
                byte[] bitfieldMessageToSend = SharedFunctions.createMessage(messageLength, (byte)5, byteBitfield);
                toPeer.write(bitfieldMessageToSend);
                //wait for handshake from peer
                byte[] handshakeMessageReceived = getNextMessage(fromPeer);
                //validate peer's handshake

                //wait for bitfield from peer
                byte[] bitfieldMessageReceived = getNextMessage(fromPeer);

                //validate peer's bitfield



                //read in the first 4 bytes from the input stream

                //based on that first 4 bytes (the length field) get the rest of the message

                //get the type of the message from its id

                //if the id is keep-alive, then skip this message

                //elif the id belongs to download (choke, unchoke, piece, have) then insert it into downloadConnection.incomingMessageQueue

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
            connectionSocket = null;
        }


    }

    private byte[] getNextMessage(InputStream fromPeer) throws IOException {
        byte[] lengthBytes = new byte[4];
        fromPeer.read(lengthBytes);
        int length = SharedFunctions.lengthOfMessage(lengthBytes);
        byte[] message = new byte[length];
        fromPeer.read(message);
        return message;
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
        incomingMessageQueue = new LinkedList<>();
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