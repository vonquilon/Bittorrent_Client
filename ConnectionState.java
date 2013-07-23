/**
 * This class maintains state on a single end of the connection, either the download or upload end, as according to the BitTorrent specification:
 *
 byte[] rawDataFromPeer
 */
public class ConnectionState {
    boolean clientChokedPeer;
    boolean clientInterestedInPeer;
    boolean peerChokedClient;
    boolean peerInterestedInClient;

    /**
     * Sets up the initial state of the connection, according to the BitTorrent protocol:
     */
    public ConnectionState() {
        clientChokedPeer = true;
        clientInterestedInPeer = false;
        peerInterestedInClient = false;
        peerChokedClient = true;
    }
}
