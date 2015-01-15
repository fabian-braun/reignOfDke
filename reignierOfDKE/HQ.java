package reignierOfDKE;

import reignierOfDKE.C.MapType;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class HQ extends AbstractRobotType {

	private int ySize;
	private int xSize;
	private MapLocation myHq;
	private MapLocation otherHq;
	private MapAnalyzer mapAnalyzer;
	private Team[] teams;
	private Direction spawningDefault;
	private int teamId = 0;
	private int pastrThreshold;

	public HQ(RobotController rc) {
		super(rc);
	}

	@Override
	protected void act() throws GameActionException {
		Team.updateSoldierCount(rc, teams);
		// Check if a robot is spawnable and spawn one if it is
		if (rc.isActive() && rc.senseRobotCount() < GameConstants.MAX_ROBOTS) {
			Channel.assignTeamId(rc, teamId);
			teamId = (teamId + 1) % teams.length;
			Robot[] closeOpponents = rc.senseNearbyGameObjects(Robot.class,
					RobotType.HQ.sensorRadiusSquared, rc.getTeam().opponent());
			if (Soldier.size(closeOpponents) > 0) {
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
		if (rc.senseRobotCount() < 1) {
			// location between our HQ and opponent's HQ:
			MapLocation target = new MapLocation(
					(myHq.x * 3 / 4 + otherHq.x / 4),
					(myHq.y * 3 / 4 + otherHq.y / 4));

			teams[0].setTask(Task.CIRCULATE, target);
			teams[1].setTask(Task.CIRCULATE, target);
			teams[2].setTask(Task.CIRCULATE, target);
		} else {
			MapLocation[] opponentPastrLocations = rc.sensePastrLocations(rc
					.getTeam().opponent());
			// If the opponent has any PASTRs
			if (Soldier.size(opponentPastrLocations) > 0) {
				// Send our teams 0 and 1 in for the kill
				teams[0].setTask(Task.GOTO, opponentPastrLocations[0]);
				teams[1].setTask(Task.GOTO, opponentPastrLocations[0]);
				teams[2].setTask(Task.GOTO, opponentPastrLocations[0]);
			} else {
				if (rc.senseRobotCount() > pastrThreshold) {

					// Check if we have any active PASTRs
					MapLocation[] ownPastrLocations = rc.sensePastrLocations(rc
							.getTeam());
					if (Soldier.size(ownPastrLocations) == 0) {
						// We need to build a PASTR, determine the best PASTR
						// location
						MapLocation bestPastrLocation = mapAnalyzer
								.evaluateBestPastrLoc();
						// Assign the correct tasks to the teams
						teams[0].setTask(Task.CIRCULATE, bestPastrLocation);
						teams[1].setTask(Task.BUILD_PASTR, bestPastrLocation);
						teams[2].setTask(Task.CIRCULATE, bestPastrLocation);
					}
				}
			}
		}
	}

	@Override
	protected void init() throws GameActionException {
		ySize = rc.getMapHeight();
		xSize = rc.getMapWidth();
		myHq = rc.senseHQLocation();
		otherHq = rc.senseEnemyHQLocation();
		teams = Team.getTeams(rc);

		mapAnalyzer = new MapAnalyzer(rc, myHq, otherHq, ySize, xSize);
		// mapAnalyzer.generateRealDistanceMap(); // TODO: too expensive
		// mapAnalyzer.printMapAnalysisDistance();
		initPastrTreshold();
		spawningDefault = myHq.directionTo(otherHq);
		int i = 0;
		while (!rc.canMove(spawningDefault) && i < C.DIRECTIONS.length) {
			spawningDefault = C.DIRECTIONS[i];
			i++;
		}
	}

	private void initPastrTreshold() {
		MapType size = mapAnalyzer.getMapType();
		switch (size) {
		case Large:
			pastrThreshold = 2;
			break;
		case Medium:
			pastrThreshold = 5;
			break;
		default: // Small
			pastrThreshold = 10;
			break;
		}
	}
}
