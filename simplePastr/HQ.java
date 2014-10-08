package simplePastr;

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
	private int height;
	private int width;
	private MapLocation myHq;
	private MapLocation otherHq;
	private MapLocation bestForPastr;

	public HQ(RobotController rc) {
		super(rc);
	}

	@Override
	protected void act() throws GameActionException {
		Map<SoldierRole, Integer> roleCount = Channel.getSoldierRoleCount(rc);
		Map<RobotType, Integer> typeCount = Channel.getRobotTypeCount(rc);

		// Check if a robot is spawnable and spawn one if it is
		// if (rc.senseRobotCount() < GameConstants.MAX_ROBOTS) { TODO:
		// uncomment
		if (rc.senseRobotCount() < GameConstants.MAX_ROBOTS) {
			if (roleCount.get(SoldierRole.PASTR_BUILDER) < 1
					&& typeCount.get(RobotType.PASTR) < 1) {
				Channel.demandSoldierRole(rc, SoldierRole.PASTR_BUILDER);
			} else if (randall.nextBoolean()) {
				Channel.demandSoldierRole(rc, SoldierRole.ATTACKER);
			} else {
				Channel.demandSoldierRole(rc, SoldierRole.PROTECTOR);
			}
			Direction spawnAt = myHq.directionTo(otherHq);
			if (rc.isActive()) {
				int i = 0;
				while (!rc.canMove(spawnAt)) {
					spawnAt = C.DIRECTIONS[i % C.DIRECTIONS.length];
					i++;
				}
				rc.spawn(spawnAt);
			}
		}
	}

	private void generatePastrRating() {
		mapPastrRating = new double[height][width];
		double currentBestRating = 0;
		bestForPastr = new MapLocation(0, 0);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
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

	@Override
	protected void init() throws GameActionException {
		height = rc.getMapHeight();
		width = rc.getMapWidth();
		mapCowGrowth = rc.senseCowGrowth();
		myHq = rc.senseHQLocation();
		otherHq = rc.senseEnemyHQLocation();

		// printMap();
		generatePastrRating();
		// printMapAnalysis();
		Channel.broadcastBestPastrLocation(rc, bestForPastr);
	}
}
