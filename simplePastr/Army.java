package simplePastr;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;

public class Army extends AbstractRobotType{
	
	private boolean inactive = false;
	private Team we; 
	private Team opponent;

	public Army(RobotController rc) {
		super(rc);
	}
	
	@Override
	protected void act() throws GameActionException {
		/**
		 * 1. attack opponents' pastr and noise tower
		 * 2. attack enemies
		 * 3. move towards own pastr
		 * 4. move randomly
		 * 
		 */
		
		MapLocation[] nextToAttack;
		
		//opponent's pastr?
		MapLocation[] pastrOpponentAll = rc.sensePastrLocations(opponent);
		if(pastrOpponentAll != null){
			nextToAttack = pastrOpponentAll;
		}else{
			//communicating opponents? 
			MapLocation[] robotsOpponentAll = rc.senseBroadcastingRobotLocations(opponent);
			if(robotsOpponentAll != null){
				nextToAttack = pastrOpponentAll;
			}
		}
		
		//Attack whatever is next
		MapLocation nextOpponent;
		if(nextToAttack != null){
			while(nextOpponent == null || !rc.canMove(nextOpponent){
				nextOpponent = (int) (nextToAttack.length * Math.random());
			}
		
			if(!rc.canAttackSquare(nextOpponent)){
				rc.sneak(nextOpponent);
			}else{
				rc.attackSquare(nextOpponent);
			}
			
		}else{
			//If there's nothing to attack, defend the own pastr
			MapLocation[] pastrOwnAll = rc.sensePastrLocations(we);
			MapLocation nextMove;
			if(pastrOwnAll !=  null){
				while(nextMove == null || !rc.canMove(nextMove){
					nextMove = (int) (pastrOwnAll.length * Math.random());
				}
				rc.move(nextMove);
			}else{
				//or move randomly because there is nothing to do
				if(rc.canMove()){
					Direction randomDirection = directions[(int) (Math.random() * directions.length)];
					rc.move(randomDirection);
				}
			}
		}
	}

	@Override
	protected void init() throws GameActionException {
		we = rc.getTeam();
		opponent = we.opponent();
	}
}