import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteOrder;
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
                socket.setSoTimeout(3000*60);
                boolean choking = true;
                boolean peerInterested = false;
                InputStream fromPeer = socket.getInputStream();
                OutputStream toPeer = socket.getOutputStream();

                //read in the handshake
                byte[] handshake = new byte[68];
                fromPeer.read(handshake);

                //if the handshake is bad, then stop the connection
                if(!verifyHandshake(handshake, torrentInfo.getInfoHashBytes())) {
                    connectionSocket.close();
                    continue;
                }
                byte[] peerID = getPeerID(handshake);

                //otherwise, immediately send our bitfield to the peer if we have data
                boolean hasData = false;
                for(char piece : file.bitfield) {
                    if(piece != '0') {
                        hasData = true;
                        break;
                    }
                }
                if(hasData) {


                }



                boolean connectedToPeer = true;
                byte[] lengthBytes = new byte[4];



                while(connectedToPeer) {
                    //read the 4 byte length and parse it into an int
                    fromPeer.read(lengthBytes);
                    int length = java.nio.ByteBuffer.wrap(lengthBytes).order(ByteOrder.BIG_ENDIAN).getInt();
                    if(length == 0) {
                        //peer sent a keep-alive message, refreshing our timeout
                        continue;
                    }
                    //construct a byte buffer equal to the size of the specified message and read it in
                    byte[] message = new byte[length];
                    fromPeer.read(message);

                    byte messageID = message[4];
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
                            //send an unchoke message to the peer indicating that we're ready to transfer data
                            //message should be 0,0,0,1,1 in bytes
                            byte[] unchokeMessage = new byte[5];
                            Arrays.fill(unchokeMessage, (byte) 0);
                            unchokeMessage[3] = 1;
                            unchokeMessage[4] = 1;
                            toPeer.write(unchokeMessage);
                            break;
                        case 3:
                            //peer sent a not interested message
                            peerInterested = false;
                            break;
                        case 4:
                            //peer sent a have message
                            payload = getPayload(message, length);
                            int pieceIndex = payload[length];
                            break;
                        case 5:
                            //peer sent a bitfield message
                            //invalid messageID; ignore it
                            break;
                        case 6:
                            //peer sent a request message
                            break;
                        case 7:
                            //peer sent a piece message
                            //invalid message ID; ignore it
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

    private byte[] getPayload(byte[] message, int length) {
        return Arrays.copyOfRange(message, 5, length+5);
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