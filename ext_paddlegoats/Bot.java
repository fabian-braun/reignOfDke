package ext_paddlegoats;

import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Team;

public abstract class Bot {
	// Constant
	static RobotController rc;
	static RobotType type;
	static int id;
	static Random rand;
	static int mapx, mapy;
	static Team ourTeam, enemyTeam;
	static MapLocation ourHQ, enemyHQ;
	static int birthday;

	// Nonconstant
	static MapLocation here;
	static MapLocation oldHere;
	static MapLocation[] trail = new MapLocation[2000];
	static int trailEnd;
	static double health;
	static boolean active;
	static int round;
	static Robot[] enemies;
	static double delay;

	enum Mode {
		ATTACK_HQ, ATTACK_PASTR, PASTR
	}

	static Mode mode = Mode.ATTACK_HQ;

	// Debug
	static boolean log = false;
	static String[] ind = { "*", "*", "*", "*" };

	static void initBot() {
		type = rc.getType();
		id = rc.getRobot().getID();
		rand = new Random(id);

		mapx = rc.getMapWidth();
		mapy = rc.getMapHeight();
		ourTeam = rc.getTeam();
		enemyTeam = ourTeam.opponent();
		ourHQ = rc.senseHQLocation();
		enemyHQ = rc.senseEnemyHQLocation();

		birthday = Clock.getRoundNum();
		trail[0] = trail[1] = ourHQ;
		trailEnd = 1;
	}

	abstract void init();

	abstract void run() throws GameActionException;

	void go(RobotController rc_) {
		rc = rc_;
		initBot();
		init();
		while (true) {
			try {
				update();
				run();
			} catch (Exception e) {
				e.printStackTrace();
			}
			log();
			rc.yield();
		}
	}

	static void update() throws GameActionException {
		if (!rc.getLocation().equals(trail[trailEnd])) {
			trailEnd++;
			trail[trailEnd] = rc.getLocation();
		}
		here = trail[trailEnd];
		oldHere = trail[trailEnd - 1];
		health = rc.getHealth();
		active = rc.isActive();
		round = Clock.getRoundNum();
		enemies = rc.senseNearbyGameObjects(Robot.class, 20, enemyTeam);
		delay = rc.getActionDelay();

	}

	static void log() {
		if (log) {
			ind[3] = here.toString() + ind[3];
			for (int i = 3; --i >= 0;) {
				rc.setIndicatorString(i, ind[i]);
			}
			ind[0] = ind[1] = ind[2] = ind[3] = "*";
		} else {
			rc.setIndicatorString(0, "PaddleGoats vS-5");
			rc.setIndicatorString(2, "2014");
		}
	}

	static void log(int i, String s) {
		if (log) {
			ind[i] += s;
		}
	}

	// Utility

	static void suicide(Direction dir) throws GameActionException {
		move(dir);
		rc.yield();
		rc.selfDestruct();
	}

	static void attack(MapLocation target) throws GameActionException {
		rc.attackSquare(target);
		notActive();
	}

	static void move(Direction dir) throws GameActionException {
		if (dir != Direction.OMNI) {
			if (canMove(dir)) {
				rc.move(dir);
				notActive();
			} else {
				ind[3] += "can't move!";
			}
		}
	}

	static void sneak(Direction dir) throws GameActionException {
		if (dir != Direction.OMNI) {
			if (canMove(dir)) {
				rc.sneak(dir);
				notActive();
			} else {
				ind[3] += "can't move!";
			}
		}
	}

	static boolean canMove(Direction dir) throws GameActionException {
		return rc.canMove(dir) && !hqDanger(here.add(dir));
	}

	static boolean hqDanger(MapLocation loc) throws GameActionException {
		return d2(loc, enemyHQ) <= 25;
	}

	static void notActive() {
		active = false;
	}

	// Broadcasting

	static int START = 60000;
	static int CASTE = START;
	static int NOISETOWERCENSUS = START + 1;
	static int PASTRCENSUS = START + 2;
	static int QUEUEDNOISETOWER = START + 3;
	static int QUEUEDPASTR = START + 4;
	static int MODE = START + 5;

	static int ORDERS = 60500;
	static int GOAL = 0;
	static int NTREQ = 1;
	static int PASTRREQ = 2;
	static int ordersWidth = 3;

	static void add(int x, int c) throws GameActionException {
		rc.broadcast(c, rc.readBroadcast(c) + x);
	}

	static int writeLoc(MapLocation a) {
		return a.x * 100 + a.y;
	}

	static MapLocation readLoc(int x) {
		return new MapLocation(x / 100, x % 100);
	}

	// GEOMETRY

	static Direction[] directions = { Direction.NORTH, Direction.NORTH_EAST,
			Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH,
			Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };

	static int d2(MapLocation a) throws GameActionException {
		return here.distanceSquaredTo(a);
	}

	static int d2(MapLocation a, MapLocation b) throws GameActionException {
		return a.distanceSquaredTo(b);
	}

	static Direction rotate(int x, Direction dir) {
		return directions[(8 + x + dir.ordinal()) % 8];
	}

}
