package reignierOfDKE;

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
	private MapLocation current;

	public PathFinderSnailTrail(RobotController rc, TerrainTile[][] map,
			MapLocation hqSelfLoc, MapLocation hqEnemLoc, int ySize, int xSize,
			int soldierId) {
		super(rc, map, hqEnemLoc, hqEnemLoc, ySize, xSize, soldierId);
		current = rc.getLocation();
	}

	public PathFinderSnailTrail(RobotController rc, int soldierId) {
		super(rc, soldierId);
		current = rc.getLocation();
	}

	private Direction getNextDirection() {
		MapLocation current = rc.getLocation();
		lastVisited.put(current, Clock.getRoundNum());
		Set<MapLocation> neighbours = getNeighbours(current);
		Direction dir = Direction.NONE;
		int bestRating = Integer.MAX_VALUE;
		for (MapLocation next : neighbours) {
			int dist = getRequiredMoves(next, target);
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
		if (!current.equals(rc.getLocation())) {
			// moves have been performed outside of this class
			// therefore lastVisited is not valid anymore
			lastVisited.clear();
		}
		Direction dir = getNextDirection();
		if (rc.canMove(dir)) {
			rc.move(dir);
			current = rc.getLocation();
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

	@Override
	public boolean isTargetReached() {
		return rc.getLocation().equals(target);
	}
}
