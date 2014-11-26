package dualcore;

import java.util.ArrayList;
import java.util.List;

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
	private int teamId;
	private int id;
	private PathFinderSnailTrail pathFinderSnailTrail;
	private PathFinderMLineBug pathFinderMLineBug;
	private PathFinderAStar pathFinderAStar;
	protected PathFinderGreedy pathFinderGreedy;
	private Team us;
	private Team opponent;
	private MapLocation enemyHq;

	Task task = Task.GOTO;
	MapLocation target = new MapLocation(0, 0);
	MapLocation myLoc;

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
		Channel.signalAlive(rc, id);
		myLoc = rc.getLocation();
		updateTask();
		if (inactive || !rc.isActive()) {
			return;
		}
		if (rc.isConstructing()) {
			inactive = true;
			return;
		}
		actMicro(target, task);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see teamreignofdke.AbstractRobotType#init()
	 */
	@Override
	protected void init() throws GameActionException {
		Channel.announceSoldierType(rc, RobotType.SOLDIER);
		id = Channel.requestSoldierId(rc);
		Channel.signalAlive(rc, id);

		teamId = Channel.requestTeamId(rc);
		Channel.announceTeamId(rc, id, teamId);
		rc.setIndicatorString(0, "SOLDIER [" + id + "] TEAM [" + teamId + "]");
		target = Channel.getTarget(rc, teamId);
		task = Channel.getTask(rc, teamId);

		us = rc.getTeam();
		opponent = us.opponent();
		pathFinderSnailTrail = new PathFinderSnailTrail(rc);
		pathFinderAStar = new PathFinderAStar(rc, pathFinderSnailTrail.map,
				pathFinderSnailTrail.hqSelfLoc, pathFinderSnailTrail.hqEnemLoc,
				pathFinderSnailTrail.height, pathFinderSnailTrail.width);
		pathFinderMLineBug = new PathFinderMLineBug(rc);
		pathFinderSnailTrail.setTarget(target);
		pathFinderMLineBug.setTarget(target);
		pathFinderGreedy = new PathFinderGreedy(rc, randall);
		enemyHq = rc.senseEnemyHQLocation();
	}

	private void actMicro(MapLocation target, Task task)
			throws GameActionException {
		Robot[] closeOpponents = rc.senseNearbyGameObjects(Robot.class,
				RobotType.SOLDIER.sensorRadiusSquared, opponent);
		boolean oppHqInRange = myLoc.distanceSquaredTo(enemyHq) <= RobotType.SOLDIER.sensorRadiusSquared;
		if (size(closeOpponents) < 1
				|| (size(closeOpponents) == 1 && oppHqInRange)) {
			// no opponents in sight - go for your task
			switch (task) {
			case BUILD_NOISETOWER:
				pathFinderMLineBug.setTarget(target);
				if (myLoc.isAdjacentTo(target)) {
					rc.construct(RobotType.NOISETOWER);
				} else {
					if (!pathFinderMLineBug.move()) {
						doRandomMove();
						pathFinderMLineBug.setTarget(target);
					}
				}
				break;
			case BUILD_PASTR:
				pathFinderMLineBug.setTarget(target);
				if (myLoc.equals(target)) {
					rc.construct(RobotType.PASTR);
				} else {
					if (!pathFinderMLineBug.move()) {
						doRandomMove();
						pathFinderMLineBug.setTarget(target);
					}
				}
				break;
			case GOTO:
				if (!target.equals(pathFinderAStar.getTarget())) {
					pathFinderAStar.setTarget(target);
				}
				if (!pathFinderAStar.move()) {
					doRandomMove();
					pathFinderAStar.setTarget(target);
				}
				// pathFinderMLineBug.setTarget(target);
				// if (!pathFinderMLineBug.move()) {
				// doRandomMove();
				// pathFinderMLineBug.setTarget(target);
				// }
				break;
			case ACCUMULATE:
				pathFinderGreedy.setTarget(target);
				pathFinderGreedy.move();
				break;
			default:
				break;
			}
		} else {
			List<Robot> closeSoldiers = new ArrayList<Robot>();
			for (Robot robot : closeOpponents) {
				RobotInfo ri = rc.senseRobotInfo(robot);
				if (ri.type.equals(RobotType.SOLDIER)) {
					closeSoldiers.add(robot);
				}
			}
			MapLocation oppAt = rc.senseLocationOf(closeOpponents[0]);
			if (oppAt.equals(enemyHq)) {
				oppAt = rc.senseLocationOf(closeOpponents[1]);
			}
			Robot[] closeFriends = rc.senseNearbyGameObjects(Robot.class,
					oppAt, RobotType.SOLDIER.sensorRadiusSquared, us);
			if (closeSoldiers.size() >= size(closeFriends)) {
				// there are more opponents. Better get away.
				MapLocation away = null;
				MapLocation myFriendsLoc = Channel.getPositionalCenterOfTeam(
						rc, teamId);
				if (myFriendsLoc.equals(myLoc)) {
					away = myLoc.add(oppAt.directionTo(myLoc), 3);
				} else {
					// away = myLoc.add(oppAt.directionTo(myLoc), 3);
					away = myLoc.add(myLoc.directionTo(myFriendsLoc), 3);
				}
				pathFinderGreedy.setTarget(away);
				pathFinderGreedy.move();
			} else {
				// we are dominating!
				int distance = myLoc.distanceSquaredTo(oppAt);
				if (distance <= RobotType.SOLDIER.attackRadiusMaxSquared) {
					rc.attackSquare(oppAt);
				} else {
					pathFinderMLineBug.setTarget(oppAt);
					pathFinderMLineBug.move();
				}
			}
		}
	}

	private void updateTask() {
		Task newTask = Channel.getTask(rc, teamId);
		MapLocation newTarget = Channel.getTarget(rc, teamId);
		if (!newTarget.equals(target) || !newTask.equals(task)) {
			task = newTask;
			target = newTarget;
			// the task has changed
			pathFinderMLineBug.setTarget(target);
		}
		rc.setIndicatorString(1, "DOING TASK " + task + " ON TARGET " + target);
	}

	private void doRandomMove() {
		Direction random = C.DIRECTIONS[randall.nextInt(C.DIRECTIONS.length)];
		if (rc.canMove(random)) {
			try {
				rc.move(random);
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * checks if the given array is null or empty
	 * 
	 * @param array
	 * @return
	 */
	public static final <T> int size(T[] array) {
		if (array == null) {
			return 0;
		}
		return array.length;
	}
}
