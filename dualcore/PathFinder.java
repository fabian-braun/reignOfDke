package dualcore;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public abstract class PathFinder {

	protected final MapLocation hqSelfLoc;
	protected final MapLocation hqEnemLoc;
	protected final int height;
	protected final int width;
	protected final RobotController rc;

	public PathFinder(RobotController rc, TerrainTile[][] map,
			MapLocation hqSelfLoc, MapLocation hqEnemLoc, int height, int width) {
		this.rc = rc;
		this.hqSelfLoc = hqSelfLoc;
		this.hqEnemLoc = hqEnemLoc;
		this.height = height;
		this.width = width;
	}

	public PathFinder(RobotController rc) {
		this.rc = rc;
		height = rc.getMapHeight();
		width = rc.getMapWidth();
		hqSelfLoc = rc.senseHQLocation();
		hqEnemLoc = rc.senseEnemyHQLocation();
	}

	public static final int distance(int y1, int x1, int y2, int x2) {
		return Math.abs(y2 - y1) + Math.abs(x2 - x1);
	}

	public static final int distance(MapLocation loc1, MapLocation loc2) {
		return distance(loc1.y, loc1.x, loc2.y, loc2.x);
	}

	public boolean isHqLocation(MapLocation loc) {
		return loc.equals(hqEnemLoc) || loc.equals(hqSelfLoc);
	}

	public boolean isXonMap(int x) {
		return x >= 0 && x < width;
	}

	public boolean isYonMap(int y) {
		return y >= 0 && y < height;
	}

	public abstract boolean move() throws GameActionException;

	public abstract boolean sneak() throws GameActionException;

	public abstract void setTarget(MapLocation target);

	public abstract MapLocation getTarget();

}
