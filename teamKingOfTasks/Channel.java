package teamKingOfTasks;

import java.util.HashMap;
import java.util.Map;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Channel {

	/* CHANNEL DEFINITIONS: [0..65535] */
	public static final int chBestPastrLocation = 65535;
	// count of soldier roles
	public static final int chCurrentPastrBuilderCount = 65534;
	public static final int chCurrentNoiseTowerBuilderCount = 65533;
	public static final int chCurrentProtectorCount = 65532;
	public static final int chCurrentAttackerCount = 65531;
	// count of soldier types
	public static final int chCurrentPastrCount = 65530;
	public static final int chCurrentNoiseTowerCount = 65529;
	public static final int chCurrentSoldierCount = 65528;
	// next role that a robot should incorporate
	public static final int chNextSoldierRole = 65500;
	// channel is used for any nonsense info
	public static final int chMisc = 0;
	// strategy
	public static final int chStrategy = 1;

	public static void broadcastBestPastrLocation(RobotController rc,
			MapLocation bestLocation) {
		int data = bestLocation.x * 1000 + bestLocation.y;
		try {
			rc.broadcast(chBestPastrLocation, data);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	public static void broadcastStrategy(RobotController rc, Strategy s) {
		int data = s.ordinal();
		try {
			rc.broadcast(chStrategy, data);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	public static Strategy getStrategy(RobotController rc) {
		int data = 0;
		try {
			data = rc.readBroadcast(chStrategy);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		Strategy[] s = Strategy.values();
		return s[data];
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

	public static void demandSoldierRole(RobotController rc, SoldierRole role) {
		try {
			rc.broadcast(chNextSoldierRole, role.ordinal());
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	public static Map<SoldierRole, Integer> getSoldierRoleCount(
			RobotController rc) {
		HashMap<SoldierRole, Integer> result = new HashMap<SoldierRole, Integer>();
		try {
			int pastrBuilderCount = rc
					.readBroadcast(chCurrentPastrBuilderCount);
			int noiseTowerBuilderCount = rc
					.readBroadcast(chCurrentNoiseTowerBuilderCount);
			int protectorCount = rc.readBroadcast(chCurrentProtectorCount);
			int attackerCount = rc.readBroadcast(chCurrentAttackerCount);
			result.put(SoldierRole.PASTR_BUILDER, pastrBuilderCount);
			result.put(SoldierRole.NOISE_TOWER_BUILDER, noiseTowerBuilderCount);
			result.put(SoldierRole.PROTECTOR, protectorCount);
			result.put(SoldierRole.ATTACKER, attackerCount);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		return result;
	}

	public static Map<RobotType, Integer> getRobotTypeCount(RobotController rc) {
		HashMap<RobotType, Integer> result = new HashMap<RobotType, Integer>();
		try {
			int pastrCount = rc.readBroadcast(chCurrentPastrCount);
			int noiseTowerCount = rc.readBroadcast(chCurrentNoiseTowerCount);
			int soldierCount = rc.readBroadcast(chCurrentSoldierCount);
			result.put(RobotType.NOISETOWER, noiseTowerCount);
			result.put(RobotType.PASTR, pastrCount);
			result.put(RobotType.SOLDIER, soldierCount);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		return result;
	}

	public static SoldierRole requestSoldierRole(RobotController rc) {
		try {
			int ordinal = rc.readBroadcast(chNextSoldierRole);
			return SoldierRole.values()[ordinal];
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		System.err.println("reading from channel chNextSoldierRole"
				+ " failed. Assume SoldierRole.ATTACKER");
		return SoldierRole.ATTACKER;
	}

	public static void announceSoldierRole(RobotController rc, SoldierRole role) {
		int chCurrent;
		switch (role) {
		case ATTACKER:
			chCurrent = chCurrentAttackerCount;
			break;
		case NOISE_TOWER_BUILDER:
			chCurrent = chCurrentNoiseTowerBuilderCount;
			break;
		case PASTR_BUILDER:
			chCurrent = chCurrentPastrBuilderCount;
			break;
		default: // case PROTECTOR
			chCurrent = chCurrentProtectorCount;
		}
		try {
			rc.broadcast(chCurrent, rc.readBroadcast(chCurrent) + 1);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	public static void announceSoldierType(RobotController rc, RobotType type) {
		int chCurrent;
		switch (type) {
		case HQ:
			// should never happen... otherwise misc channel
			chCurrent = chMisc;
			break;
		case NOISETOWER:
			chCurrent = chCurrentNoiseTowerCount;
			break;
		case PASTR:
			chCurrent = chCurrentPastrCount;
			break;
		default: // case SOLDIER
			chCurrent = chCurrentSoldierCount;
		}
		try {
			rc.broadcast(chCurrent, rc.readBroadcast(chCurrent) + 1);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

}
