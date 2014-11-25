package dualcore;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class Soldier extends AbstractRobotType {

	private boolean inactive = false;
	private int teamId;
	private int id;
	private PathFinderSnailTrail pathFinderSnailTrail;
	private PathFinderMLineBug pathFinderMLineBug;
	private PathFinderGreedy pathFinderGreedy;
	private Team us;
	private Team opponent;

	Task task = Task.GOTO;
	MapLocation target = new MapLocation(0, 0);

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
		pathFinderMLineBug = new PathFinderMLineBug(rc);
		pathFinderSnailTrail.setTarget(target);
		pathFinderMLineBug.setTarget(target);
		pathFinderGreedy = new PathFinderGreedy(rc, randall);
	}

	private void actMicro(MapLocation target, Task task)
			throws GameActionException {
		Robot[] closeOpponents = rc.senseNearbyGameObjects(Robot.class,
				RobotType.SOLDIER.sensorRadiusSquared, opponent);
		Robot[] closeFriends = rc.senseNearbyGameObjects(Robot.class,
				RobotType.SOLDIER.sensorRadiusSquared, us);
		if (size(closeOpponents) < 1) {
			// no opponents in sight - go for your task
			switch (task) {
			case BUILD_NOISETOWER:
				pathFinderMLineBug.setTarget(target);
				if (rc.getLocation().isAdjacentTo(target)) {
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
				if (rc.getLocation().equals(target)) {
					rc.construct(RobotType.PASTR);
				} else {
					if (!pathFinderMLineBug.move()) {
						doRandomMove();
						pathFinderMLineBug.setTarget(target);
					}
				}
				break;
			case GOTO:
				pathFinderMLineBug.setTarget(target);
				if (!pathFinderMLineBug.move()) {
					doRandomMove();
					pathFinderMLineBug.setTarget(target);
				}
				break;
			}
		} else if (size(closeOpponents) >= size(closeFriends)) {
			// there are more opponents. Better get away.
			MapLocation oppAt = rc.senseLocationOf(closeOpponents[0]);
			MapLocation away = rc.getLocation().add(
					oppAt.directionTo(rc.getLocation()), 3);
			pathFinderGreedy.setTarget(away);
			pathFinderGreedy.move();
		} else if (size(closeOpponents) < size(closeFriends)) {
			// we are dominating!
			Robot[] attackableOpponents = rc.senseNearbyGameObjects(
					Robot.class, RobotType.SOLDIER.attackRadiusMaxSquared,
					opponent);
			if (size(attackableOpponents) > 0) {
				MapLocation toAttack = rc
						.senseLocationOf(attackableOpponents[0]);
				rc.attackSquare(toAttack);
			} else {
				MapLocation toAttack = rc.senseLocationOf(closeOpponents[0]);
				pathFinderMLineBug.setTarget(toAttack);
				pathFinderMLineBug.move();
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
