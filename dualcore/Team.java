package dualcore;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Team {

	public final int id;
	private final RobotController rc;
	private int soldierCount = 0;
	private Task task;
	private MapLocation target;

	public Team(int id, RobotController rc) {
		this.id = id;
		this.rc = rc;
	}

	public int getSoldierCount() {
		return soldierCount;
	}

	public void setSoldierCount(int soldierCount) {
		this.soldierCount = soldierCount;
	}

	public Task getTask() {
		return task;
	}

	public MapLocation getTarget() {
		return target;
	}

	public void setTask(Task task, MapLocation target) {
		Channel.broadcastTask(rc, task, target, id);
		this.task = task;
		this.target = target;
	}
}
