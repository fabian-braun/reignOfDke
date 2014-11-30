package ext_animorphs;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class HQController extends Controller {
	public Spawner spawner;
	public Sensor sensor;

	public HQAttacker attacker;
	public CommandMessager globalCmd;

	public ObjectiveManager objectives;

	public MilkDetector milk;

	// state
	public HQState state;

	public static final int DESP = 1600;

	public HQController(RobotController rc) throws GameActionException {
		super(rc);
		rc.setIndicatorString(2, "This is version");
		spawner = new Spawner(this);
		attacker = new HQAttacker(this);
		sensor = new Sensor(this);
		globalCmd = new CommandMessager(rc);
		objectives = new ObjectiveManager(this, sensor, globalCmd);
		state = HQState.DEFEND;
		milk = new MilkDetector(this, sensor, globalCmd);
	}

	public void sense() throws GameActionException {
		sensor.init();
		objectives.loadInfo();

		switch (state) {
		case ATTACK: {
			milk.update();
			rc.setIndicatorString(2, milk.alliedRate + ", " + milk.enemyRate);
			MapLocation[] epastrs = sensor.getLocs(Sensor.ENEMY_PASTR_LOCS);
			MapLocation[] apastrs = sensor.getLocs(Sensor.ALLIED_PASTR_LOCS);

			double alliedScore = milk.alliedRate * apastrs.length;
			double enemyScore = epastrs.length * milk.enemyRate;
			if ((alliedScore >= enemyScore && Clock.getRoundNum() <= DESP)
					|| (epastrs.length == 0)) {
				objectives.disruptor.on = true;
				state = HQState.DEFEND;
				objectives.resetBFSDefend();
				return;
			}

			if (milk.enemyPastrMilk > 1) {
				objectives.disruptor.on = true;
			}
			return;
		}
		case DEFEND: {
			milk.update();
			rc.setIndicatorString(2, milk.alliedRate + ", " + milk.enemyRate);
			MapLocation[] epastrs = sensor.getLocs(Sensor.ENEMY_PASTR_LOCS);
			MapLocation[] apastrs = sensor.getLocs(Sensor.ALLIED_PASTR_LOCS);

			double alliedScore = milk.alliedRate * apastrs.length;
			double enemyScore = epastrs.length * milk.enemyRate;
			if (enemyScore > alliedScore
					|| (Clock.getRoundNum() >= DESP && epastrs.length > 0)) {
				state = HQState.ATTACK;
				objectives.resetBFSAssault();
				return;
			}

			if (milk.enemyPastrBuilt || milk.enemyPastrLost) {
				objectives.resetBFSDefend();
			}
			return;
		}
		default:
			return;
		}
	}

	public void read() throws GameActionException {
		switch (state) {
		case ATTACK:
			objectives.readPasture();
			objectives.readHerder();
			objectives.readDisruptor();
			return;
		case DEFEND:
			objectives.readPasture();
			objectives.readHerder();
			objectives.readDisruptor();
			return;
		default:
			return;
		}
	}

	public void act() throws GameActionException {
		switch (state) {
		case ATTACK:
			if (rc.isActive()) {
				if (spawner.spawn()) {
					rc.spawn(spawner.spawnDir);
				} else {
					MapLocation target = attacker.quickDamage();
					if (target != null) {
						rc.attackSquare(target);
					}
				}
			}
			return;
		case DEFEND:
			if (rc.isActive()) {
				if (spawner.spawn()) {
					rc.spawn(spawner.spawnDir);
				} else {
					MapLocation target = attacker.quickDamage();
					if (target != null) {
						rc.attackSquare(target);
					}
				}
			}
			return;
		default:
			return;
		}
	}

	public void broadcast() throws GameActionException {
		switch (state) {
		case ATTACK: {
			objectives.broadcastAssault();
			objectives.clearChannels();
			if (objectives.herder.running || objectives.herder.done) {
				objectives.broadcastHerder();
			}
			objectives.broadcastDisruptor();
			return;
		}
		case DEFEND: {
			objectives.broadcastDefend();
			objectives.broadcastPasture();
			objectives.broadcastHerder();
			objectives.broadcastDisruptor();
			objectives.clearChannels();
			return;
		}
		default:
			return;
		}
	}

	public void calc() throws GameActionException {
		switch (state) {
		case ATTACK:
		case DEFEND:
			// MAGICAL CONSTANT
			objectives.calc(Clock.getBytecodesLeft() - 1000);
			return;
		default:
			return;
		}
	}

	public void run() throws GameActionException {
		sense();
		read();
		act();
		broadcast();
		calc();
		rc.setIndicatorString(2, state + " " + objectives.pasture.running + " "
				+ objectives.pasture.id);
	}
}
