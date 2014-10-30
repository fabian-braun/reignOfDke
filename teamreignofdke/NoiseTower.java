package teamreignofdke;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

/**
 * Class representing the behaviour of a noise tower in the
 * <code>teamreignofdke</code> Battlecode player.
 * 
 * @author Antonie
 */
public class NoiseTower extends AbstractRobotType {

	/**
	 * This field indicates whether or not the <code>NoiseTower</code> is
	 * inactive.
	 */
	private boolean inactive = false;

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
		if (inactive || !rc.isActive()) {
			return;
		}
		if (rc.isConstructing()) {
			inactive = true;
			return;
		}

		// TODO implement noise tower behaviour
		System.out.println("I'm a NoiseTower, but don't know what to do yet");
	}

	/**
	 * Initialises a noise tower.
	 * 
	 * @throws GameActionException
	 */
	@Override
	protected void init() throws GameActionException {
		// No initialisation required (yet)
	}

}
