package simplePastr;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class HQ extends AbstractRobotType {

	private char[][] mapRepresentation;
	private TerrainTile[][] mapTerrain;
	private double[][] mapCowGrowth;
	private double[][] mapPastrRating;
	private int height;
	private int width;
	private MapLocation myHq;
	private MapLocation otherHq;

	public HQ(RobotController rc) {
		super(rc);
	}

	@Override
	protected void act() throws GameActionException {
		// Check if a robot is spawnable and spawn one if it is
		if (rc.isActive() && rc.senseRobotCount() < 2) {
			Direction toEnemy = rc.getLocation().directionTo(
					rc.senseEnemyHQLocation());
			if (rc.senseObjectAtLocation(rc.getLocation().add(toEnemy)) == null) {
				rc.spawn(toEnemy);
			}
		}
	}

	private void analyzeMap() {
		generatePastrRating();
	}

	private void generatePastrRating() {
		mapPastrRating = new double[height][width];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				mapPastrRating[y][x] = distance(y, x, otherHq.y, otherHq.x)
						* mapCowGrowth[y][x];
			}
		}
	}

	private int distance(int y1, int x1, int y2, int x2) {
		return Math.abs((y2 - y1)) + Math.abs((x2 - x1));
	}

	private void printMap() {
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
				System.out.print(mapPastrRating[y][x] + " ");
			}
			System.out.println();
		}
	}

	@Override
	protected void init() throws GameActionException {
		height = rc.getMapHeight();
		width = rc.getMapWidth();

		mapRepresentation = new char[height][width];
		mapTerrain = new TerrainTile[height][width];
		mapCowGrowth = rc.senseCowGrowth();
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
		myHq = rc.senseHQLocation();
		otherHq = rc.senseEnemyHQLocation();
		mapRepresentation[myHq.y][myHq.x] = 'H';
		mapRepresentation[otherHq.y][otherHq.x] = 'E';
		printMap();
		analyzeMap();
		printMapAnalysis();
	}
}
