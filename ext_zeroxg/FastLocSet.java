package ext_zeroxg;

import battlecode.common.GameConstants;
import battlecode.common.MapLocation;

public class FastLocSet {
	private static final int size = Math.max(GameConstants.MAP_MAX_WIDTH,
			GameConstants.MAP_MAX_HEIGHT);
	private boolean[][] set;

	public FastLocSet() {
		this.set = new boolean[size][size];
	}

	public void add(MapLocation l) {
		set[l.x][l.y] = true;
	}

	public boolean contains(MapLocation l) {
		return set[l.x][l.y];
	}
}
