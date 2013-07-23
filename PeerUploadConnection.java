import java.io.IOException;
import java.io.OutputStream;
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
    Socket connectionSocket;
    ConnectionState connectionState;
    boolean active;
    byte[] file;

    public PeerUploadConnection(Socket connectionSocket, ConnectionState state, byte[] file) throws IOException {
        this.connectionSocket = connectionSocket;
        connectionState = state;
        this.file = file;
        active = true;
    }

    public void run() {
        try(OutputStream peerUploadStream = connectionSocket.getOutputStream()) {
            while(active) {
                if(!connectionState.clientChokedPeer && connectionState.peerInterestedInClient){

                }
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.

        }
    }

    public void close() {
        active = false;
    }
}
