package reignierOfDKE;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class PathFinderAStarFast extends PathFinder {

	private MapLocation target = new MapLocation(-1, -1);
	private MapLocation tempTarget = new MapLocation(-1, -1);
	public static final int weightedAStarMultiplicator = 2;

	private PathFinder internalPF;

	// variables for reduced map
	private MapLocation targetR = new MapLocation(-1, -1);
	private MapLocation tempTargetR = new MapLocation(-1, -1);
	private Stack<MapLocation> pathR;
	private TerrainTile[][] mapR;
	private int ySizeR;
	private int xSizeR;
	private int yDivisor = 1;
	private int xDivisor = 1;
	public static final int reducedDim = 20;

	public PathFinderAStarFast(RobotController rc, int soldierId) {
		super(rc, soldierId);

		// create reduced map
		ySizeR = convertNyRy(ySize);
		while (ySizeR > reducedDim) {
			yDivisor++;
			ySizeR = convertNyRy(ySize);
		}
		xSizeR = convertNxRx(xSize);
		while (xSizeR > reducedDim) {
			xDivisor++;
			xSizeR = convertNxRx(xSize);
		}
		ySizeR += ((ySize % yDivisor) > 0 ? 1 : 0);
		xSizeR += ((xSize % xDivisor) > 0 ? 1 : 0);

		internalPF = new PathFinderAStar(rc, soldierId, map, hqSelfLoc,
				hqEnemLoc, ySize, xSize);
		mapR = new TerrainTile[ySizeR][xSizeR];
		for (int y = 0; y < ySizeR; y++) {
			for (int x = 0; x < xSizeR; x++) {
				Channel.signalAlive(rc, soldierId);
				TerrainTile cachedTerrain = Channel.getReducedMapTerrain(rc, y,
						x);
				if (!cachedTerrain.equals(TerrainTile.OFF_MAP)) {
					mapR[y][x] = cachedTerrain;
					continue;
				}
				Set<MapLocation> corresp = getCorresponding(y, x);
				int norm = 0; // traversable tiles
				int road = 0; // road tiles
				int bloc = 0; // blocked tiles
				for (MapLocation loc : corresp) {
					if (map[loc.y][loc.x].equals(TerrainTile.NORMAL)) {
						norm++;
					} else if (map[loc.y][loc.x].equals(TerrainTile.ROAD)) {
						road++;
					} else {
						bloc++;
					}
				}
				// if there are more traversable tiles than void, the
				// square is treated as traversable.
				if (norm + road >= bloc) {
					if (road * 2 > norm) {
						mapR[y][x] = TerrainTile.ROAD;
					} else {
						mapR[y][x] = TerrainTile.NORMAL;
					}
				} else {
					mapR[y][x] = TerrainTile.VOID;
				}

				// cache the terrain for other soldiers
				Channel.setReducedMapTerrain(rc, y, x, mapR[y][x]);
			}
		}
		// System.out.println(mapToString(map));
		// System.out.println(mapToString(mapR));
		// System.out.println("yDivisor,xDivisor = " + yDivisor + "," +
		// xDivisor);

	}

	// convert normal y-value to corresponding y-value on reduced map
	private int convertNyRy(int y) {
		return (y / yDivisor);
	}

	// convert normal x-value to corresponding x-value on reduced map
	private int convertNxRx(int x) {
		return (x / xDivisor);
	}

	private Set<MapLocation> getCorresponding(int yR, int xR) {
		Set<MapLocation> corresp = new HashSet<MapLocation>();
		for (int y = yR * yDivisor; y < yR * yDivisor + yDivisor; y++) {
			for (int x = xR * xDivisor; x < xR * xDivisor + xDivisor; x++) {
				if (isXonMap(x) && isYonMap(y)) {
					corresp.add(new MapLocation(x, y));
				}
			}
		}
		return corresp;
	}

	private MapLocation getCorrespondingTempTarget(int yR, int xR) {
		if (isCorresponding(target, yR, xR)) {
			return target;
		}
		MapLocation current = rc.getLocation();
		MapLocation currentR = new MapLocation(convertNxRx(current.x),
				convertNyRy(current.y));
		MapLocation tempTarget;

		// System.out.println("currentR = " + locToString(currentR)
		// + " ; targetR = " + locToString(new MapLocation(xR, yR)));
		if (currentR.x < xR && currentR.y < yR) { // top left
			// System.out.println("top left");
			tempTarget = new MapLocation(xR * xDivisor, yR * yDivisor);
		} else if (currentR.x == xR && currentR.y < yR) { // top
			// System.out.println("top");
			tempTarget = new MapLocation(current.x, yR * yDivisor);
		} else if (currentR.x > xR && currentR.y < yR) { // top right
			// System.out.println("top right");
			tempTarget = new MapLocation((currentR.x * xDivisor) - 1, yR
					* yDivisor);
		} else if (currentR.x < xR && currentR.y == yR) { // left
			// System.out.println("left");
			tempTarget = new MapLocation(xR * xDivisor, current.y);
		} else if (currentR.x > xR && currentR.y == yR) { // right
			// System.out.println("right");
			tempTarget = new MapLocation((currentR.x * xDivisor) - 1, current.y);
		} else if (currentR.x < xR && currentR.y > yR) { // bottom left
			// System.out.println("bottom left");
			tempTarget = new MapLocation(xR * xDivisor,
					(currentR.y * yDivisor) - 1);
		} else if (currentR.x == xR && currentR.y > yR) { // bottom
			// System.out.println("bottom");
			tempTarget = new MapLocation(current.x, (currentR.y * yDivisor) - 1);
		} else if (currentR.x > xR && currentR.y > yR) { // bottom right
			// System.out.println("bottom right");
			tempTarget = new MapLocation((currentR.x * xDivisor) - 1,
					(currentR.y * yDivisor) - 1);
		} else {
			System.out
					.println("this should not happen: relative position is wrong");
			tempTarget = getCorrespondingTempTargetSimple(yR, xR);
		}
		if (!isTraversableAndNotHq(tempTarget)) {
			tempTarget = getCorrespondingTempTargetSimple(yR, xR);
		}
		return tempTarget;
	}

	private MapLocation getCorrespondingTempTargetSimple(int yR, int xR) {
		// System.out.println("call to getCorrespondingTempTargetSimple");
		if (isCorresponding(target, yR, xR)) {
			return target;
		}
		for (int y = yR * yDivisor; y < yR * yDivisor + yDivisor; y++) {
			for (int x = xR * xDivisor; x < xR * xDivisor + xDivisor; x++) {
				tempTarget = new MapLocation(x, y);
				if (isTraversableAndNotHq(tempTarget)) {
					return tempTarget;
				}
			}
		}
		// System.out.println("fallback case in getCorrespondingTempTargetSimple");
		return target;
	}

	private boolean isCorresponding(MapLocation loc, int yR, int xR) {
		int yMin = yR * yDivisor;
		int yMax = yR * yDivisor + yDivisor;
		int xMin = xR * xDivisor;
		int xMax = xR * xDivisor + xDivisor;
		return loc.y >= yMin && loc.y < yMax && loc.x >= xMin && loc.x < xMax;
	}

	@Override
	public boolean move() throws GameActionException {
		if (isCorresponding(rc.getLocation(), tempTargetR.y, tempTargetR.x)) {
			if (!pathR.isEmpty()) {
				tempTargetR = pathR.pop();
				tempTarget = getCorrespondingTempTarget(tempTargetR.y,
						tempTargetR.x);
			} else {
				tempTarget = target;
			}
			internalPF.setTarget(tempTarget);
		}
		return internalPF.move();
	}

	@Override
	public boolean sneak() throws GameActionException {
		// TODO: implement
		return false;
	}

	@Override
	public void setTarget(MapLocation target) {
		this.target = target;
		this.targetR = new MapLocation(convertNxRx(target.x),
				convertNyRy(target.y));
		MapLocation current = rc.getLocation();
		MapLocation currentR = new MapLocation(convertNxRx(current.x),
				convertNyRy(current.y));
		pathR = aStar(currentR, targetR, mapR);
		// printPath(pathR);
		// printReducedPath(pathR);
		tempTargetR = pathR.pop();
		tempTarget = getCorrespondingTempTarget(tempTargetR.y, tempTargetR.x);
		internalPF.setTarget(tempTarget);
	}

	@SuppressWarnings("unused")
	private void printPath(Stack<MapLocation> path) {
		Iterator<MapLocation> iterator = path.iterator();
		List<MapLocation> list = new ArrayList<MapLocation>();
		while (iterator.hasNext()) {
			MapLocation locR = iterator.next();
			list.add(getCorrespondingTempTarget(locR.y, locR.x));
		}
		System.out.println(getClass().getSimpleName() + ":\n"
				+ mapToString(map, list.iterator()));
	}

	@SuppressWarnings("unused")
	private void printReducedPath(Stack<MapLocation> path) {
		Iterator<MapLocation> iterator = path.iterator();
		System.out.println(getClass().getSimpleName() + ":\n"
				+ mapToString(mapR, iterator));
	}

	@Override
	public MapLocation getTarget() {
		return target;
	}

	public Stack<MapLocation> aStar(MapLocation start,
			final MapLocation target, TerrainTile[][] map) {
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
				return PathFinderAStar.getPath(ancestors, target, start);
			closed.add(current);
			Set<MapLocation> neighbours = getNeighboursR(current);
			for (MapLocation neighbour : neighbours) {
				if (closed.contains(neighbour))
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
		// no path exists, make sure we at least have a target
		Stack<MapLocation> noPathStack = new Stack<MapLocation>();
		noPathStack.push(this.target);
		return noPathStack;
	}

	private int calcFScore(MapLocation from, MapLocation to) {
		int distance = getManhattanDist(from.y, from.x, to.y, to.x)
				* weightedAStarMultiplicator;
		return distance;
	}

	// for reduced map
	protected Set<MapLocation> getNeighboursR(MapLocation locR) {
		Set<MapLocation> neighbours = new HashSet<MapLocation>();
		for (int i = 0; i < C.DIRECTIONS.length; i++) {
			MapLocation n = locR.add(C.DIRECTIONS[i]);
			if (isTraversableR(n)) {
				neighbours.add(n);
			}
		}
		return neighbours;
	}

	// for reduced map
	protected boolean isTraversableR(MapLocation locR) {
		return isXonMap(locR.x, mapR) && isYonMap(locR.y, mapR)
				&& !mapR[locR.y][locR.x].equals(TerrainTile.VOID);
	}

	@Override
	public boolean isTargetReached() {
		return rc.getLocation().equals(target);
	}
}
