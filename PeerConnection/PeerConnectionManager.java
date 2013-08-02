package PeerConnection;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 8/2/13
 * Time: 4:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class PeerConnectionManager {
    ArrayList<PeerConnection> peerConnections;

    public void closeAllConnections() throws IOException {
        for(PeerConnection peerConnection : peerConnections) {
            peerConnection.close();
        }
    }

}
