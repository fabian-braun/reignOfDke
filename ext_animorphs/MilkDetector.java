package ext_animorphs;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;

public class MilkDetector {
	// constants
	public double MILK_GAIN = GameConstants.MILK_GAIN_FACTOR
			* GameConstants.WIN_QTY;

	public static final double STD_MILK_RATE = 20000; // assumed milk rate for
														// enemies
	public static final double PREC = 100.0;

	// controls
	public Controller c;
	public RobotController rc;
	public Sensor sensor;
	public CommandMessager cmd;

	// values
	public int enemyWeightedRounds;
	public int alliedWeightedRounds;
	public int enemyPastrs;
	public int alliedPastrs;
	public double enemyRate;
	public double alliedRate;

	public double alliedPastrMilk;
	public double enemyPastrMilk;
	public double alliedTotalMilk;
	public double enemyTotalMilk;

	public boolean enemyPastrBuilt;
	public boolean enemyPastrLost;
	public boolean alliedPastrBuilt;
	public boolean alliedPastrLost;

	public MilkDetector(Controller c, Sensor sensor, CommandMessager cmd) {
		this.c = c;
		this.rc = c.rc;
		this.sensor = sensor;
		this.enemyWeightedRounds = 0;
		this.alliedWeightedRounds = 0;
		this.enemyPastrs = 0;
		this.alliedPastrs = 0;

		this.enemyRate = STD_MILK_RATE;
		long[] memory = rc.getTeamMemory();
		if (memory.length > 0 && memory[0] > 0) {
			this.enemyRate = (memory[0] - 1) / PREC;
		}

		this.alliedRate = 0;
		this.alliedPastrMilk = 0;
		this.enemyPastrMilk = 0;
		this.alliedTotalMilk = 0;
		this.enemyTotalMilk = 0;
		this.cmd = cmd;
	}

	public void update() throws GameActionException {
		MapLocation[] ap = sensor.getLocs(Sensor.ALLIED_PASTR_LOCS);
		MapLocation[] ep = sensor.getLocs(Sensor.ENEMY_PASTR_LOCS);
		double am = rc.senseTeamMilkQuantity(c.team);
		double em = rc.senseTeamMilkQuantity(c.enemy);

		// update rounds
		this.alliedWeightedRounds += ap.length;
		this.enemyWeightedRounds += ep.length;

		// calculate allied milk
		double alliedMilkDiff = am - this.alliedTotalMilk;
		if (enemyPastrs > ep.length && alliedMilkDiff > MILK_GAIN) {
			this.alliedPastrMilk -= MILK_GAIN;
		}
		this.alliedPastrMilk += alliedMilkDiff;
		this.alliedTotalMilk = am;
		// set enemy pastrs
		enemyPastrBuilt = ep.length > enemyPastrs;
		enemyPastrLost = ep.length < enemyPastrs;
		enemyPastrs = ep.length;

		// calculate enemy milk
		if (alliedPastrs > ap.length && !deniedPasture()) {
			this.enemyTotalMilk += MILK_GAIN;
		}
		if (this.enemyTotalMilk < em) {
			this.enemyPastrMilk += em - this.enemyTotalMilk;
			this.enemyTotalMilk = em;
			// only update enemy rate if pastr milk increases
			if (this.enemyWeightedRounds > 0) {
				this.enemyRate = this.enemyPastrMilk / this.enemyWeightedRounds;
				rc.setTeamMemory(0, (long) (this.enemyRate * PREC) + 1); // .00
																			// precision
			}
		}
		// /set allied pastrs
		alliedPastrBuilt = ap.length > alliedPastrs;
		alliedPastrLost = ap.length < alliedPastrs;
		alliedPastrs = ap.length;

		// update rate
		if (this.alliedWeightedRounds > 0) {
			this.alliedRate = this.alliedPastrMilk / this.alliedWeightedRounds;
		}
	}

	public boolean deniedPasture() throws GameActionException {
		Robot[] bots = sensor.getBots(Sensor.ALL_ALLIES);
		for (Robot bot : bots) {
			int id = bot.getID();
			if (cmd.readIDChannel(id, CommsProtocol.ID_OFFSET_TO_HQ)) {
				if (cmd.lastMsg == MessageType.DENIED_PASTURE) {
					return true;
				}
			}
		}
		return false;
	}
}
