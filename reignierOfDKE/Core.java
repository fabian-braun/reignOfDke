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
	private Team[] teams;

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
		}
		Team.updateTeamLocation(rc, teams);
	}

	private void determinePathFinder() {
		TerrainTile[][] map = pathFinderGreedy.map;
		int rating = 0;
		for (int y = 1; y < pathFinderGreedy.ySize; y += 2) {
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
}
