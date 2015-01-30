package reignOfDKE;

import java.util.Map;

import battlecode.common.MapLocation;

public class MapLocationPriorityQueue {

	private class Element {
		public MapLocation loc;
		public Element next;
		public int score;

		public Element(MapLocation loc, int score) {
			this.loc = loc;
			this.score = score;
		}
	}

	private Element first;
	private MapLocationSet set;
	private Map<MapLocation, Integer> scoreMap;

	public MapLocationPriorityQueue(int capacity,
			Map<MapLocation, Integer> scoreMap) {
		set = new MapLocationSet(capacity);
		this.scoreMap = scoreMap;
		first = null;
	}

	public void add(MapLocation loc) {
		set.add(loc);
		Element toInsert = new Element(loc, scoreMap.get(loc));
		if (first == null) {
			first = toInsert;
			return;
		}
		Element current = first;
		if (current.score > toInsert.score) {
			// toInsert should be inserted in first position
			toInsert.next = current;
			first = toInsert;
			return;
		}
		while (current.next != null) {
			if (current.next.score > toInsert.score) {
				// toInsert should be inserted before next element
				toInsert.next = current.next;
				current.next = toInsert;
				return;
			}
			current = current.next;
		}
		// add loc at end of queue
		current.next = toInsert;
	}

	public boolean isEmpty() {
		return first == null;
	}

	public MapLocation poll() {
		MapLocation loc = first.loc;
		set.remove(loc);
		first = first.next;
		return loc;
	}

	public boolean contains(MapLocation loc) {
		return set.contains(loc);
	}

}
