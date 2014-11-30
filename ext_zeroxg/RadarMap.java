package ext_zeroxg;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class RadarMap {
	private static final int validTurnCount = 2;
	private int band;

	public RadarMap(int band) // takes 10000 channels
	{
		this.band = band;
	}

	public void broadcast(RobotController rc, MapLocation loc, int enemyCount)
			throws GameActionException {
		rc.broadcast(band + Channels.locToInt(loc), enemyCount);
	}

	public int read(RobotController rc, MapLocation loc)
			throws GameActionException {
		return rc.readBroadcast(band + Channels.locToInt(loc));
	}
}
