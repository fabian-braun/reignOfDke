package ext_animorphs;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

/**
 * Manages a channel. Channel is associated with a robot id and offset by
 * channel = id % CommsProtocol.ID_CHAN_MOD + offset
 * 
 * Should be initialized if you know a robot will be using a particular channel
 * (i.e. its personal inbound/outbound channels) a lot.
 */
public class RobotMessager {
	private RobotController rc;
	public final int channel;
	public final int msgIDCheck; // Added to messages to confirm id
	public static MessageType[] msgs = MessageType.values();
	public MapLocation lastLoc;
	public MessageType lastMsg;
	public final int ID_CHAN_MOD = CommsProtocol.ID_CHAN_MOD;
	public final int LOC_MASK = CommsProtocol.LOC_MASK;
	public final int WIDTH = GameConstants.MAP_MAX_WIDTH;
	public final int HEIGHT = GameConstants.MAP_MAX_HEIGHT;

	/**
	 * Feed in offset to get whether this channel is an input to or output from
	 * robot #id (according to CommsProtocol)
	 * 
	 * @param rc
	 * @param id
	 *            to associate
	 * @param offset
	 */
	public RobotMessager(RobotController rc, int myid, int offset) {
		this.rc = rc;
		channel = (myid % ID_CHAN_MOD) + offset;
		// msgIDCheck = ((myid % CommsProtocol.ID_CHAN_MOD) %
		// CommsProtocol.ID_MSG_MOD) + 1;
		msgIDCheck = (myid % CommsProtocol.ID_MSG_MOD) + 1;
	}

	/**
	 * Check channel for message. Saves if any
	 * 
	 * @return
	 * @throws GameActionException
	 */
	public boolean readMsg() throws GameActionException {
		int data = rc.readBroadcast(channel);
		if (data == 0) {
			return false;
		} else {
			if (data % ID_CHAN_MOD == msgIDCheck) {
				data = data / ID_CHAN_MOD;
				lastMsg = msgs[data / (LOC_MASK)];
				lastLoc = new MapLocation(data % WIDTH, (data / WIDTH) % HEIGHT);
				return true;
			}
			return false;
		}
	}

	public void clearChannel() throws GameActionException {
		rc.broadcast(channel, 0);
	}

	/**
	 * Broadcast to channel
	 * 
	 * @param msg
	 *            a MessageType
	 * @param loc
	 *            a location
	 * @throws GameActionException
	 */
	public void broadcastMsg(MessageType msg, MapLocation loc)
			throws GameActionException {
		rc.broadcast(channel, msgIDCheck + ID_CHAN_MOD
				* ((loc.x + loc.y * WIDTH) + msg.ordinal() * LOC_MASK));
	}
}
