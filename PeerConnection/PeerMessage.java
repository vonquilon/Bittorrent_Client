package PeerConnection;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

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
        byte[] messageLengthBytes = new byte[4];
        System.arraycopy(fullMessage,0,messageLengthBytes,0,4);

        ByteBuffer buffer = ByteBuffer.wrap(messageLengthBytes);
        buffer.order(ByteOrder.BIG_ENDIAN);
        messageLength = buffer.getInt();

        type = MessageType.decode(fullMessage);

        payload = new byte[messageLength-1];
        System.arraycopy(fullMessage,5,payload,0,messageLength-1);
    }
}

enum MessageType {
    //message from peer that has no id
    KEEPALIVE (null),
    //message from peer that have id numbers
    CHOKE(0),
    UNCHOKE(1),
    INTERESTED(2),
    UNINTERESTED(3),
    HAVE(4),
    BITFIELD(5),
    REQUEST(6),
    PIECE(7),
    //messages sent by the local connection manager to tell a peer connection what to do
    CHOKETOPEER(null),
    UNCHOKETOPEER(null);

    public static final MessageType[] peerMessagesWithID = new MessageType[]{CHOKE,UNCHOKE,INTERESTED,UNINTERESTED,HAVE,BITFIELD,REQUEST,PIECE};

    Integer id;

    MessageType(Integer id) {
        this.id = id;
    }

    public static MessageType decode(byte[] fullMessage) {
        if(fullMessage.length == 4) {
            return KEEPALIVE;
        }
        else if(fullMessage.length > 4) {
            byte id = fullMessage[4];
            return peerMessagesWithID[id];
        }
        return null;
    }
}