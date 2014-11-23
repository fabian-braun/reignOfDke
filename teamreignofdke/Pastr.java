package teamreignofdke;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class Pastr extends AbstractRobotType {

	public Pastr(RobotController rc) {
		super(rc);
	}

	@Override
	protected void act() throws GameActionException {
		if (rc.getHealth() < 5) {
			Channel.selfDestruction(rc, rc.getType());
			rc.selfDestruct();
		}

	}

	@Override
	protected void init() throws GameActionException {
		// TODO Auto-generated method stub

	}

}
