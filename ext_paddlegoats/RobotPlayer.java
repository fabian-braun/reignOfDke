package ext_paddlegoats;

import battlecode.common.RobotController;

public class RobotPlayer {
	public static void run(RobotController rc) {
		Bot bot;
		switch (rc.getType()) {
		case HQ:
			bot = new HQ();
			break;
		case SOLDIER:
			bot = new Soldier();
			break;
		case NOISETOWER:
			bot = new Noisetower();
			break;
		default:
			bot = new Pastr();
			break;
		}
		bot.go(rc);
	}
}

//
// public class RobotPlayer {
//
// //Yes, I know this is organized a little weird.
//
// public static void run(final RobotController rc) {
//
// abstract class Bot {
//
// //Constant
// RobotType type;
// int id;
// Random rand;
// int mapx, mapy;
// Team ourTeam, enemyTeam;
// MapLocation ourHQ, enemyHQ;
// int birthday;
//
// //Nonconstant
// MapLocation here;
// MapLocation oldHere;
// MapLocation[] trail = new MapLocation[2000];
// int trailEnd;
// double health;
// boolean active;
// int round;
// Robot[] enemies;
// double delay;
//
// //Debug
// boolean log = true;
// String[] ind = {"*","*","*","*"};
//
// public Bot() {
// type = rc.getType();
// id = rc.getRobot().getID();
// rand = new Random(id);
//
// mapx = rc.getMapWidth();
// mapy = rc.getMapHeight();
// ourTeam = rc.getTeam();
// enemyTeam = ourTeam.opponent();
// ourHQ = rc.senseHQLocation();
// enemyHQ = rc.senseEnemyHQLocation();
//
// birthday = Clock.getRoundNum();
// trail[0] = trail[1] = ourHQ; trailEnd = 1;
// }
//
// void go() {
// while (true) {
// try {
// update();
// run();
// } catch (Exception e) {
// e.printStackTrace();
// }
// log();
// rc.yield();
// }
// }
//
// void update() throws GameActionException {
// if (!rc.getLocation().equals(trail[trailEnd])) {
// trailEnd++;
// trail[trailEnd] = rc.getLocation();
// }
// here = trail[trailEnd];
// oldHere = trail[trailEnd - 1];
// health = rc.getHealth();
// active = rc.isActive();
// round = Clock.getRoundNum();
// enemies = rc.senseNearbyGameObjects(Robot.class, 20, enemyTeam);
// delay = rc.getActionDelay();
//
// }
//
// void log() {
// if (log) {
// ind[3] = here.toString() + ind[3];
// for (int i = 3; --i>=0;) {
// rc.setIndicatorString(i,ind[i]);
// }
// ind[0] = ind[1] = ind[2] = ind[3] = "*";
// }
// }
//
// void log(int i, String s) {
// if (log) {
// ind[i] += s;
// }
// }
//
// //Utility
//
// void construct(RobotType t) throws GameActionException {
// switch (t) {
// case NOISETOWER:
// while (true) {
// add(1,NOISETOWERCENSUS);
// rc.yield();
// }
// default:
// while (true) {
// rc.yield();
// }
// }
// }
// void attack(MapLocation target) throws GameActionException {
// rc.attackSquare(target);
// notActive();
// }
// void move(Direction dir) throws GameActionException {
// if (dir != Direction.OMNI) {
// if (canMove(dir)) {
// rc.move(dir);
// notActive();
// } else {
// ind[3] += "can't move!";
// }
// }
// }
// boolean canMove(Direction dir) throws GameActionException {
// return rc.canMove(dir) && !hqDanger(here.add(dir));
// }
// boolean hqDanger(MapLocation loc) throws GameActionException {
// return d2(loc,enemyHQ) <= 25;
// }
// void notActive() {
// active = false;
// }
//
//
//
// //Broadcasting
//
// int START = 60000;
// int CASTE = START;
// int NOISETOWERCENSUS = START + 1;
//
// int ORDERS = 60500;
// int GOAL = 0;
// int NTREQ = 1;
// int PASTRREQ = 2;
// int ordersWidth = 3;
//
// void add(int x,int c) throws GameActionException {
// rc.broadcast(c,rc.readBroadcast(c) + x);
// }
// int writeLoc(MapLocation a) {
// return a.x*100 + a.y;
// }
// MapLocation readLoc(int x) {
// return new MapLocation(x/100,x%100);
// }
//
// //GEOMETRY
//
// Direction[] directions =
// {Direction.NORTH,
// Direction.NORTH_EAST,
// Direction.EAST,
// Direction.SOUTH_EAST,
// Direction.SOUTH,
// Direction.SOUTH_WEST,
// Direction.WEST,
// Direction.NORTH_WEST};
// int d2(MapLocation a) throws GameActionException {
// return here.distanceSquaredTo(a);
// }
// int d2(MapLocation a, MapLocation b) throws GameActionException {
// return a.distanceSquaredTo(b);
// }
// Direction rotate(int x, Direction dir) {
// return directions[(8+x+dir.ordinal())%8];
// }
//
// abstract void run() throws GameActionException;
// }
//
// class HQ extends Bot {
//
// public HQ() {
// super();
// orders[0][0] = writeLoc(enemyHQ);
// }
//
// boolean alert;
//
// int botCount;
// MapLocation[] pastrs;
// int pastrCount;
// int noisetowerCount;
// int soldierCount;
//
// int[][] orders = new int[3][ordersWidth];
//
// void run() throws GameActionException {
// pre();
// if (active) {
// if (alert) {
// attack();
// } else if (botCount < 25) {
// spawn();
// }
// }
// MapLocation[] enemyPastrs = rc.sensePastrLocations(enemyTeam);
// if (enemyPastrs.length > 0) {
// orders[0][0] = writeLoc(enemyPastrs[0]);
// }
// post();
// }
//
//
// void pre() throws GameActionException {
// alert = false; //enemies.length > 0;
// botCount = rc.senseRobotCount();
// pastrs = rc.sensePastrLocations(ourTeam);
// pastrCount = pastrs.length;
// noisetowerCount = rc.readBroadcast(NOISETOWERCENSUS);
// soldierCount = (botCount - pastrCount*2 - noisetowerCount*3);
// }
//
// void post() throws GameActionException {
// for (int c = orders.length; --c>=0;) {
// for (int o = ordersWidth; --o>=0;) {
// rc.broadcast(ORDERS+ordersWidth*c+o, orders[c][o]);
// }
// }
// }
//
// void attack() throws GameActionException {
// int closest = 999;
// MapLocation target = null;
// for (Robot enemy : enemies) {
// MapLocation loc = rc.senseRobotInfo(enemy).location;
// int d = d2(loc);
// if (d < closest) {
// closest = d;
// target = loc;
// }
// }
// if (d2(target) > 15) {
// target = target.add(target.directionTo(here));
// }
// if (rc.canAttackSquare(target)) {
// attack(target);
// }
// }
//
// void spawn() throws GameActionException {
// for (Direction dir : directions) {
// if (rc.canMove(dir)) {
// rc.spawn(dir); notActive(); return;
// }
// }
// }
// }
//
// class Soldier extends Bot {
//
// public Soldier() {
// super();
// }
//
// int caste; int casteChannel = ORDERS+caste*ordersWidth;
//
// MapLocation goal = new MapLocation (-1,-1);
// int ntBuild;
// int pastrBuild;
//
// void run() throws GameActionException {
// pre();
// if (active) {
// if (enemies.length == 0) {
// if (d2(goal) < 3) {
// if (ntBuild > 0) {
// construct(RobotType.NOISETOWER);
// } else if (pastrBuild > 0) {
// construct(RobotType.PASTR);
// }
// }
// Direction dir = path(goal);
// move(dir);
// } else {
// micro();
// }
//
// } else {
// pathingChecks();
// }
// }
//
// void pre() throws GameActionException {
// if (round - birthday < 1) {
// caste = rc.readBroadcast(CASTE);
// }
// goal = readLoc(rc.readBroadcast(casteChannel+GOAL)); log(0,goal.toString());
// ntBuild = rc.readBroadcast(casteChannel+GOAL);
// pastrBuild = rc.readBroadcast(casteChannel+GOAL);
// }
//
// MapLocation oldGoal = new MapLocation(-1,-1);
// int tracing = 0;
// int tracingStartDist = 0;
//
// Direction path(MapLocation goal) throws GameActionException {
// if (!oldGoal.equals(goal)) {
// tracing = 0;
// oldGoal = goal;
// }
// if (d2(goal) < 3) {
// return here.directionTo(goal);
// }
// Direction dir = here.directionTo(goal);
// if (tracing == 0) {
// if (canMove(dir)) {
// return dir;
// } else {
// ind[1] += "startTrace";
// tracing = 1;
// tracingStartDist = d2(goal);
// for (int i = 8; --i>0 && !canMove(dir); dir = rotate(tracing,dir)) { }
// return dir;
// }
// } else {
// dir = here.directionTo(oldHere);
// dir = rotate(tracing,dir);
// if (rc.senseTerrainTile(here.add(dir)) == TerrainTile.NORMAL) {tracing = 0;}
// for (int i = 8; --i>0 && !canMove(dir); dir = rotate(tracing,dir)) {}
// if (d2(goal) <= tracingStartDist) {
// Direction dir2 = rotate(tracing,dir);
// if (canMove(dir2) && d2(here.add(dir2),goal) < d2(goal)) {
// tracing = 0; return dir2;
// }
// dir2 = rotate(tracing,dir2);
// if (canMove(dir2) && d2(here.add(dir2),goal) < d2(goal)) {
// tracing = 0; return dir2;
// }
// }
// return dir;
// }
// }
//
// void pathingChecks() throws GameActionException {
// if (tracing != 0) {
// Direction toBorder;
// int w = here.x;
// int e = mapx - here.x;
// int n = here.y;
// int s = mapy - here.y;
// if (Math.min(n,s) < Math.min(e,w)) {
// if (n < s) {
// toBorder = Direction.NORTH;
// } else {
// toBorder = Direction.SOUTH;
// }
// } else {
// if (e < w) {
// toBorder = Direction.EAST;
// } else {
// toBorder = Direction.WEST;
// }
// }
// MapLocation there = here;
// check: for (int i = 0; i < 5; i++) {
// there = there.add(toBorder);
// switch (rc.senseTerrainTile(there)) {
// case NORMAL:
// break check;
// case OFF_MAP:
// tracing = 0;
// break check;
// default:
// break;
// }
// }
// }
//
// }
//
// void micro() throws GameActionException {
//
//
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
// Robot[] enemies_ = enemies;
// //rc.senseNearbyGameObjects(Robot.class,25,enemyTeam);
//
// //proudly autogened with Haskell
// double bestAttack = -999999999;
// MapLocation bestAttackSquare = null;
// int closestDist = 999999;
// MapLocation closestEnemy = null;
// for (Robot enemy : enemies_) {
// RobotInfo info = rc.senseRobotInfo(enemy);
// MapLocation loc = info.location;
// RobotType type = info.type;
// int dist = d2(loc);
// if (dist < closestDist) {
// closestDist = dist;
// closestEnemy = loc;
// }
// if (rc.canAttackSquare(loc)) {
// double hp = info.health;
// double score = 10/hp;
// if (score > bestAttack) {
// bestAttack = score;
// bestAttackSquare = loc;
// }
// }
// //ind2 += "bestAttack:" + String.valueOf(bestAttack);
// if (type == RobotType.SOLDIER) { //ind1 += "shooter:" +
// String.valueOf(((here.x - loc.x + 50)*100 + here.y - loc.y + 50));
// switch ((here.x - loc.x + 50)*100 + here.y - loc.y + 50) {
// case 4646:
// break;
// case 4647:
// break;
// case 4648:
// se++;
// break;
// case 4649:
// e++;
// se++;
// break;
// case 4650:
// e++;
// ne++;
// se++;
// break;
// case 4651:
// e++;
// ne++;
// break;
// case 4652:
// ne++;
// break;
// case 4653:
// break;
// case 4654:
// break;
// case 4746:
// break;
// case 4747:
// se++;
// break;
// case 4748:
// s++;
// e++;
// se++;
// break;
// case 4749:
// s++;
// e++;
// ne++;
// se++;
// h++;
// break;
// case 4750:
// n++;
// s++;
// e++;
// ne++;
// se++;
// h++;
// break;
// case 4751:
// n++;
// e++;
// ne++;
// se++;
// h++;
// break;
// case 4752:
// n++;
// e++;
// ne++;
// break;
// case 4753:
// ne++;
// break;
// case 4754:
// break;
// case 4846:
// se++;
// break;
// case 4847:
// s++;
// e++;
// se++;
// break;
// case 4848:
// s++;
// e++;
// ne++;
// se++;
// sw++;
// h++;
// break;
// case 4849:
// n++;
// s++;
// e++;
// w++;
// ne++;
// se++;
// sw++;
// h++;
// break;
// case 4850:
// n++;
// s++;
// e++;
// w++;
// ne++;
// nw++;
// se++;
// sw++;
// h++;
// break;
// case 4851:
// n++;
// s++;
// e++;
// w++;
// ne++;
// nw++;
// se++;
// h++;
// break;
// case 4852:
// n++;
// e++;
// ne++;
// nw++;
// se++;
// h++;
// break;
// case 4853:
// n++;
// e++;
// ne++;
// break;
// case 4854:
// ne++;
// break;
// case 4946:
// s++;
// se++;
// break;
// case 4947:
// s++;
// e++;
// se++;
// sw++;
// h++;
// break;
// case 4948:
// n++;
// s++;
// e++;
// w++;
// ne++;
// se++;
// sw++;
// h++;
// break;
// case 4949:
// n++;
// s++;
// e++;
// w++;
// ne++;
// nw++;
// se++;
// sw++;
// h++;
// break;
// case 4950:
// n++;
// s++;
// e++;
// w++;
// ne++;
// nw++;
// se++;
// sw++;
// h++;
// break;
// case 4951:
// n++;
// s++;
// e++;
// w++;
// ne++;
// nw++;
// se++;
// sw++;
// h++;
// break;
// case 4952:
// n++;
// s++;
// e++;
// w++;
// ne++;
// nw++;
// se++;
// h++;
// break;
// case 4953:
// n++;
// e++;
// ne++;
// nw++;
// h++;
// break;
// case 4954:
// n++;
// ne++;
// break;
// case 5046:
// s++;
// se++;
// sw++;
// break;
// case 5047:
// s++;
// e++;
// w++;
// se++;
// sw++;
// h++;
// break;
// case 5048:
// n++;
// s++;
// e++;
// w++;
// ne++;
// nw++;
// se++;
// sw++;
// h++;
// break;
// case 5049:
// n++;
// s++;
// e++;
// w++;
// ne++;
// nw++;
// se++;
// sw++;
// h++;
// break;
// case 5050:
// n++;
// s++;
// e++;
// w++;
// ne++;
// nw++;
// se++;
// sw++;
// h++;
// break;
// case 5051:
// n++;
// s++;
// e++;
// w++;
// ne++;
// nw++;
// se++;
// sw++;
// h++;
// break;
// case 5052:
// n++;
// s++;
// e++;
// w++;
// ne++;
// nw++;
// se++;
// sw++;
// h++;
// break;
// case 5053:
// n++;
// e++;
// w++;
// ne++;
// nw++;
// h++;
// break;
// case 5054:
// n++;
// ne++;
// nw++;
// break;
// case 5146:
// s++;
// sw++;
// break;
// case 5147:
// s++;
// w++;
// se++;
// sw++;
// h++;
// break;
// case 5148:
// n++;
// s++;
// e++;
// w++;
// nw++;
// se++;
// sw++;
// h++;
// break;
// case 5149:
// n++;
// s++;
// e++;
// w++;
// ne++;
// nw++;
// se++;
// sw++;
// h++;
// break;
// case 5150:
// n++;
// s++;
// e++;
// w++;
// ne++;
// nw++;
// se++;
// sw++;
// h++;
// break;
// case 5151:
// n++;
// s++;
// e++;
// w++;
// ne++;
// nw++;
// se++;
// sw++;
// h++;
// break;
// case 5152:
// n++;
// s++;
// e++;
// w++;
// ne++;
// nw++;
// sw++;
// h++;
// break;
// case 5153:
// n++;
// w++;
// ne++;
// nw++;
// h++;
// break;
// case 5154:
// n++;
// nw++;
// break;
// case 5246:
// sw++;
// break;
// case 5247:
// s++;
// w++;
// sw++;
// break;
// case 5248:
// s++;
// w++;
// nw++;
// se++;
// sw++;
// h++;
// break;
// case 5249:
// n++;
// s++;
// e++;
// w++;
// nw++;
// se++;
// sw++;
// h++;
// break;
// case 5250:
// n++;
// s++;
// e++;
// w++;
// ne++;
// nw++;
// se++;
// sw++;
// h++;
// break;
// case 5251:
// n++;
// s++;
// e++;
// w++;
// ne++;
// nw++;
// sw++;
// h++;
// break;
// case 5252:
// n++;
// w++;
// ne++;
// nw++;
// sw++;
// h++;
// break;
// case 5253:
// n++;
// w++;
// nw++;
// break;
// case 5254:
// nw++;
// break;
// case 5346:
// break;
// case 5347:
// sw++;
// break;
// case 5348:
// s++;
// w++;
// sw++;
// break;
// case 5349:
// s++;
// w++;
// nw++;
// sw++;
// h++;
// break;
// case 5350:
// n++;
// s++;
// w++;
// nw++;
// sw++;
// h++;
// break;
// case 5351:
// n++;
// w++;
// nw++;
// sw++;
// h++;
// break;
// case 5352:
// n++;
// w++;
// nw++;
// break;
// case 5353:
// nw++;
// break;
// case 5354:
// break;
// case 5446:
// break;
// case 5447:
// break;
// case 5448:
// sw++;
// break;
// case 5449:
// w++;
// sw++;
// break;
// case 5450:
// w++;
// nw++;
// sw++;
// break;
// case 5451:
// w++;
// nw++;
// break;
// case 5452:
// nw++;
// break;
// case 5453:
// break;
// case 5454:
// break;
// default:
// break;
// }
// }
//
// //ind0 += "enem:" + String.valueOf(Clock.getBytecodeNum());
// }
//
// Direction toClosest = here.directionTo(closestEnemy);
//
// boolean firstMover = h == 0;
//
// double bestScore = -99999; double stayScore = 1;
// int best = 0;
// for (int i = 9; --i>= 0;) {
// Direction thisDir = null;
// switch(i) {
// case 0: thisDir = Direction.OMNI; break;
// case 1: thisDir = Direction.NORTH; break;
// case 2: thisDir = Direction.SOUTH; break;
// case 3: thisDir = Direction.EAST; break;
// case 4: thisDir = Direction.WEST; break;
// case 5: thisDir = Direction.NORTH_WEST; break;
// case 6: thisDir = Direction.NORTH_EAST; break;
// case 7: thisDir = Direction.SOUTH_WEST; break;
// case 8: thisDir = Direction.SOUTH_EAST; break;
// }
// if (!canMove(thisDir) && i != 0) {
// continue;
// }
//
//
// double score;
// int shooters = 0;
// switch(i) {
// case 0: shooters = h; break;
// case 1: shooters = n; break;
// case 2: shooters = s; break;
// case 3: shooters = e; break;
// case 4: shooters = w; break;
// case 5: shooters = nw; break;
// case 6: shooters = ne; break;
// case 7: shooters = sw; break;
// case 8: shooters = se; break;
// }
//
//
// score = -shooters*10/health;
// if (firstMover && i != 0) {
// score *= 1.4;
// }
// if (shooters == 1 && i != 0 && h == 0) {
// score = score + 0.09;
// }
//
//
// if (i == 0) {
// score += 0.01; //slight preference for standing still.
// stayScore = score;
// } else if (thisDir.equals(toClosest)) {
// score += 0.02; //slight preference to go to closest enemy.
// }
// if (thisDir.isDiagonal()) {
// score += -0.01; //diagonal is bad. inefficient.
// }
// //ind2 += "dir:" + thisDir.toString() + "score:" + String.valueOf(score) +
// "," + String.valueOf(shooters);
// if (score > bestScore) {
// bestScore = score;
// best = i;
// }
// }
//
// Direction bestDir = Direction.OMNI;
// switch(best) {
// case 0: bestDir = Direction.OMNI; break;
// case 1: bestDir = Direction.NORTH; break;
// case 2: bestDir = Direction.SOUTH; break;
// case 3: bestDir = Direction.EAST; break;
// case 4: bestDir = Direction.WEST; break;
// case 5: bestDir = Direction.NORTH_WEST; break;
// case 6: bestDir = Direction.NORTH_EAST; break;
// case 7: bestDir = Direction.SOUTH_WEST; break;
// case 8: bestDir = Direction.SOUTH_EAST; break;
// }
//
// if (bestAttack + stayScore > bestScore) {
// rc.attackSquare(bestAttackSquare);
// } else {
// if (bestDir == Direction.OMNI && bestAttackSquare != null) {
// rc.attackSquare(bestAttackSquare);
// } else {
// move(bestDir);
// }
// }
//
//
// }
// }
//
// class Noisetower extends Bot {
//
// public Noisetower() {
// super();
// }
//
// int dist = 16;
// Direction angle = Direction.NORTH;
//
// void run() throws GameActionException {
// if (active) {
// nextNT();
// while (!rc.canAttackSquare(here.add(angle,dist))) {
// nextNT();
// }
// rc.attackSquare(here.add(angle,dist));
// }
// }
//
// void nextNT() {
// if (dist < 6) {
// dist = 16;
// angle = angle.rotateLeft();
// } else {
// dist--;
// }
// }
//
// }
//
// class Pastr extends Bot {
//
// public Pastr() {
// super();
// }
//
// void run() {
//
// }
// }
//
// Bot bot;
// switch (rc.getType()) {
// case HQ:
// bot = new HQ();
// break;
// case SOLDIER:
// bot = new Soldier();
// break;
// case NOISETOWER:
// bot = new Noisetower();
// break;
// case PASTR:
// bot = new Pastr();
// break;
// default:
// bot = new Pastr();
// break;
// }
//
// bot.go();
//
// }
//
// }

/*
 * package springCleaning;
 * 
 * import battlecode.common.*;
 * 
 * import java.util.*;
 * 
 * public class RobotPlayer {
 * 
 * public static void run(RobotController rc) { AI ai = new AI(rc); ai.run();
 * //because i hate typing static over and over. } }
 * 
 * class AI {
 * 
 * public AI(RobotController rc_) { rc = rc_; }
 * 
 * public void run() {
 * 
 * init();
 * 
 * while (true) { while (true) { try { update(); switch (type) { case HQ: hq();
 * break; case SOLDIER: soldier(); break; case NOISETOWER: noisetower(); break;
 * case PASTR: break; } } catch (Exception e) { e.printStackTrace(); ind[2] +=
 * e.toString(); } log(); rc.yield(); } }
 * 
 * }
 * 
 * 
 * //LOGGING
 * 
 * boolean log = true; String[] ind = {"*","*","*","*"};
 * 
 * void log() { if (log) { ind[3] = here.toString() + ind[3]; for (int i = 3;
 * --i>=0;) { rc.setIndicatorString(i,ind[i]); } ind[0] = ind[1] = ind[2] =
 * ind[3] = "*"; } }
 * 
 * void log(int i, String s) { if (log) { ind[i] += s; } }
 * 
 * 
 * //Pretend this is a class called Bot
 * 
 * 
 * 
 * RobotController rc; RobotType type; int id; Random rand;
 * 
 * int mapx, mapy; Team ourTeam, enemyTeam; MapLocation ourHQ, enemyHQ;
 * 
 * int birthday;
 * 
 * void init() { type = rc.getType(); id = rc.getRobot().getID(); rand = new
 * Random(id);
 * 
 * mapx = rc.getMapWidth(); mapy = rc.getMapHeight(); ourTeam = rc.getTeam();
 * enemyTeam = ourTeam.opponent(); ourHQ = rc.senseHQLocation(); enemyHQ =
 * rc.senseEnemyHQLocation();
 * 
 * birthday = Clock.getRoundNum(); trail[0] = trail[1] = ourHQ; trailEnd = 1; }
 * 
 * MapLocation here; MapLocation oldHere; MapLocation[] trail = new
 * MapLocation[2000]; int trailEnd; double health; boolean active; int round;
 * Robot[] enemies; double delay;
 * 
 * void update() throws GameActionException { if
 * (!rc.getLocation().equals(trail[trailEnd])) { trailEnd++; trail[trailEnd] =
 * rc.getLocation(); } here = trail[trailEnd]; oldHere = trail[trailEnd - 1];
 * health = rc.getHealth(); active = rc.isActive(); round = Clock.getRoundNum();
 * enemies = rc.senseNearbyGameObjects(Robot.class, 20, enemyTeam); delay =
 * rc.getActionDelay(); }
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * //HQ
 * 
 * 
 * void hq() throws GameActionException { hqUpdate(); if (active) { if (alert) {
 * hqAttack(); } else if (botCount < 25) { spawn(); } } }
 * 
 * 
 * boolean alert;
 * 
 * int botCount; MapLocation[] pastrs; int pastrCount; int noisetowerCount; int
 * soldierCount;
 * 
 * int[][] orders = new int[3][3];
 * 
 * void hqUpdate() throws GameActionException { alert = enemies.length > 0;
 * botCount = rc.senseRobotCount(); pastrs = rc.sensePastrLocations(ourTeam);
 * pastrCount = pastrs.length; noisetowerCount =
 * rc.readBroadcast(NOISETOWERCENSUS); soldierCount = (botCount - pastrCount*2 -
 * noisetowerCount*3);
 * 
 * int width = orders[0].length; for (int c = orders.length; --c>=0;) { for (int
 * o = width; --o>=0;) { rc.broadcast(ORDERS+width*c+o, orders[c][o]); } }
 * 
 * }
 * 
 * void hqAttack() throws GameActionException { int closest = 999; MapLocation
 * target = null; for (Robot enemy : enemies) { MapLocation loc =
 * rc.senseRobotInfo(enemy).location; int d = d2(loc); if (d < closest) {
 * closest = d; target = loc; } } if (d2(target) > 15) { target =
 * target.add(target.directionTo(here)); } if (rc.canAttackSquare(target)) {
 * attack(target); } }
 * 
 * 
 * void spawn() throws GameActionException { for (Direction dir : directions) {
 * if (rc.canMove(dir)) { rc.spawn(dir); notActive(); return; } } }
 * 
 * 
 * MapLocation goal; int caste;
 * 
 * 
 * void soldierUpdate() throws GameActionException { if (round - birthday < 1) {
 * caste = rc.readBroadcast(CASTE); } }
 * 
 * void soldier() throws GameActionException { soldierUpdate(); if (active) { if
 * (enemies.length == 0) { Direction dir = path(goal); move(dir); } else {
 * micro(); }
 * 
 * } else { //pathingChecks(); }
 * 
 * }
 * 
 * 
 * 
 * MapLocation oldGoal = new MapLocation(-1,-1); int tracing = 0; int
 * tracingStartDist = 0;
 * 
 * Direction path(MapLocation goal) throws GameActionException { if
 * (!oldGoal.equals(goal)) { tracing = 0; oldGoal = goal; } if (d2(goal) < 3) {
 * return here.directionTo(goal); } Direction dir = here.directionTo(goal); if
 * (tracing == 0) { if (canMove(dir)) { return dir; } else { ind[1] +=
 * "startTrace"; tracing = 1; tracingStartDist = d2(goal); for (int i = 8; --i>0
 * && !canMove(dir); dir = rotate(tracing,dir)) { } return dir; } } else { dir =
 * here.directionTo(oldHere); dir = rotate(tracing,dir); if
 * (rc.senseTerrainTile(here.add(dir)) == TerrainTile.NORMAL) {tracing = 0;} for
 * (int i = 8; --i>0 && !canMove(dir); dir = rotate(tracing,dir)) {} if
 * (d2(goal) <= tracingStartDist) { Direction dir2 = rotate(tracing,dir); if
 * (canMove(dir2) && d2(here.add(dir2),goal) < d2(goal)) { tracing = 0; return
 * dir2; } dir2 = rotate(tracing,dir2); if (canMove(dir2) &&
 * d2(here.add(dir2),goal) < d2(goal)) { tracing = 0; return dir2; } } return
 * dir; } }
 * 
 * void pathingChecks() throws GameActionException { if (tracing != 0) {
 * Direction toBorder; int w = here.x; int e = mapx - here.x; int n = here.y;
 * int s = mapy - here.y; if (Math.min(n,s) < Math.min(e,w)) { if (n < s) {
 * toBorder = Direction.NORTH; } else { toBorder = Direction.SOUTH; } } else {
 * if (e < w) { toBorder = Direction.EAST; } else { toBorder = Direction.WEST; }
 * } MapLocation there = here; check: for (int i = 0; i < 5; i++) { there =
 * there.add(toBorder); switch (rc.senseTerrainTile(there)) { case NORMAL: break
 * check; case OFF_MAP: tracing = 0; break check; default: break; } } }
 * 
 * }
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * //noisetower
 * 
 * 
 * int dist = 16; Direction angle = Direction.NORTH; void noisetower() throws
 * GameActionException { if (active) { nextNT(); while
 * (!rc.canAttackSquare(here.add(angle,dist))) { nextNT(); }
 * rc.attackSquare(here.add(angle,dist)); } }
 * 
 * void nextNT() { if (dist < 6) { dist = 16; angle = angle.rotateLeft(); } else
 * { dist--; } }
 * 
 * 
 * 
 * 
 * //utility
 * 
 * void construct(RobotType t) throws GameActionException { switch (t) { case
 * NOISETOWER: while (true) { add(1,NOISETOWERCENSUS); rc.yield(); } default:
 * while (true) { rc.yield(); } } } void attack(MapLocation target) throws
 * GameActionException { rc.attackSquare(target); notActive(); } void
 * move(Direction dir) throws GameActionException { if (dir != Direction.OMNI) {
 * if (canMove(dir)) { rc.move(dir); notActive(); } else { ind[3] +=
 * "can't move!"; } } }
 * 
 * 
 * boolean canMove(Direction dir) throws GameActionException { return
 * rc.canMove(dir) && !hqDanger(here.add(dir)); }
 * 
 * boolean hqDanger(MapLocation loc) throws GameActionException { return
 * d2(loc,enemyHQ) <= 25; }
 * 
 * void notActive() { active = false; }
 * 
 * 
 * 
 * //broadcasting
 * 
 * int START = 60000; int CASTE = START; int NOISETOWERCENSUS = START + 1;
 * 
 * int ORDERS = 60500;
 * 
 * 
 * void add(int x,int c) throws GameActionException {
 * rc.broadcast(c,rc.readBroadcast(c) + x); }
 * 
 * int writeLoc(MapLocation a) { return a.x*100 + a.y; } MapLocation readLoc(int
 * x) { return new MapLocation(x/100,x%100); }
 * 
 * 
 * 
 * 
 * //GEOMETRY
 * 
 * static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST,
 * Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST,
 * Direction.WEST, Direction.NORTH_WEST};
 * 
 * int d2(MapLocation a) throws GameActionException { return
 * here.distanceSquaredTo(a); } int d2(MapLocation a, MapLocation b) throws
 * GameActionException { return a.distanceSquaredTo(b); }
 * 
 * Direction rotate(int x, Direction dir) { return
 * directions[(8+x+dir.ordinal())%8]; }
 * 
 * 
 * 
 * 
 * 
 * //MICRO
 * 
 * 
 * double harassThreshold = 600;
 * 
 * void harass() throws GameActionException { MapLocation best = null; double
 * mostCows = -1; for (int x = -3; x <= 3; x++) { for (int y = -3; y <= 3; y++)
 * { MapLocation there = here.add(x,y); double cows =
 * rc.senseCowsAtLocation(there); if (cows > mostCows) { if
 * (rc.canAttackSquare(there)) { GameObject thing =
 * rc.senseObjectAtLocation(there); if (thing != null) { if (thing.getTeam() ==
 * ourTeam) { continue; //nested checks because each one is rather expensive. }
 * } mostCows = cows; best = there; } } } } if (mostCows > harassThreshold) {
 * rc.attackSquare(best); active = false; } }
 * 
 * //THE MONSTER
 * 
 * void micro() throws GameActionException {
 * 
 * 
 * 
 * int n = 0; int nw = 0; int ne = 0; int w = 0; int e = 0; int s = 0; int sw =
 * 0; int se = 0; int h = 0;
 * 
 * Robot[] enemies_ = enemies;
 * //rc.senseNearbyGameObjects(Robot.class,25,enemyTeam);
 * 
 * //proudly autogened with Haskell double bestAttack = -999999999; MapLocation
 * bestAttackSquare = null; int closestDist = 999999; MapLocation closestEnemy =
 * null; for (Robot enemy : enemies_) { RobotInfo info =
 * rc.senseRobotInfo(enemy); MapLocation loc = info.location; RobotType type =
 * info.type; int dist = d2(loc); if (dist < closestDist) { closestDist = dist;
 * closestEnemy = loc; } if (rc.canAttackSquare(loc)) { double hp = info.health;
 * double score = 10/hp; if (score > bestAttack) { bestAttack = score;
 * bestAttackSquare = loc; } } //ind2 += "bestAttack:" +
 * String.valueOf(bestAttack); if (type == RobotType.SOLDIER) { //ind1 +=
 * "shooter:" + String.valueOf(((here.x - loc.x + 50)*100 + here.y - loc.y +
 * 50)); switch ((here.x - loc.x + 50)*100 + here.y - loc.y + 50) { case 4646:
 * break; case 4647: break; case 4648: se++; break; case 4649: e++; se++; break;
 * case 4650: e++; ne++; se++; break; case 4651: e++; ne++; break; case 4652:
 * ne++; break; case 4653: break; case 4654: break; case 4746: break; case 4747:
 * se++; break; case 4748: s++; e++; se++; break; case 4749: s++; e++; ne++;
 * se++; h++; break; case 4750: n++; s++; e++; ne++; se++; h++; break; case
 * 4751: n++; e++; ne++; se++; h++; break; case 4752: n++; e++; ne++; break;
 * case 4753: ne++; break; case 4754: break; case 4846: se++; break; case 4847:
 * s++; e++; se++; break; case 4848: s++; e++; ne++; se++; sw++; h++; break;
 * case 4849: n++; s++; e++; w++; ne++; se++; sw++; h++; break; case 4850: n++;
 * s++; e++; w++; ne++; nw++; se++; sw++; h++; break; case 4851: n++; s++; e++;
 * w++; ne++; nw++; se++; h++; break; case 4852: n++; e++; ne++; nw++; se++;
 * h++; break; case 4853: n++; e++; ne++; break; case 4854: ne++; break; case
 * 4946: s++; se++; break; case 4947: s++; e++; se++; sw++; h++; break; case
 * 4948: n++; s++; e++; w++; ne++; se++; sw++; h++; break; case 4949: n++; s++;
 * e++; w++; ne++; nw++; se++; sw++; h++; break; case 4950: n++; s++; e++; w++;
 * ne++; nw++; se++; sw++; h++; break; case 4951: n++; s++; e++; w++; ne++;
 * nw++; se++; sw++; h++; break; case 4952: n++; s++; e++; w++; ne++; nw++;
 * se++; h++; break; case 4953: n++; e++; ne++; nw++; h++; break; case 4954:
 * n++; ne++; break; case 5046: s++; se++; sw++; break; case 5047: s++; e++;
 * w++; se++; sw++; h++; break; case 5048: n++; s++; e++; w++; ne++; nw++; se++;
 * sw++; h++; break; case 5049: n++; s++; e++; w++; ne++; nw++; se++; sw++; h++;
 * break; case 5050: n++; s++; e++; w++; ne++; nw++; se++; sw++; h++; break;
 * case 5051: n++; s++; e++; w++; ne++; nw++; se++; sw++; h++; break; case 5052:
 * n++; s++; e++; w++; ne++; nw++; se++; sw++; h++; break; case 5053: n++; e++;
 * w++; ne++; nw++; h++; break; case 5054: n++; ne++; nw++; break; case 5146:
 * s++; sw++; break; case 5147: s++; w++; se++; sw++; h++; break; case 5148:
 * n++; s++; e++; w++; nw++; se++; sw++; h++; break; case 5149: n++; s++; e++;
 * w++; ne++; nw++; se++; sw++; h++; break; case 5150: n++; s++; e++; w++; ne++;
 * nw++; se++; sw++; h++; break; case 5151: n++; s++; e++; w++; ne++; nw++;
 * se++; sw++; h++; break; case 5152: n++; s++; e++; w++; ne++; nw++; sw++; h++;
 * break; case 5153: n++; w++; ne++; nw++; h++; break; case 5154: n++; nw++;
 * break; case 5246: sw++; break; case 5247: s++; w++; sw++; break; case 5248:
 * s++; w++; nw++; se++; sw++; h++; break; case 5249: n++; s++; e++; w++; nw++;
 * se++; sw++; h++; break; case 5250: n++; s++; e++; w++; ne++; nw++; se++;
 * sw++; h++; break; case 5251: n++; s++; e++; w++; ne++; nw++; sw++; h++;
 * break; case 5252: n++; w++; ne++; nw++; sw++; h++; break; case 5253: n++;
 * w++; nw++; break; case 5254: nw++; break; case 5346: break; case 5347: sw++;
 * break; case 5348: s++; w++; sw++; break; case 5349: s++; w++; nw++; sw++;
 * h++; break; case 5350: n++; s++; w++; nw++; sw++; h++; break; case 5351: n++;
 * w++; nw++; sw++; h++; break; case 5352: n++; w++; nw++; break; case 5353:
 * nw++; break; case 5354: break; case 5446: break; case 5447: break; case 5448:
 * sw++; break; case 5449: w++; sw++; break; case 5450: w++; nw++; sw++; break;
 * case 5451: w++; nw++; break; case 5452: nw++; break; case 5453: break; case
 * 5454: break; default: break; } }
 * 
 * //ind0 += "enem:" + String.valueOf(Clock.getBytecodeNum()); }
 * 
 * Direction toClosest = here.directionTo(closestEnemy);
 * 
 * boolean firstMover = h == 0;
 * 
 * double bestScore = -99999; double stayScore = 1; int best = 0; for (int i =
 * 9; --i>= 0;) { Direction thisDir = null; switch(i) { case 0: thisDir =
 * Direction.OMNI; break; case 1: thisDir = Direction.NORTH; break; case 2:
 * thisDir = Direction.SOUTH; break; case 3: thisDir = Direction.EAST; break;
 * case 4: thisDir = Direction.WEST; break; case 5: thisDir =
 * Direction.NORTH_WEST; break; case 6: thisDir = Direction.NORTH_EAST; break;
 * case 7: thisDir = Direction.SOUTH_WEST; break; case 8: thisDir =
 * Direction.SOUTH_EAST; break; } if (!canMove(thisDir) && i != 0) { continue; }
 * 
 * 
 * double score; int shooters = 0; switch(i) { case 0: shooters = h; break; case
 * 1: shooters = n; break; case 2: shooters = s; break; case 3: shooters = e;
 * break; case 4: shooters = w; break; case 5: shooters = nw; break; case 6:
 * shooters = ne; break; case 7: shooters = sw; break; case 8: shooters = se;
 * break; }
 * 
 * 
 * score = -shooters*10/health; if (firstMover && i != 0) { score *= 1.4; } if
 * (shooters == 1 && i != 0 && h == 0) { score = score + 0.09; }
 * 
 * 
 * if (i == 0) { score += 0.01; //slight preference for standing still.
 * stayScore = score; } else if (thisDir.equals(toClosest)) { score += 0.02;
 * //slight preference to go to closest enemy. } if (thisDir.isDiagonal()) {
 * score += -0.01; //diagonal is bad. inefficient. } //ind2 += "dir:" +
 * thisDir.toString() + "score:" + String.valueOf(score) + "," +
 * String.valueOf(shooters); if (score > bestScore) { bestScore = score; best =
 * i; } }
 * 
 * Direction bestDir = Direction.OMNI; switch(best) { case 0: bestDir =
 * Direction.OMNI; break; case 1: bestDir = Direction.NORTH; break; case 2:
 * bestDir = Direction.SOUTH; break; case 3: bestDir = Direction.EAST; break;
 * case 4: bestDir = Direction.WEST; break; case 5: bestDir =
 * Direction.NORTH_WEST; break; case 6: bestDir = Direction.NORTH_EAST; break;
 * case 7: bestDir = Direction.SOUTH_WEST; break; case 8: bestDir =
 * Direction.SOUTH_EAST; break; }
 * 
 * if (bestAttack + stayScore > bestScore) { rc.attackSquare(bestAttackSquare);
 * } else { if (bestDir == Direction.OMNI && bestAttackSquare != null) {
 * rc.attackSquare(bestAttackSquare); } else { move(bestDir); } }
 * 
 * 
 * }
 * 
 * }
 */

