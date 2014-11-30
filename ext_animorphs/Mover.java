package ext_animorphs;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

/**
 * Helper class for moving. Contains no state (except
 * RobotController/Controller)
 * 
 * @author Jeffrey
 *
 */
public class Mover {
	public RobotController rc;
	public Controller c;
	public static final int CHECK_DISTANCE = 3;

	// DIR[Y][X]
	public static final Direction[][] DIR = new Direction[][] {
			new Direction[] { Direction.NORTH_WEST, Direction.NORTH,
					Direction.NORTH_EAST },
			new Direction[] { Direction.WEST, Direction.OMNI, Direction.EAST },
			new Direction[] { Direction.SOUTH_WEST, Direction.SOUTH,
					Direction.SOUTH_EAST } };

	// OPP_DIR[Y][X]
	public static final Direction[][] OPP_DIR = new Direction[][] {
			new Direction[] { Direction.SOUTH_EAST, Direction.SOUTH,
					Direction.SOUTH_WEST },
			new Direction[] { Direction.EAST, Direction.OMNI, Direction.WEST },
			new Direction[] { Direction.NORTH_EAST, Direction.NORTH,
					Direction.NORTH_WEST } };

	public Mover(Controller c) {
		this.c = c;
		this.rc = c.rc;
	}

	public static int calcSplashRadiusSq(int x) {
		int sp = Controller.SPLASH_RADIUS_SQ;
		// (sqrt(x)+sqrt(2))^2 <= x + ceil(sqrt(2x)*2) + 2
		return x + ((int) Math.ceil((Math.sqrt(sp * x) * sp) + 1)) + sp;
	}

	public boolean inSplashRadius(MapLocation loc) {
		Direction dir = loc.directionTo(c.enemyhq);
		return loc.add(dir).distanceSquaredTo(c.enemyhq) <= Controller.HQ_ATTACK_RADIUS_SQ;
	}

	public boolean inDangerRadius(MapLocation loc) {
		Direction dir = loc.directionTo(c.enemyhq);
		return loc.add(dir).add(dir).distanceSquaredTo(c.enemyhq) <= Controller.HQ_ATTACK_RADIUS_SQ;
	}

	public static Direction direction(MapLocation a, MapLocation b) {
		int x = Integer.signum(b.x - a.x) + 1;
		int y = Integer.signum(b.y - a.y) + 1;
		return DIR[y][x];
	}

	public Direction moveInDir(Direction dir) {
		if (rc.canMove(dir)) {
			return dir;
		}
		Direction left = dir.rotateLeft();
		if (rc.canMove(left)) {
			return left;
		}

		Direction right = dir.rotateRight();
		if (rc.canMove(right)) {
			return right;
		}
		return Direction.NONE;
	}

	public int sensorBeam(Direction dir, int limit) {
		MapLocation loc = rc.getLocation().add(dir);
		for (int i = 1; i <= limit; i++) {
			if (rc.senseTerrainTile(loc) == TerrainTile.VOID) {
				return i - 1;
			}
			loc = loc.add(dir);
		}
		return limit;
	}

	public Direction smartMove(Direction dir) {
		int count = 0;
		MapLocation nextLocation = rc.getLocation().add(dir);

		while ((rc.senseTerrainTile(nextLocation) == TerrainTile.NORMAL || rc
				.senseTerrainTile(nextLocation) == TerrainTile.ROAD)
				&& count < CHECK_DISTANCE) {
			count++;
			nextLocation = nextLocation.add(dir);
		}

		int countLeft = 0;
		Direction dirLeft = dir.rotateLeft();
		MapLocation nextLocationLeft = rc.getLocation().add(dirLeft);

		while ((rc.senseTerrainTile(nextLocationLeft) == TerrainTile.NORMAL || rc
				.senseTerrainTile(nextLocationLeft) == TerrainTile.ROAD)
				&& countLeft < CHECK_DISTANCE) {
			countLeft++;
			nextLocationLeft = nextLocationLeft.add(dirLeft);
		}

		int countRight = 0;
		Direction dirRight = dir.rotateRight();
		MapLocation nextLocationRight = rc.getLocation().add(dirRight);

		while ((rc.senseTerrainTile(nextLocationRight) == TerrainTile.NORMAL || rc
				.senseTerrainTile(nextLocationRight) == TerrainTile.ROAD)
				&& countRight < CHECK_DISTANCE) {
			countRight++;
			nextLocationRight = nextLocationRight.add(dirRight);
		}

		if (count >= countLeft && count >= countRight && rc.canMove(dir)) {
			return dir;
		} else if (countLeft >= countRight && rc.canMove(dirLeft)) {
			return dirLeft;
		} else if (rc.canMove(dirRight)) {
			return dirRight;
		}
		return Direction.NONE;
	}
}
