import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URLConnection;
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
                    System.out.println("Accepted connection on port " + serverSocket.getLocalPort() + " to peer " + connectionSocket.getInetAddress().toString() + ".");
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

                downloadConnection = new PeerDownloadConnection(toPeer, fileManager, torrentFile, connectionSocket.getInetAddress().toString(), myPeerID);
                uploadConnection = new PeerUploadConnection(toPeer, fileManager, torrentFile, connectionSocket.getInetAddress().toString());

                downloadConnection.start();
                uploadConnection.start();

                byte[] handshakeMessage = SharedFunctions.createHandshake(torrentFile.getInfoHashBytes(), myPeerID);
                toPeer.write(handshakeMessage);
                //new code added by me
                byte[] bitfieldMessage = new byte[1];
                bitfieldMessage[0] = fileManager.getBitfield();
                toPeer.write(bitfieldMessage);


                byte[] messageFromPeer = SharedFunctions.responseFromPeer(fromPeer, handshakeMessage.length+5+bitfieldMessage.length, connectionSocket.getInetAddress().toString());

                ArrayList<byte[]> handshakeAndBitfield = detachMessage(messageFromPeer, 68);
                ArrayList<Integer> indexes = getIndexes(handshakeAndBitfield.get(1), torrentFile.getNumberOfPieces());
                if (!SharedFunctions.verifyInfoHash(handshakeMessage, messageFromPeer)) {
                    System.out.println("Connection to peer " + connectionSocket.getInetAddress().toString() + " invalidated. Connection lost.");
                    closeConnection();
                    continue;
                }

                System.out.println("Validated connection to peer " + connectionSocket.getInetAddress().toString() + ".");
                int messageLength = -1;
                while(connectedToPeer) {
                    try {
                        sleep(200);

                        //get the next message from the peer
                        byte[] message = SharedFunctions.nextPeerMessage(connectionSocket);
                        //get the type of the message from its id
                        String type = SharedFunctions.decodeMessage(message);

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
                        System.out.println("Connection to peer " + connectionSocket.getInetAddress().toString() + " timed out.");
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
    TorrentFile torrentFile;
    FileManager file;
    String peerIP;
    OutputStream toPeer;
    Queue<byte[]> incomingMessageQueue;

    boolean choked;
    boolean interested;

    boolean running;
    byte[] mypeerID;
    ArrayList<Integer> indexes;

    public PeerDownloadConnection(OutputStream toPeer, FileManager file, TorrentFile torrentFile,String peerIP, byte[] mypeerID) {
        this.toPeer = toPeer;
        incomingMessageQueue = new LinkedList<>();
        choked = true;
        interested = false;
        this.peerIP = peerIP;
        this.file = file;
        this.torrentFile = torrentFile;
        this.mypeerID = mypeerID;
    }

    public void run() {
        running = true;
        while(running) {

            try {
                //creates an "interested" message
                byte[] message = SharedFunctions.createMessage(1,(byte)2);
                toPeer.write(message);

                //wait to be unchoked
                while(!incomingMessageQueue.isEmpty()) {

                }
                byte[] unchoke = incomingMessageQueue.poll();
                if (!SharedFunctions.decodeMessage(unchoke).equals("unchoke"))
                    throw new IOException("Peer denied interested message!");
                System.out.println("Connection unchoked from " + peerIP);

                //Contacts tracker that downloading has started
                URLConnection trackerCommunication = Functions.makeURL(torrentFile.getAnnounce(), mypeerID, torrentFile.getInfoHashBytes(), 0, 0, torrentFile.getFileSize(), "started").openConnection();
                trackerCommunication.connect();
                int numberOfPieces = indexes.size();
                for (int i = 0; i < numberOfPieces; i++) {
                    //gets random index number
                    int index = indexes.get(Functions.generateRandomInt(indexes.size() - 1));
                    indexes.remove((Integer) index);
                    if(file.isDownloadable(index)) {
                        int pieceLength;
                        //if the piece at the end of the file
                        if (index == torrentFile.getNumberOfPieces() - 1)
                        //Ex: 151709-32768*(5-1) = 20637 bytes = piece at end of file
                            pieceLength = torrentFile.getFileSize() - torrentFile.getPieceSize() * (torrentFile.getNumberOfPieces() - 1);
                        else
                            pieceLength = torrentFile.getPieceSize();
                        //creates a "request" message
                        int begin = 0;
                        byte[] indexBytes = SharedFunctions.intToByteArray(index);
                        byte[] beginBytes = SharedFunctions.intToByteArray(begin);
                        byte[] lengthBytes = SharedFunctions.intToByteArray(pieceLength);
                        byte[] request = SharedFunctions.concat(indexBytes,beginBytes);
                        request = SharedFunctions.concat(request,lengthBytes);
                        message = SharedFunctions.createMessage(13, (byte)6, request);

                        //read the "piece" message
                        while(!incomingMessageQueue.isEmpty()) {

                        }
                        byte[] piece = incomingMessageQueue.poll();
                        byte[] payload = SharedFunctions.payloadOfMessage(piece);
                        if(!SharedFunctions.decodeMessage(piece).equals("piece")) {
                            throw new IOException("Peer did not send requested piece!");
                        }
                        byte[] receivedIndexBytes = new byte[4];
                        byte[] receivedBeginBytes = new byte[4];
                        byte[] receivedBlockBytes = new byte[pieceLength];
                        System.arraycopy(payload,5,receivedIndexBytes,0,4);
                        System.arraycopy(payload,9,receivedBeginBytes,0,4);
                        System.arraycopy(payload,13,receivedBlockBytes,0,pieceLength);
                        if(SharedFunctions.byteArrayToInt(receivedIndexBytes) != index || SharedFunctions.byteArrayToInt(receivedBeginBytes) != begin) {
                            throw new IOException("Peer's piece data was invalid!");
                        }

                        file.putPieceInFile(index, receivedBlockBytes, torrentFile.getPieceSize());
                        //creates a "have" message
                        toPeer.write(SharedFunctions.createMessage(5,(byte)4,SharedFunctions.intToByteArray(index)));
                        file.insertIntoBitfield(index);
                        System.out.println("Piece " + Integer.toString(index + 1) + ": " +
                                Integer.toString(pieceLength) + " bytes downloaded from " + peerIP);
                    }//end if
                }//end for
                //Contacts tracker that download has completed, Header length = 13 bytes
                trackerCommunication = Functions.makeURL(torrentFile.getAnnounce(), mypeerID, torrentFile.getInfoHashBytes(), 0, torrentFile.getFileSize() + torrentFile.getNumberOfPieces() * 13, 0, "completed").openConnection();
                trackerCommunication.connect();

            }catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

    }

    public synchronized void enqueueMessage(byte[] message) {
        incomingMessageQueue.offer(message);
    }
}

class PeerUploadConnection extends Thread {
    TorrentFile torrentFile;
    FileManager file;
    OutputStream toPeer;
    Queue<byte[]> incomingMessageQueue;

    boolean choking;
    boolean interested;

    boolean running;
    String peerIP;

    public PeerUploadConnection(OutputStream toPeer, FileManager file, TorrentFile torrentFile, String peerIP) {
        this.toPeer = toPeer;
        incomingMessageQueue = new PriorityQueue<byte[]>();
        choking = true;
        interested = false;
        this.file = file;
        this.torrentFile = torrentFile;
        this.peerIP = peerIP;
    }

    public void run() {
        running = true;
        while(running) {
            try {
                if(!incomingMessageQueue.isEmpty()) {
                    byte[] message = incomingMessageQueue.poll();
                    String type = SharedFunctions.decodeMessage(message);
                    switch(type) {
                        case "interested":
                            System.out.println("Got interested message from " + peerIP + ".");
                            interested = true;
                            //unchoke peer immediately
                            choking = false;
                            byte[] unchoke = SharedFunctions.createMessage(1,(byte)1);
                            toPeer.write(unchoke);
                            System.out.println("Sent unchoke message to " + peerIP + ".");
                            break;

                        case "not interested":
                            System.out.println("Got not interested message from " + peerIP + ".");
                            interested = false;
                            break;
                        case "request":
                            System.out.print("Got request message from " + peerIP);
                            if(interested && !choking) {
                                byte[] payloadFromPeer = SharedFunctions.payloadOfMessage(message);
                                byte[] indexBytes = new byte[4];
                                byte[] beginBytes = new byte[4];
                                byte[] lengthBytes = new byte[4];
                                int index = SharedFunctions.byteArrayToInt(indexBytes);
                                int begin = SharedFunctions.byteArrayToInt(beginBytes);
                                int length = SharedFunctions.byteArrayToInt(lengthBytes);
                                int pieceSize = torrentFile.getPieceSize();

                                byte[] data = file.getPieceFromFile(index,begin, pieceSize, length);

                                byte[] payloadToPeer = SharedFunctions.concat(indexBytes,beginBytes);
                                payloadToPeer = SharedFunctions.concat(payloadToPeer,data);
                                byte[] pieceMessage = SharedFunctions.createMessage(9+length,(byte)7,payloadToPeer);

                                toPeer.write(pieceMessage);
                                System.out.println(" and sent starting at piece " + index + ".");
                            }
                            System.out.println(" but did not send any data.");
                            break;
                        default:
                            System.out.println("Warning: invalid message in upload connection to "+ peerIP + ": " + message);
                            break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

        }

    }

    public synchronized void enqueueMessage(byte[] message) {
        incomingMessageQueue.offer(message);
    }
}