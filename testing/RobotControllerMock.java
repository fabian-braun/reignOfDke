package testing;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TerrainTile;

public class RobotControllerMock implements RobotController {

	private TerrainTile[][] map;
	private MapLocation[] hqs;
	private MapLocation currLoc;

	public RobotControllerMock(TerrainTile[][] map, MapLocation[] hqs,
			MapLocation currLoc) {
		this.map = map;
		this.hqs = hqs;
		this.currLoc = currLoc;
	}

	@Override
	public void addMatchObservation(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void attackSquare(MapLocation arg0) throws GameActionException {
		// TODO Auto-generated method stub

	}

	@Override
	public void attackSquareLight(MapLocation arg0) throws GameActionException {
		// TODO Auto-generated method stub

	}

	@Override
	public void breakpoint() {
		// TODO Auto-generated method stub

	}

	@Override
	public void broadcast(int arg0, int arg1) throws GameActionException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean canAttackSquare(MapLocation arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canMove(Direction arg0) {
		return true;
	}

	@Override
	public boolean canSenseObject(GameObject arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canSenseSquare(MapLocation arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void construct(RobotType arg0) throws GameActionException {
		// TODO Auto-generated method stub

	}

	@Override
	public double getActionDelay() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getConstructingRounds() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public RobotType getConstructingType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getControlBits() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getHealth() {
		return 100;
	}

	@Override
	public MapLocation getLocation() {
		return currLoc;
	}

	@Override
	public int getMapHeight() {
		return map.length;
	}

	@Override
	public int getMapWidth() {
		return map[0].length;
	}

	@Override
	public Robot getRobot() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Team getTeam() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long[] getTeamMemory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RobotType getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isActive() {
		return true;
	}

	@Override
	public boolean isConstructing() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void move(Direction arg0) throws GameActionException {
		MapLocation newLoc = currLoc.add(arg0);
		System.out.println("MOVE [" + currLoc.y + "," + currLoc.x + "]" + "["
				+ newLoc.y + "," + newLoc.x + "]");
		currLoc = newLoc;
	}

	@Override
	public int readBroadcast(int arg0) throws GameActionException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void resign() {
		// TODO Auto-generated method stub

	}

	@Override
	public int roundsUntilActive() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void selfDestruct() throws GameActionException {
		// TODO Auto-generated method stub

	}

	@Override
	public MapLocation[] senseBroadcastingRobotLocations() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MapLocation[] senseBroadcastingRobotLocations(Team arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Robot[] senseBroadcastingRobots() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Robot[] senseBroadcastingRobots(Team arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double[][] senseCowGrowth() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double senseCowsAtLocation(MapLocation arg0)
			throws GameActionException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public MapLocation senseEnemyHQLocation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MapLocation senseHQLocation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MapLocation senseLocationOf(GameObject arg0)
			throws GameActionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends GameObject> T[] senseNearbyGameObjects(Class<T> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends GameObject> T[] senseNearbyGameObjects(Class<T> arg0,
			int arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends GameObject> T[] senseNearbyGameObjects(Class<T> arg0,
			int arg1, Team arg2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends GameObject> T[] senseNearbyGameObjects(Class<T> arg0,
			MapLocation arg1, int arg2, Team arg3) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GameObject senseObjectAtLocation(MapLocation arg0)
			throws GameActionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MapLocation[] sensePastrLocations(Team arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int senseRobotCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public RobotInfo senseRobotInfo(Robot arg0) throws GameActionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double senseTeamMilkQuantity(Team arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public TerrainTile senseTerrainTile(MapLocation arg0) {
		return map[arg0.y][arg0.x];
	}

	@Override
	public void setIndicatorString(int arg0, String arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setTeamMemory(int arg0, long arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setTeamMemory(int arg0, long arg1, long arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sneak(Direction arg0) throws GameActionException {
		currLoc = currLoc.add(arg0);

	}

	@Override
	public void spawn(Direction arg0) throws GameActionException {
		// TODO Auto-generated method stub

	}

	@Override
	public void wearHat() throws GameActionException {
		// TODO Auto-generated method stub

	}

	@Override
	public void win() {
		// TODO Auto-generated method stub

	}

	@Override
	public void yield() {
		// TODO Auto-generated method stub

	}

}
