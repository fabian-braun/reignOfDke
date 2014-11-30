package teamreignofdke;

import java.util.HashMap;
import java.util.Map;

import battlecode.common.GameActionException;
import battlecode.common.GameObject;
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

	// Pastr with this Location wants to be destroyed
	public static final int chSelfDestruction = 50000;

	/**
	 * broadcast the location which is optimal for herding cows
	 * 
	 * @param rc
	 * @param bestLocation
	 */
	public static void broadcastBestPastrLocation(RobotController rc,
			MapLocation bestLocation) {
		int data = bestLocation.x * 1000 + bestLocation.y;
		try {
			rc.broadcast(chBestPastrLocation, data);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	/**
	 * retreive the location which is optimal for herding cows
	 * 
	 * @param rc
	 * @return
	 */
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

	/**
	 * this method should be called by the control unit. It overrides the
	 * SoldierRole which is incorporated by the next produced unit
	 * 
	 * @param rc
	 * @param role
	 */
	public static void demandSoldierRole(RobotController rc, SoldierRole role) {
		try {
			rc.broadcast(chNextSoldierRole, role.ordinal());
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	/**
	 * this method returns the number of soldiers for each {@link SoldierRole}
	 * 
	 * @param rc
	 * @return
	 */
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

	/**
	 * this method returns the number of robots for each {@link RobotType}
	 * 
	 * @param rc
	 * @return
	 */
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

	/**
	 * this method should be used by soldiers. It returns the Role which should
	 * be incorporated by the requesting soldier.
	 * 
	 * @param rc
	 * @return
	 */
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

	/**
	 * this method should be used by soldiers. They have to announce the
	 * {@link SoldierRole} that they incorporate.
	 * 
	 * @param rc
	 * @param role
	 */
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

	/**
	 * this method should be used by any robot. They have to announce the
	 * {@link RobotType} that they incorporate.
	 * 
	 * @param rc
	 * @param role
	 */
	public static void announceSoldierType(RobotController rc, RobotType type) {
		int chCurrent;
		switch (type) {
		case SOLDIER:
			chCurrent = chCurrentSoldierCount;
			break;
		case NOISETOWER:
			chCurrent = chCurrentNoiseTowerCount;
			break;
		case PASTR:
			chCurrent = chCurrentPastrCount;
			break;
		default: // case HQ
			// should never happen... otherwise misc channel
			chCurrent = chMisc;
		}
		try {
			rc.broadcast(chCurrent, rc.readBroadcast(chCurrent) + 1);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	/**
	 * executed by a pastr that wants to be destroyed
	 * 
	 * @param rc
	 * @param loc
	 */
	public static void broadcastSelfDestruction(RobotController rc,
			MapLocation loc) {
		int data = loc.x * 1000 + loc.y;
		try {
			rc.broadcast(chSelfDestruction, data);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	/**
	 * executed by the HQ to find pastr who wants to be destroyed
	 * 
	 * @param rc
	 * @return
	 */
	public static MapLocation getSelfDestructionLocation(RobotController rc) {
		int data = 0;
		try {
			data = rc.readBroadcast(chSelfDestruction);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		MapLocation selfDestruction = new MapLocation(data / 1000, data % 1000);
		return selfDestruction;
	}

	public static boolean needSelfDestruction(RobotController rc) {
		int data = 0;
		try {
			data = rc.readBroadcast(chSelfDestruction);
			MapLocation selfDestruction = new MapLocation(data / 1000,
					data % 1000);
			if (rc.canSenseSquare(selfDestruction)) {
				GameObject stillAlive = rc
						.senseObjectAtLocation(selfDestruction);
				if (stillAlive == null) {
					rc.broadcast(chSelfDestruction, 0);
					data = 0;
				}
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}

		if (data == 0) {
			return false;
		} else {
			return true;
		}
	}
}
