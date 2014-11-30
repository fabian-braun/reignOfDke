package ext_zeroxg;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class StateMap {
	private static final int validTurnCount = 2;
	private int band;

	public StateMap(int band) // takes 10000 channels
	{
		this.band = band;
	}

	public void broadcast(RobotController rc, MapLocation loc, State state)
			throws GameActionException {
		rc.broadcast(band + Channels.locToInt(loc), stateToInt(state));
	}

	public State read(RobotController rc, MapLocation loc)
			throws GameActionException {
		int val = rc.readBroadcast(band + Channels.locToInt(loc));
		if (val == 0)
			return null;
		int turn = val / 1000;
		if (Clock.getRoundNum() - turn > validTurnCount)
			return null;

		return intToState(val);
	}

	private static int stateToInt(State state) {
		return 1000 * Clock.getRoundNum() + state.ordinal();
	}

	private State intToState(int val) {
		int s = val % 1000;
		if (s >= State.values().length)
			return null;
		return State.values()[s];
	}
}
