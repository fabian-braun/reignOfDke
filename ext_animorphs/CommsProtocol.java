package ext_animorphs;

import battlecode.common.GameConstants;
import battlecode.common.MapLocation;

public class CommsProtocol {
	// Max channel is 65536

	// Set channel start locations
	public static final int GLOBAL_CHANNEL = 30000;
	public static final int ACTION_QUEUE_CHANNEL = GLOBAL_CHANNEL;
	public static final int DIRECTION_CHANNEL_START = 50000; // reserve 10001
																// channels, for
																// terrain
																// mapping and
																// also a done
																// channel
	public static final int ENEMY_ID_CHANNEL_START = 61000;
	public static final int TOWER_CHANNEL_START = 62000;
	public static final int PASTR_CHANNEL_START = 63000;
	public static final int LAST_HQ_SPAWN_CHANNEL = 40000;

	// Stuf for ID centered communication

	// Apply mod to id to get channel. Primary hash
	public static final int ID_CHAN_MOD = 2000;

	// Start of channels dedicated to i/o with specific id. "HARD-CODED" right
	// now.
	public static final int ID_OFFSET_FROM_HQ = 0;

	// starts of additional sets of channels dedicated to input/output with
	// specific robot id
	public static final int ID_OFFSET_TO_HQ = ID_CHAN_MOD;
	public static final int ID_OFFSET_FROM_LOCAL = 2 * ID_CHAN_MOD;
	public static final int ID_OFFSET_TO_LOCAL = 3 * ID_CHAN_MOD;

	// secondary head. apply mod to id to get identifier passed with a broadcast
	// message
	// less than ID_CHAN_MOD and relative prime to it
	public static final int ID_MSG_MOD = 1999;

	public static final int LOC_MASK = GameConstants.MAP_MAX_WIDTH
			* GameConstants.MAP_MAX_HEIGHT;

	public static final int DESTROYED_OFFSET = GameConstants.MAP_MAX_WIDTH
			* (GameConstants.MAP_MAX_HEIGHT + 1);

	public static int convertLoc(MapLocation loc) {
		return loc.x + loc.y * GameConstants.MAP_MAX_WIDTH;
	}
}
