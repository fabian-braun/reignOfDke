package ext_animorphs;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Team;

public abstract class Controller {
	// shortcuts for basic information
	public final int width;
	public final int height;
	public final int diagonalSq;
	public final int id;
	public final Team team;
	public final Team enemy;
	public final MapLocation teamhq;
	public final MapLocation enemyhq;
	public final RobotController rc;
	public final Robot robot;
	public final RobotType type;
	public long[] teamMemory;

	// shortcuts for type
	public final double attackPower;
	public final int attackDelay;
	public final int moveDelay = 2;
	public final int attackRadiusSquared;
	public final int sensorRadiusSquared;
	public final double maxHealth;

	// shortcuts for commonly used values
	public static final int HQ_ATTACK_RADIUS_SQ = RobotType.HQ.attackRadiusMaxSquared;
	public static final int SPLASH_RADIUS_SQ = 2;
	public final int HQ_SPLASH_RADIUS_SQ;

	public Controller(RobotController rc) {
		width = rc.getMapWidth();
		height = rc.getMapHeight();
		diagonalSq = width * width + height * height;
		team = rc.getTeam();
		enemy = team.opponent();
		teamhq = rc.senseHQLocation();
		enemyhq = rc.senseEnemyHQLocation();
		robot = rc.getRobot();
		id = robot.getID();
		type = rc.getType();
		teamMemory = rc.getTeamMemory();
		this.rc = rc;
		attackPower = type.attackPower;
		attackDelay = type.attackDelay;
		sensorRadiusSquared = type.sensorRadiusSquared;
		attackRadiusSquared = type.attackRadiusMaxSquared;
		maxHealth = type.maxHealth;
		HQ_SPLASH_RADIUS_SQ = Mover.calcSplashRadiusSq(HQ_ATTACK_RADIUS_SQ);

		// MAGICAL CONSTANT
		switch (robot.getID() % 11) {
		case 0:
		case 6:
			rc.setIndicatorString(0, "hipp0");
			break;
		case 8:
		case 3:
			rc.setIndicatorString(0, "0tter");
			break;
		case 5:
		case 7:
			rc.setIndicatorString(0, "richni");
			break;
		case 4:
		case 1:
			rc.setIndicatorString(0, "ntyagi");
			break;
		default:
			rc.setIndicatorString(0, "ANIM0RPHING");
			break;
		}

	}

	public abstract void run() throws GameActionException;
}