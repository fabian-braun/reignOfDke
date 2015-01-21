package reignOfDKE;

import battlecode.common.RobotType;

public enum Task {

	/**
	 * Move to the target.<br\>
	 * Construct a {@link RobotType}.PASTR, as soon as the target is reached
	 */
	BUILD_PASTR,

	/**
	 * Move to the target using a complete pathfinding algorithm.<br\>
	 * When adjacent to the target build a {@link RobotType}.NOISETOWER
	 */
	BUILD_NOISETOWER,

	/**
	 * Move to the target using a complete pathfinding algorithm.<br\>
	 */
	GOTO,

	/**
	 * Move to the target using a greedy pathfinding algorithm.<br\>
	 * The target must not be reached, moving in the direction of it is enough.
	 */
	ACCUMULATE,

	/**
	 * Move to the target using a complete pathfinding algorithm.<br\>
	 * When in a certain range around the target do random moves. When too close
	 * to target move away from it using a greedy path finding algorithm.
	 */
	CIRCULATE;

}
