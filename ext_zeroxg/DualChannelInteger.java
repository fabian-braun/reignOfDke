package ext_zeroxg;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class DualChannelInteger {
	private int[] channels;

	public DualChannelInteger(int channel_1, int channel_2) {
		channels = new int[] { channel_1, channel_2 };
	}

	public int read(RobotController rc) throws GameActionException {
		return rc.readBroadcast(channels[Clock.getRoundNum() % 2]);
	}

	public int readCurrent(RobotController rc) throws GameActionException {
		return rc.readBroadcast(channels[1 - Clock.getRoundNum() % 2]);
	}

	public void add(RobotController rc, int x) throws GameActionException {
		int currentChannel = channels[1 - Clock.getRoundNum() % 2];
		rc.broadcast(currentChannel, rc.readBroadcast(currentChannel) + x);
	}

	public void reset(RobotController rc) throws GameActionException {
		rc.broadcast(channels[1 - Clock.getRoundNum() % 2], 0);
	}

}
