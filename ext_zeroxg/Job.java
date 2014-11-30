package ext_zeroxg;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Job {
	public JobType type;
	public MapLocation location;
	public int owner;
	public int band;

	public Job(JobType type, MapLocation location) {
		this(type, location, 0, 0);
	}

	public Job(JobType type, MapLocation location, int owner, int band) {
		this.location = location;
		this.type = type;
		this.owner = owner;
		this.band = band;
	}

	public Job update(RobotController rc) throws GameActionException {
		int newOwner = rc.readBroadcast(this.band + 2);
		JobType jobType = JobType.values()[rc.readBroadcast(this.band + 0)];
		if (jobType == JobType.OUTDATED
				|| (newOwner != 0 && newOwner != rc.getRobot().getID()))
			return null;
		return this;
	}

	public void claim(RobotController rc) throws GameActionException {
		rc.broadcast(this.band + 2, rc.getRobot().getID());
		rc.broadcast(this.band + 3, Clock.getRoundNum());
	}

	public void unclaim(RobotController rc) throws GameActionException {
		rc.broadcast(band + 2, -1);
	}

	public void outdate(RobotController rc) throws GameActionException {
		rc.broadcast(this.band + 0, JobType.OUTDATED.ordinal());
	}
}
