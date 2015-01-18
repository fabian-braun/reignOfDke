package reignierOfDKE;

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
			if (Channel.getMapComplexity(rc).equals(MapComplexity.COMPLEX)) {
				pathFinderAStarFast = new PathFinderAStarFast(rc, id);
			}
			Channel.signalAlive(rc, id);
			secondInitFinished = true;
		}
		analyzeOpponentBehavior();
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
		if (size(locations) < 1) {
			return pathFinderGreedy.hqEnemLoc;
		}
		for (MapLocation loc : locations) {
			y += loc.y;
			x += loc.x;
		}
		return new MapLocation(y / locations.length, x / locations.length);
	}

	private int getMeanDistance(MapLocation[] locations, MapLocation to) {
		if (size(locations) < 1) {
			return 100000;
		}
		int dist = 0;
		for (MapLocation loc : locations) {
			dist += PathFinder.getManhattanDist(loc, to);
		}
		return dist / locations.length;
	}
}
