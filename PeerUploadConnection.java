import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Created with IntelliJ IDEA.
 * User: al
 * Date: 7/22/13
 * Time: 6:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class PeerUploadConnection extends Thread{

    ServerSocket connectionSocket;
    boolean active;
    int port;
    byte[] peerID;
    FileManager file;
    TorrentFile torrentInfo;
    ArrayList<Integer> indexes;

    public PeerUploadConnection(int serverPort, TorrentFile torrentInfo, byte[] peerID, FileManager file, ArrayList<Integer> indexes) throws IOException {
        this.port = serverPort;
        connectionSocket = new ServerSocket(serverPort);
        this.file = file;
        this.torrentInfo = torrentInfo;
        this.peerID = peerID;
        this.indexes = indexes;
        active = false;
    }
    @Override
    public void run() {
        try{
            active = true;
            connectionSocket = new ServerSocket(port);
            while(active) {
                Socket socket = connectionSocket.accept();
                boolean choking = true;
                boolean peerInterested = false;
                InputStream fromPeer = socket.getInputStream();
                OutputStream toPeer = socket.getOutputStream();

                byte[] handshake = new byte[68];
                fromPeer.read(handshake);

                if(!verifyHandshake(handshake, torrentInfo.getInfoHashBytes())) {
                    connectionSocket.close();
                    continue;
                }
                byte[] peerID = getPeerID(handshake);

                boolean connectedToPeer = true;
                byte[] message = new byte[torrentInfo.getPieceSize()];
                BitSet peerBitField = new BitSet(torrentInfo.getNumberOfPieces());
                peerBitField.clear();


                while(connectedToPeer) {
                    fromPeer.read(message);
                    byte messageID = message[0];
                    byte[] payload;

                    switch(messageID) {
                        case 0:
                            //peer sent a choke message
                            //invalid message ID; ignore it
                            break;
                        case 1:
                            //peer sent an unchoke message
                            //invalid message ID; ignore it
                            break;
                        case 2:
                            //peer sent an interested message
                            peerInterested = true;
                            break;
                        case 3:
                            //peer sent a not interested message
                            peerInterested = false;
                            break;
                        case 4:
                            //peer sent a have message
                            payload = getPayload(message, 4);
                            break;
                        case 5:
                            //peer sent a bitfield message
                            payload = getPayload(message, (torrentInfo.getNumberOfPieces()+7)/8);
                            break;
                        case 6:
                            //peer sent a request message
                            break;
                        case 7:
                            //peer sent a piece message
                            //invalid message ID; ignore it
                            break;
                        case 8:
                            //peer sent a cancel message
                            break;
                        default:
                            //invalid message ID; ignore it
                            break;

                    }
                }



            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.

        }
    }

    private byte[] getPayload(byte[] message, int expectedLength) {
        return Arrays.copyOfRange(message, 1, expectedLength+1);
    }

    private byte[] getPeerID(byte[] handshake) {
        int peerIDIndex = "BitTorrent protocol".getBytes().length+23;
        return Arrays.copyOfRange(handshake,peerIDIndex,handshake.length);
    }

    /**
     * Verify that the handshake we received is of the correct format
     * @param handshake our handshake message
     * @param expectedHashBytes the expected 20-byte SHA1 hash
     * @return whether this message is a valid handshake message
     */
    private boolean verifyHandshake(byte[] handshake, byte[] expectedHashBytes) {
        byte[] protocol = "BitTorrent protocol".getBytes();
        if(handshake[0] != 19 || !Arrays.equals(Arrays.copyOfRange(handshake,1,protocol.length+1),protocol)) {
            /*
            first condition: first byte must be equal to 19.
            second condition: the next bytes must be the string "BitTorrent protocol" in bytes
            if either of the two conditions aren't met, then the data is unexpected and we kill the connection
            */
            return false;
        }
        byte[] infoHashBytes = Arrays.copyOfRange(handshake,protocol.length+2,protocol.length+22);
        return Arrays.equals(expectedHashBytes, infoHashBytes);
    }


    /**
     * Stops the thread of execution, although perhaps not immediately, and frees all resources
     */
    public void close() {
        active = false;
    }
}