package dualcore;

import java.util.HashMap;
import java.util.Set;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class PathFinderSnailTrail extends PathFinder {

	private HashMap<MapLocation, Integer> lastVisited = new HashMap<MapLocation, Integer>();
	private MapLocation target;

	public PathFinderSnailTrail(RobotController rc, TerrainTile[][] map,
			MapLocation hqSelfLoc, MapLocation hqEnemLoc, int ySize, int xSize) {
		super(rc, map, hqEnemLoc, hqEnemLoc, ySize, xSize);
	}

	public PathFinderSnailTrail(RobotController rc) {
		super(rc);
	}

	private Direction getNextDirection() {
		MapLocation current = rc.getLocation();
		lastVisited.put(current, Clock.getRoundNum());
		Set<MapLocation> neighbours = getNeighbours(current);
		Direction dir = Direction.NONE;
		int bestRating = Integer.MAX_VALUE;
		for (MapLocation next : neighbours) {
			int dist = distance(next, target);
			if (lastVisited.containsKey(next)) {
				// this tile has been visited. Make the rating worse: increase
				// it by roundnumber of last visit + max distance on this map
				dist += lastVisited.get(next) + ySize + xSize;
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
