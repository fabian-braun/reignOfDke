package ext_zeroxg;

import battlecode.common.MapLocation;

public class NetworkLink {
	public int cost;
	public NetworkNode a;
	public NetworkNode b;

	public NetworkLink(NetworkNode a, NetworkNode b) {
		this.a = a;
		this.b = b;
		this.cost = distance(a.location, b.location);
	}

	public NetworkNode opposite(NetworkNode n) {
		return n == a ? b : a;
	}

	private static int distance(MapLocation a, MapLocation b) {
		int dx = Math.abs(a.x - b.x);
		int dy = Math.abs(a.y - b.y);
		return (dx + dy) * 10 - Math.min(dx, dy) * 6;
	}
}
