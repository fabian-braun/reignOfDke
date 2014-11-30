package ext_zeroxg;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class RobotPASTR {
	public static Job job;
	public static double lastHP;
	public static int lastAttackTurn;
	public static int attackMemory = 3;

	public static void init(RobotController rc) throws GameActionException {
		job = Channels.jobBoard.getJobAt(rc, rc.getLocation());
		lastHP = rc.getHealth();
		lastAttackTurn = -100;
	}

	public static void run(RobotController rc) throws GameActionException {
		if (job != null)
			job.claim(rc);

		if (rc.getHealth() < lastHP) {
			lastHP = rc.getHealth();
			lastAttackTurn = Clock.getRoundNum();
		}

		if (Clock.getRoundNum() - lastAttackTurn <= attackMemory) {
			rc.broadcast(Channels.pastrUnderAttack, 1);
			// System.out.println("I am under attack! " + rc.getHealth() + " " +
			// (Clock.getRoundNum() - lastAttackTurn));
		} else
			rc.broadcast(Channels.pastrUnderAttack, 0);
	}
}
