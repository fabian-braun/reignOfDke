package ext_animorphs;

import battlecode.common.Clock;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

/**
 * Finds good pastr locations taking into account cow growth and distance away
 * from other recommended locations.
 * 
 * @author Nirvan
 *
 */
public class SimplePastrFinder {
	public Controller c;
	public RobotController rc;

	public static final int PASTR_NUM = 4; // CONSTANT: Number of pastr
											// locations class finds
	public static final int CELL_GEN = 13627; // CONSTANT: large prime used for
												// randomly generating cells to
												// check
	public static final int CELL_START = 31081;

	public static final int REC_GEN = 5851;
	public static final int REC_START = 6661;

	public MapLocation[] pastrRecs = new MapLocation[PASTR_NUM];
	public double[] growthRecs = new double[PASTR_NUM];

	public double[][] cowGrowth;
	public int cellLoc; // variable to generate next coordinates

	public SimplePastrFinder(Controller c) {
		this.c = c;
		this.rc = c.rc;

		cowGrowth = rc.senseCowGrowth();
		cellLoc = CELL_START;
	}

	public void calc(int byteLimit) {
		// localize
		int start = Clock.getBytecodeNum();
		int width = c.width;
		int height = c.height;
		int mod = height * width;
		int PASTR_NUM = SimplePastrFinder.PASTR_NUM;
		int REC_GEN = SimplePastrFinder.REC_GEN;
		int REC_START = SimplePastrFinder.REC_START;
		int CELL_GEN = SimplePastrFinder.CELL_GEN;
		int cellLoc = this.cellLoc;
		int num = 0;

		while (Clock.getBytecodeNum() - start < byteLimit) {
			num++;
			cellLoc = cellLoc % mod;

			int x = cellLoc % width;
			int y = cellLoc / width;

			MapLocation loc = new MapLocation(x, y);
			double growth = cowGrowth[x][y];

			int hash = ((cellLoc + REC_START) * REC_GEN) % PASTR_NUM;
			if (growth > growthRecs[hash]) {
				growthRecs[hash] = growth;
				pastrRecs[hash] = loc;
			}

			cellLoc += CELL_GEN;
		}
		rc.setIndicatorString(0, "simple:" + num);

		// unlocalize
		this.cellLoc = cellLoc;
	}
}
