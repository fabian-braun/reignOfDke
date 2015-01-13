package reignierOfDKEWithoutLeader;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

/**
 * Class representing the behaviour of a noise tower in the
 * <code>teamreignofdke</code> Battlecode player.
 * 
 * @author Antonie
 */
public class NoiseTower extends AbstractRobotType {

	/**
	 * This field represents the location of the <code>NoiseTower</code>.
	 */
	private MapLocation myLocation;

	/**
	 * This field represents the current map's height.
	 */
	private int ySize;

	/**
	 * This field represents the current map's width.
	 */
	private int xSize;

	/**
	 * This field represents the location we attacked last.
	 */
	private MapLocation attackingLocation;
	private int directionIndex;

	/**
	 * Constructs an instance of the <code>NoiseTower</code> class.
	 * 
	 * @param rc
	 *            The <code>RobotController</code> instance supplied by the
	 *            Battlecode server.
	 */
	public NoiseTower(RobotController rc) {
		super(rc);
	}

	/**
	 * Defines the behaviour of the noise tower.
	 * 
	 * @throws GameActionException
	 */
	@Override
	protected void act() throws GameActionException {
		if (!rc.isActive()) {
			return;
		}
		if (attackingLocation.distanceSquaredTo(myLocation) < GameConstants.PASTR_RANGE) {
			directionIndex = (directionIndex + 1) % C.DIRECTIONS.length;
			attackingLocation = getMaxInDirection(myLocation,
					C.DIRECTIONS[directionIndex]);
		} else {
			attackingLocation = attackingLocation.add(
					attackingLocation.directionTo(myLocation), 1);
		}

		// Check if we can attack this location
		if (rc.canAttackSquare(attackingLocation)) {
			// Attack that location
			rc.attackSquareLight(attackingLocation);
		}

	}

	/**
	 * Initialises a noise tower.
	 * 
	 * @throws GameActionException
	 */
	@Override
	protected void init() throws GameActionException {
		// Get the width and height of the current map
		ySize = rc.getMapHeight();
		xSize = rc.getMapWidth();
		// Get our own location
		myLocation = rc.getLocation();

		directionIndex = rc.getRobot().getID() % C.DIRECTIONS.length;
		attackingLocation = myLocation;
	}

	private MapLocation getMaxInDirection(MapLocation from, Direction direction) {
		MapLocation distant = from.add(direction);
		while (from.distanceSquaredTo(distant) < 65
				&& isOnMap(distant.add(direction))) {
			distant = distant.add(direction);
		}
		return distant;
	}

	private boolean isOnMap(MapLocation loc) {
		return loc.x < xSize && loc.x >= 0 && loc.y < ySize && loc.y >= 0;
	}
}
