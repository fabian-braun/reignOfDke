package reignierOfDKE;

import reignierOfDKE.C.MapComplexity;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class Core extends Soldier {

	public static final int id = 500;
	// for navigation to save place
	private boolean reachedSavePlace = false;
	private int encounteredObstacles = 0;
	private MapLocation savePlace;
	private boolean secondInitFinished = false;
	private Team[] teams;
	private PathFinderAStarFast pathFinderAStarFast;

	// information about opponent
	private MapLocation[] brdCastingOppSoldiersLocations;

	public Core(RobotController rc) {
		super(rc);
	}

	@Override
	protected void init() throws GameActionException {
		rc.setIndicatorString(0, "DUALCORE - OOOOH YEAH");
		Channel.resetMapComplexity(rc);
		MapLocation oppHq = rc.senseEnemyHQLocation();
		MapLocation ourHq = rc.senseHQLocation();
		Direction saveDir = oppHq.directionTo(ourHq);
		savePlace = ourHq.add(saveDir, 3);
		pathFinderGreedy = new PathFinderGreedy(rc, randall);
		pathFinderGreedy.setTarget(savePlace);
		teams = Team.getTeams(rc);
		Channel.signalAlive(rc, id);
		determinePathFinder();
	}

	@Override
	protected void act() throws GameActionException {
		Channel.signalAlive(rc, id);
		if (!reachedSavePlace && encounteredObstacles < 3) {
			if (rc.isActive()) {
				// move to save place
				if (!pathFinderGreedy.move()) {
					encounteredObstacles++;
				}
				reachedSavePlace = rc.getLocation().equals(savePlace)
						|| encounteredObstacles > 2;
			}
			return;
		} else if (!secondInitFinished) {
			// do remaining initialization parts after reaching a save location
			// init pathFinder to help other soldiers build the reduced map
			pathFinderAStarFast = new PathFinderAStarFast(rc, id);

			secondInitFinished = true;
		}
		analyzeOpponentBehavior();
	}

	private void determinePathFinder() {
		TerrainTile[][] map = pathFinderGreedy.map;
		int rating = 0;
		for (int y = 1; y < pathFinderGreedy.ySize; y += 2) {
			Channel.signalAlive(rc, id);
			for (int x = 0; x < pathFinderGreedy.xSize; x += 2) {
				if (map[y][x].equals(TerrainTile.VOID)) {
					rating -= 6;
				} else {
					rating++;
				}
			}
		}
		if (rating > 0) {
			Channel.broadcastMapComplexity(rc, MapComplexity.SIMPLE);
		} else {
			Channel.broadcastMapComplexity(rc, MapComplexity.COMPLEX);
		}
	}

	private void analyzeOpponentBehavior() {
		brdCastingOppSoldiersLocations = rc.senseBroadcastingRobotLocations(rc
				.getTeam().opponent());
		if (Soldier.size(brdCastingOppSoldiersLocations) < 1) {
			// prevent NullPtrException
			brdCastingOppSoldiersLocations = new MapLocation[0];
		}
		int countBrdCastingOppSoldiers = brdCastingOppSoldiersLocations.length;
		Channel.broadcastCountOppBrdCastingSoldiers(rc,
				countBrdCastingOppSoldiers);
		MapLocation oppSoldiersCenter = getCenter(brdCastingOppSoldiersLocations);
		Channel.broadcastPositionalCenterOfOpponent(rc, oppSoldiersCenter);
		int oppSoldiersMeanDistToCenter = getMeanDistance(
				brdCastingOppSoldiersLocations, oppSoldiersCenter);
		Channel.broadcastOpponentMeanDistToCenter(rc,
				oppSoldiersMeanDistToCenter);
		int milk = (int) rc.senseTeamMilkQuantity(rc.getTeam().opponent());
		Channel.broadcastOpponentMilkQuantity(rc, milk);
	}

	private MapLocation getCenter(MapLocation[] locations) {
		int y = 0;
		int x = 0;
		if (Soldier.size(locations) < 1) {
			return new MapLocation(pathFinderGreedy.ySize / 2,
					pathFinderGreedy.xSize / 2);
		}
		for (MapLocation loc : locations) {
			y += loc.y;
			x += loc.x;
		}
		return new MapLocation(y / locations.length, x / locations.length);
	}

	private int getMeanDistance(MapLocation[] locations, MapLocation to) {
		if (Soldier.size(locations) < 1) {
			return 100000;
		}
		int dist = 0;
		for (MapLocation loc : locations) {
			dist += PathFinder.getManhattanDist(loc, to);
		}
		return dist / locations.length;
	}
}
