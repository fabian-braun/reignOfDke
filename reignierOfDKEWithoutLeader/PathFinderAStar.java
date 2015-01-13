package reignierOfDKEWithoutLeader;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class PathFinderAStar extends PathFinder {

	private MapLocation target = new MapLocation(0, 0);
	private Stack<MapLocation> path; // = new Stack<MapLocation>();
	public static final int weightedAStarMultiplicator = 3;
	private int soldierId = -1;

	public PathFinderAStar(RobotController rc, int soldierId) {
		super(rc);
		this.soldierId = soldierId;
	}

	public PathFinderAStar(RobotController rc, int soldierId,
			TerrainTile[][] map, MapLocation hqSelfLoc, MapLocation hqEnemLoc,
			int ySize, int xSize) {
		super(rc, map, hqSelfLoc, hqEnemLoc, ySize, xSize);
		this.soldierId = soldierId;
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
		// TODO: implement
		return false;
	}

	@Override
	public void setTarget(MapLocation target) {
		this.target = target;
		MapLocation current = rc.getLocation();
		path = aStar(current, target);
		// printPath(path);
	}

	@Override
	public MapLocation getTarget() {
		return target;
	}

	public Stack<MapLocation> aStar(MapLocation start, final MapLocation target) {
		Map<MapLocation, MapLocation> ancestors = new HashMap<MapLocation, MapLocation>();
		Map<MapLocation, Integer> gScore = new HashMap<MapLocation, Integer>();
		final Map<MapLocation, Integer> fScore = new HashMap<MapLocation, Integer>();
		Comparator<MapLocation> comparator = new Comparator<MapLocation>() {
			@Override
			public int compare(MapLocation o1, MapLocation o2) {
				return Integer.compare(fScore.get(o1), fScore.get(o2));
			}
		};
		PriorityQueue<MapLocation> open = new PriorityQueue<MapLocation>(20,
				comparator);
		Set<MapLocation> closed = new HashSet<MapLocation>();

		open.add(start);
		gScore.put(start, 0);
		fScore.put(start, calcFScore(start, target));

		// start algorithm
		while (!open.isEmpty()) {
			// broadcast being alive
			Channel.signalAlive(rc, soldierId);
			MapLocation current = open.poll();
			if (current.equals(target))
				return getPath(ancestors, target, start);
			closed.add(current);
			Set<MapLocation> neighbours = getNeighbours(current);
			for (MapLocation neighbour : neighbours) {
				if (closed.contains(neighbour))
					continue;
				int tentative = gScore.get(current)
						+ calcFScore(current, neighbour);
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

	private void printPath(Stack<MapLocation> path) {
		Iterator<MapLocation> iterator = path.iterator();
		System.out.println(getClass().getSimpleName() + ":\n"
				+ mapToString(map, iterator));
	}

}
