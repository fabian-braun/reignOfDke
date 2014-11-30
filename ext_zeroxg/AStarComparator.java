package ext_zeroxg;

import java.util.Comparator;

import battlecode.common.MapLocation;

public class AStarComparator implements Comparator<MapLocation> {
	@Override
	public int compare(MapLocation a, MapLocation b) {
		return (AStar.g[a.x][a.y] + AStar.h[a.x][a.y])
				- (AStar.g[b.x][b.y] + AStar.h[b.x][b.y]);
	}
}