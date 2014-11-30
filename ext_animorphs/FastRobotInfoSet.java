package ext_animorphs;

import battlecode.common.GameActionException;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class FastRobotInfoSet {
	public static final int HASH = 10000;
	public RobotInfo[] info = new RobotInfo[HASH];
	public RobotController rc;

	public FastRobotInfoSet(RobotController rc) {
		this.rc = rc;
	}

	public void add(Robot r) throws GameActionException {
		int id = r.getID() % HASH;
		info[id] = rc.senseRobotInfo(r);
	}

	public void add(Robot[] bots) throws GameActionException {
		for (int i = bots.length; --i >= 0;) {
			Robot bot = bots[i];
			info[bot.getID() % HASH] = rc.senseRobotInfo(bot);
		}
	}

	public void add(Robot r, RobotInfo rInfo) {
		info[r.getID() % HASH] = rInfo;
	}

	/**
	 * No check for whether you have the id OR INLINE this.info[HASHED ID]
	 * 
	 * @param r
	 * @return null if it wasn't added
	 */
	public RobotInfo get(Robot r) {
		return info[r.getID() % HASH];
	}

	/**
	 * returns non-null stored values of RobotInfo for passed bots
	 * 
	 * @param bots
	 * @return
	 */
	public RobotInfo[] get(Robot[] bots) {
		int numBots = bots.length;
		RobotInfo[] ret = new RobotInfo[numBots];
		while (numBots > 0) {
			numBots--;
			ret[numBots] = info[bots[numBots].getID() % HASH];
		}
		return ret;
	}

	public void clear() {
		info = new RobotInfo[HASH];
	}

}
