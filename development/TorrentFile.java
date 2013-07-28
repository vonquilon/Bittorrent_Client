package development;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;

/**
 * This class parses the torrent file and extracts information
 * from the torrent file into its private fields.
 * 
 * @authors Von Kenneth Quilon & Alex Loh
 * @date 07/12/2013
 * @version 1.0
 */
public class TorrentFile {
	
    String filename;
    private String announce;
    private byte[] infoHashBytes;
    private int fileSize;
    private int pieceSize;
    private int numberOfPieces;
    private ArrayList<byte[]> pieceHashes;

    /**
     * Initializes this object with the specified filename.
     *
     * @param filename
     */
    public TorrentFile(String filename) {
        this.filename = filename;
    }

    /**
     * Getter for announce data.
     *
     * @return announce
     */
    public String getAnnounce() {
        return announce;
    }

    /**
     * Getter for piece hashes.
     *
     * @return pieceHashes
     */
    public ArrayList<byte[]> getPieceHashes() {
        return pieceHashes;
    }

    /**
     * Getter for number of pieces in the file.
     *
     * @return numberOfPieces
     */
    public int getNumberOfPieces() {
        return numberOfPieces;
    }

    /**
     * Getter for size of each piece.
     *
     * @return piceSize
     */
    public int getPieceSize() {
        return pieceSize;
    }

    /**
     * Getter for size of the file.
     *
     * @return fileSize
     */
    public int getFileSize() {
        return fileSize;
    }

    /**
     * Getter for bytes in the info hash.
     *
     * @return infoHashBytes
     */
    public byte[] getInfoHashBytes() {
        return infoHashBytes;
    }


    /**
     * This method parses the torrent file and puts the torrent information
     * into an Object[]. Object[0] is the Map that holds all the torrent
     * information. Object[1] is the ByteBuffer that holds the info hash. If
     * the file cannot be found or decoded, the program exits immediately.
     * 
     * Pre-conditions:  filename must be valid and must be in the current folder.
     * Post-conditions: The torrent file will be parsed and the information will
     * 					be stored in an Object[].
     *
     * @return torrentInfo 		  Object[] that holds torrent information
     * @throws java.io.IOException        File not found
     * @throws BencodingException Bencoding error
     */
    @SuppressWarnings("rawtypes")
    public Object[] parseTorrent() {

        System.out.print("Parsing torrent file: " + filename);
        Object[] torrentInfo = new Object[2];

        try {
        	
            Path pathToFile = FileSystems.getDefault().getPath(filename);
            byte[] torrentBencodedInfo = Files.readAllBytes(pathToFile);
            torrentInfo[0] = (Map) Bencoder2.decode(torrentBencodedInfo);
            torrentInfo[1] = Bencoder2.getInfoBytes(torrentBencodedInfo);
            
        } catch (IOException e) {
            System.err.println("File not found: " + filename);
            System.exit(1);
        } catch (BencodingException e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.print("....................DONE\n");
        return torrentInfo;

    }

    /**
     * Private helper method that obtains torrent information
     * from an Object[] and puts them in their corresponding private fields.
     *
     * @param torrentInfo Contains a Map of torrent information and
     *                    a ByteBuffer of the info dictionary
     */
    @SuppressWarnings("rawtypes")
    public void getFileInfo(Object[] torrentInfo) {

        Map torrentInfoMap = (Map) torrentInfo[0];

        ByteBuffer announceBuffer = (ByteBuffer) getObjectFromMap(torrentInfoMap, "announce");
        announce = new String(announceBuffer.array());

        Map fileInfoMap = (Map) getObjectFromMap(torrentInfoMap, "info");

        fileSize = (Integer) getObjectFromMap(fileInfoMap, "length");
        pieceSize = (Integer) getObjectFromMap(fileInfoMap, "piece length");
        numberOfPieces = (int) Math.ceil((double) fileSize / pieceSize);
        pieceHashes = new ArrayList<byte[]>(numberOfPieces);

        ByteBuffer pieceHashesBuffer = (ByteBuffer) getObjectFromMap(fileInfoMap, "pieces");
        getHashes(pieceHashesBuffer.array());

        ByteBuffer infoHashBuffer = (ByteBuffer) torrentInfo[1];
        infoHashBytes = Functions.encodeToSHA1(infoHashBuffer.array());

        System.out.println("\nFile Information\nFile size: " + Integer.toString(fileSize) +
                " bytes\nPiece size: " + Integer.toString(pieceSize) + " bytes\nNumber of pieces: " +
                Integer.toString(numberOfPieces) + "\n");

    }

    /**
     * Private helper method that gets an Object from a Map.
     *
     * @param map 	  Contains data to be obtained
     * @param key 	  Locates data in Map
     * @return Object The wanted data
     */
    private Object getObjectFromMap(@SuppressWarnings("rawtypes") Map map, String key) {
        return (Object) map.get(ByteBuffer.wrap(key.getBytes()));
    }

    /**
     * Private helper method that puts the hashes of the pieces
     * into the private field pieceHashes ArrayList.
     *
     * @param hashes The hashes to be put in the list
     */
    private void getHashes(byte[] hashes) {

        int offset = 0;

        for (int i = 0; i < numberOfPieces; i++) {
            byte[] pieceHash = new byte[20];
            System.arraycopy(hashes, offset, pieceHash, 0, pieceHash.length);
            pieceHashes.add(pieceHash);
            offset += 20;
        }

    }
}