package reignierOfDKE;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class Channel {

	/* CHANNEL DEFINITIONS: [0..65535] */
	public static final int chBestPastrLocation = 65535;
	// count of soldier types
	public static final int chCurrentPastrCount = 65534;
	public static final int chCurrentNoiseTowerCount = 65533;
	public static final int chCurrentSoldierCount = 65532;

	public static final int chNextTeamId = 65531;
	public static final int chNextSoldierId = 65530;

	public static final int chMapComplexity = 65529;

	public static final int chOppCenter = 65528;
	public static final int chOppMeanDistToCenter = 65527;
	public static final int chCountOppBrdCastingSoldiers = 65526;
	public static final int chOppMilkQuantity = 65525;
	public static final int chPastrCount = 65524;

	/**
	 * channels 10000 - 30020 contain info about reduced map
	 */
	public static final int chReducedMapInfo = 10000;

	/**
	 * team channels reserved from 1001 to 1100. channels contain:<br\>
	 * (+0) id of this team;<br\>
	 * (+1) count of soldiers corresponding to this team;<br\>
	 * (+2) task of the team;<br\>
	 * (+3) target of the team;<br\>
	 * (+4) positional center of the team;<br\>
	 * (+5) temporary target of the team, set by leader;<br\>
	 * (+6) best pastr location for this team<br\>
	 */
	public static final int chTeam = 1001;
	private static final int teamChannelCount = 10;

	/**
	 * individual soldier channels reserved from 1 to 600. channels contain:<br\>
	 * (+0) id of this soldier;<br\>
	 * (+1) alive indicator;<br\>
	 * (+2) team id of this soldier;<br\>
	 * (+3) current position of this soldier<br\>
	 * channel 500 corresponds to second core
	 */
	public static final int chSoldier = 1;
	private static final int soldierChannelCount = 4;

	// channel is used for any nonsense info
	public static final int chMisc = 0;

	// ########### methods ########################################

	/**
	 * returns the corresponding channel for the given team without offset.<br\>
	 * Offsets are:<br\>
	 * (+0) id of this team;<br\>
	 * (+1) count of soldiers corresponding to this team;<br\>
	 * (+2) task of the team;<br\>
	 * (+3) target of the team;<br\>
	 * (+4) positional center of the team;<br\>
	 * (+5) temporary target of the team, set by leader;<br\>
	 * (+6) best pastr location for this team<br\>
	 * 
	 * @param teamId
	 * @return
	 */
	private static int getTeamChannel(int teamId) {
		return chTeam + teamId * teamChannelCount;
	}

	/**
	 * returns the corresponding channel for the given soldier without offset.<br\>
	 * Offsets are:<br\>
	 * (+0) id of this soldier;<br\>
	 * (+1) alive indicator;<br\>
	 * (+2) team id of this soldier;<br\>
	 * (+3) current position of this soldier<br\>
	 * 
	 * @param soldierId
	 * @return
	 */
	private static int getSoldierChannel(int soldierId) {
		return chSoldier + soldierId * soldierChannelCount;
	}

	public static void broadcastTask(RobotController rc, Task task,
			MapLocation target, int teamId) {
		int c = getTeamChannel(teamId);
		try {
			rc.broadcast(c + 2, task.ordinal());
			rc.broadcast(c + 3, toInt(target));
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	public static Task getTask(RobotController rc, int teamId) {
		int c = getTeamChannel(teamId);
		try {
			int ordinal = rc.readBroadcast(c + 2);
			return Task.values()[ordinal];
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		return Task.GOTO;
	}

	public static MapLocation getTarget(RobotController rc, int teamId) {
		int c = getTeamChannel(teamId);
		try {
			int encoded = rc.readBroadcast(c + 3);
			return toMapLocation(encoded);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		return new MapLocation(0, 0);
	}

	/**
	 * for HQ: set the id which should be incorporated by the next soldier
	 * 
	 * @param rc
	 * @param id
	 */
	public static void assignTeamId(RobotController rc, int id) {
		try {
			rc.broadcast(chNextTeamId, id);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	/**
	 * for SOLDIER. It returns the team id which should be incorporated by the
	 * requesting soldier.
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
	 * for SOLDIER. They have to announce the teamId that they incorporate.
	 * 
	 * @param rc
	 * @param role
	 */
	public static void announceTeamId(RobotController rc, int soldierId,
			int teamId) {
		int c = getSoldierChannel(soldierId);
		try {
			rc.broadcast(c + 2, teamId);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	public static int getTeamIdOfSoldier(RobotController rc, int soldierId) {
		int c = getSoldierChannel(soldierId);
		try {
			return rc.readBroadcast(c + 2);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public static int getLeaderIdOfTeam(RobotController rc, int teamId) {
		// Loop through all robots
		for (int id = 0; id < GameConstants.MAX_ROBOTS; id++) {
			// Check if the robot is alive
			if (isAlive(rc, id)) {
				// Check the alive robot is on the same team
				if (teamId == getTeamIdOfSoldier(rc, id)) {
					// The robot with the lowest ID is the leader
					return id;
				}
			}
		}
		// Didn't find an alive robot in this team, return -1
		return -1;
	}

	public static MapLocation getLocationOfSoldier(RobotController rc,
			int soldierId) {
		int c = getSoldierChannel(soldierId);
		try {
			return toMapLocation(rc.readBroadcast(c + 3));
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		return new MapLocation(0, 0);
	}

	public static int getSoldierCountOfTeam(RobotController rc, int teamId) {
		int c = getTeamChannel(teamId);
		try {
			return rc.readBroadcast(c + 1);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public static void broadcastSoldierCountOfTeam(RobotController rc,
			int teamId, int members) {
		int c = getTeamChannel(teamId);
		try {
			rc.broadcast(c + 1, members);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	public static MapLocation getPositionalCenterOfTeam(RobotController rc,
			int teamId) {
		int c = getTeamChannel(teamId);
		try {
			return toMapLocation(rc.readBroadcast(c + 4));
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		return new MapLocation(0, 0);
	}

	public static void broadcastPositionalCenterOfTeam(RobotController rc,
			int teamId, MapLocation center) {
		int c = getTeamChannel(teamId);
		try {
			rc.broadcast(c + 4, toInt(center));
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	public static MapLocation getTemporaryTarget(RobotController rc, int teamId) {
		int c = getTeamChannel(teamId);
		try {
			return toMapLocation(rc.readBroadcast(c + 5));
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		return new MapLocation(0, 0);
	}

	public static void broadcastTemporaryTarget(RobotController rc, int teamId,
			MapLocation target) {
		int c = getTeamChannel(teamId);
		try {
			rc.broadcast(c + 5, toInt(target));
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	public static int requestSoldierId(RobotController rc) {
		try {
			int myId = rc.readBroadcast(chNextSoldierId);
			if (!isAlive(rc, myId + 1)) {
				rc.broadcast(chNextSoldierId, myId + 1);
			} else {
				int i = 2;
				while (i < GameConstants.MAX_ROBOTS) {
					int id = myId + i % GameConstants.MAX_ROBOTS;
					if (!isAlive(rc, id)) {
						rc.broadcast(chNextSoldierId, id);
						break;
					}
					i++;
				}
			}
			return myId;
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * for SOLDIER. should be called at the beginning of each round. Soldier
	 * broadcasts that he is still alive and his current {@link MapLocation}
	 * 
	 * @param rc
	 * @param soldierId
	 */
	public static void signalAlive(RobotController rc, int soldierId) {
		int c = getSoldierChannel(soldierId);
		try {
			rc.broadcast(c + 1, Clock.getRoundNum());
			rc.broadcast(c + 3, toInt(rc.getLocation()));
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	/**
	 * returns whether the soldier is still alive. Not 100% accurate. Death will
	 * only be noticed after 8 rounds.
	 * 
	 * @param rc
	 * @param soldierId
	 * @return
	 */
	public static boolean isAlive(RobotController rc, int soldierId) {
		int c = getSoldierChannel(soldierId);
		try {
			int round = rc.readBroadcast(c + 1);
			return round > Clock.getRoundNum() - 12 && round > 0;
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		return false;
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
		return toInt(location.y, location.x);
	}

	private static int toInt(int y, int x) {
		return x * 1000 + y;
	}

	private static MapLocation toMapLocation(int encoded) {
		return new MapLocation(encoded / 1000, encoded % 1000);
	}

	public static TerrainTile getReducedMapTerrain(RobotController rc, int yR,
			int xR) {
		int channelOfLoc = chReducedMapInfo + toInt(yR, xR);
		int iTerrain = 0;
		try {
			iTerrain = rc.readBroadcast(channelOfLoc);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		switch (iTerrain) {
		case 1:
			return TerrainTile.NORMAL;
		case 2:
			return TerrainTile.ROAD;
		case 3:
			return TerrainTile.VOID;
		default: // 0
			return TerrainTile.OFF_MAP;
		}
	}

	public static void setReducedMapTerrain(RobotController rc, int yR, int xR,
			TerrainTile terrain) {
		int channelOfLoc = chReducedMapInfo + toInt(yR, xR);
		int iTerrain = 0;
		switch (terrain) {
		case NORMAL:
			iTerrain = 1;
			break;
		case ROAD:
			iTerrain = 2;
			break;
		case VOID:
			iTerrain = 3;
			break;
		default:
			// iTerrain = 0
			break;
		}
		try {
			rc.broadcast(channelOfLoc, iTerrain);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	public static void broadcastMapComplexity(RobotController rc,
			MapComplexity c) {
		try {
			rc.broadcast(chMapComplexity, c.ordinal());
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	public static void resetMapComplexity(RobotController rc) {
		try {
			rc.broadcast(chMapComplexity, -1);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	public static MapComplexity getMapComplexity(RobotController rc) {
		try {
			int ordinal = rc.readBroadcast(chMapComplexity);
			if (ordinal < 0) {
				return MapComplexity.COMPLEX;
			}
			return MapComplexity.values()[ordinal];
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		return MapComplexity.COMPLEX;

	}

	public static MapLocation getPositionalCenterOfOpponent(RobotController rc) {
		try {
			return toMapLocation(rc.readBroadcast(chOppCenter));
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		return new MapLocation(0, 0);
	}

	public static void broadcastPositionalCenterOfOpponent(RobotController rc,
			MapLocation center) {
		try {
			rc.broadcast(chOppCenter, toInt(center));
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	public static int getOpponentMeanDistToCenter(RobotController rc) {
		try {
			return rc.readBroadcast(chOppMeanDistToCenter);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		return 100000;
	}

	public static void broadcastOpponentMeanDistToCenter(RobotController rc,
			int meanDist) {
		try {
			rc.broadcast(chOppMeanDistToCenter, meanDist);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	public static int getCountOppBrdCastingSoldiers(RobotController rc) {
		try {
			return rc.readBroadcast(chCountOppBrdCastingSoldiers);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		return 100000;
	}

	public static void broadcastCountOppBrdCastingSoldiers(RobotController rc,
			int count) {
		try {
			rc.broadcast(chCountOppBrdCastingSoldiers, count);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	public static int getOpponentMilkQuantity(RobotController rc) {
		try {
			return rc.readBroadcast(chOppMilkQuantity);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public static void broadcastOpponentMilkQuantity(RobotController rc,
			int quantity) {
		try {
			rc.broadcast(chOppMilkQuantity, quantity);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	public static int getPastrCount(RobotController rc) {
		try {
			return rc.readBroadcast(chPastrCount);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public static void announceNewPastr(RobotController rc) {
		try {
			rc.broadcast(chPastrCount, rc.readBroadcast(chPastrCount) + 1);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	public static void announcePastrDeath(RobotController rc) {
		try {
			int count = rc.readBroadcast(chPastrCount);
			if (count > 0) {
				count--;
			}
			rc.broadcast(chPastrCount, count);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	public static void announcePastrLocation(RobotController rc,
			MapLocation location, int teamId) {
		int c = getTeamChannel(teamId) + 6;
		try {
			rc.broadcast(c, toInt(location));
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	public static MapLocation getPastrLocation(RobotController rc, int teamId) {
		int c = getTeamChannel(teamId) + 6;
		try {
			return toMapLocation(rc.readBroadcast(c));
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		return new MapLocation(-1, -1);
	}

}
