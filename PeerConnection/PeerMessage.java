package PeerConnection;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 8/2/13
 * Time: 4:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class PeerMessage {
    int messageLength;
    MessageType type;
    byte[] payload;

    public PeerMessage(byte[] fullMessage) {

    }


}

enum MessageType {
    KEEPALIVE (-1),
    CHOKE(0),
    UNCHOKE(1),
    INTERESTED(2),
    UNINTERESTED(3),
    HAVE(4),
    BITFIELD(5),
    REQUEST(6),
    PIECE(7);

    int id;

    MessageType(int id) {
        this.id = id;
    }

    MessageType decodeMessage(byte[] fullMessage) {

    }
}