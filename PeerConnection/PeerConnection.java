package PeerConnection;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 8/2/13
 * Time: 4:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class PeerConnection {
    Socket socketToPeer;
    ServerSocket serverSocketToPeer;
    PeerDownloadConnection downloadConnection;
    PeerUploadConnection uploadConnection;
}
