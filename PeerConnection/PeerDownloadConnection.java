package PeerConnection;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Queue;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 8/2/13
 * Time: 4:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class PeerDownloadConnection extends Thread{
    Queue<PeerMessage> messageQueue;
    OutputStream toPeer;
    boolean running;

    public void close(){
        running = false;
    }
}
