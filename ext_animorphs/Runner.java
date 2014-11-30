package ext_animorphs;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;

public class Runner {
	public RobotController rc;
	public Controller c;
	public MapLocation cur;
	public Mover m;
	public Sensor sensor;

	public Runner(Controller c, Sensor sensor) {
		this.rc = c.rc;
		this.c = c;
		m = new Mover(c);
		this.sensor = sensor;
	}

	public Direction flee() throws GameActionException {
		cur = rc.getLocation();
		// (r+2)^2 ~= r^2 + 20 if r ~ 4
		if (m.inDangerRadius(cur)) {
			// too close to splash range
			return m.moveInDir(c.enemyhq.directionTo(cur));
		}

		Robot[] en = sensor.getBots(Sensor.ATTACK_RANGE_ENEMIES);
		if (en.length > 0) {
			return fleeWeighted(en);
		}
		// en= sensor.getBots(Sensor.SENSE_RANGE_ENEMIES);
		// if (en.length > 0) {
		// return fleeWeighted(en);
		// }

		// no enemies
		return Direction.OMNI;
	}

	public Direction fleeWeighted(Robot[] en) throws GameActionException {
		Robot[] allies = sensor.getBots(Sensor.SENSE_RANGE_ALLIES);
		int yDir = 0;
		int xDir = 0;
		int numEnemies = en.length;
		int numAllies = allies.length;

		for (Robot e : en) {
			MapLocation eLoc = rc.senseRobotInfo(e).location;
			xDir -= eLoc.x;
			yDir -= eLoc.y;
		}
		for (Robot a : allies) {
			MapLocation aLoc = rc.senseRobotInfo(a).location;
			xDir += aLoc.x;
			yDir += aLoc.y;
		}
		xDir += numEnemies * cur.x - numAllies * cur.x;
		yDir += numEnemies * cur.y - numAllies * cur.y;

		Direction desired = cur.directionTo(cur.add(xDir, yDir));

		return m.smartMove(desired);
	}

	public Direction fleeClosest(Robot[] en) throws GameActionException {
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
