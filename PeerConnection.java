import sun.security.util.BigInt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.*;

class PeerConnection extends Thread {
    String connectionSocketIP;
    FileManager fileManager;

    TorrentFile torrentFile;
    byte[] myPeerID;

    Socket connectionSocket = null;
    ServerSocket serverSocket = null;
    int serverSocketPort;

    PeerDownloadConnection downloadConnection = null;
    PeerUploadConnection uploadConnection = null;

    List<PeerConnection> allConnections;

    boolean connectedToPeer;

    boolean active;

    char[] peerBitfield;

    /**
     * Initializes this connection as a socket already connected to a peer
     * @param socketToPeer socket connected to a peer
     * @param allConnections list of all connections
     * @param torrentFile the torrent file, read into memory and parsed
     * @param myPeerID the generated peer ID that we go by
     * @param fileManager the class that manages our downloaded file
     */
    public PeerConnection(Socket socketToPeer, List<PeerConnection> allConnections, TorrentFile torrentFile, byte[] myPeerID, FileManager fileManager) {
        connectionSocket = socketToPeer;
        this.allConnections = allConnections;
        connectedToPeer = true;
        this.torrentFile = torrentFile;
        this.fileManager = fileManager;
        this.myPeerID = myPeerID;
    }

    /**
     * Initializes this connection as a server socket waiting for a peer
     * @param socket an unbound server socket
     * @param allConnections list of all connections
     * @param torrentFile the torrent file, read into memory and parsed
     * @param myPeerID the generated peer ID that we go by
     * @param fileManager the class that manages our downloaded file
     */
    public PeerConnection(ServerSocket socket, List<PeerConnection> allConnections, TorrentFile torrentFile, byte[] myPeerID, FileManager fileManager) {
        serverSocket = socket;
        this.allConnections = allConnections;
        connectedToPeer = false;
        this.torrentFile = torrentFile;
        this.fileManager = fileManager;
        this.myPeerID = myPeerID;
    }

    /**
     * Starts this connection
     */
    public void run() {
        try {
            active = true;
            while (active) {
                if (connectionSocket == null && serverSocket != null) {
                    connectionSocket = serverSocket.accept();
                    System.out.println("Accepted connection on port " + serverSocket.getLocalPort() + " to peer " + connectionSocketIP + ".");
                    connectedToPeer = true;
                    serverSocketPort = serverSocket.getLocalPort();
                } else if (serverSocket == null && connectionSocket == null) {
                    //both are null when the connection is no longer active
                    break;
                }

                connectionSocketIP = connectionSocket.getInetAddress().toString().substring(1);
                //3 minute timeout
                connectionSocket.setSoTimeout(60 * 3000);

                InputStream fromPeer = connectionSocket.getInputStream();
                OutputStream toPeer = connectionSocket.getOutputStream();

                downloadConnection = new PeerDownloadConnection(toPeer, fileManager, torrentFile, connectionSocketIP, myPeerID);
                uploadConnection = new PeerUploadConnection(toPeer, fileManager, torrentFile, connectionSocketIP);


                byte[] handshakeMessage = SharedFunctions.createHandshake(torrentFile.getInfoHashBytes(), myPeerID);
                toPeer.write(handshakeMessage);


                int bitfieldLength = (fileManager.bitfield.length + 7) / 8;
                byte[] messageFromPeer = SharedFunctions.responseFromPeer(fromPeer, handshakeMessage.length + 5 + bitfieldLength, connectionSocketIP);

                ArrayList<byte[]> handshakeAndBitfield = detachMessage(messageFromPeer, 68);
                ArrayList<Integer> indexes = getIndexes(handshakeAndBitfield.get(1), torrentFile.getNumberOfPieces());
                if (!SharedFunctions.verifyInfoHash(handshakeMessage, messageFromPeer)) {
                    System.out.println("Connection to peer " + connectionSocketIP + " invalidated. Connection lost.");
                    closeConnection();
                    continue;
                }

                //new code added by me
                //byte[] bitfieldMessage = new byte[5+bitfieldLength];
                //byte[] payload = new byte[bitfieldLength];
                //payload[0] = fileManager.getBitfield();
                //bitfieldMessage = SharedFunctions.createMessage(bitfieldLength+5,(byte)5, payload);
                //toPeer.write(bitfieldMessage);

                System.out.println("Validated connection to peer " + connectionSocketIP + ".");
                int messageLength = -1;

                downloadConnection.setIndexes(indexes);
                downloadConnection.start();
                uploadConnection.start();

                while (connectedToPeer) {
                    try {
                        sleep(200);

                        //get the next message from the peer
                        byte[] message = SharedFunctions.nextPeerMessage(connectionSocket, connectionSocket.getInputStream());
                        //get the type of the message from its id
                        String type = SharedFunctions.decodeMessage(message);

                        //if the id is keep-alive, then skip this message since the socket's 3 minute timeout has already been reset
                        if (type.equals("keep-alive")) {
                            continue;
                        }

                        switch (type) {
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
                        System.out.println("Connection to peer " + connectionSocketIP + " timed out.");
                        connectedToPeer = false;
                        if (serverSocket == null) {
                            active = false;
                        }
                    }
                }
            }
        }
        catch(SocketException e){
            if(serverSocket != null) {
                System.out.println("Server socket closed.");
            }
        }
        catch (IOException e) {
            if(e.toString().contains("Stream closed.")) {
                System.out.println("Socket to peer " + connectionSocketIP + " is closed.");
            }
            else {
                e.printStackTrace();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        } finally {
            allConnections.remove(this);
            try {
                if(downloadConnection != null) {
                    downloadConnection.stopDownloading();
                    downloadConnection.join();
                }
                if(uploadConnection != null) {
                    uploadConnection.stopUploading();
                    uploadConnection.join();
                }
                if (connectionSocket != null && !connectionSocket.isClosed()) {
                    connectionSocket.close();
                }
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            connectionSocket = null;
        }
    }


    public synchronized void close() {
        active = false;
        connectedToPeer = false;
        if(serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        if(connectionSocket != null) {
            try {
                connectionSocket.close();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

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

        if (SharedFunctions.decodeMessage(bitfieldMessage).equals("bitfield")) {
            ArrayList<Integer> indexes = new ArrayList<Integer>(numberOfPieces);
            String bitfield = Integer.toBinaryString(bitfieldMessage[5] & 0xFF);
            for (int i = 0; i < numberOfPieces; i++) {
                if (bitfield.charAt(i) == '1')
                    indexes.add(i);
            }
            return indexes;
        } else
            throw new IOException("Invalid bitfield message!");

    }

    private boolean validateHandshake(byte[] handshakeMessageReceived) {
        if (handshakeMessageReceived.length == 68) {
            byte[] protocolMessage = new byte[19];
            System.arraycopy(handshakeMessageReceived, 1, protocolMessage, 0, 19);
            byte[] sha1Hash = new byte[20];
            System.arraycopy(handshakeMessageReceived, 28, sha1Hash, 0, 20);
            /*
            byte[] peerID = new byte[20];
            System.arraycopy(handshakeMessageReceived,48,sha1Hash,0,20);
            */
            return handshakeMessageReceived[0] == 19 && Arrays.equals(protocolMessage, "BitTorrent protocol".getBytes()) && Arrays.equals(sha1Hash, torrentFile.getInfoHashBytes());
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
        if (serverSocket != null) {
            //connection was provided by the server socket, so just close it so that we can re-accept
            serverSocket.close();
        } else {
            //connection was started from the socket, close the socket and remove this connection from the list of connections
            allConnections.remove(this);
        }
    }

    /**
     * Gets the next partial (without a length field) message from the peer
     *
     * @param fromPeer The stream to read from
     * @return next partial message
     * @throws java.io.IOException if read failure
     */
    private byte[] getNextPartialMessage(InputStream fromPeer) throws IOException {
        byte[] lengthBytes = new byte[4];
        while (fromPeer.available() < lengthBytes.length) {
            try {
                sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        fromPeer.read(lengthBytes);
        int length = SharedFunctions.lengthOfMessage(lengthBytes);
        byte[] message = new byte[length];
        while (fromPeer.available() < message.length) {
            try {
                sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        fromPeer.read(message);
        return message;
    }

    public String getIPAddress() {
        if(connectionSocket == null) {
            return null;
        }
        return connectionSocketIP;
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

    public PeerDownloadConnection(OutputStream toPeer, FileManager file, TorrentFile torrentFile, String peerIP, byte[] mypeerID) {
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

        try {
            //creates an "interested" message
            //byte[] message = SharedFunctions.createMessage(1,(byte)2);
            byte[] message = SharedFunctions.createMessage(1, 2, -1, -1, -1, 5);

            toPeer.write(message);

            //wait to be unchoked
            while (incomingMessageQueue.isEmpty()) {

            }
            byte[] unchoke = incomingMessageQueue.poll();
            if (!SharedFunctions.decodeMessage(unchoke).equals("unchoke"))
                throw new IOException("Peer denied interested message!");
            System.out.println("Connection unchoked from " + peerIP + ".");

            //Contacts tracker that downloading has started
            URLConnection trackerCommunication = Functions.makeURL(torrentFile.getAnnounce(), mypeerID, torrentFile.getInfoHashBytes(), 0, 0, torrentFile.getFileSize(), "started").openConnection();
            trackerCommunication.connect();
            while (running) {
                //gets random index number
                int index = file.getRandomDownloadableIndex(torrentFile.getNumberOfPieces());
                if (index == -1) {
                    System.out.println("piece is downloading or finished, so I'm cutting my connection with " + peerIP + ".");
                    break;
                }

                if (file.isDownloadable(index)) {
                    int pieceLength;
                    //if the piece at the end of the file
                    if (index == torrentFile.getNumberOfPieces() - 1)
                        //Ex: 151709-32768*(5-1) = 20637 bytes = piece at end of file
                        pieceLength = torrentFile.getFileSize() - torrentFile.getPieceSize() * (torrentFile.getNumberOfPieces() - 1);
                    else
                        pieceLength = torrentFile.getPieceSize();
                    //creates a "request" message
                    int begin = 0;
                    //byte[] indexBytes = SharedFunctions.intToByteArray(index);
                    //byte[] beginBytes = SharedFunctions.intToByteArray(begin);
                    //byte[] lengthBytes = SharedFunctions.intToByteArray(pieceLength);
                    //byte[] request = SharedFunctions.concat(indexBytes,beginBytes);
                    //request = SharedFunctions.concat(request,lengthBytes);
                    //message = SharedFunctions.createMessage(13, (byte)6, request);
                    //3rd -> index = 0
                    message = SharedFunctions.createMessage(13, 6, index, 0, pieceLength, 17);
                    toPeer.write(message);
                    file.startDownloading(index);
                    System.out.println("Sent a request message for piece " + Integer.toString(index + 1) + ".");

                    //read the "piece" message
                    while (incomingMessageQueue.isEmpty()) {

                    }
                    byte[] piece = incomingMessageQueue.poll();
                    if (SharedFunctions.decodeMessage(piece).equals("choke")) {
                        System.out.println("Peer choked us, so we couldn't download. Cutting the connection.");
                        break;
                    };
                    ArrayList<byte[]> detached = detachMessage(piece, 13);
                    //System.out.println("debug " + SharedFunctions.decodeMessage(piece));
                    //if(!SharedFunctions.decodeMessage(piece).equals("piece")) {
                    //    throw new IOException("Peer did not send requested piece!");
                    //}
                    for(int i = 0; i < pieceLength; i++) {

                    }
                    byte[] receivedBlockBytes = detached.get(1);
                    if(!verifyPieceHash(receivedBlockBytes, index, torrentFile.getPieceHashes()) || !( SharedFunctions.decodeMessage(detached.get(0)).equals("piece") )) {
                        System.out.println("Hashes didn't match up. Cutting the connection.");
                        break;
                    }


                    System.out.println("Piece " + Integer.toString(index + 1) + ": " + Integer.toString(pieceLength) + " bytes downloaded from " + peerIP);

                    //System.arraycopy(payload,5,receivedIndexBytes,0,4);
                    //System.arraycopy(payload,9,receivedBeginBytes,0,4);
                    //System.arraycopy(payload,13,receivedBlockBytes,0,pieceLength);
                    //if(SharedFunctions.byteArrayToInt(receivedIndexBytes) != index || SharedFunctions.byteArrayToInt(receivedBeginBytes) != begin) {
                    //    throw new IOException("Peer's piece data was invalid!");
                    //}

                    file.putPieceInFile(index, receivedBlockBytes, torrentFile.getPieceSize());
                    //creates a "have" message
                    //toPeer.write(SharedFunctions.createMessage(5,(byte)4,SharedFunctions.intToByteArray(index)));
                    toPeer.write(SharedFunctions.createMessage(5, 4, index, -1, -1, 9));
                    file.insertIntoBitfield(index);
                    file.completedDownloading(index);
                    //Contacts tracker that download has completed, Header length = 13 bytes
                    trackerCommunication = Functions.makeURL(torrentFile.getAnnounce(), mypeerID, torrentFile.getInfoHashBytes(), 0, torrentFile.getFileSize() + torrentFile.getNumberOfPieces() * 13, 0, "completed").openConnection();
                    trackerCommunication.connect();
                }//end if
            }
        } catch (IOException e) {
            System.out.println("IO in downloading from peer "+ peerIP + " was interrupted.");
        }

    }

    /**
     * Private helper method that verifies the downloaded piece hash
     * with one of the hashes given in the torrent file.
     *
     * @param piece
     * @param index       Used for locating the corresponding piece in the
     *                    ArrayList of pieceHashes
     * @param pieceHashes
     * @return boolean true if verified, false if not
     */
    private static boolean verifyPieceHash(byte[] piece, int index, ArrayList<byte[]> pieceHashes) {

        byte[] pieceHash = Functions.encodeToSHA1(piece);
        if (Arrays.equals(pieceHash, pieceHashes.get(index)))
            return true;
        else
            return false;

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

    public synchronized void enqueueMessage(byte[] message) {
        incomingMessageQueue.offer(message);
    }

    public void setIndexes(ArrayList<Integer> indexes) {
        this.indexes = indexes;
    }

    public synchronized void stopDownloading() {
        running = false;
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
        long timeOfLastMessage = System.currentTimeMillis();
        boolean gotFirstMessage = false;
        while (running) {
            try {
                if (!incomingMessageQueue.isEmpty()) {
                    timeOfLastMessage = System.currentTimeMillis();
                    byte[] message = incomingMessageQueue.poll();
                    gotFirstMessage = true;
                    String type = SharedFunctions.decodeMessage(message);
                    switch (type) {
                        case "interested":
                            System.out.println("Got interested message from " + peerIP + ".");
                            interested = true;
                            //unchoke peer immediately
                            choking = false;
                            byte[] unchoke = new byte[]{0,0,0,1,1};
                            toPeer.write(unchoke);
                            System.out.println("Sent unchoke message to " + peerIP + ".");
                            break;

                        case "not interested":
                            System.out.println("Got not interested message from " + peerIP + ".");
                            interested = false;
                            break;
                        case "request":
                            System.out.print("Got request message from " + peerIP);
                            if (interested && !choking) {
                                byte[] payloadFromPeer = SharedFunctions.payloadOfMessage(message);
                                byte[] indexBytes = new byte[4];
                                byte[] beginBytes = new byte[4];
                                byte[] lengthBytes = new byte[4];
                                int index = SharedFunctions.byteArrayToInt(indexBytes);
                                int begin = SharedFunctions.byteArrayToInt(beginBytes);
                                int length = SharedFunctions.byteArrayToInt(lengthBytes);
                                int pieceSize = torrentFile.getPieceSize();

                                byte[] data = file.getPieceFromFile(index, begin, pieceSize, length);

                                byte[] payloadToPeer = SharedFunctions.concat(indexBytes, beginBytes);
                                payloadToPeer = SharedFunctions.concat(payloadToPeer, data);
                                byte[] pieceMessage = SharedFunctions.createMessage(9 + length, (byte) 7, payloadToPeer);

                                toPeer.write(pieceMessage);
                                System.out.println(" and sent starting at piece " + index + ".");
                            }
                            System.out.println(" but did not send any data.");
                            break;
                        default:
                            System.out.println("Warning: invalid message in upload connection to " + peerIP + ": " + message);
                            break;
                    }
                }
                else if(gotFirstMessage){
                    if(System.currentTimeMillis() - timeOfLastMessage > 60*3000) {
                        System.out.println("We went over our timeout period, so we're shutting off the connection to peer " + peerIP + ".");
                        break;
                    }
                }
            } catch (IOException e) {
                System.out.println("IO in uploading to peer " + peerIP + " was interrupted.");
            }

        }

    }

    public synchronized void enqueueMessage(byte[] message) {
        incomingMessageQueue.offer(message);
    }

    public synchronized void stopUploading() {
        running = false;
    }

}