package ext_animorphs;

import battlecode.common.MapLocation;

public class PastrDisrupter {
	private NoiseTowerController ntc;
	private int counter;

	// private LocationMessager pastrLocs;

	public PastrDisrupter(NoiseTowerController ntc) {
		this.ntc = ntc;
		counter = 0;
	}

	/**
	 * Return location of a PASTR that is in range. Null if no available
	 * targets. Should cycle through available targets.
	 * 
	 * @return
	 */
	public MapLocation getTarget() {
		MapLocation[] locs = ntc.rc.sensePastrLocations(ntc.enemy);
		for (int i = counter; i < (locs.length + counter); i++) {
			if (ntc.rc.canAttackSquare(locs[i % locs.length])) {
				counter = i + 1;
				return locs[i % locs.length];
			}
		}
		return null;
	}
}
