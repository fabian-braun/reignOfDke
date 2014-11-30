package ext_animorphs;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class Assaulter {
	public Controller c;
	public RobotController rc;
	public Mover m;

	public Sensor sensor;
	public SoldierAttacker attacker;
	public SuicideAttacker suicide;
	public Bugger bugger;
	public MapLocation point;
	public RobotMessager localCmd;

	public final double attackPower;
	public final int attackRadiusSquared;
	public final int attackPlusRadiusSquared;
	public FastLocSet HQDangerSquares;
	public int suicideKills; // Use to only go in for suicides if we get a kill.

	public final Direction[] numToDir = Direction.values();

	public boolean steppedClose;

	public Assaulter(Controller c, Sensor sensor, SoldierAttacker attacker,
			SuicideAttacker suicide, Bugger bugger, RobotMessager localCmd,
			FastLocSet HQDangerSquares) {
		this.c = c;
		this.rc = c.rc;
		this.sensor = sensor;
		this.attacker = attacker;
		this.bugger = bugger;
		this.suicide = suicide;
		this.point = null;
		this.m = new Mover(c);
		this.localCmd = localCmd;
		this.HQDangerSquares = HQDangerSquares;

		this.attackRadiusSquared = c.attackRadiusSquared;
		attackPlusRadiusSquared = calcSplashRadiusSq(this.attackRadiusSquared);
		attackPower = c.attackPower;
		steppedClose = false;

	}

	public void setPoint(MapLocation point) {
		this.point = point;
		bugger.endBug();
	}

	public int numBots(MapLocation cur, Direction dir) {
		MapLocation loc = cur.add(dir);
		Robot[] bots = rc.senseNearbyGameObjects(Robot.class, loc,
				attackRadiusSquared, c.enemy);
		return bots.length;
	}

	public boolean bug() throws GameActionException {
		if (!bugger.bugging) {
			bugger.startBug(this.point);
		}
		Direction dir = bugger.bug();
		if (dir == Direction.NONE) {
			return false;
		} else if (dir == Direction.OMNI) {
			return true;
		} else {
			rc.move(dir);
			return true;
		}
	}

	public boolean assaultBug() throws GameActionException {
		MapLocation cur = rc.getLocation();

		// keep hugging
		if (bugger.bugging && bugger.hugging) {
			Direction dir = bugger.bug();
			if (dir == Direction.NONE) {
				return false;
			} else if (dir == Direction.OMNI) {
				return true;
			} else {
				if (checkStepIn(rc.getLocation().add(dir))) {
					rc.move(dir);
					return true;
				}
				return false;
			}
		} else {
			bugger.startBug(this.point);
			Direction dir = bugger.bug();
			if (dir == Direction.NONE) {
				return false;
			} else if (dir == Direction.OMNI) {
				return true;
			} else if (bugger.hugging && numBots(cur, dir) == 0) {
				rc.move(dir);
				return true;
			} else {
				bugger.endBug();
			}
		}

		Direction desired = cur.directionTo(this.point);
		if (rc.getHealth() < 20) {
			// don't go near if low
			desired = desired.opposite();
		}
		MapLocation desiredLoc = cur.add(desired);
		boolean canDir = rc.canMove(desired);
		if (canDir && checkStepIn(desiredLoc)) {
			if (!HQDangerSquares.contains(desiredLoc)) {
				rc.move(desired);
				return true;
			} else {
				if (!m.inSplashRadius(desiredLoc)) {
					rc.move(desired);
					return true;
				}
			}
		}

		Direction left = desired.rotateLeft();
		MapLocation leftLoc = cur.add(left);
		boolean canLeft = rc.canMove(left);
		if (canLeft && checkStepIn(leftLoc)) {
			if (!HQDangerSquares.contains(leftLoc)) {
				rc.move(left);
				return true;
			} else {
				if (!m.inSplashRadius(leftLoc)) {
					rc.move(left);
					return true;
				}
			}
		}

		Direction right = desired.rotateRight();
		MapLocation rightLoc = cur.add(right);
		boolean canRight = rc.canMove(right);
		if (canRight && checkStepIn(rightLoc)) {
			if (!HQDangerSquares.contains(rightLoc)) {
				rc.move(right);
				return true;
			} else {
				if (!m.inSplashRadius(rightLoc)) {
					rc.move(right);
					return true;
				}
			}
		}

		return true; // too scared
	}

	/**
	 * check whether if stepping in would put us in a bad fight
	 * 
	 * @param loc
	 * @return
	 * @throws GameActionException
	 */
	public boolean checkStepIn(MapLocation loc) throws GameActionException {
		// Simulate stepping in
		if (!sensor.infoInitSenseEnemies) {
			sensor.initSenseEnemyInfo();
		}
		Robot[] ens = rc.senseNearbyGameObjects(Robot.class, loc,
				attackRadiusSquared, c.enemy);
		int numEns = ens.length;

		if (numEns == 0) {
			return true;
		}
		Robot[] enemiesPlus = rc.senseNearbyGameObjects(Robot.class, loc,
				attackPlusRadiusSquared, c.enemy);
		RobotInfo[] enPlusInfo = sensor.getInfo(enemiesPlus);
		int numEnsPlus = enemiesPlus.length;
		for (int i = numEnsPlus; --i >= 0;) {
			if (enPlusInfo[i].type != RobotType.SOLDIER) {
				numEnsPlus--;
			}
		}

		double myHealth = rc.getHealth() / attackPower;

		RobotInfo[] enemyInfos = sensor.info.get(ens);
		myHealth = myHealth - numEns;
		if (myHealth <= 0) {
			// dead on stepping in
			return false;
		}

		// find best enemy to look at
		double bestShotsLeft = 100;
		RobotInfo bestBotInfo = enemyInfos[0];
		int numAlliedAttackers = 0;
		for (int i = numEns; --i >= 0;) {
			RobotInfo rInfo = enemyInfos[i];
			RobotType rType = rInfo.type;
			if (rType == RobotType.SOLDIER) {
				Robot[] nearbyAlliedRobots = rc.senseNearbyGameObjects(
						Robot.class, rInfo.location, attackRadiusSquared,
						c.team);
				int attackCount = nearbyAlliedRobots.length + 1;
				double currHitTime = rInfo.health / attackCount;
				if (currHitTime <= bestShotsLeft) {
					bestBotInfo = rInfo;
					bestShotsLeft = currHitTime;
					numAlliedAttackers = attackCount;
				}
			} else {
				numEns--;
			}
		}
		if (numEns == 0) {
			return true;
		}

		bestShotsLeft = bestShotsLeft / attackPower;

		numEnsPlus = Math.min(numEns + 2, numEnsPlus);

		// Check for allies stepping in and out
		if (!sensor.infoInitAllAllies) {
			sensor.initAllAllyInfo();
		}
		int stepInChange = 0;

		Robot[] allies = rc.senseNearbyGameObjects(Robot.class,
				bestBotInfo.location, attackPlusRadiusSquared, c.team);
		MapLocation bestBotLoc = bestBotInfo.location;

		RobotInfo[] allyInfos = sensor.info.get(allies);
		for (int i = allies.length; --i >= 0;) {
			RobotInfo allyInfo = allyInfos[i];
			// check if they are in range of the bestBot

			if (allyInfo.actionDelay > 1) {
				// not moving
				continue;
			}
			MapLocation theirLoc = allyInfo.location;
			double theirHealth = allyInfo.health / attackPower;
			if (theirLoc.distanceSquaredTo(bestBotLoc) > attackRadiusSquared) {
				if (stepInChange > 3) {
					continue;
				}
				MapLocation stepInSquare = theirLoc.add(theirLoc
						.directionTo(bestBotLoc));
				if (!rc.canSenseSquare(stepInSquare)) {
					if (rc.senseTerrainTile(stepInSquare) != TerrainTile.VOID) {
						Robot[] theirEns = rc.senseNearbyGameObjects(
								Robot.class, stepInSquare, attackRadiusSquared,
								c.enemy);
						int numTheirEns = theirEns.length;
						if (theirHealth > 2 * numTheirEns
								&& numTheirEns <= numAlliedAttackers + 1
								&& allyInfo.type == RobotType.SOLDIER) {
							// probably will step in
							stepInChange++;
							numAlliedAttackers++;
						}
					}
					continue;
				}

				if (rc.senseTerrainTile(stepInSquare) != TerrainTile.VOID
						&& !stepInSquare.equals(loc)
						&& rc.senseObjectAtLocation(loc) == null) {
					Robot[] theirEns = rc.senseNearbyGameObjects(Robot.class,
							stepInSquare, attackRadiusSquared, c.enemy);
					int numTheirEns = theirEns.length;
					if (theirHealth > 2 * numTheirEns
							&& numTheirEns <= numAlliedAttackers + 1
							&& allyInfo.type == RobotType.SOLDIER) {
						// probably will step in
						stepInChange++;
						numAlliedAttackers++;
					}
				}
				continue;
			}

			else {
				Robot[] theirEns = rc.senseNearbyGameObjects(Robot.class,
						theirLoc, attackRadiusSquared, c.enemy);
				int numTheirEns = theirEns.length;
				if (theirHealth <= numTheirEns
						|| allyInfo.type != RobotType.SOLDIER) {
					// probably stepping out
					numAlliedAttackers--;
				}
			}
		}
		// numAlliedAttackers += stepInChange;
		// Check if stepping into bad position (numEns>numAlliedAttackers+1) or
		// I die before enemy
		// rc.setIndicatorString(0, "Round: " + Clock.getRoundNum()+
		// "num ens Plus: " + numEnsPlus +
		// " numAlliedAttackers: " + numAlliedAttackers + " BestTarget: " +
		// Math.ceil(bestBotInfo.health/attackPower/numAlliedAttackers) +
		// " myRate: " + Math.ceil(myHealth/numEnsPlus));

		if (myHealth <= numEnsPlus
				|| (Math.ceil(myHealth / numEnsPlus) < Math
						.ceil(bestBotInfo.health / attackPower
								/ numAlliedAttackers) && numEnsPlus >= numAlliedAttackers)) { // MAGIC
																								// CONSTANT
																								// numAlliedAttackers-x
																								// ...
																								// x
																								// controls
																								// how
																								// aggressive
			// rc.setIndicatorString(1, "Round:" + Clock.getRoundNum()
			// +" lose step in to " + loc.toString());
			return false;
		}
		// rc.setIndicatorString(1, "Round:" + Clock.getRoundNum()
		// +" Win step in to " + loc.toString());

		return true;
	}

	public boolean assaultAttack() throws GameActionException {
		// fight
		MapLocation cur = rc.getLocation();
		RobotInfo targetInfo = attacker.attackOptimal();
		if (targetInfo == null) {
			// SHOULD NEVER HAPPEN
			return false;
		}
		steppedClose = false;
		boolean canRun = true;

		MapLocation target = targetInfo.location;
		double myHealth = rc.getHealth() / attackPower;
		double targetHealth = targetInfo.health / attackPower;
		int numEns = attacker.numSoldiers;

		Robot[] allies = rc.senseNearbyGameObjects(Robot.class, target,
				attackRadiusSquared, c.team);

		int numAttacks = allies.length + 1;

		if (numEns == 1) {
			// SPECIAL CASE WITH ONE ENEMY
			if (targetInfo.isConstructing
					|| targetInfo.type == RobotType.NOISETOWER
					|| targetInfo.type == RobotType.PASTR) {
				rc.attackSquare(target);
				return true;
			}
		}

		if (targetHealth <= 1) {
			rc.attackSquare(target);
			return true;
		}

		if (myHealth <= numEns) {
			// RETREAT
			Robot[] enemies = sensor.getBots(Sensor.ATTACK_RANGE_ENEMIES);
			// Don't die
			RobotInfo[] infos = sensor.info.get(enemies);
			int xLoc = 0;
			int yLoc = 0;
			for (int i = numEns; --i >= 0;) {
				MapLocation loc = infos[i].location;
				xLoc += loc.x;
				yLoc += loc.y;
			}
			xLoc = xLoc / numEns;
			yLoc = yLoc / numEns;
			Direction move = m.moveInDir(new MapLocation(xLoc, yLoc)
					.directionTo(cur));
			if (move != Direction.NONE) {
				MapLocation newLoc = cur.add(move);
				int ens = rc.senseNearbyGameObjects(Robot.class, newLoc,
						attackRadiusSquared, c.enemy).length;
				if (ens < myHealth && ens < numEns && !m.inSplashRadius(newLoc)) {
					rc.move(move);
					return true;
				}
			}
			canRun = false;
		}

		// Check for a rush
		Direction dir = cur.directionTo(target);
		MapLocation behind = cur.add(dir.opposite());
		Robot[] alliesBehind = rc.senseNearbyGameObjects(Robot.class, behind,
				2, c.team);
		// rc.setIndicatorString(1, "Allies Behind: " + alliesBehind.length);
		if (alliesBehind.length >= 3) {
			MapLocation forward = cur.add(dir);
			MapLocation forwardMore = forward.add(dir);
			TerrainTile fTile = rc.senseTerrainTile(forward);
			TerrainTile fmTile = rc.senseTerrainTile(forwardMore);
			if ((fTile == TerrainTile.ROAD || fTile == TerrainTile.NORMAL)
					&& (fmTile == TerrainTile.ROAD || fmTile == TerrainTile.NORMAL)) {
				// can move closer
				double effectiveHealth = myHealth;
				if (rc.senseTerrainTile(cur) == TerrainTile.ROAD) {
					if (!dir.isDiagonal()) {
						// Only lose one turn
						effectiveHealth = effectiveHealth * 1.5;
					}
				}
				if (dir.isDiagonal()) {
					// lose extra turn
					effectiveHealth = effectiveHealth / 1.5;
				}
				int newEns = rc.senseNearbyGameObjects(Robot.class, forward,
						attackRadiusSquared, c.enemy).length;
				// rc.setIndicatorString(2, "newEns : " + newEns +
				// " effectiveHealth: " + effectiveHealth);
				if (newEns < effectiveHealth) {
					if (rc.canMove(dir)
							&& !HQDangerSquares.contains(cur.add(dir))) {
						steppedClose = true;
						rc.move(dir);
						return true;
					}
				}
			} else {
				// rc.setIndicatorString(2, "can't rush forward");
			}
		}

		// Should we run
		if (canRun
				&& (Math.ceil(myHealth / numEns) < Math.ceil(targetHealth
						/ numAttacks) && numEns >= numAttacks)) {
			// RETREAT
			// don't die, or take bad trade. back away
			Robot[] enemies = sensor.getBots(Sensor.ATTACK_RANGE_ENEMIES);
			RobotInfo[] infos = sensor.info.get(enemies);
			int xLoc = 0;
			int yLoc = 0;
			for (int i = numEns; --i >= 0;) {
				MapLocation loc = infos[i].location;
				xLoc += loc.x;
				yLoc += loc.y;
			}
			xLoc = xLoc / numEns;
			yLoc = yLoc / numEns;
			Direction move = m.moveInDir(new MapLocation(xLoc, yLoc)
					.directionTo(cur));
			if (move != Direction.NONE) {
				MapLocation newLoc = cur.add(move);
				int ens = rc.senseNearbyGameObjects(Robot.class, newLoc,
						attackRadiusSquared, c.enemy).length;
				if (ens < myHealth && ens < numEns) {
					rc.move(move);
					return true;
				}
			}
			canRun = false;
		}
		if (!canRun) {
			Direction suicideDir = getSuicideDir(rc.senseNearbyGameObjects(
					Robot.class, 5, c.enemy));
			if (suicideDir != Direction.NONE && suicideDir != Direction.OMNI) {
				rc.move(suicideDir);
				return true;
			}
		}

		// if (bugger.bugging) {
		// bugger.endBug();
		// }
		//
		// // fight
		// RobotInfo targetInfo = attacker.attackOptimal();
		// if (targetInfo == null) {
		// return false;
		// }
		// MapLocation target = targetInfo.location;
		//
		// // TODO have this stored in soldier attacker
		// // TODO calculate death timers, and compare (health / hits)
		// Robot[] allies = rc.senseNearbyGameObjects(Robot.class, target,
		// attackRadiusSquared, c.team);
		// Robot[] enemies = sensor.getBots(Sensor.ATTACK_RANGE_ENEMIES);
		// if (allies.length + 1 < enemies.length) {
		// //reposition
		// MapLocation cur = rc.getLocation();
		// Direction move = m.moveInDir(target.directionTo(cur));
		//
		// //move away
		// if (move != Direction.NONE) {
		// rc.move(move);
		// return true;
		// }
		//
		// // fall through and fight.
		// }
		//
		rc.attackSquare(target);
		return true;
	}

	/**
	 * for 2-1 away enemies
	 * 
	 * @param enemies
	 * @return
	 * @throws GameActionException
	 */
	public boolean assaultSemiClose(Robot[] enemies) throws GameActionException {
		if (bugger.bugging) {
			bugger.endBug();
		}
		if (!sensor.infoInitSenseEnemies) {
			sensor.initSenseEnemyInfo();
		}
		Direction dir = getSuicideDir(enemies);
		if (dir != Direction.NONE && dir != Direction.OMNI) {
			rc.move(dir);
			return true;
		}

		RobotInfo targetInfo = attacker.attackOptimal();
		if (targetInfo == null) {
			// SHOULD NEVER HAPPEN
			return false;
		}
		MapLocation target = targetInfo.location;
		double targetHealth = targetInfo.health / attackPower;
		int numEns = attacker.numSoldiers;
		MapLocation cur = rc.getLocation();
		double myHealth = rc.getHealth() / attackPower;

		// CHECK FOR 2-1 suiciders
		RobotInfo[] enemiesInfo = sensor.info.get(enemies);
		for (int i = enemies.length; --i >= 0;) {
			RobotInfo enInf = enemiesInfo[i];
			MapLocation enLoc = enInf.location;
			Direction suicideDir = enLoc.directionTo(cur);
			MapLocation adj = enLoc.add(suicideDir, 2);
			Robot ally = (Robot) rc.senseObjectAtLocation(adj);
			if (ally != null && ally.getTeam() == c.team) {
				Direction backOff = m.moveInDir(getStraightBackOff(cur, enLoc));
				if (backOff != Direction.NONE) {
					MapLocation newLoc = cur.add(backOff);
					int newEns = rc.senseNearbyGameObjects(Robot.class, newLoc,
							attackRadiusSquared, c.enemy).length;
					if (newEns < myHealth && newEns <= numEns) {
						rc.move(backOff);
						return true;
					}
				}
			}
		}

		// BEHAVE LIKE ASSAULT ATTACK
		// fight
		Robot[] allies = rc.senseNearbyGameObjects(Robot.class, target,
				attackRadiusSquared, c.team);
		int numAttacks = allies.length + 1;
		if (numEns == 1) {
			// SPECIAL CASE WITH ONE ENEMY
			if (targetInfo.isConstructing
					|| targetInfo.type == RobotType.NOISETOWER
					|| targetInfo.type == RobotType.PASTR) {
				rc.attackSquare(target);
				return true;
			}
		}
		if (targetHealth <= 1) {
			rc.attackSquare(target);
			return true;
		}

		if (myHealth <= numEns
				|| (Math.ceil(myHealth / numEns) < Math.ceil(targetHealth
						/ numAttacks) && numEns >= numAttacks)) {
			// RETREAT
			Robot[] ens = sensor.getBots(Sensor.ATTACK_RANGE_ENEMIES);
			// Don't die
			RobotInfo[] infos = sensor.info.get(ens);
			int xLoc = 0;
			int yLoc = 0;
			for (int i = numEns; --i >= 0;) {
				MapLocation loc = infos[i].location;
				xLoc += loc.x;
				yLoc += loc.y;
			}
			xLoc = xLoc / numEns;
			yLoc = yLoc / numEns;
			Direction move = m.moveInDir(new MapLocation(xLoc, yLoc)
					.directionTo(cur));
			if (move != Direction.NONE) {
				MapLocation newLoc = cur.add(move);
				int newEns = rc.senseNearbyGameObjects(Robot.class, newLoc,
						attackRadiusSquared, c.enemy).length;
				if (newEns < myHealth && newEns < numEns
						&& !m.inSplashRadius(newLoc)) {
					rc.move(move);
					return true;
				}
			}
		}

		rc.attackSquare(target);
		return true;
	}

	public boolean assaultClose(Robot[] enemies) throws GameActionException {
		if (bugger.bugging) {
			bugger.endBug();
		}
		if (!sensor.infoInitSenseEnemies) {
			sensor.initSenseEnemyInfo();
		}

		// Check to rush
		if (steppedClose) {
			Direction dir = getSuicideDir(enemies);
			if (dir == Direction.NONE || dir == Direction.OMNI) {
				// not worth, just attack
				RobotInfo targetInfo = attacker.attackOptimal();
				if (targetInfo != null) {
					rc.attackSquare(targetInfo.location);
				}
				return true;
			}
			rc.move(dir);
			return true;
		}

		// check if we should suicide
		Direction dir = getSuicideDir(enemies);
		if (dir != Direction.NONE && dir != Direction.OMNI) {
			rc.move(dir);
			return true;
		}

		// back up cause they gonna blow
		// TODO change to sensor
		MapLocation enemy = sensor.info.get(enemies[0]).location;
		MapLocation cur = rc.getLocation();
		Direction desired = enemy.directionTo(cur);
		Direction moveAway = m.moveInDir(desired);
		if (moveAway != Direction.NONE) {
			rc.move(moveAway);
			return true;
		}

		// move up to meet them.
		Direction moveTowards = m.moveInDir(desired.opposite());
		if (moveTowards != Direction.NONE) {
			rc.move(moveTowards);
			return true;
		}

		RobotInfo targetInfo = attacker.attackOptimal();
		if (targetInfo != null) {
			rc.attackSquare(targetInfo.location);
		}
		// reposition
		return true;
	}

	public boolean assaultAdjacent(Robot[] enemies) throws GameActionException {
		if (bugger.bugging) {
			bugger.endBug();
		}

		// check if suicide
		// if (suicide.explode(0)) {
		// rc.selfDestruct();
		// }
		if (!sensor.infoInitSenseEnemies) {
			sensor.initSenseEnemyInfo();
		}
		if (willDie(sensor.getBots(Sensor.ATTACK_RANGE_ENEMIES))) {
			if (suicide.explode(0)) {
				rc.selfDestruct();
			}
		}

		if (enemies.length <= 2) {
			RobotInfo[] enInfo = sensor.getInfo(enemies);
			boolean noSoldiers = true;
			for (int i = enemies.length; --i >= 0;) {
				if (enInfo[0].type == RobotType.SOLDIER) {
					noSoldiers = false;
				}
			}
			if (noSoldiers) {
				return assaultAttack();
			}
		}

		Direction dir = getSuicideDir(enemies);
		if (dir != Direction.NONE) {
			if (dir == Direction.OMNI) {
				rc.selfDestruct();
			} else {
				rc.move(dir);
				return true;
			}
		}

		RobotInfo targetInfo = attacker.attackOptimal();
		// check whether we can live.
		if (attacker.numSoldiers < rc.getHealth() / attackPower) {
			// back up cause they gonna blow
			// TODO change to sensor
			MapLocation enemy = rc.senseRobotInfo(enemies[0]).location;
			MapLocation cur = rc.getLocation();
			Direction desired = enemy.directionTo(cur);
			Direction moveAway = m.moveInDir(desired);
			if (moveAway != Direction.NONE) {
				rc.move(moveAway);
				return true;
			}
		}

		if (targetInfo != null) {
			rc.attackSquare(targetInfo.location);
		}

		return true;
	}

	// true if good state
	public boolean assault() throws GameActionException {
		// if someone in atk range, stay/kite/meet/sidestep?
		// otherwise, try to move towards goal without running into atk range
		// for now ignore hq

		// only option is to suicide

		Robot[] snsRngEnemies = sensor.getBots(Sensor.SENSE_RANGE_ENEMIES);
		// no enemies move/bug around
		if (snsRngEnemies.length == 0) {
			steppedClose = false;
			if (!rc.isActive()) {
				if (bugger.bugging) {
					bugger.calcPreDir();
				}
				return true;
			}
			return bug();
		}

		Robot[] atkRngEnemies = sensor.getBots(Sensor.ATTACK_RANGE_ENEMIES);

		// far off enemies, try to go around them
		if (atkRngEnemies.length == 0) {
			steppedClose = false;
			if (!rc.isActive()) {
				if (bugger.bugging) {
					bugger.calcPreDir();
				}
				return true;
			}
			return assaultBug();
		}

		Robot[] semiCloseEnemies = sensor.getBots(Sensor.SEMI_CLOSE_ENEMIES);
		if (semiCloseEnemies.length == 0) {
			if (!rc.isActive()) {
				if (bugger.bugging) {
					bugger.calcPreDir();
				}
				return true;
			}
			return assaultAttack();
		}

		Robot[] closeEnemies = sensor.getBots(Sensor.CLOSE_ENEMIES);
		if (closeEnemies.length == 0) {
			if (!rc.isActive()) {
				if (bugger.bugging) {
					bugger.calcPreDir();
				}
				return true;
			}
			return assaultSemiClose(semiCloseEnemies);
		}

		Robot[] adjEnemies = sensor.getBots(Sensor.ADJACENT_ENEMIES);
		if (adjEnemies.length == 0) {
			if (!rc.isActive()) {
				if (bugger.bugging) {
					bugger.calcPreDir();
				}
				return true;
			}
			return assaultClose(closeEnemies);
		}
		if (!rc.isActive()) {
			if (willDie(atkRngEnemies)) {

				if (suicide.explode(0)) {
					rc.selfDestruct();
				}
			}

			Direction dir = getSuicideDir(adjEnemies);
			if (dir == Direction.OMNI) {
				rc.selfDestruct();
			}
			return true;
		}
		return assaultAdjacent(adjEnemies);
	}

	/**
	 * get suicide direction given nearby (close) enemies # close enemies should
	 * be small
	 * 
	 * @param enemies
	 * @return OMNI if should explode. NONE if shouldn't explode. otherwise,
	 *         direction to move in
	 * @throws GameActionException
	 */

	public Direction getSuicideDir(Robot[] enemies) throws GameActionException {
		MapLocation cur = rc.getLocation();
		double myHealth = rc.getHealth();
		int BONUS_FOR_GUARANTEE = 2; // MAGIC CONSTANT additional bonus for
										// guaranteed enemy health taken (i.e.
										// if 2, potential enemies we explode on
										// give 1/3 beneift
		// base line is exploding now
		Direction bestDir = Direction.OMNI;
		double bestBenefit = (BONUS_FOR_GUARANTEE + 1)
				* suicide.explodeAt(cur, myHealth);
		if (suicide.kills == 0) {
			bestBenefit -= myHealth * (BONUS_FOR_GUARANTEE - 1);
		}

		double SCALE = GameConstants.SELF_DESTRUCT_DAMAGE_FACTOR;
		double BASE = GameConstants.SELF_DESTRUCT_BASE_DAMAGE;
		boolean[] checked = new boolean[numToDir.length];
		double[] benefits = new double[numToDir.length];
		double[] damage = new double[numToDir.length];
		boolean[] kills = new boolean[numToDir.length];

		for (int i = enemies.length; --i >= 0;) {
			RobotInfo enInfo = sensor.info.get(enemies[i]);
			double theirDelay = enInfo.actionDelay;
			MapLocation enLocation = enInfo.location;
			Direction main = cur.directionTo(enLocation);
			Direction left = main.rotateLeft();
			Direction right = main.rotateRight();

			if (rc.canMove(main)) {
				MapLocation loc = cur.add(main);
				if (!HQDangerSquares.contains(loc)) {
					int index = main.ordinal();

					if (checked[index]) {
						// check for additional corner
						if (corners(loc, enLocation) || theirDelay > 1) {
							benefits[index] += BONUS_FOR_GUARANTEE
									* Math.min(enInfo.health, damage[index]);
							if (!kills[index]) {
								if (damage[index] >= enInfo.health) {
									kills[index] = true;
									benefits[index] += BONUS_FOR_GUARANTEE
											+ myHealth;
								}
							}
						}
					} else {
						checked[index] = true;
						// check move
						int numEns = rc.senseNearbyGameObjects(Robot.class,
								loc, attackRadiusSquared, c.enemy).length;
						double dmgTaken = numEns * attackPower;
						double newHealth = myHealth - dmgTaken;
						damage[index] = (newHealth) * SCALE + BASE;
						if (newHealth > 0) {
							// check survival
							double benefit = suicide.explodeAt(loc, myHealth);
							if (suicide.kills > 0) {
								benefit -= 2 * myHealth * BONUS_FOR_GUARANTEE;
							} else {
								benefit -= (2 * BONUS_FOR_GUARANTEE + 1)
										* myHealth;
							}

							// check if we corner the enemy
							if (corners(loc, enLocation) || theirDelay > 1) {
								benefit += BONUS_FOR_GUARANTEE
										* Math.min(enInfo.health, damage[index]); // MAGIC
																					// CONSTANT
								if (damage[index] >= enInfo.health) {
									kills[index] = true;
									benefit += BONUS_FOR_GUARANTEE + myHealth;
								}
							}
							benefits[index] = benefit;
						} else {
							// we would die. Don't do it
							benefits[index] = -100000;
						}
					}
				}
			}

			if (rc.canMove(left)) {
				MapLocation loc = cur.add(left);
				if (!HQDangerSquares.contains(loc)) {
					int index = left.ordinal();

					if (checked[index]) {
						// check for additional corner
						if (corners(loc, enLocation) || theirDelay > 1) {
							benefits[index] += BONUS_FOR_GUARANTEE
									* Math.min(enInfo.health, damage[index]);
							if (!kills[index]) {
								if (damage[index] >= enInfo.health) {
									kills[index] = true;
									benefits[index] += BONUS_FOR_GUARANTEE
											+ myHealth;
								}
							}
						}
					} else {
						checked[index] = true;
						// check move
						int numEns = rc.senseNearbyGameObjects(Robot.class,
								loc, attackRadiusSquared, c.enemy).length;
						double dmgTaken = numEns * attackPower;
						double newHealth = myHealth - dmgTaken;
						damage[index] = (newHealth) * SCALE + BASE;
						if (newHealth > 0) {
							// check survival
							double benefit = suicide.explodeAt(loc, myHealth);
							if (suicide.kills > 0) {
								benefit -= 2 * myHealth * BONUS_FOR_GUARANTEE;
							} else {
								benefit -= (2 * BONUS_FOR_GUARANTEE + 1)
										* myHealth;
							}

							// check if we corner the enemy
							if (corners(loc, enLocation) || theirDelay > 1) {
								benefit += BONUS_FOR_GUARANTEE
										* Math.min(enInfo.health, damage[index]); // MAGIC
																					// CONSTANT
								if (damage[index] >= enInfo.health) {
									kills[index] = true;
									benefit += BONUS_FOR_GUARANTEE + myHealth;
								}
							}
						} else {
							// we would die. Don't do it
							benefits[index] = -100000;
						}
					}
				}
			}
			if (rc.canMove(right)) {
				MapLocation loc = cur.add(right);
				if (!HQDangerSquares.contains(loc)) {
					int index = right.ordinal();

					if (checked[index]) {
						// check for additional corner
						if (corners(loc, enLocation) || theirDelay > 1) {
							benefits[index] += BONUS_FOR_GUARANTEE
									* Math.min(enInfo.health, damage[index]);
							if (!kills[index]) {
								if (damage[index] >= enInfo.health) {
									kills[index] = true;
									benefits[index] += BONUS_FOR_GUARANTEE
											+ myHealth;
								}
							}
						}
					} else {
						checked[index] = true;
						// check move
						int numEns = rc.senseNearbyGameObjects(Robot.class,
								loc, attackRadiusSquared, c.enemy).length;
						double dmgTaken = numEns * attackPower;
						double newHealth = myHealth - dmgTaken;
						damage[index] = (newHealth) * SCALE + BASE;
						if (newHealth > 0) {
							// check survival
							double benefit = suicide.explodeAt(loc, myHealth);
							if (suicide.kills > 0) {
								benefit -= 2 * myHealth * BONUS_FOR_GUARANTEE;
							} else {
								benefit -= (2 * BONUS_FOR_GUARANTEE + 1)
										* myHealth;
							}

							// check if we corner the enemy
							if (corners(loc, enLocation) || theirDelay > 1) {
								benefit += BONUS_FOR_GUARANTEE
										* Math.min(enInfo.health, damage[index]); // MAGIC
																					// CONSTANT
								if (damage[index] >= enInfo.health) {
									kills[index] = true;
									benefit += BONUS_FOR_GUARANTEE + myHealth;
								}
							}
						} else {
							// we would die. Don't do it
							benefits[index] = -100000;
						}
					}
				}
			}
		}
		for (int i = benefits.length; --i >= 0;) {

			if (benefits[i] > bestBenefit) {
				bestDir = numToDir[i];
				bestBenefit = benefits[i];
			}
		}
		if (bestBenefit <= 0) { // MAGIC CONSTANT threshold for suicide.
			// Dont try to explode
			return Direction.NONE;
		}
		return bestDir;
	}

	/**
	 * Check if moving to a location corners enemy at other location if they
	 * back off in 3 directions
	 * 
	 * @param loc
	 * @param enLocation
	 */
	public boolean corners(MapLocation loc, MapLocation enLocation)
			throws GameActionException {
		Direction flee = loc.directionTo(enLocation);
		MapLocation check = enLocation.add(flee);
		TerrainTile t = rc.senseTerrainTile(check);
		if ((t == TerrainTile.VOID || t == TerrainTile.OFF_MAP)
				&& rc.senseObjectAtLocation(check) != null) {
			flee = flee.rotateLeft();
			check = enLocation.add(flee);
			t = rc.senseTerrainTile(check);
			if ((t == TerrainTile.VOID || t == TerrainTile.OFF_MAP)
					&& rc.senseObjectAtLocation(check) != null) {
				flee = flee.rotateRight().rotateRight();
				check = enLocation.add(flee);
				t = rc.senseTerrainTile(check);
				if ((t == TerrainTile.VOID || t == TerrainTile.OFF_MAP)
						&& rc.senseObjectAtLocation(check) != null) {
					return true;
				}
			}
		}
		return false;
	}

	public static int calcSplashRadiusSq(int x) {
		int sp = Controller.SPLASH_RADIUS_SQ;
		// (sqrt(x)+sqrt(2))^2 <= x + ceil(sqrt(2x)*2) + 2
		return x + ((int) Math.ceil((Math.sqrt(sp * x) * sp) + 1)) + sp;
	}

	/**
	 * Helper method, decides whether we win 1v1 vs the enemy.
	 * 
	 * @param enemyInfo
	 *            enemy info
	 * @param myHealth
	 *            Health/attackpower
	 */
	public boolean shouldFightDuel(RobotInfo enemyInfo, double myHealth) {
		if (enemyInfo.isConstructing || enemyInfo.type == RobotType.NOISETOWER
				|| enemyInfo.type == RobotType.PASTR) {
			return true;
		}
		double enTurns = enemyInfo.health / attackPower;
		if (Math.ceil(enTurns) > Math.ceil(myHealth)) {
			// don't go in
			return false;
		}
		return true;

	}

	/**
	 * Helper to calculate backoff in a 1-2 position
	 * 
	 * @param cur
	 * @param en
	 * @return
	 */
	public Direction getStraightBackOff(MapLocation cur, MapLocation en) {
		int myX = cur.x;
		int myY = cur.y;
		int hisX = en.x;
		int hisY = en.y;
		if (hisX > myX) {
			if (hisX - myX > 1) {
				return Direction.WEST;
			}
			if (hisY > myY) {
				return Direction.NORTH;
			}
			return Direction.SOUTH;

		}
		if (myX - hisX > 1) {
			return Direction.EAST;
		}
		if (hisY > myY) {
			return Direction.NORTH;
		}
		return Direction.SOUTH;

	}

	public boolean willDie(Robot[] attackRangeEnemies)
			throws GameActionException {
		if (!sensor.infoInitSenseEnemies) {
			sensor.initSenseEnemyInfo();
		}
		RobotInfo[] infos = sensor.getInfo(attackRangeEnemies);
		int numEns = 0;
		for (int i = infos.length; --i >= 0;) {
			if (infos[i].actionDelay <= 1) {
				numEns++;
			}
		}
		if (numEns >= rc.getHealth() / attackPower) {
			return true;
		}
		return false;
	}
}
