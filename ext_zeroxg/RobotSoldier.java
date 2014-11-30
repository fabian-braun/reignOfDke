package ext_zeroxg;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class RobotSoldier {
	private static final int[] directionalLooks = new int[] { 0, 1, -1, 2, -2,
			3, -3, 4 };
	private static final int[] directionalLooksReversed = new int[] { 0, -1, 1,
			-2, 2, -3, 3, 4 };
	private static final int[] attackLooks = new int[] { 1, -1, 0, 2, -2, 3,
			-3, 4 };
	private static final int[] directionalLooksReduced = new int[] { 0, 1, -1 };
	private static final int trailSize = 3;
	private static int trailIndex = 0;
	private static MapLocation[] trail = new MapLocation[trailSize];
	private static RobotController rc = null;
	private static Random rng;

	private static RobotInfo bestTarget;
	private static int attackingAllyCount;
	private static int attackableSoldierCount;
	private static int nearbyAllyCount;
	private static int enemySoldierCount;
	private static int bestTargetDistance;
	private static int enemiesAroundClosestAlly;

	private static Job currentJob;
	private static MapLocation myHQ;
	private static MapLocation myLoc;
	private static Network myNetwork;
	private static NetworkDownloader networkDownloader;
	private static Path currentPath;
	private static int currentPathIndex;
	private static boolean onPathTrack;
	private static MapLocation lastNetworkPoint;
	private static MapLocation previousGoal;
	private static int lonelyTurns;
	private static boolean sneaking;
	private static AlertLevel alertLevel;
	private static MapLocation myPASTR;
	private static boolean isNinja;

	public static void init(RobotController rc) throws GameActionException {
		RobotSoldier.rc = rc;
		myHQ = RobotPlayer.myHQ;
		myNetwork = new Network(rc.getMapWidth(), rc.getMapHeight());
		networkDownloader = new NetworkDownloader(myNetwork);
		lastNetworkPoint = myHQ;
		previousGoal = myHQ;
		rng = new Random();
		alertLevel = AlertLevel.GREEN;
	}

	public static void run(RobotController rc) throws GameActionException {
		sneaking = false;
		myLoc = rc.getLocation();
		updateMyPASTR();
		updateAlertLevel();
		updateNearbyRobots();
		networkDownloader.update(rc);
		if (myNetwork.isNode(myLoc))
			lastNetworkPoint = myLoc;

		int attitude = attitude();
		// rc.setIndicatorString(2, "");
		if (currentJob == null && attitude == 2) {
			currentJob = Channels.jobBoard.getAvailableJob(rc);
		}
		if (currentJob != null) {
			currentJob = currentJob.update(rc);
			if (currentJob != null
					&& (attitude == 2 || myLoc
							.distanceSquaredTo(currentJob.location) < 10)) {
				currentJob.claim(rc);
				// rc.setIndicatorString(2, currentJob.type.ordinal()+"");
			}
		}

		isNinja = currentJob != null && currentJob.type == JobType.NINJA;

		// rc.setIndicatorString(1, "Attitude " + attitude);

		if (rc.isActive()) {
			MapLocation goal = currentGoal();
			if (goal == null)
				return;
			// rc.setIndicatorString(0, "Goal " + goal);
			int distToGoal = myLoc.distanceSquaredTo(goal);
			MapLocation nextPoint = goal;
			if (currentPath != null && currentPath.end.equals(goal))
				nextPoint = currentPath.get(currentPathIndex);
			else {
				currentPath = myNetwork.path(lastNetworkPoint, goal);
				if (!previousGoal.equals(goal)) {
					// clearTrail();
					previousGoal = goal;
				}
				if (currentPath != null) {
					currentPathIndex = 0;
					onPathTrack = false;
				}
			}

			if (attitude < 0 && tryToFlee()) // Flee if outnumbered and possible
				return;
			if (bestTarget != null && bestTargetDistance <= 10) // Attack if
																// enemy in
																// range
			{
				rc.attackSquare(bestTarget.location);
				Channels.stateMap.broadcast(rc, myLoc, State.ATTACKING);
			} else if (attitude > 0 && bestTarget != null
					&& bestTargetDistance <= 21) // charge if agressive and
													// ready
			{
				attackMove(bestTarget.location);
			}
			// else if (attitude == 0 && bestTargetDistance > 21 && alertLevel
			// == AlertLevel.ORANGE)
			// {
			// moveToward(bestTarget.location);
			// }
			else if (attitude == 0 && isNinja) {
				ninjaAvoid(goal, bestTarget.location);
			} else if (attitude > 0 || distToGoal > 21) {
				if (currentPath != null && currentPath.end.equals(goal))
					followCurrentPath(attitude == 0 ? bestTarget.location
							: null);
				else {
					if (currentPath != null)
						followCurrentPath(attitude == 0 ? bestTarget.location
								: null);
					else {
						if (attitude == 0)
							tryToAvoid(goal, bestTarget.location);
						else
							moveToward(goal);
					}
				}
			}
		}
	}

	private static void updateMyPASTR() {
		MapLocation[] myPASTRs = rc.sensePastrLocations(rc.getTeam());
		if (myPASTRs.length > 0)
			myPASTR = myPASTRs[0];
		else
			myPASTR = null;
	}

	private static void updateAlertLevel() throws GameActionException {
		alertLevel = Channels.readAlertLevel(rc);
		if (myPASTR != null && myLoc.distanceSquaredTo(myPASTR) < 16
				&& alertLevel == AlertLevel.ORANGE)
			alertLevel = AlertLevel.GREEN;
	}

	private static MapLocation currentGoal() throws GameActionException {
		MapLocation goal = Channels.readLocation(rc,
				Channels.armyTargetLocation);

		Job jobAt = Channels.jobBoard.getJobAt(rc, myLoc);
		if (jobAt != null) {
			currentJob = jobAt;
			jobAt.claim(rc);
		}

		if (currentJob != null) {
			switch (currentJob.type) {
			case NINJA:
				// System.out.println("I am a NINJA!!");
				MapLocation[] enemyPASTR = rc.sensePastrLocations(rc.getTeam()
						.opponent());
				if (enemyPASTR.length > 0)
					return enemyPASTR[0];
				break;
			case NT_FARMING:
			case NT_OFFENSIVE:
				goal = currentJob.location;
				if (myLoc.equals(currentJob.location)) {
					if (enemySoldierCount < rc.senseNearbyGameObjects(
							Robot.class, 36, RobotPlayer.team).length) {
						rc.construct(RobotType.NOISETOWER);
						return null;
					}
				}
				break;
			case BUILD_PASTR:
				goal = currentJob.location;
				if (myLoc.equals(currentJob.location)) {
					if (enemySoldierCount < rc.senseNearbyGameObjects(
							Robot.class, 36, RobotPlayer.team).length) {
						rc.construct(RobotType.PASTR);
						return null;
						// }
						// Robot[] allies =
						// rc.senseNearbyGameObjects(Robot.class, myLoc, 2,
						// rc.getTeam());
						// if (allies.length == 0)
						// lonelyTurns++;
						// for (Robot r : allies)
						// {
						// RobotInfo info = rc.senseRobotInfo(r);
						// if ((info.isConstructing && info.constructingType ==
						// RobotType.NOISETOWER && info.constructingRounds <=
						// 50) || (info.type == RobotType.NOISETOWER))
						// {
						// rc.construct(RobotType.PASTR);
						// return null;
						// }
						// else
						// lonelyTurns++;
						// }
					}
				}
				break;
			case GOTO:
				goal = currentJob.location;
				break;
			case OUTDATED:
				break;
			default:
				break;
			}
		}

		return goal;
	}

	private static void clearTrail() {
		trail = new MapLocation[trailSize];
	}

	private static void followCurrentPath(MapLocation avoid)
			throws GameActionException {
		MapLocation nextPoint = currentPath.get(currentPathIndex);
		if (myLoc.isAdjacentTo(nextPoint) || myLoc.equals(nextPoint)) {
			// clearTrail();
			currentPathIndex = Math.min(currentPath.size() - 1,
					currentPathIndex + 1);
			lastNetworkPoint = nextPoint;
			nextPoint = currentPath.get(currentPathIndex);
			if (!onPathTrack) {
				onPathTrack = true;
				clearTrail();
			}
			if (avoid == null)
				moveToward(nextPoint);
			else
				tryToAvoid(nextPoint, avoid);
		} else {
			if (avoid == null)
				moveToward(nextPoint);
			else
				tryToAvoid(nextPoint, avoid);
		}
	}

	private static void updateNearbyRobots() throws GameActionException {
		nearbyAllyCount = 0;
		attackingAllyCount = 0;
		enemySoldierCount = 0;
		attackableSoldierCount = 0;
		bestTargetDistance = 0;
		enemiesAroundClosestAlly = 0;
		bestTarget = null;

		Robot[] enemyRobots;
		if (alertLevel == AlertLevel.GREEN)
			enemyRobots = rc.senseNearbyGameObjects(Robot.class, 36,
					RobotPlayer.team.opponent());
		else
			enemyRobots = rc.senseNearbyGameObjects(Robot.class, 21,
					RobotPlayer.team.opponent());

		bestTargetDistance = 0;
		double bestHealth = 0;
		int bestID = 0;

		for (Robot r : enemyRobots) {
			RobotInfo info = rc.senseRobotInfo(r);
			int distance = myLoc.distanceSquaredTo(info.location);
			distance = Math.max(distance, 10);

			switch (info.type) {
			case HQ:
				break;
			case NOISETOWER:
				if (bestTarget == null) {
					bestTarget = info;
					bestTargetDistance = distance;
					bestHealth = info.health;
					bestID = info.robot.getID();
					bestTarget = info;
				}
				break;
			case PASTR:
				if (bestTarget == null || bestTarget.type != RobotType.SOLDIER
						|| bestTargetDistance > 10) {
					bestTarget = info;
					bestTargetDistance = distance;
					bestHealth = info.health;
					bestID = info.robot.getID();
					bestTarget = info;
				}
				break;
			case SOLDIER:
				if (info.isConstructing
						&& info.constructingType == RobotType.PASTR) // TODO
					break;

				enemySoldierCount++;
				if (distance <= 10)
					attackableSoldierCount++;
				if (bestTarget == null
						|| distance < bestTargetDistance
						|| (distance <= 10 && bestTarget.type != RobotType.SOLDIER)
						|| (distance == bestTargetDistance && info.health < bestHealth)
						|| (distance == bestTargetDistance
								&& info.health <= bestHealth && info.robot
								.getID() < bestID)) {
					bestTargetDistance = distance;
					bestHealth = info.health;
					bestID = info.robot.getID();
					bestTarget = info;
				}
				break;
			default:
				break;
			}
		}

		Channels.radarMap.broadcast(rc, myLoc, enemySoldierCount);

		Robot[] alliedRobots;
		// if (alertLevel != AlertLevel.GREEN)
		// System.out.println(alertLevel.name());
		if (bestTarget != null)
			alliedRobots = rc.senseNearbyGameObjects(Robot.class,
					bestTarget.location, 21, RobotPlayer.team);
		else
			alliedRobots = rc.senseNearbyGameObjects(Robot.class, 36,
					RobotPlayer.team);

		for (Robot r : alliedRobots) {
			RobotInfo info = rc.senseRobotInfo(r);
			switch (info.type) {
			case HQ:
				break;
			case NOISETOWER:
				break;
			case PASTR:
				break;
			case SOLDIER:
				if (bestTarget != null && !info.isConstructing) {
					int distance = bestTarget.location
							.distanceSquaredTo(info.location);
					if (distance <= 10) {
						attackingAllyCount++;
					}
					enemiesAroundClosestAlly = Math.max(
							enemiesAroundClosestAlly,
							Channels.radarMap.read(rc, info.location));
					// if (closestAlly == null || distance <
					// closestAllyDistance)
					// {
					// closestAllyDistance = distance;
					// closestAlly = info;
					// }
					nearbyAllyCount++;
				}
				break;
			default:
				break;
			}
		}
		// if (closestAlly != null)
		// enemiesAroundClosestAlly = Channels.radarMap.read(rc,
		// closestAlly.location);

		if (enemySoldierCount == 0) {
			MapLocation[] pastrs = rc.sensePastrLocations(rc.getTeam());
			if (pastrs.length > 0) {
				if (myLoc.distanceSquaredTo(pastrs[0]) < 36)
					sneaking = true;
			}
		}

		// rc.setIndicatorString(0, "enemiesAroundClosestAlly: " +
		// enemiesAroundClosestAlly);
		// rc.setIndicatorString(1, "EnemySoldierCount: " + enemySoldierCount);
		// rc.setIndicatorString(2, "AllySoldierCount: " + nearbyAllyCount);
	}

	private static int attitude() {
		// if (alertLevel == AlertLevel.RED)
		// {
		// if (enemySoldierCount == 0)
		// return 2;
		// return 1;
		// }

		switch (attackableSoldierCount) {
		case 0:
			if (enemySoldierCount == 0)
				return 2;
			if (enemySoldierCount <= nearbyAllyCount
					&& (alertLevel != AlertLevel.GREEN || enemiesAroundClosestAlly <= nearbyAllyCount))
				return 1;
			return 0;
		case 1:
			if (attackingAllyCount == 0 && rc.getHealth() < bestTarget.health
					&& alertLevel == AlertLevel.GREEN)
				return -1;
			return 0;
		default:
			if (attackableSoldierCount > attackingAllyCount + 1
					&& alertLevel == AlertLevel.GREEN)
				return -1;
			return 0;
		}
	}

	private static boolean tryToFlee() throws GameActionException {
		Direction fleeDir;

		if (bestTarget != null)
			fleeDir = bestTarget.location.directionTo(myLoc);
		else {
			if (myLoc.isAdjacentTo(RobotPlayer.myHQ))
				return true;
			fleeDir = myLoc.directionTo(RobotPlayer.myHQ);
		}

		int dirIndex = fleeDir.ordinal() + 8;
		for (int look : bestTarget == null ? directionalLooks
				: directionalLooksReduced) {
			Direction trialDir = Direction.values()[(dirIndex + look) % 8];
			MapLocation nextLoc = myLoc.add(trialDir);
			if (canSafelyMove(trialDir, false, true)
					&& (bestTarget == null || nextLoc
							.distanceSquaredTo(bestTarget.location) > 10)) {
				rc.move(trialDir);
				Channels.stateMap.broadcast(rc, myLoc.add(trialDir),
						State.FLEEING);
				break;
			}
		}

		clearTrail();

		return !rc.isActive();
	}

	private static boolean canAvoid(MapLocation goal, MapLocation avoid) {
		Direction toAvoid = myLoc.directionTo(avoid);
		Direction toGoal = myLoc.directionTo(goal);
		if (toGoal.equals(toAvoid) || toGoal.equals(toAvoid.rotateLeft())
				|| toGoal.equals(toAvoid.rotateRight()))
			return false;
		return true;
	}

	private static void ninjaAvoid(MapLocation loc, MapLocation avoid)
			throws GameActionException {
		// rc.setIndicatorString(2, "Trying to avoid " + Clock.getRoundNum());
		Direction bestDirection = myLoc.directionTo(loc);
		if (bestDirection == Direction.NONE || bestDirection == Direction.OMNI)
			return;
		int dirIndex = bestDirection.ordinal() + 8;
		Direction forbiddenDir1 = myLoc.directionTo(avoid);
		Direction forbiddenDir2 = forbiddenDir1.rotateLeft();
		Direction forbiddenDir3 = forbiddenDir1.rotateRight();

		for (int look : directionalLooks) {
			Direction trialDir = Direction.values()[(dirIndex + look) % 8];
			if (forbiddenDir1.equals(trialDir)
					|| forbiddenDir2.equals(trialDir)
					|| forbiddenDir3.equals(trialDir))
				continue;

			if (canSafelyMove(trialDir, true, true)) {
				rc.move(trialDir);
				MapLocation nextLoc = myLoc.add(trialDir);
				// System.out.println("Using ninja avoid!");
				Channels.stateMap.broadcast(rc, nextLoc, State.WALKING);
				break;
			}
		}
		trail[trailIndex % trailSize] = myLoc;
		trailIndex++;
	}

	private static void tryToAvoid(MapLocation loc, MapLocation avoid)
			throws GameActionException {
		// rc.setIndicatorString(2, "Trying to avoid " + Clock.getRoundNum());
		Direction bestDirection = myLoc.directionTo(loc);
		if (bestDirection == Direction.NONE || bestDirection == Direction.OMNI)
			return;
		int dirIndex = bestDirection.ordinal() + 8;
		Direction forbiddenDir1 = myLoc.directionTo(avoid);
		Direction forbiddenDir2 = forbiddenDir1.rotateLeft();
		Direction forbiddenDir3 = forbiddenDir1.rotateRight();

		for (int look : directionalLooks) {
			Direction trialDir = Direction.values()[(dirIndex + look) % 8];
			if (forbiddenDir1.equals(trialDir)
					|| forbiddenDir2.equals(trialDir)
					|| forbiddenDir3.equals(trialDir))
				break;

			if (canSafelyMove(trialDir, true, true)) {
				rc.move(trialDir);
				MapLocation nextLoc = myLoc.add(trialDir);
				Channels.stateMap.broadcast(rc, nextLoc, State.WALKING);
				break;
			}
		}
		trail[trailIndex % trailSize] = myLoc;
		trailIndex++;
	}

	private static void attackMove(MapLocation loc) throws GameActionException {
		Direction bestDirection = myLoc.directionTo(loc);
		if (bestDirection == Direction.NONE || bestDirection == Direction.OMNI)
			return;
		int dirIndex = bestDirection.ordinal() + 8;
		for (int look : bestDirection.isDiagonal() ? attackLooks
				: directionalLooks) {
			Direction trialDir = Direction.values()[(dirIndex + look) % 8];
			if (canSafelyMove(trialDir, true, true)) {
				rc.move(trialDir);
				MapLocation nextLoc = myLoc.add(trialDir);
				if (nextLoc.distanceSquaredTo(loc) <= RobotType.SOLDIER.attackRadiusMaxSquared)
					Channels.stateMap.broadcast(rc, nextLoc, State.ATTACKING);
				else
					Channels.stateMap.broadcast(rc, nextLoc, State.WALKING);
				break;
			}
		}
		trail[trailIndex % trailSize] = myLoc;
		trailIndex++;
	}

	private static void moveToward(MapLocation loc) throws GameActionException {
		Direction bestDirection = myLoc.directionTo(loc);
		if (bestDirection == Direction.NONE || bestDirection == Direction.OMNI)
			return;
		int dirIndex = bestDirection.ordinal() + 8;
		int[] looks = ((rng.nextInt() + rc.getRobot().getID()) % 2) == 0 ? directionalLooks
				: directionalLooksReversed;
		for (int look : looks) {
			Direction trialDir = Direction.values()[(dirIndex + look) % 8];
			if (canSafelyMove(trialDir, true, true)) {
				try {
					if (sneaking)
						rc.sneak(trialDir);
					else
						rc.move(trialDir);
					MapLocation nextLoc = myLoc.add(trialDir);
					Channels.stateMap.broadcast(rc, nextLoc, State.WALKING);
				} catch (GameActionException e) {
					e.printStackTrace();
				}
				break;
			}
		}
		trail[trailIndex % trailSize] = myLoc;
		trailIndex++;
	}

	private static boolean canSafelyMove(Direction d, boolean avoidTrail,
			boolean avoidEnemyHQ) {
		if (!rc.canMove(d))
			return false;
		MapLocation next = myLoc.add(d);
		if (avoidTrail) {
			for (MapLocation tLoc : trail) {
				if (tLoc != null && !tLoc.equals(myLoc)
						&& (next.isAdjacentTo(tLoc) || next.equals(tLoc)))
					return false;
			}
		}
		if (avoidEnemyHQ) {
			MapLocation loc2 = next.add(next.directionTo(rc
					.senseEnemyHQLocation()));
			double dist2 = loc2.distanceSquaredTo(rc.senseEnemyHQLocation());
			return dist2 > RobotType.HQ.attackRadiusMaxSquared;
		}
		return true;
	}
}
