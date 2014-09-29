package simplePastr;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public abstract class AbstractRobotType {
	protected RobotController rc;
	protected Random randall;

	static Direction[] directions = { Direction.NORTH, Direction.NORTH_EAST,
			Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH,
			Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };

	protected AbstractRobotType(RobotController rc) {
		this.rc = rc;
		this.randall = new Random(rc.getRobot().getID());
	}

	public void run() throws GameActionException {
		init();
		while (true) {
			act();
			rc.yield();
		}
	}

	protected abstract void act() throws GameActionException;

	protected abstract void init() throws GameActionException;

}
