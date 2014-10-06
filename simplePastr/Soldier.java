package simplePastr;

import java.util.HashMap;
import java.util.Random;

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
	private PathFinder pathFinder;
	private Random randall;
	MapLocation bestPastrLocation = new MapLocation(0, 0);
	HashMap<MapLocation, Integer> visited = new HashMap<MapLocation, Integer>();

	public Soldier(RobotController rc) {
		super(rc);
		randall = new Random(rc.getRobot().getID());
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
		pathFinder = new PathFinderSimple(rc);
	}

	private void visit(MapLocation loc) {
		for (MapLocation old : visited.keySet()) {
			visited.put(old, visited.get(old) + 1);
		}
		visited.put(loc, 0);
	}

	private void actAttacker() throws GameActionException {
		Team we = rc.getTeam();
		Team opponent = we.opponent();
		MapLocation currentLoc = rc.getLocation();
		visit(currentLoc);

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

		if (nextToAttack != null) {
			// find direction of where to attack next
			MapLocation target = nextToAttack[0];
			Direction nextDir = pathFinder.getNextDirection(visited, target,
					currentLoc);
			while (target == null || !rc.canMove(nextDir)) {
				target = nextToAttack[(int) (nextToAttack.length * Math
						.random())];
				nextDir = pathFinder.getNextDirection(visited, target,
						currentLoc);
			}

			if (!rc.canAttackSquare(target)) {
				rc.sneak(nextDir);
			} else {
				rc.attackSquare(target);
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
			visit(currentLoc);
			Direction dir = pathFinder.getNextDirection(visited,
					bestPastrLocation, currentLoc);
			if (rc.canMove(dir)) {
				rc.move(dir);
			}
		}
	}

	private void actProtector() throws GameActionException {
		MapLocation currentLoc = rc.getLocation();
		visit(currentLoc);
		if (PathFinder.distance(currentLoc, bestPastrLocation) > 10) {
			Direction dir = pathFinder.getNextDirection(visited,
					bestPastrLocation, currentLoc);
			if (rc.canMove(dir)) {
				rc.move(dir);
			}
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
					Direction moveDirection = directions[randall.nextInt(8)];
					if (rc.canMove(moveDirection)) {
						rc.move(moveDirection);
					}
				}
			}
		}

	}
}
