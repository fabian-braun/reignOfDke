package ext_animorphs;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class ObjectiveManager {
	public Controller c;
	public RobotController rc;
	public Sensor sensor;
	public CommandMessager globalCmd;

	public PastrFinder finder;
	public SimplePastrFinder simpleFinder;
	public BFS bfs;
	public boolean bfsDone;
	public boolean pastrCalcDone;

	public int pastureNum;
	public int waitNum;

	public ObjectiveThread disruptor;
	public ObjectiveThread herder;

	public ObjectiveThread pasture;
	public RobotInfo[] infos;

	public static final int STICK_PASTURE_RAD_SQ = 100;

	public ObjectiveManager(Controller c, Sensor sensor,
			CommandMessager globalCmd) throws GameActionException {
		this.c = c;
		this.rc = c.rc;
		this.sensor = sensor;
		this.globalCmd = globalCmd;

		this.simpleFinder = new SimplePastrFinder(c);
		this.finder = new PastrFinder(c);
		this.pastureNum = 0;

		// disruptor

		// pasture
		this.pasture = new ObjectiveThread();
		this.herder = new ObjectiveThread();
		this.disruptor = new ObjectiveThread();

		// bfs
		this.bfs = new BFS(c);
		this.bfsDone = false;
		this.pastrCalcDone = false;
		calcWaitNum();
		// bfs.add(c.teamhq);
		// bfs.add(c.enemyhq);
	}

	public void calcWaitNum() {
		waitNum = 5; // constant
		waitNum += (200 - c.height - c.width) * 200 / 200; // map size
		waitNum += (20000 - (c.enemyhq.distanceSquaredTo(c.teamhq))) * 100 / 20000; // diagonalsq
	}

	public void loadInfo() throws GameActionException {
		Robot[] bots = sensor.getBots(Sensor.ALL_ALLIES);
		infos = new RobotInfo[bots.length];

		// localize
		RobotInfo[] infos = this.infos;

		for (int i = 0; i < bots.length; i++) {
			infos[i] = rc.senseRobotInfo(bots[i]);
		}
	}

	// Allocates calculations
	public void calc(int bytecode) throws GameActionException {
		int round = Clock.getRoundNum();

		// Search 1/4 the map
		round -= c.height * c.width / 90 / 2;
		if (round < 0) {
			finder.calc(bytecode);
			return;
		}
		if (round == 0) {
			pastrCalcDone = true;
		}

		if (!bfsDone) {
			bfsDone = bfs.calc(bytecode);
		}
		rc.setIndicatorString(1, bfs.queueEnd + "");
	}

	public void resetBFSAssault() throws GameActionException {
		waitNum = 0;
		MapLocation[] ep = sensor.getLocs(Sensor.ENEMY_PASTR_LOCS);
		bfs = new BFS(c);

		for (int i = ep.length - 1; i >= 0; i--) {
			bfs.add(ep[i]);
		}

		bfsDone = false;
	}

	public void resetBFSDefend() throws GameActionException {
		MapLocation[] ap = sensor.getLocs(Sensor.ALLIED_PASTR_LOCS);
		bfs = new BFS(c);
		for (int i = ap.length - 1; i >= 0; i--) {
			bfs.add(ap[i]);
		}
		// add pastures that aren't done
		if (!pasture.done && pasture.running && pasture.loc != null) {
			bfs.add(pasture.loc);
		} else {
			MapLocation rec = finder.pastrRecs[pastureNum
					% PastrFinder.PASTR_NUM];
			if (rec != null) {
				bfs.add(rec);
			}
		}

		bfsDone = false;
	}

	public void readPasture() throws GameActionException {
		if (pasture.done) {
			Robot[] target = rc.senseNearbyGameObjects(Robot.class,
					pasture.loc, 0, c.team);

			if (target.length == 0
					|| rc.senseRobotInfo(target[0]).type != RobotType.PASTR) {
				pasture = new ObjectiveThread();
				pastureNum++;
			}
			return;
		}

		// localize
		ObjectiveThread pasture = this.pasture;
		int offset = CommsProtocol.ID_OFFSET_TO_HQ;

		if (!pasture.running) {
			return;
		}

		int id = pasture.id;
		if (!globalCmd.readIDChannel(id, offset)) {
			// Track, if found, refresh, else see if pasture
			Robot[] teamBots = sensor.getBots(Sensor.ALL_ALLIES);
			for (int i = 0; i < teamBots.length; i++) {
				Robot bot = teamBots[i];

				if (bot.getID() == id) {
					RobotInfo info = infos[i];
					pasture.lastLoc = info.location;
					if (info.isConstructing) {
						pasture.constructing = true;
						pasture.loc = pasture.lastLoc;
					}
					return;
				}
			}

			Robot[] target = rc.senseNearbyGameObjects(Robot.class,
					pasture.lastLoc, 0, c.team);

			if (target.length == 0
					|| rc.senseRobotInfo(target[0]).type != RobotType.PASTR) {
				// find new
				pastureNum++;
				pasture.running = false;
				return;
			}

			// pasture made
			pasture.loc = pasture.lastLoc;
			pasture.done = true;
			return;
		}

		int offsetFrom = CommsProtocol.ID_OFFSET_FROM_HQ;

		switch (globalCmd.lastMsg) {
		case INTERCEPTED_BY_ENEMY:
		case LOCATION_HAS_ENEMY:
		case PROBLEM_BUGGING:
			// find new
			pastureNum++;
			pasture.running = false;
			globalCmd.clearIDChannel(id, offsetFrom);
			return;
		default:
			globalCmd.clearIDChannel(id, offset);
			return;
		}
	}

	public void broadcastPasture() throws GameActionException {
		if (!pastrCalcDone || Clock.getRoundNum() <= waitNum) {
			return; // still calcing
		}

		if (pasture.done || pasture.running) {
			return;
		}

		// localize
		ObjectiveThread pasture = this.pasture;
		Robot[] teamBots = sensor.getBots(Sensor.ALL_ALLIES);
		int offsetFrom = CommsProtocol.ID_OFFSET_FROM_HQ;
		int offsetTo = CommsProtocol.ID_OFFSET_TO_HQ;

		// find pasture
		MapLocation rec = finder.pastrRecs[pastureNum % PastrFinder.PASTR_NUM];
		while (rec == null && pastureNum < PastrFinder.PASTR_NUM) {
			pastureNum++;
			rec = finder.pastrRecs[pastureNum % PastrFinder.PASTR_NUM];
		}
		pastureNum = pastureNum % PastrFinder.PASTR_NUM;
		if (rec != null) {
			pasture.loc = rec;
		}

		int minDistSq = c.diagonalSq;

		// find bot
		for (int i = teamBots.length; --i >= 0;) {
			Robot bot = teamBots[i];
			RobotInfo info = infos[i];
			int id = bot.getID();
			if (info.type == RobotType.SOLDIER) {
				if (globalCmd.readIDChannel(id, offsetTo)) {
					if (globalCmd.lastMsg == MessageType.NEARBY_ENEMY) {
						continue;
					}
				}

				int dist = info.location.distanceSquaredTo(pasture.loc);
				if (dist < minDistSq) {
					minDistSq = dist;
					pasture.id = id;
					pasture.running = true;
					pasture.lastLoc = info.location;
				}

			}
		}

		if (pasture.running) {
			globalCmd.broadcastIDChannel(pasture.id, offsetFrom,
					MessageType.MAKE_PASTR, pasture.loc);
			this.resetBFSDefend();
		}
	}

	public void clearChannels() throws GameActionException {
		// localize
		Robot[] teamBots = sensor.getBots(Sensor.ALL_ALLIES);
		int offsetTo = CommsProtocol.ID_OFFSET_TO_HQ;

		for (int i = teamBots.length; --i >= 0;) {
			Robot bot = teamBots[i];
			globalCmd.clearIDChannel(bot.getID(), offsetTo);
		}
	}

	public void broadcastAssault() throws GameActionException {
		MapLocation[] pastrs = sensor.getLocs(Sensor.ENEMY_PASTR_LOCS);

		Robot[] bots = sensor.getBots(Sensor.ALL_ALLIES);
		MapLocation enemyhq = c.enemyhq;

		int offsetFrom = CommsProtocol.ID_OFFSET_FROM_HQ;
		// find bot
		for (int i = bots.length; --i >= 0;) {
			Robot bot = bots[i];
			RobotInfo info = infos[i];
			int id = bot.getID();
			if (info.type != RobotType.SOLDIER) {
				continue;
			}
			MapLocation loc = null;
			for (MapLocation p : pastrs) {
				if (p.distanceSquaredTo(enemyhq) <= 5) {
					continue;
				}

				if (p.distanceSquaredTo(info.location) <= STICK_PASTURE_RAD_SQ) {
					loc = p;
					break;
				}
			}
			if (loc == null) {
				if (pastrs.length > 0) {
					loc = pastrs[id % pastrs.length];
				} else {
					loc = c.enemyhq;
				}
			}
			globalCmd.broadcastIDChannel(id, offsetFrom, MessageType.ASSAULT,
					loc);
		}
	}

	public void broadcastDefend() throws GameActionException {
		if (pasture.constructing && pasture.running) {
			int offsetFrom = CommsProtocol.ID_OFFSET_FROM_HQ;
			Robot[] bots = sensor.getBots(Sensor.ALL_ALLIES);
			for (Robot bot : bots) {
				globalCmd.broadcastIDChannel(bot.getID(), offsetFrom,
						MessageType.DEFEND, pasture.loc);
			}
			return;
		}
		MapLocation rec = finder.pastrRecs[pastureNum % PastrFinder.PASTR_NUM];
		if (rec != null) {
			int offsetFrom = CommsProtocol.ID_OFFSET_FROM_HQ;
			Robot[] bots = sensor.getBots(Sensor.ALL_ALLIES);
			for (Robot bot : bots) {
				globalCmd.broadcastIDChannel(bot.getID(), offsetFrom,
						MessageType.DEFEND, rec);
			}
		}
	}

	public void readHerder() throws GameActionException {
		if (herder.done) {
			Robot[] target = rc.senseNearbyGameObjects(Robot.class,
					herder.lastLoc, 0, c.team);

			if (target.length == 0
					|| rc.senseRobotInfo(target[0]).type != RobotType.NOISETOWER) {
				herder = new ObjectiveThread();
			}
			return;
		}

		// localize
		ObjectiveThread herder = this.herder;
		int offset = CommsProtocol.ID_OFFSET_TO_HQ;

		if (!herder.running) {
			return;
		}

		int id = herder.id;
		if (!globalCmd.readIDChannel(id, offset)) {
			// Track, if found, refresh, else see if herder
			Robot[] teamBots = sensor.getBots(Sensor.ALL_ALLIES);
			for (int i = 0; i < teamBots.length; i++) {
				Robot bot = teamBots[i];

				if (bot.getID() == id) {
					RobotInfo info = infos[i];
					herder.lastLoc = info.location;
					if (info.isConstructing) {
						herder.constructing = true;
					}
					return;
				}
			}

			Robot[] target = rc.senseNearbyGameObjects(Robot.class,
					herder.lastLoc, 0, c.team);

			if (target.length == 0
					|| rc.senseRobotInfo(target[0]).type != RobotType.NOISETOWER) {
				// find new
				herder.running = false;
				return;
			}

			// herder made, update id
			herder.id = target[0].getID();
			herder.made = true;
			return;
		}

		int offsetFrom = CommsProtocol.ID_OFFSET_FROM_HQ;
		switch (globalCmd.lastMsg) {
		case INTERCEPTED_BY_ENEMY:
		case LOCATION_HAS_ENEMY:
		case PROBLEM_BUGGING:
			herder.running = false;
			globalCmd.clearIDChannel(id, offsetFrom);
			return;
		default:
			return;
		}
	}

	public void broadcastHerder() throws GameActionException {
		if (!pastrCalcDone || Clock.getRoundNum() <= waitNum) {
			return; // still calcing or waiting
		}

		if (herder.done) {
			int offset = CommsProtocol.ID_OFFSET_FROM_HQ;
			globalCmd.broadcastIDChannel(herder.id, offset,
					MessageType.TOWER_HERD, herder.loc);
			return;
		}

		if (herder.running) {
			if (herder.made) {
				int offset = CommsProtocol.ID_OFFSET_FROM_HQ;
				globalCmd.broadcastIDChannel(herder.id, offset,
						MessageType.TOWER_HERD, herder.loc);
				System.out.println(herder.id + ", " + herder.loc);
				herder.done = true;
			}
			return;
		}

		// localize
		ObjectiveThread herder = this.herder;
		Robot[] teamBots = sensor.getBots(Sensor.ALL_ALLIES);
		int offsetFrom = CommsProtocol.ID_OFFSET_FROM_HQ;
		int offsetTo = CommsProtocol.ID_OFFSET_TO_HQ;

		// find pasture to place herder
		MapLocation[] pastrs = sensor.getLocs(Sensor.ALLIED_PASTR_LOCS);
		if (pasture.done) {
			if (pastrs.length == 0) {
				return;
			}

			herder.loc = pastrs[0];
		} else if (pasture.running && pasture.constructing
				&& pasture.loc != null) {
			herder.loc = pasture.loc;
		} else {
			return;
		}

		int minDistSq = c.diagonalSq;

		// find bot
		for (int i = teamBots.length; --i >= 0;) {
			Robot bot = teamBots[i];
			RobotInfo info = infos[i];
			int id = bot.getID();
			if (id == pasture.id) {
				continue;
			}
			if (info.type == RobotType.SOLDIER) {
				if (globalCmd.readIDChannel(id, offsetTo)) {
					if (globalCmd.lastMsg == MessageType.NEARBY_ENEMY) {
						continue;
					}
				}

				int dist = info.location.distanceSquaredTo(herder.loc);
				if (dist < minDistSq) {
					minDistSq = dist;
					herder.id = id;
					herder.running = true;
					herder.lastLoc = info.location;
				}

			}
		}

		if (herder.running) {
			globalCmd.broadcastIDChannel(herder.id, offsetFrom,
					MessageType.MAKE_TOWER_HERDER, herder.loc);
		}
	}

	public void readDisruptor() {
		if (disruptor.done) {
			return;
		}

		MapLocation[] locs = sensor.getLocs(Sensor.ENEMY_PASTR_LOCS);
		if (locs.length > 0) {
			disruptor.loc = locs[0];
		}
	}

	public void broadcastDisruptor() throws GameActionException {
		if (disruptor.done || disruptor.loc == null) {
			return;
		}

		if (!disruptor.on) {
			return;
		}

		// localize
		ObjectiveThread disruptor = this.disruptor;
		Robot[] teamBots = sensor.getBots(Sensor.ALL_ALLIES);
		int offsetFrom = CommsProtocol.ID_OFFSET_FROM_HQ;
		int offsetTo = CommsProtocol.ID_OFFSET_TO_HQ;

		int maxDistSq = 0;

		// find bot
		for (int i = teamBots.length; --i >= 0;) {
			Robot bot = teamBots[i];
			RobotInfo info = infos[i];
			int id = bot.getID();
			if (id == pasture.id || id == herder.id) {
				continue;
			}
			if (info.type == RobotType.SOLDIER) {
				if (globalCmd.readIDChannel(id, offsetTo)) {
					if (globalCmd.lastMsg == MessageType.NEARBY_ENEMY) {
						continue;
					}
				}

				int dist = info.location.distanceSquaredTo(disruptor.loc);
				if (dist < 90 && dist >= maxDistSq) {
					maxDistSq = dist;
					disruptor.id = id;
					disruptor.running = true;
					disruptor.lastLoc = info.location;
				}

			}
		}

		if (disruptor.running) {
			globalCmd.broadcastIDChannel(disruptor.id, offsetFrom,
					MessageType.MAKE_DISRUPTOR, disruptor.loc);
			disruptor.done = true;
		}
	}

}
