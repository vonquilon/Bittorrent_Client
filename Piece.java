import java.util.ArrayList;

/**
 * Piece holds the occurrences of this piece and a list of the peers that have this piece.
 * 
 * @author Von Kenneth Quilon
 * @date 08/09/2013
 * @version 1.0
 */
public class Piece {

	/**
	 * The piece number.
	 */
	public int index;
	
	/**
	 * The list of peers.
	 */
	public volatile ArrayList<String> peers = new ArrayList<String>();
	
	/**
	 * The number of occurrences.
	 */
	public volatile int occurrences = 0;
	
	/**
	 * Constructs a piece.
	 * 
	 * @param index - the piece number
	 */
	public Piece(int index) {
		this.index = index;
	}
}