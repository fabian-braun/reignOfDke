package ext_paddlegoats;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Soldier extends Bot {

	static final double SUI_RELUC = 1;
	static final double FIRST_MOVER_RELUC = 0.9;
	static final double VA = 0.33;
	static final double VB = 0.33;

	static int caste;
	static int casteChannel = ORDERS + caste * ordersWidth;

	static MapLocation goal = new MapLocation(-1, -1);
	static int ntBuild;
	static int pastrBuild;

	void init() {

	}

	void run() throws GameActionException {
		log(0, String.valueOf(caste));
		pre();
		if (health > 20) {
			rc.broadcast(START - 1, 0); // so that PASTR-buiders aren't super
										// conspicuous.
		}
		if (active) {
			if (enemies.length == 0) {
				if (d2(goal) < 3) {
					if (ntBuild > 0) {
						construct(RobotType.NOISETOWER);
					} else if (pastrBuild > 0) {
						construct(RobotType.PASTR);
					}
				}
				Direction dir = path(goal);
				if (mode == Mode.PASTR && d2(goal) < 36) {
					sneak(dir);
				} else {
					move(dir);
				}
			} else {
				newmicro();
			}

		} else {
			pathingChecks();
		}
	}

	static void pre() throws GameActionException {
		if (round - birthday < 1) {
			caste = rc.readBroadcast(CASTE);
		}
		casteChannel = ORDERS + caste * ordersWidth;
		goal = readLoc(rc.readBroadcast(casteChannel + GOAL)); // log(0,goal.toString());
		ntBuild = rc.readBroadcast(casteChannel + NTREQ); // log(1,String.valueOf(ntBuild));
		pastrBuild = rc.readBroadcast(casteChannel + PASTRREQ);// log(2,String.valueOf(pastrBuild));
		mode = Mode.values()[rc.readBroadcast(MODE)];// log(0,mode.toString());
	}

	static MapLocation oldGoal = new MapLocation(-1, -1);
	static MapLocation tangent = new MapLocation(-1, -1);
	static int tracing = 0;
	static int tracingStartDist = 0;

	static Direction path(MapLocation goal) throws GameActionException {
		if (!oldGoal.equals(goal)) {
			tracing = 0;
			oldGoal = goal;
		}
		Direction dir = here.directionTo(goal);
		if (tracing == 0) {
			if (canMove(dir)) {
				return dir;
			} else {
				ind[1] += "startTrace";
				tracingStartDist = d2(goal);
				tangent = here.add(dir);
				Direction dir1 = dir;
				Direction dir2 = dir;
				for (int i = 8; --i > 0 && !canMove(dir1); dir1 = rotate(1,
						dir1)) {
				}
				for (int i = 8; --i > 0 && !canMove(dir2); dir2 = rotate(-1,
						dir2)) {
				}
				if (d2(here.add(dir1), goal) < d2(here.add(dir2), goal)) {
					tracing = 1;
					dir = dir1;
				} else {
					tracing = -1;
					dir = dir2;
				}
				return dir;
			}
		} else {
			dir = here.directionTo(tangent);
			if (canMove(dir)) {
				tracing = 0;
			}
			for (int i = 8; --i > 0 && !canMove(dir); dir = rotate(tracing, dir)) {
				;
			}
			tangent = here.add(rotate(-tracing, dir));
			if (d2(goal) <= tracingStartDist) {
				Direction dir2 = rotate(tracing, dir);
				if (canMove(dir2)
						&& d2(here.add(dir2), goal) < d2(goal, here.add(dir))
						&& d2(here.add(dir2), goal) < d2(goal)) {
					tracing = 0;
					return dir2;
				}
				dir2 = rotate(tracing, dir2);
				if (canMove(dir2)
						&& d2(here.add(dir2), goal) < d2(goal, here.add(dir))
						&& d2(here.add(dir2), goal) < d2(goal)) {
					tracing = 0;
					return dir2;
				}
			}
			return dir;
		}
	}

	static int roundsSinceNearPastr = 9999;

	static void pathingChecks() throws GameActionException {
		if (tracing != 0) {
			Direction toBorder;
			int w = here.x;
			int e = mapx - here.x;
			int n = here.y;
			int s = mapy - here.y;
			if (Math.min(n, s) < Math.min(e, w)) {
				if (n < s) {
					toBorder = Direction.NORTH;
				} else {
					toBorder = Direction.SOUTH;
				}
			} else {
				if (e < w) {
					toBorder = Direction.EAST;
				} else {
					toBorder = Direction.WEST;
				}
			}
			MapLocation there = here;
			check: for (int i = 0; i < 5; i++) {
				there = there.add(toBorder);
				switch (rc.senseTerrainTile(there)) {
				case NORMAL:
					break check;
				case OFF_MAP:
					tracing = 0;
					break check;
				default:
					break;
				}
			}
		}
		if (mode == Mode.PASTR && rc.sensePastrLocations(ourTeam).length > 0) {
			if (d2(goal) > 10) {
				roundsSinceNearPastr++;
			} else {
				roundsSinceNearPastr = 0;
			}
			if (d2(goal) > 20 && roundsSinceNearPastr < 20) {
				tracing = 0;
			}
		}

	}

	static void construct(RobotType t) throws GameActionException {
		switch (t) {
		case NOISETOWER:
			add(-1, casteChannel + NTREQ);
			rc.construct(RobotType.NOISETOWER);
			while (true) {
				add(1, QUEUEDNOISETOWER);
				rc.yield();
			}
		default:
			add(-1, casteChannel + PASTRREQ);
			rc.construct(RobotType.PASTR);
			while (true) {
				add(1, QUEUEDPASTR);
				rc.yield();
			}
		}
	}

	@SuppressWarnings("unused")
	static void newmicro() throws GameActionException {

		// getting shot threat
		double n = 0;
		double nw = 0;
		double ne = 0;
		double w = 0;
		double e = 0;
		double s = 0;
		double sw = 0;
		double se = 0;
		double h = 0;

		// shooting enemies opportunity
		double t_n = 0;
		double t_nw = 0;
		double t_ne = 0;
		double t_w = 0;
		double t_e = 0;
		double t_s = 0;
		double t_sw = 0;
		double t_se = 0;
		double t_h = 0;

		// potential-contact
		int pn = 0;
		int pnw = 0;
		int pne = 0;
		int pw = 0;
		int pe = 0;
		int ps = 0;
		int psw = 0;
		int pse = 0;
		int ph = 0;

		// suicide opportunities
		double s_n = 0;
		double s_nw = 0;
		double s_ne = 0;
		double s_w = 0;
		double s_e = 0;
		double s_s = 0;
		double s_sw = 0;
		double s_se = 0;
		double s_h = 0;

		// danger from enemy suicide bombers
		double sdng_n = 0;
		double sdng_nw = 0;
		double sdng_ne = 0;
		double sdng_w = 0;
		double sdng_e = 0;
		double sdng_s = 0;
		double sdng_sw = 0;
		double sdng_se = 0;
		double sdng_h = 0;

		int herePos = here.x * 1000 + here.y;
		double s_damage = health / 2 + 41;

		Robot[] enemies = rc.senseNearbyGameObjects(Robot.class, 25, enemyTeam);
		MapLocation[] friends = rc.senseBroadcastingRobotLocations(ourTeam);

		int[] friendPos = new int[friends.length];
		for (int i = friends.length; --i >= 0;) {
			friendPos[i] = friends[i].x * 1000 + friends[i].y;
		}

		double bestAttackScore = -9999999;
		MapLocation bestAttack = null;
		for (int i = enemies.length; --i >= 0;) {
			RobotInfo info = rc.senseRobotInfo(enemies[i]);
			double enemyHealth = info.health;

			if (rc.canAttackSquare(info.location)) {
				double score = 10 / enemyHealth;
				if (score > bestAttackScore) {
					bestAttackScore = score;
					bestAttack = info.location;
				}
			}

			int enemyPos = info.location.x * 1000 + info.location.y;
			// now for the ridiculously expensive part.
			double vuln = 0;
			double almostVuln = 0;

			for (int j = friendPos.length; --j >= 0;) {
				switch (friendPos[j] - enemyPos) {
				case -5005:
					break;
				case -5004:
					break;
				case -5003:
					break;
				case -5002:
					break;
				case -5001:
					break;
				case -5000:
					break;
				case -4999:
					break;
				case -4998:
					break;
				case -4997:
					break;
				case -4996:
					break;
				case -4995:
					break;
				case -4005:
					break;
				case -4004:
					break;
				case -4003:
					break;
				// etc...
				case -4002:
					almostVuln++;
					break;
				case -4001:
					almostVuln++;
					break;
				case -4000:
					almostVuln++;
					break;
				case -3999:
					almostVuln++;
					break;
				case -3998:
					almostVuln++;
					break;
				case -3997:
					break;
				case -3996:
					break;
				case -3995:
					break;
				case -3005:
					break;
				case -3004:
					break;
				case -3003:
					almostVuln++;
					break;
				case -3002:
					almostVuln++;
					break;
				case -3001:
					almostVuln++;
					vuln++;
					break;
				case -3000:
					almostVuln++;
					vuln++;
					break;
				case -2999:
					almostVuln++;
					vuln++;
					break;
				case -2998:
					almostVuln++;
					break;
				case -2997:
					almostVuln++;
					break;
				case -2996:
					break;
				case -2995:
					break;
				case -2005:
					break;
				case -2004:
					almostVuln++;
					break;
				case -2003:
					almostVuln++;
					break;
				case -2002:
					almostVuln++;
					vuln++;
					break;
				case -2001:
					almostVuln++;
					vuln++;
					break;
				case -2000:
					almostVuln++;
					vuln++;
					break;
				case -1999:
					almostVuln++;
					vuln++;
					break;
				case -1998:
					almostVuln++;
					vuln++;
					break;
				case -1997:
					almostVuln++;
					break;
				case -1996:
					almostVuln++;
					break;
				case -1995:
					break;
				case -1005:
					break;
				case -1004:
					almostVuln++;
					break;
				case -1003:
					almostVuln++;
					vuln++;
					break;
				case -1002:
					almostVuln++;
					vuln++;
					break;
				case -1001:
					almostVuln++;
					vuln++;
					break;
				case -1000:
					almostVuln++;
					vuln++;
					break;
				case -999:
					almostVuln++;
					vuln++;
					break;
				case -998:
					almostVuln++;
					vuln++;
					break;
				case -997:
					almostVuln++;
					vuln++;
					break;
				case -996:
					almostVuln++;
					break;
				case -995:
					break;
				case -5:
					break;
				case -4:
					almostVuln++;
					break;
				case -3:
					almostVuln++;
					vuln++;
					break;
				case -2:
					almostVuln++;
					vuln++;
					break;
				case -1:
					almostVuln++;
					vuln++;
					break;
				case 0:
					almostVuln++;
					vuln++;
					break;
				case 1:
					almostVuln++;
					vuln++;
					break;
				case 2:
					almostVuln++;
					vuln++;
					break;
				case 3:
					almostVuln++;
					vuln++;
					break;
				case 4:
					almostVuln++;
					break;
				case 5:
					break;
				case 995:
					break;
				case 996:
					almostVuln++;
					break;
				case 997:
					almostVuln++;
					vuln++;
					break;
				case 998:
					almostVuln++;
					vuln++;
					break;
				case 999:
					almostVuln++;
					vuln++;
					break;
				case 1000:
					almostVuln++;
					vuln++;
					break;
				case 1001:
					almostVuln++;
					vuln++;
					break;
				case 1002:
					almostVuln++;
					vuln++;
					break;
				case 1003:
					almostVuln++;
					vuln++;
					break;
				case 1004:
					almostVuln++;
					break;
				case 1005:
					break;
				case 1995:
					break;
				case 1996:
					almostVuln++;
					break;
				case 1997:
					almostVuln++;
					break;
				case 1998:
					almostVuln++;
					vuln++;
					break;
				case 1999:
					almostVuln++;
					vuln++;
					break;
				case 2000:
					almostVuln++;
					vuln++;
					break;
				case 2001:
					almostVuln++;
					vuln++;
					break;
				case 2002:
					almostVuln++;
					vuln++;
					break;
				case 2003:
					almostVuln++;
					break;
				case 2004:
					almostVuln++;
					break;
				case 2005:
					break;
				case 2995:
					break;
				case 2996:
					break;
				case 2997:
					almostVuln++;
					break;
				case 2998:
					almostVuln++;
					break;
				case 2999:
					almostVuln++;
					vuln++;
					break;
				case 3000:
					almostVuln++;
					vuln++;
					break;
				case 3001:
					almostVuln++;
					vuln++;
					break;
				case 3002:
					almostVuln++;
					break;
				case 3003:
					almostVuln++;
					break;
				case 3004:
					break;
				case 3005:
					break;
				case 3995:
					break;
				case 3996:
					break;
				case 3997:
					break;
				case 3998:
					almostVuln++;
					break;
				case 3999:
					almostVuln++;
					break;
				case 4000:
					almostVuln++;
					break;
				case 4001:
					almostVuln++;
					break;
				case 4002:
					almostVuln++;
					break;
				case 4003:
					break;
				case 4004:
					break;
				case 4005:
					break;
				case 4995:
					break;
				case 4996:
					break;
				case 4997:
					break;
				case 4998:
					break;
				case 4999:
					break;
				case 5000:
					break;
				case 5001:
					break;
				case 5002:
					break;
				case 5003:
					break;
				case 5004:
					break;
				case 5005:
					break;
				}

			}
			// log(1,"H:" + String.valueOf(health));
			double magic = vuln * VA + almostVuln * VB;
			double totalVuln = -(10 / health) / (magic);
			if (info.type == RobotType.PASTR
					|| info.type == RobotType.NOISETOWER) {
				totalVuln = 0;
			}
			double targ = 10 * magic / Math.max(40, enemyHealth);

			if (true || info.type == RobotType.SOLDIER) {
				// log(1,info.location.toString() + "/" + String.valueOf(vuln) +
				// "/" + String.valueOf(almostVuln) + "/" +
				// String.valueOf(totalVuln) + "//");
				double s_d = s_damage / enemyHealth;
				double s_dng = -(enemyHealth / 2 + 41) / health;
				s_d = s_d > 1 ? 1 : s_d;
				switch (herePos - enemyPos) {
				case -5005:
					break;
				case -5004:
					break;
				case -5003:
					pse++;
					break;
				case -5002:
					pe++;
					pse++;
					break;
				case -5001:
					pne++;
					pe++;
					pse++;
					break;
				case -5000:
					pne++;
					pe++;
					pse++;
					break;
				case -4999:
					pne++;
					pe++;
					pse++;
					break;
				case -4998:
					pne++;
					pe++;
					break;
				case -4997:
					pne++;
					break;
				case -4996:
					break;
				case -4995:
					break;
				case -4005:
					break;
				case -4004:
					pse++;
					break;
				case -4003:
					pe++;
					ps++;
					pse++;
					break;
				case -4002:
					se += totalVuln;
					pne++;
					pe++;
					ps++;
					pse++;
					ph++;
					t_se = t_se > targ ? t_se : targ;
					break;
				case -4001:
					e += totalVuln;
					se += totalVuln;
					pn++;
					pne++;
					pe++;
					ps++;
					pse++;
					ph++;
					t_e = t_e > targ ? t_e : targ;
					t_se = t_se > targ ? t_se : targ;
					break;
				case -4000:
					ne += totalVuln;
					e += totalVuln;
					se += totalVuln;
					pn++;
					pne++;
					pe++;
					ps++;
					pse++;
					ph++;
					t_ne = t_ne > targ ? t_ne : targ;
					t_e = t_e > targ ? t_e : targ;
					t_se = t_se > targ ? t_se : targ;
					break;
				case -3999:
					ne += totalVuln;
					e += totalVuln;
					pn++;
					pne++;
					pe++;
					ps++;
					pse++;
					ph++;
					t_ne = t_ne > targ ? t_ne : targ;
					t_e = t_e > targ ? t_e : targ;
					break;
				case -3998:
					ne += totalVuln;
					pn++;
					pne++;
					pe++;
					pse++;
					ph++;
					t_ne = t_ne > targ ? t_ne : targ;
					break;
				case -3997:
					pn++;
					pne++;
					pe++;
					break;
				case -3996:
					pne++;
					break;
				case -3995:
					break;
				case -3005:
					pse++;
					break;
				case -3004:
					pe++;
					ps++;
					pse++;
					break;
				case -3003:
					se += totalVuln;
					pne++;
					pe++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_se = t_se > targ ? t_se : targ;
					break;
				case -3002:
					e += totalVuln;
					s += totalVuln;
					se += totalVuln;
					pn++;
					pne++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_e = t_e > targ ? t_e : targ;
					t_s = t_s > targ ? t_s : targ;
					t_se = t_se > targ ? t_se : targ;
					break;
				case -3001:
					ne += totalVuln;
					e += totalVuln;
					s += totalVuln;
					se += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_ne = t_ne > targ ? t_ne : targ;
					t_e = t_e > targ ? t_e : targ;
					t_s = t_s > targ ? t_s : targ;
					t_se = t_se > targ ? t_se : targ;
					t_h = t_h > targ ? t_h : targ;
					break;
				case -3000:
					n += totalVuln;
					ne += totalVuln;
					e += totalVuln;
					s += totalVuln;
					se += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_ne = t_ne > targ ? t_ne : targ;
					t_e = t_e > targ ? t_e : targ;
					t_s = t_s > targ ? t_s : targ;
					t_se = t_se > targ ? t_se : targ;
					t_h = t_h > targ ? t_h : targ;
					break;
				case -2999:
					n += totalVuln;
					ne += totalVuln;
					e += totalVuln;
					se += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_ne = t_ne > targ ? t_ne : targ;
					t_e = t_e > targ ? t_e : targ;
					t_se = t_se > targ ? t_se : targ;
					t_h = t_h > targ ? t_h : targ;
					break;
				case -2998:
					n += totalVuln;
					ne += totalVuln;
					e += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_ne = t_ne > targ ? t_ne : targ;
					t_e = t_e > targ ? t_e : targ;
					break;
				case -2997:
					ne += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pse++;
					ph++;
					t_ne = t_ne > targ ? t_ne : targ;
					break;
				case -2996:
					pn++;
					pne++;
					pe++;
					break;
				case -2995:
					pne++;
					break;
				case -2005:
					ps++;
					pse++;
					break;
				case -2004:
					se += totalVuln;
					pe++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_se = t_se > targ ? t_se : targ;
					break;
				case -2003:
					e += totalVuln;
					s += totalVuln;
					se += totalVuln;
					pn++;
					pne++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_e = t_e > targ ? t_e : targ;
					t_s = t_s > targ ? t_s : targ;
					t_se = t_se > targ ? t_se : targ;
					break;
				case -2002:
					s_se += s_d;
					ne += totalVuln;
					e += totalVuln;
					s += totalVuln;
					se += totalVuln;
					sw += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_ne = t_ne > targ ? t_ne : targ;
					t_e = t_e > targ ? t_e : targ;
					t_s = t_s > targ ? t_s : targ;
					t_se = t_se > targ ? t_se : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					t_h = t_h > targ ? t_h : targ;
					sdng_se += s_dng;
					break;
				case -2001:
					s_e += s_d;
					s_se += s_d;
					n += totalVuln;
					ne += totalVuln;
					e += totalVuln;
					w += totalVuln;
					s += totalVuln;
					se += totalVuln;
					sw += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_ne = t_ne > targ ? t_ne : targ;
					t_e = t_e > targ ? t_e : targ;
					t_w = t_w > targ ? t_w : targ;
					t_s = t_s > targ ? t_s : targ;
					t_se = t_se > targ ? t_se : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					t_h = t_h > targ ? t_h : targ;
					sdng_e += s_dng;
					sdng_se += s_dng;
					break;
				case -2000:
					s_ne += s_d;
					s_e += s_d;
					s_se += s_d;
					n += totalVuln;
					ne += totalVuln;
					nw += totalVuln;
					e += totalVuln;
					w += totalVuln;
					s += totalVuln;
					se += totalVuln;
					sw += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_ne = t_ne > targ ? t_ne : targ;
					t_nw = t_nw > targ ? t_nw : targ;
					t_e = t_e > targ ? t_e : targ;
					t_w = t_w > targ ? t_w : targ;
					t_s = t_s > targ ? t_s : targ;
					t_se = t_se > targ ? t_se : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					t_h = t_h > targ ? t_h : targ;
					sdng_ne += s_dng;
					sdng_e += s_dng;
					sdng_se += s_dng;
					break;
				// 50 cases later...
				case -1999:
					s_ne += s_d;
					s_e += s_d;
					n += totalVuln;
					ne += totalVuln;
					nw += totalVuln;
					e += totalVuln;
					w += totalVuln;
					s += totalVuln;
					se += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_ne = t_ne > targ ? t_ne : targ;
					t_nw = t_nw > targ ? t_nw : targ;
					t_e = t_e > targ ? t_e : targ;
					t_w = t_w > targ ? t_w : targ;
					t_s = t_s > targ ? t_s : targ;
					t_se = t_se > targ ? t_se : targ;
					t_h = t_h > targ ? t_h : targ;
					sdng_ne += s_dng;
					sdng_e += s_dng;
					break;
				case -1998:
					s_ne += s_d;
					n += totalVuln;
					ne += totalVuln;
					nw += totalVuln;
					e += totalVuln;
					se += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_ne = t_ne > targ ? t_ne : targ;
					t_nw = t_nw > targ ? t_nw : targ;
					t_e = t_e > targ ? t_e : targ;
					t_se = t_se > targ ? t_se : targ;
					t_h = t_h > targ ? t_h : targ;
					sdng_ne += s_dng;
					break;
				case -1997:
					n += totalVuln;
					ne += totalVuln;
					e += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_ne = t_ne > targ ? t_ne : targ;
					t_e = t_e > targ ? t_e : targ;
					break;
				case -1996:
					ne += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					ph++;
					t_ne = t_ne > targ ? t_ne : targ;
					break;
				case -1995:
					pn++;
					pne++;
					break;
				case -1005:
					ps++;
					pse++;
					psw++;
					break;
				case -1004:
					s += totalVuln;
					se += totalVuln;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_s = t_s > targ ? t_s : targ;
					t_se = t_se > targ ? t_se : targ;
					break;
				case -1003:
					e += totalVuln;
					s += totalVuln;
					se += totalVuln;
					sw += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_e = t_e > targ ? t_e : targ;
					t_s = t_s > targ ? t_s : targ;
					t_se = t_se > targ ? t_se : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					t_h = t_h > targ ? t_h : targ;
					break;
				case -1002:
					s_s += s_d;
					s_se += s_d;
					n += totalVuln;
					ne += totalVuln;
					e += totalVuln;
					w += totalVuln;
					s += totalVuln;
					se += totalVuln;
					sw += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_ne = t_ne > targ ? t_ne : targ;
					t_e = t_e > targ ? t_e : targ;
					t_w = t_w > targ ? t_w : targ;
					t_s = t_s > targ ? t_s : targ;
					t_se = t_se > targ ? t_se : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					t_h = t_h > targ ? t_h : targ;
					sdng_s += s_dng;
					sdng_se += s_dng;
					break;
				case -1001:
					s_e += s_d;
					s_s += s_d;
					s_se += s_d;
					s_h += s_d;
					n += totalVuln;
					ne += totalVuln;
					nw += totalVuln;
					e += totalVuln;
					w += totalVuln;
					s += totalVuln;
					se += totalVuln;
					sw += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_ne = t_ne > targ ? t_ne : targ;
					t_nw = t_nw > targ ? t_nw : targ;
					t_e = t_e > targ ? t_e : targ;
					t_w = t_w > targ ? t_w : targ;
					t_s = t_s > targ ? t_s : targ;
					t_se = t_se > targ ? t_se : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					t_h = t_h > targ ? t_h : targ;
					sdng_e += s_dng;
					sdng_s += s_dng;
					sdng_se += s_dng;
					sdng_h += s_dng;
					break;
				case -1000:
					s_n += s_d;
					s_ne += s_d;
					s_e += s_d;
					s_s += s_d;
					s_se += s_d;
					s_h += s_d;
					n += totalVuln;
					ne += totalVuln;
					nw += totalVuln;
					e += totalVuln;
					w += totalVuln;
					s += totalVuln;
					se += totalVuln;
					sw += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_ne = t_ne > targ ? t_ne : targ;
					t_nw = t_nw > targ ? t_nw : targ;
					t_e = t_e > targ ? t_e : targ;
					t_w = t_w > targ ? t_w : targ;
					t_s = t_s > targ ? t_s : targ;
					t_se = t_se > targ ? t_se : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					t_h = t_h > targ ? t_h : targ;
					sdng_n += s_dng;
					sdng_ne += s_dng;
					sdng_e += s_dng;
					sdng_s += s_dng;
					sdng_se += s_dng;
					sdng_h += s_dng;
					break;
				case -999:
					s_n += s_d;
					s_ne += s_d;
					s_e += s_d;
					s_h += s_d;
					n += totalVuln;
					ne += totalVuln;
					nw += totalVuln;
					e += totalVuln;
					w += totalVuln;
					s += totalVuln;
					se += totalVuln;
					sw += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_ne = t_ne > targ ? t_ne : targ;
					t_nw = t_nw > targ ? t_nw : targ;
					t_e = t_e > targ ? t_e : targ;
					t_w = t_w > targ ? t_w : targ;
					t_s = t_s > targ ? t_s : targ;
					t_se = t_se > targ ? t_se : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					t_h = t_h > targ ? t_h : targ;
					sdng_n += s_dng;
					sdng_ne += s_dng;
					sdng_e += s_dng;
					sdng_h += s_dng;
					break;
				case -998:
					s_n += s_d;
					s_ne += s_d;
					n += totalVuln;
					ne += totalVuln;
					nw += totalVuln;
					e += totalVuln;
					w += totalVuln;
					s += totalVuln;
					se += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_ne = t_ne > targ ? t_ne : targ;
					t_nw = t_nw > targ ? t_nw : targ;
					t_e = t_e > targ ? t_e : targ;
					t_w = t_w > targ ? t_w : targ;
					t_s = t_s > targ ? t_s : targ;
					t_se = t_se > targ ? t_se : targ;
					t_h = t_h > targ ? t_h : targ;
					sdng_n += s_dng;
					sdng_ne += s_dng;
					break;
				case -997:
					n += totalVuln;
					ne += totalVuln;
					nw += totalVuln;
					e += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_ne = t_ne > targ ? t_ne : targ;
					t_nw = t_nw > targ ? t_nw : targ;
					t_e = t_e > targ ? t_e : targ;
					t_h = t_h > targ ? t_h : targ;
					break;
				case -996:
					n += totalVuln;
					ne += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_ne = t_ne > targ ? t_ne : targ;
					break;
				case -995:
					pn++;
					pne++;
					pnw++;
					break;
				case -5:
					ps++;
					pse++;
					psw++;
					break;
				case -4:
					s += totalVuln;
					se += totalVuln;
					sw += totalVuln;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_s = t_s > targ ? t_s : targ;
					t_se = t_se > targ ? t_se : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					break;
				case -3:
					e += totalVuln;
					w += totalVuln;
					s += totalVuln;
					se += totalVuln;
					sw += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_e = t_e > targ ? t_e : targ;
					t_w = t_w > targ ? t_w : targ;
					t_s = t_s > targ ? t_s : targ;
					t_se = t_se > targ ? t_se : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					t_h = t_h > targ ? t_h : targ;
					break;
				case -2:
					s_s += s_d;
					s_se += s_d;
					s_sw += s_d;
					n += totalVuln;
					ne += totalVuln;
					nw += totalVuln;
					e += totalVuln;
					w += totalVuln;
					s += totalVuln;
					se += totalVuln;
					sw += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_ne = t_ne > targ ? t_ne : targ;
					t_nw = t_nw > targ ? t_nw : targ;
					t_e = t_e > targ ? t_e : targ;
					t_w = t_w > targ ? t_w : targ;
					t_s = t_s > targ ? t_s : targ;
					t_se = t_se > targ ? t_se : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					t_h = t_h > targ ? t_h : targ;
					sdng_s += s_dng;
					sdng_se += s_dng;
					sdng_sw += s_dng;
					break;
				case -1:
					s_e += s_d;
					s_w += s_d;
					s_s += s_d;
					s_se += s_d;
					s_sw += s_d;
					s_h += s_d;
					n += totalVuln;
					ne += totalVuln;
					nw += totalVuln;
					e += totalVuln;
					w += totalVuln;
					s += totalVuln;
					se += totalVuln;
					sw += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_ne = t_ne > targ ? t_ne : targ;
					t_nw = t_nw > targ ? t_nw : targ;
					t_e = t_e > targ ? t_e : targ;
					t_w = t_w > targ ? t_w : targ;
					t_s = t_s > targ ? t_s : targ;
					t_se = t_se > targ ? t_se : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					t_h = t_h > targ ? t_h : targ;
					sdng_e += s_dng;
					sdng_w += s_dng;
					sdng_s += s_dng;
					sdng_se += s_dng;
					sdng_sw += s_dng;
					sdng_h += s_dng;
					break;
				case 0:
					s_n += s_d;
					s_ne += s_d;
					s_nw += s_d;
					s_e += s_d;
					s_w += s_d;
					s_s += s_d;
					s_se += s_d;
					s_sw += s_d;
					s_h += s_d;
					n += totalVuln;
					ne += totalVuln;
					nw += totalVuln;
					e += totalVuln;
					w += totalVuln;
					s += totalVuln;
					se += totalVuln;
					sw += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_ne = t_ne > targ ? t_ne : targ;
					t_nw = t_nw > targ ? t_nw : targ;
					t_e = t_e > targ ? t_e : targ;
					t_w = t_w > targ ? t_w : targ;
					t_s = t_s > targ ? t_s : targ;
					t_se = t_se > targ ? t_se : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					t_h = t_h > targ ? t_h : targ;
					sdng_n += s_dng;
					sdng_ne += s_dng;
					sdng_nw += s_dng;
					sdng_e += s_dng;
					sdng_w += s_dng;
					sdng_s += s_dng;
					sdng_se += s_dng;
					sdng_sw += s_dng;
					sdng_h += s_dng;
					break;
				case 1:
					s_n += s_d;
					s_ne += s_d;
					s_nw += s_d;
					s_e += s_d;
					s_w += s_d;
					s_h += s_d;
					n += totalVuln;
					ne += totalVuln;
					nw += totalVuln;
					e += totalVuln;
					w += totalVuln;
					s += totalVuln;
					se += totalVuln;
					sw += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_ne = t_ne > targ ? t_ne : targ;
					t_nw = t_nw > targ ? t_nw : targ;
					t_e = t_e > targ ? t_e : targ;
					t_w = t_w > targ ? t_w : targ;
					t_s = t_s > targ ? t_s : targ;
					t_se = t_se > targ ? t_se : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					t_h = t_h > targ ? t_h : targ;
					sdng_n += s_dng;
					sdng_ne += s_dng;
					sdng_nw += s_dng;
					sdng_e += s_dng;
					sdng_w += s_dng;
					sdng_h += s_dng;
					break;
				case 2:
					s_n += s_d;
					s_ne += s_d;
					s_nw += s_d;
					n += totalVuln;
					ne += totalVuln;
					nw += totalVuln;
					e += totalVuln;
					w += totalVuln;
					s += totalVuln;
					se += totalVuln;
					sw += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_ne = t_ne > targ ? t_ne : targ;
					t_nw = t_nw > targ ? t_nw : targ;
					t_e = t_e > targ ? t_e : targ;
					t_w = t_w > targ ? t_w : targ;
					t_s = t_s > targ ? t_s : targ;
					t_se = t_se > targ ? t_se : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					t_h = t_h > targ ? t_h : targ;
					sdng_n += s_dng;
					sdng_ne += s_dng;
					sdng_nw += s_dng;
					break;
				case 3:
					n += totalVuln;
					ne += totalVuln;
					nw += totalVuln;
					e += totalVuln;
					w += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_ne = t_ne > targ ? t_ne : targ;
					t_nw = t_nw > targ ? t_nw : targ;
					t_e = t_e > targ ? t_e : targ;
					t_w = t_w > targ ? t_w : targ;
					t_h = t_h > targ ? t_h : targ;
					break;
				case 4:
					n += totalVuln;
					ne += totalVuln;
					nw += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_ne = t_ne > targ ? t_ne : targ;
					t_nw = t_nw > targ ? t_nw : targ;
					break;
				case 5:
					pn++;
					pne++;
					pnw++;
					break;
				case 995:
					ps++;
					pse++;
					psw++;
					break;
				case 996:
					s += totalVuln;
					sw += totalVuln;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_s = t_s > targ ? t_s : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					break;
				case 997:
					w += totalVuln;
					s += totalVuln;
					se += totalVuln;
					sw += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_w = t_w > targ ? t_w : targ;
					t_s = t_s > targ ? t_s : targ;
					t_se = t_se > targ ? t_se : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					t_h = t_h > targ ? t_h : targ;
					break;
				case 998:
					s_s += s_d;
					s_sw += s_d;
					n += totalVuln;
					nw += totalVuln;
					e += totalVuln;
					w += totalVuln;
					s += totalVuln;
					se += totalVuln;
					sw += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_nw = t_nw > targ ? t_nw : targ;
					t_e = t_e > targ ? t_e : targ;
					t_w = t_w > targ ? t_w : targ;
					t_s = t_s > targ ? t_s : targ;
					t_se = t_se > targ ? t_se : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					t_h = t_h > targ ? t_h : targ;
					sdng_s += s_dng;
					sdng_sw += s_dng;
					break;
				case 999:
					s_w += s_d;
					s_s += s_d;
					s_sw += s_d;
					s_h += s_d;
					n += totalVuln;
					ne += totalVuln;
					nw += totalVuln;
					e += totalVuln;
					w += totalVuln;
					s += totalVuln;
					se += totalVuln;
					sw += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_ne = t_ne > targ ? t_ne : targ;
					t_nw = t_nw > targ ? t_nw : targ;
					t_e = t_e > targ ? t_e : targ;
					t_w = t_w > targ ? t_w : targ;
					t_s = t_s > targ ? t_s : targ;
					t_se = t_se > targ ? t_se : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					t_h = t_h > targ ? t_h : targ;
					sdng_w += s_dng;
					sdng_s += s_dng;
					sdng_sw += s_dng;
					sdng_h += s_dng;
					break;
				case 1000:
					s_n += s_d;
					s_nw += s_d;
					s_w += s_d;
					s_s += s_d;
					s_sw += s_d;
					s_h += s_d;
					n += totalVuln;
					ne += totalVuln;
					nw += totalVuln;
					e += totalVuln;
					w += totalVuln;
					s += totalVuln;
					se += totalVuln;
					sw += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_ne = t_ne > targ ? t_ne : targ;
					t_nw = t_nw > targ ? t_nw : targ;
					t_e = t_e > targ ? t_e : targ;
					t_w = t_w > targ ? t_w : targ;
					t_s = t_s > targ ? t_s : targ;
					t_se = t_se > targ ? t_se : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					t_h = t_h > targ ? t_h : targ;
					sdng_n += s_dng;
					sdng_nw += s_dng;
					sdng_w += s_dng;
					sdng_s += s_dng;
					sdng_sw += s_dng;
					sdng_h += s_dng;
					break;
				case 1001:
					s_n += s_d;
					s_nw += s_d;
					s_w += s_d;
					s_h += s_d;
					n += totalVuln;
					ne += totalVuln;
					nw += totalVuln;
					e += totalVuln;
					w += totalVuln;
					s += totalVuln;
					se += totalVuln;
					sw += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_ne = t_ne > targ ? t_ne : targ;
					t_nw = t_nw > targ ? t_nw : targ;
					t_e = t_e > targ ? t_e : targ;
					t_w = t_w > targ ? t_w : targ;
					t_s = t_s > targ ? t_s : targ;
					t_se = t_se > targ ? t_se : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					t_h = t_h > targ ? t_h : targ;
					sdng_n += s_dng;
					sdng_nw += s_dng;
					sdng_w += s_dng;
					sdng_h += s_dng;
					break;
				case 1002:
					s_n += s_d;
					s_nw += s_d;
					n += totalVuln;
					ne += totalVuln;
					nw += totalVuln;
					e += totalVuln;
					w += totalVuln;
					s += totalVuln;
					sw += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_ne = t_ne > targ ? t_ne : targ;
					t_nw = t_nw > targ ? t_nw : targ;
					t_e = t_e > targ ? t_e : targ;
					t_w = t_w > targ ? t_w : targ;
					t_s = t_s > targ ? t_s : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					t_h = t_h > targ ? t_h : targ;
					sdng_n += s_dng;
					sdng_nw += s_dng;
					break;
				case 1003:
					n += totalVuln;
					ne += totalVuln;
					nw += totalVuln;
					w += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_ne = t_ne > targ ? t_ne : targ;
					t_nw = t_nw > targ ? t_nw : targ;
					t_w = t_w > targ ? t_w : targ;
					t_h = t_h > targ ? t_h : targ;
					break;
				case 1004:
					n += totalVuln;
					nw += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_nw = t_nw > targ ? t_nw : targ;
					break;
				case 1005:
					pn++;
					pne++;
					pnw++;
					break;
				case 1995:
					ps++;
					psw++;
					break;
				case 1996:
					sw += totalVuln;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_sw = t_sw > targ ? t_sw : targ;
					break;
				case 1997:
					w += totalVuln;
					s += totalVuln;
					sw += totalVuln;
					pn++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_w = t_w > targ ? t_w : targ;
					t_s = t_s > targ ? t_s : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					break;
				case 1998:
					s_sw += s_d;
					nw += totalVuln;
					w += totalVuln;
					s += totalVuln;
					se += totalVuln;
					sw += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_nw = t_nw > targ ? t_nw : targ;
					t_w = t_w > targ ? t_w : targ;
					t_s = t_s > targ ? t_s : targ;
					t_se = t_se > targ ? t_se : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					t_h = t_h > targ ? t_h : targ;
					sdng_sw += s_dng;
					break;
				case 1999:
					s_w += s_d;
					s_sw += s_d;
					n += totalVuln;
					nw += totalVuln;
					e += totalVuln;
					w += totalVuln;
					s += totalVuln;
					se += totalVuln;
					sw += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_nw = t_nw > targ ? t_nw : targ;
					t_e = t_e > targ ? t_e : targ;
					t_w = t_w > targ ? t_w : targ;
					t_s = t_s > targ ? t_s : targ;
					t_se = t_se > targ ? t_se : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					t_h = t_h > targ ? t_h : targ;
					sdng_w += s_dng;
					sdng_sw += s_dng;
					break;
				case 2000:
					s_nw += s_d;
					s_w += s_d;
					s_sw += s_d;
					n += totalVuln;
					ne += totalVuln;
					nw += totalVuln;
					e += totalVuln;
					w += totalVuln;
					s += totalVuln;
					se += totalVuln;
					sw += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_ne = t_ne > targ ? t_ne : targ;
					t_nw = t_nw > targ ? t_nw : targ;
					t_e = t_e > targ ? t_e : targ;
					t_w = t_w > targ ? t_w : targ;
					t_s = t_s > targ ? t_s : targ;
					t_se = t_se > targ ? t_se : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					t_h = t_h > targ ? t_h : targ;
					sdng_nw += s_dng;
					sdng_w += s_dng;
					sdng_sw += s_dng;
					break;
				case 2001:
					s_nw += s_d;
					s_w += s_d;
					n += totalVuln;
					ne += totalVuln;
					nw += totalVuln;
					e += totalVuln;
					w += totalVuln;
					s += totalVuln;
					sw += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_ne = t_ne > targ ? t_ne : targ;
					t_nw = t_nw > targ ? t_nw : targ;
					t_e = t_e > targ ? t_e : targ;
					t_w = t_w > targ ? t_w : targ;
					t_s = t_s > targ ? t_s : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					t_h = t_h > targ ? t_h : targ;
					sdng_nw += s_dng;
					sdng_w += s_dng;
					break;
				case 2002:
					s_nw += s_d;
					n += totalVuln;
					ne += totalVuln;
					nw += totalVuln;
					w += totalVuln;
					sw += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_ne = t_ne > targ ? t_ne : targ;
					t_nw = t_nw > targ ? t_nw : targ;
					t_w = t_w > targ ? t_w : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					t_h = t_h > targ ? t_h : targ;
					sdng_nw += s_dng;
					break;
				case 2003:
					n += totalVuln;
					nw += totalVuln;
					w += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					psw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_nw = t_nw > targ ? t_nw : targ;
					t_w = t_w > targ ? t_w : targ;
					break;
				case 2004:
					nw += totalVuln;
					pn++;
					pne++;
					pnw++;
					pw++;
					ph++;
					t_nw = t_nw > targ ? t_nw : targ;
					break;
				case 2005:
					pn++;
					pnw++;
					break;
				case 2995:
					psw++;
					break;
				case 2996:
					pw++;
					ps++;
					psw++;
					break;
				case 2997:
					sw += totalVuln;
					pnw++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_sw = t_sw > targ ? t_sw : targ;
					break;
				case 2998:
					w += totalVuln;
					s += totalVuln;
					sw += totalVuln;
					pn++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_w = t_w > targ ? t_w : targ;
					t_s = t_s > targ ? t_s : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					break;
				case 2999:
					nw += totalVuln;
					w += totalVuln;
					s += totalVuln;
					sw += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_nw = t_nw > targ ? t_nw : targ;
					t_w = t_w > targ ? t_w : targ;
					t_s = t_s > targ ? t_s : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					t_h = t_h > targ ? t_h : targ;
					break;
				case 3000:
					n += totalVuln;
					nw += totalVuln;
					w += totalVuln;
					s += totalVuln;
					sw += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_nw = t_nw > targ ? t_nw : targ;
					t_w = t_w > targ ? t_w : targ;
					t_s = t_s > targ ? t_s : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					t_h = t_h > targ ? t_h : targ;
					break;
				case 3001:
					n += totalVuln;
					nw += totalVuln;
					w += totalVuln;
					sw += totalVuln;
					h += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					pse++;
					psw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_nw = t_nw > targ ? t_nw : targ;
					t_w = t_w > targ ? t_w : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					t_h = t_h > targ ? t_h : targ;
					break;
				case 3002:
					n += totalVuln;
					nw += totalVuln;
					w += totalVuln;
					pn++;
					pne++;
					pnw++;
					pe++;
					pw++;
					ps++;
					psw++;
					ph++;
					t_n = t_n > targ ? t_n : targ;
					t_nw = t_nw > targ ? t_nw : targ;
					t_w = t_w > targ ? t_w : targ;
					break;
				case 3003:
					nw += totalVuln;
					pn++;
					pne++;
					pnw++;
					pw++;
					psw++;
					ph++;
					t_nw = t_nw > targ ? t_nw : targ;
					break;
				case 3004:
					pn++;
					pnw++;
					pw++;
					break;
				case 3005:
					pnw++;
					break;
				case 3995:
					break;
				case 3996:
					psw++;
					break;
				case 3997:
					pw++;
					ps++;
					psw++;
					break;
				case 3998:
					sw += totalVuln;
					pnw++;
					pw++;
					ps++;
					psw++;
					ph++;
					t_sw = t_sw > targ ? t_sw : targ;
					break;
				case 3999:
					w += totalVuln;
					sw += totalVuln;
					pn++;
					pnw++;
					pw++;
					ps++;
					psw++;
					ph++;
					t_w = t_w > targ ? t_w : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					break;
				case 4000:
					nw += totalVuln;
					w += totalVuln;
					sw += totalVuln;
					pn++;
					pnw++;
					pw++;
					ps++;
					psw++;
					ph++;
					t_nw = t_nw > targ ? t_nw : targ;
					t_w = t_w > targ ? t_w : targ;
					t_sw = t_sw > targ ? t_sw : targ;
					break;
				case 4001:
					nw += totalVuln;
					w += totalVuln;
					pn++;
					pnw++;
					pw++;
					ps++;
					psw++;
					ph++;
					t_nw = t_nw > targ ? t_nw : targ;
					t_w = t_w > targ ? t_w : targ;
					break;
				case 4002:
					nw += totalVuln;
					pn++;
					pnw++;
					pw++;
					psw++;
					ph++;
					t_nw = t_nw > targ ? t_nw : targ;
					break;
				case 4003:
					pn++;
					pnw++;
					pw++;
					break;
				case 4004:
					pnw++;
					break;
				case 4005:
					break;
				case 4995:
					break;
				case 4996:
					break;
				case 4997:
					psw++;
					break;
				case 4998:
					pw++;
					psw++;
					break;
				case 4999:
					pnw++;
					pw++;
					psw++;
					break;
				case 5000:
					pnw++;
					pw++;
					psw++;
					break;
				case 5001:
					pnw++;
					pw++;
					psw++;
					break;
				case 5002:
					pnw++;
					pw++;
					break;
				case 5003:
					pnw++;
					break;
				case 5004:
					break;
				case 5005:
					break;
				}

			}
		}
		// end of for loop
		// getting shot threat
		// int n = 0;
		// int nw = 0;
		// int ne = 0;
		// int w = 0;
		// int e = 0;
		// int s = 0;
		// int sw = 0;
		// int se = 0;
		// int h = 0;
		//
		// //potential-contact
		// int pn = 0;
		// int pnw = 0;
		// int pne = 0;
		// int pw = 0;
		// int pe = 0;
		// int ps = 0;
		// int psw = 0;
		// int pse = 0;
		// int ph = 0;
		//
		// //suicide opportunities
		// int s_n = 0;
		// int s_nw = 0;
		// int s_ne = 0;
		// int s_w = 0;
		// int s_e = 0;
		// int s_s = 0;
		// int s_sw = 0;
		// int s_se = 0;
		// int s_h = 0;
		//
		Direction[] directions_ = { Direction.OMNI, Direction.SOUTH_EAST,
				Direction.SOUTH_WEST, Direction.SOUTH, Direction.EAST,
				Direction.WEST, Direction.NORTH_EAST, Direction.NORTH_WEST,
				Direction.NORTH };
		double bestMoveScore = -999;
		int bestMove = 0;
		double bestSuicideScore = -999;
		int bestSuicide = 0;
		for (int i = 9; --i >= 0;) {
			Direction dir;
			double shootingThreat = 0;
			double targetOpp = 0;
			int potentialContact = 0;
			double suicideOpp = 0;
			double suicideDanger = 0;
			switch (i) {
			case 8:
				dir = Direction.NORTH;
				shootingThreat = n;
				targetOpp = t_n;
				potentialContact = pn;
				suicideOpp = s_n;
				suicideDanger = sdng_n;
				break;
			case 7:
				dir = Direction.NORTH_WEST;
				shootingThreat = nw;
				targetOpp = t_nw;
				potentialContact = pnw;
				suicideOpp = s_nw;
				suicideDanger = sdng_nw;
				break;
			case 6:
				dir = Direction.NORTH_EAST;
				shootingThreat = ne;
				targetOpp = t_ne;
				potentialContact = pne;
				suicideOpp = s_ne;
				suicideDanger = sdng_ne;
				break;
			case 5:
				dir = Direction.WEST;
				shootingThreat = w;
				targetOpp = t_w;
				potentialContact = pw;
				suicideOpp = s_w;
				suicideDanger = sdng_w;
				break;
			case 4:
				dir = Direction.EAST;
				shootingThreat = e;
				targetOpp = t_e;
				potentialContact = pe;
				suicideOpp = s_e;
				suicideDanger = sdng_e;
				break;
			case 3:
				dir = Direction.SOUTH;
				shootingThreat = s;
				targetOpp = t_s;
				potentialContact = ps;
				suicideOpp = s_s;
				suicideDanger = sdng_s;
				break;
			case 2:
				dir = Direction.SOUTH_WEST;
				shootingThreat = sw;
				targetOpp = t_sw;
				potentialContact = psw;
				suicideOpp = s_sw;
				suicideDanger = sdng_sw;
				break;
			case 1:
				dir = Direction.SOUTH_EAST;
				shootingThreat = se;
				targetOpp = t_se;
				potentialContact = pse;
				suicideOpp = s_se;
				suicideDanger = sdng_se;
				break;
			case 0:
				dir = Direction.OMNI;
				shootingThreat = h;
				targetOpp = t_h;
				potentialContact = ph;
				suicideOpp = s_h;
				suicideDanger = sdng_h;
				break;
			default:
				dir = Direction.OMNI;
				break;

			}
			if (i != 0 && !canMove(dir)) {
				continue;
			}

			if (i != 0) {
				targetOpp *= FIRST_MOVER_RELUC;
			}
			double score = shootingThreat + targetOpp + suicideDanger;
			double suicideScore = suicideOpp - SUI_RELUC;
			if (i == 0) {
				score += 0.001; // bonus for staying still
				suicideScore += 0.01; // not having to wait a turn to suicide is
										// actually important.
			}
			if (health < 40 && potentialContact > 0) {
				score -= 0.002;
			} else if (potentialContact == 1) {
				score += 0.002;
			}

			if (i == 0) {
				bestAttackScore += shootingThreat; // because shooting entails
													// not moving --
				// problem: += score double counts damage from itself via the 1+
				// in (1 + vuln + almostVuln) in targetOpp
			}

			if (score > bestMoveScore) {
				bestMoveScore = score;
				bestMove = i;
			}
			if (suicideScore > bestSuicideScore) {
				bestSuicideScore = suicideScore;
				bestSuicide = i;
			}
			// log(2,dir.toString());
			// log(2,String.valueOf(suicideDanger));
		}
		// log(1,"..." + String.valueOf(bestMove) + "..." +
		// String.valueOf(bestMoveScore));
		if (bestSuicide == 0) {
			if (rc.senseNearbyGameObjects(Robot.class, here, 2, ourTeam).length > 0) {
				bestSuicideScore = -999;
			}
		} else {
			if (rc.senseNearbyGameObjects(Robot.class,
					here.add(directions_[bestSuicide]), 2, ourTeam).length > 0) {
				bestSuicideScore = -999;
			}
		}

		if (bestAttackScore > bestMoveScore) {
			if (bestSuicideScore > bestAttackScore) {
				log(0, "goodbye");
				log();
				suicide(directions_[bestSuicide]);
			} else {
				attack(bestAttack);
			}
		} else {
			if (bestSuicideScore > bestMoveScore) {
				log(0, "goodbye");
				log();
				suicide(directions_[bestSuicide]);
			} else {
				if (bestMove == 0 && bestAttack != null) {
					attack(bestAttack);
				} else {
					move(directions_[bestMove]);
				}
			}
		}

	}

	static void suicide(Direction dir) throws GameActionException {
		if (dir.equals(Direction.OMNI)) {
			rc.selfDestruct();
		} else {
			move(dir);
			rc.yield();
			if (rc.senseNearbyGameObjects(Robot.class, 2, enemyTeam).length == 0) {
				return; // whoops, they dodged.
			} else {
				rc.selfDestruct();
			}
		}

	}
}
