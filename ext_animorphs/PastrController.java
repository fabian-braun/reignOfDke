package ext_animorphs;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class PastrController extends Controller {
	public CommandMessager localCommander;
	public Sensor sensor;

	public final static int numDirs = Direction.values().length;
	public final Direction[] indexToDirection = Direction.values();
	public MapLocation myLoc;
	public final int ID_OFFSET_FROM_LOCAL = CommsProtocol.ID_OFFSET_FROM_LOCAL;
	public final int ID_OFFSET_TO_LOCAL = CommsProtocol.ID_OFFSET_TO_LOCAL;

	public Robot[] allies;
	public RobotInfo[] allyInfos;
	public MapLocation[] toLocs;
	public FastSmallIntSet myIDs;
	public Direction reserveDir; // Direction to reserve a guy
	public MapLocation[] validToLocs; // non null values
	public int numToLocs; // track number of valid to locs
	public boolean hasNoiseTower;

	public int sneakCounter; // Track pulses of sneak counter

	public final int SNEAK_PULSE_LENGTH = 6;
	public final int SNEAK_CYCLE_LENGTH = 20;

	public PastrController(RobotController rc) {
		super(rc);
		localCommander = new CommandMessager(rc);
		myIDs = new FastSmallIntSet();
		sensor = new Sensor(this);
		toLocs = new MapLocation[numDirs];

		myLoc = rc.getLocation();
		sneakCounter = 0;

	}

	public void init() throws GameActionException {
		sensor.init();
		toLocs = new MapLocation[numDirs];
		myIDs.clear();
		sensor.initAllAllyInfo();
		allies = sensor.getBots(Sensor.ALL_ALLIES);
		allyInfos = sensor.info.get(allies);

		hasNoiseTower = false;
		RobotInfo[] nearby = sensor.info.get(sensor
				.getBots(Sensor.SENSE_RANGE_ALLIES));
		for (int i = nearby.length; --i >= 0;) {
			RobotInfo info = nearby[i];
			if (info.type == RobotType.NOISETOWER) {
				hasNoiseTower = true;
			}
		}

		reserveDir = null;
		validToLocs = new MapLocation[numDirs];
		numToLocs = 0;
	}

	public void read() throws GameActionException {
		for (int i = allies.length; --i >= 0;) {
			if (checkMine(allies[i])) {
				int id = allies[i].getID();
				// CLEAR CHANNEL
				if (localCommander.readIDChannel(id, ID_OFFSET_FROM_LOCAL)) {
					if (localCommander.lastMsg == MessageType.RETREAT) {
						RobotInfo info = sensor.getInfo(allies[i]);
						if (info.location.distanceSquaredTo(myLoc) <= 4) {
							localCommander.clearIDChannel(id,
									ID_OFFSET_FROM_LOCAL);
						} else {
							localCommander.broadcastIDChannel(id,
									ID_OFFSET_TO_LOCAL,
									MessageType.CANT_FORWARD, myLoc);
						}
					} else {
						localCommander.clearIDChannel(id, ID_OFFSET_FROM_LOCAL);
					}
				}
				// check message from only bots defending me
				if (localCommander.readIDChannel(id, ID_OFFSET_TO_LOCAL)) {
					// sees enemy, add its location to list
					if (localCommander.lastMsg == MessageType.ENGAGING_ENEMY
							|| localCommander.lastMsg == MessageType.NEARBY_ENEMY) {
						MapLocation loc = localCommander.lastLoc;
						int index = myLoc.directionTo(loc).ordinal();
						if (toLocs[index] == null
								|| toLocs[index].distanceSquaredTo(myLoc) > loc
										.distanceSquaredTo(myLoc)) {
							// remember loc, if its closest so far in that
							// direction
							toLocs[index] = loc;
						}
						localCommander.clearIDChannel(id, ID_OFFSET_TO_LOCAL);
						continue;
					}
					if (localCommander.lastMsg == MessageType.CANT_FORWARD) {
						localCommander.broadcastIDChannel(id,
								ID_OFFSET_FROM_LOCAL, MessageType.RETREAT,
								myLoc);
						continue;
					}
				}
				// sees nothing, free to command. Add id to pool
				myIDs.add(id);
			}
		}
	}

	public void process() {
		// get whether we have action opposite of action;
		for (int i = numDirs; --i >= 0;) {
			if (toLocs[i] != null) {
				validToLocs[numToLocs] = toLocs[i];
				numToLocs++;
				if (reserveDir == null) {
					// haven't reserved a direction. Check.
					Direction dir = indexToDirection[i].opposite();
					if (toLocs[dir.ordinal()] == null
							&& toLocs[dir.rotateLeft().ordinal()] == null
							&& toLocs[dir.rotateRight().ordinal()] == null) {
						reserveDir = dir;
					}
				}
			}
		}
	}

	public void broadcast() throws GameActionException {
		if (myIDs.size == 0) {
			// no available
			return;
		}
		if (numToLocs == 0) {
			// no available.
			if (!hasNoiseTower) {
				// SNEAK
				sneakCounter = (sneakCounter + 1) % SNEAK_CYCLE_LENGTH;
				rc.setIndicatorString(1, "sneak: " + sneakCounter);
				if (sneakCounter <= SNEAK_PULSE_LENGTH) {
					for (int i = allies.length; --i >= 0;) {
						int id = allies[i].getID();
						if (myIDs.contains(id)) {
							localCommander.broadcastIDChannel(id,
									ID_OFFSET_FROM_LOCAL,
									MessageType.SNEAK_OUT, myLoc);
						}
					}
				}
			}
			return;
		}
		// do we have to reserve a direction?
		sneakCounter = 0; // reset sneak
		boolean reserved = reserveDir == null;
		int maxReserveDist = 0;
		int reserveID = -1;

		for (int i = allies.length; --i >= 0;) {
			int id = allies[i].getID();
			if (myIDs.contains(id)) {
				// was idle
				MapLocation loc = allyInfos[i].location;
				if (reserveID < 0) {
					reserveID = id;
				}

				int minDist = diagonalSq;
				MapLocation bestLoc = myLoc;
				// Loop through available directions and pick closest
				for (int j = 0; j < numToLocs; j++) {
					MapLocation temp = validToLocs[j];
					int dist = loc.distanceSquaredTo(temp);
					if (dist < minDist) {
						minDist = dist;
						bestLoc = temp;
					}
				}

				localCommander.broadcastIDChannel(id, ID_OFFSET_FROM_LOCAL,
						MessageType.REPOSITION, bestLoc);

				if (!reserved && myLoc.directionTo(loc) == reserveDir) {
					// Reserve farthest unit in the reserve direction
					int dist = myLoc.distanceSquaredTo(loc);
					if (dist > maxReserveDist) {
						maxReserveDist = dist;
						reserveID = id;
					}
				}
			}
		}
		if (!reserved) {
			MapLocation center;
			if (reserveDir.isDiagonal()) {
				center = myLoc.add(reserveDir);
			} else {
				center = myLoc.add(reserveDir, 2);
			}
			// Check for empty locations (in attack range of pasture in this
			// direction)
			// DESIRED LOC
			TerrainTile t = rc.senseTerrainTile(center);
			if (t != TerrainTile.VOID && t != TerrainTile.OFF_MAP) {
				localCommander.broadcastIDChannel(reserveID,
						ID_OFFSET_FROM_LOCAL, MessageType.REPOSITION, center);
				return;
			}
			// ONE STEP OUT FROM DESIRED LOC
			MapLocation out = center.add(reserveDir);
			TerrainTile o = rc.senseTerrainTile(out);
			if (o != TerrainTile.VOID && t != TerrainTile.OFF_MAP) {
				localCommander.broadcastIDChannel(reserveID,
						ID_OFFSET_FROM_LOCAL, MessageType.REPOSITION, out);
				return;
			}
			// TO SIDES OF ONE STEP OUT
			Direction dirRight = reserveDir.rotateRight();
			Direction dirLeft = reserveDir.rotateLeft();
			MapLocation right = center.add(dirRight);
			MapLocation left = center.add(dirLeft);
			t = rc.senseTerrainTile(left);
			if (t != TerrainTile.VOID && t != TerrainTile.OFF_MAP) {
				localCommander.broadcastIDChannel(reserveID,
						ID_OFFSET_FROM_LOCAL, MessageType.REPOSITION, left);
				return;
			}
			t = rc.senseTerrainTile(right);
			if (t != TerrainTile.VOID && t != TerrainTile.OFF_MAP) {
				localCommander.broadcastIDChannel(reserveID,
						ID_OFFSET_FROM_LOCAL, MessageType.REPOSITION, right);
				return;
			}
			// SIDES OF DESIRED LOC
			dirRight = dirRight.rotateRight();
			dirLeft = dirLeft.rotateLeft();
			right = center.add(dirRight);
			left = center.add(dirLeft);
			t = rc.senseTerrainTile(left);
			if (t != TerrainTile.VOID && t != TerrainTile.OFF_MAP) {
				localCommander.broadcastIDChannel(reserveID,
						ID_OFFSET_FROM_LOCAL, MessageType.REPOSITION, left);
				return;
			}
			if (t != TerrainTile.VOID && t != TerrainTile.OFF_MAP) {
				localCommander.broadcastIDChannel(reserveID,
						ID_OFFSET_FROM_LOCAL, MessageType.REPOSITION, right);
				return;
			}
			// SIDES INWARD OF DESIRED LOC
			dirRight = dirRight.rotateRight();
			dirLeft = dirLeft.rotateLeft();
			right = center.add(dirRight);
			left = center.add(dirLeft);
			t = rc.senseTerrainTile(left);
			if (t == TerrainTile.OFF_MAP) {
				return;
			}
			if (t != TerrainTile.VOID) {
				localCommander.broadcastIDChannel(reserveID,
						ID_OFFSET_FROM_LOCAL, MessageType.REPOSITION, left);
				return;
			}
			t = rc.senseTerrainTile(right);
			if (t == TerrainTile.OFF_MAP) {
				return;
			}
			if (t != TerrainTile.VOID) {
				localCommander.broadcastIDChannel(reserveID,
						ID_OFFSET_FROM_LOCAL, MessageType.REPOSITION, right);
				return;
			}

			// fuck it, just come to pasture
			localCommander.broadcastIDChannel(reserveID, ID_OFFSET_FROM_LOCAL,
					MessageType.REPOSITION, myLoc);
		}
	}

	// Check if a bot is defending this pasture
	public boolean checkMine(Robot r) throws GameActionException {
		// check if this bot is defending me
		if (localCommander.readIDChannel(r.getID(),
				CommsProtocol.ID_OFFSET_FROM_HQ)) {
			return localCommander.lastMsg == MessageType.DEFEND
					&& localCommander.lastLoc.distanceSquaredTo(myLoc) < 5;
		}
		return false;
	}

	public void run() throws GameActionException {
		init();
		read();
		process();
		broadcast();
	}
}
