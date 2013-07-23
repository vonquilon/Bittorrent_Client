import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

/**
 * Created with IntelliJ IDEA.
 * User: al
 * Date: 7/22/13
 * Time: 6:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class PeerDownloadConnection extends Thread{
    InputStream peerDownloadSocket;
    ConnectionState connectionState;
    boolean active;
    byte[] file;


    public PeerDownloadConnection(Socket connectionSocket, ConnectionState state, byte[] file) throws IOException {
        peerDownloadSocket = connectionSocket.getInputStream();
        connectionState = state;
        this.file = file;
        active = true;
    }

    public void run() {
        try {
            while(active) {
                if(!connectionState.peerChokedClient && connectionState.clientInterestedInPeer){
                    //byte[] rawDataFromPeer = new byte[];
                    //peerDownloadSocket.read(rawDataFromPeer);
                }

            }
            peerDownloadSocket.close();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }


    public void close() {
        active = false;
    }
}
