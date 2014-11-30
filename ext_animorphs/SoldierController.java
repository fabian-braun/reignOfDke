package ext_animorphs;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class SoldierController extends Controller {
	// might have more states for different parts]
	public SoldierState state;

	public Mover m;
	public Bugger bugger;
	public SoldierAttacker attacker;
	public SuicideAttacker suicideAttacker;
	public Kiter kiter;
	public Runner runner;
	public Assaulter assaulter;
	public Defender defender;
	public PastureBuilder pastureBuilder;
	public NoiseTowerBuilder towerBuilder;
	public Suicider suicider;
	public Chaser chaser;
	public Sensor sensor;
	public DisrupterBuilder disrupterBuilder;

	public CommandMessager globalCmd;
	public RobotMessager personalCmd;
	public RobotMessager localCmd;// listen to local commands when executing
									// moves. Does not cause state transitions
	public RobotMessager hqResponse;
	public RobotMessager localResponse;

	// shared broadcasting stuff
	public int actionCh;
	public RobotType actionConstruct;

	public SoldierController(RobotController rc) throws GameActionException {
		super(rc);
		// init state
		state = SoldierState.IDLE;

		// init movement
		m = new Mover(this);
		bugger = new Bugger(this);
		sensor = new Sensor(this);

		// messaging
		globalCmd = new CommandMessager(rc);
		personalCmd = new RobotMessager(rc, this.id,
				CommsProtocol.ID_OFFSET_FROM_HQ);
		hqResponse = new RobotMessager(rc, this.id,
				CommsProtocol.ID_OFFSET_TO_HQ);
		localCmd = new RobotMessager(rc, this.id,
				CommsProtocol.ID_OFFSET_FROM_LOCAL);
		localResponse = new RobotMessager(rc, this.id,
				CommsProtocol.ID_OFFSET_TO_LOCAL);

		FastLocSet HQDangerSquares = new FastLocSet();
		MapLocation[] dangers = MapLocation.getAllMapLocationsWithinRadiusSq(
				this.enemyhq, this.HQ_SPLASH_RADIUS_SQ);
		for (int i = dangers.length; --i >= 0;) {
			MapLocation danger = dangers[i];
			if (danger.x >= 0 && danger.x < this.width && danger.y >= 0
					&& danger.y < this.height) {
				HQDangerSquares.add(danger);
			}
		}

		// behaviour helpers
		attacker = new SoldierAttacker(this);
		kiter = new Kiter(this, sensor);
		suicideAttacker = new SuicideAttacker(this);
		runner = new Runner(this, sensor);
		chaser = new Chaser(this, sensor);
		suicider = new Suicider(this, chaser, suicideAttacker);

		// behaviors
		assaulter = new Assaulter(this, sensor, attacker, suicideAttacker,
				bugger, localCmd, HQDangerSquares);
		defender = new Defender(this, sensor, attacker, suicideAttacker,
				bugger, localCmd, localResponse, HQDangerSquares);
		pastureBuilder = new PastureBuilder(this, bugger, sensor, hqResponse,
				HQDangerSquares);
		towerBuilder = new NoiseTowerBuilder(this, bugger, sensor, hqResponse,
				HQDangerSquares);
		disrupterBuilder = new DisrupterBuilder(this, bugger, sensor,
				hqResponse, HQDangerSquares);

		// spawn turn decisions
		init();
	}

	public void init() throws GameActionException {
	}

	public void sense() throws GameActionException {
		sensor.init();
		switch (state) {
		case IDLE:
		case BUILD_TOWER:
		case BUILD_PASTR:
		case BUILD_DISRUPTER:
		case CONSTRUCTING:
		case ASSAULT:
		case RALLY:
		case DEFEND:
		default:
			break;
		}
	}

	public void read() throws GameActionException {
		switch (state) {
		case IDLE:
		case ASSAULT:
		case RALLY:
		case DEFEND:
			// TODO update
			boolean msg = personalCmd.readMsg();
			if (msg) {
				switch (personalCmd.lastMsg) {
				case MAKE_DISRUPTOR: {
					disrupterBuilder.clear();
					disrupterBuilder.setEnemyPasturePoint(personalCmd.lastLoc);
					actionConstruct = RobotType.NOISETOWER;
					state = SoldierState.BUILD_DISRUPTER;
					return;
				}
				case MAKE_TOWER_HERDER: {
					towerBuilder.clear();
					towerBuilder.setTowerPoint(personalCmd.lastLoc);
					actionConstruct = RobotType.NOISETOWER;
					state = SoldierState.BUILD_TOWER;
					return;
				}

				case MAKE_PASTR: {
					pastureBuilder.clear();
					pastureBuilder.setPasturePoint(personalCmd.lastLoc);
					actionConstruct = RobotType.PASTR;
					state = SoldierState.BUILD_PASTR;
					return;
				}
				case ASSAULT: {
					if (state != SoldierState.ASSAULT
							|| !personalCmd.lastLoc.equals(assaulter.point)) {
						assaulter.setPoint(personalCmd.lastLoc);
					}
					state = SoldierState.ASSAULT;
					return;

				}
				case RALLY: {
					state = SoldierState.RALLY;
					return;
				}
				case DEFEND: {
					if (state != SoldierState.DEFEND
							|| !personalCmd.lastLoc.equals(defender.point)) {
						defender.clearDefend();
						defender.setPoint(personalCmd.lastLoc);
					}
					state = SoldierState.DEFEND;
					return;
				}
				default:
					break;
				}
			}
			// No personal commands, Generic behavior
			boolean cmd = globalCmd.readChannel(CommsProtocol.GLOBAL_CHANNEL);
			if (cmd) {
				switch (globalCmd.lastMsg) {
				case ASSAULT: {
					assaulter.setPoint(globalCmd.lastLoc);
					state = SoldierState.ASSAULT;
					return;
				}
				case RALLY: {
					state = SoldierState.RALLY;
					return;
				}
				case DEFEND: {
					if (!globalCmd.lastLoc.equals(defender.point)) {
						defender.clearDefend();
						defender.setPoint(globalCmd.lastLoc);
					}
					state = SoldierState.DEFEND;
					return;
				}
				default:
					return;
				}
			}
			return;
		case BUILD_TOWER:
		case BUILD_PASTR:
		case BUILD_DISRUPTER:
		case CONSTRUCTING:
		default:
			return;
		}
	}

	public boolean act() throws GameActionException {
		switch (state) {
		case IDLE:
			if (!rc.isActive()) {
				return true;
			}

			if (sensor.getBots(Sensor.ADJACENT_ALL).length >= 1) {
				Direction desired = teamhq.directionTo(rc.getLocation());
				if (rc.canMove(desired)) {
					rc.move(desired);
				} else if (rc.canMove(desired.rotateLeft())) {
					rc.move(desired.rotateLeft());
				} else if (rc.canMove(desired.rotateRight())) {
					rc.move(desired.rotateRight());
				}
			}
			return true;
		case ASSAULT:
			return assaulter.assault();
		case DEFEND:
			return defender.defend();
		case RALLY:
			return true; // TODO
		case BUILD_TOWER:
			return towerBuilder.act();
		case BUILD_DISRUPTER:
			return disrupterBuilder.act();
		case BUILD_PASTR:
			return pastureBuilder.act();
		case CONSTRUCTING:
		default:
			return true;
		}

	}

	public void broadcast() throws GameActionException {
		switch (state) {
		case BUILD_TOWER:
		case BUILD_PASTR:
			// rc.broadcast(actionCh, 0);
			return;
		case RALLY:
		case DEFEND:
			if (defender.seesEnemies)
				hqResponse.broadcastMsg(MessageType.NEARBY_ENEMY,
						rc.getLocation());
			break;
		case ASSAULT:
			// cmdIn.broadcastMsg(MessageType.FIGHTING, rc.getLocation());
			// rc.broadcast(actionCh, 0);
			return;
		case IDLE:
		case CONSTRUCTING:
		default:
			break;
		}
	}

	public void broadcastFailure() throws GameActionException {
		switch (state) {
		case ASSAULT:
		case DEFEND:
			// pasture died
			if (defender.deniedPasture) {
				hqResponse.broadcastMsg(MessageType.DENIED_PASTURE,
						rc.getLocation());
				defender.deniedPasture = false;
			}
			return;
		case RALLY:
			// TODO something to handle false returns from fighter methods
			return;

		case BUILD_PASTR:
			pastureBuilder.broadcastFailure();
			assaulter.setPoint(this.teamhq);
			state = SoldierState.ASSAULT;
			return;
		case BUILD_TOWER:
			towerBuilder.broadcastFailure();
			assaulter.setPoint(this.teamhq);
			state = SoldierState.ASSAULT;
			return;
		case BUILD_DISRUPTER:
			disrupterBuilder.broadcastFailure();
			assaulter.setPoint(this.teamhq);
			state = SoldierState.ASSAULT;
		default:
			return;
		}
	}

	public void run() throws GameActionException {
		sense();
		read();
		if (!act()) {
			broadcastFailure();
			act();
		} else {
			broadcast();
		}
	}
}
