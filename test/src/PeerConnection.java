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

    boolean active;

    char[] peerBitfield;

    //initializes this connection as a socket already connected to a peer
    public PeerConnection(Socket socketToPeer, List<PeerConnection> allConnections, TorrentFile torrentFile, byte[] myPeerID, FileManager fileManager) {
        connectionSocket = socketToPeer;
        this.allConnections = allConnections;
        connectedToPeer = true;
        this.torrentFile = torrentFile;
        this.fileManager = fileManager;
        this.myPeerID = myPeerID;
    }
    //initializes this connecton as a server socket waiting for a peer to connect to it
    public PeerConnection(ServerSocket socket, List<PeerConnection> allConnections, TorrentFile torrentFile, byte[] myPeerID, FileManager fileManager) {
        serverSocket = socket;
        this.allConnections = allConnections;
        connectedToPeer = false;
        this.torrentFile = torrentFile;
        this.fileManager = fileManager;
        this.myPeerID = myPeerID;
    }



    public void run() {
        try{
            active = true;
            while(active) {
                if(connectionSocket == null && serverSocket != null) {
                    connectionSocket = serverSocket.accept();
                    connectedToPeer = true;
                }
                else if(serverSocket == null && connectionSocket == null) {
                    //both are null when the connection is no longer active
                    break;
                }

                //3 minute timeout
                connectionSocket.setSoTimeout(60 * 3000);

                InputStream fromPeer = connectionSocket.getInputStream();
                OutputStream toPeer = connectionSocket.getOutputStream();

                downloadConnection = new PeerDownloadConnection(toPeer);
                uploadConnection = new PeerUploadConnection(toPeer);

                downloadConnection.start();
                uploadConnection.start();

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
                byte[] handshakeMessageReceived = getNextPartialMessage(fromPeer);
                //validate peer's handshake
                if(!validateHandshake(handshakeMessageReceived)) {
                    closeConnection();
                    continue;
                }
                //wait for bitfield from peer
                byte[] bitfieldMessageReceived = getNextPartialMessage(fromPeer);
                //validate peer's bitfield
                if(!validateBitfield(bitfieldMessageReceived)) {
                    closeConnection();
                    continue;
                }
                //set peer's bitfield
                byte[] payload = SharedFunctions.payloadOfPartialMessage(bitfieldMessageReceived);
                peerBitfield = SharedFunctions.decompressBitfield(payload);


                while(connectedToPeer) {

                    //read in the first 4 bytes from the input stream

                    //based on that first 4 bytes (the length field) get the rest of the message

                    //get the type of the message from its id

                    //if the id is keep-alive, then skip this message

                    //elif the id belongs to download (choke, unchoke, piece, have) then insert it into downloadConnection.incomingMessageQueue

                    //elif the id belongs to upload (interested, uninterested, request) then insert it into uploadConnection.incomingMessageQueue

                    //else the message is unidentified; forget about it
                }
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
                connectionSocket.close();
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            connectionSocket = null;
        }


    }

    private boolean validateHandshake(byte[] handshakeMessageReceived) {
        if(handshakeMessageReceived.length == 68) {
            byte[] protocolMessage = new byte[19];
            System.arraycopy(handshakeMessageReceived,1,protocolMessage,0,19);
            byte[] sha1Hash = new byte[20];
            System.arraycopy(handshakeMessageReceived,28,sha1Hash,0,20);
            /*
            byte[] peerID = new byte[20];
            System.arraycopy(handshakeMessageReceived,48,sha1Hash,0,20);
            */
            return handshakeMessageReceived[0] == 19 && Arrays.equals(protocolMessage,"BitTorrent protocol".getBytes()) && Arrays.equals(sha1Hash,torrentFile.getInfoHashBytes());
        }
        return false;
    }

    private boolean validateBitfield(byte[] bitfieldMessageReceived) {
        return SharedFunctions.decodePartialMessage(bitfieldMessageReceived).equals("bitfield");
    }

    private void closeConnection() throws IOException {
        //connection was started from the server socket; close the connection
        connectionSocket.close();
        connectionSocket = null;
        if(serverSocket != null) {
            //connection was provided by the server socket, so just close it so that we can re-accept
            serverSocket.close();
        }
        else {
            //connection was started from the socket, close the socket and remove this connection from the list of connections
            allConnections.remove(this);
        }
    }

    /**
     * Gets the next partial (without a length field) message from the peer
     * @param fromPeer The stream to read from
     * @return next partial message
     * @throws java.io.IOException if read failure
     */
    private byte[] getNextPartialMessage(InputStream fromPeer) throws IOException {
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