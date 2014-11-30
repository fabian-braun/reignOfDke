package ext_animorphs;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

/**
 * Class that returns locations to attack to scare sheep towards an input
 * location. Spirals in along 8 directions
 */
public class SpiralHerder {
	private Controller c;
	private RobotController rc;
	// In order - NORTH, NORTHEAST, EAST, SOUTHEAST, SOUTH, SOUTHWEST, WEST,
	// NORTHWEST
	// public Direction[] directions;
	public final int numDir = 16; // MAGIC CONSTANT number of directions we have
									// (4 cardinal + 4 diags + 8 mixes)

	public MapLocation[] maxRangeLocs;
	public MapLocation[] currentTargets;

	public int[] maxSquaredOffset;

	public final int jumpCutoff = 15; // MAGIC CONSTANT - distance to switch to
										// from 16 to 8 direction herding
	public final int jumpCutoff2 = 6; // MAGIC CONSTANT - distance to switch
										// from 8 to 4 herding
	public final int stoppingOffset = 4; // MAGIC CONSTANT - should be pasture
											// range. When to reset distance
											// counter
	public final int mapExtension = 4; // MAGIC CONSTANT - Expands map borders
										// when choosing where to shoot
	public final int xymin = -mapExtension;
	public final int xmax; // max x-value on grid
	public final int ymax; // max y-value on grid

	public int dirCounter;
	public int distCounter;
	public int distMax;

	public MapLocation center;

	/**
	 * Init a spiralHerder that returns locations spiraling in towards input
	 * pastrLoc. Initial pastrLoc and surrounding squares should be in range and
	 * on map.
	 * 
	 * @param rc
	 * @param pastrLoc
	 */
	public SpiralHerder(Controller c, MapLocation pastrLoc) {
		this.c = c;
		xmax = c.width - 1 + mapExtension; // max x-value on grid
		ymax = c.height - 1 + mapExtension; // max y-value on grid
		rc = c.rc;
		center = pastrLoc;
		dirCounter = 0;

		// directions = new Direction[]{Direction.NORTH, Direction.NORTH_EAST,
		// Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH,
		// Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
		// NOTE initMaxDistance... is hardcoded to this order because of logic
		// for forcing locations in grid range
		maxRangeLocs = new MapLocation[numDir];
		currentTargets = new MapLocation[numDir];

		maxSquaredOffset = new int[numDir];

		// initMaxDistanceAndCurrentTargets(pastrLoc);
		initMaxDistances(pastrLoc);
		distMax = 0;
		for (int i : maxSquaredOffset) {
			distMax = Math.max(distMax, i);
		}
		distMax = (int) Math.sqrt(distMax);
		distCounter = distMax;
	}

	public MapLocation nextTarget() {
		// check for when we can never give a location;
		if (distMax < stoppingOffset) {
			return null;
		}

		while (true) {
			if (dirCounter == numDir) {// decrease distance
				distCounter--;
				if (distCounter <= stoppingOffset) {
					for (int i = 0; i < numDir; i++) {
						currentTargets[i] = maxRangeLocs[i];
					}
					distCounter = distMax;
				}
				dirCounter = 0;
			}
			if (distCounter * distCounter > maxSquaredOffset[dirCounter]) { // not
																			// in
																			// range
																			// yet,
																			// skip
				dirCounter++;
				if (distCounter < jumpCutoff) {
					dirCounter++;
					if (distCounter < jumpCutoff2) {
						dirCounter = dirCounter + 2;
					}
				}
			} else {
				MapLocation target = currentTargets[dirCounter];
				if (distCounter * distCounter < target
						.distanceSquaredTo(center)) { // decrement target
					currentTargets[dirCounter] = nextLocIn(dirCounter, target);
				}
				dirCounter++;
				if (distCounter < jumpCutoff) {
					dirCounter++;
					if (distCounter < jumpCutoff2) {
						dirCounter = dirCounter + 2;
					}
				}
				return target;
			}

			// if(distCounter > maxOffset[dirCounter]){ //Skip this direction
			// for now
			// dirCounter++;
			// }else{ // return cur target in this direction
			// MapLocation target = currentTargets[dirCounter];
			// currentTargets[dirCounter] =
			// target.subtract(directions[dirCounter]); //update target in this
			// direction
			// dirCounter++; //incr direction
			// return target;
			// }
		}

	}

	/**
	 * get map location going out along given dir. directions hard coded,
	 * clockwise from north along 16ths
	 * 
	 * @param dir
	 * @param curLoc
	 */
	private MapLocation nextLocOut(int dir, MapLocation curLoc) {
		switch (dir) {
		case 0:
			return curLoc.add(0, -1);
		case 1:
			return curLoc.add(1, -2);
		case 2:
			return curLoc.add(1, -1);
		case 3:
			return curLoc.add(2, -1);
		case 4:
			return curLoc.add(1, 0);
		case 5:
			return curLoc.add(2, 1);
		case 6:
			return curLoc.add(1, 1);
		case 7:
			return curLoc.add(1, 2);
		case 8:
			return curLoc.add(0, 1);
		case 9:
			return curLoc.add(-1, 2);
		case 10:
			return curLoc.add(-1, 1);
		case 11:
			return curLoc.add(-2, 1);
		case 12:
			return curLoc.add(-1, 0);
		case 13:
			return curLoc.add(-2, -1);
		case 14:
			return curLoc.add(-1, -1);
		case 15:
			return curLoc.add(-1, -2);
		default:
			return null;
		}
	}

	/**
	 * get map location going out along given dir. directions hard coded,
	 * clockwise from north along 16ths
	 * 
	 * @param dir
	 * @param curLoc
	 * @param x
	 *            number of times
	 */
	private MapLocation nextLocOut(int dir, MapLocation curLoc, int x) {
		switch (dir) {
		case 0:
			return curLoc.add(0, -x);
		case 1:
			return curLoc.add(x, -2 * x);
		case 2:
			return curLoc.add(x, -x);
		case 3:
			return curLoc.add(2 * x, -x);
		case 4:
			return curLoc.add(x, 0);
		case 5:
			return curLoc.add(2 * x, x);
		case 6:
			return curLoc.add(x, x);
		case 7:
			return curLoc.add(x, 2 * x);
		case 8:
			return curLoc.add(0, x);
		case 9:
			return curLoc.add(-x, 2 * x);
		case 10:
			return curLoc.add(-x, x);
		case 11:
			return curLoc.add(-2 * x, x);
		case 12:
			return curLoc.add(-x, 0);
		case 13:
			return curLoc.add(-2 * x, -x);
		case 14:
			return curLoc.add(-x, -x);
		case 15:
			return curLoc.add(-x, -2 * x);
		default:
			return null;
		}
	}

	/**
	 * get map location going opposite given dir. directions hard coded,
	 * clockwise from north along 16ths
	 * 
	 * @param dir
	 * @param curLoc
	 */
	private MapLocation nextLocIn(int dir, MapLocation curLoc) {
		switch (dir) {
		case 0:
			return curLoc.add(0, 1);
		case 1:
			return curLoc.add(-1, 2);
		case 2:
			return curLoc.add(-1, 1);
		case 3:
			return curLoc.add(-2, 1);
		case 4:
			return curLoc.add(-1, 0);
		case 5:
			return curLoc.add(-2, -1);
		case 6:
			return curLoc.add(-1, -1);
		case 7:
			return curLoc.add(-1, -2);
		case 8:
			return curLoc.add(0, -1);
		case 9:
			return curLoc.add(1, -2);
		case 10:
			return curLoc.add(1, -1);
		case 11:
			return curLoc.add(2, -1);
		case 12:
			return curLoc.add(1, 0);
		case 13:
			return curLoc.add(2, 1);
		case 14:
			return curLoc.add(1, 1);
		case 15:
			return curLoc.add(1, 2);
		default:
			return null;
		}
	}

	/**
	 * send an out of bounds location to the map
	 * 
	 * @param dir
	 * @param curLoc
	 * @return
	 */
	private MapLocation toBoundary(int dir, MapLocation curLoc) {
		int xoff, yoff;
		// Determine xOffset
		if (curLoc.x < xymin) {
			xoff = xymin - curLoc.x;
		} else if (curLoc.x > xmax) {
			xoff = curLoc.x - xmax;
		} else {
			xoff = 0;
		}
		// Determine yOffset
		if (curLoc.y < xymin) {
			yoff = xymin - curLoc.y;
		} else if (curLoc.y > ymax) {
			yoff = curLoc.y - ymax;
		} else {
			yoff = 0;
		}
		// if adjustment needed to adjust
		if (yoff != 0 || xoff != 0) {
			switch (dir) {
			case 0:
				int off = yoff;
				return curLoc.add(0, off);
			case 1:
				off = Math.max(xoff, yoff / 2);
				return curLoc.add(-off, 2 * off);
			case 2:
				off = Math.max(xoff, yoff);
				return curLoc.add(-off, off);
			case 3:
				off = Math.max(xoff / 2, yoff);
				return curLoc.add(-2 * off, off);
			case 4:
				off = xoff;
				return curLoc.add(-off, 0);
			case 5:
				off = Math.max(xoff / 2, yoff);
				return curLoc.add(-2 * off, -off);
			case 6:
				off = Math.max(xoff, yoff);
				return curLoc.add(-off, -off);
			case 7:
				off = Math.max(xoff, yoff / 2);
				return curLoc.add(-off, -2 * off);
			case 8:
				off = yoff;
				return curLoc.add(0, -off);
			case 9:
				off = Math.max(xoff, yoff / 2);
				return curLoc.add(off, -2 * off);
			case 10:
				off = Math.max(xoff, yoff);
				return curLoc.add(off, -off);
			case 11:
				off = Math.max(xoff / 2, yoff);
				return curLoc.add(2 * off, -off);
			case 12:
				off = xoff;
				return curLoc.add(off, 0);
			case 13:
				off = Math.max(xoff / 2, yoff);
				return curLoc.add(2 * off, off);
			case 14:
				off = Math.max(xoff, yoff);
				return curLoc.add(off, off);
			case 15:
				off = Math.max(xoff, yoff / 2);
				return curLoc.add(off, 2 * off);
			default:
				return null;
			}
		}
		return curLoc;
	}

	/**
	 * init for 16 directions
	 * 
	 * @param pastrLoc
	 */
	private void initMaxDistances(MapLocation pastrLoc) {
		int radius = (int) Math.sqrt(c.attackRadiusSquared);
		int diag = radius * 100 / 141; // for directions 45 degrees out
		int slant = radius * 100 / 223; // for directions 22.5 degrees off

		MapLocation maxCand;
		// Figure out max locations
		for (int dir = 0; dir < numDir; dir++) {
			switch (dir % 4) {
			case 0: // cardinal directions
				maxCand = nextLocOut(dir, pastrLoc, radius);
			case 2: // diagonals
				maxCand = nextLocOut(dir, pastrLoc, diag);
			default: // half diags
				maxCand = nextLocOut(dir, pastrLoc, slant);
			}
			if (rc.canAttackSquare(maxCand)) { // expand range
				while (rc.canAttackSquare(nextLocOut(dir, maxCand))) {
					maxCand = nextLocOut(dir, maxCand);
				}
			} else {
				maxCand = nextLocIn(dir, maxCand); // shrink range
				while (!rc.canAttackSquare(maxCand)) {
					maxCand = nextLocIn(dir, maxCand);
				}
			}
			maxCand = toBoundary(dir, maxCand); // send to boundary if off map
			maxRangeLocs[dir] = maxCand;
			currentTargets[dir] = maxCand;
			maxSquaredOffset[dir] = pastrLoc.distanceSquaredTo(maxCand);
		}
	}

	/**
	 * Init Max distance to spiral from and currentTargets. Also inits offsets
	 * 
	 * @param pastrLoc
	 */
	private void initMaxDistanceAndCurrentTargets(MapLocation pastrLoc) {
		int radius = (int) Math.sqrt(c.attackRadiusSquared);
		int diag = radius * 100 / 141;
		int xymin = -mapExtension;
		int xmax = c.width - 1 + mapExtension; // max x-value on grid
		int ymax = c.height - 1 + mapExtension; // max y-value on grid
		// Figure out max locations
		// NORTH
		Direction curDir = Direction.NORTH;
		MapLocation maxCand = pastrLoc.add(curDir, radius);
		if (maxCand.y < xymin) {
			maxCand = new MapLocation(maxCand.x, xymin); // put location onto
															// map
		}
		if (rc.canAttackSquare(maxCand)) { // expand range
			while (maxCand.y > xymin && rc.canAttackSquare(maxCand.add(curDir))) {
				maxCand = maxCand.add(curDir);
			}
		} else {
			maxCand = maxCand.subtract(curDir); // shrink range
			while (!rc.canAttackSquare(maxCand)) {
				maxCand = maxCand.subtract(curDir);
			}
		}
		maxRangeLocs[0] = maxCand;
		currentTargets[0] = maxCand;
		maxSquaredOffset[0] = pastrLoc.distanceSquaredTo(maxCand);
		// EAST
		curDir = Direction.EAST;
		maxCand = pastrLoc.add(curDir, radius);
		if (maxCand.x > xmax) {
			maxCand = new MapLocation(xmax, maxCand.y); // put location onto map
		}
		if (rc.canAttackSquare(maxCand)) { // expand range
			while (maxCand.x < xmax + mapExtension
					&& rc.canAttackSquare(maxCand.add(curDir))) {
				maxCand = maxCand.add(curDir);
			}
		} else {
			maxCand = maxCand.subtract(curDir); // shrink range
			while (!rc.canAttackSquare(maxCand)) {
				maxCand = maxCand.subtract(curDir);
			}
		}
		maxRangeLocs[2] = maxCand;
		currentTargets[2] = maxCand;
		maxSquaredOffset[2] = pastrLoc.distanceSquaredTo(maxCand);
		// SOUTH
		curDir = Direction.SOUTH;
		maxCand = pastrLoc.add(curDir, radius);
		if (maxCand.y > ymax) {
			maxCand = new MapLocation(maxCand.x, ymax); // put location onto map
		}
		if (rc.canAttackSquare(maxCand)) { // expand range
			while (maxCand.y < ymax && rc.canAttackSquare(maxCand.add(curDir))) {
				maxCand = maxCand.add(curDir);
			}
		} else {
			maxCand = maxCand.subtract(curDir); // shrink range
			while (!rc.canAttackSquare(maxCand)) {
				maxCand = maxCand.subtract(curDir);
			}
		}
		maxRangeLocs[4] = maxCand;
		currentTargets[4] = maxCand;
		maxSquaredOffset[4] = pastrLoc.distanceSquaredTo(maxCand);
		// WEST
		curDir = Direction.WEST;
		maxCand = pastrLoc.add(curDir, radius);
		if (maxCand.x < xymin) {
			maxCand = new MapLocation(xymin, maxCand.y); // put location onto
															// map
		}
		if (rc.canAttackSquare(maxCand)) { // expand range
			while (maxCand.x > xymin && rc.canAttackSquare(maxCand.add(curDir))) {
				maxCand = maxCand.add(curDir);
			}
		} else {
			maxCand = maxCand.subtract(curDir); // shrink range
			while (!rc.canAttackSquare(maxCand)) {
				maxCand = maxCand.subtract(curDir);
			}
		}
		maxRangeLocs[6] = maxCand;
		currentTargets[6] = maxCand;
		maxSquaredOffset[6] = pastrLoc.distanceSquaredTo(maxCand);

		// NORTHEAST
		curDir = Direction.NORTH_EAST;
		maxCand = pastrLoc.add(curDir, diag);
		if (maxCand.x > xmax || maxCand.y < xymin) {
			int worse = Math.max(maxCand.x - xmax, xymin - maxCand.y);
			maxCand = new MapLocation(maxCand.x - worse, maxCand.y + worse); // put
																				// location
																				// onto
																				// map
		}
		if (rc.canAttackSquare(maxCand)) { // expand range
			while (maxCand.y > xymin && maxCand.x < xmax
					&& rc.canAttackSquare(maxCand.add(curDir))) {
				maxCand = maxCand.add(curDir);
			}
		} else {
			maxCand = maxCand.subtract(curDir); // shrink range
			while (!rc.canAttackSquare(maxCand)) {
				maxCand = maxCand.subtract(curDir);
			}
		}
		maxRangeLocs[1] = maxCand;
		currentTargets[1] = maxCand;
		maxSquaredOffset[1] = pastrLoc.distanceSquaredTo(maxCand);
		// SOUTHEAST
		curDir = Direction.SOUTH_EAST;
		maxCand = pastrLoc.add(curDir, diag);
		if (maxCand.x > xmax || maxCand.y > ymax) {
			int worse = Math.max(maxCand.x - xmax, maxCand.y - ymax);
			maxCand = new MapLocation(maxCand.x - worse, maxCand.y - worse); // put
																				// location
																				// onto
																				// map
		}
		if (rc.canAttackSquare(maxCand)) { // expand range
			while (maxCand.y < ymax && maxCand.x < xmax
					&& rc.canAttackSquare(maxCand.add(curDir))) {
				maxCand = maxCand.add(curDir);
			}
		} else {
			maxCand = maxCand.subtract(curDir); // shrink range
			while (!rc.canAttackSquare(maxCand)) {
				maxCand = maxCand.subtract(curDir);
			}
		}
		maxRangeLocs[3] = maxCand;
		currentTargets[3] = maxCand;
		maxSquaredOffset[3] = pastrLoc.distanceSquaredTo(maxCand);
		// SOUTHWEST
		curDir = Direction.SOUTH_WEST;
		maxCand = pastrLoc.add(curDir, diag);
		if (maxCand.x < xymin || maxCand.y > ymax) {
			int worse = Math.max(xymin - maxCand.x, maxCand.y - ymax);
			maxCand = new MapLocation(maxCand.x + worse, maxCand.y - worse); // put
																				// location
																				// onto
																				// map
		}
		if (rc.canAttackSquare(maxCand)) { // expand range
			while (maxCand.y < ymax && maxCand.x > xymin
					&& rc.canAttackSquare(maxCand.add(curDir))) {
				maxCand = maxCand.add(curDir);
			}
		} else {
			maxCand = maxCand.subtract(curDir); // shrink range
			while (!rc.canAttackSquare(maxCand)) {
				maxCand = maxCand.subtract(curDir);
			}
		}
		maxRangeLocs[5] = maxCand;
		currentTargets[5] = maxCand;
		maxSquaredOffset[5] = pastrLoc.distanceSquaredTo(maxCand);
		// NORTHWEST
		curDir = Direction.NORTH_WEST;
		maxCand = pastrLoc.add(curDir, diag);
		if (maxCand.x < xymin || maxCand.y < xymin) {
			int worse = Math.max(xymin - maxCand.x, xymin - maxCand.y);
			maxCand = new MapLocation(maxCand.x + worse, maxCand.y + worse); // put
																				// location
																				// onto
																				// map
		}
		if (rc.canAttackSquare(maxCand)) { // expand range
			while (maxCand.y > xymin && maxCand.x > xymin
					&& rc.canAttackSquare(maxCand.add(curDir))) {
				maxCand = maxCand.add(curDir);
			}
		} else {
			maxCand = maxCand.subtract(curDir); // shrink range
			while (!rc.canAttackSquare(maxCand)) {
				maxCand = maxCand.subtract(curDir);
			}
		}
		maxRangeLocs[7] = maxCand;
		currentTargets[7] = maxCand;
		maxSquaredOffset[7] = pastrLoc.distanceSquaredTo(maxCand);
	}

}
