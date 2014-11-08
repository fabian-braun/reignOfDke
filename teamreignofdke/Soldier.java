package teamreignofdke;

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
	MapLocation bestPastrLocation = new MapLocation(0, 0);
	private Team us;
	private Team opponent;

	public Soldier(RobotController rc) {
		super(rc);
	}

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
		us = rc.getTeam();
		opponent = us.opponent();
	}

	private void actAttacker() throws GameActionException {
		MapLocation[] nextToAttack = null;

		Robot[] soldiersInRange = rc.senseNearbyGameObjects(Robot.class,
				RobotType.SOLDIER.sensorRadiusSquared, opponent);

		if (soldiersInRange.length > 0) {
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
			MapLocation[] pastrLocationsOpponent = rc.sensePastrLocations(opponent);
			if (pastrLocationsOpponent.length > 0) {
				nextToAttack = pastrLocationsOpponent;
			} else {
				// communicating opponents?
				MapLocation[] robotsOpponentAll = rc
						.senseBroadcastingRobotLocations(opponent);
				if (robotsOpponentAll.length > 0) {
					nextToAttack = robotsOpponentAll;
				}
			}
		}

		if (nextToAttack.length > 0) {
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
					pathFinderSnailTrail.setTarget(target);
				}
				pathFinderSnailTrail.move();
			}

		} else {
			actProtector();
		}
	}

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

	private void actProtector() throws GameActionException {
		MapLocation currentLoc = rc.getLocation();
		if (PathFinder.distance(currentLoc, bestPastrLocation) > 4) {
			pathFinderSnailTrail.move();
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
}
