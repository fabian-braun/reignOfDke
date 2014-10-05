package simplePastr;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Channel {

	/* CHANNEL DEFINITIONS: [0..65535] */
	public static final int chBestPastrLocation = 65535;
	public static final int chTargetPastrCount = 65534;
	public static final int chTargetNoiseTowerCount = 65533;
	public static final int chTargetProtectorCount = 65532;
	public static final int chTargetAttackerCount = 65531;
	public static final int chCurrentPastrCount = 65530;
	public static final int chCurrentNoiseTowerCount = 65529;
	public static final int chCurrentProtectorCount = 65528;
	public static final int chCurrentAttackerCount = 65527;

	public static void broadcastBestPastrLocation(RobotController rc,
			MapLocation bestLocation) {
		int data = bestLocation.x * 1000 + bestLocation.y;
		try {
			rc.broadcast(chBestPastrLocation, data);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	public static MapLocation getBestPastrLocation(RobotController rc) {
		int data = 0;
		try {
			data = rc.readBroadcast(chBestPastrLocation);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		MapLocation bestLocation = new MapLocation(data / 1000, data % 1000);
		return bestLocation;
	}

	public static boolean requestSoldierRole(RobotController rc,
			SoldierRole role) {
		int chCurrent;
		int chTarget;
		switch (role) {
		case ATTACKER:
			chCurrent = chCurrentAttackerCount;
			chTarget = chTargetAttackerCount;
			break;
		case NOISE_TOWER_BUILDER:
			chCurrent = chCurrentNoiseTowerCount;
			chTarget = chTargetNoiseTowerCount;
			break;
		case PASTR_BUILDER:
			chCurrent = chCurrentPastrCount;
			chTarget = chTargetPastrCount;
			break;
		default: // case PROTECTOR
			chCurrent = chCurrentProtectorCount;
			chTarget = chTargetProtectorCount;
		}

		try {
			int target = rc.readBroadcast(chTarget);
			int current = rc.readBroadcast(chCurrent);
			if (current < target) {
				rc.broadcast(chCurrent, current + 1);
				return true;
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		return false;
	}
}
