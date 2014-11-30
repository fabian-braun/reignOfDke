package ext_animorphs;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Team;

/**
 * Helper class for memo-izing sensing
 */
public class Sensor {
	public Robot[][] bots;
	// public RobotInfo[][] soldiers;
	// public int[] numSoldiers;
	// public RobotInfo[][] pastures;
	// public int[] numPastures;
	// public RobotInfo[][] towers;
	// public int[] numTowers;

	public FastRobotInfoSet info;
	public final int INFO_HASH;
	public boolean infoInitAllAllies; // whether a list of stuff has been added
										// to info
	public boolean infoInitSenseEnemies;

	public MapLocation[][] locs;
	private RobotController rc;

	/************** MAGIC CONSTANTS FOR LENGTHS OF BOTS AND LOCS **************/
	// Numbers arrays in bots and locs respectively
	public final int botsLength = 17;
	public final int locsLength = 3;

	public final int attackRadiusSquared;
	public final int sensorRadiusSquared;
	public static final int CLOSE_RADIUS_SQUARED = 4;
	public static final int SEMI_CLOSE_RADIUS_SQUARED = 5;
	public final int maxDistance;
	public final Team team;
	public final Team enemy;

	/************** CONSTANTS FOR ARRAY INDEXING **************/

	/************** BOTS **************/
	// nearby attack range
	public static final int ATTACK_RANGE_ENEMIES = 0;
	public static final int ATTACK_RANGE_ALLIES = 1;
	public static final int ATTACK_RANGE_ALL = 2;
	// nearby sense range
	public static final int SENSE_RANGE_ENEMIES = 3;
	public static final int SENSE_RANGE_ALLIES = 4;
	public static final int SENSE_RANGE_ALL = 5;
	// adjacent
	public static final int ADJACENT_ENEMIES = 6;
	public static final int ADJACENT_ALLIES = 7;
	public static final int ADJACENT_ALL = 8;
	// all allies
	public static final int ALL_ALLIES = 9;
	// broadcasting enemy robots
	public static final int BROADCASTING_ENEMIES = 10;
	// close
	public static final int CLOSE_ENEMIES = 11;
	public static final int CLOSE_ALLIES = 12;
	public static final int CLOSE_ALL = 13;
	// semi close
	public static final int SEMI_CLOSE_ENEMIES = 14;
	public static final int SEMI_CLOSE_ALLIES = 15;
	public static final int SEMI_CLOSE_ALL = 16;

	/************** LOCS **************/
	// broadcasting enemy robot locations
	public static final int BROADCASTING_ENEMY_LOCS = 0;
	// pastrs
	public static final int ENEMY_PASTR_LOCS = 1;
	public static final int ALLIED_PASTR_LOCS = 2;

	/**
	 * init method
	 * 
	 * @param c
	 *            Controller
	 */
	public Sensor(Controller c) {
		this.attackRadiusSquared = c.attackRadiusSquared;
		this.sensorRadiusSquared = c.sensorRadiusSquared;
		this.maxDistance = c.diagonalSq;
		this.rc = c.rc;
		this.team = c.team;
		this.enemy = c.enemy;

		bots = new Robot[botsLength][];
		// soldiers = new RobotInfo[botsLength][];
		// numSoldiers = new int[botsLength];
		// pastures = new RobotInfo[botsLength][];
		// numPastures = new int[botsLength];
		// towers = new RobotInfo[botsLength][] ;
		// numTowers = new int[botsLength];

		info = new FastRobotInfoSet(c.rc);
		INFO_HASH = FastRobotInfoSet.HASH;
		infoInitAllAllies = false;
		infoInitSenseEnemies = false;

		locs = new MapLocation[locsLength][];
	}

	/**
	 * CLEAR SENSOR. REQUIRED.
	 */
	public void init() {
		bots = new Robot[botsLength][];
		// soldiers = new RobotInfo[botsLength][];
		// numSoldiers = new int[botsLength];
		// pastures = new RobotInfo[botsLength][];
		// numPastures = new int[botsLength];
		// towers = new RobotInfo[botsLength][] ;
		// numTowers = new int[botsLength];

		// info.clear();
		info.info = new RobotInfo[INFO_HASH];
		locs = new MapLocation[locsLength][];
		infoInitAllAllies = false;
		infoInitSenseEnemies = false;
	}

	public void initSenseEnemyInfo() throws GameActionException {
		// Get all enemy bots around
		if (bots[3] == null) {
			bots[3] = rc.senseNearbyGameObjects(Robot.class,
					sensorRadiusSquared, enemy);
		}
		info.add(bots[3]);
		infoInitSenseEnemies = true;
	}

	public void initAllAllyInfo() throws GameActionException {
		// Get all allies
		if (bots[9] == null) {
			bots[9] = rc.senseNearbyGameObjects(Robot.class, maxDistance, team);
		}
		info.add(bots[9]);
		infoInitAllAllies = true;
	}

	/**
	 * Get RobotInfo from stored set. Returns null if info is not in hashset, or
	 * initInfo was not called; SHOULD JUST INLINE CALL this.getInfo(robot)
	 * 
	 * @param r
	 * @return
	 */
	public RobotInfo getInfo(Robot r) {
		return info.info[r.getID() % INFO_HASH];
	}

	/**
	 * Get RobotInfos from stored set. Returns a null entry for bots not in
	 * sense range or if initInfo was not called SHOULD JUST INLINE CALL
	 * this.info.get(bots)
	 * 
	 * @param bots
	 * @return
	 */
	public RobotInfo[] getInfo(Robot[] bots) {
		return info.get(bots);
	}

	// /** UNUSED - STUFF FOR STORING GROUPS BY ROBOT TYPE. ADD IN IF REUSING
	// MANY OF THE SAME LOOP WITHOUT THE HQ
	// * get soldier info specified by input (a constant of this class). No
	// checks.
	// * Some values may be null, numSoldiers for how many are not null.
	// * Num Soldiers is null until one of these methods is called
	// * @param type which list of bots you want (taken from constants of this
	// class
	// * @return null if invalid type
	// */
	// public RobotInfo[] getSoldierInfo(int type){
	// if(soldiers[type] != null){
	// return soldiers[type];
	// }
	// else{
	// Robot[] bots = getBots(type);
	// soldiers[type] = new RobotInfo[bots.length];
	// }
	// }
	//

	/**
	 * get robot list specified by input (a constant of this class). No checks.
	 * 
	 * @param type
	 *            which list of bots you want (taken from constants of this
	 *            class
	 * @return null if invalid type
	 */
	public Robot[] getBots(int type) {
		if (bots[type] != null) {
			return bots[type];
		} else {
			switch (type) {
			case 0:
				bots[0] = rc.senseNearbyGameObjects(Robot.class,
						attackRadiusSquared, enemy);
				return bots[0];
			case 1:
				bots[1] = rc.senseNearbyGameObjects(Robot.class,
						attackRadiusSquared, team);
				return bots[1];
			case 2:
				bots[2] = rc.senseNearbyGameObjects(Robot.class,
						attackRadiusSquared);
				return bots[2];
			case 3:
				bots[3] = rc.senseNearbyGameObjects(Robot.class,
						sensorRadiusSquared, enemy);
				return bots[3];
			case 4:
				bots[4] = rc.senseNearbyGameObjects(Robot.class,
						sensorRadiusSquared, team);
				return bots[4];
			case 5:
				bots[5] = rc.senseNearbyGameObjects(Robot.class,
						sensorRadiusSquared);
				return bots[5];
			case 6:
				bots[6] = rc.senseNearbyGameObjects(Robot.class, 2, enemy);
				return bots[6];
			case 7:
				bots[7] = rc.senseNearbyGameObjects(Robot.class, 2, team);
				return bots[7];
			case 8:
				bots[8] = rc.senseNearbyGameObjects(Robot.class, 2);
				return bots[8];
			case 9:
				bots[9] = rc.senseNearbyGameObjects(Robot.class, maxDistance,
						team);
				return bots[9];
			case 10:
				bots[10] = rc.senseBroadcastingRobots(enemy);
				return bots[10];
			case 11:
				bots[11] = rc.senseNearbyGameObjects(Robot.class,
						CLOSE_RADIUS_SQUARED, enemy);
				return bots[11];
			case 12:
				bots[12] = rc.senseNearbyGameObjects(Robot.class,
						CLOSE_RADIUS_SQUARED, team);
				return bots[12];
			case 13:
				bots[13] = rc.senseNearbyGameObjects(Robot.class,
						CLOSE_RADIUS_SQUARED);
				return bots[13];
			case 14:
				bots[14] = rc.senseNearbyGameObjects(Robot.class,
						SEMI_CLOSE_RADIUS_SQUARED, enemy);
				return bots[14];
			case 15:
				bots[15] = rc.senseNearbyGameObjects(Robot.class,
						SEMI_CLOSE_RADIUS_SQUARED, team);
				return bots[15];
			case 16:
				bots[16] = rc.senseNearbyGameObjects(Robot.class,
						SEMI_CLOSE_RADIUS_SQUARED);
				return bots[16];
			default:
				return null;
			}
		}
	}

	/**
	 * get location list specified by type (specified by constants of this
	 * class).
	 * 
	 * @param type
	 *            what locs you want
	 * @return null if invalid type
	 */
	public MapLocation[] getLocs(int type) {
		if (locs[type] != null) {
			return locs[type];
		} else {
			switch (type) {
			case 0:
				locs[0] = rc.senseBroadcastingRobotLocations(enemy);
				return locs[0];
			case 1:
				locs[1] = rc.sensePastrLocations(enemy);
				return locs[1];
			case 2:
				locs[2] = rc.sensePastrLocations(team);
				return locs[2];
			default:
				return null;
			}
		}
	}

}
