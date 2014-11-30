package ext_animorphs;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

/**
 * General class for broadcasting, receiving and storing messages about
 * locations of stuff
 */
public class LocationMessager {
	public int channel;
	private RobotController rc;
	private LocSet locs;

	/**
	 * Initiliazes Messager
	 * 
	 * @param rc
	 *            Robot Controller
	 * @param empty
	 *            should be an empty loc set
	 * @param channel
	 *            Starting channel given by communication protocol for object
	 *            using this for. e.g. CommsProtocol.TOWER_CHANNEL_START
	 */
	public LocationMessager(RobotController rc, LocSet empty, int channel) {
		this.channel = channel; // should be one of starting channel from
								// communication protocol, e.g.
								// CommsProtocol.TOWER_CHANNEL_START
		this.rc = rc;
		this.locs = empty; // should be empty
	}

	/**
	 * Always make sure update is done. Or else broadcasts will override. Checks
	 * channels in a rolling fashion. No check against channel number going too
	 * high, but it's limited in theory.
	 * 
	 * @throws GameActionException
	 */
	public void update() throws GameActionException {
		int data = rc.readBroadcast(channel);
		while (data != 0) {
			data--;
			if (data < CommsProtocol.DESTROYED_OFFSET) {
				locs.add(new MapLocation(data % GameConstants.MAP_MAX_WIDTH,
						data / GameConstants.MAP_MAX_WIDTH));
			} else {
				locs.remove(new MapLocation(data % GameConstants.MAP_MAX_WIDTH,
						(data - CommsProtocol.DESTROYED_OFFSET)
								/ GameConstants.MAP_MAX_WIDTH));
			}
			channel++;
			data = rc.readBroadcast(channel);
		}
	}

	/**
	 * Takes in some locations. Checks if it is marked already and broadcasts if
	 * new. Returns whether broadcast made. Should be called after updates to
	 * make sure an existing broadcast isn't wiped
	 * 
	 * @throws GameActionException
	 */
	public boolean broadcast(MapLocation[] new_locs) throws GameActionException {
		boolean broadcasted = false;
		for (int i = 0; i < new_locs.length; i++) {
			if (!locs.contains(new_locs[i])) {
				rc.broadcast(channel, CommsProtocol.convertLoc(new_locs[i]) + 1);
				// manually update receiver for this
				channel++;
				locs.add(new_locs[i]);
				broadcasted = true;
			}
		}
		return broadcasted;
	}

	public boolean broadcast(MapLocation loc) throws GameActionException {
		if (!locs.contains(loc)) {
			rc.broadcast(channel, CommsProtocol.convertLoc(loc) + 1);
			channel++;
			locs.add(loc);
			return true;
		}
		return false;
	}

	/**
	 * Broadcast a kill
	 * 
	 * @param location
	 *            of killed object. Null if no kill
	 * @return whether you broadcasted a kill
	 * @throws GameActionException
	 */
	public boolean broadcastKill(MapLocation loc) throws GameActionException {
		if (loc != null) {
			rc.broadcast(channel, CommsProtocol.convertLoc(loc)
					+ CommsProtocol.DESTROYED_OFFSET + 1);
			// manually update receiver
			channel++;
			locs.remove(loc);
			return true;
		}
		return false;
	}
}
