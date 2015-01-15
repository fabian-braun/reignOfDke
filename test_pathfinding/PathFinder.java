package test_pathfinding;

import java.util.HashSet;
import java.util.Set;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public abstract class PathFinder extends BaseFinder {

	protected final MapLocation hqSelfLoc;
	protected final MapLocation hqEnemLoc;
	protected final int ySize;
	protected final int xSize;
	protected final RobotController rc;
	protected final TerrainTile[][] map;

	public PathFinder(RobotController rc, TerrainTile[][] map,
			MapLocation hqSelfLoc, MapLocation hqEnemLoc, int ySize, int xSize) {
		this.rc = rc;
		this.hqSelfLoc = hqSelfLoc;
		this.hqEnemLoc = hqEnemLoc;
		this.ySize = ySize;
		this.xSize = xSize;
		this.map = map;
	}

	public PathFinder(RobotController rc) {
		this.rc = rc;
		ySize = rc.getMapHeight();
		xSize = rc.getMapWidth();
		hqSelfLoc = rc.senseHQLocation();
		hqEnemLoc = rc.senseEnemyHQLocation();
		map = new TerrainTile[ySize][xSize];
		for (int y = 0; y < ySize; y++) {
			for (int x = 0; x < xSize; x++) {
				map[y][x] = rc.senseTerrainTile(new MapLocation(x, y));
			}
		}
	}

	public static final int distance(int y1, int x1, int y2, int x2) {
		int dx = x1 - x2;
		int dy = y1 - y2;
		if (dx < 0)
			dx *= -1;
		if (dy < 0)
			dy *= -1;
		return dx > dy ? dx : dy;
	}

	public static final int distance(MapLocation loc1, MapLocation loc2) {
		return distance(loc1.y, loc1.x, loc2.y, loc2.x);
	}

	public boolean isHqLocation(MapLocation loc) {
		return loc.equals(hqEnemLoc) || loc.equals(hqSelfLoc);
	}

	public boolean isXonMap(int x) {
		return x >= 0 && x < xSize;
	}

	public boolean isYonMap(int y) {
		return y >= 0 && y < ySize;
	}

	public Set<MapLocation> getNeighbours(MapLocation loc) {
		Set<MapLocation> neighbours = new HashSet<MapLocation>();
		for (int i = 0; i < C.DIRECTIONS.length; i++) {
			MapLocation n = loc.add(C.DIRECTIONS[i]);
			if (isTraversable(n)) {
				neighbours.add(n);
			}
		}
		return neighbours;
	}

	public boolean isTraversable(MapLocation location) {
		return isXonMap(location.x) && isYonMap(location.y)
				&& !map[location.y][location.x].equals(TerrainTile.VOID)
				&& !isHqLocation(location);
	}

	public abstract boolean move() throws GameActionException;

	public abstract boolean sneak() throws GameActionException;

	public abstract void setTarget(MapLocation target);

	public abstract MapLocation getTarget();

}
