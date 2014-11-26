package dualcore;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class HQ extends AbstractRobotType {

	private int ySize;
	private int xSize;
	private MapLocation myHq;
	private MapLocation otherHq;
	private MapAnalyzer mapAnalyzer;
	private Team[] teams;
	private Direction spawningDefault;
	private int teamId = 0;

	public HQ(RobotController rc) {
		super(rc);
	}

	@Override
	protected void act() throws GameActionException {
		Team.updateSoldierCount(rc, teams);
		// Check if a robot is spawnable and spawn one if it is
		if (rc.isActive() && rc.canMove(spawningDefault)
				&& rc.senseRobotCount() < GameConstants.MAX_ROBOTS) {
			Channel.assignTeamId(rc, teamId);
			teamId = (teamId + 1) % teams.length;
			rc.spawn(spawningDefault);
		}
		if (rc.senseRobotCount() < 1) {
			// location between our HQ and opponent's HQ:
			MapLocation target = new MapLocation(
					(myHq.x * 3 / 4 + otherHq.x / 4),
					(myHq.y * 3 / 4 + otherHq.y / 4));

			teams[0].setTask(Task.GOTO, target);
			teams[1].setTask(Task.GOTO, target);
			teams[2].setTask(Task.GOTO, target);

		} else {
			MapLocation[] pastrLocations = rc.sensePastrLocations(rc.getTeam()
					.opponent());
			if (Soldier.size(pastrLocations) > 0) {
				teams[0].setTask(Task.GOTO, pastrLocations[0]);
				teams[1].setTask(Task.GOTO, pastrLocations[0]);
			} else {
				if (rc.senseRobotCount() > 5) {
					mapAnalyzer = new MapAnalyzer(rc, myHq, otherHq, ySize,
							xSize);
					teams[2].setTask(Task.BUILD_PASTR,
							mapAnalyzer.evaluateBestPastrLoc());
					teams[1].setTask(Task.BUILD_NOISETOWER,
							mapAnalyzer.evaluateBestPastrLoc());
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
		spawningDefault = myHq.directionTo(otherHq);
		int i = 0;
		while (!rc.canMove(spawningDefault) && i < C.DIRECTIONS.length) {
			spawningDefault = C.DIRECTIONS[i];
			i++;
		}
	}
}
