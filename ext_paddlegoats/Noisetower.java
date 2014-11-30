package ext_paddlegoats;

import battlecode.common.Direction;
import battlecode.common.GameActionException;

public class Noisetower extends Bot {

	static int dist = 16;
	static Direction angle = Direction.NORTH;

	void init() {

	}

	void run() throws GameActionException {
		add(1, NOISETOWERCENSUS);
		if (active) {
			nextNT();
			while (!rc.canAttackSquare(here.add(angle, dist))) {
				nextNT();
			}
			rc.attackSquare(here.add(angle, dist));
		}
	}

	static void nextNT() {
		if (dist < 6) {
			dist = 16;
			angle = angle.rotateLeft();
		} else {
			dist--;
		}
	}
}
