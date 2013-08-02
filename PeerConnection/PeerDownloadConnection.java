package PeerConnection;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Queue;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 8/2/13
 * Time: 4:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class PeerDownloadConnection {
    Queue<PeerMessage> messageQueue;
    OutputStream toPeer;

    public void close() throws IOException {

    }
}
