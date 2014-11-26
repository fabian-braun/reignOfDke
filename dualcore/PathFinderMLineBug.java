package dualcore;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class PathFinderMLineBug extends PathFinder {

	private Direction lastDir = Direction.NONE;
	private int minDistance = Integer.MAX_VALUE;
	private RobotController rc;
	private MapLocation target;
	private boolean obstacleMode = false;
	private Set<MapLocation> mTiles = new HashSet<MapLocation>();
	private Set<MapLocation> visited = new HashSet<MapLocation>();
	private static final List<Direction> prioDirClockwise = Arrays
			.asList(new Direction[] { Direction.NORTH, Direction.NORTH_EAST,
					Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH,
					Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST });
	private static final List<Direction> prioDirCounterClockwise = Arrays
			.asList(new Direction[] { Direction.NORTH, Direction.NORTH_WEST,
					Direction.WEST, Direction.SOUTH_WEST, Direction.SOUTH,
					Direction.SOUTH_EAST, Direction.EAST, Direction.NORTH_EAST });
	private List<Direction> currentTurnDirection = prioDirClockwise;

	public PathFinderMLineBug(RobotController rc) {
		super(rc);
		this.rc = rc;
		// init target to middle of map
		setTarget(new MapLocation(width / 2, height / 2));
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
		minDistance = PathFinder.distance(rc.getLocation(), target);
		mTiles.clear();
		visited.clear();
		MapLocation current = rc.getLocation();
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
		int nextDirToTry = (currentTurnDirection.indexOf(lastDir) + 8 - 2) % 8;
		while (!isWalkable(rc.getLocation(),
				currentTurnDirection.get(nextDirToTry))) {
			nextDirToTry++;
			nextDirToTry %= 8;
		}
		return currentTurnDirection.get(nextDirToTry);
	}

	private void decideTurnDirection() {
		if (rc.getRobot().getID() % 2 < 1) {
			currentTurnDirection = prioDirClockwise;
		} else {
			currentTurnDirection = prioDirCounterClockwise;
		}
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
					&& PathFinder.distance(current, target) < minDistance) {
				minDistance = PathFinder.distance(rc.getLocation(), target);
				// System.out.println("mLine found at " + current);
				obstacleMode = false;
				moveTo = getNextOnMLine();
			} else {
				moveTo = getNextAroundObstacle();
			}
		} else { // move on mLine
			moveTo = getNextOnMLine();
			rc.senseTerrainTile(current.add(moveTo));
			if (!isWalkable(current, moveTo)) {
				// System.out.println("mLine left at " + rc.getLocation());
				obstacleMode = true;
				decideTurnDirection();
				lastDir = currentTurnDirection.get((currentTurnDirection
						.indexOf(moveTo) + 2) % 8);
				moveTo = getNextAroundObstacle();
			}
		}
		return moveTo;
	}

	private boolean isWalkable(MapLocation current, Direction into) {
		TerrainTile target = rc.senseTerrainTile(current.add(into));
		return TerrainTile.NORMAL.equals(target)
				|| TerrainTile.ROAD.equals(target);
	}

	@Override
	public boolean move() throws GameActionException {
		MapLocation current = rc.getLocation();
		if (visited.contains(current)) {
			updateMLine();
		}
		visited.add(current);
		Direction moveTo = getNextDirection();
		if (rc.canMove(moveTo)) {
			lastDir = moveTo;
			rc.move(moveTo);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean sneak() throws GameActionException {
		Direction moveTo = getNextDirection();
		if (rc.canMove(moveTo)) {
			lastDir = moveTo;
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
}
