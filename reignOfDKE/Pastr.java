package reignOfDKE;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Pastr extends AbstractRobotType {

	private double dHealth; // delta of health
	private double lastHealth;
	private boolean amIDead = false;

	public Pastr(RobotController rc) {
		super(rc);
	}

	@Override
	protected void act() throws GameActionException {
		double health = rc.getHealth();
		dHealth = lastHealth - health;
		lastHealth = health;
		if (dHealth > 0) {
			// health is decreasing
			if (!amIDead) {
				amIDead = true;
				Channel.announcePastrDeath(rc);
				Channel.broadcastSelfDestruction(rc, rc.getLocation());
				// rc.setIndicatorString(0, "I'm dead");
			}
		} else if (dHealth < 0) {
			// health is increasing
			if (amIDead) {
				amIDead = false;
				Channel.announceNewPastr(rc);
				Channel.broadcastSelfDestruction(rc, new MapLocation(-1, -1));
				// rc.setIndicatorString(0, "I'm alive");
			}
		}
	}

	@Override
	protected void init() throws GameActionException {
		lastHealth = rc.getHealth();

	}
}
