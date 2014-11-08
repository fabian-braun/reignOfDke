package teamreignofdke;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class PathFinderSnailTrail extends PathFinder {

	private HashMap<MapLocation, Integer> lastVisited = new HashMap<MapLocation, Integer>();
	private MapLocation target;

	public PathFinderSnailTrail(RobotController rc, TerrainTile[][] map,
			MapLocation hqSelfLoc, MapLocation hqEnemLoc, int height, int width) {
		super(rc, map, hqEnemLoc, hqEnemLoc, height, width);
	}

	public PathFinderSnailTrail(RobotController rc) {
		super(rc);
	}

	private Direction getNextDirection() {
		MapLocation current = rc.getLocation();
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
				dir = current.directionTo(next);
			}
		}
		return dir;
	}

	@Override
	public boolean move() throws GameActionException {
		Direction dir = getNextDirection();
		if (rc.canMove(dir)) {
			rc.move(dir);
			return true;
		}
		return false;
	}

	@Override
	public boolean sneak() throws GameActionException {
		Direction dir = getNextDirection();
		if (rc.canMove(dir)) {
			rc.sneak(dir);
			return true;
		}
		return false;
	}

	@Override
	public void setTarget(MapLocation target) {
		lastVisited.clear();
		this.target = target;
	}

	@Override
	public MapLocation getTarget() {
		return target;
	}
}
