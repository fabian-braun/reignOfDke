package teamKingOfTasks;

import java.util.Random;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public abstract class AbstractRobotType {
	protected RobotController rc;
	protected Random randall;

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
