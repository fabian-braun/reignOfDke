package teamreignofdke;

import java.util.Map;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class HQ extends AbstractRobotType {

	private int height;
	private int width;
	private MapLocation myHq;
	private MapLocation otherHq;
	private int boundaryBeforePastr = 10;
	private MapAnalyzer mapAnalyzer;

	public HQ(RobotController rc) {
		super(rc);
	}

	@Override
	protected void act() throws GameActionException {
		Map<SoldierRole, Integer> roleCount = Channel.getSoldierRoleCount(rc);
		Map<RobotType, Integer> typeCount = Channel.getRobotTypeCount(rc);

		// Check if a robot is spawnable and spawn one if it is
		if (rc.senseRobotCount() < GameConstants.MAX_ROBOTS) {
			if (roleCount.get(SoldierRole.PASTR_BUILDER) < 1
					&& typeCount.get(RobotType.PASTR) < 1
					&& typeCount.get(RobotType.SOLDIER) > boundaryBeforePastr) {
				// printMap();
				MapLocation bestPastrLoc = mapAnalyzer.evaluateBestPastrLoc();
				// printMapAnalysis();
				Channel.broadcastBestPastrLocation(rc, bestPastrLoc);
				Channel.demandSoldierRole(rc, SoldierRole.PASTR_BUILDER);
				// If we don't have a noise-tower-builder and also don't have a
				// noise tower yet
			} else if (roleCount.get(SoldierRole.NOISE_TOWER_BUILDER) < 1
					&& typeCount.get(RobotType.NOISETOWER) < 1
					&& typeCount.get(RobotType.SOLDIER) > boundaryBeforePastr) {
				// Demand a noise-tower-builder
				Channel.demandSoldierRole(rc, SoldierRole.NOISE_TOWER_BUILDER);
			} else if (randall.nextInt(3) > 0
					|| roleCount.get(SoldierRole.PROTECTOR) > 4) {
				Channel.demandSoldierRole(rc, SoldierRole.ATTACKER);
			} else {
				Channel.demandSoldierRole(rc, SoldierRole.PROTECTOR);
			}
			Direction spawnAt = myHq.directionTo(otherHq);
			if (rc.isActive()) {
				int i = 0;
				while (!rc.canMove(spawnAt) && i < C.DIRECTIONS.length) {
					spawnAt = C.DIRECTIONS[i];
					i++;
				}
				if (rc.canMove(spawnAt)) {
					rc.spawn(spawnAt);
				}
			}
		}
	}

	@Override
	protected void init() throws GameActionException {
		height = rc.getMapHeight();
		width = rc.getMapWidth();
		myHq = rc.senseHQLocation();
		otherHq = rc.senseEnemyHQLocation();

		// location between our HQ and opponent's HQ:
		MapLocation temporaryTarget = new MapLocation(
				(myHq.x * 3 / 4 + otherHq.x / 4),
				(myHq.y * 3 / 4 + otherHq.y / 4));

		Channel.broadcastBestPastrLocation(rc, temporaryTarget);

		mapAnalyzer = new MapAnalyzer(rc, myHq, otherHq, height, width);
		mapAnalyzer.generateRealDistanceMap();
		mapAnalyzer.printMapAnalysisDistance();
	}
}
