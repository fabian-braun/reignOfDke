package ext_animorphs;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class NoiseTowerController extends Controller {
	public RobotMessager cmdIn;
	public PastrDisrupter dis;
	public SunrayHerder herd;
	public int counter;
	public BuggingHerder buggyHerd;

	public NoiseTowerController(RobotController rc) {
		super(rc);
		buggyHerd = new BuggingHerder(this);
		cmdIn = new RobotMessager(rc, this.id, CommsProtocol.ID_OFFSET_FROM_HQ);
		dis = new PastrDisrupter(this);
		counter = 0;
	}

	public void run() throws GameActionException {
		// Check cmd along channel to see what to do
		if (cmdIn.readMsg() == true) {

			switch (cmdIn.lastMsg) {
			case DISRUPT:
				// Try to disrupt their herding
				if (rc.isActive()) {
					MapLocation loc = dis.getTarget();
					if (loc != null) {
						rc.attackSquare(loc);
						return;
					}
				}
			case TOWER_HERD:
				if (!buggyHerd.center.equals(cmdIn.lastLoc)) {
					buggyHerd.setCenter(cmdIn.lastLoc);
				}

				// if(herd == null || !herd.center.equals(cmdIn.lastLoc)){
				// herd = new SunrayHerder(this,cmdIn.lastLoc);
				MapLocation target = buggyHerd.getNextLocation();
				if (rc.isActive()) {
					if (target != null) {
						rc.attackSquare(target);
						return;
					}
				}

				MapLocation[] pastrs = rc.sensePastrLocations(team);
				if (pastrs.length > 0) {
					if (pastrs[0].distanceSquaredTo(rc.getLocation()) > 5) {
						rc.selfDestruct();
					}
				}
				return;
			default:
				break;
			}
		} else {
			if (rc.isActive()) {
				// try disrupt
				MapLocation loc = dis.getTarget();
				if (loc != null) {
					rc.attackSquare(loc);
					counter++;
					return;
				}
				MapLocation[] pastrs = rc.sensePastrLocations(enemy);
				if (pastrs.length > 0) {
					rc.selfDestruct();
				}
			}
		}
		return;
	}
}
