package reignierOfDKE;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class MapAnalyzer2 extends PathFinder {

	private final MapSize type;

	private double[][] mapCowGrowth;
	private double[][] mapPastrRating;

	private final int MINIMUM_DISTANCE = 2 * GameConstants.NOISE_SCARE_RANGE_LARGE;
	private MapLocation[] bestLocations;

	public MapAnalyzer2(RobotController rc) {
		super(rc);
		type = MapSize.get(ySize, xSize);
	}

	public MapSize getMapType() {
		return type;
	}

	public MapAnalyzer2(RobotController rc, TerrainTile[][] map,
			MapLocation hqSelfLoc, MapLocation hqEnemLoc, int ySize, int xSize) {
		super(rc, map, hqSelfLoc, hqEnemLoc, ySize, xSize);
		type = MapSize.get(ySize, xSize);
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

	public MapLocation[] evaluateBestPastrLocs(int maximum) {
		mapCowGrowth = rc.senseCowGrowth();
		mapPastrRating = new double[ySize][xSize];
		bestLocations = new MapLocation[maximum];

		int xStep = xSize / 25 + 1;
		int yStep = ySize / 25 + 1;

		for (int y = 2; y < ySize; y += yStep) {
			for (int x = 2; x < xSize; x += xStep) {
				MapLocation current = new MapLocation(x, y);
				TerrainTile tile = map[y][x];
				if ((tile != TerrainTile.NORMAL && tile != TerrainTile.ROAD)
						|| alreadyInArray(current)) {
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
				// int distance = getManhattanDist(current, hqEnemLoc);
				int distance = getEuclidianDist(current, hqEnemLoc);

				mapPastrRating[y][x] = distance * sumCowGrowth;
				// Decide if current should be add to List or not
				for (int i = 0; i < bestLocations.length; i++) {
					if (bestLocations[i] == null) {
						bestLocations[i] = current;
						break;
					} else {
						MapLocation compare = bestLocations[i];
						if (mapPastrRating[y][x] > mapPastrRating[compare.y][compare.x]) {
							addLocation(i, current);
							break;
						}
					}
				}

			}
		}
		return bestLocations;
	}

	public boolean alreadyInArray(MapLocation next) {
		for (int i = 0; i < bestLocations.length; i++) {
			if (bestLocations[i] == null) {
				break;
			} else if (next.distanceSquaredTo(bestLocations[i]) <= MINIMUM_DISTANCE) {
				return true;
			}
		}
		return false;
	}

	public void addLocation(int index, MapLocation toAdd) {
		for (int i = bestLocations.length - 1; i > index; i--) {
			if (bestLocations[i - 1] != null) {
				MapLocation toCopy = bestLocations[i - 1];
				bestLocations[i] = toCopy;
			}
		}
		bestLocations[index] = toAdd;
	}

	public void printBest() {
		for (int i = 0; i < bestLocations.length; i++) {
			MapLocation current = bestLocations[i];
			System.out.println(current.toString() + ", "
					+ mapPastrRating[current.y][current.x] + "\n");
		}

	}

}