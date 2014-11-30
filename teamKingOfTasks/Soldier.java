package teamKingOfTasks;

import teamreignofdke.PathFinderGreedy;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class Soldier extends AbstractRobotType {

	private boolean inactive = false;
	private SoldierRole role;
	private PathFinderSnailTrail pathFinderSnailTrail;
	private PathFinderMLineBug pathFinderMLineBug;
	private PathFinderGreedy pathFinderGreedy;
	MapLocation bestPastrLocation = new MapLocation(0, 0);
	private Team us;
	private Team opponent;
	private boolean fleeMode = false;
	private boolean kamikazeMode = false;
	private static final double HEALTH_ABOUT_TO_DIE = 40;
	private static final double HEALTH_REGENERATED = 50;

	public Soldier(RobotController rc) {
		super(rc);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see teamreignofdke.AbstractRobotType#act()
	 */
	@Override
	protected void act() throws GameActionException {
		if (inactive || !rc.isActive()) {
			return;
		}
		if (rc.isConstructing()) {
			inactive = true;
		}
		switch (role) {
		case ATTACKER:
			actAttacker();
			break;
		case NOISE_TOWER_BUILDER:
			actNoiseTowerBuilder();
			break;
		case PASTR_BUILDER:
			actPastrBuilder();
			break;
		case PROTECTOR:
			actProtector();
			break;
		default:
			break;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see teamreignofdke.AbstractRobotType#init()
	 */
	@Override
	protected void init() throws GameActionException {
		Channel.announceSoldierType(rc, RobotType.SOLDIER);
		bestPastrLocation = Channel.getBestPastrLocation(rc);
		role = Channel.requestSoldierRole(rc);
		rc.setIndicatorString(0, role.toString());
		Channel.announceSoldierRole(rc, role);
		pathFinderSnailTrail = new PathFinderSnailTrail(rc);
		pathFinderMLineBug = new PathFinderMLineBug(rc);
		pathFinderSnailTrail.setTarget(bestPastrLocation);
		pathFinderMLineBug.setTarget(bestPastrLocation);
		pathFinderGreedy = new PathFinderGreedy(rc, randall);
		us = rc.getTeam();
		opponent = us.opponent();
	}

	/**
	 * Act as an attacker
	 * 
	 * @throws GameActionException
	 */
	private void actAttacker() throws GameActionException {
		if (kamikaze()) {
			return;
		}
		MapLocation[] nextToAttack = null;

		Robot[] soldiersInRange = rc.senseNearbyGameObjects(Robot.class,
				RobotType.SOLDIER.sensorRadiusSquared, opponent);

		if (notNull(soldiersInRange)) {
			nextToAttack = new MapLocation[soldiersInRange.length];
			int k = 0;
			for (Robot robot : soldiersInRange) {
				RobotInfo info = rc.senseRobotInfo(robot);
				if (info.type == RobotType.HQ) {
					nextToAttack[k] = new MapLocation(Integer.MAX_VALUE / 2,
							Integer.MAX_VALUE / 2);
				} else {
					nextToAttack[k] = info.location;
				}
				k++;
			}
		} else {
			// opponent's pastr?
			MapLocation[] pastrLocationsOpponent = rc
					.sensePastrLocations(opponent);
			if (notNull(pastrLocationsOpponent)) {
				nextToAttack = pastrLocationsOpponent;
			} else {
				// communicating opponents?
				MapLocation[] robotsOpponentAll = rc
						.senseBroadcastingRobotLocations(opponent);
				if (notNull(robotsOpponentAll)) {
					nextToAttack = robotsOpponentAll;
				}
			}
		}

		if (notNull(nextToAttack)) {
			MapLocation target = nextToAttack[0];

			// find closest target
			int minDistance = Integer.MAX_VALUE;
			for (int i = 0; i < nextToAttack.length; i += 2) {
				int distance = PathFinder.distance(nextToAttack[i],
						rc.getLocation());
				if (distance < minDistance) {
					target = nextToAttack[i];
					minDistance = distance;
				}
				if (minDistance < RobotType.SOLDIER.attackRadiusMaxSquared) {
					break;
				}
			}
			if (rc.canAttackSquare(target)) {
				rc.attackSquare(target);
			} else {
				if (!pathFinderSnailTrail.getTarget().equals(target)) {
					pathFinderMLineBug.setTarget(target);
				}
				pathFinderMLineBug.move();
			}

		} else {
			actProtector();
		}
	}

	/**
	 * Act as a pastr builder
	 * 
	 * @throws GameActionException
	 */
	private void actPastrBuilder() throws GameActionException {
		MapLocation currentLoc = rc.getLocation();
		if (currentLoc.x == bestPastrLocation.x
				&& currentLoc.y == bestPastrLocation.y) {
			rc.construct(RobotType.PASTR);
		} else {
			pathFinderMLineBug.move();
		}
	}

	/**
	 * Act as a noise tower builder.
	 * 
	 * @throws GameActionException
	 */
	private void actNoiseTowerBuilder() throws GameActionException {
		// Get our current location
		MapLocation currentLocation = rc.getLocation();
		// Check if we are adjacent to the best PASTR location
		if (currentLocation.isAdjacentTo(bestPastrLocation)) {
			// Construct our noise tower here
			rc.construct(RobotType.NOISETOWER);
		} else {
			// Move
			pathFinderMLineBug.move();
		}
	}

	/**
	 * Act as a protector
	 * 
	 * @throws GameActionException
	 */
	private void actProtector() throws GameActionException {
		if (flee()) {
			return;
		}

		MapLocation currentLoc = rc.getLocation();
		if (Channel.needSelfDestruction(rc)) {
			MapLocation destroy = Channel.getSelfDestructionLocation(rc);
			if (PathFinder.distance(currentLoc, destroy) > 4) {
				pathFinderMLineBug.move();
			} else {
				rc.attackSquare(destroy);
			}
		} else if (PathFinder.distance(currentLoc, bestPastrLocation) > 4) {
			pathFinderMLineBug.move();
		} else {
			Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class, 10,
					rc.getTeam().opponent());
			if (nearbyEnemies.length > 0) {
				RobotInfo robotInfo = rc.senseRobotInfo(nearbyEnemies[0]);
				rc.attackSquare(robotInfo.location);

			} else {
				// Sneak towards the enemy
				Direction toEnemy = rc.getLocation().directionTo(
						rc.senseEnemyHQLocation());
				if (rc.canMove(toEnemy)) {
					rc.sneak(toEnemy);
				} else {
					// move randomly
					Direction moveDirection = C.DIRECTIONS[randall.nextInt(8)];
					if (rc.canMove(moveDirection)) {
						rc.sneak(moveDirection);
					}
				}
			}
		}
	}

	/**
	 * checks if the current health is lower than HEALTH_ABOUT_TO_DIE. If that
	 * is true the flee mode is entered. In the flee mode the soldier moves away
	 * from the closest opponent.
	 * 
	 * @return
	 * @throws GameActionException
	 */
	private boolean flee() throws GameActionException {
		if (rc.getHealth() < HEALTH_ABOUT_TO_DIE && !fleeMode) {
			fleeMode = true;
			Robot[] soldiersInRange = rc.senseNearbyGameObjects(Robot.class,
					RobotType.SOLDIER.sensorRadiusSquared, opponent);
			MapLocation fleeTo = null;
			if (notNull(soldiersInRange)) {
				MapLocation enemyLoc = rc.senseRobotInfo(soldiersInRange[0]).location;
				MapLocation myLoc = rc.getLocation();
				fleeTo = new MapLocation(myLoc.x - (enemyLoc.x - myLoc.x),
						myLoc.x - (enemyLoc.x - myLoc.x));
				System.out.println("Flee from " + myLoc + " to " + fleeTo
						+ " because of enemy at " + enemyLoc);
			} else {
				fleeTo = rc.senseHQLocation();
			}
			pathFinderGreedy.setTarget(fleeTo);
			rc.setIndicatorString(1, "AAAAAAAAAAAAAAAH");
		} else if (rc.getHealth() > HEALTH_REGENERATED && fleeMode) {
			fleeMode = false;
			return fleeMode;
		} else if (fleeMode) {
			pathFinderGreedy.move();
		}
		return fleeMode;
	}

	/**
	 * checks if the current health is lower than HEALTH_ABOUT_TO_DIE. If that
	 * is true the kamikaze mode is entered. In the kamikaze mode the soldier
	 * moves on tile in the direction of the opponent and self destructs itself.
	 * 
	 * @return
	 * @throws GameActionException
	 */
	private boolean kamikaze() throws GameActionException {
		if (rc.getHealth() < HEALTH_ABOUT_TO_DIE && !kamikazeMode) {
			kamikazeMode = true;
			Robot[] soldiersInRange = rc.senseNearbyGameObjects(Robot.class,
					RobotType.SOLDIER.sensorRadiusSquared, opponent);
			MapLocation fleeTo = null;
			if (notNull(soldiersInRange)) {
				fleeTo = rc.senseRobotInfo(soldiersInRange[0]).location;
			} else {
				rc.selfDestruct();
			}
			pathFinderGreedy.setTarget(fleeTo);
			rc.setIndicatorString(1, "AAAAAAAAAAAAAAAH");
		} else if (rc.getHealth() > HEALTH_REGENERATED && kamikazeMode) {
			kamikazeMode = false;
			return kamikazeMode;
		} else if (kamikazeMode) {
			pathFinderGreedy.move();
			rc.selfDestruct();
		}
		return kamikazeMode;
	}

	/**
	 * checks if the given array is null or empty
	 * 
	 * @param array
	 * @return
	 */
	public static final <T> boolean notNull(T[] array) {
		if (array == null) {
			return false;
		}
		if (array.length < 1) {
			return false;
		}
		return true;
	}
}
