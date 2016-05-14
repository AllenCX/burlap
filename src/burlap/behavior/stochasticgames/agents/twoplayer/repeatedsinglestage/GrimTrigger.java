package burlap.behavior.stochasticgames.agents.twoplayer.repeatedsinglestage;

import java.util.Map;

import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.SGAgent;
import burlap.mdp.stochasticgames.AgentFactory;
import burlap.mdp.stochasticgames.agentactions.SGAgentAction;
import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.SGDomain;
import burlap.mdp.stochasticgames.agentactions.SGAgentActionType;
import burlap.mdp.stochasticgames.agentactions.SimpleGroundedSGAgentAction;


/**
 * A class for an agent that plays grim trigger. The agent starts by following a "cooperate" action. If at any point their opponent plays
 * a "defect" action, then this agent will play their "defect" action for the rest of the repeated game (until the {@link #gameStarting()} method is called again).
 * @author James MacGlashan
 *
 */
public class GrimTrigger extends SGAgent {

	/**
	 * This agent's cooperate action
	 */
	protected SGAgentActionType myCoop;
	
	/**
	 * This agent's defect action
	 */
	protected SGAgentActionType myDefect;
	
	/**
	 * The opponent's defect action
	 */
	protected SGAgentActionType opponentDefect;
	
	
	/**
	 * Whether this agent will play its defect action or not.
	 */
	protected boolean grimTrigger = false;
	
	
	/**
	 * Initializes with the specified cooperate and defect actions for both players.
	 * @param domain the domain in which this agent will play.
	 * @param coop the cooperate action for both players
	 * @param defect the defect action for both players
	 */
	public GrimTrigger(SGDomain domain, SGAgentActionType coop, SGAgentActionType defect){
		this.init(domain);
		this.myCoop = coop;
		this.myDefect = defect;
		this.opponentDefect = defect;
	}
	
	
	/**
	 * Initializes with differently specified cooperate and defect actions for both players.
	 * @param domain the domain in which this agent will play
	 * @param myCoop this agent's cooperate action
	 * @param myDefect this agent's defect action
	 * @param opponentDefect the opponent's defect action
	 */
	public GrimTrigger(SGDomain domain, SGAgentActionType myCoop, SGAgentActionType myDefect, SGAgentActionType opponentDefect){
		this.init(domain);
		this.myCoop = myCoop;
		this.myDefect = myDefect;
		this.opponentDefect = opponentDefect;
	}
	
	@Override
	public void gameStarting() {
		grimTrigger = false;
	}

	@Override
	public SGAgentAction getAction(State s) {
		if(this.grimTrigger){
			return new SimpleGroundedSGAgentAction(this.worldAgentName, myDefect);
		}
		return new SimpleGroundedSGAgentAction(this.worldAgentName, myCoop);
	}

	@Override
	public void observeOutcome(State s, JointAction jointAction,
			Map<String, Double> jointReward, State sprime, boolean isTerminal) {
		
		for(SGAgentAction gsa : jointAction){
			if(!gsa.actingAgent.equals(this.worldAgentName) && this.opponentDefect.actionName.equals(gsa.action.actionName)){
			    grimTrigger = true;
			}
		}

	}

	@Override
	public void gameTerminated() {
	}

	
	
	/**
	 * An agent factory for GrimTrigger
	 * @author James MacGlashan
	 *
	 */
	public static class GrimTriggerAgentFactory implements AgentFactory{

		/**
		 * The agent's cooperate action
		 */
		protected SGAgentActionType myCoop;
		
		/**
		 * The agent's defect action
		 */
		protected SGAgentActionType myDefect;
		
		/**
		 * The opponent's defect action
		 */
		protected SGAgentActionType opponentDefect;
		
		/**
		 * The domain in which the agent will play
		 */
		protected SGDomain domain;
		
		
		/**
		 * Initializes with the specified cooperate and defect actions for both players.
		 * @param domain the domain in which this agent will play.
		 * @param coop the cooperate action for both players
		 * @param defect the defect action for both players
		 */
		public GrimTriggerAgentFactory(SGDomain domain, SGAgentActionType coop, SGAgentActionType defect){
			this.domain = domain;
			this.myCoop = coop;
			this.myDefect = defect;
			this.opponentDefect = defect;
		}
		
		
		/**
		 * Initializes with differently specified cooperate and defect actions for both players.
		 * @param domain the domain in which this agent will play
		 * @param myCoop the agent's cooperate action
		 * @param myDefect the agent's defect action
		 * @param opponentDefect the opponent's defect action
		 */
		public GrimTriggerAgentFactory(SGDomain domain, SGAgentActionType myCoop, SGAgentActionType myDefect, SGAgentActionType opponentDefect){
			this.domain = domain;
			this.myCoop = myCoop;
			this.myDefect = myDefect;
			this.opponentDefect = opponentDefect;
		}
		
		@Override
		public SGAgent generateAgent() {
			return new GrimTrigger(domain, myCoop, myDefect, opponentDefect);
		}
		
		
		
	}
	
}
