package ext_animorphs;

import battlecode.common.Direction;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;

public class FastLocDirSet {
	private static final int HASH = Math.max(GameConstants.MAP_MAX_WIDTH,
			GameConstants.MAP_MAX_HEIGHT);
	private int size = 0;
	private boolean[][][] has = new boolean[HASH][HASH][10];

	public void add(MapLocation loc, Direction dir) {
		int x = loc.x;
		int y = loc.y;
		int z = dir.ordinal();
		if (!has[x][y][z]) {
			size++;
			has[x][y][z] = true;
		}
	}

	public void remove(MapLocation loc, Direction dir) {
		int x = loc.x;
		int y = loc.y;
		int z = dir.ordinal();
		if (has[x][y][z]) {
			size--;
			has[x][y][z] = false;
		}
	}

	public boolean contains(MapLocation loc, Direction dir) {
		return has[loc.x][loc.y][dir.ordinal()];
	}

	public void clear() {
		has = new boolean[HASH][HASH][10];
		size = 0;
	}

	public int size() {
		return size;
	}
}