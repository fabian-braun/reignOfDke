package ext_animorphs;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class NoiseTowerBuilder {
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

	// INTERNAL MEMORY
	public MapLocation buildPoint; // Spot HQ suggested
	public Robot[] enemies;
	public boolean nearedPoint; // whether you've seen buildPoint in sense range

	public int endState; // Track final state on end. Int value -> meaning as
							// followss

	// -1 You fucked up. No Pasture Point.
	// 0 Success
	// 1 Stopped on way - bugger got stuck.
	// 2 Stopped on way - enemy
	// 3 Stopped on way - enemy approach
	// 4 Enemy at pasture location

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
	public NoiseTowerBuilder(Controller c, Bugger b, Sensor sensor,
			RobotMessager hqResponse, FastLocSet HQDangerSquares)
			throws GameActionException {
		this.c = c;
		this.rc = c.rc;
		bugger = b;

		this.sensor = sensor;
		this.hqResponse = hqResponse;

		this.attackRadiusSquared = c.attackRadiusSquared;
		this.senseRadiusSquared = c.sensorRadiusSquared;
		this.HQDangerSquares = HQDangerSquares;

		nearedPoint = false;
		endState = -1;
	}

	/**
	 * Reset this. Clears pasture point, reverts endState to -1, ends bug
	 */
	public void clear() {
		bugger.endBug();
		nearedPoint = false;
		buildPoint = null;
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
			hqResponse.broadcastMsg(MessageType.LOCATION_HAS_ENEMY, buildPoint);
			break;
		default:
			break;
		}
		clear();
	}

	public void setTowerPoint(MapLocation loc) {
		buildPoint = loc;
		bugger.startBug(loc);
		endState = 1;
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

		if (buildPoint == null) {
			clear();
			endState = -1;
			return false;
		}
		MapLocation curLoc = rc.getLocation();

		if (nearedPoint) {
			// Trying to build pasture
			return actClose(curLoc);
		} else {
			if (curLoc.distanceSquaredTo(buildPoint) <= senseRadiusSquared) {
				// switch states
				nearedPoint = true;
				return actClose(curLoc);
			} else {
				// Trying to get to location
				return actFar(curLoc);
			}
		}
	}

	/**
	 * On nearing pasture location if enemy nearby pasture, give up and return
	 * false. Start sneaking at some point and look for high cow locations near
	 * point.
	 * 
	 * After building on high cow location return false
	 * 
	 * @return whether we want to stay in the pasture making state
	 * @throws GameActionException
	 */
	public boolean actClose(MapLocation cur) throws GameActionException {
		Robot[] ens = sensor.getBots(Sensor.SENSE_RANGE_ENEMIES);
		if (ens.length > 0) {
			if (rc.senseNearbyGameObjects(Robot.class, buildPoint,
					senseRadiusSquared, c.enemy).length > 0) {
				// enemy around build point
				endState = 4;
				return false;
			}
			if (sensor.getBots(Sensor.ATTACK_RANGE_ENEMIES).length > 0) {
				// enemy approached
				endState = 3;
				return false;
			}
			// Enemy ignorable act as if no enemies
			if (!rc.isActive()) {
				return true;
			}
			Direction dir = bugger.bug();
			if (dir == Direction.OMNI) {
				// enemy around build point
				endState = 4;
				return false;
			}
			if (dir == Direction.NONE) {
				endState = 1;
				return false;
			}
			MapLocation target = cur.add(dir);
			if (rc.senseNearbyGameObjects(Robot.class, target,
					attackRadiusSquared, c.enemy).length > 0) {
				endState = 2;
				return false;
			}
			rc.move(dir);
			return true;
		} else {
			// NO ENEMIES
			if (!rc.isActive()) {
				return true;
			}
			Direction dir = bugger.bug();
			if (dir == Direction.OMNI) {// Construct
				rc.construct(RobotType.NOISETOWER);
				clear();
				endState = 0;
				return false;
			}
			if (dir == Direction.NONE) {
				endState = 1;
				return false;
			}
			MapLocation[] pastrLocs = sensor.getLocs(Sensor.ALLIED_PASTR_LOCS);
			for (int i = pastrLocs.length; --i >= 0;) {
				MapLocation loc = pastrLocs[i];
				if (cur.distanceSquaredTo(loc) <= 1) {
					rc.construct(RobotType.NOISETOWER);
					clear();
					endState = 0;
					return false;
				}
			}
			rc.move(dir);
			return true;
		}
	}

	/**
	 * Default: Tries to bug towards pasture location. On sensing enemy, ends
	 * bug. Treat attack range as wall and try to move towards pasture around
	 * enemy. If enemy moves into attack range, return false
	 * 
	 * @return whether we want to stay in pasture building state
	 * @throws GameActionException
	 */
	public boolean actFar(MapLocation cur) throws GameActionException {
		Robot[] ens = sensor.getBots(Sensor.SENSE_RANGE_ENEMIES);
		if (!sensor.infoInitSenseEnemies) {
			sensor.initSenseEnemyInfo();
		}
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
					endState = 1;
					hqResponse.broadcastMsg(MessageType.PROBLEM_BUGGING,
							buildPoint);
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
				bugger.startBug(buildPoint); // Maybe because of wall in the
												// way?
				dir = bugger.bug();
				if (dir == Direction.OMNI || dir == Direction.NONE) {
					// we fucked up somewhere
					endState = 1;
					hqResponse.broadcastMsg(MessageType.PROBLEM_BUGGING,
							buildPoint);
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
		}
		// NO ENEMIES AROUND
		else {
			if (!rc.isActive()) {
				return true; // end turn
			}
			if (!bugger.bugging) { // restart bug if necessary
				bugger.startBug(buildPoint);
			}
			Direction dir = bugger.bug();
			if (dir == Direction.OMNI || dir == Direction.NONE) {
				// we fucked up somewhere
				endState = 1;
				return false;
			}
			rc.move(dir);
			return true;
		}
	}

	/**
	 * Return a direction to go around enemies.
	 * 
	 * @param ens
	 *            List of nearby enemies
	 * @return Direction to go towards buildLocation weighted away from enemies
	 */
	public Direction getAvoidDir(Robot[] ens, MapLocation cur) {
		int remaining_directions = 7; // binary rep of directions remaining:
										// main, left right
		Direction main = cur.directionTo(buildPoint);
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
