package simplePastr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class PathFinderSimple extends PathFinder {

	public PathFinderSimple(TerrainTile[][] map, MapLocation hqSelfLoc,
			MapLocation hqEnemLoc) {
		super(map, hqEnemLoc, hqEnemLoc);
	}

	public PathFinderSimple(RobotController rc) {
		super(rc);
	}

	@Override
	public Direction getNextDirection(
			HashMap<MapLocation, Integer> lastVisited, MapLocation target,
			MapLocation current) {
		List<MapLocation> possibleLocs = new ArrayList<MapLocation>();
		possibleLocs.add(new MapLocation(current.x - 1, current.y - 1));
		possibleLocs.add(new MapLocation(current.x - 1, current.y));
		possibleLocs.add(new MapLocation(current.x - 1, current.y + 1));
		possibleLocs.add(new MapLocation(current.x, current.y - 1));
		possibleLocs.add(new MapLocation(current.x, current.y + 1));
		possibleLocs.add(new MapLocation(current.x + 1, current.y - 1));
		possibleLocs.add(new MapLocation(current.x + 1, current.y));
		possibleLocs.add(new MapLocation(current.x + 1, current.y + 1));
		Direction dir = Direction.NONE;
		int bestRating = Integer.MAX_VALUE;
		for (MapLocation next : possibleLocs) {
			if (next.y < 0 || next.x < 0 || next.y >= map.length
					|| next.x >= map[0].length) {
				// out of range
				continue;
			}
			if (map[next.y][next.x] == TerrainTile.OFF_MAP
					|| map[next.y][next.x] == TerrainTile.VOID
					|| isHqLocation(new MapLocation(next.x, next.y))) {
				continue;
			}
			int dist = distance(next, target);
			if (lastVisited.containsKey(next)) {
				dist += map.length + map[0].length - lastVisited.get(next);
			}
			if (dist < bestRating) {
				bestRating = dist;
				if (next.x > current.x) {
					if (next.y > current.y) {
						dir = Direction.SOUTH_EAST;
					} else if (next.y == current.y) {
						dir = Direction.EAST;
					} else {
						dir = Direction.NORTH_EAST;
					}
				} else if (next.x == current.x) {
					if (next.y > current.y) {
						dir = Direction.SOUTH;
					} else {
						dir = Direction.NORTH;
					}
				} else {
					if (next.y > current.y) {
						dir = Direction.SOUTH_WEST;
					} else if (next.y == current.y) {
						dir = Direction.WEST;
					} else {
						dir = Direction.NORTH_WEST;
					}
				}
			}
		}
		return dir;
	}

}
