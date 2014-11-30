package ext_animorphs;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Rallyer {
	public Controller c;
	public RobotController rc;
	public Mover m;

	public Sensor sensor;
	public SoldierAttacker attacker;
	public SuicideAttacker suicide;
	public Bugger bugger;
	public MapLocation point;
	public RobotMessager localCmd;

	public Rallyer(Controller c, Sensor sensor, SoldierAttacker attacker,
			SuicideAttacker suicide, Bugger bugger, RobotMessager localCmd) {
		this.c = c;
		this.rc = c.rc;
		this.sensor = sensor;
		this.attacker = attacker;
		this.bugger = bugger;
		this.suicide = suicide;
		this.point = null;
		this.m = new Mover(c);
		this.localCmd = localCmd;
	}

	public void setPoint(MapLocation point) {
		if (this.point == null || !this.point.equals(point)) {
			this.point = point;
			bugger.endBug();
		}
	}

	// RALLLLLLLLYYYYYYY
	public boolean rally() throws GameActionException {

		return true;
	}

}
