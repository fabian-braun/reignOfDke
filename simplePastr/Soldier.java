package simplePastr;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Soldier extends AbstractRobotType {

	private boolean inactive = false;

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
		int action = randall.nextInt(100);
		// Construct a PASTR
		if (action < 1
				&& rc.getLocation().distanceSquaredTo(rc.senseHQLocation()) > 6) {
			rc.construct(RobotType.PASTR);
			// Attack a random nearby enemy
		} else if (action < 30) {
			Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class, 10,
					rc.getTeam().opponent());
			if (nearbyEnemies.length > 0) {
				RobotInfo robotInfo = rc.senseRobotInfo(nearbyEnemies[0]);
				rc.attackSquare(robotInfo.location);
			}
			// Move in a random direction
		} else if (action < 80) {
			Direction moveDirection = C.DIRECTIONS[randall.nextInt(8)];
			if (rc.canMove(moveDirection)) {
				rc.move(moveDirection);
			}
			// Sneak towards the enemy
		} else {
			Direction toEnemy = rc.getLocation().directionTo(
					rc.senseEnemyHQLocation());
			if (rc.canMove(toEnemy)) {
				rc.sneak(toEnemy);
			}
		}

	}

	@Override
	protected void init() throws GameActionException {
		// TODO Auto-generated method stub

	}

}
