package reignOfDKE;

import battlecode.common.MapLocation;

public class MapLocationSet {

	public MapLocation[] array;

	public MapLocationSet(int capacity) {
		array = new MapLocation[capacity + 1];
		// +1 because one element must always be null, to make set work
	}

	public void add(MapLocation loc) {
		int index = loc.hashCode() % array.length;
		while (array[index] != null) {
			if (array[index].equals(loc)) {
				return; // Location is already in set
			} else {
				index++;
				index %= array.length;
			}
		}
		array[index] = loc;
	}

	public void remove(MapLocation loc) {
		int index = loc.hashCode() % array.length;
		while (array[index] != null) {
			if (array[index].equals(loc)) {
				array[index] = null;
				rehashFrom(index);
				return;
			} else {
				index++;
				index %= array.length;
			}
		}
		// loc was not in set
	}

	// rehashes the sequence of elements!=null right after index
	private void rehashFrom(int index) {
		index++;
		index %= array.length;
		int initialIndex = index;
		while (array[index] != null) {
			index++;
			index %= array.length;
		}
		MapLocation[] temp = new MapLocation[index - initialIndex];
		index = initialIndex;
		while (array[index] != null) {
			temp[index - initialIndex] = array[index];
			array[index] = null;
			index++;
			index %= array.length;
		}
		for (MapLocation loc : temp) {
			add(loc);
		}
	}

	public boolean contains(MapLocation loc) {
		int index = loc.hashCode() % array.length;
		while (array[index] != null) {
			if (array[index].equals(loc)) {
				return true;
			} else {
				index++;
				index %= array.length;
			}
		}
		return false;
	}

	public void clear() {
		array = new MapLocation[array.length];
	}
}
