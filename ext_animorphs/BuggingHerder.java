package ext_animorphs;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class BuggingHerder {
	private RobotController rc;
	public Controller c;

	public MapLocation center;
	public MapLocation current;

	public int numOut;
	public MapLocation[] locationsOut; // track where "cows are";
	public Direction[] directionsOut; // track which ways we went out
	public int dirIndex = 0; // track which direction we are on

	public final Direction[] dirs = new Direction[] { Direction.WEST,
			Direction.NORTH_WEST, Direction.NORTH, Direction.NORTH_EAST,
			Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH,
			Direction.SOUTH_WEST };

	public BuggingHerder(Controller c) {
		this.c = c;
		rc = c.rc;

		numOut = 0;
		locationsOut = new MapLocation[50];
		directionsOut = new Direction[50];
		center = rc.getLocation();
	}

	public void setCenter(MapLocation loc) {
		center = loc;
		initNextDir();
	}

	public MapLocation getNextLocation() {
		int cutoff;
		if (dirs[dirIndex].isDiagonal()) {
			cutoff = 2;
		} else {
			cutoff = 1;
		}

		while (numOut <= cutoff) {
			initNextDir();
			if (dirs[dirIndex].isDiagonal()) {
				cutoff = 2;
			} else {
				cutoff = 1;
			}
		}
		numOut--;
		rc.setIndicatorString(1, locationsOut[numOut].toString());
		rc.setIndicatorString(2, directionsOut[numOut].toString());
		return locationsOut[numOut].add(directionsOut[numOut]);
	}

	public void initNextDir() {
		Direction main = dirs[dirIndex];
		Direction left = main.rotateLeft();
		Direction right = main.rotateRight();
		current = center;

		locationsOut = new MapLocation[50];
		directionsOut = new Direction[50];
		numOut = 0;
		while (numOut < 50) {
			MapLocation loc = current.add(main);
			if (canMove(main, loc)) {
				// try main
				directionsOut[numOut] = main;
				locationsOut[numOut] = loc;
			} else {
				// randomize
				if (Math.random() < .5) {
					// try left
					loc = current.add(left);
					if (canMove(left, loc)) {
						directionsOut[numOut] = left;
						locationsOut[numOut] = loc;
					} else {
						// try right
						loc = current.add(right);
						if (canMove(right, loc)) {
							directionsOut[numOut] = right;
							locationsOut[numOut] = loc;
						}
						// stop
						else {
							break;
						}
					}
				} else {
					// try right first
					loc = current.add(right);
					if (canMove(right, loc)) {
						directionsOut[numOut] = right;
						locationsOut[numOut] = loc;
					} else {
						// try left next
						loc = current.add(left);
						if (canMove(left, loc)) {
							directionsOut[numOut] = left;
							locationsOut[numOut] = loc;
						}
						// stop
						else {
							break;
						}
					}
				}
			}
			current = locationsOut[numOut];
			// found direction
			numOut++;
		}
		dirIndex = (dirIndex + 1) % 8;
	}

	public boolean canMove(Direction dir, MapLocation loc) {
		MapLocation newLoc = loc.add(dir);
		if (rc.canAttackSquare(newLoc)) {
			TerrainTile t = rc.senseTerrainTile(loc);
			if (t != TerrainTile.VOID && t != TerrainTile.OFF_MAP) {
				return true;
			}
		}
		return false;
	}

}
