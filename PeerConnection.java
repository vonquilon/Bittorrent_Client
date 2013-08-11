import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 8/2/13
 * Time: 4:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class PeerConnection extends Thread{
    boolean hosting;
    final List<PeerConnection> activeConnections;
    final HashMap<String, PeerConnection> addressToConnection;
    final Queue<PeerAction> queuedActions;

    String peerIPAddress;
    int peerPort;
    int hostPort;

    boolean hostChokingPeer = true;
    boolean peerChokingHost = true;
    boolean hostInterestedPeer = false;
    boolean peerInterestedHost = false;

    TorrentInfo torrentInfo;

    boolean closed;
    boolean paused;


    boolean[] peerBitfield;

    DownloadedFile file;

    public PeerConnection(String peerIPAddress, int peerPort, List<PeerConnection> activeConnections, HashMap<String, PeerConnection> addressToConnection, TorrentInfo torrentInfo, DownloadedFile file) {
        hostPort = 0;

        this.peerPort = peerPort;
        this.peerIPAddress = peerIPAddress;
        this.activeConnections = activeConnections;
        this.addressToConnection = addressToConnection;

        hosting = false;
        paused = false;
        queuedActions = new LinkedList<PeerAction>();
        this.torrentInfo = torrentInfo;
        peerBitfield = new boolean[(torrentInfo.file_length+torrentInfo.piece_length-1)/torrentInfo.piece_length];
        this.file = file;
    }

    public PeerConnection(int hostPort, List<PeerConnection> activeConnections, HashMap<String, PeerConnection> addressToConnection, TorrentInfo torrentInfo, DownloadedFile file) {
        this.hostPort = hostPort;

        this.peerPort = 0;
        this.peerIPAddress = "";
        this.activeConnections = activeConnections;
        this.addressToConnection = addressToConnection;

        hosting = true;
        paused = false;
        queuedActions = new LinkedList<PeerAction>();
        this.torrentInfo = torrentInfo;
        peerBitfield = new boolean[(torrentInfo.file_length+torrentInfo.piece_length-1)/torrentInfo.piece_length];
        this.file = file;
    }



    public void run() {
        activeConnections.add(this);
        addressToConnection.put(peerIPAddress,this);

        SocketChannel socketChannel = connectToPeer();
        if (socketChannel == null) {
            removeConnection();
            return;
        }
        //handshake with peer
        try {
            if(!handshakeWithPeer(socketChannel, torrentInfo.info_hash.array(), ClientInfo.PEER_ID, null, file.bitfield)) {
                System.out.println("Handshake with peer " + peerIPAddress + " failed.");
                removeConnection();
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            removeConnection();
            return;
        }

        //successful handshake, now we can continue

        DataReader reader = new DataReader();
        DataWriter writer = new DataWriter();

        boolean readInLength = false;
        int messageLength;
        byte[] messageData = null;

        reader.setBufferLength(4);
        while (!closed) {
            if(paused) {
                System.out.println("Pausing connection to " + peerIPAddress + ".");
                while(paused) {

                }
            }

            //code for acting upon an action the manager or another peer has told us to do
            PeerAction receivedAction;
            synchronized (queuedActions) {
                receivedAction = queuedActions.poll();
            }
            if(receivedAction != null) {
                try {
                    switch (receivedAction.code) {
                        case CHOKEPEER:
                            byte[] length = intToBytes(1);
                            byte[] idBytes = new byte[]{0};
                            byte[] chokeMessage = concat(length,idBytes);
                            writer.putBytes(chokeMessage);
                            hostChokingPeer = true;
                            break;
                        case UNCHOKEPEER:
                            length = intToBytes(1);
                            idBytes = new byte[]{1};
                            byte[] unchokeMessage = concat(length, idBytes);
                            writer.putBytes(unchokeMessage);
                            hostChokingPeer = false;
                            break;
                        case REQUESTPIECE:
                            length = intToBytes(13);
                            idBytes = new byte[]{6};
                            int index = receivedAction.argument;
                            int begin = 0;
                            int pieceLength = torrentInfo.piece_length;
                            byte[] indexBytes = intToBytes(index);
                            byte[] beginBytes = intToBytes(begin);
                            byte[] pieceLengthBytes = intToBytes(pieceLength);
                            byte[] requestMessage = concat(length,idBytes,indexBytes,beginBytes,pieceLengthBytes);
                            writer.putBytes(requestMessage);
                            break;
                        case BROADCASTHAVE:
                            index = receivedAction.argument;
                            length = intToBytes(5);
                            idBytes = new byte[]{};
                            indexBytes = intToBytes(index);
                            byte[] haveMessage = concat(length, idBytes, indexBytes);
                            writer.putBytes(haveMessage);
                            break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            //parse the read data from the peer and acting upon it
            messageData = parseIncomingData(writer, messageData);

            //code for reading data in from the peer
            if (!readInLength) {
                //if we haven't read in the length bytes already, then read the length (first 4 bytes)
                try {
                    if (reader.read(socketChannel)) {
                        messageLength = reader.getInt();
                        readInLength = true;
                        reader.setBufferLength(messageLength);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                //if we've read in the length bytes already, then read the rest of the message
                try {
                    if (reader.read(socketChannel)) {
                        messageData = reader.getBytes();
                        readInLength = false;
                        reader.setBufferLength(4);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            //code for writing data to the peer
            try {
                writer.write(socketChannel);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //finalize closing
        removeConnection();
    }

    private byte[] parseIncomingData(DataWriter writer, byte[] messageData) {
        if (messageData != null) {

            byte[] messageIDBytes = subarray(messageData,0,1);
            byte messageID = messageIDBytes[0];
            byte[] payload = subarray(messageData,1,messageData.length);
            switch (messageID) {
                case 0://choke
                    peerChokingHost = true;
                    break;
                case 1://unchoke
                    peerChokingHost = false;
                    break;
                case 2://interested
                    peerInterestedHost = true;
                    break;
                case 3://uninterested
                    peerInterestedHost = false;
                    break;
                case 4://have
                    int index = bytesToInt(payload);
                    if(index >= peerBitfield.length) {
                        //invalid data, close the connection
                        closed = true;
                    }
                    else {
                        peerBitfield[index] = true;
                    }
                    break;
                case 5://bitfield
                    peerBitfield = bytesToBools(payload, file.numberOfPieces);
                    break;
                case 6://request
                    if(!hostChokingPeer && peerInterestedHost) {
                        //break up the payload
                        byte[] indexBytes = subarray(payload,0,4);
                        byte[] beginBytes = subarray(payload,4,8);
                        byte[] lengthBytes = subarray(payload,8,12);

                        index = bytesToInt(indexBytes);
                        int begin = bytesToInt(beginBytes);
                        int length = bytesToInt(lengthBytes);

                        //if requested length > 2^15, then close the connection
                        if(length > Math.pow(2,15)) {
                            System.out.println("Requested piece size from " + peerIPAddress + " was too large: " + length + ". Closing connection.");
                            closed = true;
                        }

                        //get the requested data
                        byte[] data = new byte[length];
                        try {
                            data = file.readBytes(length, index, begin);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        //send piece message with requested data
                        byte[] lengthPrefixBytes = intToBytes(9+length);
                        byte[] idBytes = new byte[]{7};
                        try {
                            byte[] pieceMessage = concat(lengthPrefixBytes,idBytes,indexBytes,beginBytes,data);
                            writer.putBytes(pieceMessage);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    break;
                case 7://piece
                    //break up the payload
                    byte[] indexBytes = subarray(payload, 0, 4);
                    byte[] beginBytes = subarray(payload, 4, 8);
                    byte[] blockBytes = subarray(payload, 8, payload.length);

                    index = bytesToInt(indexBytes);
                    int begin = bytesToInt(beginBytes);

                    //write data to file
                    try {
                        file.writeBytes(blockBytes, index, begin);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //tell all peers to broadcast have message
                    synchronized (activeConnections) {
                        for(PeerConnection peerConnection : activeConnections) {
                            peerConnection.addAction(new PeerAction(PeerActionCode.BROADCASTHAVE,index));
                        }
                    }
                    file.downloading[index] = false;
                    file.bitfield[index] = true;
                    break;
            }
            messageData = null;
        }
        return messageData;
    }

    private boolean[] bytesToBools(byte[] payload, int numberOfPieces) {
        boolean[] bools = new boolean[numberOfPieces];
        for(int i = 0; i < payload.length*8 && i < numberOfPieces; i++) {
            int value = payload[i/8] & 1 << (7-i);
            bools[i] = value != 0;
        }
        return bools;
    }

    private void removeConnection() {
        activeConnections.remove(this);
        addressToConnection.remove(peerIPAddress);
    }

    private boolean handshakeWithPeer(SocketChannel socketChannel, byte[] sha1Hash, byte[] myPeerID, byte[] peerPeerID, boolean[] myBitfield) throws IOException {
        byte[] byte19 = new byte[]{19};
        byte[] bitTorrentHeader = "BitTorrent Protocol".getBytes();
        byte[] reservedBytes = new byte[8];
        Arrays.fill(reservedBytes, (byte) 0);
        byte[] handshakeMessage = concat(byte19, bitTorrentHeader, reservedBytes, sha1Hash, myPeerID);

        int bitfieldLength = (myBitfield.length + 7) / 8 + 1;
        byte[] bitfieldLengthBytes = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(bitfieldLength).array();
        byte[] bitfieldID = new byte[]{5};
        byte[] bitfieldBytes = boolsToBytes(myBitfield);
        byte[] bitfieldMessage = concat(bitfieldLengthBytes, bitfieldID, bitfieldBytes);

        ByteBuffer buffer;

        if (hosting) {
            //wait for handshake, then send out our handshake and bitfield

            //wait for handshake
            buffer = ByteBuffer.allocate(68);
            while (!closed && buffer.hasRemaining()) {
                socketChannel.read(buffer);
            }
            buffer.flip();
            byte[] handshakeFromPeer = new byte[68];
            buffer.put(handshakeFromPeer);
            if (!verifyHandshake(handshakeFromPeer, sha1Hash, peerPeerID)) {
                return false;
            }

            //send out handshake
            buffer.clear();
            buffer.put(handshakeMessage);
            buffer.flip();
            while (!closed && buffer.hasRemaining()) {
                socketChannel.write(buffer);
            }

            //send out bitfield
            buffer = ByteBuffer.allocate(bitfieldLength + 4);
            buffer.clear();
            buffer.put(bitfieldMessage);
            buffer.flip();
            while (!closed && buffer.hasRemaining()) {
                socketChannel.write(buffer);
            }

        } else {
            //send out our handshake, then wait for handshake, then send bitfield

            //send out handshake
            buffer = ByteBuffer.allocate(68);
            buffer.put(handshakeMessage);
            buffer.flip();
            while (!closed && buffer.hasRemaining()) {
                socketChannel.write(buffer);
            }

            //wait for handshake
            buffer.clear();
            while (!closed && buffer.hasRemaining()) {
                socketChannel.read(buffer);
            }
            buffer.flip();
            byte[] handshakeFromPeer = new byte[68];
            buffer.put(handshakeFromPeer);
            if (!verifyHandshake(handshakeFromPeer, sha1Hash, peerPeerID)) {
                return false;
            }

            //send out bitfield
            buffer = ByteBuffer.allocate(bitfieldLength + 4);
            buffer.clear();
            buffer.put(bitfieldMessage);
            buffer.flip();
            while (!closed && buffer.hasRemaining()) {
                socketChannel.write(buffer);
            }
        }
        return true;
    }

    private byte[] boolsToBytes(boolean[] myBitfield) {
        byte[] bytes = new byte[(myBitfield.length+7)/8];
        for(int i = 0; i < myBitfield.length; i++) {
            bytes[i/8] |= 1 << (7-i%8);
        }
        return bytes;
    }

    private boolean verifyHandshake(byte[] handshakeFromPeer, byte[] sha1Hash, byte[] peerPeerID) {
        if (handshakeFromPeer.length != 68) {
            return false;
        }
        for (int i = 0; i < 20; i++) {
            if (handshakeFromPeer[i + 28] != sha1Hash[i]) {
                return false;
            }
        }
        if (peerPeerID == null) {
            return true;
        }
        for (int i = 0; i < 20; i++) {
            if (peerPeerID[i] != handshakeFromPeer[i + 48]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Helper function to connect to a peer, either from a server socket or from a socket.
     *
     * @return a SocketChannel object if able to connect, otherwise null
     */
    private SocketChannel connectToPeer() {
        ServerSocketChannel serverSocketChannel;
        SocketChannel socketChannel = null;
        if (hosting) {

            //bind a new server socket to the port that this connection uses
            try {
                serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.configureBlocking(false);
                serverSocketChannel.socket().bind(new InetSocketAddress(hostPort));
            } catch (IOException e) {
                System.err.println("Warning: unable to create server socket for port " + hostPort + ".");
                return socketChannel;
            }

            //accept a new connection until we close this connection or we get a remote connection
            try {
                while (!closed && socketChannel == null) {
                    socketChannel = serverSocketChannel.accept();
                }
                if (socketChannel != null) {
                    socketChannel.configureBlocking(false);
                    peerIPAddress = socketChannel.getRemoteAddress().toString();

                }
            } catch (IOException e) {
                System.err.println("Warning: unable to connect server socket on port " + hostPort + " to a peer.");
            }

        } else {
            try {
                //connect this socket channel to the address 
                socketChannel = SocketChannel.open();
                socketChannel.configureBlocking(false);
                while (!closed && !socketChannel.finishConnect()) {
                    socketChannel.connect(new InetSocketAddress(peerIPAddress, peerPort));
                }
            } catch (IOException e) {
                System.err.println("Warning: unable to open or connect socket channel to " + peerIPAddress + ":" + peerPort + ".");
            }
        }
        return socketChannel;
    }

    /**
     * Helper function to concatenate a list of byte arrays
     *
     * @param args list of byte arrays to concatenate
     * @return the concatenated byte arrays
     * @throws IOException
     */
    private byte[] concat(byte[]... args) throws IOException {
        ByteArrayOutputStream builder = new ByteArrayOutputStream();
        for (byte[] arg : args) {
            builder.write(arg);
        }
        return builder.toByteArray();
    }

    private byte[] subarray(byte[] original, int from, int to) {
        byte[] newArray = new byte[to - from];
        System.arraycopy(original, from, newArray, from - from, to - from);
        return newArray;
    }

    private int bytesToInt(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.BIG_ENDIAN);
        return buffer.getInt();
    }

    private byte[] intToBytes(int value) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(value);
        return buffer.array();
    }

    public synchronized void addAction(PeerAction action) {
        queuedActions.offer(action);
    }

    public boolean canRequest() {
        return hostInterestedPeer && !peerChokingHost;
    }
}


class DataReader {
    private ByteBuffer data;

    public void setBufferLength(int length) {
        data = ByteBuffer.allocate(length).order(ByteOrder.BIG_ENDIAN);
    }

    /**
     * Method to read data from a socket channel
     *
     * @param socketChannel socket connected to a peer
     * @return true if able to read all of the allocated length's worth of byte data or false if not
     * @throws IOException
     */
    public boolean read(SocketChannel socketChannel) throws IOException {
        socketChannel.read(data);
        if (data.hasRemaining()) {
            return false;
        }
        data.flip();
        return true;
    }

    public byte[] getBytes() {
        byte[] readData = new byte[data.position()];
        data.get(readData);
        return readData;
    }

    public int getInt() {
        return data.getInt();
    }
}

class DataWriter {
    Queue<byte[]> messageQueue;
    private ByteBuffer data;
    boolean finishedWriting = true;

    DataWriter() {
        messageQueue = new LinkedList<byte[]>();
    }

    public void putBytes(byte[] bytes) {
        messageQueue.add(bytes);
    }

    /**
     * method that writes inputted data into a socket channel
     * @param socketChannel the socket channel to write to
     * @return true if able to input all data, false if still in progress
     * @throws IOException
     */
    public boolean write(SocketChannel socketChannel) throws IOException {
        if(finishedWriting) {
            if(messageQueue.isEmpty()) {
                return true;
            }
            byte[] currentMessage = messageQueue.poll();
            data = ByteBuffer.wrap(currentMessage);
        }
        socketChannel.write(data);
        if(data.hasRemaining()) {
            finishedWriting = false;
            return false;
        }
        if(!messageQueue.isEmpty()) {
            finishedWriting = true;
            return false;
        }
        finishedWriting = true;
        return true;
    }
}
