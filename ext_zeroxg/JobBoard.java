package ext_zeroxg;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class JobBoard {
	private int band;
	private static final int jobLength = 4;
	private static final int legalDelay = 10;
	private static int tmpCount = 0;

	public JobBoard(int channelBand) {
		band = channelBand;
	}

	public void update(RobotController rc) throws GameActionException {
		int count = jobCount(rc);
		int turn = Clock.getRoundNum();
		for (int i = 0; i < count; i++) {
			int localBand = band + 1 + jobLength * i;
			if (turn - rc.readBroadcast(localBand + 3) > legalDelay) {
				rc.broadcast(localBand + 2, 0);
				rc.broadcast(localBand + 3, 0);
				// System.out.println("Unclaimed job " + i);
			}
		}
	}

	public void add(RobotController rc, Job job) throws GameActionException {
		job.band = band + 1 + jobLength * jobCount(rc);
		add(rc, job.type, job.location);
	}

	public void add(RobotController rc, JobType type, MapLocation loc)
			throws GameActionException {
		int count = jobCount(rc);
		int localBand = band + 1 + jobLength * count;
		rc.broadcast(localBand + 0, type.ordinal());
		rc.broadcast(localBand + 1, Channels.locToInt(loc));
		rc.broadcast(localBand + 2, 0);
		rc.broadcast(localBand + 3, 0);

		tmpCount = count + 1;
		rc.broadcast(band, tmpCount);
	}

	public int jobCount(RobotController rc) throws GameActionException {
		return Math.max(tmpCount, rc.readBroadcast(band));
	}

	public Job getJob(RobotController rc, int i) throws GameActionException {
		int localBand = band + 1 + jobLength * i;
		JobType type = JobType.values()[rc.readBroadcast(localBand)];
		MapLocation location = Channels.readLocation(rc, localBand + 1);
		int owner = rc.readBroadcast(localBand + 2);
		return new Job(type, location, owner, localBand);
	}

	public Job getJobAt(RobotController rc, MapLocation loc)
			throws GameActionException {
		int count = jobCount(rc);
		int locHash = Channels.locToInt(loc);
		for (int i = 0; i < count; i++) {
			if (rc.readBroadcast(band + 1 + jobLength * i + 1) == locHash)
				return getJob(rc, i);
		}
		return null;
	}

	public Job getAvailableJob(RobotController rc) throws GameActionException {
		int count = jobCount(rc);
		for (int i = 0; i < count; i++) {
			int owner = rc.readBroadcast(band + 1 + jobLength * i + 2);
			JobType type = JobType.values()[rc.readBroadcast(band + 1
					+ jobLength * i + 0)];
			if (owner == 0 && type != JobType.OUTDATED)
				return getJob(rc, i);
		}
		return null;
	}

	private void printBand(RobotController rc, int x)
			throws GameActionException {
		System.out.println();
		for (int i = 0; i < x; i++) {
			System.out.print(rc.readBroadcast(band + i) + " ");
		}
		System.out.println();
	}
}
