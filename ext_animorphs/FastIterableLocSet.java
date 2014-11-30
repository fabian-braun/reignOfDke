package ext_animorphs;

import battlecode.common.GameConstants;
import battlecode.common.MapLocation;

public class FastIterableLocSet implements LocSet {
	private int size = 0;
	private StringBuilder keys = new StringBuilder();

	private String locToStr(MapLocation loc) {
		return "" + (char) (loc.x + GameConstants.MAP_MAX_HEIGHT)
				+ (char) (loc.y);
	}

	public void add(MapLocation loc) {
		String key = locToStr(loc);
		if (keys.indexOf(key) == -1) {
			keys.append(key);
			size++;
		}
	}

	public void remove(MapLocation loc) {
		String key = locToStr(loc);
		int index;
		if ((index = keys.indexOf(key)) != -1) {
			keys.delete(index, index + 2);
			size--;
		}
	}

	public boolean contains(MapLocation loc) {
		return keys.indexOf(locToStr(loc)) != -1;
	}

	public void clear() {
		size = 0;
		keys = new StringBuilder();
	}

	public MapLocation[] getKeys() {
		MapLocation[] locs = new MapLocation[size];
		for (int i = 0; i < size; i++) {
			locs[i] = new MapLocation(keys.charAt(i * 2)
					- GameConstants.MAP_MAX_HEIGHT, keys.charAt(i * 2 + 1));
		}
		return locs;
	}

	public void replace(String newSet) {
		keys.replace(0, keys.length(), newSet);
		size = newSet.length() / 2;
	}

	public int size() {
		return size;
	}
}