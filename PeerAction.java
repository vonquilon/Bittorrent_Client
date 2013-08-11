/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 8/10/13
 * Time: 4:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class PeerAction {
    public PeerActionCode code;
    public int argument;

    public PeerAction(PeerActionCode code, int argument) {
        this.code = code;
        this.argument = argument;
    }

    public PeerAction(PeerActionCode code) {
        this.code = code;
        argument = 0;
    }
}

enum PeerActionCode {
    CHOKEPEER,
    UNCHOKEPEER,
    REQUESTPIECE,
    BROADCASTHAVE
}
