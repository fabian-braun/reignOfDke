package reignierOfDKE;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class HQ extends AbstractRobotType {

	private MapLocation myHq;
	private MapLocation otherHq;
	private MapAnalyzer mapAnalyzer;
	/**
	 * soldiers are distributed over 3 teams: <br>
	 * team 0: 50% of soldiers -> flexible attacking team <br>
	 * team 1: 25% of soldiers -> build pastr / defend pastr <br>
	 * team 2: 25% of soldiers -> flexible attacking team / build 2nd pastr /
	 * defend 2nd pastr <br>
	 */
	private Team[] teams;
	private Direction spawningDefault;
	private int teamId = teamIdAssignment[0];
	private int pastrThreshold;

	private static final int[] teamIdAssignment = new int[] { 2, 0, 1, 0 };
	private int teamIndex = 0;

	// info about opponent, is updated in updateInfoAboutOpponent()
	private int countBrdCastingOppSoldiers = 0;
	private MapLocation oppSoldiersCenter;
	private int oppSoldiersMeanDistToCenter = 0;
	private int oppMilkQuantity = 0;

	public HQ(RobotController rc) {
		super(rc);
	}

	@Override
	protected void act() throws GameActionException {
		Team.updateSoldierCount(rc, teams);
		// Check if a robot is spawnable and spawn one if it is
		if (rc.isActive() && rc.senseRobotCount() < GameConstants.MAX_ROBOTS) {
			// determine team id of soldier to spawn:
			teamIndex++;
			teamIndex %= teams.length;
			teamId = teamIdAssignment[teamIndex];
			Channel.assignTeamId(rc, teamId);
			Robot[] closeOpponents = rc.senseNearbyGameObjects(Robot.class,
					RobotType.HQ.sensorRadiusSquared, rc.getTeam().opponent());
			if (size(closeOpponents) > 0) {
				Direction away = rc.senseLocationOf(closeOpponents[0])
						.directionTo(myHq);
				spawningDefault = away;
			}
			if (rc.canMove(spawningDefault)) {
				rc.spawn(spawningDefault);
			} else {
				int i = 0;
				Direction dir = spawningDefault;
				while (!rc.canMove(dir) && i < C.DIRECTIONS.length) {
					dir = dir.rotateLeft();
				}
				if (rc.canMove(dir)) {
					rc.spawn(dir);
				}
			}
		}
		updateInfoAboutOpponent();
		if (rc.senseRobotCount() < 1) {
			// location between our HQ and opponent's HQ:
			MapLocation target = new MapLocation(
					(myHq.x * 3 / 4 + otherHq.x / 4),
					(myHq.y * 3 / 4 + otherHq.y / 4));

			teams[0].setTask(Task.CIRCULATE, target);
			teams[1].setTask(Task.CIRCULATE, target);
			teams[2].setTask(Task.CIRCULATE, target);
		} else {
			coordinateTroops();
		}
	}

	private void coordinateTroops() {
		MapLocation[] opponentPastrLocations = rc.sensePastrLocations(rc
				.getTeam().opponent());
		// If the opponent has any PASTRs
		if (size(opponentPastrLocations) > 0) {
			// Send our teams 0 and 2 in for the kill
			teams[0].setTask(Task.GOTO, opponentPastrLocations[0]);
			if (oppMilkQuantity > 4000000) {
				teams[1].setTask(Task.GOTO, opponentPastrLocations[0]);
				teams[2].setTask(Task.GOTO, opponentPastrLocations[0]);
			} else if (Channel.getPastrCount(rc) == 0) {
				assignBuildPastrTask(1);
				assignBuildPastrTask(2);
			} else if (Channel.getPastrCount(rc) == 1) {
				assignBuildPastrTask(1);
				assignDefendIfNotBuilding(2, Channel.getTarget(rc, 1));
			} else { // more than 1 pastr already
				assignDefendIfNotBuilding(1, Channel.getTarget(rc, 1));
				assignDefendIfNotBuilding(2, Channel.getTarget(rc, 1));
			}
		} else {
			if (Channel.getPastrCount(rc) == 0) {
				assignBuildPastrTask(1);
				assignBuildPastrTask(2);
			} else if (Channel.getPastrCount(rc) == 1) {
				assignBuildPastrTask(1);
				assignDefendIfNotBuilding(2, Channel.getTarget(rc, 1));
			} else { // more than 1 pastr already
				assignDefendIfNotBuilding(1, Channel.getTarget(rc, 1));
				assignDefendIfNotBuilding(2, Channel.getTarget(rc, 1));
			}
			teams[0].setTask(Task.CIRCULATE, Channel.getTarget(rc, 1));
		}
	}

	private void assignBuildPastrTask(int teamId) {
		if (Channel.getTask(rc, teamId).equals(Task.BUILD_NOISETOWER)) {
			return;
		}
		if (rc.senseRobotCount() > pastrThreshold
				&& Channel.getPastrLocation(rc, teamId).y > 0) {
			// 2nd check is for being sure that there is a valid location
			teams[teamId].setTask(Task.BUILD_PASTR,
					Channel.getPastrLocation(rc, teamId));
		}
	}

	private void assignDefendIfNotBuilding(int teamId, MapLocation target) {
		if (Channel.getTask(rc, teamId).equals(Task.BUILD_PASTR)
				|| Channel.getTask(rc, teamId).equals(Task.BUILD_NOISETOWER)) {
			return;
		}
		teams[teamId].setTask(Task.CIRCULATE, target);
	}

	@Override
	protected void init() throws GameActionException {
		myHq = rc.senseHQLocation();
		otherHq = rc.senseEnemyHQLocation();
		teams = Team.getTeams(rc);
		int ySize = rc.getMapHeight();
		int xSize = rc.getMapHeight();
		mapAnalyzer = new MapAnalyzer(rc, null, otherHq, myHq, ySize, xSize, 0);

		initPastrTreshold();
		spawningDefault = myHq.directionTo(otherHq);
		int i = 0;
		while (!rc.canMove(spawningDefault) && i < C.DIRECTIONS.length) {
			spawningDefault = C.DIRECTIONS[i];
			i++;
		}
	}

	private void initPastrTreshold() {
		MapSize size = mapAnalyzer.getMapType();
		switch (size) {
		case LARGE:
			pastrThreshold = 2;
			break;
		case MEDIUM:
			pastrThreshold = 2;
			break;
		default: // Small
			pastrThreshold = 2;
			break;
		}
	}

	private void updateInfoAboutOpponent() {
		countBrdCastingOppSoldiers = Channel.getCountOppBrdCastingSoldiers(rc);
		oppSoldiersCenter = Channel.getPositionalCenterOfOpponent(rc);
		oppSoldiersMeanDistToCenter = Channel.getOpponentMeanDistToCenter(rc);
		oppMilkQuantity = Channel.getOpponentMilkQuantity(rc);
	}
}
