package teamreignofdke;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class HQ extends AbstractRobotType {

	private char[][] mapRepresentation;
	private double[][] mapCowGrowth;
	private double[][] mapPastrRating;
	private int[][] realDistanceFromHQ;
	private int height;
	private int width;
	private MapLocation myHq;
	private MapLocation otherHq;
	private MapLocation bestForPastr;
	private int boundaryBeforePastr = 10;

	public HQ(RobotController rc) {
		super(rc);
	}

	@Override
	protected void act() throws GameActionException {
		Map<SoldierRole, Integer> roleCount = Channel.getSoldierRoleCount(rc);
		Map<RobotType, Integer> typeCount = Channel.getRobotTypeCount(rc);

		// Check if a robot is spawnable and spawn one if it is
		if (rc.senseRobotCount() < GameConstants.MAX_ROBOTS) {
			if (roleCount.get(SoldierRole.PASTR_BUILDER) < 1
					&& typeCount.get(RobotType.PASTR) < 1
					&& typeCount.get(RobotType.SOLDIER) > boundaryBeforePastr) {
				// printMap();
				generatePastrRating();
				// printMapAnalysis();
				Channel.broadcastBestPastrLocation(rc, bestForPastr);
				Channel.demandSoldierRole(rc, SoldierRole.PASTR_BUILDER);
				// If we don't have a noise-tower-builder and also don't have a
				// noise tower yet
			} else if (roleCount.get(SoldierRole.NOISE_TOWER_BUILDER) < 1
					&& typeCount.get(RobotType.NOISETOWER) < 1
					&& typeCount.get(RobotType.SOLDIER) > boundaryBeforePastr) {
				// Demand a noise-tower-builder
				Channel.demandSoldierRole(rc, SoldierRole.NOISE_TOWER_BUILDER);
			} else if (randall.nextInt(3) > 0
					|| roleCount.get(SoldierRole.PROTECTOR) > 4) {
				Channel.demandSoldierRole(rc, SoldierRole.ATTACKER);
			} else {
				Channel.demandSoldierRole(rc, SoldierRole.PROTECTOR);
			}
			Direction spawnAt = myHq.directionTo(otherHq);
			if (rc.isActive()) {
				int i = 0;
				while (!rc.canMove(spawnAt) && i < C.DIRECTIONS.length) {
					spawnAt = C.DIRECTIONS[i];
					i++;
				}
				if (rc.canMove(spawnAt)) {
					rc.spawn(spawnAt);
				}
			}
		}
	}

	private void generateRealDistanceMap() {
		realDistanceFromHQ = new int[height][width];
		realDistanceFromHQ = evaluateNeighbours(realDistanceFromHQ, myHq);
		realDistanceFromHQ[myHq.y][myHq.x] = -1;
	}

	private int[][] evaluateNeighbours(int[][] map, MapLocation current) {
		if (map[current.y][current.x] != 0) {
			return map;
		} else {
			TerrainTile terrain = rc.senseTerrainTile(new MapLocation(
					current.x, current.y));
			if (terrain == TerrainTile.VOID || terrain == TerrainTile.OFF_MAP) {
				map[current.y][current.x] = -1;
				return map;
			}
		}
		List<MapLocation> neighbours = getNeighbours(current);
		for (MapLocation neighbour : neighbours) {
			if (map[neighbour.y][neighbour.x] == 0) {
				System.out.println("update distance to "
						+ (map[current.y][current.x] + 1));
				map[neighbour.y][neighbour.x] = map[current.y][current.x] + 1;
			}
		}
		for (MapLocation neighbour : neighbours) {
			// recursion
			if (isXonMap(neighbour.x) && isYonMap(neighbour.y)) {
				map = evaluateNeighbours(map, neighbour);
			}
		}
		return map;
	}

	private boolean isXonMap(int x) {
		return x >= 0 && x < width;
	}

	private boolean isYonMap(int y) {
		return y >= 0 && y < height;
	}

	private List<MapLocation> getNeighbours(MapLocation from) {
		List<MapLocation> neighbours = new ArrayList<MapLocation>();
		addNeighbour(neighbours, new MapLocation(from.x - 1, from.y - 1));
		addNeighbour(neighbours, new MapLocation(from.x - 1, from.y));
		addNeighbour(neighbours, new MapLocation(from.x - 1, from.y + 1));
		addNeighbour(neighbours, new MapLocation(from.x, from.y - 1));
		addNeighbour(neighbours, new MapLocation(from.x, from.y + 1));
		addNeighbour(neighbours, new MapLocation(from.x + 1, from.y - 1));
		addNeighbour(neighbours, new MapLocation(from.x + 1, from.y));
		addNeighbour(neighbours, new MapLocation(from.x + 1, from.y + 1));
		return neighbours;
	}

	private void addNeighbour(List<MapLocation> neighbours,
			MapLocation potentialNeighbour) {
		if (isXonMap(potentialNeighbour.x) && isYonMap(potentialNeighbour.y)) {
			neighbours.add(potentialNeighbour);
		}
	}

	private void generatePastrRating() {
		if (bestForPastr != null) {
			// already found previously
			return;
		}
		mapPastrRating = new double[height][width];
		double currentBestRating = 0;
		bestForPastr = new MapLocation(0, 0);
		int xStep = width / 25 + 1;
		int yStep = height / 25 + 1;
		for (int y = 2; y < height; y += yStep) {
			for (int x = 2; x < width; x += xStep) {
				TerrainTile tile = rc.senseTerrainTile(new MapLocation(x, y));
				if (tile != TerrainTile.NORMAL && tile != TerrainTile.ROAD) {
					continue;
				}
				double sumCowGrowth = 0;
				for (int ylocal = y - 1; ylocal <= y + 1; ylocal++) {
					for (int xlocal = x - 1; xlocal <= x + 1; xlocal++) {
						if (ylocal >= 0 && xlocal >= 0 && ylocal < height
								&& xlocal < width) {
							// mapCowGrowth has x before y.. this is no bug
							sumCowGrowth += mapCowGrowth[x][y];
						}
					}
				}
				mapPastrRating[y][x] = PathFinder.distance(
						new MapLocation(x, y), otherHq) * sumCowGrowth;
				if (mapPastrRating[y][x] > currentBestRating) {
					currentBestRating = mapPastrRating[y][x];
					bestForPastr = new MapLocation(x, y);
				}
			}
		}
	}

	@Override
	protected void init() throws GameActionException {
		height = rc.getMapHeight();
		width = rc.getMapWidth();
		mapCowGrowth = rc.senseCowGrowth();
		myHq = rc.senseHQLocation();
		otherHq = rc.senseEnemyHQLocation();

		// location between our HQ and opponent's HQ:
		MapLocation temporaryTarget = new MapLocation(
				(myHq.x * 3 / 4 + otherHq.x / 4),
				(myHq.y * 3 / 4 + otherHq.y / 4));

		Channel.broadcastBestPastrLocation(rc, temporaryTarget);

		generateRealDistanceMap();
		printMapAnalysisDistance();

	}

	private void printMap() {
		mapRepresentation = new char[height][width];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				TerrainTile tile = rc.senseTerrainTile(new MapLocation(x, y));
				switch (tile) {
				case OFF_MAP:
					mapRepresentation[y][x] = 'X';
					break;
				case ROAD:
					mapRepresentation[y][x] = '#';
					break;
				case VOID:
					mapRepresentation[y][x] = 'X';
					break;
				default:
					if (mapCowGrowth[y][x] > 0) {
						mapRepresentation[y][x] = (char) (48 + (int) mapCowGrowth[y][x]);
					} else {
						mapRepresentation[y][x] = ' ';
					}
				}
			}
		}
		mapRepresentation[myHq.y][myHq.x] = 'H';
		mapRepresentation[otherHq.y][otherHq.x] = 'E';
		System.out.println("map:");
		for (int y = 0; y < mapRepresentation.length; y++) {
			for (int x = 0; x < mapRepresentation[y].length; x++) {
				System.out.print(mapRepresentation[y][x]);
			}
			System.out.println();
		}
	}

	private void printMapAnalysis() {
		System.out.println("map:");
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				System.out.print(String.format("%5.0f", mapPastrRating[y][x]));
			}
			System.out.println();
		}
		System.out.println("best Pastr Rating at " + bestForPastr.toString());
		System.out.println("other hq is at " + otherHq.toString());
	}

	private void printMapAnalysisDistance() {
		System.out.println("map:");
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				System.out.print(String.format("%03d ",
						realDistanceFromHQ[y][x]));
			}
			System.out.println();
		}
		System.out.println("other hq is at " + otherHq.toString());
	}

}
