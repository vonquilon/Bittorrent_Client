package PeerConnection;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Queue;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 8/2/13
 * Time: 4:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class PeerUploadConnection extends Thread{
    Queue<PeerMessage> messageQueue;
    OutputStream toPeer;
    boolean running;

    public void close(){
        running = false;
        this.interrupt();
    }
}
