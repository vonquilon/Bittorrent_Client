package development;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * This class contains helper functions for generating a peer ID,
 * making a URL class, converting a hex string to a URL encoded string,
 * converting a byte[] to a hex string, encoding a byte[] to a
 * SHA-1 encoded byte[], and generating a random integer.
 * 
 * @authors Von Kenneth Quilon & Alex Loh
 * @date 07/12/2013
 * @version 1.0
 */
public class Functions {
	
    /**
     * private constructor, ensures that no instance of this class can be created
     */
    private Functions() {
    	
    }

    /**
     * Private helper method that sets the private field peerID to a
     * unique 20 bytes peer ID every time the program is started.
     * 
     * Algorithm: first 6 randomly-generated characters are numbers [0-9]
     * 			  next 6 randomly-generated characters are uppercase letters [A-Z]
     * 			  last 8 randomly-generated characters are lowercase letters [a-z]
     * 
     * @return peerID
     */
    public static byte[] generatePeerID() {

        byte[] peerID = new byte[20];

        for (int i = 0; i < 6; i++)
            //generate random numbers [0-9]
            peerID[i] = (byte) (generateRandomInt(9) + 48);
        for (int i = 6; i < 12; i++)
            //generates random uppercase letters [A-Z]
            peerID[i] = (byte) (generateRandomInt(25) + 65);
        for (int i = 12; i < 20; i++)
            //generates random lowercase letters [a-z]
            peerID[i] = (byte) (generateRandomInt(25) + 97);

        return peerID;
        
    }

    /**
     * Private helper method that generates a URL object.
     *
     * @param announce      The announce string data from the torrent file
     * @param peerID        
     * @param infoHashBytes
     * @param uploaded      Bytes uploaded
     * @param downloaded    Total bytes downloaded including header data
     * @param left          Bytes left to download
     * @param event         {started, stopped, completed}
     * @return URL 			The created URL object
     * @throws java.net.MalformedURLException Bad URL
     */
    public static URL makeURL(String announce, byte[] peerID, byte[] infoHashBytes, int uploaded,
    		int downloaded, int left, String event) throws MalformedURLException {

        StringBuilder urlSb = new StringBuilder();

        urlSb.append(announce);
        urlSb.append("?info_hash=");
        urlSb.append(hexStringToURL(bytesToHex(infoHashBytes)));
        urlSb.append("&peer_id=");
        urlSb.append(new String(peerID));
        urlSb.append("&port=9593");

        if (uploaded >= 0)
            urlSb.append("&uploaded=" + Integer.toString(uploaded));

        if (downloaded >= 0)
            urlSb.append("&downloaded=" + Integer.toString(downloaded));

        if (left >= 0)
            urlSb.append("&left=" + Integer.toString(left));

        if (event != null)
            urlSb.append("&event=" + event);

        return new URL(urlSb.toString());

    }

    /**
     * Private helper method that converts a hex string
     * into a URL encoded string.
     *
     * @param hexString
     * @return String The URL encoded hex string
     */
    private static String hexStringToURL(String hexString) {

        int length = hexString.length();
        char[] urlEncoded = new char[length + length / 2];

        for (int i = 0, j = 0; j < length; i++, j++) {
            urlEncoded[i] = '%';
            i++;
            urlEncoded[i] = hexString.charAt(j);
            i++;
            j++;
            urlEncoded[i] = hexString.charAt(j);
        }

        return new String(urlEncoded);

    }

    /**
     * Private helper method that converts a byte[] into a hex String.
     *
     * @param bytes The byte[] to be converted
     * @return hexString
     */
    private static String bytesToHex(byte[] bytes) {

        String hexString = "";

        for (byte byteObject : bytes)
            hexString += String.format("%02X", byteObject & 0xff);

        return hexString;

    }

    /**
     * Private helper method that encodes a byte[] into
     * a 20 bytes SHA-1 encoded byte[].
     *
     * @param toEncode 	   The byte[] to be encoded
     * @return sha1Encoded The encoded byte[]
     * @throws java.security.NoSuchAlgorithmException Unknown algorithm
     */
    public static byte[] encodeToSHA1(byte[] toEncode) {

        byte[] sha1Encoded = null;

        try {
            MessageDigest encoder = MessageDigest.getInstance("SHA-1");
            sha1Encoded = encoder.digest(toEncode);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Algorithm is not supported!");
            System.exit(1);
        }

        return sha1Encoded;

    }

    /**
     * Private helper method that generates a random integer.
     *
     * @param max The highest possible number
     * @return int A randomly-generated integer
     */
    public static int generateRandomInt(int max) {

        Random randomGenerator = new Random();
        if (max != 0)
            return randomGenerator.nextInt(max);
        else
            return 0;

    }
}