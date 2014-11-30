package ext_animorphs;

import battlecode.common.MapLocation;

public class ObjectiveThread {
	public int id;
	public MapLocation lastLoc;
	public boolean running;
	public boolean constructing;
	public boolean made;
	public boolean done;
	public MapLocation loc;
	public boolean on;

	public ObjectiveThread() {
		running = false;
		done = false;
		constructing = false;
	}
}
