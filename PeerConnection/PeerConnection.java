package PeerConnection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

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
        DataReader reader = new DataReader();
        DataWriter writer = new DataWriter();

        boolean readInLength = false;
        int messageLength = 0;

        while(!closed) {
            if(readInLength) {
                reader.setBufferLength(messageLength);
                readInLength = false;
            }
            else {
                reader.setBufferLength(4);
                try {
                    if(reader.read(socketChannel)) {

                        readInLength = true;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

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
}



class DataReader {
    private ByteBuffer data;

    public void setBufferLength(int length) {
        data = ByteBuffer.allocate(length);
    }

    /**
     * Method to read data from a socket channel
     * @param socketChannel
     * @return
     * @throws IOException
     */
    public boolean read(SocketChannel socketChannel) throws IOException {
        socketChannel.read(data);
        if(data.hasRemaining()) {
            return false;
        }
        return true;
    }

    public byte[] getDataAsByteArray() {
        byte[] readData = new byte[data.position()];
        data.get(readData);
        return readData;
    }

    public int getDataAsInt() {
        return data.order(ByteOrder.BIG_ENDIAN).getInt();
    }
}

class DataWriter {
    private ByteBuffer data;


    public void setBufferLength(int length) {
        data = ByteBuffer.allocate(length);
    }


}
