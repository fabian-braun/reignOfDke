package reignierOfDKE;

import java.util.HashSet;
import java.util.Set;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class Core extends Soldier {

	private static final int pastrLocUpdateInterval = 70;
	public static final int id = 500;
	// for navigation to save place
	private boolean reachedSavePlace = false;
	private int encounteredObstacles = 0;
	private MapLocation savePlace;
	private boolean secondInitFinished = false;
	@SuppressWarnings("unused")
	// is not used, but increases speed of init for soldiers
	private PathFinderAStarFast pathFinderAStarFast;
	private MapAnalyzer mapAnalyzer;

	// information about opponent
	private MapLocation[] brdCastingOppSoldiersLocations;
	private MapLocation oppSoldiersCenter;

	public Core(RobotController rc) {
		super(rc);
	}

	private void updatePastrLocations() {
		Set<MapLocation> avoidLenient = new HashSet<MapLocation>();
		avoidLenient.add(pathFinderGreedy.hqEnemLoc);
		avoidLenient.add(oppSoldiersCenter);
		Set<MapLocation> avoidStrict = new HashSet<MapLocation>();
		MapLocation loc1 = mapAnalyzer.getGoodPastrLocation(avoidStrict,
				avoidLenient);
		Channel.announcePastrLocation(rc, loc1, 1);
		avoidStrict.add(loc1); // don't want to get that location again
		MapLocation loc2 = mapAnalyzer.getGoodPastrLocation(avoidStrict,
				avoidLenient);
		Channel.announcePastrLocation(rc, loc2, 2);
	}

	@Override
	protected void init() throws GameActionException {
		// rc.setIndicatorString(0, "DUALCORE - OOOOH YEAH");
		Channel.resetMapComplexity(rc);
		MapLocation oppHq = rc.senseEnemyHQLocation();
		MapLocation ourHq = rc.senseHQLocation();
		Direction saveDir = oppHq.directionTo(ourHq);
		savePlace = ourHq.add(saveDir, 3);
		pathFinderGreedy = new PathFinderGreedy(rc, randall, id);
		oppSoldiersCenter = pathFinderGreedy.hqEnemLoc;
		pathFinderGreedy.setTarget(savePlace);
		Channel.signalAlive(rc, id);
		determinePathFinder();
		mapAnalyzer = new MapAnalyzer(rc, null, pathFinderGreedy.hqSelfLoc,
				pathFinderGreedy.hqEnemLoc, pathFinderGreedy.ySize,
				pathFinderGreedy.xSize, id, randall);
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
			updatePastrLocations();
			if (Channel.getMapComplexity(rc).equals(MapComplexity.COMPLEX)) {
				pathFinderAStarFast = new PathFinderAStarFast(rc, id);
			}
			Channel.signalAlive(rc, id);
			secondInitFinished = true;
		}
		analyzeOpponentBehavior();
		if (Clock.getRoundNum() % pastrLocUpdateInterval == 0) {
			updatePastrLocations();
		}
	}

	private void determinePathFinder() {
		// Get the map from the pathFinder
		TerrainTile[][] map = pathFinderGreedy.map;
		boolean complexMap = false;
		// We only check half the map, since they are mirrored
		int halfwayCoord = (int) Math.ceil(Math.min(pathFinderGreedy.ySize,
				pathFinderGreedy.xSize) / 2.0);
		for (int i = 0; i < halfwayCoord; i++) {
			Channel.signalAlive(rc, id);
			if (map[i][i].equals(TerrainTile.VOID)) {
				// If we find a VOID tile anywhere on this cross section, it's a
				// complex map
				complexMap = true;
				break;
			}
		}

		if (complexMap) {
			Channel.broadcastMapComplexity(rc, MapComplexity.COMPLEX);
		} else {
			Channel.broadcastMapComplexity(rc, MapComplexity.SIMPLE);
		}
	}

	private void analyzeOpponentBehavior() {
		brdCastingOppSoldiersLocations = rc.senseBroadcastingRobotLocations(rc
				.getTeam().opponent());
		if (size(brdCastingOppSoldiersLocations) < 1) {
			// prevent NullPtrException
			brdCastingOppSoldiersLocations = new MapLocation[0];
		}
		oppSoldiersCenter = getCenter(brdCastingOppSoldiersLocations);
		int milk = (int) rc.senseTeamMilkQuantity(rc.getTeam().opponent());
		Channel.broadcastOpponentMilkQuantity(rc, milk);
	}

	private MapLocation getCenter(MapLocation[] locations) {
		int y = 0;
		int x = 0;
		if (size(locations) < 1) {
			return pathFinderGreedy.hqEnemLoc;
		}
		for (MapLocation loc : locations) {
			y += loc.y;
			x += loc.x;
		}
		return new MapLocation(y / locations.length, x / locations.length);
	}

}
