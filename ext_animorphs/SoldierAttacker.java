package ext_animorphs;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class SoldierAttacker {
	private SoldierController sc;
	private Robot[] enemyRobotsInAttackRange;
	private RobotController rc;
	public final int attackRadiusSquared;
	public final Team team;
	public final double attackPower;
	public FastRobotInfoSet info;
	public int numSoldiers;

	public Sensor sensor;

	public SoldierAttacker(SoldierController sc) throws GameActionException {
		this.sc = sc;
		this.rc = sc.rc;
		attackRadiusSquared = sc.attackRadiusSquared;
		team = sc.team;
		attackPower = sc.attackPower;

		this.sensor = sc.sensor;
		this.info = this.sensor.info;
	}

	public void updateEnemiesSensed() throws GameActionException {
		enemyRobotsInAttackRange = sensor.getBots(Sensor.ATTACK_RANGE_ENEMIES);
	}

	// Always chooses to attack a soldier if it can kill it. Suboptimal: if we
	// have like 5 soldiers surrounding a 10hp soldier, they'll all do it rather
	// than spreading
	// out their attacks.

	// Chooses to maximize damage done to a single soldier otherwise.
	public RobotInfo attackOptimal() throws GameActionException {
		enemyRobotsInAttackRange = sensor.getBots(Sensor.ATTACK_RANGE_ENEMIES);
		if (!sensor.infoInitSenseEnemies) {
			sensor.initSenseEnemyInfo();
		}
		RobotInfo[] enemyInfos = info.get(enemyRobotsInAttackRange);

		int numEns = enemyRobotsInAttackRange.length;
		if (numEns == 0) {
			return null;
		}
		if (numEns == 1) {
			RobotInfo rInfo = enemyInfos[0];
			if (rInfo.type == RobotType.HQ) {
				return null;
			}
			numSoldiers = 1;
			return rInfo;
		}

		double minHitTime = 100;
		RobotInfo bestBotInfo = null;
		// if(rinfo.type == RobotType.HQ){
		// attackLocation=rc.senseRobotInfo(enemyRobotsInAttackRange[1]).location;
		// }

		numSoldiers = 0;
		for (int i = numEns; --i >= 0;) {
			RobotInfo rInfo = enemyInfos[i];

			MapLocation rLocation = rInfo.location;
			double rHealth = rInfo.health;
			RobotType rType = rInfo.type;

			if (rHealth <= attackPower) {
				return rInfo;
			}
			if (rType == RobotType.SOLDIER) {
				numSoldiers++;
				Robot[] nearbyAlliedRobots = rc.senseNearbyGameObjects(
						Robot.class, rLocation, attackRadiusSquared, team);
				int attackCount = nearbyAlliedRobots.length + 1;
				double currHitTime = rHealth / attackCount / attackPower;
				if (currHitTime <= minHitTime) {
					bestBotInfo = rInfo;
					minHitTime = currHitTime;
				}
			}

		}
		if (numEns != 0 && numSoldiers == 0) {
			return enemyInfos[0];
		}
		return bestBotInfo;
	}

	public MapLocation attackWeakest() throws GameActionException {
		if (enemyRobotsInAttackRange.length == 0) {
			return null;
		}
		if (enemyRobotsInAttackRange.length == 1) {
			RobotInfo rInfo = rc.senseRobotInfo(enemyRobotsInAttackRange[0]);
			if (rInfo.type == RobotType.HQ) {
				return null;
			}
			return rInfo.location;
		} else {
			Robot currWeakest = enemyRobotsInAttackRange[0];
			for (Robot r : enemyRobotsInAttackRange) {
				if (rc.senseRobotInfo(r).health < rc
						.senseRobotInfo(currWeakest).health) {
					currWeakest = r;
				}
			}
			return rc.senseRobotInfo(currWeakest).location;
		}
	}

	public MapLocation attackWithPriority(RobotType rt)
			throws GameActionException {
		if (enemyRobotsInAttackRange.length == 0) {
			return null;
		}
		if (enemyRobotsInAttackRange.length == 1) {
			RobotInfo rInfo = rc.senseRobotInfo(enemyRobotsInAttackRange[0]);
			if (rInfo.type == RobotType.HQ) {
				return null;
			}
			return rInfo.location;
		} else {
			Robot currTarget = enemyRobotsInAttackRange[0];
			double currTargetHealth = rc.senseRobotInfo(currTarget).health;
			RobotType currTargetType = rc.senseRobotInfo(currTarget).type;

			for (Robot r : enemyRobotsInAttackRange) {
				double rHealth = rc.senseRobotInfo(r).health;
				RobotType rType = rc.senseRobotInfo(r).type;

				if (rType == rt) {
					if (currTargetType != rt || currTargetHealth < rHealth) {
						currTarget = r;
						currTargetHealth = rHealth;
						currTargetType = rType;
					}
				} else if (rc.senseRobotInfo(r).health < rc
						.senseRobotInfo(currTarget).health) {
					currTarget = r;
					currTargetHealth = rHealth;
					currTargetType = rType;
				}
			}
			return rc.senseRobotInfo(currTarget).location;
		}
	}

	// We should only attack cows in pastures if there are no other soldiers we
	// can attack nearby (due to nonexistence or being forced to go into HQ
	// range)
	public MapLocation attackCowsInPasture() throws GameActionException {
		boolean hasPasture = false;
		RobotInfo pastureInfo = null;
		for (Robot r : sensor.getBots(Sensor.SENSE_RANGE_ENEMIES)) {
			RobotInfo rInfo = rc.senseRobotInfo(r);
			if (rInfo.type == RobotType.PASTR) {
				hasPasture = true;
				pastureInfo = rInfo;
			}
		}
		if (!hasPasture) {
			return null;
		}
		MapLocation[] pastureLocs = MapLocation
				.getAllMapLocationsWithinRadiusSq(pastureInfo.location,
						GameConstants.PASTR_RANGE);
		for (MapLocation loc : pastureLocs) {
			if (rc.canAttackSquare(loc) && rc.senseCowsAtLocation(loc) > 100) {
				return loc;
			}
		}
		// for(MapLocation loc: pastureLocs){
		// if (rc.canAttackSquare(loc)){
		// return loc;
		// }
		// }
		return null;
	}
}
