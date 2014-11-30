package ext_animorphs;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TerrainTile;

public class Defender {
	public Controller c;
	public RobotController rc;
	public Mover m;

	public Sensor sensor;
	public SoldierAttacker attacker;
	public SuicideAttacker suicide;
	public Bugger bugger;
	public MapLocation point;
	public RobotMessager localCmd;
	public RobotMessager localResponse;

	public FastLocSet HQDangerSquares;
	public static final int LARGE_PRIME = 19037;
	public final int attackPlusRadiusSquared;
	public final int attackRadiusSquared;
	public final double attackPower;
	public FastLocSet allyLocs;
	public boolean reachedPoint;
	public Robot myPasture; // a pasture (may be null) we want to defend
	public MapLocation pastureLoc;
	public int chaseDistance; // track how far we will chase enemies
	public boolean seesEnemies;
	public boolean deniedPasture;

	public Team enemy;
	public Team team;

	public final Direction[] numToDir = Direction.values();
	public final int MAX_SNEAK_DISTANCE; // MAGIC CONSTANT

	public Defender(Controller c, Sensor sensor, SoldierAttacker attacker,
			SuicideAttacker suicide, Bugger bugger, RobotMessager localCmd,
			RobotMessager localResponse, FastLocSet HQDangerSquares) {
		this.c = c;
		this.rc = c.rc;
		this.sensor = sensor;
		this.attacker = attacker;
		this.bugger = bugger;
		this.suicide = suicide;
		this.point = null;
		this.m = new Mover(c);
		this.localCmd = localCmd;
		this.localResponse = localResponse;
		this.HQDangerSquares = HQDangerSquares;
		this.team = c.team;
		this.enemy = c.enemy;
		this.attackPower = c.attackPower;

		MAX_SNEAK_DISTANCE = c.sensorRadiusSquared;
		attackRadiusSquared = c.attackRadiusSquared;
		attackPlusRadiusSquared = calcSplashRadiusSq(attackRadiusSquared);

		reachedPoint = false;
		allyLocs = new FastLocSet();
		chaseDistance = -1;
		seesEnemies = false;

	}

	public void setPoint(MapLocation point) {
		this.point = point;
		bugger.endBug();
	}

	/**
	 * method to reset defend
	 * 
	 * @throws GameActionException
	 */
	public void clearDefend() throws GameActionException {
		myPasture = null;
		reachedPoint = false;
		chaseDistance = -1;
		pastureLoc = null;
		localCmd.clearChannel(); // remove lingering pasture commands
		deniedPasture = false;
	}

	public boolean defend() throws GameActionException {
		MapLocation cur = rc.getLocation();
		if (reachedPoint) {
			if (pastureLoc != null) {
				return defendPoint(pastureLoc, cur);
			}
			return defendPoint(point, cur);
		} else {
			// rc.setIndicatorString(2, "far");
			return defendMove(cur);
		}
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

	// defending point;
	public boolean defendPoint(MapLocation target, MapLocation cur)
			throws GameActionException {
		Robot[] ens = sensor.getBots(Sensor.SENSE_RANGE_ENEMIES);
		if (ens.length == 0) {
			// NO ENEMIES
			seesEnemies = false;
			chaseDistance = -1; // reset chase distance
			if (!rc.isActive()) {
				// look for nearby pasture
				if (myPasture == null) {
					MapLocation[] pastrLocs = rc.sensePastrLocations(team);
					for (int i = pastrLocs.length; --i >= 0;) {
						if (point.distanceSquaredTo(pastrLocs[i]) < c.attackRadiusSquared) {
							Robot[] pastrs = rc.senseNearbyGameObjects(
									Robot.class, pastrLocs[i], 0, team);
							for (int j = pastrs.length; --j >= 0;) {
								if (rc.senseRobotInfo(pastrs[j]).type == RobotType.PASTR) {
									myPasture = pastrs[j];
									pastureLoc = rc.senseRobotInfo(myPasture).location;
									target = pastureLoc;
									bugger.startBug(pastureLoc);
									break;
								}
							}
							break;
						}
					}
				} else {
					if (!updatePastureStatus()) {
						// pasture died. stop defending.
						return false;
					}
					return true;
				}
				return true;
			}
			if (localCmd.readMsg()) {
				// rc.setIndicatorString(1, localCmd.lastMsg.toString() + ": " +
				// localCmd.lastLoc.toString() + " Round: "+
				// Clock.getRoundNum());
				if (localCmd.lastMsg == MessageType.REPOSITION) {
					if (bugger.to != localCmd.lastLoc) {
						bugger.startBug(localCmd.lastLoc);

					}
					bug();
					return true;
				}
				if (localCmd.lastMsg == MessageType.SNEAK_OUT) {
					if (cur.distanceSquaredTo(localCmd.lastLoc) >= MAX_SNEAK_DISTANCE) {
						return true;
					}
					bugger.endBug();
					Direction dir = localCmd.lastLoc.directionTo(cur);
					if (rc.canMove(dir)) {
						rc.sneak(dir);
						return true;
					}
					Direction left = dir.rotateLeft();
					if (rc.canMove(left)) {
						rc.sneak(left);
						return true;
					}
					Direction right = dir.rotateRight();
					if (rc.canMove(right)) {
						rc.sneak(right);
						return true;
					}
					return true;
				}
			}
			// get closer to the point
			if (!bugger.bugging) {
				bugger.startBug(target);
			}
			moveToPoint(target, cur);
			return true;
		}
		// ENEMIES SENSED
		seesEnemies = true;
		bugger.endBug();
		Robot[] atkRngEnemies = sensor.getBots(Sensor.ATTACK_RANGE_ENEMIES);
		if (atkRngEnemies.length == 0) {
			localResponse.broadcastMsg(MessageType.NEARBY_ENEMY, cur);
			if (!updatePastureStatus()) {
				// pasture died. stop defending.
				return false;
			}
			if (!rc.isActive()) {
				return true;
			}
			if (denyPasture()) {
				return false;
				// PASTR DEAD, STOP DEFENDING LOCATION, BROADCAST BUT DON"T ACT
			}
			if (chaseDistance < 0) { // set chase range
				chaseDistance = cur.distanceSquaredTo(target) + 16; // MAGIC
																	// CONSTANT
																	// chase
																	// range
			}
			if (localCmd.readMsg()) {
				if (localCmd.lastMsg == MessageType.RETREAT) {
					if (bugger.to != localCmd.lastLoc) {
						bugger.startBug(localCmd.lastLoc);
					}
					Direction dir = bugger.bug();
					if (dir == Direction.OMNI || dir == Direction.NONE) {
						return true;
					}
					MapLocation desired = cur.add(dir);
					if (checkStepIn(desired)) {
						rc.move(dir);
					}
					return true;
				}
			}
			// TRY TO INTERCEPT ENEMY;
			interceptMove(target, cur, ens);
			return true;
		}
		// TODO semi-Close range

		// CLOSE ENEMIES IN RANGE
		localResponse.broadcastMsg(MessageType.ENGAGING_ENEMY, cur);
		if (!updatePastureStatus()) {
			// pasture died. stop defending.
			return false;
		}
		if (!rc.isActive()) {
			if (willDie(atkRngEnemies)) {
				if (suicide.explode(0)) {
					rc.selfDestruct();
				}
			}
			Robot[] adjEnemies = sensor.getBots(Sensor.ADJACENT_ENEMIES);
			Direction dir = getSuicideDir(adjEnemies);
			if (dir == Direction.OMNI) {
				rc.selfDestruct();
			}
			return true;
		}
		if (denyPasture()) {
			return false;
			// PASTR DEAD, STOP DEFENDING LOCATION, BROADCAST BUT DON"T ACT
		}

		Robot[] semiCloseEnemies = sensor.getBots(Sensor.SEMI_CLOSE_ENEMIES);
		if (semiCloseEnemies.length == 0) {
			return defendAttack(cur);
		}

		Robot[] closeEnemies = sensor.getBots(Sensor.CLOSE_ENEMIES);
		if (closeEnemies.length == 0) {
			return defendSemiClose(semiCloseEnemies);
		}

		Robot[] adjEnemies = sensor.getBots(Sensor.ADJACENT_ENEMIES);
		if (adjEnemies.length == 0) {
			return defendClose(closeEnemies);
		}
		return defendAdjacent(adjEnemies);
	}

	// //////////////////////// FIGHTING /////////////////////////////
	/**
	 * Deal with enemies at range
	 * 
	 * @return
	 * @throws GameActionException
	 */
	public boolean defendAttack(MapLocation cur) throws GameActionException {

		// fight
		RobotInfo targetInfo = attacker.attackOptimal();
		if (targetInfo == null) {
			// SHOULD NEVER HAPPEN
			return false;
		}
		MapLocation target = targetInfo.location;

		Robot[] allies = rc.senseNearbyGameObjects(Robot.class, target,
				c.attackRadiusSquared, team);
		Robot[] enemies = sensor.getBots(Sensor.ATTACK_RANGE_ENEMIES);
		int numEns = enemies.length;
		int numAttacks = allies.length + 1;
		double myHealth = rc.getHealth() / c.attackPower;
		double targetHealth = targetInfo.health / c.attackPower;

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
			// take killing shot
			rc.attackSquare(target);
			return true;
		}

		if (myHealth <= numEns
				|| (Math.ceil(myHealth / numEns) < Math.ceil(targetHealth
						/ numAttacks) && numEns >= numAttacks)) {
			// don't die, or take bad trade. back away
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
						attackRadiusSquared, enemy).length;
				if (ens < myHealth && ens < numEns) {
					rc.move(move);
					return true;
				}
			}
		}

		rc.attackSquare(target);
		return true;
	}

	public boolean defendSemiClose(Robot[] enemies) throws GameActionException {
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
			return false;
		}
		MapLocation target = targetInfo.location;
		double targetHealth = targetInfo.health / c.attackPower;
		int numEns = attacker.numSoldiers;
		MapLocation cur = rc.getLocation();
		double myHealth = rc.getHealth() / c.attackPower;

		// CHECK FOR 2-1 suiciders
		RobotInfo[] enemiesInfo = sensor.info.get(enemies);
		for (int i = enemies.length; --i >= 0;) {
			RobotInfo enInf = enemiesInfo[i];
			MapLocation enLoc = enInf.location;
			Direction suicideDir = enLoc.directionTo(cur);
			MapLocation adj = enLoc.add(suicideDir, 2);
			Robot ally = (Robot) rc.senseObjectAtLocation(adj);
			if (ally != null && ally.getTeam() == team) {
				Direction backOff = m.moveInDir(getStraightBackOff(cur, enLoc));
				if (backOff != Direction.NONE) {
					MapLocation newLoc = cur.add(backOff);
					int newEns = rc.senseNearbyGameObjects(Robot.class, newLoc,
							attackRadiusSquared, enemy).length;
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
				attackRadiusSquared, team);
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
						attackRadiusSquared, enemy).length;
				if (newEns < myHealth && newEns < numEns) {
					rc.move(move);
					return true;
				}
			}
		}

		rc.attackSquare(target);
		return true;
	}

	public boolean defendClose(Robot[] enemies) throws GameActionException {

		// back up cause they gonna blow
		MapLocation enemy = rc.senseRobotInfo(enemies[0]).location;
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

	public boolean defendAdjacent(Robot[] enemies) throws GameActionException {
		// check if suicide
		if (suicide.explode(0)) {
			rc.selfDestruct();
		}

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

		return true;
	}

	// //////////////////INTERCEPTING/////////////////////////////////
	/**
	 * Try to go between enemy and target location
	 * 
	 * @target pastureLoc or point defending
	 * @return whether move was made
	 * @throws GameActionException
	 */
	public boolean interceptMove(MapLocation target, MapLocation cur,
			Robot[] ens) throws GameActionException {
		if (!sensor.infoInitSenseEnemies) {
			sensor.initSenseEnemyInfo();
		}
		int numEns = ens.length;
		RobotInfo[] enInfos = sensor.getInfo(ens);
		int enXLoc = 0;
		int enYLoc = 0;

		// weighted enemy location
		// for(int i = numEns; --i >= 0;){
		// enXLoc += enInfos[i].location.x;
		// enYLoc += enInfos[i].location.y;
		// }
		// enXLoc = enXLoc/numEns;
		// enYLoc = enYLoc/numEns;

		// closest enemy location
		int closestDistance = 100;
		int index = 0;
		for (int i = numEns; --i >= 0;) {
			int distance = cur.distanceSquaredTo(enInfos[i].location);
			if (distance < closestDistance) {
				closestDistance = distance;
				index = i;
			}
		}
		enXLoc = enInfos[index].location.x;
		enYLoc = enInfos[index].location.y;
		if (rc.getHealth() <= 30)
			;

		Direction dir = cur.directionTo(new MapLocation(enXLoc, enYLoc));
		if (rc.getHealth() <= 30) {
			dir = dir.opposite();
			// BACKUP WHEN LOW
		}
		if (cur.directionTo(target).opposite() == dir) {
			// between weighted enemy and base
			MapLocation desired = cur.add(dir);
			boolean canDesired = rc.canMove(dir)
					&& !HQDangerSquares.contains(desired);
			Direction left = dir.rotateLeft();
			MapLocation desiredLeft = cur.add(left);
			boolean canLeft = rc.canMove(left)
					&& !HQDangerSquares.contains(desiredLeft);
			Direction right = dir.rotateRight();
			MapLocation desiredRight = cur.add(right);
			boolean canRight = rc.canMove(right)
					&& !HQDangerSquares.contains(desiredRight);
			if (!canDesired && !canLeft && !canRight) {
				TerrainTile desiredTile = rc.senseTerrainTile(desired);
				TerrainTile leftTile = rc.senseTerrainTile(desiredLeft);
				TerrainTile rightTile = rc.senseTerrainTile(desiredRight);
				if (desiredTile == TerrainTile.VOID
						&& leftTile == TerrainTile.VOID
						&& rightTile == TerrainTile.VOID) {
					localResponse.broadcastMsg(MessageType.CANT_FORWARD, cur);
				}
			}
			if (canDesired && checkStepIn(desired)
					&& desired.distanceSquaredTo(target) < chaseDistance) {
				rc.move(dir); // move towards enemy
				return true;
			}
			if (canLeft && checkStepIn(desiredLeft)
					&& desiredLeft.distanceSquaredTo(target) < chaseDistance) {
				rc.move(left);
				return true;
			}
			if (canRight && checkStepIn(desiredRight)
					&& desiredRight.distanceSquaredTo(target) < chaseDistance) {
				rc.move(right);
				return true;
			}
			return false; // hold
		}
		// get weighted location between point and enemies
		int nearenXLoc = (enXLoc + target.x) / 2;
		int nearenYLoc = (enYLoc + target.y) / 2;
		dir = cur.directionTo(new MapLocation(nearenXLoc, nearenYLoc));
		if (rc.getHealth() <= 30) {
			// BACK UP WHEN LOW
			dir = cur.directionTo(target);
		}

		MapLocation desired = cur.add(dir);
		// rc.setIndicatorString(2, dir.toString());
		boolean canDesired = rc.canMove(dir)
				&& !HQDangerSquares.contains(desired);
		Direction left = dir.rotateLeft();
		MapLocation desiredLeft = cur.add(left);
		boolean canLeft = rc.canMove(left)
				&& !HQDangerSquares.contains(desiredLeft);
		Direction right = dir.rotateRight();
		MapLocation desiredRight = cur.add(right);
		boolean canRight = rc.canMove(right)
				&& !HQDangerSquares.contains(desiredRight);

		// //Try point closer to HQ
		// enXLoc = (enXLoc + 3*target.x)/4;
		// enYLoc = (enYLoc + 3*target.y)/4;
		// Direction towardsHQ = cur.directionTo(new MapLocation(enXLoc,
		// enYLoc));
		// MapLocation desiredHQ = cur.add(towardsHQ);
		// boolean canHQ = rc.canMove(towardsHQ) &&
		// !HQDangerSquares.contains(desiredHQ);
		//

		if (!canDesired && !canLeft && !canRight) {
			TerrainTile desiredTile = rc.senseTerrainTile(desired);
			TerrainTile leftTile = rc.senseTerrainTile(desiredLeft);
			TerrainTile rightTile = rc.senseTerrainTile(desiredRight);
			// TerrainTile hqTile = rc.senseTerrainTile(desiredHQ);
			if (desiredTile == TerrainTile.VOID && leftTile == TerrainTile.VOID
					&& rightTile == TerrainTile.VOID) {
				localResponse.broadcastMsg(MessageType.CANT_FORWARD, cur);
			}
		}
		if (canDesired && checkStepIn(desired)
				&& desired.distanceSquaredTo(target) < chaseDistance) {
			rc.move(dir); // move towards enemy
			return true;
		}
		if (canLeft && checkStepIn(desiredLeft)
				&& desiredLeft.distanceSquaredTo(target) < chaseDistance) {
			rc.move(left);
			return true;
		}
		if (canRight && checkStepIn(desiredRight)
				&& desiredRight.distanceSquaredTo(target) < chaseDistance) {
			rc.move(right);
			return true;
		}
		// if(canHQ && checkStepIn(desiredHQ) &&
		// desiredHQ.distanceSquaredTo(target) < chaseDistance){
		// rc.move(towardsHQ);
		// return true;
		// }
		if (rc.getHealth() <= 50) {
			// BACK UP WHEN LOW
			dir = cur.directionTo(target);
			desired = cur.add(dir);
			canDesired = rc.canMove(dir) && !HQDangerSquares.contains(desired);
			if (canDesired && checkStepIn(desired)) {
				rc.move(dir); // move back
				return true;
			}

		}
		return false;
	}

	/**
	 * check whether if stepping in would put us in a bad fight
	 * 
	 * @param loc
	 * @return
	 * @throws GameActionException
	 */
	public boolean checkStepIn(MapLocation loc) throws GameActionException {
		if (!sensor.infoInitSenseEnemies) {
			sensor.initSenseEnemyInfo();
		}
		Robot[] ens = rc.senseNearbyGameObjects(Robot.class, loc,
				attackRadiusSquared, enemy);
		int numEns = ens.length;

		if (numEns == 0) {
			return true;
		}
		Robot[] enemiesPlus = rc.senseNearbyGameObjects(Robot.class, loc,
				attackPlusRadiusSquared, enemy);
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
						Robot.class, rInfo.location, attackRadiusSquared, team);
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
				bestBotInfo.location, attackPlusRadiusSquared, team);
		MapLocation bestBotLoc = bestBotInfo.location;

		RobotInfo[] allyInfos = sensor.info.get(allies);
		for (int i = allies.length; --i >= 0;) {
			RobotInfo allyInfo = allyInfos[i];
			// check if they are in range of the bestBot
			MapLocation theirLoc = allyInfo.location;
			if (allyInfo.actionDelay > 1) {
				continue;
			}
			double theirHealth = allyInfo.health / c.attackPower;
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
								enemy);
						int numTheirEns = theirEns.length;
						if (theirHealth > 2 * numTheirEns && theirHealth > 50
								&& allyInfo.type == RobotType.SOLDIER) {
							// probably will step in
							stepInChange++;
						}
					}
					continue;
				}

				if (rc.senseTerrainTile(stepInSquare) != TerrainTile.VOID
						&& !stepInSquare.equals(loc)
						&& rc.senseObjectAtLocation(loc) == null) {
					Robot[] theirEns = rc.senseNearbyGameObjects(Robot.class,
							stepInSquare, attackRadiusSquared, enemy);
					int numTheirEns = theirEns.length;
					if (theirHealth > 2 * numTheirEns && theirHealth > 50
							&& allyInfo.type == RobotType.SOLDIER) {
						// probably will step in
						stepInChange++;
					}
				}
				continue;
			} else {
				Robot[] theirEns = rc.senseNearbyGameObjects(Robot.class,
						theirLoc, attackRadiusSquared, enemy);
				int numTheirEns = theirEns.length;
				if (theirHealth <= numTheirEns
						|| allyInfo.type != RobotType.SOLDIER) {
					// probably stepping out
					numAlliedAttackers--;
				}
			}
		}
		numAlliedAttackers += stepInChange;
		// Check if stepping into bad position (numEns>numAlliedAttackers+1) or
		// I die before enemy
		// rc.setIndicatorString(0, "Round: " +
		// Clock.getRoundNum()+" num ens Plus: " + numEnsPlus +
		// " numAlliedAttackers: " + numAlliedAttackers +
		// " myHealth: " + myHealth + " theirHealth: " +
		// bestBotInfo.health/attackPower);

		if (myHealth <= numEnsPlus
				|| (Math.ceil(myHealth / numEnsPlus) < Math
						.ceil(bestBotInfo.health / attackPower
								/ numAlliedAttackers) && numEnsPlus >= numAlliedAttackers)) {
			// rc.setIndicatorString(1, "Round:" + Clock.getRoundNum()
			// +" lose step in to " + loc.toString());
			return false;
		}
		// rc.setIndicatorString(1, "Round:" + Clock.getRoundNum()
		// +" Win step in to " + loc.toString());

		return true;

		// //TODO change logic for stepping in
		// Robot[] adjAllies = sensor.getBots(Sensor.ADJACENT_ALLIES);
		// if(numEns > adjAllies.length ){
		// return false;
		// }
		// return true;
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
	 * Check for closer (meaning can be changed) empty square to loc If so, bug
	 * towards loc. When close to loc, just try 3 dir towards it
	 * 
	 * @return whether a move was made
	 * @throws GameActionException
	 */
	public boolean moveToPoint(MapLocation loc, MapLocation cur)
			throws GameActionException {
		allyLocs.clear();
		int distanceToLoc = cur.distanceSquaredTo(loc) - 1;
		if (distanceToLoc < 3) { // MAGIC CONSTANT cutoff
			Direction dir = cur.directionTo(loc);
			if (rc.canMove(dir)) {
				rc.move(dir);
				return true;
			}
			Direction left = dir.rotateLeft();
			if (rc.canMove(left)) {
				rc.move(left);
				return true;
			}
			Direction right = dir.rotateRight();
			if (rc.canMove(right)) {
				rc.move(right);
				return true;
			}
			return false;
		}
		MapLocation[] closer = MapLocation.getAllMapLocationsWithinRadiusSq(
				loc, distanceToLoc);
		int numLocs = closer.length;

		Robot[] allies = rc.senseNearbyGameObjects(Robot.class, loc,
				distanceToLoc, team);
		for (int i = allies.length; --i >= 0;) {
			allyLocs.add(rc.senseLocationOf(allies[i]));
		}
		int index = Clock.getRoundNum();
		for (int i = 4; --i >= 0;) { // MAGIC CONSTANT #squares to check
			index += LARGE_PRIME;
			MapLocation close = closer[index % numLocs];
			TerrainTile t = rc.senseTerrainTile(close);
			if (t == TerrainTile.VOID || t == TerrainTile.OFF_MAP) {
				continue;
			}
			if (allyLocs.contains(close)) {
				continue;
			}
			return bug();
		}
		return false;
	}

	/**
	 * update pasture and pasture loc to null if can't sense its location
	 * anymore.
	 * 
	 * @return whether pasture has died
	 * @throws GameActionException
	 */
	public boolean updatePastureStatus() throws GameActionException {
		if (myPasture != null) {
			boolean found = false;
			MapLocation[] pastrLocs = rc.sensePastrLocations(team);
			for (int i = pastrLocs.length; --i >= 0;) {
				if (pastrLocs[i] == pastureLoc) {
					found = true;
					break;
				}
			}
			if (!found) {
				// pasture dead
				localCmd.clearChannel();
				myPasture = null;
				pastureLoc = null;
				return false;
			}
		}
		return true;
	}

	/**
	 * Check if pasture is 1 hit. Will attack square if in range. No check on if
	 * we can attack
	 * 
	 * @return whether pasture was attacked.
	 * @throws GameActionException
	 */
	public boolean denyPasture() throws GameActionException {
		if (myPasture != null) {
			if (rc.canAttackSquare(pastureLoc)) {
				if (rc.senseRobotInfo(myPasture).health <= c.attackPower) {
					rc.attackSquare(pastureLoc);
					deniedPasture = true;
					myPasture = null;
					pastureLoc = null;
					localCmd.clearChannel();
					return true;
				}
			}
		}
		return false;
	}

	// try to get to defend point asap without fighting
	public boolean defendMove(MapLocation cur) throws GameActionException {
		if (rc.canSenseSquare(this.point)) {
			reachedPoint = true;
			return defendPoint(point, cur);
		}
		Robot[] ens = sensor.getBots(Sensor.SENSE_RANGE_ENEMIES);
		if (ens.length == 0) {
			// NO ENEMIES
			seesEnemies = false;
			if (!rc.isActive()) {
				return true;
			}
			bug();
			return true;
		}
		seesEnemies = true;
		Robot[] atkRngEnemies = sensor.getBots(Sensor.ATTACK_RANGE_ENEMIES);
		if (atkRngEnemies.length == 0) {
			// SENSE RANGE ENEMIES
			if (!rc.isActive()) {
				return true;
			}
			// TRY TO AVOID ENEMY IF POSSIBLE;
			avoidMove(cur, ens);
			return true;
		}
		bugger.endBug();
		// ENEMIES IN RANGE
		if (!rc.isActive()) {
			if (willDie(atkRngEnemies)) {

				if (suicide.explode(0)) {
					rc.selfDestruct();
				}
			}
			Robot[] adjEnemies = sensor.getBots(Sensor.ADJACENT_ENEMIES);
			Direction dir = getSuicideDir(adjEnemies);
			if (dir == Direction.OMNI) {
				rc.selfDestruct();
			}
			return true;
		}

		Robot[] semiCloseEnemies = sensor.getBots(Sensor.SEMI_CLOSE_ENEMIES);
		if (semiCloseEnemies.length == 0) {
			return defendAttack(cur);
		}
		Robot[] closeEnemies = sensor.getBots(Sensor.CLOSE_ENEMIES);
		if (closeEnemies.length == 0) {
			return defendSemiClose(semiCloseEnemies);
		}

		Robot[] adjEnemies = sensor.getBots(Sensor.ADJACENT_ENEMIES);
		if (adjEnemies.length == 0) {
			return defendClose(closeEnemies);
		}

		return defendAdjacent(adjEnemies);
	}

	public boolean avoidMove(MapLocation cur, Robot[] ens)
			throws GameActionException {
		// NEAR WALL, probably good to bug around
		if (bugger.bugging && bugger.hugging) {

			// rc.setIndicatorString(1, "Round: " + Clock.getRoundNum() +
			// " bugging ");
			Direction dir = bugger.bug();
			if (dir == Direction.OMNI || dir == Direction.NONE) {
				// bad bugging
				return false;
			} else {
				MapLocation desired = cur.add(dir);
				if (!checkStepIn(desired)) {
					return false;
				}
				rc.move(dir);
				return true;
			}
		}
		bugger.endBug();
		Direction dir = getAvoidDir(ens, cur);
		if (dir == Direction.NONE) {// No easy way around enemies

			dir = cur.directionTo(point);
			MapLocation dirLoc = cur.add(dir);
			if (rc.canMove(dir) && checkStepIn(dirLoc)) {
				if (!HQDangerSquares.contains(dirLoc)) {
					rc.move(dir);
					return true;
				} else {
					if (!m.inSplashRadius(dirLoc)) {
						rc.move(dir);
						return true;
					}
				}
			}

			Direction left = dir.rotateLeft();
			MapLocation leftLoc = cur.add(left);
			if (rc.canMove(left) && checkStepIn(leftLoc)) {
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
			Direction right = dir.rotateRight();
			MapLocation rightLoc = cur.add(right);
			if (rc.canMove(right) && checkStepIn(rightLoc)) {
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
			// rc.setIndicatorString(1, "Round: " + Clock.getRoundNum() +
			// " tried bugging ");
			bugger.startBug(point); // Maybe because of wall in the way?
			dir = bugger.bug();
			if (dir == Direction.NONE) {
				return false;
			}
			MapLocation desired = cur.add(dir);
			if (checkStepIn(desired)) {
				if (!HQDangerSquares.contains(desired)) {
					rc.move(dir);
					return true;
				} else {
					if (!m.inSplashRadius(desired)) {
						rc.move(dir);
						return true;
					}
				}
			}
			return false;
		} else {
			// rc.setIndicatorString(1, "Round: " + Clock.getRoundNum() +
			// " trying avoid");
			MapLocation desired = cur.add(dir);
			if (checkStepIn(desired)) {
				if (!HQDangerSquares.contains(desired)) {
					rc.move(dir);
					return true;
				} else {
					if (!m.inSplashRadius(desired)) {
						rc.move(dir);
						return true;
					}
				}
			}
			bugger.startBug(point); // Maybe because of wall in the way?
			dir = bugger.bug();
			if (dir == Direction.NONE) {
				return false;
			}

			desired = cur.add(dir);
			if (checkStepIn(desired)) {
				if (!HQDangerSquares.contains(desired)) {
					rc.move(dir);
					return true;
				} else {
					if (!m.inSplashRadius(desired)) {
						rc.move(dir);
						return true;
					}
				}
			}
			return false;
		}
	}

	public Direction getAvoidDir(Robot[] ens, MapLocation cur)
			throws GameActionException {
		if (!sensor.infoInitSenseEnemies) {
			sensor.initSenseEnemyInfo();
		}
		int remaining_directions = 7; // binary rep of directions remaining:
										// main, left right
		Direction main = cur.directionTo(point);
		if (!rc.canMove(main)) {
			remaining_directions = 3;
		}
		Direction left = main.rotateLeft();
		if (!rc.canMove(left)) {
			remaining_directions = remaining_directions - 2;
		}
		Direction right = main.rotateRight();
		if (!rc.canMove(right)) {
			remaining_directions--;
		}
		RobotInfo[] enInfo = sensor.info.get(ens);

		// check enemy robots for constraints
		for (int i = enInfo.length; --i >= 0;) {
			MapLocation enLoc = enInfo[i].location;
			Direction enDir = cur.directionTo(enLoc);
			switch (remaining_directions) {
			case 7: {
				if (enDir == main) {
					remaining_directions = 3;
					// check which side its on
					if (main.isDiagonal()) {
						enDir = cur.add(main, 2).directionTo(enLoc);
						if (enDir == left) {
							return right;
						} else if (enDir == right) {
							return left;
						} else {
							break;
						}
					} else {
						enDir = cur.add(main, 3).directionTo(enLoc);
						if (enDir == left) {
							return right;
						} else if (enDir == right) {
							return left;
						} else {
							break;
						}
					}
				}
				if (enDir == left) {
					remaining_directions = 5;
					break;
				}
				if (enDir == right) {
					remaining_directions = 6;
					break;
				}
				break;
			}
			case 6: {
				if (enDir == main) {
					return left;
				}
				if (enDir == left) {
					return main;
				}
				break;
			}
			case 5: {
				if (enDir == main) {
					return right;
				}
				if (enDir == right) {
					return main;
				}
				break;
			}
			case 3: {
				if (enDir == left) {
					return right;
				}
				if (enDir == right) {
					return left;
				}
				break;
			}
			case 4:
				return main;
			case 2:
				return left;
			case 1:
				return right;
			default:
				return Direction.NONE;
			}
		}
		if (remaining_directions > 3) {
			return main;
		} else {
			if (remaining_directions == 2) {
				return left; // TO DO randomly chose left over right in the case
								// we have only these 2 options
			}
			if (remaining_directions != 0) {
				return right;
			}
		}
		return Direction.NONE;
	}

	public static int calcSplashRadiusSq(int x) {
		int sp = Controller.SPLASH_RADIUS_SQ;
		// (sqrt(x)+sqrt(2))^2 <= x + ceil(sqrt(2x)*2) + 2
		return x + ((int) Math.ceil((Math.sqrt(sp * x) * sp) + 1)) + sp;
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
		if (numEns >= rc.getHealth() / c.attackPower) {
			return true;
		}
		return false;
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
				* suicide.explodeAt(cur, myHealth) + BONUS_FOR_GUARANTEE
				* myHealth;

		double SCALE = GameConstants.SELF_DESTRUCT_DAMAGE_FACTOR;
		double BASE = GameConstants.SELF_DESTRUCT_BASE_DAMAGE;
		boolean[] checked = new boolean[numToDir.length];
		double[] benefits = new double[numToDir.length];
		double[] damage = new double[numToDir.length];
		int[] kills = new int[numToDir.length];

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
						}
					} else {
						checked[index] = true;
						// check move
						int numEns = rc.senseNearbyGameObjects(Robot.class,
								loc, attackRadiusSquared, enemy).length;
						double dmgTaken = numEns * c.attackPower;
						double newHealth = myHealth - dmgTaken;
						damage[index] = (newHealth) * SCALE + BASE;
						if (newHealth > 0) {
							// check survival
							double benefit = suicide.explodeAt(loc, myHealth);
							kills[index] = suicide.kills;
							benefit = benefit - dmgTaken * BONUS_FOR_GUARANTEE;

							// check if we corner the enemy
							if (corners(loc, enLocation) || theirDelay > 1) {
								benefit += BONUS_FOR_GUARANTEE
										* Math.min(enInfo.health, damage[index]); // MAGIC
																					// CONSTANT
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
						}
					} else {
						checked[index] = true;
						// check move
						int numEns = rc.senseNearbyGameObjects(Robot.class,
								loc, attackRadiusSquared, enemy).length;
						double dmgTaken = numEns * c.attackPower;
						double newHealth = myHealth - dmgTaken;
						damage[index] = (newHealth) * SCALE + BASE;
						if (newHealth > 0) {
							// check survival
							double benefit = suicide.explodeAt(loc, myHealth);
							kills[index] = suicide.kills;
							benefit = benefit - dmgTaken * BONUS_FOR_GUARANTEE;
							// check if we corner the enemy
							if (corners(loc, enLocation) || theirDelay > 1) {
								benefit += BONUS_FOR_GUARANTEE
										* Math.min(enInfo.health, damage[index]); // MAGIC
																					// CONSTANT
							}
							benefits[index] = benefit;
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
						}
					} else {
						checked[index] = true;
						// check move
						int numEns = rc.senseNearbyGameObjects(Robot.class,
								loc, attackRadiusSquared, enemy).length;
						double dmgTaken = numEns * c.attackPower;
						double newHealth = myHealth - dmgTaken;
						damage[index] = (newHealth) * SCALE + BASE;
						if (newHealth > 0) {
							// check survival
							double benefit = suicide.explodeAt(loc, myHealth);
							kills[index] = suicide.kills;
							benefit = benefit - dmgTaken * BONUS_FOR_GUARANTEE;
							// check if we corner the enemy
							if (corners(loc, enLocation) || theirDelay > 1) {
								benefit += BONUS_FOR_GUARANTEE
										* Math.min(enInfo.health, damage[index]); // MAGIC
																					// CONSTANT
							}
							benefits[index] = benefit;
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
		if (bestBenefit <= myHealth * BONUS_FOR_GUARANTEE) { // MAGIC CONSTANT
																// threshold for
																// suicide.
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

}
