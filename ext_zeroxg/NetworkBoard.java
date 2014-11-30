package ext_zeroxg;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class NetworkBoard {
	private int band;
	private int tmpCount;

	public NetworkBoard(int band) // 10,000 channels
	{
		this.band = band;
		this.tmpCount = 0;
	}

	public void addPath(RobotController rc, Path path)
			throws GameActionException {
		int count = pathCount(rc);
		if (count == 10) {
			System.out.println("MAXIMUM PATH COUNT REACHED!");
			return;
		}
		PathBroadcast.write(rc, path, band + 1 + count * 1000);
		tmpCount = count + 1;
		rc.broadcast(band, tmpCount);
	}

	public Path getPath(RobotController rc, int i) throws GameActionException {
		return PathBroadcast.read(rc, band + 1 + i * 1000);
	}

	public int pathCount(RobotController rc) throws GameActionException {
		return Math.max(tmpCount, rc.readBroadcast(band));
	}
}
