package dualcore;

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
	public static final int chNextTeamId = 65500;

	// team channels reserved from 1001 to 1100
	// channels contain: count of soldiers corresponding to this team; task of
	// the team; target of the team;
	public static final int chTeam = 1001;

	// individual soldier channels reserved from 1 to 500
	// channels contain: alive indicator; team id
	public static final int chSoldierAliveIndicator = 1001;
	public static final int chSoldierTeamId = 1002;

	// channel is used for any nonsense info
	public static final int chMisc = 0;

	public static void broadcastTask(RobotController rc, Task task,
			MapLocation target, int teamId) {
		int c = chTeam + teamId * 3;
		try {
			rc.broadcast(c + 1, task.ordinal());
			rc.broadcast(c + 2, toInt(target));
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	public static Task getTask(RobotController rc, int teamId) {
		int c = chTeam + teamId * 3;
		try {
			int ordinal = rc.readBroadcast(c + 1);
			return Task.values()[ordinal];
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		return Task.GOTO;
	}

	public static MapLocation getTarget(RobotController rc, int teamId) {
		int c = chTeam + teamId * 3;
		try {
			int encoded = rc.readBroadcast(c + 2);
			return toMapLocation(encoded);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		return new MapLocation(0, 0);
	}

	public static void assignTeamId(RobotController rc, int id) {
		try {
			rc.broadcast(chNextTeamId, id);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	/**
	 * this method should be used by soldiers. It returns the team id which
	 * should be incorporated by the requesting soldier.
	 * 
	 * @param rc
	 * @return
	 */
	public static int requestTeamId(RobotController rc) {
		try {
			return rc.readBroadcast(chNextTeamId);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * this method should be used by soldiers. They have to announce the
	 * {@link SoldierRole} that they incorporate.
	 * 
	 * @param rc
	 * @param role
	 */
	public static void announceTeamId(RobotController rc, int soldierId,
			int teamId) {
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

	private static int toInt(MapLocation location) {
		return location.x * 10000 + location.y;
	}

	private static MapLocation toMapLocation(int encoded) {
		return new MapLocation(encoded / 10000, encoded % 10000);
	}

}
