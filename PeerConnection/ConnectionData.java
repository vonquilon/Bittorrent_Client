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

    int bytesDownloaded;
    int timeToDownload;

    int bytesUploaded;
    int timeToUpload;


    boolean[] peerBitfield;

    public ConnectionData(String ip) {
        peerIP = ip;
        bytesDownloaded = 0;
        timeToDownload = 0;
        bytesUploaded = 0;
        timeToUpload = 0;
    }

}
