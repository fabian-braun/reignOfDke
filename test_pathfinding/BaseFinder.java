package test_pathfinding;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

public abstract class BaseFinder {

	public abstract boolean move() throws GameActionException;

	public abstract boolean sneak() throws GameActionException;

	public abstract void setTarget(MapLocation target);

}
