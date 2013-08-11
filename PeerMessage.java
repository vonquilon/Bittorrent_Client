import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A class built to facilitate transforming peer messages to and from byte arrays and parsed information
 */
public class PeerMessage {
    int messageLength;
    MessageType type;
    byte[] payload;

    byte[] rawMessage;

    /**
     * getter for the parsed message length
     * @return the parsed message length
     */
    public int getMessageLength() {
        return messageLength;
    }

    /**
     * getter for the parsed message type
     * @return the parsed message type
     */
    public MessageType getType() {
        return type;
    }

    /**
     * getter for the parsed payload data
     * @return the parsed payload data
     */
    public byte[] getPayload() {
        return payload;
    }

    /**
     * getter for the raw message in its entirety
     * @return the raw message
     */
    public byte[] getRawMessage() {
        return rawMessage;
    }
    /**
     * constructs this peer message object as a parsed form of a received byte[] message
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

        rawMessage = fullMessage;
    }

    /**
     * constructs this peer message object out of defined fields from which a byte[] message can be constructed from
     * @param messageLength the length of this message
     * @param type the type of this message
     * @param payload the byte array payload of this message
     */
    public PeerMessage(int messageLength, MessageType type, byte[] payload) {
        this.messageLength = messageLength;
        this.type = type;
        this.payload = payload;

        byte[] messageLengthBytes = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(messageLength).array();

        byte typeByte = MessageType.encode(type);

        rawMessage = new byte[messageLengthBytes.length+1+payload.length];
        System.arraycopy(messageLengthBytes,0,rawMessage,0,messageLengthBytes.length);
        rawMessage[4] = typeByte;
        System.arraycopy(payload,0,rawMessage,messageLengthBytes.length+1,payload.length);


    }
}

/**
 * class that acts as an identifier for messages
 */
enum MessageType {
    //message from peer that has no id
    KEEPALIVE (null),
    //message from peer that have id numbers
    CHOKE((byte)0),
    UNCHOKE((byte)1),
    INTERESTED((byte)2),
    UNINTERESTED((byte)3),
    HAVE((byte)4),
    BITFIELD((byte)5),
    REQUEST((byte)6),
    PIECE((byte)7),
    //messages sent by the local connection manager to tell a peer connection what to do (note that these id numbers are not actually sent in standard peer-to-peer communication)
    CHOKETOPEER((byte)8),
    UNCHOKETOPEER((byte)9);


    public static final MessageType[] peerMessagesWithID = new MessageType[]{CHOKE,UNCHOKE,INTERESTED,UNINTERESTED,HAVE,BITFIELD,REQUEST,PIECE};

    Byte id;

    /**
     * constructor for enum objects; the id is this message's encoded id
     * @param id
     */
    MessageType(Byte id) {
        this.id = id;
    }

    /**
     * decodes this message into a MessageType enum describing what type of message this is
     * @param fullMessage the message to decode in its entirety
     * @return the type of this message as a MessageType
     */
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

    /**
     * encodes this message from a MessageType object into the byte id of the corresponding message
     * @param type the type of the message to encode
     * @return the id of the message
     */
    public static byte encode(MessageType type) {
       if(type.id == null) {
           return -1;
       }
       else {
           return type.id;
       }
    }
}