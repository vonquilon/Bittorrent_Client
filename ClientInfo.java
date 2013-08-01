import java.io.IOException;
import java.net.ServerSocket;
import java.util.NoSuchElementException;
import java.util.Random;

/**
 * ClientInfo holds a client's peer ID, port number, total uploaded, and total downloaded.
 * 
 * @author Von Kenneth Quilon
 * @date 08/01/2013
 * @version 1.0
 */
public class ClientInfo {

	public final static byte[] PEER_ID = generatePeerID();
	public static Integer port;
	
	/**
     * Sets the public final static field, PEER_ID, to 20 bytes of characters.
     * 
     * Algorithm: first 6 randomly-generated characters are numbers [0-9]
     * 			   next 6 randomly-generated characters are uppercase letters [A-Z]
     * 			   last 8 randomly-generated characters are lowercase letters [a-z]
     * 
     * @return peerID - the 20 byte peer ID
     */
    private static byte[] generatePeerID() {
        byte[] peerID = new byte[20];
        Random randomGenerator = new Random();
        
        for (int i = 0; i < 6; i++)
            //generate random numbers [0-9]
            peerID[i] = (byte) (randomGenerator.nextInt(9) + 48);
        for (int i = 6; i < 12; i++)
            //generates random uppercase letters [A-Z]
            peerID[i] = (byte) (randomGenerator.nextInt(25) + 65);
        for (int i = 12; i < 20; i++)
            //generates random lowercase letters [a-z]
            peerID[i] = (byte) (randomGenerator.nextInt(25) + 97);

        return peerID;
    }
    
    /**
     * Looks for an available port number between 6881 and 6889. If a port number
     * is available, the ClientInfo's public static field, port, is set to this
     * port number. Otherwise, it throws a NoSuchElementException.
     * 
     * @throws NoSuchElementException
     */
    public static void setPort() throws NoSuchElementException {
    	port = null;
    	ServerSocket testSocket = null;
    	for(int i = 6881; i <= 6889; i++) {
    		try {
				testSocket = new ServerSocket(i);
				port = i;
				break;
			} catch (IOException e) {
				//do nothing
			} finally {
				if(testSocket != null) {
					try {
						testSocket.close();
					} catch (IOException e) {
						//do nothing
					}
				}
			}//end finally
    	}//end for
    	if(port.equals(null))
    		throw new NoSuchElementException("No available port! Free up a port between 6881 and 6889.");
    }
}