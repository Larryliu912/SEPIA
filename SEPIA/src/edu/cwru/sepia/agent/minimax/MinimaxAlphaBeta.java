package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MinimaxAlphaBeta extends Agent {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1;
	
	private final int numPlys;
	
    public MinimaxAlphaBeta(int playernum, String[] args)
    {
        super(playernum);

        if(args.length < 1)
        {
            System.err.println("You must specify the number of plys");
            System.exit(1);
        }
        numPlys = Integer.parseInt(args[0]);
    }
    
	private static final Comparator<GameStateChild> COMPARATOR = (child1, child2) -> {
    	if(child1.state.getUtility() > child2.state.getUtility()){
    		return -1;
    	} else if (child1.state.getUtility() < child2.state.getUtility()){
    		return 1;
    	} else {
    		return 0;
    	}
	};

    @Override
    public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
        return middleStep(newstate, statehistory);
    }

    @Override
    public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory) {
        GameStateChild bestChild = alphaBetaSearch(new GameStateChild(newstate),
                numPlys,
                Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY);

        return bestChild.action;
    }

    @Override
    public void terminalStep(State.StateView newstate, History.HistoryView statehistory) {

    }

    @Override
    public void savePlayerData(OutputStream os) {

    }

    @Override
    public void loadPlayerData(InputStream is) {

    }

    /**
     * You will implement this.
     *
     * This is the main entry point to the alpha beta search. Refer to the slides, assignment description
     * and book for more information.
     *
     * Try to keep the logic in this function as abstract as possible (i.e. move as much SEPIA specific
     * code into other functions and methods)
     *
     * @param node The action and state to search from
     * @param depth The remaining number of plys under this node
     * @param alpha The current best value for the maximizing node from this node to the root
     * @param beta The current best value for the minimizing node from this node to the root
     * @return The best child of this node with updated values
     */
    public GameStateChild alphaBetaSearch(GameStateChild node, int depth, double alpha, double beta)
    {    	
    	//Node is an action
    	//Let us create the similar construction like the pseudo code in book
    	
    	double max = maxValue(node,depth,alpha,beta);
    	
    	GameStateChild result = getStateByUtility(node, max);
        return result;
    }
    
    public double maxValue (GameStateChild node, int depth, double alpha, double beta) {
		
    	double max = Double.NEGATIVE_INFINITY;
    	
    	if (terminalTest(node,depth)) {
			return node.state.getUtility();
		}
    	
    	for (GameStateChild child : orderChildrenWithHeuristics (node.state.getChildren())) {
    		max = Math.max(max, minValue(child, depth - 1, alpha, beta));
    		if (max >= beta) {
    			return max;
    		}
    		alpha = Math.max(alpha, max);
    	} 
    	return max;
    	
    }
    
    public double minValue (GameStateChild node, int depth, double alpha, double beta) {
		
    	double min = Double.NEGATIVE_INFINITY;
    	
    	if (terminalTest(node,depth)) {
			return node.state.getUtility();
		}
    	
    	for (GameStateChild child : orderChildrenWithHeuristics (node.state.getChildren())) {
    		min = Math.min(min, minValue(child, depth - 1, alpha, beta));
    		if (min <= alpha) {
    			return min;
    		}
    		beta = Math.min(alpha, min);
    	} 
    	return min;
    }
    
    public boolean terminalTest (GameStateChild node, int depth) {
		
    	if (depth == 0) {
    		return true;
    	}
    	
    	return false;
    }
    
    public List<GameStateChild> orderChildrenWithHeuristics(List<GameStateChild> children){ 
        List<GameStateChild> priority = new LinkedList<GameStateChild>();
        List<GameStateChild> others   = new LinkedList<GameStateChild>();
        //Attack is the most important things in this game, so, in heuristic function, we take ATTACK first rule
        for(GameStateChild child : children){
	        for(Action action : child.action.values()){
	    		if(action.getType().name().equals("PRIMITIVEATTACK")){
	    			priority.add(0,child);
	    		}
	    	}
	        others.add(child);
        }
        others.sort(COMPARATOR);
        priority.addAll(others);
        return priority;
    }
    
    private GameStateChild getStateByUtility(GameStateChild node, double value) {
    	//This function is used to return the childState by utiliy function
    	List<GameStateChild> children = node.state.getChildren();
    	for(GameStateChild child : children){
    		if(child.state.getUtility() == value){
    			return child;
    		}
    	}
        children.sort(COMPARATOR);
    	return children.get(0);
	}

}
