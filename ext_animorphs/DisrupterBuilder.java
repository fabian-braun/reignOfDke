package ext_animorphs;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class DisrupterBuilder {

	public Controller c;
	public RobotController rc;

	// HELPER MODULES
	// will send out messages on this
	public RobotMessager hqResponse;
	public Sensor sensor;
	public Bugger bugger;
	public FastLocSet HQDangerSquares;

	// CONSTANTS
	public final int senseRadiusSquared;
	public final int attackRadiusSquared;
	public final int noiseTowerRange = RobotType.NOISETOWER.attackRadiusMaxSquared;

	// INTERNAL MEMORY
	public final MapLocation myHQ;
	public MapLocation pasturePoint; // Spot that had a pasture

	public boolean far; // whether you're past the half-way out point
	public Direction hqDir;
	public Direction outDir; // Direction we choose to set out along
	public Direction secondaryDir; // Secondary direction. Roughly perpendicular
									// to original path
	public Direction extremeDir;
	public int moveCount;

	public int endState; // Track final state on end. Int value -> meaning as
							// followss

	// -1 You fucked up. No Pasture Point.
	// 0 Success
	// 1 Stopped on way out - bugger got stuck.
	// 2 Stopped on way - enemy
	// 3 Stopped on way - enemy approach
	// 4 Enemy found me location

	/**
	 * Init
	 * 
	 * @param c
	 *            robot controller
	 * @param b
	 *            bugger for movement
	 * @param sensor
	 *            sensor for sensing things
	 * @param cmdOut
	 *            RobotMessager used to tell HQ things
	 * @throws GameActionException
	 */
	public DisrupterBuilder(Controller c, Bugger b, Sensor sensor,
			RobotMessager hqResponse, FastLocSet HQDangerSquares)
			throws GameActionException {
		this.c = c;
		this.rc = c.rc;
		bugger = b;
		myHQ = c.teamhq;

		this.sensor = sensor;
		this.hqResponse = hqResponse;

		this.attackRadiusSquared = c.attackRadiusSquared;
		this.senseRadiusSquared = c.sensorRadiusSquared;
		this.HQDangerSquares = HQDangerSquares;

		far = false;
		endState = -1;
	}

	/**
	 * Reset this. Clears pasture point, reverts endState to -1, ends bug
	 */
	public void clear() {
		bugger.endBug();
		pasturePoint = null;
		far = false;
		hqDir = null;
		outDir = null;
		secondaryDir = null;
		moveCount = 0;
	}

	/**
	 * broadcast based on current end state. Does nothing for -1 (uninit-ed) and
	 * 0 (success). Also clears pasture point after.
	 * 
	 * @throws GameActionException
	 */
	public void broadcastFailure() throws GameActionException {
		switch (endState) {
		case 1:
			hqResponse.broadcastMsg(MessageType.PROBLEM_BUGGING,
					rc.getLocation());
			break;
		case 2:
		case 3:
			hqResponse.broadcastMsg(MessageType.INTERCEPTED_BY_ENEMY,
					rc.getLocation()); // TODO difference for aggressive
										// intercept
			break;
		case 4:
			hqResponse.broadcastMsg(MessageType.LOCATION_HAS_ENEMY,
					rc.getLocation());
			break;
		default:
			break;
		}
		clear();
	}

	public void setEnemyPasturePoint(MapLocation loc) {
		pasturePoint = loc;
		bugger.startBug(myHQ);
		endState = 1;
	}

	/**
	 * initiate switch to far
	 * 
	 * @param curLoc
	 */
	public void initFar(MapLocation curLoc) {
		far = true;
		hqDir = curLoc.directionTo(myHQ);
		Direction left = hqDir.rotateLeft();
		Direction right = hqDir.rotateRight();
		int xLoc = 0;
		int yLoc = 0;

		// Get weighted center of objectives and their hq;
		MapLocation[] pastrLocs = sensor.getLocs(Sensor.ALLIED_PASTR_LOCS);
		int numPastr = pastrLocs.length;
		if (numPastr == 0) {
			xLoc = pasturePoint.x;
			yLoc = pasturePoint.y;
		} else {
			for (int i = numPastr; --i >= 0;) {
				MapLocation pastrLoc = pastrLocs[i];
				xLoc += pastrLoc.x;
				yLoc += pastrLoc.y;
			}
			xLoc = xLoc / numPastr;
			yLoc = yLoc / numPastr;

		}
		xLoc = (xLoc + c.enemyhq.x) / 2;
		yLoc = (yLoc + c.enemyhq.y) / 2;
		MapLocation ref = new MapLocation(xLoc, yLoc);
		if (curLoc.add(left).distanceSquaredTo(ref) >= curLoc.add(right)
				.distanceSquaredTo(ref)) {
			// pick direction away from their hqs path center
			outDir = left;
			secondaryDir = left.rotateLeft();
			extremeDir = secondaryDir.rotateLeft();
		} else {
			outDir = right;
			secondaryDir = right.rotateRight();
			extremeDir = secondaryDir.rotateRight();
		}
		moveCount = 0;

	}

	/**
	 * Returns whether we want to stay in this state
	 * 
	 * On returning false, stores reason in endState as an int
	 * 
	 * @return whether we want to stay in pasture building state
	 * @throws GameActionException
	 */
	public boolean act() throws GameActionException {

		if (pasturePoint == null) {
			// Wasn't given a place
			clear();
			endState = -1;
			return false;
		}
		MapLocation curLoc = rc.getLocation();

		if (!far) {
			// Trying to go out
			if (curLoc.distanceSquaredTo(pasturePoint) >= noiseTowerRange / 4) { // MAGIC
																					// CONSTANT
				// switch states
				initFar(curLoc);
				return actFar(curLoc);
			}
			return actClose(curLoc);
		} else {
			return actFar(curLoc);
		}
	}

	/**
	 * Bug Towards Base. Broadcast Failure when near enemies After building on
	 * high cow location return false
	 * 
	 * @return whether we want to stay in the pasture making state
	 * @throws GameActionException
	 */
	public boolean actClose(MapLocation cur) throws GameActionException {
		Robot[] ens = sensor.getBots(Sensor.SENSE_RANGE_ENEMIES);
		if (ens.length > 0) {
			if (sensor.getBots(Sensor.ATTACK_RANGE_ENEMIES).length > 0) {
				// enemy approached
				endState = 3;
				return false;
			}
			if (!rc.isActive()) {
				return true; // end turn, not active
			}
			// NEAR WALL, probably good to bug around
			if (bugger.bugging && bugger.hugging) {
				Direction dir = bugger.bug();
				if (dir == Direction.OMNI || dir == Direction.NONE) {
					// we fucked up somewhere
					// #YOLOSWAG
					rc.construct(RobotType.NOISETOWER);
					endState = 1;
					return false;
				} else {
					MapLocation target = cur.add(dir);
					if (rc.senseNearbyGameObjects(Robot.class, target,
							attackRadiusSquared, c.enemy).length > 0) {
						endState = 2;
						return false;
					}
					rc.move(dir);
					return true;
				}
			}
			bugger.endBug();
			Direction dir = getAvoidDir(ens, cur);
			if (dir == Direction.NONE) {// No easy way around enemies
				bugger.startBug(myHQ); // Maybe because of wall in the way?
				dir = bugger.bug();
				if (dir == Direction.OMNI || dir == Direction.NONE) {
					// we fucked up somewhere
					// #YOLOSWAG
					rc.construct(RobotType.NOISETOWER);
					endState = 1;
					return false;
				}
				MapLocation target = cur.add(dir);
				if (rc.senseNearbyGameObjects(Robot.class, target,
						attackRadiusSquared, c.enemy).length == 0) {
					rc.move(dir);
					return true;
				}
				endState = 2;
				return false;
			} else {
				MapLocation target = cur.add(dir);
				if (rc.senseNearbyGameObjects(Robot.class, target,
						attackRadiusSquared, c.enemy).length > 0) {
					endState = 2;
					return false;
				}
				rc.move(dir);
				return true;
			}
		} else {
			// NO ENEMIES
			if (!rc.isActive()) {
				return true;
			}
			Direction dir = bugger.bug();
			if (dir == Direction.OMNI || cur.distanceSquaredTo(myHQ) < 2) {// Construct
				rc.construct(RobotType.NOISETOWER);
				endState = 0;
				return false;
			}
			if (dir == Direction.NONE) {
				// #YOLOSWAG
				rc.construct(RobotType.NOISETOWER);
				endState = 1;
				return false;
			}
			rc.move(dir);
			return true;
		}
	}

	/**
	 * If enemy moves into attack range, return false
	 * 
	 * @return whether we want to stay in pasture building state
	 * @throws GameActionException
	 */
	public boolean actFar(MapLocation cur) throws GameActionException {
		Robot[] ens = sensor.getBots(Sensor.SENSE_RANGE_ENEMIES);
		if (ens.length > 0) {
			if (sensor.getBots(Sensor.ATTACK_RANGE_ENEMIES).length > 0) {
				// enemy approached
				endState = 3;
				return false;
			}
			if (!rc.isActive()) {
				return true; // end turn, not active
			}
			// found us
			endState = 4;
			return false;
		}
		// NO ENEMIES AROUND
		else {
			if (!rc.isActive()) {
				return true; // end turn
			}
			if (cur.distanceSquaredTo(pasturePoint) >= noiseTowerRange / 2) { // MAGIC
																				// CONSTANT,
																				// how
																				// far
																				// away
																				// we
																				// build
																				// noise
				rc.construct(RobotType.NOISETOWER);
				endState = 0;
				return false;
			}

			if (bugger.bugging && bugger.hugging) {
				Direction dir = bugger.bug();
				if (cur.distanceSquaredTo(myHQ) <= 2) {
					rc.construct(RobotType.NOISETOWER);
					endState = 0;
					return false;
				}
				if (dir == Direction.OMNI || dir == Direction.NONE) {
					// #YOLOSWAG
					rc.construct(RobotType.NOISETOWER);
					endState = 1;
					return false;
				}
				rc.move(dir);
				return true;
			}

			// try outDir, secondaryDir, hqDir in that order. If number of
			// non-hq moves under 5, also try a extremeDir before hqDir.
			// OUTDIR
			if (tooFar(outDir, cur)) {
				rc.construct(RobotType.NOISETOWER);
				endState = 0;
				return false;
			}
			if (rc.canMove(outDir)) {
				moveCount++;
				rc.move(outDir);
				bugger.endBug();
				return true;
			}

			// SECONDARY DIR
			if (tooFar(secondaryDir, cur)) {
				rc.construct(RobotType.NOISETOWER);
				endState = 0;
				return false;
			}
			if (rc.canMove(secondaryDir)) {
				moveCount++;
				rc.move(secondaryDir);
				bugger.endBug();
				return true;
			}
			// EXTREME
			if (rc.canMove(extremeDir) && moveCount <= 5) { // Magic Constant
				moveCount++;
				rc.move(extremeDir);
				bugger.endBug();
				return true;
			}
			// HQ
			if (tooFar(hqDir, cur)) {
				rc.construct(RobotType.NOISETOWER);
				endState = 0;
				return false;
			}
			if (rc.canMove(hqDir)) {
				bugger.endBug();
				rc.move(hqDir);
				return true;
			}

			if (moveCount <= 3) { // Magic Constant avoid just hitting a wall
									// right off the bat
				bugger.startBug(myHQ);
				Direction dir = bugger.bug();
				if (dir == Direction.NONE || dir == Direction.OMNI) {
					// #YOLOSWAG
					rc.construct(RobotType.NOISETOWER);
					endState = 1;
					return false;
				}
				rc.move(dir);
				return true;
			}
			// well... fuck it
			// #YOLOSWAG
			rc.construct(RobotType.NOISETOWER);
			endState = 1;
			return false;
		}
	}

	public boolean tooFar(Direction dir, MapLocation cur) {
		MapLocation to = cur.add(dir);
		if (to.distanceSquaredTo(pasturePoint) > noiseTowerRange) {
			return true;
		}
		return false;
	}

	/**
	 * Return a direction to go around enemies.
	 * 
	 * @param ens
	 *            List of nearby enemies
	 * @return Direction to go towards buildLocation weighted away from enemies
	 */
	public Direction getAvoidDir(Robot[] ens, MapLocation cur)
			throws GameActionException {
		if (!sensor.infoInitSenseEnemies) {
			sensor.initSenseEnemyInfo();
		}
		int remaining_directions = 7; // binary rep of directions remaining:
										// main, left right
		Direction main = cur.directionTo(myHQ);
		if (!rc.canMove(main)) {
			remaining_directions = 3;
		}
		Direction left = main.rotateLeft();
		if (!rc.canMove(left)) {
			remaining_directions = remaining_directions - 2;
		}
		Direction right = main.rotateRight();
		if (!rc.canMove(right)) {
			remaining_directions--;
		}
		RobotInfo[] enInfo = sensor.info.get(ens);

		// check enemy robots for constraints
		for (int i = enInfo.length; --i >= 0;) {
			MapLocation enLoc = enInfo[i].location;
			Direction enDir = cur.directionTo(enLoc);
			switch (remaining_directions) {
			case 7: {
				if (enDir == main) {
					remaining_directions = 3;
					// check which side its on
					if (main.isDiagonal()) {
						enDir = cur.add(main, 2).directionTo(enLoc);
						if (enDir == left) {
							return right;
						} else if (enDir == right) {
							return left;
						} else {
							continue;
						}
					} else {
						enDir = cur.add(main, 3).directionTo(enLoc);
						if (enDir == left) {
							return right;
						} else if (enDir == right) {
							return left;
						} else {
							continue;
						}
					}
				}
				if (enDir == left) {
					remaining_directions = 5;
					continue;
				}
				if (enDir == right) {
					remaining_directions = 6;
					continue;
				}
			}
			case 6: {
				if (enDir == main) {
					return left;
				}
				if (enDir == left) {
					return main;
				}
				break;
			}
			case 5: {
				if (enDir == main) {
					return right;
				}
				if (enDir == right) {
					return main;
				}
				break;
			}
			case 3: {
				if (enDir == left) {
					return right;
				}
				if (enDir == right) {
					return left;
				}
				break;
			}
			case 4:
				return main;
			case 2:
				return left;
			case 1:
				return right;
			default:
				return Direction.NONE;
			}
		}
		if (remaining_directions > 3) {
			return main;
		} else {
			if (remaining_directions >= 2) {
				return left;
			}
			if (remaining_directions == 1) {
				return right;
			}
			return Direction.NONE;
		}
	}

}
