package simplePastr;

import java.util.HashMap;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public abstract class PathFinder {

    protected final TerrainTile[][] map;
    protected final MapLocation hqSelfLoc;
    protected final MapLocation hqEnemLoc;

    public PathFinder(TerrainTile[][] map, MapLocation hqSelfLoc,
	    MapLocation hqEnemLoc) {
	this.map = map;
	this.hqSelfLoc = hqSelfLoc;
	this.hqEnemLoc = hqEnemLoc;
    }

    public PathFinder(RobotController rc) {
	int height = rc.getMapHeight();
	int width = rc.getMapWidth();
	map = new TerrainTile[height][width];
	for (int y = 0; y < height; y++) {
	    for (int x = 0; x < width; x++) {
		map[y][x] = rc.senseTerrainTile(new MapLocation(x, y));
	    }
	}
	hqSelfLoc = rc.senseHQLocation();
	hqEnemLoc = rc.senseEnemyHQLocation();
    }

    public static final int distance(int y1, int x1, int y2, int x2) {
	return Math.abs(y2 - y1) + Math.abs(x2 - x1);
    }

    public static final int distance(MapLocation loc1, MapLocation loc2) {
	return distance(loc1.y, loc1.x, loc2.y, loc2.x);
    }

    public abstract Direction getNextDirection(
	    HashMap<MapLocation, Integer> lastVisited, MapLocation target,
	    MapLocation current);

    public boolean isHqLocation(MapLocation loc) {
	return loc.equals(hqEnemLoc) || loc.equals(hqSelfLoc);
    }

}
