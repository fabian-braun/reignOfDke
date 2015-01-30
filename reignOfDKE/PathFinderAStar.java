package reignOfDKE;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class PathFinderAStar extends PathFinder {

	private MapLocation target = new MapLocation(-1, -1);
	private Stack<MapLocation> path;
	public static final int weightedAStarMultiplicator = 3;

	public PathFinderAStar(RobotController rc, int soldierId) {
		super(rc, soldierId);
	}

	public PathFinderAStar(RobotController rc, int soldierId,
			TerrainTile[][] map, MapLocation hqSelfLoc, MapLocation hqEnemLoc,
			int ySize, int xSize) {
		super(rc, map, hqSelfLoc, hqEnemLoc, ySize, xSize, soldierId);
	}

	@Override
	public boolean move() throws GameActionException {
		if (path.isEmpty()) {
			return false;
		} else {
			MapLocation myLoc = rc.getLocation();
			while (myLoc.equals(path.peek())) {
				// we are already here due to some movement performed outside of
				// this class
				path.pop();
			}
			if (!myLoc.isAdjacentTo(path.peek())) {
				path = aStar(myLoc, target);
			}
			MapLocation next = path.peek();
			Direction dir = rc.getLocation().directionTo(next);
			if (rc.canMove(dir)) {
				rc.move(dir);
				path.pop();
				return true;
			} else {
				return false;
			}
		}
	}

	@Override
	public boolean sneak() throws GameActionException {
		return false;
	}

	@Override
	public void setTarget(MapLocation target) {
		this.target = target;
		MapLocation current = rc.getLocation();
		path = aStar(current, target);
	}

	@Override
	public MapLocation getTarget() {
		return target;
	}

	public Stack<MapLocation> aStar(MapLocation start, final MapLocation target) {
		Map<MapLocation, MapLocation> ancestors = new HashMap<MapLocation, MapLocation>();
		Map<MapLocation, Integer> gScore = new HashMap<MapLocation, Integer>();
		final Map<MapLocation, Integer> fScore = new HashMap<MapLocation, Integer>();
		MapLocationPriorityQueue open = new MapLocationPriorityQueue(ySize
				* xSize, fScore);
		MapLocationSet closed = new MapLocationSet(ySize * xSize);

		gScore.put(start, 0);
		fScore.put(start, calcFScore(start, target));
		open.add(start);

		// start algorithm
		while (!open.isEmpty()) {
			// broadcast being alive
			Channel.signalAlive(rc, soldierId);
			MapLocation current = open.poll();
			if (current.equals(target))
				return getPath(ancestors, target, start);
			closed.add(current);
			MapLocationSet neighbours = getNeighbours(current);
			for (MapLocation neighbour : neighbours.array) {
				if (neighbour == null || closed.contains(neighbour))
					continue;
				int tentative = gScore.get(current)
						+ getManhattanDist(current, neighbour);
				if (open.contains(neighbour)
						&& tentative >= gScore.get(neighbour))
					continue;
				ancestors.put(neighbour, current);
				gScore.put(neighbour, tentative);
				fScore.put(neighbour, tentative + calcFScore(neighbour, target));
				if (!open.contains(neighbour))
					open.add(neighbour);
			}
		}
		// no path exists
		return new Stack<MapLocation>();
	}

	public static Stack<MapLocation> getPath(
			Map<MapLocation, MapLocation> ancestors, MapLocation target,
			MapLocation origin) {
		Stack<MapLocation> path = new Stack<MapLocation>();
		MapLocation current = target;
		while (!origin.equals(current)) {
			path.push(current);
			current = ancestors.get(current);
		}
		// save. fix bug
		if (path.isEmpty()) {
			path.push(target);
		}
		return path;
	}

	private int calcFScore(MapLocation from, MapLocation to) {
		int distance = getManhattanDist(from, to) * weightedAStarMultiplicator;
		if (map[from.y][from.x].equals(TerrainTile.ROAD)) {
			if (distance > 2)
				distance = distance - 2;
		}
		return distance;
	}

	@Override
	public boolean isTargetReached() {
		return rc.getLocation().equals(target);
	}

}
