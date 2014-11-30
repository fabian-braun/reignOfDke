package ext_animorphs;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class BFS {
	public Controller c;
	public RobotController rc;

	private int MAX_STATES = 60000;

	public int queueStart;
	public int queueEnd;
	public int[] queueX;
	public int[] queueY;
	public int[] queueR;
	public MapLocation[] queueD;
	public boolean[][] seen;
	public DirectionMap map;

	public BFS(Controller c) {
		this.c = c;
		rc = c.rc;
		queueStart = 0;
		queueEnd = 0;
		queueX = new int[MAX_STATES];
		queueY = new int[MAX_STATES];
		queueR = new int[MAX_STATES];
		queueD = new MapLocation[MAX_STATES];
		seen = new boolean[GameConstants.MAP_MAX_WIDTH][GameConstants.MAP_MAX_HEIGHT];
		map = new DirectionMap(rc);
	}

	public void add(MapLocation loc) throws GameActionException {
		int x = loc.x;
		int y = loc.y;
		queueX[queueEnd] = x;
		queueY[queueEnd] = y;
		queueR[queueEnd] = 1;
		queueD[queueEnd] = loc;
		map.broadcastDirection(loc, loc, Direction.OMNI);
		queueEnd++;
	}

	public boolean calc(int byteLimit) throws GameActionException {
		int start = Clock.getBytecodeNum();
		int num = 0;

		// localize statics
		int width = c.width;
		int height = c.height;

		// localize pointers
		int[] queueX = this.queueX;
		int[] queueY = this.queueY;
		int[] queueR = this.queueR;
		MapLocation[] queueD = this.queueD;
		boolean[][] seen = this.seen;
		Direction[][] dirs = Mover.OPP_DIR;
		DirectionMap map = this.map;

		// localize variables
		int queueStart = this.queueStart;
		int queueEnd = this.queueEnd;

		while (Clock.getBytecodeNum() - start < byteLimit) {
			num++;
			if (queueStart == queueEnd) {
				// unlocalize
				this.queueStart = queueStart;
				this.queueEnd = queueEnd;
				return true;
			}
			// pop
			int x = queueX[queueStart];
			int y = queueY[queueStart];
			int r = queueR[queueStart];
			MapLocation d = queueD[queueStart];
			queueStart++;

			r--;

			// stay same
			if (r > 0) {
				queueX[queueEnd] = x;
				queueY[queueEnd] = y;
				queueR[queueEnd] = r;
				queueD[queueEnd] = d;
				queueEnd++;
				continue;
			}

			for (int i = -1; i <= 1; i++) {
				for (int j = -1; j <= 1; j++) {
					int nx = x + i;
					int ny = y + j;
					if (nx < width && nx >= 0 && ny < height && ny >= 0
							&& !seen[nx][ny]) {
						seen[nx][ny] = true;
						MapLocation next = new MapLocation(nx, ny);
						TerrainTile t = rc.senseTerrainTile(next);
						switch (t) {
						case NORMAL:
							switch (i * j) {
							case 0: // adj
								map.broadcastDirection(next, d,
										dirs[j + 1][i + 1]);
								queueX[queueEnd] = nx;
								queueY[queueEnd] = ny;
								queueR[queueEnd] = 4;
								queueD[queueEnd] = d;
								queueEnd++;
								continue;
							case 1: // diag
							case -1:
								map.broadcastDirection(next, d,
										dirs[j + 1][i + 1]);
								queueX[queueEnd] = nx;
								queueY[queueEnd] = ny;
								queueR[queueEnd] = 6;
								queueD[queueEnd] = d;
								queueEnd++;
								continue;

							default:
								System.out.println("ASDFASDF");
							}
							continue;
						case VOID:
							continue;
						case ROAD:
							switch (i * j) {
							case 0: // adj
								map.broadcastDirection(next, d,
										dirs[j + 1][i + 1]);
								queueX[queueEnd] = nx;
								queueY[queueEnd] = ny;
								queueR[queueEnd] = 2;
								queueD[queueEnd] = d;
								queueEnd++;
								continue;
							case 1: // diag
							case -1:
								map.broadcastDirection(next, d,
										dirs[j + 1][i + 1]);
								queueX[queueEnd] = nx;
								queueY[queueEnd] = ny;
								queueR[queueEnd] = 3;
								queueD[queueEnd] = d;
								queueEnd++;
								continue;
							default:
								System.out.println("ASDFASDF");
							}
							continue;
						default:
							System.out.println("ASDFASDF");
							continue;
						}
					}
				}
			}

		}
		// rc.setIndicatorString(0, "bfs: " + num);

		// unlocalize
		this.queueStart = queueStart;
		this.queueEnd = queueEnd;

		return false;
	}
}
