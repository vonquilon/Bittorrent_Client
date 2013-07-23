import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * This class connects to a peer, communicates with the peer,
 * downloads pieces, and puts the pieces together.
 * 
 * @authors Von Kenneth Quilon & Alex Loh
 * @date 07/12/2013
 * @version 1.0
 */
public class PeerConnection{
    Socket connectionSocket;
    PeerDownloadConnection fromPeer;
    PeerUploadConnection toPeer;
    ConnectionState state;


    /**
     * Constructs a PeerConnection object connected to the machine with the specified IP address and port
     * @param ipAddress IP address of the peer, in the form "xxx.xxx.xxx.xxx" (ie. "255.255.255.255")
     * @param port port that we're connecting to
     * @throws IOException if unable to connect
     */
    public PeerConnection(String ipAddress, int port, byte[] file) throws IOException {
        connectionSocket = new Socket(InetAddress.getByName(ipAddress), port);
        fromPeer = new PeerDownloadConnection(connectionSocket, state, file);
        toPeer = new PeerUploadConnection(connectionSocket, state, file);
    }

    /**
     * Closes the connection to this peer
     */
    public void close() throws InterruptedException {
        fromPeer.close();
        toPeer.close();
        fromPeer.join();
        toPeer.join();
    }
}