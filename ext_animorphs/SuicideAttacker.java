package ext_animorphs;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class SuicideAttacker {
	public SoldierController sc;
	public RobotController rc;
	public Robot[] enemies;
	public Robot[] allies;
	public static final double SCALE = GameConstants.SELF_DESTRUCT_DAMAGE_FACTOR;
	public static final double BASE = GameConstants.SELF_DESTRUCT_BASE_DAMAGE;
	public Sensor sensor;
	public int kills;

	public SuicideAttacker(SoldierController sc) throws GameActionException {
		this.sc = sc;
		this.rc = sc.rc;
		this.sensor = sc.sensor;
	}

	public boolean explode(double threshold) throws GameActionException {
		if (!sensor.infoInitSenseEnemies) {
			sensor.initSenseEnemyInfo();
		}
		if (!sensor.infoInitAllAllies) {
			sensor.initAllAllyInfo();
		}
		kills = 0;
		enemies = sensor.getBots(Sensor.ADJACENT_ENEMIES);
		allies = sensor.getBots(Sensor.ADJACENT_ALLIES);

		RobotInfo[] enInfos = sensor.info.get(enemies);
		RobotInfo[] alInfos = sensor.info.get(allies);

		double dmg = BASE + rc.getHealth() * SCALE;
		double benefit = 0;
		for (int i = enemies.length; --i >= 0;) {
			RobotInfo info = enInfos[i];
			if (info.type != RobotType.HQ) {
				if (dmg < info.health) {
					benefit += dmg;
				} else {
					benefit += info.health;
					kills++;
				}
			}
		}

		for (int i = allies.length; --i >= 0;) {
			RobotInfo info = alInfos[i];
			if (info.type != RobotType.HQ) {
				benefit -= Math.min(dmg, info.health);
			}
		}
		benefit -= rc.getHealth();
		return benefit >= threshold;
	}

	/**
	 * returns benefit of exploding. Benefit is zero if there are only
	 * non-soldiers
	 * 
	 * @param loc
	 * @param health
	 * @return
	 * @throws GameActionException
	 */
	public double explodeAt(MapLocation loc, double health)
			throws GameActionException {
		kills = 0;
		if (!sensor.infoInitSenseEnemies) {
			sensor.initSenseEnemyInfo();
		}
		if (!sensor.infoInitAllAllies) {
			sensor.initAllAllyInfo();
		}
		enemies = rc.senseNearbyGameObjects(Robot.class, loc, 2, sc.enemy);
		allies = rc.senseNearbyGameObjects(Robot.class, loc, 2, sc.team);

		RobotInfo[] enInfos = sensor.info.get(enemies);
		RobotInfo[] alInfos = sensor.info.get(allies);

		boolean noSoldiers = true;

		double dmg = BASE + health * SCALE;
		double benefit = 0;
		for (int i = enemies.length; --i >= 0;) {
			RobotInfo info = enInfos[i];
			if (info.type == RobotType.SOLDIER) {
				noSoldiers = false;
			}
			if (info.type != RobotType.HQ) {
				if (dmg < info.health) {
					benefit += dmg;
				} else {
					benefit += info.health;
					kills++;
				}
			}
		}

		if (noSoldiers) {
			return -health;
		}

		for (int i = allies.length; --i >= 0;) {
			RobotInfo info = alInfos[i];
			if (info.type != RobotType.HQ) {
				benefit -= Math.min(dmg, info.health);
			}
		}

		benefit -= health;
		return benefit;
	}
}
