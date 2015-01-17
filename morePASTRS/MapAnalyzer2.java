package morePASTRS;

import java.util.ArrayList;

import morePASTRS.C.MapType;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class MapAnalyzer2 extends PathFinder {

	/**
	 * protected final MapLocation hqSelfLoc; protected final MapLocation
	 * hqEnemLoc; protected final int ySize; protected final int xSize;
	 * protected final RobotController rc; protected TerrainTile[][] map;
	 */

	private final MapType type;

	ArrayList<Location> bestLocations = new ArrayList<Location>();
	private double[][] mapCowGrowth;
	private double[][] mapPastrRating;

	private static final int MAP_SIZE_SMALL_THRESHOLD = 40;
	private static final int MAP_SIZE_MEDIUM_THRESHOLD = 60;
	private static final int MAP_SIZE_NO_SQUARE_MEDIUM_THRESHOLD = 50;

	public MapAnalyzer2(RobotController rc) {
		super(rc);
		type = determineMapType();
	}

	public MapType getMapType() {
		return type;
	}

	public MapAnalyzer2(RobotController rc, TerrainTile[][] map,
			MapLocation hqSelfLoc, MapLocation hqEnemLoc, int ySize, int xSize) {
		super(rc, map, hqSelfLoc, hqEnemLoc, ySize, xSize);
		type = determineMapType();
	}

	@Override
	public boolean move() throws GameActionException {
		return false;
	}

	@Override
	public boolean sneak() throws GameActionException {
		return false;
	}

	@Override
	public void setTarget(MapLocation target) {
	}

	@Override
	public MapLocation getTarget() {
		return null;
	}

	@Override
	public boolean isTargetReached() {
		return false;
	}

	private MapType determineMapType() {
		if (ySize < MAP_SIZE_SMALL_THRESHOLD
				&& xSize < MAP_SIZE_SMALL_THRESHOLD) {
			return MapType.Small;
		}
		if (ySize < MAP_SIZE_MEDIUM_THRESHOLD
				&& xSize < MAP_SIZE_MEDIUM_THRESHOLD) {
			return MapType.Medium;
		}
		if (ySize > MAP_SIZE_MEDIUM_THRESHOLD
				&& xSize < MAP_SIZE_MEDIUM_THRESHOLD) {
			return MapType.Large;
		}
		int largestDimension = Math.max(ySize, xSize);
		if (largestDimension < MAP_SIZE_NO_SQUARE_MEDIUM_THRESHOLD) {
			return MapType.Medium;
		} else {
			return MapType.Large;
		}
	}

	public ArrayList<Location> evaluateBestPastrLocs(int maximum) {
		mapCowGrowth = rc.senseCowGrowth();
		mapPastrRating = new double[ySize][xSize];
		bestLocations = new ArrayList<Location>();

		int xStep = xSize / 25 + 1;
		int yStep = ySize / 25 + 1;

		for (int y = 2; y < ySize; y += yStep) {
			for (int x = 2; x < xSize; x += xStep) {
				MapLocation current = new MapLocation(x, y);
				TerrainTile tile = map[y][x];
				if (tile != TerrainTile.NORMAL && tile != TerrainTile.ROAD) {
					continue;
				}
				double sumCowGrowth = 0;
				for (int ylocal = y - 1; ylocal <= y + 1; ylocal++) {
					for (int xlocal = x - 1; xlocal <= x + 1; xlocal++) {
						if (ylocal >= 0 && xlocal >= 0 && ylocal < ySize
								&& xlocal < ySize) {
							// NO BUG! mapCowGrowth has x before y!!
							sumCowGrowth += mapCowGrowth[x][y];
						}
					}
				}
				// 3 possibilities to get the distance to the enemies HQ
				// minimum moves required, manhattan or euclidian distance
				// TODO: Test for best method
				// int distance = getRequiredMoves(current, hqEnemLoc);
				int distance = getManhattanDist(current, hqEnemLoc);
				// int distance = getEuclidianDist(current,
				// hqEnemLoc);

				mapPastrRating[y][x] = distance * sumCowGrowth;
				Location currentLoc = new Location(current,
						mapPastrRating[y][x]);

				insertInList(currentLoc, maximum);

			}
		}
		return bestLocations;
	}

	public void insertInList(Location loc, int maximum) {
		// if the list is empty -> add the first Location tested.
		if (bestLocations.isEmpty()) {
			bestLocations.add(loc);
		} else if (!inList(loc)) {
			int bestIndex = 0;
			for (int i = 0; i < bestLocations.size(); i++) {
				int difference = loc.compareTo(bestLocations.get(i));
				if (difference == 1) {
					bestIndex = i;
					break;
				}
			}
			if (bestIndex < maximum) {
				bestLocations.add(bestIndex, loc);
			}
			while (bestLocations.size() > maximum) {
				bestLocations.remove(bestLocations.size() - 1);
			}
		}
	}

	private boolean inList(Location current) {
		for (int i = 0; i < bestLocations.size(); i++) {
			if (current.compareTo(bestLocations.get(i)) == 0) {
				return true;
			}
		}
		return false;
	}

	public String listToString() {
		String s = "";
		if (bestLocations.isEmpty()) {
			return s;
		} else {
			for (int i = 0; i < bestLocations.size(); i++) {
				s += bestLocations.get(i).toString();
				s += "\n";
			}
		}
		return s;
	}

}