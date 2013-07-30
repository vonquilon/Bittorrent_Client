import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
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
        //this.setDaemon(true);
    }
    //initializes this connecton as a server socket waiting for a peer to connect to it
    public PeerConnection(ServerSocket socket, List<PeerConnection> allConnections, TorrentFile torrentFile, byte[] myPeerID, FileManager fileManager) {
        serverSocket = socket;
        this.allConnections = allConnections;
        connectedToPeer = false;
        this.torrentFile = torrentFile;
        this.fileManager = fileManager;
        this.myPeerID = myPeerID;
        //this.setDaemon(true);
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
                connectionSocket.setSoTimeout(60 * 300);

                InputStream fromPeer = connectionSocket.getInputStream();
                OutputStream toPeer = connectionSocket.getOutputStream();

                downloadConnection = new PeerDownloadConnection(toPeer);
                uploadConnection = new PeerUploadConnection(toPeer);

                downloadConnection.start();
                uploadConnection.start();

                byte[] handshakeMessage = SharedFunctions.createHandshake(torrentFile.getInfoHashBytes(), myPeerID);
                toPeer.write(handshakeMessage);
                //new code added by me
                byte[] bitfield = new String(fileManager.bitfield).getBytes();
                //turns '0' -> 0 (int), '1' -> 1 (int)
                for(int i = 0; i < bitfield.length; i++) {
                    bitfield[i] -= 48;
                }
                byte[] bitfieldMessage = SharedFunctions.createMessage(5+bitfield.length,(byte)5,bitfield);
                toPeer.write(bitfieldMessage);


                byte[] messageFromPeer = SharedFunctions.responseFromPeer(fromPeer, handshakeMessage.length+6, connectionSocket.getInetAddress().toString());
                ArrayList<byte[]> handshakeAndBitfield = detachMessage(messageFromPeer, 68);
                ArrayList<Integer> indexes = getIndexes(handshakeAndBitfield.get(1), torrentFile.getNumberOfPieces());
                if (!SharedFunctions.verifyInfoHash(handshakeMessage, messageFromPeer)) {
                    closeConnection();
                    continue;
                }


                int messageLength = -1;
                while(connectedToPeer) {
                    try {
                        sleep(200);

                        //if length not defined, read in the first 4 bytes from the input stream; if we don't have enough bytes yet, then try again later
                        if(messageLength == -1) {
                            byte[] lengthBytes = new byte[4];
                            fromPeer.read(lengthBytes);
                            messageLength = SharedFunctions.lengthOfMessage(lengthBytes);
                        }
                        //based on the length, get the rest of the message
                        byte[] message = new byte[messageLength];
                        fromPeer.read(message);
                        //get the type of the message from its id
                        String type = SharedFunctions.decodePartialMessage(message);

                        //if the id is keep-alive, then skip this message since the socket's 3 minute timeout has already been reset
                        if(type.equals("keep-alive")) {
                            continue;
                        }

                        switch(type) {
                            //if the id belongs to download (choke, unchoke, piece, have) then insert it into downloadConnection.incomingMessageQueue
                            case "choke":
                            case "unchoke":
                            case "piece":
                            case "have":
                                downloadConnection.enqueueMessage(message);
                                break;
                            //if the id belongs to upload (interested, uninterested, request) then insert it into uploadConnection.incomingMessageQueue
                            case "interested":
                            case "not interested":
                            case "request":
                                uploadConnection.enqueueMessage(message);
                                break;
                            //skip it because this message's type is invalid
                            default:
                                break;
                        }
                    } catch (SocketTimeoutException e) {
                        System.out.println("Socket to peer " + connectionSocket.getInetAddress().toString() + " timed out.");
                        connectedToPeer = false;
                        if(serverSocket == null) {
                            active = false;
                            allConnections.remove(this);
                        }
                    }
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


    /**
     * Private helper method that separates a data that contains two different information.
     *
     * @param attachedMessage The data to be separated
     * @param message1Length  Used for obtaining message2Length:
     *                        message2Length=attachedMessage.length-message1Length
     */
    private static ArrayList<byte[]> detachMessage(byte[] attachedMessage, int message1Length) {

        byte[] message1 = new byte[message1Length];
        int message2Length = attachedMessage.length - message1Length;
        byte[] message2 = new byte[message2Length];
        ArrayList<byte[]> detachedData = new ArrayList<byte[]>(2);

        //copies header data into message byte[]
        System.arraycopy(attachedMessage, 0, message1, 0, message1Length);
        detachedData.add(message1);
        //copies piece data into piece byte[]
        System.arraycopy(attachedMessage, message1Length, message2, 0, message2Length);
        detachedData.add(message2);

        return detachedData;

    }

    private static ArrayList<Integer> getIndexes(byte[] bitfieldMessage, int numberOfPieces) throws IOException {

        if(SharedFunctions.decodeMessage(bitfieldMessage).equals("bitfield")) {
            ArrayList<Integer> indexes = new ArrayList<Integer>(numberOfPieces);
            String bitfield = Integer.toBinaryString(bitfieldMessage[5] & 0xFF);
            for(int i = 0; i < numberOfPieces; i++) {
                if(bitfield.charAt(i) == '1')
                    indexes.add(i);
            }
            return indexes;
        }
        else
            throw new IOException("Invalid bitfield message!");

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
        while(fromPeer.available() < lengthBytes.length) {
            try {
                sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        fromPeer.read(lengthBytes);
        int length = SharedFunctions.lengthOfMessage(lengthBytes);
        byte[] message = new byte[length];
        while(fromPeer.available() < message.length) {
            try {
                sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
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

    public synchronized void enqueueMessage(byte[] message) {
        incomingMessageQueue.offer(message);
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

    public synchronized void enqueueMessage(byte[] message) {
        incomingMessageQueue.offer(message);
    }

}