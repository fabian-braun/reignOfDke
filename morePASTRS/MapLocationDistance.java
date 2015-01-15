package morePASTRS;

import battlecode.common.MapLocation;

public class MapLocationDistance implements Comparable<MapLocationDistance> {

	private int distance;
	private MapLocation location;
	private MapLocationDistance ancestor;

	public MapLocationDistance(MapLocation loc, int distance) {
		this.location = loc;
		this.distance = distance;
	}

	public MapLocation getMapLocation() {
		return location;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}

	public MapLocationDistance getAncestor() {
		return ancestor;
	}

	public void setAncestor(MapLocationDistance ancestor) {
		this.ancestor = ancestor;
	}

	public int getDistance() {
		return distance;
	}

	@Override
	public int compareTo(MapLocationDistance other) {
		return Integer.compare(distance, other.distance);
	}

}
