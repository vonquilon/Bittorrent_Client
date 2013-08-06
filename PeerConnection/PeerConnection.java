package PeerConnection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
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
    boolean createdFromServerSocket;
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

        createdFromServerSocket = false;
    }

    public PeerConnection(int hostPort, ArrayList<PeerConnection> activeConnections) {
        this.hostPort = hostPort;

        this.activeConnections = activeConnections;

        createdFromServerSocket = true;
    }

    public void run() {
        ServerSocketChannel serverSocketChannel;
        SocketChannel socketChannel = null;
        if(createdFromServerSocket) {
            try {
                serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.configureBlocking(false);
                serverSocketChannel.socket().bind(new InetSocketAddress(hostPort));
            } catch (IOException e) {
                System.err.println("Warning: unable to create server socket for port " + hostPort + ".");
                return;
            }
            while(!closed && socketChannel == null) {
                try {
                    socketChannel = serverSocketChannel.accept();
                    socketChannel.configureBlocking(false);
                } catch (IOException e) {
                    System.err.println("Warning: unable to connect server socket on port " + hostPort + " to a peer.");
                    return;
                }
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
                return;
            }

        }

        while(!closed) {
            ByteBuffer readBuffer = ByteBuffer.allocate(4);
            try {
                socketChannel.read(readBuffer);
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }
}
