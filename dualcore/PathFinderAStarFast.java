package dualcore;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class PathFinderAStarFast extends PathFinder {

	private MapLocation target = new MapLocation(0, 0);
	private PathFinderMLineBug bug;
	protected final TerrainTile[][] map;
	private Stack<MapLocation> path;
	int yCount;
	int xCount;

	public PathFinderAStarFast(RobotController rc) {
		super(rc);
		bug = new PathFinderMLineBug(rc);
		yCount = height / 2 + height % 2;
		xCount = width / 2 + width % 2;
		map = new TerrainTile[yCount][xCount];
		for (int y = 0; y < yCount; y++) {
			for (int x = 0; x < xCount; x++) {
				map[y][x] = rc.senseTerrainTile(new MapLocation(x, y));
			}
		}
	}

	@Override
	public boolean move() throws GameActionException {
		if (path.isEmpty()) {
			System.out.println("A-Star does not know the way");
			return false;
		} else {
			MapLocation myLoc = rc.getLocation();
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
			MapLocation current = open.poll();
			if (current.equals(target))
				return getPath(ancestors, target);
			closed.add(current);
			Set<MapLocation> neighbours = getNeighbours(current);
			for (MapLocation neighbour : neighbours) {
				if (closed.contains(neighbour))
					continue;
				int tentative = gScore.get(current) + 2;
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
			Map<MapLocation, MapLocation> ancestors, MapLocation target) {
		Stack<MapLocation> path = new Stack<MapLocation>();
		MapLocation current = target;
		while (current != null) {
			path.push(current);
			current = ancestors.get(current);
		}
		return path;
	}

	private int calcFScore(MapLocation from, MapLocation to) {
		int distance = distance(from, to);
		if (map[from.y][from.x].equals(TerrainTile.ROAD)) {
			if (distance > 3)
				distance = distance - 3;
		}
		return distance * 4;
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
}
