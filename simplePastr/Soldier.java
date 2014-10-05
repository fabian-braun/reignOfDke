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

public class Soldier extends AbstractRobotType {

	private boolean inactive = false;
	private boolean pastrBuilder = false;
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
		// Construct a PASTR
		if (pastrBuilder) {
			actPastrBuilder();
		} else {
			actProtector();
		}
	}

	@Override
	protected void init() throws GameActionException {
		bestPastrLocation = Channel.getBestPastrLocation(rc);

		// TODO: change communication. HQ says which should be created
		this.pastrBuilder = Channel.requestSoldierRole(rc,
				SoldierRole.PASTR_BUILDER);
		pathFinder = new PathFinderSimple(rc);
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

	private void visit(MapLocation loc) {
		for (MapLocation old : visited.keySet()) {
			visited.put(old, visited.get(old) + 1);
		}
		visited.put(loc, 0);
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
