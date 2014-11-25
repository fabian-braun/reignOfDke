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

	public HQ(RobotController rc) {
		super(rc);
	}

	@Override
	protected void act() throws GameActionException {
		// Check if a robot is spawnable and spawn one if it is
		if (rc.senseRobotCount() < 1) {
			// location between our HQ and opponent's HQ:
			MapLocation target = new MapLocation(
					(myHq.x * 3 / 4 + otherHq.x / 4),
					(myHq.y * 3 / 4 + otherHq.y / 4));
			for (Team team : teams) {
				team.setTask(Task.GOTO, target);
			}
		}
		if (rc.isActive() && rc.canMove(spawningDefault)
				&& rc.senseRobotCount() < GameConstants.MAX_ROBOTS) {
			rc.spawn(spawningDefault);
		}
	}

	@Override
	protected void init() throws GameActionException {
		ySize = rc.getMapHeight();
		xSize = rc.getMapWidth();
		myHq = rc.senseHQLocation();
		otherHq = rc.senseEnemyHQLocation();
		teams = new Team[3];
		teams[0] = new Team(0, rc);
		teams[1] = new Team(1, rc);
		teams[2] = new Team(2, rc);

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
