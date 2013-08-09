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
        closed = false;

        int readLength = 0;
        ByteBuffer lengthByteBuffer = ByteBuffer.allocate(4);
        ByteBuffer messageBuffer;
        lengthByteBuffer.order(ByteOrder.BIG_ENDIAN);
        while(!closed) {
            if(!lengthByteBuffer.hasRemaining()) {
                //if we don't have any remaining bytes in the buffer, then we've filled it up with the length bytes
                int length = lengthByteBuffer.getInt();
            }
            else{
                //otherwise, try again
                continue;
            }
        }
    }

    /**
     * Helper function to connect to a peer.
     * @return a SocketChannel object if able to connect, otherwise null
     */
    private SocketChannel connectToPeer() {
        ServerSocketChannel serverSocketChannel;
        SocketChannel socketChannel = null;
        if(hosting) {

            try {
                //bind a new server socket to the port that this connection uses
                serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.configureBlocking(false);
                serverSocketChannel.socket().bind(new InetSocketAddress(hostPort));
            } catch (IOException e) {
                System.err.println("Warning: unable to create server socket for port " + hostPort + ".");
                return socketChannel;
            }

            try {
                while(!closed && socketChannel == null) {
                    socketChannel = serverSocketChannel.accept();
                }
                if(socketChannel != null) {
                    socketChannel.configureBlocking(false);
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
