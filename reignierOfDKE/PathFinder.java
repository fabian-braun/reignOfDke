package reignierOfDKE;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public abstract class PathFinder {

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

	public static final int getRequiredMoves(int y1, int x1, int y2, int x2) {
		int dx = x1 - x2;
		int dy = y1 - y2;
		if (dx < 0)
			dx *= -1;
		if (dy < 0)
			dy *= -1;
		return dx > dy ? dx : dy;
	}

	public static final int getEuclidianDist(int y1, int x1, int y2, int x2) {
		int dx = x1 - x2;
		int dy = y1 - y2;
		return (int) Math.sqrt(dx * dx + dy * dy);
	}

	public static final int getManhattanDist(int y1, int x1, int y2, int x2) {
		int dx = x1 - x2;
		int dy = y1 - y2;
		if (dx < 0)
			dx *= -1;
		if (dy < 0)
			dy *= -1;
		return dx + dy;
	}

	public static final int getRequiredMoves(MapLocation loc1, MapLocation loc2) {
		return getRequiredMoves(loc1.y, loc1.x, loc2.y, loc2.x);
	}

	public static final int getEuclidianDist(MapLocation loc1, MapLocation loc2) {
		return getEuclidianDist(loc1.y, loc1.x, loc2.y, loc2.x);
	}

	public static final int getManhattanDist(MapLocation loc1, MapLocation loc2) {
		return getManhattanDist(loc1.y, loc1.x, loc2.y, loc2.x);
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

	public static boolean isXonMap(int x, TerrainTile[][] map) {
		return x >= 0 && x < map[0].length;
	}

	public static boolean isYonMap(int y, TerrainTile[][] map) {
		return y >= 0 && y < map.length;
	}

	public Set<MapLocation> getNeighbours(MapLocation loc) {
		Set<MapLocation> neighbours = new HashSet<MapLocation>();
		for (int i = 0; i < C.DIRECTIONS.length; i++) {
			MapLocation n = loc.add(C.DIRECTIONS[i]);
			if (isTraversableAndNotHq(n)) {
				neighbours.add(n);
			}
		}
		return neighbours;
	}

	public boolean isTraversableAndNotHq(MapLocation location) {
		return isTraversable(location, map) && !isHqLocation(location);
	}

	public static boolean isTraversable(MapLocation location,
			TerrainTile[][] map) {
		return isXonMap(location.x, map) && isYonMap(location.y, map)
				&& !map[location.y][location.x].equals(TerrainTile.VOID);
	}

	public abstract boolean move() throws GameActionException;

	public abstract boolean sneak() throws GameActionException;

	public abstract void setTarget(MapLocation target);

	public abstract MapLocation getTarget();

	public abstract boolean isTargetReached();

	public static String mapToString(TerrainTile[][] map) {
		char[][] mapRepresentation = mapToCharArray(map);
		StringBuilder sb = new StringBuilder();
		sb.append("  ");
		for (int x = 0; x < mapRepresentation[0].length; x++) {
			sb.append(String.format("%3d", x));
		}
		sb.append("\n");
		for (int y = 0; y < mapRepresentation.length; y++) {
			sb.append(String.format("%2d", y));
			for (int x = 0; x < mapRepresentation[y].length; x++) {
				sb.append("  " + mapRepresentation[y][x]);
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	public static String mapToString(TerrainTile[][] map,
			Iterator<MapLocation> path) {
		char[][] mapRepresentation = mapToCharArray(map);
		String locations = "";
		int i = 0;
		while (path.hasNext()) {
			MapLocation loc = path.next();
			locations += "->" + locToString(loc);
			mapRepresentation[loc.y][loc.x] = ("" + i)
					.charAt(("" + i).length() - 1); // ugly
			i++;
		}
		StringBuilder sb = new StringBuilder(locations + "\n");
		sb.append("  ");
		for (int x = 0; x < mapRepresentation[0].length; x++) {
			sb.append(String.format("%3d", x));
		}
		sb.append("\n");
		for (int y = 0; y < mapRepresentation.length; y++) {
			sb.append(String.format("%2d", y));
			for (int x = 0; x < mapRepresentation[y].length; x++) {
				sb.append("  " + mapRepresentation[y][x]);
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	public static char[][] mapToCharArray(TerrainTile[][] map) {
		char[][] mapRepresentation = new char[map.length][map[0].length];
		for (int y = 0; y < map.length; y++) {
			for (int x = 0; x < map[0].length; x++) {
				TerrainTile tile = map[y][x];
				switch (tile) {
				case NORMAL:
					mapRepresentation[y][x] = '.';
					break;
				case ROAD:
					mapRepresentation[y][x] = '#';
					break;
				default:
					mapRepresentation[y][x] = 'X';
				}
			}
		}
		return mapRepresentation;

	}

	public static String locToString(MapLocation loc) {
		return "(" + loc.y + ";" + loc.x + ")";
	}
}
