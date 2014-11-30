package ext_zeroxg;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class PathBroadcast {
	private static final int startFlag = -999;

	public static void write(RobotController rc, Path path, int band)
			throws GameActionException {
		for (int i = 0; i < path.size(); i++)
			rc.broadcast(band + 2 + i, Channels.locToInt(path.get(i)));
		rc.broadcast(band + 1, path.size());
		rc.broadcast(band, startFlag);
	}

	public static Path read(RobotController rc, int band)
			throws GameActionException {
		if (rc.readBroadcast(band) != startFlag)
			return null;
		int size = rc.readBroadcast(band + 1);
		MapLocation[] path = new MapLocation[size];
		band += 2;
		for (int i = size; --i >= 0;)
			path[i] = Channels.intToLoc(rc.readBroadcast(band + i));
		return new Path(path);
	}
}
