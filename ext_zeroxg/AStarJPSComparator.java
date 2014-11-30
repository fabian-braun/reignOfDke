package ext_zeroxg;

import java.util.Comparator;

import battlecode.common.MapLocation;

public class AStarJPSComparator implements Comparator<MapLocation> {
	@Override
	public int compare(MapLocation a, MapLocation b) {
		return (AStarJPS.g[a.x][a.y] + AStarJPS.h[a.x][a.y])
				- (AStarJPS.g[b.x][b.y] + AStarJPS.h[b.x][b.y]);
	}
}