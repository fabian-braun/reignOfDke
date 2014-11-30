package ext_animorphs;

import battlecode.common.RobotController;

public class RobotPlayer {
	public static void run(RobotController rc) {
		Controller cont = null;
		do {
			try {
				switch (rc.getType()) {
				case HQ:
					cont = new HQController(rc);
					break;
				case SOLDIER:
					cont = new SoldierController(rc);
					break;
				case PASTR:
					cont = new PastrController(rc);
					break;
				case NOISETOWER:
					cont = new NoiseTowerController(rc);
					break;
				default:
					cont = null;
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} while (cont == null);

		while (true) {
			try {
				cont.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
			rc.yield();
		}
	}
}