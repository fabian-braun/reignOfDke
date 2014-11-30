package ext_zeroxg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.PriorityQueue;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.TerrainTile;

public class AStarJPS {
	public static int[][] h;
	public static int[][] g;
	private static PriorityQueue<MapLocation> open;
	private static FastLocSet closed;
	private static MapLocation[][] from;
	private static MapLocation start;
	private static MapLocation end;
	private static int mapWidth;
	private static int mapHeight;
	private static TerrainTile[][] map;
	private static Direction[][][][] neighborMap = generateNeighborMap();
	private static final boolean pathShortener = false;
	private static boolean done;

	private static void init(TerrainTile[][] m, MapLocation a, MapLocation b) {
		// System.out.println("Initializing ASTAR + JPS");

		mapWidth = m.length;
		mapHeight = m[0].length;
		start = a;
		end = b;
		map = m;

		AStarJPSComparator comparator = new AStarJPSComparator();
		open = new PriorityQueue<MapLocation>(10, comparator);
		closed = new FastLocSet();
		from = new MapLocation[mapWidth][mapHeight];
		g = new int[mapWidth][mapHeight]; // distance of the best path to x y
		h = new int[mapWidth][mapHeight]; // estimated distance to the end

		closed.add(start);
		g[start.x][start.y] = 0;
		h[start.x][start.y] = heuristic(start.x, start.y, end.x, end.y);

		for (Direction dir : RobotPlayer.customDirections) {
			MapLocation next = a.add(dir);
			if (isWalkable(next)) {
				from[next.x][next.y] = a;
				h[next.x][next.y] = heuristic(next.x, next.y, b.x, b.y);
				g[next.x][next.y] = 10;
				open.add(next);
			}
		}
	}

	public static Path find(TerrainTile[][] map, MapLocation startLoc,
			MapLocation endLoc, int turns) {
		done = false;
		if (map[startLoc.x][startLoc.y] == TerrainTile.VOID
				|| map[endLoc.x][endLoc.y] == TerrainTile.VOID) {
			done = true;
			System.out.println(startLoc + " " + endLoc);
			return null;
		}
		if ((start == null || !start.equals(startLoc)) || end == null
				|| !end.equals(endLoc))
			init(map, startLoc, endLoc);

		int startTurn = Clock.getRoundNum();
		while (!open.isEmpty() && Clock.getRoundNum() - startTurn < turns - 1) {
			MapLocation current = open.poll();
			if (current.equals(end)) {
				return buildPath();
			}

			Direction fromDir = (from[current.x][current.y])
					.directionTo(current);
			Direction[] neighbors = neighbors(current, fromDir);
			for (Direction d : neighbors) {
				MapLocation next = jump(current, d, end);

				if (next == null)
					continue;

				if (h[next.x][next.y] == 0)
					h[next.x][next.y] = heuristic(next.x, next.y, end.x, end.y);
				int newg = g[current.x][current.y] + (d.isDiagonal() ? 14 : 10);

				if (!closed.contains(next)) {
					boolean better = newg < g[next.x][next.y];
					if (better)
						open.remove(next);
					if (g[next.x][next.y] == 0 || better) {
						g[next.x][next.y] = newg;
						from[next.x][next.y] = current;
						open.add(next);
					}
				}
			}
			closed.add(current);
		}

		return null;
	}

	private static MapLocation jump(MapLocation current, Direction dir,
			MapLocation end) {

		if (!isWalkable(current.x + dir.dx, current.y + dir.dy))
			return null;
		MapLocation next = current.add(dir);
		// System.out.println("Jump: " + next + " dir: " + dir.name() + " map: "
		// + jumpMap[next.x][next.y][dir.ordinal()]);
		// if (jumpMap[next.x][next.y][dir.ordinal()] != null)
		// {
		// System.out.println("Useful! " + next + " " + dir);
		// return jumpMap[next.x][next.y][dir.ordinal()];
		// }

		// double bc = Clock.getBytecodeNum();
		// int t = Clock.getRoundNum();
		if (hasForcedNeighbors(next, dir) || next.equals(end)) {
			// jumpMap[next.x][next.y][dir.ordinal()] = next;
			return next;
		}
		// System.out.println("jump cost: " + ((Clock.getRoundNum() - t) * 10000
		// + Clock.getBytecodeNum() - bc));

		if (dir.isDiagonal()
				&& (jump(next, dir.rotateLeft(), end) != null || jump(next,
						dir.rotateRight(), end) != null)) {
			// jumpMap[next.x][next.y][dir.ordinal()] = next;
			return next;
		}

		// jumpMap[next.x][next.y][dir.ordinal()] =
		return jump(next, dir, end);
	}

	private static boolean isWalkable(MapLocation loc) {
		return isWalkable(loc.x, loc.y);
	}

	private static boolean isWalkable(int x, int y) {
		return x >= 0 && y >= 0 && x < mapWidth && y < mapHeight
				&& map[x][y] != TerrainTile.VOID;
	}

	private static boolean hasForcedNeighbors(MapLocation loc, Direction dir) {
		switch (dir) {
		case EAST:
		case WEST:
			return (!isWalkable(loc.x, loc.y + 1))
					&& isWalkable(loc.x + dir.dx, loc.y + 1)
					|| (!isWalkable(loc.x, loc.y - 1) && isWalkable(loc.x
							+ dir.dx, loc.y - 1));
		case NORTH:
		case SOUTH:
			return (!isWalkable(loc.x + 1, loc.y) && isWalkable(loc.x + 1,
					loc.y + dir.dy))
					|| (!isWalkable(loc.x - 1, loc.y) && isWalkable(loc.x - 1,
							loc.y + dir.dy));
		case NORTH_EAST:
		case NORTH_WEST:
		case SOUTH_EAST:
		case SOUTH_WEST:
			return (!isWalkable(loc.x - dir.dx, loc.y) && isWalkable(loc.x
					- dir.dx, loc.y + dir.dy))
					|| (!isWalkable(loc.x, loc.y - dir.dy) && isWalkable(loc.x
							+ dir.dx, loc.y - dir.dy));
		default:
			break;
		}

		return false;
	}

	private static Direction[] neighbors(MapLocation loc, Direction dir) {
		switch (dir) {
		case EAST:
			return neighborMap[dir.ordinal()][isWalkable(loc.x, loc.y - 1) ? 0
					: 1][isWalkable(loc.x, loc.y + 1) ? 0 : 1];
		case WEST:
			return neighborMap[dir.ordinal()][isWalkable(loc.x, loc.y + 1) ? 0
					: 1][isWalkable(loc.x, loc.y - 1) ? 0 : 1];
		case NORTH:
			return neighborMap[dir.ordinal()][isWalkable(loc.x - 1, loc.y) ? 0
					: 1][isWalkable(loc.x + 1, loc.y) ? 0 : 1];
		case SOUTH:
			return neighborMap[dir.ordinal()][isWalkable(loc.x + 1, loc.y) ? 0
					: 1][isWalkable(loc.x - 1, loc.y) ? 0 : 1];
		case NORTH_EAST:
			return neighborMap[dir.ordinal()][isWalkable(loc.x - 1, loc.y) ? 0
					: 1][isWalkable(loc.x, loc.y + 1) ? 0 : 1];
		case NORTH_WEST:
			return neighborMap[dir.ordinal()][isWalkable(loc.x, loc.y + 1) ? 0
					: 1][isWalkable(loc.x + 1, loc.y) ? 0 : 1];
		case SOUTH_EAST:
			return neighborMap[dir.ordinal()][isWalkable(loc.x, loc.y - 1) ? 0
					: 1][isWalkable(loc.x - 1, loc.y) ? 0 : 1];
		case SOUTH_WEST:
			return neighborMap[dir.ordinal()][isWalkable(loc.x + 1, loc.y) ? 0
					: 1][isWalkable(loc.x, loc.y - 1) ? 0 : 1];
		default:
			return null;
		}
	}

	private static Direction[][][][] generateNeighborMap() {
		Direction[][][][] neighborMap = new Direction[8][][][];
		for (int i = 0; i < 8; i++) {
			Direction dir = Direction.values()[i];
			neighborMap[i] = new Direction[2][][];
			for (int j = 0; j < 2; j++) {
				neighborMap[i][j] = new Direction[2][];
				for (int k = 0; k < 2; k++) {
					if (dir.isDiagonal()) {
						if (j == 0 && k == 0)
							neighborMap[i][j][k] = new Direction[] { dir,
									dir.rotateLeft(), dir.rotateRight() };
						else if (j != 0 && j != 0)
							neighborMap[i][j][k] = new Direction[] { dir,
									dir.rotateLeft(), dir.rotateRight(),
									dir.rotateLeft().rotateLeft(),
									dir.rotateRight().rotateRight() };
						else if (j != 0)
							neighborMap[i][j][k] = new Direction[] { dir,
									dir.rotateLeft(), dir.rotateRight(),
									dir.rotateLeft().rotateLeft() };
						else if (k != 0)
							neighborMap[i][j][k] = new Direction[] { dir,
									dir.rotateLeft(), dir.rotateRight(),
									dir.rotateRight().rotateRight() };
					} else {
						if (j == 0 && k == 0)
							neighborMap[i][j][k] = new Direction[] { dir };
						else if (j != 0 && j != 0)
							neighborMap[i][j][k] = new Direction[] { dir,
									dir.rotateLeft(), dir.rotateRight() };
						else if (j != 0)
							neighborMap[i][j][k] = new Direction[] { dir,
									dir.rotateLeft() };
						else if (k != 0)
							neighborMap[i][j][k] = new Direction[] { dir,
									dir.rotateRight() };
					}
				}
			}
		}

		return neighborMap;
	}

	public static int heuristic(int ax, int ay, int bx, int by) {
		int dx = Math.abs(ax - bx);
		int dy = Math.abs(ay - by);
		return (dx + dy) * 10 - Math.min(dx, dy) * 6;
	}

	private static Path buildPath() {
		ArrayList<MapLocation> path = new ArrayList<MapLocation>();
		MapLocation current = end;
		path.add(current);
		MapLocation next;
		Direction lastDir = null;
		while (!current.equals(start)) {
			next = from[current.x][current.y];
			Direction nextDir = next.directionTo(current);
			current = next;
			if (!pathShortener || lastDir != nextDir) {
				path.add(current);
				lastDir = nextDir;
			} else
				path.set(path.size() - 1, next);
		}

		Collections.reverse(path);

		return new Path(path);
	}

	public static boolean isDone() {
		return done || (open != null && open.isEmpty());
	}
}
