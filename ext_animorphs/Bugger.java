package ext_animorphs;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Bugger {
	public Controller c;
	public RobotController rc;
	public MapLocation start;
	public MapLocation from;
	public MapLocation cur;
	public MapLocation to;
	public boolean hugging;
	public boolean wentLeft;
	public boolean hugLeft;
	public Direction hugDir;
	public FastLocDirSet seen;
	public boolean recursed;
	public boolean bugging;
	public Mover m;

	public Direction preDir;
	public DirectionMap map;
	public boolean useMap;

	public Bugger(Controller c) {
		m = new Mover(c);
		this.rc = c.rc;
		this.c = c;
		seen = new FastLocDirSet();
		bugging = false;
		preDir = Direction.NONE;
		map = new DirectionMap(rc);
	}

	public void endBug() {
		bugging = false;
	}

	public void startBug(MapLocation loc) {
		start = rc.getLocation();
		from = rc.getLocation();
		to = loc;
		hugging = false;
		hugLeft = true;
		wentLeft = true;
		seen.clear();
		bugging = true;
		useMap = true;
	}

	// Something went wrong during, restart the bugging.
	private void restart(MapLocation loc) {
		from = rc.getLocation();
		to = loc;
		hugging = false;
		hugLeft = true;
		seen.clear();
	}

	public Direction bug() throws GameActionException {
		cur = rc.getLocation();

		if (useMap) {
			Direction bfsdir = bfsDir();
			if (bfsdir == Direction.OMNI) {
				useMap = false;
			} else if (bfsdir != Direction.NONE) {
				if (canMove(bfsdir)) {
					return bfsdir;
				} else {
					useMap = false; // hit enemyhq or someone in the way
				}
			}
		}

		Direction desiredDir = cur.directionTo(to);// Mover.direction(cur,to);
		if (desiredDir == Direction.NONE || desiredDir == Direction.OMNI) {
			return desiredDir;
		}

		if (hugging) {
			// desiredDir if possible, else left, else right. Makes dist smaller
			Direction bestDir = goInDir(desiredDir);
			if (seen.contains(cur, hugDir)) {
				hugging = false;
			}

			if (bestDir != null) {
				if (canMove(bestDir)
						&& cur.distanceSquaredTo(to) < from
								.distanceSquaredTo(to)) {
					hugging = false;
					return bestDir;
				}
			}
			seen.add(cur, hugDir);
			return hug();
		} else {
			Direction bestDir = goInDir(desiredDir);
			if (bestDir != null) {
				return bestDir;
			}

			// start hugging
			seen.clear();
			hugging = true;
			from = cur;
			hugDir = desiredDir;
			hugLeft = wentLeft;
			recursed = false;
			return hug();
		}
	}

	private Direction oppositeTriTurn(Direction dir) {
		return (hugLeft ? dir.rotateRight().rotateRight().rotateRight() : dir
				.rotateLeft().rotateLeft().rotateLeft());
	}

	private Direction oppositeDuoTurn(Direction dir) {
		return (hugLeft ? dir.rotateRight().rotateRight() : dir.rotateLeft()
				.rotateLeft());
	}

	private Direction handleOffMap() {
		if (!recursed) {
			seen.clear();
			hugLeft = !hugLeft;
			recursed = true;
			return hug();
		} else {
			// something went wrong, reset
			restart(to);
			return Direction.NONE;
		}
	}

	private boolean canMove(Direction dir) {
		return rc.canMove(dir) && !m.inSplashRadius(cur.add(dir));
	}

	private Direction hug() {
		Direction tryDir;
		MapLocation tryLoc;
		int i;
		int width = c.width;
		int height = c.height;
		if (hugLeft) {
			tryDir = hugDir.rotateLeft();
			tryLoc = cur.add(tryDir);
			for (i = 0; i < 8 && !canMove(tryDir); i++) {
				if (tryLoc.x < 0 || tryLoc.y < 0 || tryLoc.x >= width
						|| tryLoc.y >= height) {
					return handleOffMap();
				}

				tryDir = tryDir.rotateLeft();
				tryLoc = cur.add(tryDir);
			}
		} else {
			tryDir = hugDir.rotateRight();
			tryLoc = cur.add(tryDir);
			for (i = 0; i < 8 && !canMove(tryDir); i++) {
				if (tryLoc.x < 0 || tryLoc.y < 0 || tryLoc.x >= width
						|| tryLoc.y >= height) {
					return handleOffMap();
				}

				tryDir = tryDir.rotateRight();
				tryLoc = cur.add(tryDir);
			}
		}

		if (i == 8) {
			// blocked in all directions
			return Direction.NONE;
		} else {
			hugDir = tryDir;
			if (hugDir.isDiagonal()) {
				hugDir = oppositeTriTurn(hugDir);
			} else {
				hugDir = oppositeDuoTurn(hugDir);
			}
			return tryDir;
		}
	}

	public Direction bfsDir() throws GameActionException {
		if (map.readDirection(cur)) {
			Direction dir = map.lastDir;
			MapLocation loc = map.lastLoc;
			// adjacent or touching
			// rc.setIndicatorString(1, Clock.getRoundNum() + "," + dir + ", " +
			// loc + ", " + to);
			if (loc.distanceSquaredTo(to) <= 5) {
				return dir;
			}
			return Direction.NONE;
		} else {
			return Direction.NONE;
		}
	}

	public void calcPreDir() {
		Direction desired = cur.directionTo(to);// Mover.direction(cur,to);
		Direction left = desired.rotateLeft();
		Direction right = desired.rotateRight();
		// magic constant
		double factor = GameConstants.SOLDIER_DIAGONAL_MOVEMENT_ACTION_DELAY_FACTOR;
		double desSensor = desired.isDiagonal() ? factor : 1;
		double leftSensor = desired.isDiagonal() ? factor : 1;
		double rightSensor = desired.isDiagonal() ? factor : 1;
		desSensor *= m.sensorBeam(desired, 8);
		leftSensor *= m.sensorBeam(left, 8);
		rightSensor *= m.sensorBeam(right, 8);

		// manual max comparison
		if (desSensor > leftSensor) {
			if (rightSensor > desSensor) {
				preDir = right;
				return;
			} else {
				preDir = desired;
				return;
			}
		} else {
			if (leftSensor > rightSensor) {
				preDir = left;
				return;
			} else {
				preDir = right;
				return;
			}
		}
	}

	public Direction goInDir(Direction desiredDir) {
		// boolean preDirMove = canMove(preDir);
		// Direction tempPreDir = preDir;
		// preDir = Direction.NONE;
		// if (preDirMove) {
		// return tempPreDir;
		// }

		if (canMove(desiredDir)) {
			return desiredDir;
		}

		Direction left = desiredDir.rotateLeft();
		Direction right = desiredDir.rotateRight();
		boolean leftIsBetter = (cur.add(left).distanceSquaredTo(to) < cur.add(
				right).distanceSquaredTo(to));
		if (leftIsBetter) {
			if (canMove(left)) {
				wentLeft = true;
				return left;
			}

			if (canMove(right)) {
				wentLeft = false;
				return right;
			}
		} else {
			if (canMove(right)) {
				wentLeft = false;
				return right;
			}

			if (canMove(left)) {
				wentLeft = true;
				return left;
			}
		}
		return null;
	}
}
