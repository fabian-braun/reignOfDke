package simplePastr;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class PathFinderMLineBug {

	private Direction lastDir = Direction.NONE;
	private int minDistance = Integer.MAX_VALUE;
	private RobotController rc;
	private MapLocation target;
	private int width;
	private int height;
	private boolean obstacleMode = false;
	private Set<MapLocation> mTiles = new HashSet<MapLocation>();
	private static final List<Direction> prioDirClockwise = Arrays
			.asList(new Direction[] { Direction.NORTH, Direction.NORTH_EAST,
					Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH,
					Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST });

	public PathFinderMLineBug(RobotController rc) {
		this.rc = rc;
		this.width = rc.getMapWidth();
		this.height = rc.getMapHeight();
		// init target to middle of map
		setTarget(rc.getLocation(), new MapLocation(width / 2, height / 2));
	}

	public void setTarget(MapLocation current, MapLocation target) {
		obstacleMode = false;
		this.target = target;
		minDistance = Integer.MAX_VALUE;
		updateMLine();
	}

	public void move() throws GameActionException {
		if (obstacleMode) { // move around obstacle
			int nextDirToTry = (prioDirClockwise.indexOf(lastDir) + 8 - 2) % 8;
			while (!rc.canMove(prioDirClockwise.get(nextDirToTry))) {
				nextDirToTry++;
				nextDirToTry %= 8;
			}
			lastDir = prioDirClockwise.get(nextDirToTry);
			rc.move(prioDirClockwise.get(nextDirToTry));
			MapLocation current = rc.getLocation();
			// check if mLine reached again
			if (mTiles.contains(current)
					&& PathFinder.distance(current, target) < minDistance) {
				System.out.println("mLine found at " + current);
				obstacleMode = false;
			}
		} else { // move on mLine
			// TODO: bytecode check and maybe yield
			Direction moveTo = rc.getLocation().directionTo(target);
			if (rc.canMove(moveTo)) {
				rc.move(moveTo);
			} else {
				System.out.println("mLine left at " + rc.getLocation());
				minDistance = PathFinder.distance(rc.getLocation(), target);
				obstacleMode = true;
				lastDir = prioDirClockwise.get((prioDirClockwise
						.indexOf(moveTo) + 2) % 8);
			}
		}
	}

	private void updateMLine() {
		mTiles.clear();
		MapLocation current = rc.getLocation();
		MapLocation temp = new MapLocation(current.x, current.y);
		while (!temp.equals(target)) {
			mTiles.add(temp);
			temp = temp.add(temp.directionTo(target));
			System.out.println(temp);
		}
		mTiles.add(target);
	}
}
