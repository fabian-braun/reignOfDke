package ext_animorphs;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;

public class Chaser {
	public RobotController rc;
	public Controller c;
	public Mover m;
	public MapLocation cur;
	public Sensor sensor;

	public Chaser(Controller c, Sensor sensor) {
		this.rc = c.rc;
		this.c = c;
		this.sensor = sensor;
		m = new Mover(c);
	}

	public Direction chase() throws GameActionException {
		cur = rc.getLocation();
		// (r+2)^2 ~= r^2 + 20 if r ~ 4
		if (m.inDangerRadius(cur)) {
			// too close to splash range
			return Direction.NONE;
		}

		Robot[] en = sensor.getBots(Sensor.ATTACK_RANGE_ENEMIES);
		if (en.length > 0) {
			return chaseClosest(en);
		}
		en = sensor.getBots(Sensor.SENSE_RANGE_ENEMIES);
		if (en.length > 0) {
			return chaseClosest(en);
		}

		// no enemies
		return Direction.OMNI;
	}

	public Direction chaseClosest(Robot[] en) throws GameActionException {
		MapLocation min = rc.senseLocationOf(en[0]);
		int minDist = min.distanceSquaredTo(cur);
		for (int i = 1; i < en.length; i++) {
			MapLocation next = rc.senseLocationOf(en[i]);
			int dist = next.distanceSquaredTo(cur);
			if (minDist < dist) {
				min = next;
				minDist = dist;
			}
		}
		Direction desired = cur.directionTo(min);
		return m.moveInDir(desired);
	}
}
