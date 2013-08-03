package PeerConnection;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 8/2/13
 * Time: 6:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConnectionData {
    String peerIP;

    float downloadSpeed;
    float uploadSpeed;

    boolean[] peerBitfield;

    public ConnectionData(String ip) {
        peerIP = ip;
        downloadSpeed = -1;
        uploadSpeed = -1;

    }

}
