package dualcore;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class PathFinderAStar extends PathFinder {

	private Map<MapLocation, MapLocation> fromTo = new HashMap<MapLocation, MapLocation>();
	private MapLocation target = new MapLocation(0, 0);
	protected final TerrainTile[][] map;

	public PathFinderAStar(RobotController rc) {
		super(rc);
		map = new TerrainTile[height][width];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				map[y][x] = rc.senseTerrainTile(new MapLocation(x, y));
			}
		}
	}

	public PathFinderAStar(RobotController rc, TerrainTile[][] map,
			MapLocation hqSelfLoc, MapLocation hqEnemLoc, int height, int width) {
		super(rc, hqEnemLoc, hqEnemLoc, height, width);
		this.map = map;
	}

	@Override
	public boolean move() throws GameActionException {
		MapLocation next = fromTo.get(rc.getLocation());
		if (null == next) {
			System.out.println("A-Start does not know the way");
			return false;
		} else {
			Direction dir = rc.getLocation().directionTo(next);
			if (rc.canMove(dir)) {
				rc.move(dir);
				return true;
			} else {
				return false;
			}
		}
	}

	@Override
	public boolean sneak() throws GameActionException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setTarget(MapLocation target) {
		int byteCodeLeftStart = Clock.getBytecodesLeft();
		int roundsStart = Clock.getRoundNum();
		this.target = target;
		MapLocation current = rc.getLocation();
		fromTo = aStar(current, target);
		int roundsEnd = Clock.getRoundNum();
		int byteCodeLeftEnd = Clock.getBytecodesLeft();
		System.out.println("Started in round " + roundsStart
				+ " with bytecode left " + byteCodeLeftStart
				+ ". Ended in round " + roundsEnd + " with bytecode left "
				+ byteCodeLeftEnd);
		while (!target.equals(current) && current != null) {
			current = fromTo.get(current);
			System.out.println("--> " + current);
		}
	}

	@Override
	public MapLocation getTarget() {
		return target;
	}

	private Map<MapLocation, MapLocation> aStar(MapLocation start,
			final MapLocation target) {
		Map<MapLocation, MapLocation> fromTo = new HashMap<MapLocation, MapLocation>();
		Map<MapLocation, Integer> gScore = new HashMap<MapLocation, Integer>();
		final Map<MapLocation, Integer> fScore = new HashMap<MapLocation, Integer>();
		Comparator<MapLocation> comparator = new Comparator<MapLocation>() {
			@Override
			public int compare(MapLocation o1, MapLocation o2) {
				return Integer.compare(fScore.get(o1), fScore.get(o2));
			}
		};
		PriorityQueue<MapLocation> open = new PrioQueueNoDuplicates<MapLocation>(
				20, comparator);
		Set<MapLocation> closed = new HashSet<MapLocation>();

		open.add(start);
		gScore.put(start, 0);
		fScore.put(start, calcFScore(start, target));

		// start algorithm
		while (!open.isEmpty()) {
			MapLocation current = open.poll();
			if (current.equals(target))
				return fromTo;
			closed.add(current);
			Set<MapLocation> neighbours = getNeighbours(current);
			for (MapLocation neighbour : neighbours) {
				if (closed.contains(neighbour))
					continue;
				int tentative = gScore.get(current) + 2;
				if (open.contains(neighbour)
						&& tentative >= gScore.get(neighbour))
					continue;
				fromTo.put(current, neighbour);
				gScore.put(neighbour, tentative);
				fScore.put(neighbour, tentative + calcFScore(neighbour, target));
				// if (!open.contains(neighbour))
				open.add(neighbour);
			}
		}
		// no path exists
		return new HashMap<MapLocation, MapLocation>();
	}

	private int calcFScore(MapLocation from, MapLocation to) {
		int distance = distance(from, to);
		// if (map[from.y][from.x].equals(TerrainTile.ROAD)) {
		// distance = (distance * 3) / 4;
		// }
		return distance;
	}

	private Set<MapLocation> getNeighbours(MapLocation loc) {
		Set<MapLocation> neighbours = new HashSet<MapLocation>();
		for (int i = 0; i < C.DIRECTIONS.length; i++) {
			MapLocation n = loc.add(C.DIRECTIONS[i]);
			if (n.x < width
					&& n.y < height
					&& n.x >= 0
					&& n.y >= 0
					&& (map[n.y][n.x].equals(TerrainTile.NORMAL) || map[n.y][n.x]
							.equals(TerrainTile.ROAD))) {
				neighbours.add(n);
			}
		}
		return neighbours;
	}
}
