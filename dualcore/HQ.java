package dualcore;

import java.util.HashSet;
import java.util.Set;

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
	private Set<MapLocation> pastPastrLocations = new HashSet<MapLocation>();

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
			if (rc.canMove(spawningDefault))
				rc.spawn(spawningDefault);
			else {
				// TODO: this is a dirty workaround! randall should not be
				// misused here
				int r = randall.nextInt(C.DIRECTIONS.length);
				if (rc.canMove(C.DIRECTIONS[r])) {
					rc.spawn(C.DIRECTIONS[r]);
				}
			}
		}
		if (rc.senseRobotCount() < 1) {
			// location between our HQ and opponent's HQ:
			MapLocation target = new MapLocation(
					(myHq.x * 3 / 4 + otherHq.x / 4),
					(myHq.y * 3 / 4 + otherHq.y / 4));

			teams[0].setTask(Task.ACCUMULATE, target);
			teams[1].setTask(Task.ACCUMULATE, target);
			teams[2].setTask(Task.ACCUMULATE, target);
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
					if (!pastPastrLocations.contains(mapAnalyzer
							.evaluateBestPastrLoc())) {
						teams[2].setTask(Task.BUILD_PASTR,
								mapAnalyzer.evaluateBestPastrLoc());
						teams[1].setTask(Task.BUILD_NOISETOWER,
								mapAnalyzer.evaluateBestPastrLoc());
						pastPastrLocations.add(mapAnalyzer
								.evaluateBestPastrLoc());
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
		spawningDefault = myHq.directionTo(otherHq);
		int i = 0;
		while (!rc.canMove(spawningDefault) && i < C.DIRECTIONS.length) {
			spawningDefault = C.DIRECTIONS[i];
			i++;
		}
	}
}
