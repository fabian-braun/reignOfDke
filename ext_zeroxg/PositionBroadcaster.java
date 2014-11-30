package ext_zeroxg;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class PositionBroadcaster {
	private DualChannelInteger positionSum;
	private DualChannelInteger positionCount;

	public PositionBroadcaster(int channel_1) {
		positionSum = new DualChannelInteger(channel_1, channel_1 + 1);
		positionCount = new DualChannelInteger(channel_1 + 2, channel_1 + 3);
	}

	public void reset(RobotController rc) throws GameActionException {
		positionSum.reset(rc);
		positionCount.reset(rc);
	}

	public MapLocation read(RobotController rc) throws GameActionException {
		MapLocation locSum = Channels.intToLoc(positionSum.read(rc));
		int count = positionCount.read(rc);
		count = count == 0 ? 1 : count;
		MapLocation averageLoc = new MapLocation(locSum.x / count, locSum.y
				/ count);
		return averageLoc;
	}

	public void write(RobotController rc, MapLocation loc)
			throws GameActionException {
		positionSum.add(rc, Channels.locToInt(loc));
		positionCount.add(rc, 1);
	}
}
