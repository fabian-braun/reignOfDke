package ext_animorphs;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class DirectionMap {
	private RobotController rc;
	public Direction lastDir;
	public MapLocation lastLoc;

	public final static int channelDone = CommsProtocol.DIRECTION_CHANNEL_START;
	public final static int offset = channelDone + 1;
	public final static int LOC_MASK = CommsProtocol.LOC_MASK;
	public final static int WIDTH = GameConstants.MAP_MAX_WIDTH;
	public final static int HEIGHT = GameConstants.MAP_MAX_HEIGHT;

	public Direction[] intToDir;

	public DirectionMap(RobotController rc) {
		intToDir = Direction.values();
		this.rc = rc;
	}

	/**
	 * Broadcast on "done" channel that map is ready to use.
	 * 
	 * @throws GameActionException
	 */
	public void broadcastDone() throws GameActionException {
		rc.broadcast(channelDone, 1);
	}

	public void clearDone() throws GameActionException {
		rc.broadcast(channelDone, 0);
	}

	public boolean readDone() throws GameActionException {
		return rc.readBroadcast(channelDone) != 0;
	}

	/**
	 * Post a direction to target location "to" at a loc
	 */
	public void broadcastDirection(MapLocation loc, MapLocation to,
			Direction dir) throws GameActionException {
		rc.broadcast(offset + loc.x + loc.y * WIDTH, 1 + to.x + to.y * WIDTH
				+ LOC_MASK * dir.ordinal());
	}

	/**
	 * clear a location
	 * 
	 * @param loc
	 * @throws GameActionException
	 */
	public void clearDirection(MapLocation loc) throws GameActionException {
		rc.broadcast(offset + loc.x + loc.y * WIDTH, 0);
	}

	/**
	 * Read Direction and Location posted for a location. Returns false if there
	 * is no message
	 * 
	 * @param loc
	 * @param to
	 * @return
	 * @throws GameActionException
	 */
	public boolean readDirection(MapLocation loc) throws GameActionException {
		int data = rc.readBroadcast(offset + loc.x + loc.y * WIDTH);
		if (data == 0) {
			// nothing there
			return false;
		} else {
			data--;
			lastLoc = new MapLocation(data % WIDTH, (data / WIDTH) % HEIGHT);
			lastDir = intToDir[data / LOC_MASK];
			return true;
		}
	}

}
