package ext_zeroxg;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class NetworkDownloader {
	private Network network;
	private int lastPathCount;

	public NetworkDownloader(Network network) {
		this.network = network;
		this.lastPathCount = 0;
	}

	public void update(RobotController rc) throws GameActionException {
		int newPathCount = Channels.network.pathCount(rc);
		for (int i = lastPathCount; i < newPathCount; i++) {
			Path path = Channels.network.getPath(rc, i);
			network.add(path);
		}
		lastPathCount = newPathCount;
	}
}
