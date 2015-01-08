package reignierOfDKE;

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
	private PathFinderMLineBug pathFinderMLineBug;
	private PathFinderAStar pathFinderAStar;
	protected PathFinderGreedy pathFinderGreedy;
	private Team us;
	private Team opponent;
	private MapLocation enemyHq;
	private int fleeCounter = 0;
	private final int MAX_CIRCULATE = 30;
	private final int MIN_CIRCULATE = 10;

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
		if (rc.isActive()) {
			actMicro(target, task);
		} else {
			return;
		}
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
		pathFinderAStar = new PathFinderAStar(rc);
		pathFinderMLineBug = new PathFinderMLineBug(rc, pathFinderAStar.map,
				pathFinderAStar.hqSelfLoc, pathFinderAStar.hqEnemLoc,
				pathFinderAStar.ySize, pathFinderAStar.xSize);
		pathFinderMLineBug.setTarget(target);
		pathFinderGreedy = new PathFinderGreedy(rc, randall);
		enemyHq = pathFinderAStar.hqEnemLoc;
	}

	private void actMicro(MapLocation target, Task task)
			throws GameActionException {
		Robot[] closeOpponents = rc.senseNearbyGameObjects(Robot.class,
				RobotType.SOLDIER.sensorRadiusSquared, opponent);
		boolean oppHqInRange = myLoc.distanceSquaredTo(enemyHq) <= RobotType.SOLDIER.sensorRadiusSquared;
		if (fleeCounter > 0) {
			if (fleeCounter == 1) {
				// this adds a "random" factor, such that the robots not always
				// go backwards and forward on the same line
				pathFinderGreedy.setTarget(target);
			}
			pathFinderGreedy.move();
			fleeCounter--;
			return;
		}
		if (size(closeOpponents) < 1
				|| (size(closeOpponents) == 1 && oppHqInRange)) {
			// no opponents in sight - go for your task
			switch (task) {
			case BUILD_NOISETOWER:
				if (myLoc.isAdjacentTo(target)
						&& rc.senseObjectAtLocation(target) != null) {
					Channel.broadcastTask(rc, Task.CIRCULATE, target, teamId);
					rc.construct(RobotType.NOISETOWER);
					break;
				}
			case BUILD_PASTR:
				if (myLoc.equals(target)) {
					Channel.broadcastTask(rc, Task.BUILD_NOISETOWER, target,
							teamId);
					rc.construct(RobotType.PASTR);
					break;
				}
			case GOTO:
				if (!target.equals(pathFinderAStar.getTarget())) {
					pathFinderAStar.setTarget(target);
				}
				if (!pathFinderAStar.move()) {
					doRandomMove();
				}
				break;
			case CIRCULATE:
				circulate(target);
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
			if (closeSoldiers.size() > size(closeFriends)) {
				// there are more opponents. Better get away.
				fleeCounter = 4;
				MapLocation away = myLoc.add(oppAt.directionTo(myLoc), 10);
				pathFinderGreedy.setTarget(away);
			} else {
				// we are dominating!
				int distance = myLoc.distanceSquaredTo(oppAt);
				if (distance <= RobotType.SOLDIER.attackRadiusMaxSquared) {
					rc.attackSquare(oppAt);
				} else {
					pathFinderGreedy.setTarget(oppAt);
					pathFinderGreedy.move();
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

	private void circulate(MapLocation center) {
		int distance = myLoc.distanceSquaredTo(center);

		if (distance >= MIN_CIRCULATE && distance <= MAX_CIRCULATE) {
			doRandomMove();
		} else {
			if (distance < MIN_CIRCULATE) {
				pathFinderGreedy
						.setTarget(myLoc.add(center.directionTo(myLoc)));
			} else {
				pathFinderGreedy.setTarget(center);
			}
			try {
				pathFinderGreedy.move();
			} catch (GameActionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
