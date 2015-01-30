package reignOfDKE;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class PathFinderMLineBug extends PathFinder {

	private Direction lastDir = Direction.NONE;
	private int minDistance = Integer.MAX_VALUE;
	private MapLocation target = new MapLocation(-1, -1);
	private boolean obstacleMode = false;
	private MapLocationSet mTiles;
	private MapLocation current;

	public PathFinderMLineBug(RobotController rc, int soldierId) {
		super(rc, soldierId);
		current = rc.getLocation();
		mTiles = new MapLocationSet(ySize * xSize);
	}

	public PathFinderMLineBug(RobotController rc, TerrainTile[][] map,
			MapLocation hqSelfLoc, MapLocation hqEnemLoc, int ySize, int xSize,
			int soldierId) {
		super(rc, map, hqSelfLoc, hqEnemLoc, ySize, xSize, soldierId);
		current = rc.getLocation();
		mTiles = new MapLocationSet(ySize * xSize);
	}

	@Override
	public void setTarget(MapLocation target) {
		if (target.equals(this.target)) {
			return;
		}
		this.target = target;
		updateMLine();
	}

	private void updateMLine() {
		obstacleMode = false;
		mTiles.clear();
		MapLocation current = rc.getLocation();
		minDistance = PathFinder.getRequiredMoves(current, target);
		MapLocation temp = new MapLocation(current.x, current.y);
		while (!temp.equals(target)) {
			mTiles.add(temp);
			// awesome trick:
			mTiles.add(new MapLocation(temp.x + 1, temp.y));
			temp = temp.add(temp.directionTo(target));
		}
		mTiles.add(target);
	}

	private Direction getNextAroundObstacle() {
		Direction nextDirToTry;
		nextDirToTry = lastDir.rotateRight().rotateRight();
		while (!isTraversableAndNotHq(rc.getLocation().add(nextDirToTry))) {
			nextDirToTry = nextDirToTry.rotateLeft();
		}
		return nextDirToTry;
	}

	private Direction getNextOnMLine() {
		return rc.getLocation().directionTo(target);
	}

	public Direction getNextDirection() {
		MapLocation current = rc.getLocation();
		Direction moveTo;
		if (obstacleMode) { // move around obstacle
			// check if mLine reached again
			if (mTiles.contains(current)
					&& PathFinder.getRequiredMoves(current, target) < minDistance) {
				obstacleMode = false;
				moveTo = getNextOnMLine();
				minDistance = PathFinder.getRequiredMoves(current, target);
			} else {
				moveTo = getNextAroundObstacle();
				lastDir = moveTo;
			}
		} else { // move on mLine
			moveTo = getNextOnMLine();
			if (!isTraversableAndNotHq(current.add(moveTo))) {
				obstacleMode = true;
				lastDir = moveTo.rotateLeft().rotateLeft();
				moveTo = getNextAroundObstacle();
				lastDir = moveTo;
			} else {
				minDistance = PathFinder.getRequiredMoves(current.add(moveTo),
						target);
			}
		}
		return moveTo;
	}

	@Override
	public boolean move() throws GameActionException {
		if (!current.equals(rc.getLocation())) {
			// moves have been performed outside of this class
			// therefore mline is not valid anymore
			updateMLine();
		}
		Direction moveTo = getNextDirection();
		if (rc.canMove(moveTo)) {
			rc.move(moveTo);
			current = rc.getLocation();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean sneak() throws GameActionException {
		Direction moveTo = getNextDirection();
		if (rc.canMove(moveTo)) {
			rc.sneak(moveTo);
			return true;
		} else {
			return false;
		}
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
