package dualcore;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class Soldier extends AbstractRobotType {

	private boolean inactive = false;
	private SoldierRole role;
	private PathFinderSnailTrail pathFinderSnailTrail;
	private PathFinderMLineBug pathFinderMLineBug;
	private PathFinderGreedy pathFinderGreedy;
	MapLocation bestPastrLocation = new MapLocation(0, 0);
	private Team us;
	private Team opponent;
	private boolean fleeMode = false;
	private static final double HEALTH_ABOUT_TO_DIE = 40;
	private static final double HEALTH_REGENERATED = 50;
	private int ySize = 0;
	private int xSize = 0;

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
		if (inactive || !rc.isActive()) {
			return;
		}
		if (rc.isConstructing()) {
			inactive = true;
		}
		switch (role) {
		case ATTACKER:
			actMicro();
			break;
		case NOISE_TOWER_BUILDER:
			actMicro();
			break;
		case PASTR_BUILDER:
			actMicro();
			break;
		case PROTECTOR:
			actMicro();
			break;
		default:
			break;
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
		role = Channel.requestSoldierRole(rc);
		rc.setIndicatorString(0, role.toString());
		Channel.announceSoldierRole(rc, role);
		us = rc.getTeam();
		opponent = us.opponent();
		ySize = rc.getMapHeight();
		xSize = rc.getMapWidth();
		bestPastrLocation = Channel.getBestPastrLocation(rc);
		pathFinderSnailTrail = new PathFinderSnailTrail(rc);
		pathFinderMLineBug = new PathFinderMLineBug(rc);
		pathFinderSnailTrail.setTarget(bestPastrLocation);
		pathFinderMLineBug.setTarget(bestPastrLocation);
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
				if (!pathFinderMLineBug.getTarget().equals(target)) {
					pathFinderMLineBug.setTarget(target);
				}
				if (rc.getLocation().isAdjacentTo(target)) {
					rc.construct(RobotType.NOISETOWER);
				} else {
					pathFinderMLineBug.move();
				}
				break;
			case BUILD_PASTR:
				if (!pathFinderMLineBug.getTarget().equals(target)) {
					pathFinderMLineBug.setTarget(target);
				}
				if (rc.getLocation().equals(target)) {
					rc.construct(RobotType.PASTR);
				} else {
					pathFinderMLineBug.move();
				}
				break;
			case GOTO:
				if (!pathFinderMLineBug.getTarget().equals(target)) {
					pathFinderMLineBug.setTarget(target);
				}
				pathFinderMLineBug.move();
				break;
			}
		} else if (size(closeOpponents) > size(closeFriends)) {
			// there are more opponents. Better get away.
			MapLocation away = getFleeTarget(closeOpponents);
			pathFinderGreedy.setTarget(away);
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
				pathFinderGreedy.setTarget(toAttack);
				pathFinderGreedy.move();
			}
		}
	}

	public MapLocation getFleeTarget(Robot[] fleeFrom) {
		int totalX = 0;
		int totalY = 0;
		int count = 0;
		for (Robot robot : fleeFrom) {
			MapLocation loc;
			try {
				loc = rc.senseLocationOf(robot);
				totalX += loc.x;
				totalY += loc.y;
				count++;
			} catch (GameActionException e) {
				// ignore that soldier
				e.printStackTrace();
			}
		}
		totalX = totalX / count;
		totalY = totalY / count;
		return new MapLocation(xSize - totalX, ySize - totalY);
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
