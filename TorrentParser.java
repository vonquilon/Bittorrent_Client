import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * TorrentParser opens a torrent file and uses the class, TorrentInfo, to extract
 * information from the torrent file.
 * 
 * @author Von Kenneth Quilon
 * @date 08/01/2013
 * @version 1.0
 */
public class TorrentParser {

	/**
	 * Parses the torrent meta info using the TorrentInfo class, prints torrent information,
	 * and returns the TorrentInfo class.
	 * 
	 * @param torrentName - the torrent file name
	 * @return torrentInfo - the TorrentInfo class
	 */
	public static TorrentInfo parseTorrent(String torrentName) {
		Path pathToFile = FileSystems.getDefault().getPath(torrentName);
		TorrentInfo torrentInfo = null;
		byte[] torrentMetaInfo;
        try {
			torrentMetaInfo = Files.readAllBytes(pathToFile);
			torrentInfo = new TorrentInfo(torrentMetaInfo);
		} catch (IOException e) {
			System.err.println("I/O error!\n");
		} catch (BencodingException e) {
			System.err.println(e.getMessage() + "\n");
		}
        System.out.println("Parsed torrent file: " + torrentName + "\n");
        System.out.println("File Information");
		System.out.println("File size: " + torrentInfo.file_length + " bytes");
		System.out.println("Piece size: " + torrentInfo.piece_length + " bytes");
		System.out.println("Number of pieces: " + torrentInfo.piece_hashes.length + "\n");
        return torrentInfo;
	}
}