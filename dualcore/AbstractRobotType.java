package dualcore;

import java.util.Random;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public abstract class AbstractRobotType {
	protected RobotController rc;
	protected Random randall;

	/**
	 * this constructor must be called by any extending class
	 * 
	 * @param rc
	 */
	protected AbstractRobotType(RobotController rc) {
		this.rc = rc;
		this.randall = new Random(rc.getRobot().getID());
	}

	/**
	 * this method is executed in each iteration of the game execution.
	 * 
	 * @throws GameActionException
	 */
	public void run() throws GameActionException {
		init();
		while (true) {
			act();
			rc.yield();
		}
	}

	protected abstract void act() throws GameActionException;

	/**
	 * this method is executed once after the construction of this
	 * {@link AbstractRobotType}
	 * 
	 * @throws GameActionException
	 */
	protected abstract void init() throws GameActionException;

}
