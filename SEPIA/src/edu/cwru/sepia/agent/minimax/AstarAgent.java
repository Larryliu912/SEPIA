package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class AstarAgent extends Agent {

    public class MapLocation
    {
        public int x, y;
        private MapLocation cameFrom;
        private float cost;

        public MapLocation(int x, int y, MapLocation cameFrom, float cost)
        {
            this.x = x;
            this.y = y;
            this.cameFrom = cameFrom;
            this.cost = cost;
        }
        
        public float getcost() {
        		return cost;
        }
        public MapLocation getparent() {
    		return cameFrom;
    }
    }

    Stack<MapLocation> path;
    int footmanID, townhallID, enemyFootmanID;
    MapLocation nextLoc;

    private long totalPlanTime = 0; // nsecs
    private long totalExecutionTime = 0; //nsecs

    public AstarAgent(int playernum)
    {
        super(playernum);

    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
        // get the footman location
        List<Integer> unitIDs = newstate.getUnitIds(playernum);

        if(unitIDs.size() == 0)
        {
            System.err.println("No units found!");
            return null;
        }

        footmanID = unitIDs.get(0);

        // double check that this is a footman
        if(!newstate.getUnit(footmanID).getTemplateView().getName().equals("Footman"))
        {
            System.err.println("Footman unit not found");
            return null;
        }

        // find the enemy playernum
        Integer[] playerNums = newstate.getPlayerNumbers();
        int enemyPlayerNum = -1;
        for(Integer playerNum : playerNums)
        {
            if(playerNum != playernum) {
                enemyPlayerNum = playerNum;
                break;
            }
        }

        if(enemyPlayerNum == -1)
        {
            System.err.println("Failed to get enemy playernumber");
            return null;
        }

        // find the townhall ID
        List<Integer> enemyUnitIDs = newstate.getUnitIds(enemyPlayerNum);

        if(enemyUnitIDs.size() == 0)
        {
            System.err.println("Failed to find enemy units");
            return null;
        }

        townhallID = -1;
        enemyFootmanID = -1;
        for(Integer unitID : enemyUnitIDs)
        {
            Unit.UnitView tempUnit = newstate.getUnit(unitID);
            String unitType = tempUnit.getTemplateView().getName().toLowerCase();
            if(unitType.equals("townhall"))
            {
                townhallID = unitID;
            }
            else if(unitType.equals("footman"))
            {
                enemyFootmanID = unitID;
            }
            else
            {
                System.err.println("Unknown unit type");
            }
        }

        if(townhallID == -1) {
            System.err.println("Error: Couldn't find townhall");
            return null;
        }

        long startTime = System.nanoTime();
        path = findPath(newstate);
        totalPlanTime += System.nanoTime() - startTime;

        return middleStep(newstate, statehistory);
    }

    @Override
    public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory) {
        long startTime = System.nanoTime();
        long planTime = 0;

        Map<Integer, Action> actions = new HashMap<Integer, Action>();

        if(shouldReplanPath(newstate, statehistory, path)) {
            long planStartTime = System.nanoTime();
            path = findPath(newstate);
            planTime = System.nanoTime() - planStartTime;
            totalPlanTime += planTime;
        }

        Unit.UnitView footmanUnit = newstate.getUnit(footmanID);

        int footmanX = footmanUnit.getXPosition();
        int footmanY = footmanUnit.getYPosition();
        if(!path.empty() && (nextLoc == null || (footmanX == nextLoc.x && footmanY == nextLoc.y))) {

            // stat moving to the next step in the path
            nextLoc = path.pop();
            System.out.println("Moving to (" + nextLoc.x + ", " + nextLoc.y + ")");
        }

        if(nextLoc != null && (footmanX != nextLoc.x || footmanY != nextLoc.y))
        {
            int xDiff = nextLoc.x - footmanX;
            int yDiff = nextLoc.y - footmanY;

            // figure out the direction the footman needs to move in
            Direction nextDirection = getNextDirection(xDiff, yDiff);
            
            actions.put(footmanID, Action.createPrimitiveMove(footmanID, nextDirection));
            
        } else {
            Unit.UnitView townhallUnit = newstate.getUnit(townhallID);

            // if townhall was destroyed on the last turn
            if(townhallUnit == null) {
                terminalStep(newstate, statehistory);
                return actions;
            }

            if(Math.abs(footmanX - townhallUnit.getXPosition()) > 1 ||
                    Math.abs(footmanY - townhallUnit.getYPosition()) > 1)
            {
                System.err.println("No available path");
                totalExecutionTime += System.nanoTime() - startTime - planTime;
                return actions;
            }
            else {
                System.out.println("Attacking TownHall");
                // if no more movements in the planned path then attack
                actions.put(footmanID, Action.createPrimitiveAttack(footmanID, townhallID));
            }
        }

        totalExecutionTime += System.nanoTime() - startTime - planTime;
        return actions;
    }

    @Override
    public void terminalStep(State.StateView newstate, History.HistoryView statehistory) {
        System.out.println("Total turns: " + newstate.getTurnNumber());
        System.out.println("Total planning time: " + totalPlanTime/1e9);
        System.out.println("Total execution time: " + totalExecutionTime/1e9);
        System.out.println("Total time: " + (totalExecutionTime + totalPlanTime)/1e9);
    }

    @Override
    public void savePlayerData(OutputStream os) {

    }

    @Override
    public void loadPlayerData(InputStream is) {

    }

    /**
     * You will implement this method.
     *
     * This method should return true when the path needs to be replanned
     * and false otherwise. This will be necessary on the dynamic map where the
     * footman will move to block your unit.
     *
     * @param state
     * @param history
     * @param currentPath
     * @return
     */
    private boolean shouldReplanPath(State.StateView state, History.HistoryView history, Stack<MapLocation> currentPath)
    {
    	
    	//Because we failed to config the maze_16x16h_dynamic.xml, it always return error, we implement this function ideally. 
    	//It will check the possible enemy and then Replan the path
        {
        Unit.UnitView myUnit = state.getUnit(footmanID);
        Unit.UnitView enemyUnit = null;

        if (enemyFootmanID != -1) {
            enemyUnit = state.getUnit(enemyFootmanID);
        }

        if (enemyUnit == null) {
            return false;
        }

        MapLocation enemyLocation = new MapLocation(enemyUnit.getXPosition(), enemyUnit.getYPosition(), null, 0 );

        Iterator<MapLocation> iterator = currentPath.iterator();
        while(iterator.hasNext()) {
            MapLocation location = iterator.next();
            if (location.x == enemyLocation.x
                    && location.y == enemyLocation.y
                    && Math.abs(myUnit.getXPosition() - enemyUnit.getXPosition()) <= 1
                    && Math.abs(myUnit.getYPosition() - enemyUnit.getYPosition()) <= 1) {
                return true;
            }
        }
        }
        return false;
    }

    /**
     * This method is implemented for you. You should look at it to see examples of
     * how to find units and resources in Sepia.
     *
     * @param state
     * @return
     */
    private Stack<MapLocation> findPath(State.StateView state)
    {
        Unit.UnitView townhallUnit = state.getUnit(townhallID);
        Unit.UnitView footmanUnit = state.getUnit(footmanID);

        MapLocation startLoc = new MapLocation(footmanUnit.getXPosition(), footmanUnit.getYPosition(), null, 0);

        MapLocation goalLoc = new MapLocation(townhallUnit.getXPosition(), townhallUnit.getYPosition(), null, 0);

        MapLocation footmanLoc = null;
        if(enemyFootmanID != -1) {
            Unit.UnitView enemyFootmanUnit = state.getUnit(enemyFootmanID);
            footmanLoc = new MapLocation(enemyFootmanUnit.getXPosition(), enemyFootmanUnit.getYPosition(), null, 0);
        }

        // get resource locations
        List<Integer> resourceIDs = state.getAllResourceIds();
        Set<MapLocation> resourceLocations = new HashSet<MapLocation>();
        for(Integer resourceID : resourceIDs)
        {
            ResourceNode.ResourceView resource = state.getResourceNode(resourceID);

            resourceLocations.add(new MapLocation(resource.getXPosition(), resource.getYPosition(), null, 0));
        }

        return AstarSearch(startLoc, goalLoc, state.getXExtent(), state.getYExtent(), footmanLoc, resourceLocations);
    }
    /**
     * This is the method you will implement for the assignment. Your implementation
     * will use the A* algorithm to compute the optimum path from the start position to
     * a position adjacent to the goal position.
     *
     * You will return a Stack of positions with the top of the stack being the first space to move to
     * and the bottom of the stack being the last space to move to. If there is no path to the townhall
     * then return null from the method and the agent will print a message and do nothing.
     * The code to execute the plan is provided for you in the middleStep method.
     *
     * As an example consider the following simple map
     *
     * F - - - -
     * x x x - x
     * H - - - -
     *
     * F is the footman
     * H is the townhall
     * x's are occupied spaces
     *
     * xExtent would be 5 for this map with valid X coordinates in the range of [0, 4]
     * x=0 is the left most column and x=4 is the right most column
     *
     * yExtent would be 3 for this map with valid Y coordinates in the range of [0, 2]
     * y=0 is the top most row and y=2 is the bottom most row
     *
     * resourceLocations would be {(0,1), (1,1), (2,1), (4,1)}
     *
     * The path would be
     *
     * (1,0)
     * (2,0)
     * (3,1)
     * (2,2)
     * (1,2)
     *
     * Notice how the initial footman position and the townhall position are not included in the path stack
     *
     * @param start Starting position of the footman
     * @param goal MapLocation of the townhall
     * @param xExtent Width of the map
     * @param yExtent Height of the map
     * @param resourceLocations Set of positions occupied by resources
     * @return Stack of positions with top of stack being first move in plan
     */
    public Stack<MapLocation> AstarSearch(MapLocation start, MapLocation goal, int xExtent, int yExtent, MapLocation enemyFootmanLoc, Set<MapLocation> resourceLocations)
    {
        // return an empty path
    		int sx = start.x;   //sx is short for start.x
    		int sy = start.y;
    		int gx = goal.x;
    		int gy = goal.y;
    		
    		ArrayList<MapLocation> openList = new ArrayList<MapLocation>();    //openlist
    		ArrayList<MapLocation> closeList = new ArrayList<MapLocation>();   //closelist
    		
    		openList.add(new MapLocation(sx, sy, null, 0));
    		MapLocation lastLoc = null;
    		while(openList.size() > 0) {    //when openList is not empty, continuing find pop out element to search its neighbor
    			MapLocation temp = getLowestCost(openList, goal);
    			if(Math.abs(temp.x - goal.x) <= 1 && Math.abs(temp.y- goal.y) <= 1) {   // if the goal is one of the element's neighbor
    				lastLoc = temp;
    				break;
    			}
    			ArrayList<MapLocation> neighbors = new ArrayList<MapLocation>();
    			neighbors = getNeighbors(temp, goal, enemyFootmanLoc, resourceLocations);
    			for (MapLocation neighbor : neighbors) {
    				if(!INopenListANDcloseList(neighbor, closeList, openList))      // the new element added to the openlist should not be in the closed list and not already in the openlist 
    					if(neighbor.x >= 0 && neighbor.x < xExtent && neighbor.y >= 0 && neighbor.y < yExtent)   // should be in the map
    						openList.add(neighbor);
    			}		
    			openList.remove(temp); //remove the object from the openList and add it to the closeList
			closeList.add(temp);
    		}
    		Stack<MapLocation> path = new Stack<MapLocation>();
    		if(lastLoc != null)
    			while(lastLoc.getparent() != null) {      // get a path
    				path.push(lastLoc);
    				lastLoc = lastLoc.getparent();
    			}
        return path;
    }
    
//    public double getHopDistance(MapLocation start, MapLocation goal, int xExtent, int yExtent, MapLocation otherFootmanLoc, HashSet<MapLocation> resourceLocations){
//    	if (start.x == goal.x && start.y == goal.y) {
//    		return 0;
//    	}
//    	if (otherFootmanLoc != null && goal.x == otherFootmanLoc.x && goal.y == otherFootmanLoc.y) {
//    		return (double)Integer.MAX_VALUE;
//    	}
//    	
//    	Stack<MapLocation> foundPath = AstarSearch(start, goal, xExtent, yExtent, otherFootmanLoc, resourceLocations);
//
//    	return (double)foundPath.size();
//    }
    
    /**
     * to check whether the neighor is in the closelist or the openlist
     * @return true = is in one of the list/ false = not in the two lists
     */
    public boolean INopenListANDcloseList(MapLocation neighbor, ArrayList<MapLocation> closeList, ArrayList<MapLocation> openList) {
    		for (MapLocation temp : closeList) {
    			if(temp.x == neighbor.x && temp.y == neighbor.y)
    				return true;
    		}
    		for (MapLocation temp : openList) {
    			if(temp.x == neighbor.x && temp.y == neighbor.y)
    				return true;
    		}
    		return false;
    }
    
    /**
     * compute the f(x)
     *
     * @param current is the current location
     * @param goal is the goal location
     * @return the value of f(x)
     */
    public float fcost(MapLocation current, MapLocation goal){  // f(x) = g(x) + h(x)
    		return current.getcost() + HeuristicFunction(current, goal);
    }
    
    /**
     * compute the Heuristic Function h(x)
     *
     * @param current is the current location
     * @param goal is the goal location
     * @return the value of h(x)
     */
    public int HeuristicFunction(MapLocation current, MapLocation goal) {  // h(x) = max(|x1 - x2|, |y1 - y2|)
    		if(Math.abs(current.x - goal.x) > Math.abs(current.y - goal.y))
    			return Math.abs(current.x - goal.x);
    		return Math.abs(current.y - goal.y);
    }
    
    /**
     * find the lowest f(x) value in the openlist
     *
     * @param List is the open list
     * @param goal is the goal location
     * @return the MapLocation whose f(x) is the smallest in the openList
     */
    public MapLocation getLowestCost(ArrayList<MapLocation> List, MapLocation goal)  // get the lowest cost F in the list
	{
    		MapLocation lowestCost = List.get(0); // set the first location in the map as the lowest one
		
		for(int i = 0; i < List.size(); i++) // find the lowest one
			if(List.get(i).getcost() + fcost(List.get(i), goal) < lowestCost.getcost() + fcost(lowestCost, goal)) 
				lowestCost = List.get(i); 

		return lowestCost;
	}
    
    /**
     * check if the location (x,y) is occupied by other unit
     *
     * @param x, the coordinate x
     * @param y, the coordinate y  
     * @param goal is the goal location
     * @param enemyFootmanLoc is the enemy Footman Lococation
     * @param resourceLocations is a set of resource 
     * @return true = (x,y) is occupied/ false = (x,y) is not occupied
     */
    public boolean checkoccupied(Integer x, Integer y, MapLocation goal, MapLocation enemyFootmanLoc, Set<MapLocation> resourceLocations) {
    		for (MapLocation resource : resourceLocations) {  // not occupied by a resource unit
    			if(resource.x == x && resource.y == y)
    				return true;
    		}
    		if(goal.x == x && goal.y == y)   // not occupied by a goal unit
    			return true;
    		if(enemyFootmanLoc != null)
    			if(enemyFootmanLoc.x == x && enemyFootmanLoc.y == y)   // not occupied by a enemy Footman unit
    				return true;
		return false;
    }
    
    /**
     * get all the vaild neighbor of current location
     *
     * @param current is the current location
     * @param goal is the goal location
     * @param enemyFootmanLoc is the enemy Footman Lococation
     * @param resourceLocations is a set of resource 
     * @return a list of neighbor location
     */
	public ArrayList<MapLocation> getNeighbors(MapLocation current, MapLocation goal, MapLocation enemyFootmanLoc, Set<MapLocation> resourceLocations)
	{
		ArrayList<MapLocation> neighbors = new ArrayList<MapLocation>(); //The return list of all empty neighbors
		int x = current.x;
		int y = current.y;
		int East = x + 1;
		int West = x - 1;
		int South = y + 1;
		int North = y - 1;		
		int tempX = 0, tempY = 0;
		
		//checking all 8 possible spaces
		int j = 7;
		while(j != -1) {
			switch(j)
			{
				case 0: //x + 1, y
					tempX = East;
					tempY = y;
					break;
				case 1: //x + 1, y + 1
					tempX = East;
					tempY = South;
					break;
				case 2: //x + 1, y - 1
					tempX = East;
					tempY = North;
					break;
				case 3: //x, y + 1
					tempX = x;
					tempY = South;
					break;
				case 4: //x, y - 1
					tempX = x;
					tempY = North;
					break;
				case 5: //x - 1, y
					tempX = West;
					tempY = y;
					break;
				case 6: //x - 1, y + 1
					tempX = West;
					tempY = South;
					break;
				case 7: //x - 1, y - 1
					tempX = West;
					tempY = North;
					break;
				default:
					break;
			}
			float cost = (float) Math.sqrt(Math.abs(tempX - x)*Math.abs(tempX - x) + Math.abs(tempY - y)*Math.abs(tempY - y)) + current.getcost();  // g(x) is the Euclidean distance
			MapLocation neighbor = new MapLocation(tempX, tempY, current, cost);
			if(!checkoccupied(tempX, tempY, goal, enemyFootmanLoc, resourceLocations)) //check if it's a empty space
				neighbors.add(neighbor);
			j--;
		}
		
		return neighbors;
	}

    /**
     * Primitive actions take a direction (e.g. NORTH, NORTHEAST, etc)
     * This converts the difference between the current position and the
     * desired position to a direction.
     *
     * @param xDiff Integer equal to 1, 0 or -1
     * @param yDiff Integer equal to 1, 0 or -1
     * @return A Direction instance (e.g. SOUTHWEST) or null in the case of error
     */
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
