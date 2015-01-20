package reignierOfDKE;

import java.util.Random;
import java.util.Set;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class MapAnalyzer extends PathFinder {

	private final MapSize type;

	private double[][] mapCowGrowth;

	private int xStep;

	private int yStep;

	private final Random randall;

	public MapAnalyzer(RobotController rc, int soldierId, Random randall) {
		super(rc, soldierId);
		type = MapSize.get(ySize, xSize);
		xStep = xSize / 20 + 1;
		yStep = ySize / 20 + 1;
		this.randall = randall;
	}

	public MapSize getMapType() {
		return type;
	}

	public MapAnalyzer(RobotController rc, TerrainTile[][] map,
			MapLocation hqSelfLoc, MapLocation hqEnemLoc, int ySize, int xSize,
			int soldierId, Random randall) {
		super(rc, map, hqSelfLoc, hqEnemLoc, ySize, xSize, soldierId);
		type = MapSize.get(ySize, xSize);
		xStep = xSize / 12 + 1;
		yStep = ySize / 12 + 1;
		this.randall = randall;
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

	public MapLocation getGoodPastrLocation(Set<MapLocation> avoidStrict,
			Set<MapLocation> avoidLenient) {
		if (AbstractRobotType.size(mapCowGrowth) < 1) {
			mapCowGrowth = rc.senseCowGrowth();
		}
		Channel.signalAlive(rc, soldierId);

		MapLocation best = new MapLocation(-1, -1);
		double bestRating = 0;

		for (int y = randall.nextInt(yStep); y < ySize; y += yStep) {
			inner: for (int x = randall.nextInt(xStep); x < xSize; x += xStep) {
				Channel.signalAlive(rc, soldierId);
				MapLocation current = new MapLocation(x, y);
				if (!isTraversableAndNotHq(current)) {
					continue;
				}
				for (MapLocation toBeAvoided : avoidStrict) {
					if (getManhattanDist(current, toBeAvoided) < GameConstants.PASTR_RANGE * 2) {
						// too close
						continue inner;
					}
				}
				double distance = 0;
				for (MapLocation toBeAvoided : avoidLenient) {
					distance += getRealDist(current, toBeAvoided);
				}
				distance /= avoidLenient.size();
				double sumCowGrowth = 0;
				for (int ylocal = y - 1; ylocal <= y + 1; ylocal++) {
					for (int xlocal = x - 1; xlocal <= x + 1; xlocal++) {
						if (isYonMap(ylocal) && isXonMap(xlocal)) {
							// NO BUG! mapCowGrowth has x before y!!
							sumCowGrowth += mapCowGrowth[xlocal][ylocal];
						}
					}
				}

				double rating = distance * sumCowGrowth;

				if (rating > bestRating) {
					bestRating = rating;
					best = current;
				}
			}
		}

		return best;
	}

}