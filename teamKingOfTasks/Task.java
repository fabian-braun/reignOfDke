package teamKingOfTasks;

import battlecode.common.MapLocation;

public class Task {
	private Mission todo;
	private int priority;
	// How many times is this task needed? For example: Destroy an enemies pastr
	// with 3 soldiers
	private int requestedAmount;
	private MapLocation target;

	public Task(Mission m, int p, int a) {
		setMission(m);
		setPriority(p);
		setRequestedAmount(a);
	}

	public void setMission(Mission m) {
		todo = m;
	}

	public void setPriority(int p) {
		priority = p;
	}

	public void setRequestedAmount(int a) {
		requestedAmount = a;
	}

	public Mission getMission() {
		return todo;
	}

	public int getPriority() {
		return priority;
	}

	public int getRequestedAmount() {
		return requestedAmount;
	}

	public MapLocation getTarget() {
		return target;
	}

	public void setTarget(MapLocation loc) {
		target = loc;
	}
}