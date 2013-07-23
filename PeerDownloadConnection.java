import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Created with IntelliJ IDEA.
 * User: al
 * Date: 7/22/13
 * Time: 6:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class PeerDownloadConnection extends Thread{
    Socket connectionSocket;
    ConnectionState connectionState;
    boolean active;
    AtomicReferenceArray fileReference;


    public PeerDownloadConnection(String peerIPAddress, int port, ConnectionState state, TorrentFile torrentInfo, byte[] peerID, AtomicReferenceArray fileReference, ArrayList<Integer> indexes) throws IOException {
        //this.connectionSocket = connectionSocket;
        connectionState = state;
        this.fileReference = fileReference;
        active = true;
    }

    @Override
    public void run() {
        try(InputStream peerDownloadStream = connectionSocket.getInputStream()) {
            while(active) {
                if(!connectionState.peerChokedClient && connectionState.clientInterestedInPeer){
                    //byte[] rawDataFromPeer = new byte[];
                    //peerDownloadStream.read(rawDataFromPeer);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }


    /**
     * Stops the thread of execution, although perhaps not immediately, and frees all resources
     */
    public void close() {
        active = false;
    }
}
