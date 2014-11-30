package teamreignofdke;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class Pastr extends AbstractRobotType {

	public Pastr(RobotController rc) {
		super(rc);
	}

	@Override
	protected void act() throws GameActionException {
		// The direct self-destruction of a pastr gives the opponent 2 million
		// gallons of milk. So this is nothing we should try.
		if (rc.getHealth() < 70) {
			Channel.broadcastSelfDestruction(rc, rc.getLocation());
		}
	}

	@Override
	protected void init() throws GameActionException {
		// TODO Auto-generated method stub

	}

}
