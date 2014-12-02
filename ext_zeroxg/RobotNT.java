package ext_zeroxg;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class RobotNT {
	public static final Direction[] customDirections = new Direction[] {
			Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST,
			Direction.NORTH_EAST, Direction.NORTH_WEST, Direction.SOUTH_EAST,
			Direction.SOUTH_WEST };
	public static Job job;
	private static MapLocation pastrLoc;
	private static MapLocation ntLoc;
	private static Direction[][] from;
	private static MapLocation[] senseLocs;
	private static final int maxRange = 150;
	@SuppressWarnings("unused")
	private static List<MapLocation> outerPath;
	@SuppressWarnings("unused")
	private static final int innerTurns = 10;
	@SuppressWarnings("unused")
	private static int currentInnerTurn = 0;
	private static final int diagonalMinDist = 1;
	private static final int minDist = 1;
	private static final int diagonalNoiseDist = 5;
	private static final int noiseDist = 7;
	private static final int diagonalMaxDist = 12;
	private static final int maxDist = 17;
	private static final int length = maxDist - noiseDist - minDist;
	private static final int diagonalLength = diagonalMaxDist
			- diagonalNoiseDist - diagonalMinDist;
	private static ArrayList<MapLocation> attacks;
	private static int pathIndex = 0;

	public static void init(RobotController rc) throws GameActionException {
		job = Channels.jobBoard.getJobAt(rc, rc.getLocation());
		initAttacks(rc);
	}

	private static void initAttacks(RobotController rc) {
		attacks = new ArrayList<MapLocation>();
		pastrLoc = findPASTRInRange(rc);
		if (pastrLoc == null)
			pastrLoc = rc.getLocation();
		for (Direction dir : customDirections) {
			int start = (dir.isDiagonal() ? diagonalMaxDist : maxDist);
			int end = start - (dir.isDiagonal() ? diagonalLength : length);
			boolean offmapFound = false;
			for (int i = start; i >= end; i--) {
				MapLocation loc = addDirection(pastrLoc, dir, i);

				if (rc.canAttackSquare(loc)) {
					if (rc.senseTerrainTile(loc) == TerrainTile.OFF_MAP) {
						offmapFound = true;
					} else {
						if (offmapFound) {
							attacks.add(loc.add(dir));
							offmapFound = false;
						}
						if (i == end) {
							attacks.add(loc);
							attacks.add(loc);
						} else
							attacks.add(loc);
					}
				}
			}
			if (offmapFound) {
				MapLocation loc = addDirection(pastrLoc, dir,
						(dir.isDiagonal() ? diagonalNoiseDist + diagonalMinDist
								: noiseDist + minDist));
				attacks.add(loc);
				attacks.add(loc);
			}
		}
	}

	public static void run(RobotController rc) throws GameActionException {
		if (job != null)
			job.claim(rc);

		if (rc.isActive()) {
			attack(rc, pathIndex);
			pathIndex++;
		}
	}

	private static void attack(RobotController rc, int index)
			throws GameActionException {
		rc.attackSquare(attacks.get(index % attacks.size()));
	}

	private static MapLocation addDirection(MapLocation loc, Direction dir,
			int times) {
		return new MapLocation(loc.x + times * dir.dx, loc.y + times * dir.dy);
	}

	@SuppressWarnings("unused")
	private static void attackBestInSensorRange(RobotController rc)
			throws GameActionException {
		double bestPop = -1;
		MapLocation bestLoc = null;
		for (MapLocation loc : senseLocs) {
			double pop = rc.senseCowsAtLocation(loc);
			if (pop > bestPop) {
				bestLoc = loc;
				bestPop = pop;
			}
		}

		rc.attackSquare(bestLoc.add(from[bestLoc.x][bestLoc.y]));
	}

	public static boolean isInitialized() {
		return senseLocs != null;
	}

	public static boolean isFarming(RobotController rc)
			throws GameActionException {
		MapLocation[] pLocs = rc.sensePastrLocations(rc.getTeam());
		for (MapLocation loc : pLocs) {
			if (loc.distanceSquaredTo(rc.getLocation()) <= 10)
				return true;
		}
		return false;
	}

	@SuppressWarnings("unused")
	private static boolean pastrExists(RobotController rc, MapLocation loc) {
		try {
			Robot r = (Robot) rc.senseObjectAtLocation(loc);
			if (r != null)
				return true;
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		return false;
	}

	private static MapLocation findPASTRInRange(RobotController rc) {
		MapLocation nearest = null;
		int bestDistance = 0;
		for (MapLocation loc : rc.sensePastrLocations(rc.getTeam())) {
			int distance = rc.getLocation().distanceSquaredTo(loc);
			if (nearest == null || distance < bestDistance) {
				nearest = loc;
				bestDistance = distance;
			}
		}

		if (bestDistance > RobotType.NOISETOWER.attackRadiusMaxSquared)
			return null;
		return nearest;
	}

	@SuppressWarnings("unused")
	private static void initPaths(RobotController rc) {
		PerfCounter.start();

		if (pastrLoc == null)
			return;
		ntLoc = rc.getLocation();
		LinkedList<MapLocation> open = new LinkedList<MapLocation>();
		FastLocSet closed = new FastLocSet();
		ArrayList<MapLocation> targets = new ArrayList<MapLocation>();
		ArrayList<MapLocation> ends = new ArrayList<MapLocation>();
		open.add(pastrLoc);
		closed.add(pastrLoc);
		int w = rc.getMapWidth();
		int h = rc.getMapHeight();
		from = new Direction[w][h];
		double[][] rate = rc.senseCowGrowth();

		while (!open.isEmpty()) {
			MapLocation current = open.pollFirst();
			boolean foundNext = false;
			for (Direction dir : RobotPlayer.customDirections) {
				MapLocation next = current.add(dir);
				TerrainTile type = rc.senseTerrainTile(next);

				if (type == TerrainTile.OFF_MAP || type == TerrainTile.VOID)
					continue;

				int distToNT = ntLoc.distanceSquaredTo(next);
				if (distToNT > maxRange)
					continue;

				if (!closed.contains(next)) {
					foundNext = true;
					open.add(next);
					closed.add(next);
					if (pastrLoc.distanceSquaredTo(next) > GameConstants.PASTR_RANGE) {
						from[next.x][next.y] = dir;
						if (distToNT <= RobotType.NOISETOWER.sensorRadiusSquared)
							targets.add(next);
					}
				}
			}
			if (!foundNext)
				ends.add(current);
		}
		senseLocs = targets.toArray(new MapLocation[targets.size()]);
		// PerfCounter.printCount();
	}
}
