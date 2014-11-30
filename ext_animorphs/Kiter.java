package ext_animorphs;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Kiter {
	public RobotController rc;
	public Controller c;
	public MapLocation cur;
	public Mover m;
	public Sensor sensor;
	// will never move out of attack range.
	// ..***...
	// .**+**..
	// **+++**.
	// *++x++*.
	// **+++**.
	// .**+**..
	// ..***...
	public static final int CLOSE_RADIUS_SQ = 4;

	public Kiter(Controller c, Sensor sensor) {
		this.rc = c.rc;
		this.c = c;
		this.sensor = sensor;
		m = new Mover(c);
	}

	public Direction kite() throws GameActionException {
		cur = rc.getLocation();
		// TODO ADD TO SENSE
		Robot[] close = rc.senseNearbyGameObjects(Robot.class, CLOSE_RADIUS_SQ,
				c.enemy);
		if (m.inDangerRadius(cur)) {
			if (close.length == 0) {
				return Direction.OMNI;
			}
			// assume that the close robots are not flanking you
			return m.moveInDir(c.enemyhq.directionTo(cur));
		}
		if (close.length == 0) {
			Robot[] en = sensor.getBots(Sensor.ATTACK_RANGE_ENEMIES);
			if (en.length > 0) {
				return Direction.OMNI;
			} else {
				Robot[] far = sensor.getBots(Sensor.SENSE_RANGE_ENEMIES);
				if (far.length > 0) {
					RobotInfo info = rc.senseRobotInfo(far[0]);
					return m.moveInDir(cur.directionTo(info.location));
				} else {
					return Direction.NONE;
				}
			}
		} else if (close.length == 1) {
			RobotInfo info = rc.senseRobotInfo(close[0]);
			return m.moveInDir(info.location.directionTo(cur));
		} else {
			Robot[] superClose = rc.senseNearbyGameObjects(Robot.class,
					Controller.SPLASH_RADIUS_SQ, c.enemy);
			if (superClose.length > 0) {
				RobotInfo info = rc.senseRobotInfo(superClose[0]);
				return m.moveInDir(info.location.directionTo(cur));
			} else {
				RobotInfo info = rc.senseRobotInfo(close[0]);
				return m.moveInDir(info.location.directionTo(cur));
			}
		}
	}
}
