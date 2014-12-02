package test_pathfinding;

import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class RobotPlayer {

	private static MapLocation myHq;
	private static MapLocation otherHq;
	private static PathFinder pathFinder;
	private static boolean initComplete = false;
	private static String pfType = "";
	private static MapLocation target;
	private static int spawnNextInRound = 0;
	private static int robotCount = 0;

	public static void run(RobotController rc) throws GameActionException {
		Random randall = new Random();
		myHq = rc.senseHQLocation();
		otherHq = rc.senseEnemyHQLocation();
		loop: while (true) {
			if (rc.getType().equals(RobotType.HQ)
					&& Clock.getRoundNum() > spawnNextInRound) {
				Direction spawnAt = myHq.directionTo(otherHq);
				if (rc.isActive() && robotCount < 4) {
					if (rc.canMove(spawnAt)) {
						rc.spawn(spawnAt);
						spawnNextInRound += 200;
						robotCount++;
						rc.broadcast(0, robotCount);
					}
				}
			} else if (rc.getType().equals(RobotType.SOLDIER)) { // SOLDIER
				if (!initComplete) {
					System.out.println(Clock.getRoundNum());
					initComplete = true;
					int robotCount = rc.readBroadcast(0);
					switch (robotCount) {
					case 1:
						pfType = "PathFinderGreedy";
						pathFinder = new PathFinderGreedy(rc, randall);
						break;
					case 2:
						pfType = "PathFinderMLineBug";
						pathFinder = new PathFinderMLineBug(rc);
						break;
					case 3:
						pfType = "PathFinderSnailTrail";
						pathFinder = new PathFinderSnailTrail(rc);
						break;
					default:
						pfType = "PathFinderAStar";
						pathFinder = new PathFinderAStar(rc);
						break;
					}
					rc.setIndicatorString(0, pfType);
					target = otherHq.add(otherHq.directionTo(myHq));
					System.out.println("start " + pfType + " in round "
							+ Clock.getRoundNum() + "; used bytecode "
							+ Clock.getBytecodeNum() + "; left bytecode "
							+ Clock.getBytecodesLeft());
					pathFinder.setTarget(target);
					System.out.println("finish calculation of " + pfType
							+ " in round " + Clock.getRoundNum()
							+ "; used bytecode " + Clock.getBytecodeNum()
							+ "; left bytecode " + Clock.getBytecodesLeft());
				} else {
					if (target.equals(rc.getLocation())) {
						System.out.println("finish " + pfType + " in round "
								+ Clock.getRoundNum() + "; used bytecode "
								+ Clock.getBytecodeNum() + "; left bytecode "
								+ Clock.getBytecodesLeft());
						break loop;
					}
					if (rc.isActive())
						pathFinder.move();
				}
			}
			rc.yield();
		}
	}
}
