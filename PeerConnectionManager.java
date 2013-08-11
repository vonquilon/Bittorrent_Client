import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 8/2/13
 * Time: 4:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class PeerConnectionManager extends Thread{
    ArrayList<String> peerAddresses;

    List<PeerConnection> activeConnections = Collections.synchronizedList(new ArrayList<PeerConnection>());
    HashMap<String, PeerConnection> addressToConnection;
    TorrentInfo info;
    DownloadedFile file;

    boolean closed;
    boolean paused;
    private byte[] peerID;


    public void run() {
        Random random = new Random();
        long[] downloadingTimer = new long[file.numberOfPieces];
        for(int i = 0; i < downloadingTimer.length; i++ ){
            downloadingTimer[i] = System.currentTimeMillis();
        }

        while(!closed) {
            if(paused) {
                System.out.println("Pausing peer manager.");
                while(paused) {

                }
            }

            connectToAllPeers();

            //unchoke a random peer
            ArrayList<PeerConnection> chokedPeers = getChokedPeers();
            //only unchoke if we can find peers to unchoke and if the number of unchoked peers is less than 3
            if(chokedPeers.size() > 0 && activeConnections.size()-chokedPeers.size() < 3) {
                int indexOfUnchokedPeer = random.nextInt(chokedPeers.size());
                chokedPeers.get(indexOfUnchokedPeer).addAction(new PeerAction(PeerActionCode.UNCHOKEPEER));
            }

            //tell a peer to request the rarest piece
            int index = indexOfRarestMissingPiece();
            if(index == -1) {
                //we've downloaded all the available pieces
                continue;
            }
            PeerConnection peerConnection = peerWithPiece(index);
            peerConnection.addAction(new PeerAction(PeerActionCode.REQUESTPIECE,index));
            file.downloading[index] = true;
            downloadingTimer[index] = System.currentTimeMillis();

            //update the timer; if any downloading bits have expired, then clear them so that we can try again
            long currentTime = System.currentTimeMillis();
            for(int i = 0; i < downloadingTimer.length; i++) {
                //30 second expiry time
                if(file.downloading[i] && currentTime-downloadingTimer[i] > 30000) {
                    downloadingTimer[i] = currentTime;
                    file.downloading[i] = false;
                }
            }
        }
    }

    private synchronized int indexOfRarestMissingPiece() {
        int[] missingPieceCounts = new int[file.numberOfPieces];
        //tally up all pieces
        for (PeerConnection peerConnection : activeConnections) {
            boolean[] peerBitfield = peerConnection.peerBitfield;
            for (int i = 0; i < peerBitfield.length; i++) {
                if(!peerBitfield[i]) {
                    missingPieceCounts[i]++;
                }
            }
        }
        //look for index with smallest number of pieces
        int lowestIndex = -1;
        int lowestValue = Integer.MAX_VALUE;
        for(int i = 0; i < missingPieceCounts.length; i++) {
            int missingPieceCount = missingPieceCounts[i];
            if(lowestValue > missingPieceCount && !file.bitfield[i] && !file.downloading[i]) {
                lowestIndex = i;
                lowestValue = missingPieceCount;
            }
        }
        return lowestIndex;
    }

    private synchronized PeerConnection peerWithPiece(int pieceIndex) {
        //look for peer with the rarest piece
        for(PeerConnection peerConnection : activeConnections) {
            if(peerConnection.canRequest() && peerConnection.peerBitfield[pieceIndex]) {
                return peerConnection;
            }
        }
        return null;
    }

    private synchronized ArrayList<PeerConnection> getChokedPeers() {
        ArrayList<PeerConnection> chokedPeers = new ArrayList<PeerConnection>();
        for (PeerConnection peerConnection : activeConnections) {
            if (peerConnection.hostChokingPeer) {
                chokedPeers.add(peerConnection);
            }
        }
        return chokedPeers;
    }

    /**
     * Connects to all the peers provided in the peerlist by the tracker. Ensures that each peer is only connected to once.
     */
    private void connectToAllPeers() {
        for(String address : peerAddresses) {
            String[] splittedAddress = address.split(":");
            String ipAddress = splittedAddress[0];
            if(!addressToConnection.containsKey(ipAddress)) {
                int port = Integer.parseInt(splittedAddress[1]);
                PeerConnection newConnection = new PeerConnection(ipAddress,port, activeConnections, addressToConnection, info, peerID, file);
                newConnection.start();
            }
        }
    }

    public synchronized void updateConnection() {

    }

    public void closeAllConnections() throws IOException {
    }

}
