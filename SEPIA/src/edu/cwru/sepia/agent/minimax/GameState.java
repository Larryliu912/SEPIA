package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.agent.minimax.AstarAgent;
import edu.cwru.sepia.agent.minimax.AstarAgent.MapLocation;
import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.util.Direction;

import java.util.*;

/**
 * This class stores all of the information the agent
 * needs to know about the state of the game. For example this
 * might include things like footmen HP and positions.
 *
 * Add any information or methods you would like to this class,
 * but do not delete or change the signatures of the provided methods.
 */
public class GameState {
	
	int myID = 0;
	int enemyID = 1;
	
	int enemyNum;
	int myNum;
	
	int mapX;
	int mapY;
	Boolean ourTurn;
	List<Integer> myUnitIDs = new ArrayList<>(); 
	List<Integer> enemyUnitIDs = new ArrayList<>();
	List<Integer> resourceIDs = new ArrayList<>();
	Set<AstarAgent.MapLocation> resourceLocations  = new HashSet<>();
	UnitState[] units = new UnitState[4];
	//We will refer the A-star path finding in our programming work 2
	
	AstarAgent pathFinding = new AstarAgent(0);
	//This class is used to record the status of the unit
	class UnitState {
		private int[] position = new int[2];
	    List<Stack<AstarAgent.MapLocation>> movePath = new ArrayList<>(); 
		private int HP, ID, ATK, range, player;
		public UnitState (int x, int y, int hp, int id, int atk, int range, int player) {
			this.position[0] = x;
			this.position[1] = y;
			this.HP = hp;
			this.ID = id;
			this.ATK = atk;
			this.range = range;
			this.player = player;
		}
		public UnitState(UnitState unitState) {
			this.position = unitState.position;
			this.HP = unitState.HP;
			this.ID = unitState.ID;
			this.ATK = unitState.ATK;
			this.range = unitState.range;
			this.player = unitState.player;

		}
		private void setHp(int HP) {
			this.HP = HP;
		}
		private void setPos(int x, int y) {
			this.position[0] = x;
			this.position[1] = y;
		}
		
	}


	//Create state for current status

	public GameState(State.StateView state) {
		
		int index = 0;
		
		this.myUnitIDs = state.getUnitIds(myID);
		this.enemyUnitIDs = state.getUnitIds(enemyID);
		this.resourceIDs = state.getAllResourceIds();
		this.enemyNum = enemyUnitIDs.size();
		this.myNum = myUnitIDs.size();
		
		this.mapX = state.getXExtent();
		this.mapY = state.getYExtent();
		
		this.ourTurn = true;
		
		//Get all the footman of us
		for (Integer unitID : myUnitIDs) {
			UnitView unit = state.getUnit(unitID);
			this.units[index++] = new UnitState(unit.getXPosition(), unit.getYPosition(), 
					unit.getHP(), unit.getID(), unit.getTemplateView().getBasicAttack(), 
					unit.getTemplateView().getRange(), unit.getTemplateView().getPlayer());
		}
		//Get all the archer of enemy
		for (Integer unitID : enemyUnitIDs) {
			UnitView unit = state.getUnit(unitID);
			this.units[index++] = new UnitState(unit.getXPosition(), unit.getYPosition(), 
					unit.getHP(), unit.getID(), unit.getTemplateView().getBasicAttack(), 
					unit.getTemplateView().getRange(), unit.getTemplateView().getPlayer());
		}
		//Get the resource for A-star path finding
		for (Integer resourceID:resourceIDs) {
			ResourceNode.ResourceView resource = state.getResourceNode(resourceID);
			this.resourceLocations.add(pathFinding.new MapLocation(resource.getXPosition(),
					resource.getYPosition(), null, 0));				
			}
		
    }
	
	//Create GameState for child state
	
	public GameState(GameState newState) {
		//Initializes the new GameState object with the same fields as the one passed in
		//Basically making a deep copy of a GameState object

		this.myUnitIDs.addAll(0, newState.myUnitIDs); 
		this.enemyUnitIDs.addAll(0, newState.enemyUnitIDs); 
		this.enemyNum = newState.enemyNum;
		this.myNum = newState.myNum;
		this.resourceLocations = new HashSet<MapLocation>(newState.resourceLocations);

		this.mapX = newState.mapX;
		this.mapY = newState.mapY;
		
		this.ourTurn = !newState.ourTurn;

		//Copy all units
		for (int i = 0; i < enemyNum+myNum; i++) {
			this.units[i] = new UnitState(newState.units[i].position[0],newState.units[i].position[1], 
					newState.units[i].HP, newState.units[i].ID, newState.units[i].ATK, 
					newState.units[i].range, newState.units[i].player);
		}
		
	}
	
    public double getUtility() {
    	double curDistance = 0;
        double utilitySum = 0;
        double attackableUnits = 0;
        
        //We found that if we use two footman to chase the one archer, other archer will attack one of the footman, and that will lead that footman die soon, so we have to 
        //force the archer to escape to avoid the hurt, thus, we care about the shortest distance between the footman and archer
    	for (int j = 0; j < myNum; j++ ) {
			AstarAgent.MapLocation ftmLoc = pathFinding.new MapLocation(units[j].position[0], units[j].position[1], null, 0);
			double min = Double.MAX_VALUE;
        for (int i = myNum; i < myNum + enemyNum; i++) {
    		AstarAgent.MapLocation arcLoc = pathFinding.new MapLocation(units[i].position[0], units[i].position[1], null, 0);
			if (pathFinding.AstarSearch(ftmLoc, arcLoc, mapX, mapY,null, resourceLocations).size() < min) {
				min = pathFinding.AstarSearch(ftmLoc, arcLoc, mapX, mapY,null, resourceLocations).size();
			}
    	}
        curDistance += min;
    }

        
        //This is actually the difference between our total HP and other total HP.
		int HPtotal = 0; 
		for (int i = 0; i < myNum + enemyNum; i++) {
			if (units[i].player == myID) {
				HPtotal += units[i].HP;
			}
			else {
				HPtotal -= units[i].HP;
			}
		}
        
		//It is obvious that two footman is better than one footman although the total HP is same
		//Because two footman means more DPS, so we take the unit number difference between us and enemy
		//Very important, the weight will be fixed when we can do more experiments.
		for (int i = 0; i < myNum; i++) {
			attackableUnits += getAttackable(units[i]).size();	
		}
        utilitySum = 1000*(myNum - enemyNum) + HPtotal - curDistance + attackableUnits; 
        return utilitySum;
        
    }
    
    //This function will create all successive children states and judge them by utility;
    public List<GameStateChild> getChildren() {
    	List<UnitState> aliveUnits = new ArrayList<>();
    	List<List<Action>> actions = new ArrayList<>();
    	for ( int i = 0; i < myNum+enemyNum; i++) {
        	units[i].movePath.clear();
    	}
        for (int i = 0; i < myNum; i++) {
			AstarAgent.MapLocation ftmLoc = pathFinding.new MapLocation(units[i].position[0], units[i].position[1], null, 0);
        	for (int j = myNum; j < myNum + enemyNum; j++ ) {
				AstarAgent.MapLocation arcLoc = pathFinding.new MapLocation(units[j].position[0], units[j].position[1], null, 0);
				Stack<AstarAgent.MapLocation> path =  pathFinding.AstarSearch(ftmLoc, arcLoc, mapX, mapY,null, resourceLocations);
				Stack<AstarAgent.MapLocation> arcPath =  pathFinding.AstarSearch(arcLoc, ftmLoc, mapX, mapY,null, resourceLocations);
				units[i].movePath.add(path);
				units[j].movePath.add(arcPath);
        	}
        }
    	if(ourTurn){
    		aliveUnits = aliveUnits(myID);
    	} else {
    		aliveUnits = aliveUnits(enemyID);
		}
    	
    	for (int i = 0 ; i < aliveUnits.size(); i++) {
    		actions.add(getActions(aliveUnits.get(i)));
    	}
    	return ChildrenStateByAction(actions);
    }
    
	//Get list of attackable units
	public List<Integer> getAttackable(UnitState unit) {
		List<Integer> attackableIDs = new ArrayList<>();
		for (int i = 0; i < myNum+enemyNum; i++) {
			double d = Math.floor(Math.hypot(Math.abs(unit.position[0] - units[i].position[0]), Math.abs(unit.position[1] - units[i].position[1])));
			if (units[i].player != unit.player && d <= unit.range) {
				attackableIDs.add(units[i].ID);
			}
		}
		return attackableIDs;
	}
    
	//Get list of alive units
	
	public List<UnitState> aliveUnits (int player) {
		
		List<UnitState> aliveUnits = new ArrayList<>();
		
		for (int i = 0; i < myNum+enemyNum; i++ ) {
			if (units[i].player == player && units[i].HP > 0) {
				aliveUnits.add(units[i]);
			}
		}
		return aliveUnits;
	}
    
    //We use this function to get the actions
	private List<Action> getActions(UnitState unit){
		//It is easy to see that a unit have only two action: attack or move, so we only care these two
		List<Action> actions = new ArrayList<Action>();
		//Get move from Astar pathFinding
		for (Stack<AstarAgent.MapLocation> move : unit.movePath) {
            if (move.size() != 0) {
				AstarAgent.MapLocation nextLoc = move.pop();
	            int xDiff = nextLoc.x -  unit.position[0];
	            int yDiff = nextLoc.y -  unit.position[1];
		        Direction nextDirection = getNextDirection(xDiff, yDiff);
		        if ( nextDirection != null ) {
			        actions.add(Action.createPrimitiveMove(unit.ID, nextDirection));
		        }
            }
		}
		//Get attackable units
		for(Integer id : getAttackable(unit)){
			actions.add(0, Action.createPrimitiveAttack(unit.ID, id));
		}	

		return actions;
	}
	
	//This function will create the actionMaps and return the children states of actions
	private List<GameStateChild> ChildrenStateByAction(List<List<Action>> actions){
		List<Map<Integer, Action>> actionMaps = new ArrayList<Map<Integer, Action>>();
		if(!actions.isEmpty()){
			List<Action> actionsForFirst = actions.get(0);	
			for(Action action : actionsForFirst){
				if(actions.size() == 1){
					Map<Integer, Action> actionMap = new HashMap<Integer, Action>();
					actionMap.put(action.getUnitId(), action);
					actionMaps.add(actionMap);
				} else {
					for(Action actionForOther : actions.get(1)){
						Map<Integer, Action> actionMap = new HashMap<Integer, Action>();
						actionMap.put(action.getUnitId(), action);
						actionMap.put(actionForOther.getUnitId(), actionForOther);
						actionMaps.add(actionMap);
					}
				}
			}
		}

		List<GameStateChild> children = new ArrayList<GameStateChild>();
		for(Map<Integer, Action> actionMap : actionMaps){
			GameState child = new GameState(this);
			for(Action action : actionMap.values()){
				child.applyAction(action);
			}
			children.add(new GameStateChild(actionMap, child));
		}
		return children;
	}

	//Apply action into the current child state
	private void applyAction(Action action) {
		if(action.getType().name().equals("PRIMITIVEMOVE")){
			DirectedAction directedAction = (DirectedAction) action;
			for (int i = 0; i < myNum+enemyNum; i ++) {
				if (this.units[i].ID == directedAction.getUnitId()) {
					this.units[i].setPos(units[i].position[0]+directedAction.getDirection().xComponent(),
										 units[i].position[1]+directedAction.getDirection().yComponent());
				}
			}
		} else {
			TargetedAction attackAction = (TargetedAction) action;
			attackAction.getUnitId();
			attackAction.getTargetId();
			int unit = 0, target = 0;
			for (int i = 0; i < myNum+enemyNum; i ++) {
				if (this.units[i].ID == attackAction.getUnitId()) {
					unit = i;
				}
				if (this.units[i].ID == attackAction.getTargetId()) {
					target = i;
				}
			}
			this.units[target].setHp(this.units[target].HP - this.units[unit].ATK); 
		}
	}
	
	
	//This is used to get the direction
    Direction getNextDirection(int xDiff, int yDiff) {
        // figure out the direction the footman needs to move in
        if(xDiff == 1 && yDiff == 1)
        {
            return Direction.SOUTHEAST;
        }
        else if(xDiff == 1 && yDiff == 0)
        {
            return Direction.EAST;
        }
        else if(xDiff == 1 && yDiff == -1)
        {
            return Direction.NORTHEAST;
        }
        else if(xDiff == 0 && yDiff == 1)
        {
            return Direction.SOUTH;
        }
        else if(xDiff == 0 && yDiff == -1)
        {
            return Direction.NORTH;
        }
        else if(xDiff == -1 && yDiff == 1)
        {
            return Direction.SOUTHWEST;
        }
        else if(xDiff == -1 && yDiff == 0)
        {
            return Direction.WEST;
        }
        else if(xDiff == -1 && yDiff == -1)
        {
            return Direction.NORTHWEST;
        }

        System.err.println("Invalid path. Could not determine direction");
        return null;
    }
	

}
