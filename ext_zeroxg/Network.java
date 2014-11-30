package ext_zeroxg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import battlecode.common.MapLocation;

public class Network {
	public static boolean debug;
	private List<NetworkNode> nodes;
	private NetworkNode[][] nodeMap;
	private int mapWidth;
	private int mapHeight;

	public Network(int w, int h) {
		nodes = new ArrayList<NetworkNode>();
		nodeMap = new NetworkNode[w][h];
		mapWidth = w;
		mapHeight = h;
	}

	public void add(Path p) {
		NetworkNode previousNode = null;
		boolean previousIsNew = false;
		for (int i = 0; i < p.size(); i++) {
			MapLocation loc = p.get(i);
			NetworkNode newNode;
			boolean isnew;
			if (nodeMap[loc.x][loc.y] != null) {
				isnew = false;
				newNode = nodeMap[loc.x][loc.y];
			} else {
				isnew = true;
				newNode = new NetworkNode(p.get(i));
				nodes.add(newNode);
				nodeMap[loc.x][loc.y] = newNode;
			}
			if (i > 0 && (previousIsNew || isnew))
				newNode.link(previousNode);
			previousNode = newNode;
			previousIsNew = isnew;
		}
	}

	public boolean isNode(MapLocation loc) {
		return nodeMap[loc.x][loc.y] != null;
	}

	private NetworkNode getNode(MapLocation loc) {
		return nodeMap[loc.x][loc.y];
	}

	private NetworkNode getAdjacentNode(MapLocation loc) {
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				if ((i != 0 || j != 0) && i + loc.x >= 0
						&& i + loc.x < mapWidth && j + loc.y >= 0
						&& j + loc.y < mapHeight
						&& nodeMap[i + loc.x][j + loc.y] != null)
					return nodeMap[i + loc.x][j + loc.y];
			}
		}
		return null;
	}

	public Path path(MapLocation lastVisited, MapLocation goal) {
		NetworkNode start = getNode(lastVisited);
		NetworkNode end = getNode(goal);
		MapLocation adjStart = null;
		MapLocation adjEnd = null;

		if (start == null) {
			start = getAdjacentNode(lastVisited);
			adjStart = lastVisited;
		}
		if (end == null) {
			end = getAdjacentNode(goal);
			adjEnd = goal;
		}

		if (start == null || end == null)
			return null;

		for (NetworkNode node : nodes)
			node.clean();

		NetworkNode[] open = new NetworkNode[nodes.size()];
		open[0] = start;
		int count = 1;
		start.open = true;
		NetworkNode current = start;
		while (current != null) {
			current.closed = true;
			for (NetworkLink link : current.links) {
				NetworkNode next = link.opposite(current);
				if (next == end) {
					next.from = current;
					return buildPath(start, end, adjStart, adjEnd);
				}
				if (!next.open) {
					open[count] = next;
					count++;
					next.open = true;
					next.from = current;
					next.g = current.g + link.cost;
					next.h = heuristic(next.location, end.location);
				} else if (!next.closed && next.g > current.g + link.cost) {
					next.from = current;
					next.g = current.g + link.cost;
				}
			}
			current = bestOpen(open, count);
		}

		return null;
	}

	public static int heuristic(MapLocation a, MapLocation b) {
		int dx = Math.abs(a.x - b.x);
		int dy = Math.abs(a.y - b.y);
		return (dx + dy) * 10 - Math.min(dx, dy) * 6;
	}

	private static NetworkNode bestOpen(NetworkNode[] nodes, int count) {
		NetworkNode best = null;
		int bestScore = 0;
		for (int i = 0; i < count; i++) {
			NetworkNode c = nodes[i];
			if (!c.closed && (c.g + c.h < bestScore || best == null)) {
				best = c;
				bestScore = c.g + c.h;
			}
		}
		return best;
	}

	private static Path buildPath(NetworkNode start, NetworkNode end,
			MapLocation adjStart, MapLocation adjEnd) {
		ArrayList<MapLocation> path = new ArrayList<MapLocation>();
		if (adjEnd != null)
			path.add(adjEnd);
		NetworkNode current = end;
		path.add(current.location);
		while (current != start) {
			current = current.from;
			path.add(current.location);
		}
		if (adjStart != null)
			path.add(adjStart);

		Collections.reverse(path);

		return new Path(path);
	}

	public void print() {
		System.out.println("Network of " + nodes.size() + " nodes");
		for (NetworkNode node : nodes)
			node.print();
	}

	public void printMap() {
		for (int i = 0; i < nodeMap[0].length; i++) {
			for (int j = 0; j < nodeMap.length; j++) {
				System.out.print(nodeMap[j][i] == null ? "."
						: nodeMap[j][i].links.size());
			}
			System.out.println();
		}
		System.out.println();
	}
}
