package ext_animorphs;

import battlecode.common.GameActionException;
import battlecode.common.Robot;
import battlecode.common.RobotController;

/**
 * General class for broadcasting, receiving and storing messages about
 * locations of stuff
 */
public class IDMessager {
	public int channel;
	private RobotController rc;
	private IntSet ints;

	/**
	 * Initiliazes Messager
	 * 
	 * @param rc
	 *            Robot Controller
	 * @param empty
	 *            should be an empty intset
	 * @param channel
	 *            Starting channel given by communication protocol for object
	 *            using this for. e.g. CommsProtocol.ENEMY_ID
	 */
	public IDMessager(RobotController rc, IntSet empty, int channel) {
		this.channel = channel; // should be one of starting channel from
								// communication protocol, e.g.
								// CommsProtocol.TOWER_CHANNEL_START
		this.rc = rc;
		this.ints = empty; // should be empty
		ints.add(22); // HQ IDS
		ints.add(82); // HQ IDS
	}

	/**
	 * Always make sure update is done. Or else broadcasts may override. Checks
	 * channels in a rolling fashion. No check against channel number going too
	 * high, but it's limited in theory.
	 * 
	 * @throws GameActionException
	 */
	public void update() throws GameActionException {
		int data = rc.readBroadcast(channel);
		while (data != 0) {
			if (data > 0) {
				ints.add(data);
				;
			} else {
				ints.remove(-data);
			}
			channel++;
			data = rc.readBroadcast(channel);
		}
	}

	/**
	 * Takes in some ids. Checks if it is marked already and broadcasts if new.
	 * DoesNot return whether broadcast made. Should be called after updates to
	 * make sure an existing broadcast isn't wiped
	 * 
	 * @throws GameActionException
	 */
	public void broadcast(int[] ids) throws GameActionException {
		for (int i = ids.length; --i >= 0;) {
			if (!ints.contains(ids[i])) {
				rc.broadcast(channel, ids[i]);
				// manually update receiver for this
				channel++;
				ints.add(ids[i]);
			}
		}
	}

	/**
	 * Takes in some robots. Checks if it is marked already and broadcasts if
	 * new. DoesNot return whether broadcast made. Should be called after
	 * updates to make sure an existing broadcast isn't wiped
	 * 
	 * @throws GameActionException
	 */
	public void broadcast(Robot[] bots) throws GameActionException {
		int id;
		for (int i = bots.length; --i >= 0;) {
			id = bots[i].getID();
			if (!ints.contains(bots[i].getID())) {
				rc.broadcast(channel, id);
				// manually update receiver for this
				channel++;
				ints.add(id);
			}
		}
	}

	/**
	 * Checks for whether its marked. Broadcast if not. Returns whether a
	 * broadcast is made.
	 * 
	 * @param id
	 * @return
	 * @throws GameActionException
	 */
	public boolean broadcast(int id) throws GameActionException {
		if (!ints.contains(id)) {
			rc.broadcast(channel, id);
			channel++;
			ints.add(id);
			return true;
		}
		return false;
	}

	/**
	 * Broadcast a kill
	 * 
	 * @throws GameActionException
	 */
	public void broadcastKill(int id) throws GameActionException {
		if (ints.contains(id)) {
			rc.broadcast(channel, id);
			// manually update receiver
			channel++;
			ints.remove(id);
		}
	}

	/**
	 * Broadcast a kill
	 * 
	 * @throws GameActionException
	 */
	public void broadcastKill(Robot bot) throws GameActionException {
		int id = bot.getID();
		if (ints.contains(id)) {
			rc.broadcast(channel, id);
			// manually update receiver
			channel++;
			ints.remove(id);
		}
	}
}
