package ext_zeroxg;

import battlecode.common.Clock;
import battlecode.common.GameConstants;

public abstract class PerfCounter {
	private static final int counterCost = 28;
	private static int bcStart;
	private static int turnStart;

	public static void start() {
		bcStart = Clock.getBytecodeNum();
		turnStart = Clock.getRoundNum();
	}

	public static int getCount() {
		return (Clock.getRoundNum() - turnStart) * GameConstants.BYTECODE_LIMIT
				+ (Clock.getBytecodeNum() - bcStart) - counterCost;
	}

	public static void printCount() {
		System.out.println("Cost: " + getCount());
	}

	public static void printTotal() {
		System.out.println("BC: " + Clock.getBytecodeNum());
	}
}
