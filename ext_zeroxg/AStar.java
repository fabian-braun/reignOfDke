package ext_zeroxg;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.TerrainTile;

public class AStar {
	public static int[][] h;
	public static int[][] g;
	protected static PriorityQueue<MapLocation> open;
	protected static FastLocSet closed;
	protected static MapLocation[][] from;
	protected static MapLocation realStart;
	protected static MapLocation realEnd;
	protected static MapLocation start;
	protected static MapLocation end;
	protected static int mapWidth;
	protected static int mapHeight;
	protected static TerrainTile[][] map;

	/*
	 * public static void init(TerrainTile[][] map, MapLocation start,
	 * MapLocation end) { mapWidth = map.length; mapHeight = map[0].length;
	 * realStart = start; realEnd = end; AStar.start = new
	 * MapLocation(realEnd.x, realEnd.y); AStar.end = new
	 * MapLocation(realStart.x, realStart.y); AStar.map = map;
	 * 
	 * MapLocationComparator comparator = new MapLocationComparator(); open =
	 * new PriorityQueue<MapLocation>(10, comparator); closed = new
	 * FastLocSet(); from = new MapLocation[mapWidth][mapHeight]; g = new
	 * int[mapWidth][mapHeight]; // distance of the best path to x y h = new
	 * int[mapWidth][mapHeight]; // estimated distance to the end
	 * 
	 * open.add(AStar.start); g[AStar.start.x][AStar.start.y] = 0;
	 * h[AStar.start.x][AStar.start.y] = heuristic(AStar.start.x, AStar.start.y,
	 * AStar.end.x, AStar.end.y); }
	 */

	/*
	 * public static ArrayList<MapLocation> find(int turns) { int startTurn =
	 * Clock.getRoundNum(); while (!open.isEmpty() && Clock.getRoundNum() -
	 * startTurn < turns - 1) { MapLocation current = open.poll(); for
	 * (Direction d : RobotPlayer.customDirections) { MapLocation next =
	 * current.add(d); if (next.x < 0 || next.y < 0 || next.x >= mapWidth ||
	 * next.y >= mapHeight) continue;
	 * 
	 * if (next.equals(end)) { from[end.x][end.y] = current; return buildPath();
	 * }
	 * 
	 * if (h[next.x][next.y] == 0) h[next.x][next.y] = heuristic(next.x, next.y,
	 * end.x, end.y); if (map[next.x][next.y] == TerrainTile.VOID) continue; int
	 * speed = moveSpeed(next, d); int newg = g[current.x][current.y] + speed;
	 * 
	 * if (!closed.contains(next)) { boolean better = newg < g[next.x][next.y];
	 * if (better) open.remove(next); if (g[next.x][next.y] == 0 || better) {
	 * g[next.x][next.y] = newg; from[next.x][next.y] = current; open.add(next);
	 * } }
	 * 
	 * } closed.add(current); }
	 * 
	 * return null; }
	 */

	@SuppressWarnings("unused")
	private static int moveSpeed(MapLocation l, Direction d) {
		switch (d) {
		case EAST:
		case NORTH:
		case SOUTH:
		case WEST:
			switch (map[l.x][l.y]) {
			case NORMAL:
				return 10;
			case ROAD:
				return 5;
			default:
				return 10000;
			}
		default:
			switch (map[l.x][l.y]) {
			case NORMAL:
				return 14;
			case ROAD:
				return 7;
			default:
				return 10000;
			}
		}
	}

	@SuppressWarnings("unused")
	private static int heuristic(int ax, int ay, int bx, int by) {
		int dx = Math.abs(ax - bx);
		int dy = Math.abs(ay - by);
		return (dx + dy) * 10;
	}

	@SuppressWarnings("unused")
	private static ArrayList<MapLocation> buildPath() {
		ArrayList<MapLocation> path = new ArrayList<MapLocation>();
		MapLocation current = end;
		path.add(realStart);
		while (!current.equals(start)) {
			current = from[current.x][current.y];
			path.add(current);
		}

		return path;
	}

	public static boolean isDone() {
		return open.isEmpty();
	}

	public static void printPath(List<MapLocation> path) {
		System.out.println();
		for (MapLocation l : path) {
			System.out.print(l.toString() + " ");
		}
		System.out.println();
	}

	public static void printMap(int[][] map) {
		System.out.println("MAP: ");
		for (int i = 0; i < map[0].length; i++) {
			for (int j = 0; j < map.length; j++) {
				System.out.print(map[j][i] + " ");
			}
			System.out.println();
		}
	}

	public static void printMap(TerrainTile[][] map) {
		System.out.println("MAP: ");
		for (int i = 0; i < map[0].length; i++) {
			for (int j = 0; j < map.length; j++) {
				switch (map[j][i]) {
				case NORMAL:
					System.out.print(".");
					break;
				case OFF_MAP:
					System.out.print("O");
					break;
				case ROAD:
					System.out.print(":");
					break;
				case VOID:
					System.out.print("X");
					break;
				default:
					break;

				}
			}
			System.out.println();
		}
	}
}
