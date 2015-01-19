package reignierOfDKE;

import java.util.ArrayList;
import java.util.List;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class Soldier extends AbstractRobotType {

	private int teamId;
	private int id;
	private PathFinder pathFinderComplex;
	protected PathFinderGreedy pathFinderGreedy;
	private Team us;
	private Team opponent;
	private MapLocation enemyHq;
	private int fleeCounter = 0;
	private final int MAX_CIRCULATE = 30;
	private final int MIN_CIRCULATE = 10;

	Task task = Task.GOTO;
	MapLocation target = new MapLocation(10, 10);
	MapLocation myLoc;

	private static final int CLOSE_TEAM_MEMBER_DISTANCE_THRESHOLD = 5;

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
		MapComplexity complexity = Channel.getMapComplexity(rc);
		if (complexity.equals(MapComplexity.COMPLEX)) {
			pathFinderComplex = new PathFinderAStarFast(rc, id);
		} else {
			pathFinderComplex = new PathFinderMLineBug(rc, id);
		}
		pathFinderGreedy = new PathFinderGreedy(rc, randall, id);
		enemyHq = pathFinderComplex.hqEnemLoc;
	}

	private void actMicro(MapLocation target, Task task)
			throws GameActionException {
		Robot[] closeOpponents = rc.senseNearbyGameObjects(Robot.class,
				RobotType.SOLDIER.sensorRadiusSquared, opponent);
		boolean oppHqInRange = myLoc.distanceSquaredTo(enemyHq) <= RobotType.SOLDIER.sensorRadiusSquared;
		if (Channel.needSelfDestruction(rc)) {
			MapLocation toDestroy = Channel.getSelfDestructionLocation(rc);
			if (rc.canAttackSquare(toDestroy)) {
				rc.attackSquare(toDestroy);
			} else if (rc.getLocation().distanceSquaredTo(toDestroy) < MAX_CIRCULATE) {
				pathFinderComplex.setTarget(toDestroy);
				pathFinderComplex.move();
			}
		} else if (fleeCounter > 0) {
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
					Channel.announceNewPastr(rc);
					rc.construct(RobotType.PASTR);
					break;
				}
			case GOTO:
				doAStarMoveTo(target);
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
	 * convenience method: updates the target if necessary. tries to perform a
	 * move. if not possible performs a random move.
	 * 
	 * @param target
	 */
	private void doAStarMoveTo(MapLocation target) {
		if (!target.equals(pathFinderComplex.getTarget())) {
			pathFinderComplex.setTarget(target);
		}
		// If we fail to move where we want to go
		try {
			if (!pathFinderComplex.move()) {
				// Move random
				doRandomMove();
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	private int getNumberOfCloseTeamMembers() {
		int closeTeamMembers = 0;
		// Loop through all robots
		for (int id = 0; id < GameConstants.MAX_ROBOTS; id++) {
			// Check if the robot is alive
			if (Channel.isAlive(rc, id)) {
				// Check the alive robot is on the same team
				if (teamId == Channel.getTeamIdOfSoldier(rc, id)) {
					// Get the position of this robot
					MapLocation teamMemberLocation = Channel
							.getLocationOfSoldier(rc, id);
					int distance = PathFinder.getRequiredMoves(myLoc,
							teamMemberLocation);
					if (distance <= CLOSE_TEAM_MEMBER_DISTANCE_THRESHOLD) {
						closeTeamMembers++;
					}
				}
			}
		}
		return closeTeamMembers;
	}

	private void circulate(MapLocation center) {
		int distance = myLoc.distanceSquaredTo(center);
		if (distance >= MIN_CIRCULATE && distance <= MAX_CIRCULATE) {
			doRandomMove();
		} else {
			if (distance < MIN_CIRCULATE) {
				pathFinderGreedy
						.setTarget(myLoc.add(center.directionTo(myLoc)));
				try {
					pathFinderGreedy.move();
				} catch (GameActionException e) {
					e.printStackTrace();
				}
			} else {
				doAStarMoveTo(center);
			}
		}
	}
}
