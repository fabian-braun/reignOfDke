package ext_examplefuncsplayer;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class RobotPlayer {

	public static void run(RobotController rc) {
		// causes exeption: Random rand = new Random();
		Direction[] directions = { Direction.NORTH, Direction.NORTH_EAST,
				Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH,
				Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };

		int rand = 0;
		Random r = new Random();
		while (true) {
			rand = ((rand + 191) * 103) % 101;
			if (rc.getType() == RobotType.HQ) {
				try {
					// Check if a robot is spawnable and spawn one if it is
					if (rc.isActive() && rc.senseRobotCount() < 25) {
						Direction toEnemy = rc.getLocation().directionTo(
								rc.senseEnemyHQLocation());
						if (rc.senseObjectAtLocation(rc.getLocation().add(
								toEnemy)) == null) {
							rc.spawn(toEnemy);
						}
					}
				} catch (Exception e) {
					System.out.println("HQ Exception");
				}
			}

			if (rc.getType() == RobotType.SOLDIER) {
				try {
					if (rc.isActive()) {
						// causes exeption: int action = (rc.getRobot().getID()
						// * rand.nextInt(101) + 50) % 101;
						int action = (rc.getRobot().getID() * rand + 50) % 151;
						// Construct a PASTR
						if (action < 1
								&& rc.getLocation().distanceSquaredTo(
										rc.senseHQLocation()) > 6) {
							rc.construct(RobotType.PASTR);
							// Attack a random nearby enemy
						} else if (action < 30) {
							Robot[] nearbyEnemies = rc.senseNearbyGameObjects(
									Robot.class, 10, rc.getTeam().opponent());
							if (nearbyEnemies.length > 0) {
								RobotInfo robotInfo = rc
										.senseRobotInfo(nearbyEnemies[0]);
								rc.attackSquare(robotInfo.location);
							}
							// Move in a random direction
						} else if (action < 80) {
							Direction moveDirection = directions[r.nextInt(8)];
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
				} catch (Exception e) {
					System.out.println("Soldier Exception");
				}
			}

			rc.yield();
		}
	}
}
