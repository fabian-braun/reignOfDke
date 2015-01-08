package dualcore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class MapAnalyzer {

	protected RobotController rc;

	private MapLocation myHq;
	private MapLocation otherHq;
	private int ySize;
	private int xSize;

	private MapLocation bestForPastr;
	private char[][] mapRepresentation;
	private double[][] mapCowGrowth;
	private double[][] mapPastrRating;

	private int[][] realDistanceFromOpponentHQ;
	PriorityQueue<MapLocationDistance> queue;
	private boolean realDistanceReady = false;

	public MapAnalyzer(RobotController rc, MapLocation myHQ,
			MapLocation otherHq, int ySize, int xSize) {
		this.rc = rc;
		this.myHq = myHQ;
		this.otherHq = otherHq;
		this.ySize = ySize;
		this.xSize = xSize;
	}

	/**
	 * generates a map. Each location gets assigned the number of moves
	 * necessary to reach it from the opponent's HQ. (shortest path) Currently
	 * takes about 500 rounds to be executed.. way to expensive. The underlying
	 * algorithm is Dijkstra.
	 */
	public void generateRealDistanceMap() {
		realDistanceFromOpponentHQ = new int[ySize][xSize];
		for (int[] row : realDistanceFromOpponentHQ) {
			Arrays.fill(row, Integer.MAX_VALUE - 1);
		}
		queue = new PriorityQueue<MapLocationDistance>();
		MapLocationDistance source = new MapLocationDistance(otherHq, 0);
		queue.add(source);
		while (!queue.isEmpty()) {
			MapLocationDistance current = queue.poll();
			List<MapLocation> neighbours = getNeighbours(current
					.getMapLocation());
			for (MapLocation neighbour : neighbours) {
				MapLocationDistance n = new MapLocationDistance(neighbour,
						realDistanceFromOpponentHQ[neighbour.y][neighbour.x]);
				int distanceOverCurrent = current.getDistance() + 1;
				if (distanceOverCurrent < n.getDistance()) {
					queue.remove(n);
					n.setDistance(distanceOverCurrent);
					realDistanceFromOpponentHQ[n.getMapLocation().y][n
							.getMapLocation().x] = distanceOverCurrent;
					queue.add(n);
				}
				n.setDistance(current.getDistance() + 1);
			}
		}
	}

	private boolean isXonMap(int x) {
		return x >= 0 && x < xSize;
	}

	private boolean isYonMap(int y) {
		return y >= 0 && y < ySize;
	}

	/**
	 * returns all neighbours of a location fulfilling the following conditions:
	 * <li>the terrain type is NORMAL or ROAD</li> <li>the location is located
	 * on the map</li><li>the location is not an HQ-location</li> <br>
	 * in short: any location which a soldier can move to</br>
	 * 
	 * @param from
	 * @return
	 */
	private List<MapLocation> getNeighbours(MapLocation from) {
		List<MapLocation> neighbours = new ArrayList<MapLocation>();
		addNeighbour(neighbours, new MapLocation(from.x - 1, from.y - 1));
		addNeighbour(neighbours, new MapLocation(from.x - 1, from.y));
		addNeighbour(neighbours, new MapLocation(from.x - 1, from.y + 1));
		addNeighbour(neighbours, new MapLocation(from.x, from.y - 1));
		addNeighbour(neighbours, new MapLocation(from.x, from.y + 1));
		addNeighbour(neighbours, new MapLocation(from.x + 1, from.y - 1));
		addNeighbour(neighbours, new MapLocation(from.x + 1, from.y));
		addNeighbour(neighbours, new MapLocation(from.x + 1, from.y + 1));
		for (int i = neighbours.size() - 1; i >= 0; i--) {
			MapLocation current = neighbours.get(i);
			TerrainTile terrain = rc.senseTerrainTile(new MapLocation(
					current.x, current.y));
			if (terrain == TerrainTile.VOID || terrain == TerrainTile.OFF_MAP
					|| current.equals(myHq) || current.equals(otherHq)) {
				neighbours.remove(i);
			}
		}
		return neighbours;
	}

	private void addNeighbour(List<MapLocation> neighbours,
			MapLocation potentialNeighbour) {
		if (isXonMap(potentialNeighbour.x) && isYonMap(potentialNeighbour.y)) {
			neighbours.add(potentialNeighbour);
		}
	}

	public int getRealDistanceToOpponentHq(MapLocation loc) {
		if (realDistanceReady)
			return realDistanceFromOpponentHQ[loc.y][loc.x];
		else
			return Integer.MAX_VALUE - 1;
	}

	/**
	 * evaluates which location on the map is optimal to construct a pastr.<br/>
	 * This evaluation for each location is based on <li>the cow growth rate</li>
	 * <li>the distance to the opponent's HQ</li><br/>
	 * 
	 * In order to keep the performance small, not all tiles are investigated
	 * but only around 24*24. If the map is smaller then all tiles are
	 * investigated. If the real distance has been calculated already it is used
	 * for this method (see {@link #generateRealDistanceMap()}). If not the
	 * direct distance is used instead.
	 * 
	 * @return the optimal pastr location
	 */
	public MapLocation evaluateBestPastrLoc() {
		if (bestForPastr != null) {
			// already found previously
			return bestForPastr;
		}
		mapCowGrowth = rc.senseCowGrowth();
		mapPastrRating = new double[ySize][xSize];
		double currentBestRating = 0;
		bestForPastr = new MapLocation(0, 0);
		int xStep = xSize / 25 + 1;
		int yStep = ySize / 25 + 1;
		for (int y = 2; y < ySize; y += yStep) {
			for (int x = 2; x < xSize; x += xStep) {
				TerrainTile tile = rc.senseTerrainTile(new MapLocation(x, y));
				if (tile != TerrainTile.NORMAL && tile != TerrainTile.ROAD) {
					continue;
				}
				double sumCowGrowth = 0;
				for (int ylocal = y - 1; ylocal <= y + 1; ylocal++) {
					for (int xlocal = x - 1; xlocal <= x + 1; xlocal++) {
						if (ylocal >= 0 && xlocal >= 0 && ylocal < ySize
								&& xlocal < xSize) {
							// mapCowGrowth has x before y.. this is no bug
							sumCowGrowth += mapCowGrowth[x][y];
						}
					}
				}
				int distance = PathFinder.getRequiredMoves(new MapLocation(x, y),
						otherHq);
				if (realDistanceReady) {
					distance = getRealDistanceToOpponentHq(new MapLocation(x, y));
				}
				mapPastrRating[y][x] = distance * sumCowGrowth;
				if (mapPastrRating[y][x] > currentBestRating) {
					currentBestRating = mapPastrRating[y][x];
					bestForPastr = new MapLocation(x, y);
				}
			}
		}
		return bestForPastr;
	}

	/**
	 * should not be used in competition. Prints out the map terrain
	 */
	public void printMap() {
		mapRepresentation = new char[ySize][xSize];
		for (int y = 0; y < ySize; y++) {
			for (int x = 0; x < xSize; x++) {
				TerrainTile tile = rc.senseTerrainTile(new MapLocation(x, y));
				switch (tile) {
				case OFF_MAP:
					mapRepresentation[y][x] = 'X';
					break;
				case ROAD:
					mapRepresentation[y][x] = '#';
					break;
				case VOID:
					mapRepresentation[y][x] = 'X';
					break;
				default:
					if (mapCowGrowth[y][x] > 0) {
						mapRepresentation[y][x] = (char) (48 + (int) mapCowGrowth[y][x]);
					} else {
						mapRepresentation[y][x] = ' ';
					}
				}
			}
		}
		mapRepresentation[myHq.y][myHq.x] = 'H';
		mapRepresentation[otherHq.y][otherHq.x] = 'E';
		System.out.println("map:");
		for (int y = 0; y < mapRepresentation.length; y++) {
			for (int x = 0; x < mapRepresentation[y].length; x++) {
				System.out.print(mapRepresentation[y][x]);
			}
			System.out.println();
		}
	}

	/**
	 * should not be used in competition. Prints out the pastr rating of the
	 * locations
	 */
	public void printMapAnalysis() {
		System.out.println("map:");
		for (int y = 0; y < ySize; y++) {
			for (int x = 0; x < xSize; x++) {
				System.out.print(String.format("%5.0f", mapPastrRating[y][x]));
			}
			System.out.println();
		}
		System.out.println("best Pastr Rating at " + bestForPastr.toString());
		System.out.println("other hq is at " + otherHq.toString());
	}

	/**
	 * should not be used in competition. Prints out the distance of each
	 * location to the opponent's HQ
	 */
	public void printMapAnalysisDistance() {
		System.out.println("distance to opponent map:");
		for (int y = 0; y < ySize; y++) {
			for (int x = 0; x < xSize; x++) {
				if (realDistanceFromOpponentHQ[y][x] > 1000) {
					System.out.print("XX ");
				} else {
					System.out.print(String.format("%02d ",
							realDistanceFromOpponentHQ[y][x]));
				}
			}
			System.out.println();
		}
	}
}
