package ext_animorphs;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

/**
 * Most general command messaging class. Can read/post to ID-specific channels
 * if given an id and offset. Can also post and read general messages to
 * specified channels
 *
 */
public class CommandMessager {
	private RobotController rc;
	public static MessageType[] msgs = MessageType.values();
	public MapLocation lastLoc;
	public MessageType lastMsg;

	public CommandMessager(RobotController rc) {
		this.rc = rc;

	}

	/**
	 * Attempt to read a channel associated with specific robot. Which channel
	 * type (input/output of robot) determined by offset. Expects a id tag in
	 * the message. Returns whether the channel had data
	 * 
	 * @param id
	 *            ID of target
	 * @param offset
	 *            Offset of channeltype of target robot. Apply according to
	 *            comms protocol
	 * @throws GameActionException
	 * @returns whether something was received
	 */
	public boolean readIDChannel(int id, int offset) throws GameActionException {
		int chan = id % CommsProtocol.ID_CHAN_MOD;
		int data = rc.readBroadcast(offset + chan);
		if (data == 0) {
			return false;
		} else {
			data--;
			if (data % CommsProtocol.ID_CHAN_MOD == id
					% CommsProtocol.ID_MSG_MOD) {
				data = data / CommsProtocol.ID_CHAN_MOD;
				lastMsg = msgs[data / (CommsProtocol.LOC_MASK)];
				lastLoc = new MapLocation(data % GameConstants.MAP_MAX_WIDTH,
						(data / GameConstants.MAP_MAX_WIDTH)
								% GameConstants.MAP_MAX_HEIGHT);
				return true;
			}
			return false;
		}
	}

	/**
	 * Broadcast to channel associated with specified robot. Which channel type
	 * (input/output) determined by offset from comms protocol. Puts in id tag
	 * check in the message.
	 * 
	 * @param id
	 *            ID of robot
	 * @param offset
	 *            Offset for channeltype (input1, output1, etc)
	 * @throws GameActionException
	 */
	public void broadcastIDChannel(int id, int offset, MessageType msg,
			MapLocation loc) throws GameActionException {
		int chan = id % CommsProtocol.ID_CHAN_MOD;
		rc.broadcast(offset + chan, 1
				+ (id % CommsProtocol.ID_MSG_MOD)
				+ CommsProtocol.ID_CHAN_MOD
				* (CommsProtocol.convertLoc(loc) + msg.ordinal()
						* CommsProtocol.LOC_MASK));
	}

	/**
	 * clear channel (set to 0) for id input/output channel
	 * 
	 * @param id
	 * @param offset
	 * @throws GameActionException
	 */
	public void clearIDChannel(int id, int offset) throws GameActionException {
		rc.broadcast(offset + (id % CommsProtocol.ID_CHAN_MOD), 0);
	}

	/**
	 * Broadcast to a channel generically. No id check added. Broadcast 0 to
	 * clear.
	 * 
	 * @param channel
	 *            Global channel to broadcast to.
	 * @param msg
	 *            Messagetype
	 * @param loc
	 *            Location
	 * @throws GameActionException
	 */
	public void broadcastChannel(int channel, MessageType msg, MapLocation loc)
			throws GameActionException {
		rc.broadcast(channel, 1 + CommsProtocol.convertLoc(loc) + msg.ordinal()
				* CommsProtocol.LOC_MASK);
	}

	public boolean readChannel(int channel) throws GameActionException {
		int data = rc.readBroadcast(channel);
		if (data != 0) {
			data--;
			lastMsg = msgs[data / (CommsProtocol.LOC_MASK)];
			lastLoc = new MapLocation(data % GameConstants.MAP_MAX_WIDTH,
					(data / GameConstants.MAP_MAX_WIDTH)
							% GameConstants.MAP_MAX_HEIGHT);
			return true;
		}
		return false;
	}

}
