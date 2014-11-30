package ext_animorphs;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Scout {
	public SoldierController sc;
	public RobotController rc;
	public Bugger bugger;
	public boolean bugging;
	public SoldierAttacker attacker;

	public Scout(SoldierController sc, Bugger b, SoldierAttacker a)
			throws GameActionException {
		this.sc = sc;
		this.rc = sc.rc;
		bugger = b;
		attacker = a;
		bugging = false;
	}

	public void setRetreatPoint(MapLocation point) {

	}

	public void scout() throws GameActionException {

	}
}
