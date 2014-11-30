package ext_zeroxg;

import battlecode.common.MapLocation;

public abstract class LocPool {

	private static MapLocation[][] pool;

	public MapLocation get(int x, int y) {
		return pool[x][y];
	}
}
