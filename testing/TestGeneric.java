package testing;

import org.junit.Assert;
import org.junit.Test;

import reignOfDKE.PathFinder;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;
import dualcore.PathFinderAStarFast;

public class TestGeneric {

	@Test
	public void test() throws GameActionException {
		MapLocation[] hqs = new MapLocation[2];
		hqs[0] = new MapLocation(0, 0);
		hqs[1] = new MapLocation(90, 60);
		RobotController rc = new RobotControllerMock(getMap1(), hqs,
				new MapLocation(1, 1));
		PathFinderAStarFast pathFinder = new PathFinderAStarFast(rc, 0);
		pathFinder.setTarget(new MapLocation(89, 59));
		int saveCounter = 200;
		while (!pathFinder.isTargetReached() && saveCounter > 0) {
			saveCounter--;
			pathFinder.move();
		}
	}

	private TerrainTile[][] getMap1() {
		TerrainTile[][] map = new TerrainTile[61][91];
		for (int y = 0; y < map.length; y++) {
			for (int x = 0; x < map[0].length; x++) {
				map[y][x] = TerrainTile.NORMAL;
			}
		}
		for (int y = 20; y < map.length - 20; y++) {
			for (int x = 20; x < map[0].length - 20; x++) {
				map[y][x] = TerrainTile.VOID;
			}
		}
		for (int x = 0; x < map[0].length; x++) {
			map[map.length / 2][x] = TerrainTile.ROAD;
		}

		return map;
	}

	@Test
	public void testDistanceMethods() {
		MapLocation loc1 = new MapLocation(2, 2);
		MapLocation loc2 = new MapLocation(0, 15);
		System.out.println("Euclidian: "
				+ PathFinder.getEuclidianDist(loc1, loc2));
		System.out.println("Manhattan: "
				+ PathFinder.getManhattanDist(loc1, loc2));
		System.out.println("Real: " + PathFinder.getRealDist(loc1, loc2));
		System.out.println("Moves: " + PathFinder.getRequiredMoves(loc1, loc2));
		Assert.assertEquals(PathFinder.getRealDist(loc1, loc2),
				PathFinder.getRealDist(loc2, loc1), 0);
	}

	@Test
	public void testMapLocationIntConversion() {
		for (int y = 0; y < 100; y++) {
			for (int x = 0; x < 100; x++) {
				MapLocation testLoc = new MapLocation(x, y);
				Assert.assertEquals(testLoc, toMapLocation(toInt(testLoc)));
			}
		}
	}

	private static int toInt(MapLocation location) {
		return toInt(location.y, location.x);
	}

	private static int toInt(int y, int x) {
		return x * 1000 + y;
	}

	private static MapLocation toMapLocation(int encoded) {
		return new MapLocation(encoded / 1000, encoded % 1000);
	}
}
