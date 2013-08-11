import java.util.ArrayList;

public class Piece {

	public int index;
	public volatile ArrayList<String> peers = new ArrayList<String>();
	public volatile int occurrences = 0;
	
	public Piece(int index) {
		this.index = index;
	}
}