package ext_animorphs;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class SunrayHerder {
	private Controller c;
	private RobotController rc;

	public MapLocation pLoc; // pastr location
	public MapLocation tLoc; // tower location
	public MapLocation center;
	public boolean inRange;
	public MapLocation[] maxRangeLocs; // max range where noise tower starts ray
	public int[] rayLengths; // how many noise tower attacks in each ray
	public final Direction[] dirs = new Direction[] { Direction.WEST,
			Direction.NORTH_WEST, Direction.NORTH, Direction.NORTH_EAST,
			Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH,
			Direction.SOUTH_WEST };

	public final int pastrRadius = RobotType.PASTR.sensorRadiusSquared; // pasture
																		// range
																		// -
																		// when
																		// to
																		// switch
																		// to
																		// next
																		// ray
	public final int attackRadius;
	public final int xmax;
	public final int ymax;

	// keep track of next target
	public int dirIndex = 0;
	public int currRayLength = 0;
	public MapLocation lastTarget;

	public SunrayHerder(Controller c, MapLocation pLoc) {
		this.c = c;
		this.rc = c.rc;
		this.pLoc = pLoc;
		center = pLoc;
		tLoc = rc.getLocation();
		attackRadius = c.attackRadiusSquared;
		ymax = c.height;
		xmax = c.width;

		// initialize ray paths
		inRange = pastrInRange();
		findRayPaths();
	}

	/**
	 * Checks if any of pastr squares are in tower range and updates pLoc to
	 * non-center pastr square if necessary.
	 * 
	 * @return true if in range, false else
	 */
	public boolean pastrInRange() {
		if (pLoc.distanceSquaredTo(tLoc) <= attackRadius) {
			return true;
		}
		// check if other squares in pastr are in range
		MapLocation nextPLoc = pLoc.add(pLoc.directionTo(tLoc));
		while (nextPLoc.distanceSquaredTo(pLoc) <= pastrRadius) {
			if (nextPLoc.distanceSquaredTo(tLoc) <= attackRadius) {
				pLoc = nextPLoc;
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks if square is void, out of map, out of tower range
	 * 
	 * @param loc
	 * @return
	 */
	public boolean locationValid(MapLocation loc) {
		return !(rc.senseTerrainTile(loc) == TerrainTile.VOID || loc.x > xmax
				|| loc.y > ymax || loc.x < 0 || loc.y < 0 || loc
				.distanceSquaredTo(tLoc) > attackRadius);

	}

	/**
	 * Finds max locations and lengths of rays in 8 directions
	 * 
	 * @return
	 */
	public void findRayPaths() {
		maxRangeLocs = new MapLocation[8];
		rayLengths = new int[8];

		for (int i = 0; i < 8; i++) { // find max range in each direction from
										// pastr
			Direction dir = dirs[i];
			int rayLength = 1; // counts ray length
			MapLocation nextLoc = pLoc.add(dir);
			while (locationValid(nextLoc.add(dir))) { // while location is
														// valid, expand ray
				rayLength++;
				nextLoc = nextLoc.add(dir);
			}
			maxRangeLocs[i] = nextLoc;
			rayLengths[i] = rayLength;
		}
	}

	public MapLocation nextTarget() {
		if (currRayLength <= 3) { // switch to next direction
			dirIndex = (dirIndex + 1) % 8;
			currRayLength = rayLengths[dirIndex] - 1;
			lastTarget = maxRangeLocs[dirIndex];
			return lastTarget;
		} else { // reduce ray length in current direction
			lastTarget = lastTarget.add(dirs[dirIndex].opposite());
			currRayLength--;
			return lastTarget;
		}
	}

}
