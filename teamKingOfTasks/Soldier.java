package teamKingOfTasks;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;

public class Soldier extends AbstractRobotType {

	private boolean inactive = false;
	private Soldier task;

	// private PathFinderSnailTrail pathFinderSnailTrail;
	// private PathFinderMLineBug pathFinderMLineBug;
	// MapLocation bestPastrLocation = new MapLocation(0, 0);

	public Soldier(RobotController rc) {
		super(rc);
	}

	@Override
	protected void act() throws GameActionException {
		if (inactive || !rc.isActive()) {
			return;
		}
		if (rc.isConstructing()) {
			inactive = true;
		}
	}

	@Override
	protected void init() throws GameActionException {
	}

	private void actAttacker() throws GameActionException {
		Team we = rc.getTeam();
		Team opponent = we.opponent();

		MapLocation[] nextToAttack = null;
		// opponent's pastr?
		MapLocation[] pastrOpponentAll = rc.sensePastrLocations(opponent);
		if (pastrOpponentAll != null) {
			nextToAttack = pastrOpponentAll.clone();
		} else {
			// communicating opponents?
			MapLocation[] robotsOpponentAll = rc
					.senseBroadcastingRobotLocations(opponent);
			if (robotsOpponentAll != null) {
				nextToAttack = robotsOpponentAll.clone();
			}
		}

		if (nextToAttack.length != 0) {
			boolean shoot = false;

			// attack any pastr in range
			MapLocation target = nextToAttack[0];
			for (int i = 0; i < nextToAttack.length; i++) {
				target = nextToAttack[i];
				if (rc.canAttackSquare(target)) {
					rc.attackSquare(target);
					shoot = true;
					break;
				}
			}

			if (!shoot) {
			}

		} else {
			actProtector();
		}
	}

	private void actPastrBuilder() throws GameActionException {
	}

	private void actProtector() throws GameActionException {
		MapLocation currentLoc = rc.getLocation();

	}
}
