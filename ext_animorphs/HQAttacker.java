package ext_animorphs;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotInfo;

public class HQAttacker {
	HQController hc;
	MapLocation myLocation;
	double[][] damageValues;
	MapLocation splashBestTargetLocation;
	double splashBestTargetDamage;

	static final int RADIUS_CHECK = 4; // Assuming hc.attackRAdiusMaxSquared =
										// 15
	static final int RADIUS_MAX_SQUARED_ADDER = 10; // Assuming
													// hc.attackRAdiusMaxSquared
													// = 15

	public HQAttacker(HQController hc) {
		this.hc = hc;
		myLocation = hc.rc.getLocation();
		damageValues = new double[2 * RADIUS_CHECK + 1][2 * RADIUS_CHECK + 1];
	}

	public MapLocation quickDamage() throws GameActionException {
		int splashAttackRadius = (int) (hc.attackRadiusSquared + RADIUS_MAX_SQUARED_ADDER);
		Robot[] enemyRobots = hc.rc.senseNearbyGameObjects(Robot.class,
				splashAttackRadius, hc.enemy);

		if (enemyRobots.length == 0) {
			return null;
		} else if (enemyRobots.length == 1) {
			RobotInfo rInfo = hc.rc.senseRobotInfo(enemyRobots[0]);
			MapLocation robotLoc = rInfo.location;
			double robotHealth = rInfo.health;
			if (hc.rc.canAttackSquare(robotLoc)) {
				int surroundingAllies = hc.rc.senseNearbyGameObjects(
						Robot.class, robotLoc, 2, hc.team).length;
				int surroundingEnemies = hc.rc.senseNearbyGameObjects(
						Robot.class, robotLoc, 2, hc.enemy).length;
				if (myLocation.isAdjacentTo(robotLoc)) {
					surroundingAllies--;
				}

				if (surroundingEnemies + 1 >= surroundingAllies) {
					return robotLoc;
				}
			} else {
				MapLocation adjacentLoc = robotLoc.add(robotLoc
						.directionTo(myLocation));
				int surroundingAllies = hc.rc.senseNearbyGameObjects(
						Robot.class, adjacentLoc, 2, hc.team).length;
				int surroundingEnemies = hc.rc.senseNearbyGameObjects(
						Robot.class, adjacentLoc, 2, hc.enemy).length;
				if (myLocation.isAdjacentTo(adjacentLoc)) {
					surroundingAllies--;
				}
				if (hc.rc.senseObjectAtLocation(adjacentLoc) != null
						&& hc.rc.senseObjectAtLocation(adjacentLoc).getTeam() == hc.team) {
					surroundingAllies += 2;
				}
				if (hc.rc.canAttackSquare(adjacentLoc)
						&& surroundingEnemies > surroundingAllies) {
					return adjacentLoc;
				} else {
					MapLocation leftLocation = robotLoc.add(robotLoc
							.directionTo(myLocation).rotateLeft());

					surroundingAllies = hc.rc.senseNearbyGameObjects(
							Robot.class, leftLocation, 2, hc.team).length;
					surroundingEnemies = hc.rc.senseNearbyGameObjects(
							Robot.class, leftLocation, 2, hc.enemy).length;
					if (myLocation.isAdjacentTo(leftLocation)) {
						surroundingAllies--;
					}
					if (hc.rc.senseObjectAtLocation(leftLocation) != null
							&& hc.rc.senseObjectAtLocation(leftLocation)
									.getTeam() == hc.team) {
						surroundingAllies += 2;
					}

					if (hc.rc.canAttackSquare(leftLocation)
							&& surroundingEnemies > surroundingAllies) {
						return leftLocation;
					} else {
						MapLocation rightLocation = robotLoc.add(robotLoc
								.directionTo(myLocation).rotateRight());

						surroundingAllies = hc.rc.senseNearbyGameObjects(
								Robot.class, rightLocation, 2, hc.team).length;
						surroundingEnemies = hc.rc.senseNearbyGameObjects(
								Robot.class, rightLocation, 2, hc.enemy).length;

						if (myLocation.isAdjacentTo(rightLocation)) {
							surroundingAllies--;
						}
						if (hc.rc.senseObjectAtLocation(rightLocation) != null
								&& hc.rc.senseObjectAtLocation(rightLocation)
										.getTeam() == hc.team) {
							surroundingAllies += 2;
						}

						if (hc.rc.canAttackSquare(rightLocation)
								&& surroundingEnemies > surroundingAllies) {
							return rightLocation;
						}
					}
				}
			}
			return null;
		} else {
			for (Robot r : enemyRobots) {
				RobotInfo rInfo = hc.rc.senseRobotInfo(r);
				MapLocation robotLoc = rInfo.location;

				if (hc.rc.canAttackSquare(robotLoc)) {
					int surroundingAllies = hc.rc.senseNearbyGameObjects(
							Robot.class, robotLoc, 2, hc.team).length;
					int surroundingEnemies = hc.rc.senseNearbyGameObjects(
							Robot.class, robotLoc, 2, hc.enemy).length;
					if (myLocation.isAdjacentTo(robotLoc)) {
						surroundingAllies--;
					}
					if (surroundingEnemies > surroundingAllies) {
						return robotLoc;
					}
				}
			}

			for (Robot r : enemyRobots) {
				RobotInfo rInfo = hc.rc.senseRobotInfo(r);
				MapLocation robotLoc = rInfo.location;
				MapLocation adjacentLoc = robotLoc.add(robotLoc
						.directionTo(myLocation));

				if (hc.rc.canAttackSquare(adjacentLoc)) {
					int surroundingAllies = hc.rc.senseNearbyGameObjects(
							Robot.class, adjacentLoc, 2, hc.team).length;
					int surroundingEnemies = hc.rc.senseNearbyGameObjects(
							Robot.class, adjacentLoc, 2, hc.enemy).length;
					if (myLocation.isAdjacentTo(adjacentLoc)) {
						surroundingAllies--;
					}
					if (hc.rc.senseObjectAtLocation(adjacentLoc) != null
							&& hc.rc.senseObjectAtLocation(adjacentLoc)
									.getTeam() == hc.team) {
						surroundingAllies += 2;
					}
					if (surroundingEnemies > surroundingAllies) {
						return adjacentLoc;
					}
				}
			}

			return null;
		}
	}

	// private int countSurroundingAllies(MapLocation robotLoc) throws
	// GameActionException{
	// int allyCount=0;
	// for(int x=-1; x<=1; x++){
	// for(int y=-1; y<=1; y++){
	// Robot target = (Robot)hc.rc.senseObjectAtLocation(robotLoc.add(x,y));
	// if(target!=null && target.getTeam()==hc.team && target!=hc.robot){
	// allyCount++;
	// }
	// }
	// }
	// return allyCount;
	// }

	public MapLocation maximizeNetDamage() throws GameActionException {
		int splashAttackRadius = (int) (hc.attackRadiusSquared + RADIUS_MAX_SQUARED_ADDER);
		Robot[] enemyRobots = hc.rc.senseNearbyGameObjects(Robot.class,
				splashAttackRadius, hc.enemy);

		if (enemyRobots.length == 0) {
			return null;
		}

		for (int x = 0; x <= 2 * RADIUS_CHECK; x++) {
			for (int y = 0; y <= 2 * RADIUS_CHECK; y++) {
				damageValues[x][y] = 0;
			}
		}

		Robot[] alliedRobots = hc.rc.senseNearbyGameObjects(Robot.class,
				splashAttackRadius, hc.team);

		for (Robot r : enemyRobots) {
			RobotInfo rInfo = hc.rc.senseRobotInfo(r);
			MapLocation rLocation = rInfo.location;
			damageValues[rLocation.x - myLocation.x + RADIUS_CHECK][rLocation.y
					- myLocation.y + RADIUS_CHECK] = rInfo.health;
		}
		for (Robot r : alliedRobots) {
			RobotInfo rInfo = hc.rc.senseRobotInfo(r);
			MapLocation rLocation = rInfo.location;
			damageValues[rLocation.x - myLocation.x + RADIUS_CHECK][rLocation.y
					- myLocation.y + RADIUS_CHECK] = rInfo.health;
		}
		damageValues[RADIUS_CHECK][RADIUS_CHECK] = 0; // Negate any damage to HQ
														// doesn't count
		double currBestDamage = 0;
		MapLocation currBestLocation = null;

		for (Robot r : enemyRobots) {
			splashTarget(r);
			if (splashBestTargetDamage > currBestDamage) {
				currBestLocation = splashBestTargetLocation;
			}
		}
		return currBestLocation;
	}

	private void splashTarget(Robot r) throws GameActionException {
		MapLocation rLocation = hc.rc.senseRobotInfo(r).location;
		MapLocation currBestLocation = null;
		double currBestDamage = 0;
		double damageAtLocation;

		for (int locChoiceX = -1; locChoiceX <= 1; locChoiceX++) {
			for (int locChoiceY = -1; locChoiceY <= 1; locChoiceY++) {
				MapLocation targetLoc = new MapLocation(rLocation.x
						+ locChoiceX, rLocation.y + locChoiceY);
				damageAtLocation = 0;
				if (hc.rc.canAttackSquare(targetLoc)) {
					for (int splashX = -1; splashX <= 1; splashX++) {
						for (int splashY = -1; splashY <= 1; splashY++) {
							if (splashX == 0 && splashY == 0) {
								damageAtLocation += Math.min(
										damageValues[rLocation.x - myLocation.x
												+ locChoiceX + splashX
												+ RADIUS_CHECK][rLocation.y
												- myLocation.y + locChoiceY
												+ splashY + RADIUS_CHECK], 50);
							} else {
								damageAtLocation += Math.min(
										damageValues[rLocation.x - myLocation.x
												+ locChoiceX + splashX
												+ RADIUS_CHECK][rLocation.y
												- myLocation.y + locChoiceY
												+ splashY + RADIUS_CHECK], 25);
							}
						}
					}
				} else {
					damageAtLocation = -1000;
				}

				if (damageAtLocation > currBestDamage) {
					currBestLocation = rLocation.add(locChoiceX, locChoiceY);
					currBestDamage = damageAtLocation;
				}
			}
		}
		splashBestTargetLocation = currBestLocation;
		splashBestTargetDamage = currBestDamage;
	}
}
