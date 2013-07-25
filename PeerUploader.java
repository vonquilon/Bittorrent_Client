import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: al
 * Date: 7/25/13
 * Time: 5:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class PeerUploader {

    boolean active;
    int portMinRange;
    int portHighRange;
    byte[] peerID;
    FileManager file;
    TorrentFile torrentInfo;
    ArrayList<Integer> indexes;
    ArrayList<PeerUploadConnection> uploadConnections;

    public PeerUploader(int portMinRange, int portHighRange, TorrentFile torrentInfo, byte[] peerID, FileManager file, ArrayList<Integer> indexes) throws IOException {
        uploadConnections = new ArrayList<>();
        for(int i = portMinRange; i <= portHighRange; i++) {
            uploadConnections.add(new PeerUploadConnection(i,torrentInfo, peerID, file, indexes));
        }
        this.portMinRange = portMinRange;
        this.portHighRange = portHighRange;
        this.file = file;
        this.torrentInfo = torrentInfo;
        this.peerID = peerID;
        this.indexes = indexes;
        active = false;
    }

    public void startUploading() {
        active = true;
        for(PeerUploadConnection uploadConnection : uploadConnections) {
            uploadConnection.run();
        }
    }

    public void stopUploading() throws InterruptedException {
        active = false;
        for(PeerUploadConnection uploadConnection : uploadConnections) {
            uploadConnection.close();
            uploadConnection.join();
        }
    }
}
