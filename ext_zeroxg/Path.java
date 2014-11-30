package ext_zeroxg;

import java.util.ArrayList;

import battlecode.common.MapLocation;

public class Path {
	private MapLocation[] path;
	public MapLocation start;
	public MapLocation end;

	public Path(MapLocation[] p) {
		path = p;
		start = p[0];
		end = p[p.length - 1];
	}

	public Path(ArrayList<MapLocation> p) {
		path = p.toArray(new MapLocation[p.size()]);
		start = path[0];
		end = path[path.length - 1];
	}

	public int size() {
		return path.length;
	}

	public MapLocation get(int i) {
		return path[i];
	}

	public void print() {
		System.out.println();
		for (MapLocation l : path) {
			System.out.print(l.toString() + " ");
		}
		System.out.println();
	}
}
