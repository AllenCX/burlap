package burlap.behavior.stochasticgames.agents;

import java.util.List;
import java.util.Map;

import burlap.debugtools.RandomFactory;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.SGAgent;
import burlap.mdp.stochasticgames.agentactions.GroundedSGAgentAction;
import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.agentactions.SGAgentAction;


/**
 * Stochastic games agent that chooses actions uniformly randomly.
 * @author James MacGlashan
 *
 */
public class RandomSGAgent extends SGAgent {

	@Override
	public void gameStarting() {
		//do nothing

	}

	@Override
	public GroundedSGAgentAction getAction(State s) {
		
		List<GroundedSGAgentAction> gsas = SGAgentAction.getAllApplicableGroundedActionsFromActionList(s, this.worldAgentName, this.agentType.actions);
		
		int r = RandomFactory.getMapped(0).nextInt(gsas.size());
		GroundedSGAgentAction gsa = gsas.get(r);
		
		return gsa;
	}

	@Override
	public void observeOutcome(State s, JointAction jointAction,
			Map<String, Double> jointReward, State sprime, boolean isTerminal) {
		//do nothing

	}

	@Override
	public void gameTerminated() {
		//do nothing
	}

}
