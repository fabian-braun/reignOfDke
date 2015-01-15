package morePASTRS;

import battlecode.common.GameConstants;
import battlecode.common.MapLocation;

public class Location implements Comparable<Location> {

	private MapLocation loc;
	private double rating;
	private final int MINIMUM_DISTANCE = 2 * GameConstants.NOISE_SCARE_RANGE_LARGE;

	public Location(MapLocation l, double r) {
		loc = l;
		rating = r;
	}

	public MapLocation getLoc() {
		return loc;
	}

	public void setLoc(MapLocation loc) {
		this.loc = loc;
	}

	public double getRating() {
		return rating;
	}

	public void setRating(double rating) {
		this.rating = rating;
	}

	public int compareTo(Location otherLoc) {
		if ((loc.distanceSquaredTo(otherLoc.getLoc()) - MINIMUM_DISTANCE) > 0) {
			if (rating < otherLoc.getRating()) {
				return -1;
			} else {
				return 1;
			}
		}
		return 0;
	}

	public String toString() {
		return loc.toString() + ", " + rating;
	}
}
