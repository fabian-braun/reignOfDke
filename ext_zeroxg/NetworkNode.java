package ext_zeroxg;

import java.util.ArrayList;

import battlecode.common.MapLocation;

public class NetworkNode {
	public MapLocation location;
	public ArrayList<NetworkLink> links;
	public NetworkNode from;
	public int g;
	public int h;
	public boolean closed;
	public boolean open;

	public NetworkNode(MapLocation loc) {
		location = loc;
		links = new ArrayList<NetworkLink>();
	}

	public void link(NetworkNode n) {
		NetworkLink link = new NetworkLink(this, n);
		this.links.add(link);
		n.links.add(link);
	}

	public void print() {
		System.out.println("Node " + location + " linked with " + links.size()
				+ " nodes");
		for (NetworkLink link : links)
			System.out.println("Linked with : " + link.opposite(this).location);
	}

	public void clean() {
		from = null;
		g = 0;
		h = 0;
		closed = false;
		open = false;
	}
}
