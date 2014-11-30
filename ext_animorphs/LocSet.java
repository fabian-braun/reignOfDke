package ext_animorphs;

import battlecode.common.MapLocation;

/**
 * Interface that represents a collection of something (e.g. towers) locations.
 * Supports adding and removing from collection
 */
public interface LocSet {
	// add a location to the collection
	void add(MapLocation loc);

	// remove a location from the collection
	void remove(MapLocation loc);

	// check for membership
	boolean contains(MapLocation loc);

	// returns size
	int size();

}
