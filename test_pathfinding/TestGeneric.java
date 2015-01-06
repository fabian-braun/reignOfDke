package test_pathfinding;

import org.junit.Test;

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
		PathFinderAStarFast pathFinder = new PathFinderAStarFast(rc);
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

}
