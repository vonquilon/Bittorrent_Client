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

    public int getMessageLength() {
        return messageLength;
    }

    public MessageType getType() {
        return type;
    }

    public byte[] getPayload() {
        return payload;
    }

    /**
     * constructs this peer message object as a parsed form of the full message
     * @param fullMessage the peer message in its entirety
     */
    public PeerMessage(byte[] fullMessage) {
        byte[] messageLengthBytes;
        int id;
        byte[] payload;
    }
}

enum MessageType {
    KEEPALIVE (null),

    CHOKE(0),
    UNCHOKE(1),
    INTERESTED(2),
    UNINTERESTED(3),
    HAVE(4),
    BITFIELD(5),
    REQUEST(6),
    PIECE(7),

    CHOKETOPEER(null),
    UNCHOKETOPEER(null);



    Integer id;

    MessageType(Integer id) {
        this.id = id;
    }

    MessageType decodeMessage(byte[] fullMessage) {

    }
}