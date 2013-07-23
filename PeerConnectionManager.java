import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: al
 * Date: 7/22/13
 * Time: 11:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class PeerConnectionManager {
    List<PeerConnection> peers;

    public PeerConnectionManager() {
        peers = new ArrayList<PeerConnection>();
    }
}
