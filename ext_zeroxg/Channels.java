package ext_zeroxg;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public abstract class Channels {
	public static int alertLevel = 1;
	public static int pastrUnderAttack = 3;
	public static int armyTargetLocation = 10;
	public static JobBoard jobBoard = new JobBoard(10000);
	public static StateMap stateMap = new StateMap(30000);
	public static RadarMap radarMap = new RadarMap(40000);
	public static NetworkBoard network = new NetworkBoard(50000);

	public static int locToInt(MapLocation loc) {
		return loc.x * 100 + loc.y;
	}

	public static MapLocation intToLoc(int x) {
		return new MapLocation(x / 100, x % 100);
	}

	public static void broadcastAlertLevel(RobotController rc, AlertLevel level)
			throws GameActionException {
		rc.broadcast(alertLevel, level.ordinal());
	}

	public static AlertLevel readAlertLevel(RobotController rc)
			throws GameActionException {
		return AlertLevel.values()[rc.readBroadcast(alertLevel)];
	}

	public static void broadcastLocation(RobotController rc, int channel,
			MapLocation loc) throws GameActionException {
		rc.broadcast(channel, Channels.locToInt(loc));
	}

	public static MapLocation readLocation(RobotController rc, int channel)
			throws GameActionException {
		return intToLoc(rc.readBroadcast(channel));
	}

	public static void printBand(RobotController rc, int start, int size)
			throws GameActionException {
		System.out.print("Band: " + start + " | ");
		for (int i = start; i < start + size; i++) {
			System.out.print(rc.readBroadcast(i) + " ");
		}
		System.out.println();
	}
}
