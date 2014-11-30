package ext_animorphs;

public enum MessageType {
	// global
	ASSAULT, DEFEND, RALLY, HELP,

	// to individual bots
	MAKE_PASTR, MAKE_DISRUPTOR, MAKE_TOWER_HERDER, DISRUPT, TOWER_HERD,

	// to hq/pasture
	NEARBY_ENEMY, // when assaulting/defending

	// defending
	ENGAGING_ENEMY, REPOSITION, CANT_FORWARD, RETREAT, DENIED_PASTURE, SNEAK_OUT,

	// pasture building
	INTERCEPTED_BY_ENEMY, LOCATION_HAS_ENEMY, PROBLEM_BUGGING;

}
