import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created with IntelliJ IDEA.
 * User: al
 * Date: 7/22/13
 * Time: 6:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class PeerUploadConnection extends Thread{
    ServerSocket peerUploadSocket;
    boolean active;

    public PeerUploadConnection(String ipAddress, int port) throws IOException {

        active = true;
    }

    public void run() {
        while(active) {

        }
        try {
            peerUploadSocket.close();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void close() {
        active = false;
    }
}
