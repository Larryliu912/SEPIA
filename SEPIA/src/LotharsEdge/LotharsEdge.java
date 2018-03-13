package LotharsEdge;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.ResourceNode.Type;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Template.TemplateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

public class LotharsEdge extends Agent {
	private static final long serialVersionUID = -7481143097108592969L;
	static int playerNo = 0;
	static int neededPeasants = 3;
	static int neededFootman = 2;
	static String townHall = "TownHall";
	static String peasant = "Peasant";
	static String farm = "Farm";
	static String barracks = "Barracks";
	static String footman = "Footman";
	
	public LotharsEdge(int playerNo) {
		super(playerNo);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Map<Integer, Action> initialStep(StateView state, HistoryView statehistory) {
		// TODO Auto-generated method stub
        return middleStep(state, statehistory);
	}

	@Override
	public void loadPlayerData(InputStream arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public Map<Integer, Action> middleStep(StateView state, HistoryView statehistory) {
		
		//List for team action
		List<Integer> allUnitIds = state.getAllUnitIds();
		List<Integer> townHalls = findUnit(allUnitIds, state, townHall);
		List<Integer> peasants = findUnit(allUnitIds, state, peasant);
		
		Map<Integer, Action> actions = new HashMap<Integer,Action>();
		
		//As all the units cost gold and wood, for convenient
		//we use WC represent wood cost, GC represent Gold cost
		
		int peasantGC =  state.getUnit(peasants.get(0)).getTemplateView().getGoldCost();
		int farmGC = state.getTemplate(playerNo, farm).getGoldCost();
		int farmWC = state.getTemplate(playerNo, farm).getWoodCost();
		int barracksGC = state.getTemplate(playerNo, barracks).getGoldCost();
		int barracksWC = state.getTemplate(playerNo, barracks).getWoodCost();
		int footmanGC = state.getTemplate(playerNo, footman).getGoldCost();
		
		//current Golden and Wood
		int curG = state.getResourceAmount(playerNo, ResourceType.GOLD);
		int curW = state.getResourceAmount(playerNo, ResourceType.WOOD);
		
		if (peasants.size() < neededPeasants ) //build more peasants
		{
			//build 2 more peasants
			if ( curG >= peasantGC )
			{
				TemplateView peasantTemplate = state.getTemplate(playerNo, peasant);
				Action buildPeasants = Action.createCompoundProduction(townHalls.get(0), peasantTemplate.getID());
				actions.put(townHalls.get(0), buildPeasants);
			}
			
			//if we don't have enough gold to product peasants, then collect gold
			
			collectResource(peasants, actions, state, townHalls.get(0), Type.GOLD_MINE); //collect gold
		}
		else if (peasants.size() == neededPeasants) //full peasants
		{
			if (findUnit(allUnitIds, state, farm).size() <= 0) //check if there is no Farm
			{
				//collect gold and wood until there's enough for a farm
				if (curG <= farmGC)
				{
					collectResource(peasants, actions, state, townHalls.get(0), Type.GOLD_MINE);
				}
				else if (curW <= farmWC)
				{
					collectResource(peasants, actions, state, townHalls.get(0), Type.TREE);
				}
				else
				{					
					int peasantId = peasants.get(0);
					Action buildFarm = Action.createCompoundBuild(peasantId,state.getTemplate(playerNo, farm).getID(),8,8);
					actions.put(peasantId, buildFarm);
				}
			}
			else if (findUnit(allUnitIds, state, barracks).size() <= 0) //build barracks
			{
				//collect gold and wood until there's enough for a barracks
				if (curG <= barracksGC)
				{
					collectResource(peasants, actions, state, townHalls.get(0), Type.GOLD_MINE);
				}
				else if (curW <= barracksWC)
				{
					collectResource(peasants, actions, state, townHalls.get(0), Type.TREE);
				}
				else
				{					
					int peasantId = peasants.get(0);
					Action buildBarracks = Action.createCompoundBuild(peasantId,state.getTemplate(playerNo, barracks).getID(),5,5);
					actions.put(peasantId, buildBarracks);
				}
			}
			else //build footmen
			{
				//if enough gold, build footmen
				if (curG >= footmanGC && findUnit(allUnitIds, state, footman).size() <= neededFootman - 1)
				{
					int barracksId = findUnit(allUnitIds, state, barracks).get(0);
					TemplateView footmanTemplate = state.getTemplate(playerNo, footman);
					Action buildFootman = Action.createCompoundProduction(barracksId, footmanTemplate.getID());
					actions.put(barracksId, buildFootman);
				}
				
				collectResource(peasants, actions, state, townHalls.get(0), Type.GOLD_MINE);
			}
		}
		
		return actions;
	}

	@Override
	public void savePlayerData(OutputStream arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void terminalStep(StateView arg0, HistoryView arg1) {
        System.out.println("END GAME");

	}
	
	public List<Integer> findUnit(List<Integer> ids, StateView state, String name)
	{
		
		List<Integer> unitIds = new ArrayList<Integer>();
		
		for (int x = 0; x < ids.size(); x++)
		{
			Integer unitId = ids.get(x);
			UnitView unit = state.getUnit(unitId);
			
			if(unit.getTemplateView().getName().equals(name))
			{
				unitIds.add(unitId);
			}
		}
		
		return unitIds;
	}
	
	public void collectResource(List<Integer> peasants, Map<Integer, Action> actionList, StateView state, Integer townHall, Type resource)	
	{
		Action action = null;
		
		for (Integer peasantId: peasants)
		{
			List<Integer> resourceIds = state.getResourceNodeIds(resource);
			
			if(state.getUnit(peasantId).getCargoType() == Type.getResourceType(resource))
			{
				action = new TargetedAction(peasantId, ActionType.COMPOUNDDEPOSIT, townHall);
			}
			else if(resourceIds.size() > 0) 
			{
				action = new TargetedAction(peasantId, ActionType.COMPOUNDGATHER, resourceIds.get(0));
			}
			actionList.put(peasantId, action);
		}
	}
}
