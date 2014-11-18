package teamKingOfTasks;

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
	private Soldier task;

	// private PathFinderSnailTrail pathFinderSnailTrail;
	// private PathFinderMLineBug pathFinderMLineBug;
	// MapLocation bestPastrLocation = new MapLocation(0, 0);

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
		bestPastrLocation = Channel.getBestPastrLocation(rc);
		role = Channel.requestSoldierRole(rc);
		rc.setIndicatorString(0, role.toString());
		Channel.announceSoldierRole(rc, role);
		pathFinderSnailTrail = new PathFinderSnailTrail(rc);
		pathFinderMLineBug = new PathFinderMLineBug(rc);
		pathFinderSnailTrail.setTarget(bestPastrLocation);
		pathFinderMLineBug.setTarget(bestPastrLocation);
	}

	private void actAttacker() throws GameActionException {
		Team we = rc.getTeam();
		Team opponent = we.opponent();

		MapLocation[] nextToAttack = null;
		// opponent's pastr?
		MapLocation[] pastrOpponentAll = rc.sensePastrLocations(opponent);
		if (pastrOpponentAll != null) {
			nextToAttack = pastrOpponentAll.clone();
		} else {
			// communicating opponents?
			MapLocation[] robotsOpponentAll = rc
					.senseBroadcastingRobotLocations(opponent);
			if (robotsOpponentAll != null) {
				nextToAttack = robotsOpponentAll.clone();
			}
		}

		if (nextToAttack.length != 0) {
			boolean shoot = false;

			// attack any pastr in range
			MapLocation target = nextToAttack[0];
			for (int i = 0; i < nextToAttack.length; i++) {
				target = nextToAttack[i];
				if (rc.canAttackSquare(target)) {
					rc.attackSquare(target);
					shoot = true;
					break;
				}
			}

			if (!shoot) {
				if (!pathFinderSnailTrail.move()) {
					for (Direction dir : C.DIRECTIONS) {
						if (rc.canMove(dir)) {
							rc.move(dir);
							break;
						}
					}
				}
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
						rc.move(moveDirection);
					}
				}
			}
		}

	}
}
