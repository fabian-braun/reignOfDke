package ext_zeroxg;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;

public class RobotPlayer {
	public static final Direction[] customDirections = new Direction[] {
			Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST,
			Direction.NORTH_EAST, Direction.SOUTH_EAST, Direction.NORTH_WEST,
			Direction.SOUTH_WEST };
	public static MapLocation mapCenter;
	public static int startTurn;
	public static Team team;
	public static MapLocation enemyHQ;
	public static MapLocation myHQ;

	public static void run(RobotController rc) {
		mapCenter = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
		startTurn = Clock.getRoundNum();
		team = rc.getTeam();
		enemyHQ = rc.senseEnemyHQLocation();
		myHQ = rc.senseHQLocation();
		try {
			switch (rc.getType()) // INIT
			{

			case HQ:
				RobotHQ.init(rc);
				break;
			case SOLDIER:
				RobotSoldier.init(rc);
				break;
			case PASTR:
				RobotPASTR.init(rc);
				break;
			case NOISETOWER:
				RobotNT.init(rc);
				break;
			default:
				break;
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}

		while (true) // RUN
		{
			try {
				switch (rc.getType()) {
				case HQ:
					RobotHQ.run(rc);
					break;
				case SOLDIER:
					RobotSoldier.run(rc);
					break;
				case PASTR:
					RobotPASTR.run(rc);
					break;
				case NOISETOWER:
					RobotNT.run(rc);
					break;
				default:
					break;
				}

				rc.yield();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
