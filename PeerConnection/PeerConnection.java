package PeerConnection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 8/2/13
 * Time: 4:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class PeerConnection {
    boolean hosting;
    ArrayList<PeerConnection> activeConnections;

    String peerIPAddress;
    int peerPort;
    int hostPort;

    boolean hostChokingPeer = true;
    boolean peerChokingHost = true;
    boolean hostInterestedPeer = false;
    boolean peerInterestedHost = false;


    boolean closed;


    public PeerConnection(String peerIPAddress, int peerPort, ArrayList<PeerConnection> activeConnections) {
        hostPort = 0;

        this.peerPort = peerPort;
        this.peerIPAddress = peerIPAddress;
        this.activeConnections = activeConnections;

        hosting = false;
    }

    public PeerConnection(int hostPort, ArrayList<PeerConnection> activeConnections) {
        this.hostPort = hostPort;

        this.peerPort = 0;
        this.peerIPAddress = "";
        this.activeConnections = activeConnections;

        hosting = true;
    }

    public void run() {
        SocketChannel socketChannel = connectToPeer();
        if(socketChannel == null) {
            return;
        }
        //if(handshakeWithPeer(socketChannel))

        DataReader reader = new DataReader();
        DataWriter writer = new DataWriter();

        boolean readInLength = false;
        boolean readInData = false;
        int messageLength = 0;
        int messageID = 0;
        byte[] messagePayload = null;

        reader.setBufferLength(4);
        while(!closed) {
            if(readInLength) {
                //if we've read in the length bytes already, then read the rest of the message
                try {
                    if(reader.read(socketChannel)) {
                        messageID = reader.getInt();
                        messagePayload = reader.getBytes();
                        readInLength = false;
                        readInData = true;
                        readInLength = false;
                        reader.setBufferLength(4);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else {
                //if we haven't, then read the length (first 4 bytes)
                try {
                    if(reader.read(socketChannel)) {
                        messageLength = reader.getInt();
                        readInLength = true;
                        reader.setBufferLength(messageLength);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            switch(messageID) {
                case 0:
                    peerChokingHost = true;
                    break;
                case 1:
                    peerChokingHost = false;
                    break;
                case 2:
                    peerInterestedHost = true;
                    break;
                case 3:
                    peerInterestedHost = false;
                    break;
                case 4:

                    break;
                case 5:

                    break;
                case 6:

                    break;
                case 7:
                    break;
            }
        }

    }

    private boolean handshakeWithPeer(SocketChannel socketChannel, byte[] sha1Hash, byte[] myPeerID, byte[] peerPeerID, BitSet myBitfield) throws IOException {
        byte[] byte19 = new byte[]{19};
        byte[] bitTorrentHeader = "BitTorrent Protocol".getBytes();
        byte[] reservedBytes = new byte[8];
        Arrays.fill(reservedBytes,(byte)0);
        byte[] handshakeMessage = concat(byte19,bitTorrentHeader,reservedBytes,sha1Hash,myPeerID);

        int bitfieldLength = (myBitfield.length()+7)/8 + 1;
        byte[] bitfieldLengthBytes = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(bitfieldLength).array();
        byte[] bitfieldID = new byte[]{5};
        byte[] bitfieldBytes = myBitfield.toByteArray();
        byte[] bitfieldMessage = concat(bitfieldLengthBytes,bitfieldID,bitfieldBytes);

        ByteBuffer buffer;

        if(hosting) {
            //wait for handshake, then send out our handshake and bitfield

            //wait for handshake
            buffer = ByteBuffer.allocate(68);
            while(!closed && buffer.hasRemaining()) {
                socketChannel.read(buffer);
            }
            buffer.flip();
            byte[] handshakeFromPeer = new byte[68];
            buffer.put(handshakeFromPeer);
            if(!verifyHandshake(handshakeFromPeer, sha1Hash, peerPeerID)) {
                return false;
            }

            //send out handshake
            buffer.clear();
            buffer.put(handshakeMessage);
            buffer.flip();
            while(!closed && buffer.hasRemaining()) {
                socketChannel.write(buffer);
            }

            //send out bitfield
            buffer = ByteBuffer.allocate(bitfieldLength+4);
            buffer.clear();
            buffer.put(bitfieldMessage);
            buffer.flip();
            while(!closed && buffer.hasRemaining()) {
                socketChannel.write(buffer);
            }

        }
        else {
            //send out our handshake, then wait for handshake, then send bitfield

            //send out handshake
            buffer = ByteBuffer.allocate(68);
            buffer.put(handshakeMessage);
            buffer.flip();
            while(!closed && buffer.hasRemaining()) {
                socketChannel.write(buffer);
            }

            //wait for handshake
            buffer.clear();
            while(!closed && buffer.hasRemaining()) {
                socketChannel.read(buffer);
            }
            buffer.flip();
            byte[] handshakeFromPeer = new byte[68];
            buffer.put(handshakeFromPeer);
            if(!verifyHandshake(handshakeFromPeer, sha1Hash, peerPeerID)) {
                return false;
            }

            //send out bitfield
            buffer = ByteBuffer.allocate(bitfieldLength+4);
            buffer.clear();
            buffer.put(bitfieldMessage);
            buffer.flip();
            while(!closed && buffer.hasRemaining()) {
                socketChannel.write(buffer);
            }
        }
        return true;
    }

    private boolean verifyHandshake(byte[] handshakeFromPeer, byte[] sha1Hash, byte[] peerPeerID) {
        for(int i = 0; i < 20; i++) {
            if(handshakeFromPeer[i+28] != sha1Hash[i]) {
                return false;
            }
        }
        if(peerPeerID == null) {
            return true;
        }
        for(int i = 0; i < 20; i++) {
            if(peerPeerID[i] != handshakeFromPeer[i+48]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Helper function to connect to a peer, either from a server socket or from a socket.
     * @return a SocketChannel object if able to connect, otherwise null
     */
    private SocketChannel connectToPeer() {
        ServerSocketChannel serverSocketChannel;
        SocketChannel socketChannel = null;
        if(hosting) {

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
                while(!closed && socketChannel == null) {
                    socketChannel = serverSocketChannel.accept();
                }
                if(socketChannel != null) {
                    socketChannel.configureBlocking(false);
                    peerIPAddress = socketChannel.getRemoteAddress().toString();

                }
            }
            catch (IOException e) {
                System.err.println("Warning: unable to connect server socket on port " + hostPort + " to a peer.");
            }

        }
        else {
            try {
                //connect this socket channel to the address 
                socketChannel = SocketChannel.open();
                socketChannel.configureBlocking(false);
                while(!closed && !socketChannel.finishConnect()) {
                    socketChannel.connect(new InetSocketAddress(peerIPAddress,peerPort));
                }
            } catch (IOException e) {
                System.err.println("Warning: unable to open or connect socket channel to "+ peerIPAddress + ":" + peerPort + ".");
            }
        }
        return socketChannel;
    }

    /**
     * Helper function to concatenate a list of byte arrays
     * @param args list of byte arrays to concatenate
     * @return the concatenated byte arrays
     * @throws IOException
     */
    private byte[] concat(byte[]... args) throws IOException {
        ByteArrayOutputStream builder = new ByteArrayOutputStream();
        for(byte[] arg : args) {
            builder.write(arg);
        }
        return builder.toByteArray();
    }

    private byte[] subarray(byte[] original, int to, int from) {
        byte[] newArray = new byte[from-to];
        System.arraycopy(original, to, newArray, to - to, from - to);
        return newArray;
    }

    private int bytesToInt(byte[] b) {
        ByteBuffer buffer = ByteBuffer.wrap(b);
        buffer.order(ByteOrder.BIG_ENDIAN);
        return buffer.getInt();
    }

    private byte[] intToBytes(int i) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(i);
        return buffer.array();
    }
}



class DataReader {
    private ByteBuffer data;

    public void setBufferLength(int length) {
        data = ByteBuffer.allocate(length).order(ByteOrder.BIG_ENDIAN);
    }

    /**
     * Method to read data from a socket channel
     * @param socketChannel socket connected to a peer
     * @return true if able to read the allocated length's worth of byte data or false if not
     * @throws IOException
     */
    public boolean read(SocketChannel socketChannel) throws IOException {
        socketChannel.read(data);
        if(data.hasRemaining()) {
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
    private ByteBuffer data;


    public void setBufferLength(int length) {
        data = ByteBuffer.allocate(length);
    }


}
