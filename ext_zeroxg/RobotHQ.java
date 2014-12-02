package ext_zeroxg;

import java.util.ArrayList;
import java.util.LinkedList;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class RobotHQ {
	private static RobotController rc;
	private static int w;
	private static int h;
	private static double[][] milkMap;
	private static TerrainTile[][] map;
	private static boolean pastrRequested = false;
	private static LinkedList<MapLocation[]> pathfindingQueue;
	private static ArrayList<MapLocation> targetsPathfinded;
	private static MapLocation myHQ;
	private static MapLocation enemyHQ;
	private static MapLocation myPASTRLocation;
	private static boolean invalidPASTRLocation;
	private static int pastrBuildingTurn;
	private static double pastrTurnFactor = -5;
	private static double pastrTurnBase = 400;
	private static MapLocation rallyPoint;
	private static boolean destroyedPASTR;
	private static boolean pastrMoved = false;
	private static boolean ntRequested = false;
	private static boolean enemyHasBuiltPastr = false;
	private static boolean pastrBuilt = false;
	private static boolean enemyHasBuiltFirst = false;
	private static Job pastrJob;
	private static Job ntJob;
	private static int turnsWithPASTR = 0;
	private static double lastMilkRate = 500;
	private static int lastStep = 0;
	private static int limitDistance = 9999999;

	public static void init(RobotController rc) throws GameActionException {
		w = rc.getMapWidth();
		h = rc.getMapHeight();
		milkMap = rc.senseCowGrowth();
		myHQ = RobotPlayer.myHQ;
		enemyHQ = RobotPlayer.enemyHQ;
		RobotHQ.rc = rc;
		pathfindingQueue = new LinkedList<MapLocation[]>();
		targetsPathfinded = new ArrayList<MapLocation>();

		rallyPoint = myHQ;
		// if (!rallyPoint.equals(myHQ))
		// pathfindingQueue.add(new MapLocation[] { myHQ, rallyPoint });
		Channels.broadcastLocation(rc, Channels.armyTargetLocation, rallyPoint);

		pastrBuildingTurn = pastrRound();
		// System.out.println("PASTR turn: " + pastrBuildingTurn);
	}

	public static void run(RobotController rc) throws GameActionException {
		Channels.jobBoard.update(rc);

		if (rc.isActive()) // attacking
		{
			for (Robot r : rc.senseNearbyGameObjects(Robot.class,
					RobotType.HQ.sensorRadiusSquared,
					RobotPlayer.team.opponent())) {
				MapLocation loc = rc.senseRobotInfo(r).location;
				double distance = loc.distanceSquaredTo(rc.getLocation());
				if (distance <= RobotType.HQ.attackRadiusMaxSquared) {
					rc.attackSquare(loc);
					break;
				}
				loc = loc.add(loc.directionTo(rc.getLocation()));
				distance = loc.distanceSquaredTo(rc.getLocation());
				if (distance <= RobotType.HQ.attackRadiusMaxSquared) {
					rc.attackSquare(loc);
					break;
				}
			}
		}

		tryToSpawn();

		if (map == null)
			initMap();

		// Computation
		if (myPASTRLocation == null) {
			myPASTRLocation = computePASTRLocation();
			// System.out.println("PASTR Location computed! " +
			// myPASTRLocation);
			tryToSpawn();
			adjustPASTRLocation();
			// System.out.println("PASTR Location optimized! " +
			// myPASTRLocation);
			pathfindingQueue.add(new MapLocation[] { rallyPoint,
					myPASTRLocation });
			if (rallyPoint != myHQ)
				pathfindingQueue
						.add(new MapLocation[] { myHQ, myPASTRLocation });
		}

		strategy();

		int turnsLeft = rc.roundsUntilActive();
		if (!pathfindingQueue.isEmpty()) {
			MapLocation[] requestedPath = pathfindingQueue.get(0);
			if (requestedPath[0].equals(requestedPath[1]))
				pathfindingQueue.removeFirst();
			else {
				Path path = AStarJPS.find(map, requestedPath[0],
						requestedPath[1], turnsLeft);
				if (path != null || AStarJPS.isDone()) {
					pathfindingQueue.removeFirst();
					if (path != null) {
						Channels.network.addPath(rc, path);
						// path.print();
					} else {
						// WARNING: Invalid request
						// System.out.println("WARNING: Invalid path requested! "
						// + requestedPath[0] + " " + requestedPath[1]);
						if (requestedPath[0].equals(myPASTRLocation)
								|| requestedPath[1].equals(myPASTRLocation))
							invalidPASTRLocation = true;
					}
				}
			}
		}
	}

	@SuppressWarnings("unused")
	private static void strategy() throws GameActionException {
		MapLocation[] myPASTR = rc.sensePastrLocations(rc.getTeam());
		MapLocation[] enemyPASTRs = rc.sensePastrLocations(rc.getTeam()
				.opponent());
		double enemyMilk = rc.senseTeamMilkQuantity(rc.getTeam().opponent());

		if (myPASTR.length > 0)
			pastrBuilt = true;

		if (enemyPASTRs.length > 0) {
			if (lastStep < enemyMilk
					/ GameConstants.OPPONENT_MILK_SENSE_ACCURACY) {
				lastStep = (int) (enemyMilk / GameConstants.OPPONENT_MILK_SENSE_ACCURACY);
				lastMilkRate = GameConstants.OPPONENT_MILK_SENSE_ACCURACY
						/ turnsWithPASTR;
				turnsWithPASTR = 0;
			}
			turnsWithPASTR++;
		}

		double myMilk = rc.senseTeamMilkQuantity(rc.getTeam());
		double estimatedEnemyMilk = estimateEnemyMilk(enemyMilk);
		// rc.setIndicatorString(2, "Estimation: " + estimatedEnemyMilk);
		boolean pastrUnderAttack = rc.readBroadcast(Channels.pastrUnderAttack) == 1;
		boolean enemyHasAPASTR = enemyPASTRs.length > 0
				&& enemyPASTRs[0].distanceSquaredTo(enemyHQ) > 25;
		boolean endIsNear = enemyMilk >= 5000000 || Clock.getRoundNum() > 1750;
		boolean enemyHasMoreMilk = estimatedEnemyMilk > myMilk;

		// if (pastrUnderAttack || (enemyHasAPASTR && (enemyHasMoreMilk ||
		// enemyHasReached50)))
		// {
		// alert = AlertLevel.ORANGE;
		//
		// if (enemyHasAPASTR && enemyHasMoreMilk && enemyHasReached50)
		// {
		// alert = AlertLevel.RED;
		// }
		// }

		AlertLevel alert = AlertLevel.GREEN;
		if (enemyHasAPASTR
				&& (myPASTR.length == 0 || (enemyMilk >= 2500000
						&& enemyHasMoreMilk || (enemyHasBuiltFirst && !destroyedPASTR)))) {
			Channels.broadcastLocation(rc, Channels.armyTargetLocation,
					enemyPASTRs[0]);
			alert = AlertLevel.ORANGE;
			// if (endIsNear)
			// {
			// alert = AlertLevel.RED;
			// }
		} else
			Channels.broadcastLocation(rc, Channels.armyTargetLocation,
					myPASTRLocation);

		Channels.broadcastAlertLevel(rc, alert);
		// rc.setIndicatorString(0, "WOLOLO!   " + alert.ordinal());

		if (!destroyedPASTR) {
			main_loop: for (MapLocation loc : targetsPathfinded) {
				for (MapLocation pastr : enemyPASTRs) {
					if (loc.equals(pastr))
						continue main_loop;
				}
				destroyedPASTR = true;
				// System.out.println("Destroyed PASTR");
			}
		}

		if (map != null
				&& !pastrRequested
				&& (destroyedPASTR || Clock.getRoundNum() > pastrBuildingTurn || enemyMilk > GameConstants.WIN_QTY / 4)) {
			pastrRequested = true;
			requestPASTRCreation();
		}
		if (myPASTRLocation != null && invalidPASTRLocation
				&& myPASTR.length == 0) // TODO
		{
			// requestPASTRCreation();
			// invalidPASTRLocation = false;
			// Channels.broadcastLocation(rc, Channels.armyTargetLocation,
			// myPASTRLocation);
			// System.out.println("New pastr requested");
		}
		if (!ntRequested && rc.senseRobotCount() > 12) {
			ntRequested = true;
			requestNTCreation();
		}
		if ((!pastrMoved && !pastrBuilt && Clock.getRoundNum() > 1000)
				|| invalidPASTRLocation) {
			pastrMoved = true;
			myPASTRLocation = safestPASTRLocation();
			requestPASTRCreation();
			requestNTCreation();
			ntRequested = true;
		}

		if (enemyPASTRs != null && enemyPASTRs.length > 0) {
			MapLocation target = enemyPASTRs[0];
			if (target.distanceSquaredTo(enemyHQ) > 25) {
				if (!enemyHasBuiltPastr) {
					enemyHasBuiltPastr = true;
					if (!pastrBuilt)
						enemyHasBuiltFirst = true;
					requestNinja();
				}
				if (!targetsPathfinded.contains(target)) {
					targetsPathfinded.add(target);
					pathfindingQueue
							.add(new MapLocation[] { rallyPoint, target });
					if (rallyPoint != myHQ)
						pathfindingQueue
								.add(new MapLocation[] { myHQ, target });
					pathfindingQueue.add(new MapLocation[] { myPASTRLocation,
							target });
				}
			}
		}
	}

	private static void tryToSpawn() throws GameActionException {
		if (rc.isActive()) // spawning
		{
			if (rc.senseRobotCount() < GameConstants.MAX_ROBOTS) {
				for (Direction dir : RobotPlayer.customDirections) {
					if (rc.canMove(dir)) {
						rc.spawn(dir);
						Clock.getRoundNum();
						break;
					}
				}
			}
		}
	}

	private static double estimateEnemyMilk(double sensed) {
		return sensed + lastMilkRate * turnsWithPASTR;
	}

	private static int pastrRound() {
		int dx = Math.abs(enemyHQ.x - myHQ.x);
		int dy = Math.abs(enemyHQ.y - myHQ.y);
		double mDist = (dx + dy) - 0.6 * Math.min(dx, dy);
		// System.out.println(mDist);
		return (int) (pastrTurnFactor * mDist + pastrTurnBase);
	}

	@SuppressWarnings("unused")
	private static MapLocation bestRallyPoint() {
		int dx = enemyHQ.x - myHQ.x;
		int dy = enemyHQ.y - myHQ.y;
		MapLocation loc = new MapLocation(myHQ.x + dx / 3, myHQ.y + dy / 3);
		TerrainTile type = rc.senseTerrainTile(loc);
		if (type == TerrainTile.NORMAL || type == TerrainTile.ROAD)
			return loc;
		return myHQ;
	}

	private static void requestPASTRCreation() throws GameActionException {
		if (pastrJob != null)
			pastrJob.outdate(rc);

		pastrJob = new Job(JobType.BUILD_PASTR, myPASTRLocation);
		Channels.jobBoard.add(rc, pastrJob);
	}

	private static void requestNTCreation() throws GameActionException {
		MapLocation ntLoc = buildableLocationNextTo(myPASTRLocation);
		if (ntLoc != null) {
			if (ntJob != null)
				ntJob.outdate(rc);
			ntJob = new Job(JobType.NT_FARMING, ntLoc);
			Channels.jobBoard.add(rc, ntJob);
		}
	}

	private static void requestNinja() throws GameActionException {
		Channels.jobBoard.add(rc, new Job(JobType.NINJA, enemyHQ));
	}

	private static void adjustPASTRLocation() {
		if (myPASTRLocation != null) {
			MapLocation[] locs = MapLocation.getAllMapLocationsWithinRadiusSq(
					myPASTRLocation, 9);
			MapLocation best = null;
			double bestRate = milkRate(myPASTRLocation.x, myPASTRLocation.y);
			for (MapLocation loc : locs) {
				if (loc.equals(myPASTRLocation))
					continue;
				double rate = milkRate(loc.x, loc.y);
				if (rate > bestRate) {
					bestRate = rate;
					best = loc;
				}
			}
			if (best != null) {
				myPASTRLocation = best;
				adjustPASTRLocation();
			}
		}
	}

	private static boolean walkable(int x, int y) {
		return x >= 0 && x < w && y >= 0 && y < h
				&& (map[x][y] != TerrainTile.VOID);
	}

	private static double locRate(int x, int y) {
		return walkable(x, y) ? milkMap[x][y] : 0;
	}

	private static double milkRate(int i, int j) {
		if (!walkable(i, j))
			return -2;
		double rate = 0;
		rate += locRate(i, j);
		rate += locRate(i - 1, j);
		rate += locRate(i + 1, j);
		rate += locRate(i - 1, j - 1);
		rate += locRate(i - 1, j + 1);
		rate += locRate(i + 1, j - 1);
		rate += locRate(i + 1, j + 1);
		rate += locRate(i, j - 1);
		rate += locRate(i, j + 1);

		rate += locRate(i, j + 2);
		rate += locRate(i + 1, j + 2);
		rate += locRate(i - 1, j + 2);

		rate += locRate(i, j - 2);
		rate += locRate(i + 1, j - 2);
		rate += locRate(i - 1, j - 2);

		rate += locRate(i + 2, j);
		rate += locRate(i + 2, j + 1);
		rate += locRate(i + 2, j - 1);

		rate += locRate(i - 2, j);
		rate += locRate(i - 2, j + 1);
		rate += locRate(i - 2, j - 1);

		return rate;

	}

	private static MapLocation safestPASTRLocation() {
		double[][] growth = rc.senseCowGrowth();
		int w = rc.getMapWidth();
		int h = rc.getMapHeight();

		MapLocation bestLoc = null;
		int bestDistance = 0;

		for (int i = 0; i < w; i++) {
			for (int j = 0; j < h; j++) {
				if (map[i][j] != TerrainTile.VOID && growth[i][j] > 0) {
					MapLocation loc = new MapLocation(i, j);
					int distance = closeness(loc);

					if (bestDistance > distance) {
						bestLoc = loc;
						bestDistance = distance;
					}
				}
			}
		}

		return bestLoc;
	}

	// private static int fastDistance(MapLocation a, MapLocation b)
	// {
	// int dx = a.x - b.x;
	// int dy = a.y - b.y;
	// return dx * dx + dy * dy;
	// }

	private static int closeness(MapLocation loc) {
		int toHQ = loc.distanceSquaredTo(myHQ);
		int toEHQ = loc.distanceSquaredTo(enemyHQ);
		return toHQ < toEHQ ? 3 * toHQ - toEHQ : 9999999;
	}

	private static MapLocation computePASTRLocation()
			throws GameActionException {
		double[][] growth = rc.senseCowGrowth();
		int w = rc.getMapWidth();
		int h = rc.getMapHeight();
		int gridSize = 2;

		double bestRate = -1;
		MapLocation bestLoc = null;
		int bestDistance = 0;

		for (int i = 0; i < w; i += gridSize) {
			for (int j = 0; j < h; j += gridSize) {
				if (map[i][j] != TerrainTile.VOID) {
					double rate = growth[i][j];
					// rate += growth[i - 1][j];
					// rate += growth[i + 1][j];
					// rate += growth[i - 1][j - 1];
					// rate += growth[i - 1][j + 1];
					// rate += growth[i + 1][j - 1];
					// rate += growth[i + 1][j + 1];
					// rate += growth[i][j - 1];
					// rate += growth[i][j + 1];

					if (rate > bestRate) {
						MapLocation loc = new MapLocation(i, j);
						int distance = closeness(loc);
						if (distance != limitDistance) {
							bestRate = rate;
							bestLoc = loc;
							bestDistance = distance;
						}
					} else if (rate == bestRate) {
						MapLocation loc = new MapLocation(i, j);
						int distance = closeness(loc);
						if (distance < bestDistance) {
							bestRate = rate;
							bestLoc = loc;
							bestDistance = distance;
						}
					}
				}
			}
			tryToSpawn();
		}

		return bestLoc;
	}

	private static MapLocation buildableLocationNextTo(MapLocation start) {
		int w = rc.getMapWidth();
		int h = rc.getMapHeight();
		for (int i = 0; i < 8; i++) {
			Direction dir = Direction.values()[i];
			MapLocation l = start.add(dir);
			if (l.x >= 0 && l.x < w && l.y >= 0 && l.y < h
					&& (map[l.x][l.y] != TerrainTile.VOID))
				return l;
		}
		for (MapLocation l : MapLocation.getAllMapLocationsWithinRadiusSq(
				myPASTRLocation, 10))
			if (l.x >= 0 && l.x < w && l.y >= 0 && l.y < h
					&& (map[l.x][l.y] != TerrainTile.VOID))
				return l;

		for (MapLocation l : MapLocation.getAllMapLocationsWithinRadiusSq(
				myPASTRLocation, 20))
			if (l.x >= 0 && l.x < w && l.y >= 0 && l.y < h
					&& (map[l.x][l.y] != TerrainTile.VOID))
				return l;

		return null;
	}

	private static void initMap() {
		int w = rc.getMapWidth();
		int h = rc.getMapHeight();
		map = new TerrainTile[w][h];

		for (int i = w; --i >= 0;) {
			for (int j = h; --j >= 0;) {
				map[i][j] = rc.senseTerrainTile(new MapLocation(i, j));
			}
		}

		MapLocation[] locs = MapLocation.getAllMapLocationsWithinRadiusSq(
				enemyHQ, 25);
		for (MapLocation l : locs) {
			if (l.x >= 0
					&& l.x < w
					&& l.y >= 0
					&& l.y < h
					&& (l.distanceSquaredTo(enemyHQ) != 25 || l.directionTo(
							enemyHQ).isDiagonal())) {
				map[l.x][l.y] = TerrainTile.VOID;
			}
		}
	}

	@SuppressWarnings("unused")
	private static int mapInitCost() {
		return 5 * rc.getMapHeight() * rc.getMapWidth() * 10
				/ GameConstants.BYTECODE_LIMIT;
	}
}
