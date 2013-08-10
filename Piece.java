import java.util.ArrayList;

public class Piece {

	public volatile ArrayList<String> peers = new ArrayList<String>();
	public volatile int occurrences = 0;
}