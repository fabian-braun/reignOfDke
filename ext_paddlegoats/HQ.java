package ext_paddlegoats;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.TerrainTile;

public class HQ extends Bot {

	static boolean alert;

	static int botCount;
	static MapLocation[] pastrs;
	static int pastrCount;
	static int noisetowerCount;
	static int queuedPastrs;
	static int queuedNoisetowers;
	static int soldierCount;
	static MapLocation[] enemyPastrs;

	static int[][] orders = new int[3][ordersWidth];
	static int currentCaste = 0;
	static int spawned;
	// never have to worry about decrementing it too far. just defaults to 0.

	static Mode previousMode = Mode.ATTACK_HQ;
	static boolean modeChanged;

	static MapLocation pastrGoal;

	void init() {

	}

	void run() throws GameActionException {
		pre();
		MapLocation ptarg = null;
		for (MapLocation p : enemyPastrs) {
			if (d2(p, enemyHQ) > 3) {
				ptarg = p;
			}
		}
		if (ptarg != null) {
			mode = Mode.ATTACK_PASTR;
		} else if (botCount > 9) {
			mode = Mode.PASTR;
		} else {
			mode = Mode.ATTACK_HQ;
		}
		log(0, mode.toString());
		modeChanged = mode == previousMode ? false : true;
		previousMode = mode;
		switch (mode) {
		case ATTACK_HQ:
			orders[0][1] = orders[0][2] = 0;
			orders[0][GOAL] = writeLoc(enemyHQ);
			break;
		case PASTR:
			if (pastrGoal == null) {
				pastrGoal = choosePastrLoc();
			}
			orders[0][GOAL] = writeLoc(pastrGoal);
			orders[0][1] = 0;
			orders[0][2] = 0;
			if (rc.senseNearbyGameObjects(Robot.class, pastrGoal, 30, ourTeam).length > 6) {
				if (noisetowerCount + queuedNoisetowers == 0) {
					orders[0][1] = 1;
					orders[0][2] = 0;
				} else if (pastrCount + queuedPastrs == 0) {
					orders[0][1] = 0;
					orders[0][2] = 1;
				}
			}
			break;
		case ATTACK_PASTR:
			orders[0][1] = orders[0][2] = 0;
			orders[0][GOAL] = writeLoc(ptarg);
			break;
		}
		post();
	}

	static void pre() throws GameActionException {
		alert = false; // enemies.length > 0;
		botCount = rc.senseRobotCount();
		pastrs = rc.sensePastrLocations(ourTeam);
		pastrCount = pastrs.length;
		noisetowerCount = rc.readBroadcast(NOISETOWERCENSUS);
		rc.broadcast(NOISETOWERCENSUS, 0);
		soldierCount = (botCount - pastrCount * 2 - noisetowerCount * 3);
		enemyPastrs = rc.sensePastrLocations(enemyTeam);

		queuedNoisetowers = rc.readBroadcast(QUEUEDNOISETOWER);
		rc.broadcast(QUEUEDNOISETOWER, 0);
		queuedPastrs = rc.readBroadcast(QUEUEDPASTR);
		rc.broadcast(QUEUEDPASTR, 0);
		if (active) {
			if (alert) {
				attack();
			} else if (botCount < 25) {
				spawn();
			}
		}
	}

	static void post() throws GameActionException {
		for (int c = orders.length; --c >= 0;) {
			for (int o = ordersWidth; --o >= 0;) {
				int order = orders[c][o];
				int channel = ORDERS + ordersWidth * c + o;
				if (rc.readBroadcast(channel) != order) {
					rc.broadcast(channel, order);
				} // not sure if this really saves that many bytecodes. but we
					// don't need to be stingy with the HQ.
			}
		}
		rc.broadcast(MODE, mode.ordinal());
	}

	static MapLocation choosePastrLoc() throws GameActionException {
		double[][] cows = rc.senseCowGrowth();
		double bestFound = 0;
		MapLocation best = null;
		for (int i = 50; best == null || --i >= 0;) {
			MapLocation loc = new MapLocation(rand.nextInt(mapx),
					rand.nextInt(mapy));
			if (rc.senseTerrainTile(loc) == TerrainTile.NORMAL) {
				if (d2(loc, ourHQ) < d2(loc, enemyHQ)) {
					double score = cows[loc.x][loc.y];
					if (score > bestFound) {
						bestFound = score;
						best = loc;
					}
				}
			}
		}
		return best;
	}

	static void attack() throws GameActionException {
		int closest = 999;
		MapLocation target = null;
		for (Robot enemy : enemies) {
			MapLocation loc = rc.senseRobotInfo(enemy).location;
			int d = d2(loc);
			if (d < closest) {
				closest = d;
				target = loc;
			}
		}
		if (d2(target) > 15) {
			target = target.add(target.directionTo(here));
		}
		if (rc.canAttackSquare(target)) {
			attack(target);
		}
	}

	static void spawn() throws GameActionException {
		for (Direction dir : directions) {
			if (rc.canMove(dir)) {
				rc.broadcast(CASTE, currentCaste);
				rc.spawn(dir);
				notActive();
				spawned++;
				return;
			}
		}
	}
}
