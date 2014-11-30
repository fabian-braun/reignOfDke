package ext_animorphs;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

/**
 * Class for suicide behavior of soldier units. Contains 1 method that checks
 * what next move of suicide soldier should be. Updates variables
 * "shouldExplode" and "chaseDir" with instructions
 * 
 * @author Nirvan
 *
 */
public class Suicider {

	public Controller c;
	public RobotController rc;

	public Chaser chaser;
	public SuicideAttacker attacker;
	public double threshold = 0;

	public boolean shouldExplode;
	public Direction chaseDir;

	public Suicider(Controller c, Chaser chaser, SuicideAttacker attacker)
			throws GameActionException {
		this.c = c;
		this.rc = c.rc;
		this.chaser = chaser;
		this.attacker = attacker;
	}

	// Can change suicide threshold
	public void setThreshold(double t) {
		threshold = t;
	}

	public boolean next(boolean isActive) throws GameActionException {
		shouldExplode = attacker.explode(threshold);
		if (shouldExplode) {
			chaseDir = null;
			return true;
		}
		if (!isActive) { // if not active, do not set chaseDir : return false
			return false;
		}
		chaseDir = chaser.chase();
		return !(chaseDir == Direction.NONE || chaseDir == Direction.OMNI);
	}

}
