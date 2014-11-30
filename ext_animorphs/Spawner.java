package ext_animorphs;

import battlecode.common.Direction;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;

public class Spawner {
	public Controller c;
	public RobotController rc;
	public Direction spawnDir;

	public Spawner(Controller c) {
		this.c = c;
		this.rc = c.rc;
		this.spawnDir = Direction.NORTH;
	}

	public boolean spawn() {
		RobotController rc = this.rc;
		if (rc.senseRobotCount() >= GameConstants.MAX_ROBOTS) {
			return false;
		}
		Direction dir = spawnDir.rotateLeft();
		for (int i = 8; i >= 0; i--) {
			if (rc.canMove(dir)) {
				spawnDir = dir;
				return true;
			}
			dir = dir.rotateLeft();
		}
		return false;
	}

}
