package reignierOfDKE;

import java.util.Arrays;

import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Team {

	public final int id;
	private final RobotController rc;
	private int soldierCount = 0;
	private Task task;
	private MapLocation target;

	public Team(int id, RobotController rc) {
		this.id = id;
		this.rc = rc;
	}

	public int getSoldierCount() {
		return soldierCount;
	}

	public void setSoldierCount(int soldierCount) {
		this.soldierCount = soldierCount;
	}

	public void incSoldierCount() {
		this.soldierCount++;
	}

	public Task getTask() {
		return task;
	}

	public MapLocation getTarget() {
		return target;
	}

	public void setTask(Task task, MapLocation target) {
		Channel.broadcastTask(rc, task, target, id);
		this.task = task;
		this.target = target;
	}

	public static void updateSoldierCount(RobotController rc, Team[] teams) {
		for (Team team : teams) {
			team.setSoldierCount(0);
		}
		for (int id = 0; id < GameConstants.MAX_ROBOTS; id++) {
			if (Channel.isAlive(rc, id)) {
				int teamId = Channel.getTeamIdOfSoldier(rc, id);
				teams[teamId].incSoldierCount();
			}
		}
		// Broadcast the new member counts
		for (Team team : teams) {
			Channel.broadcastSoldierCountOfTeam(rc, team.id, team.soldierCount);
		}
	}

	public static void updateTeamLocation(RobotController rc, Team[] teams) {
		int[] count = new int[teams.length];
		MapLocation[] avgLoc = new MapLocation[teams.length];
		Arrays.fill(avgLoc, new MapLocation(0, 0));
		for (int id = 0; id < GameConstants.MAX_ROBOTS; id++) {
			if (Channel.isAlive(rc, id)) {
				int teamId = Channel.getTeamIdOfSoldier(rc, id);
				count[teamId]++;
				MapLocation loc = Channel.getLocationOfSoldier(rc, id);
				avgLoc[teamId] = avgLoc[teamId].add(loc.x, loc.y);
			}
		}
		for (int i = 0; i < avgLoc.length; i++) {
			if (count[i] > 0) {
				avgLoc[i] = new MapLocation(avgLoc[i].x / count[i], avgLoc[i].y
						/ count[i]);
				Channel.broadcastPositionalCenterOfTeam(rc, i, avgLoc[i]);
			}
		}
		rc.setIndicatorString(2, avgLoc[0].toString());
	}

	public static Team[] getTeams(RobotController rc) {
		Team[] teams = new Team[3];
		teams[0] = new Team(0, rc);
		teams[1] = new Team(1, rc);
		teams[2] = new Team(2, rc);
		return teams;
	}
}
