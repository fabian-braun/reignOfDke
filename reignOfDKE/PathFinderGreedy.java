package reignOfDKE;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class PathFinderGreedy extends PathFinder {

	private MapLocation target;
	private int moveRandom = 0;
	private Random randall;

	public PathFinderGreedy(RobotController rc, Random randall, int soldierId) {
		super(rc, soldierId);
		this.randall = randall;
	}

	public PathFinderGreedy(RobotController rc, TerrainTile[][] map,
			MapLocation hqSelfLoc, MapLocation hqEnemLoc, int ySize, int xSize,
			int soldierId) {
		super(rc, map, hqSelfLoc, hqEnemLoc, ySize, xSize, soldierId);
		this.randall = new Random();
	}

	@Override
	public boolean move() throws GameActionException {
		if (moveRandom > 0) {
			moveRandom--;
			int index = randall.nextInt(C.DIRECTIONS.length);
			Direction dir = C.DIRECTIONS[index];
			if (rc.canMove(dir)) {
				rc.move(dir);
				return true;
			} else {
				return false;
			}
		} else {
			Direction dir = rc.getLocation().directionTo(target);
			if (rc.canMove(dir)) {
				rc.move(dir);
				return true;
			} else {
				moveRandom = 3;
				return false;
			}
		}
	}

	@Override
	public boolean sneak() throws GameActionException {
		return false;
	}

	@Override
	public void setTarget(MapLocation target) {
		this.target = target;
	}

	@Override
	public MapLocation getTarget() {
		return target;
	}

	@Override
	public boolean isTargetReached() {
		return rc.getLocation().equals(target);
	}
}
