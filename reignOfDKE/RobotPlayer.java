package reignOfDKE;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class RobotPlayer {

	public static void run(RobotController rc) throws GameActionException {
		AbstractRobotType robot;
		switch (rc.getType()) {
		case NOISETOWER:
			robot = new NoiseTower(rc);
			break;
		case PASTR:
			robot = new Pastr(rc);
			break;
		case SOLDIER:
			if (!Channel.isAlive(rc, Core.id)) {
				robot = new Core(rc);
			} else {
				robot = new Soldier(rc);
			}
			break;
		// case HQ:
		default:
			robot = new HQ(rc);
		}
		robot.run();
	}
}
