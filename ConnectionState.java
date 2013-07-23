/**
 * This class maintains state on a single end of the connection, either the download or upload end, as according to the BitTorrent specification:
 * "
 byte[] rawDataFromPeer
 */
public class ConnectionState {
    boolean clientChokedPeer;
    boolean clientInterestedInPeer;
    boolean peerChokedClient;
    boolean peerInterestedInClient;

    public ConnectionState() {
        clientChokedPeer = false;
        clientInterestedInPeer = false;
        peerInterestedInClient = false;
        peerChokedClient = false;
    }
}
