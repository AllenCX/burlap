package burlap.behavior.singleagent.planning.stochastic.valueiteration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import burlap.behavior.singleagent.planning.ActionTransitions;
import burlap.behavior.singleagent.planning.HashedTransitionProbability;
import burlap.behavior.singleagent.planning.ValueFunctionPlanner;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.debugtools.DPrint;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;



/**
 * An implementation of asynchronous value iteration. Values of states are updated using the Bellman operator in an arbitrary order and a complete pass
 * over the state space is performed on each iteration. VI can be set to terminate under two possible conditions: when the maximum change in the value
 * function is smaller than some threshold or when a threshold of iterations is passed. This implementation first determines the state space by finding
 * all reachable states from a source state. The time complexity of this operation is equivalent to that of one VI iteration and has the added benefit
 * that VI does not pass over non-reachable states.
 * 
 * This implementation is compatible with options.
 * 
 * 
 * @author James MacGlashan
 *
 */
public class ValueIteration extends ValueFunctionPlanner{

	/**
	 * When the maximum change in the value function is smaller than this value, VI will terminate.
	 */
	protected double												maxDelta;
	
	/**
	 * When the number of VI iterations exceeds this value, VI will terminate.
	 */
	protected int													maxIterations;
	
	
	/**
	 * Indicates whether the reachable states has been computed yet.
	 */
	protected boolean												foundReachableStates = false;
	
	
	
	/**
	 * Initializers the planner.
	 * @param domain the domain in which to plan
	 * @param rf the reward function
	 * @param tf the terminal state function
	 * @param gamma the discount factor
	 * @param hashingFactory the state hashing factor to use
	 * @param maxDelta when the maximum change in the value function is smaller than this value, VI will terminate.
	 * @param maxIterations when the number of VI iterations exceeds this value, VI will terminate.
	 */
	public ValueIteration(Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory, double maxDelta, int maxIterations){
		
		this.VFPInit(domain, rf, tf, gamma, hashingFactory);
		
		this.maxDelta = maxDelta;
		this.maxIterations = maxIterations;
		
	}
	
	
	
	
	@Override
	public void planFromState(State initialState){
		this.initializeOptionsForExpectationComputations();
		if(this.performReachabilityFrom(initialState)){
			this.runVI();
		}
			
	}
	
	/**
	 * Runs VI until the specified termination conditions are met. In general, this method should only be called indirectly through the {@link planFromState(State)} method.
	 * The {@link performReachabilityFrom(State)} must have been performed at least once
	 * in the past or a runtime exception will be thrown. The {@link planFromState(State)} method will automatically call the {@link performReachabilityFrom(State)} 
	 * method first and then this if it hasn't been run.
	 */
	public void runVI(){
		
		if(!this.foundReachableStates){
			throw new RuntimeException("Cannot run VI until the reachable states have been found. Use planFromState method at least once or instead.");
		}
		
		Set <StateHashTuple> states = mapToStateIndex.keySet();
		
		int i = 0;
		for(i = 0; i < this.maxIterations; i++){
			
			double delta = 0.;
			for(StateHashTuple sh : states){
				
				if(tf.isTerminal(sh.s)){
					//no need to compute this state; always zero because it is terminal and agent cannot behave here
					valueFunction.put(sh, 0.);
					continue;
					
				}
				
				double v = this.value(sh);
				
				double maxQ = this.performBellmanUpdateOn(sh);
				delta = Math.max(Math.abs(maxQ - v), delta);
				
			}
			
			if(delta < this.maxDelta){
				break; //approximated well enough; stop iterating
			}
			
		}
		
		DPrint.cl(this.debugCode, "Passes: " + i);
		
	}
	
	
	/**
	 * This method will find all reachable states that will be used by the {@link runVI()} method and will cache all the transition dynamics.
	 * This method will not do anything if all reachable states from the input state have been discovered from previous calls to this method.
	 * @param si the source state from which all reachable states will be found
	 * @return true if a reachability analysis had never been performed from this state; false otherwise.
	 */
	public boolean performReachabilityFrom(State si){
		
		
		
		StateHashTuple sih = this.stateHash(si);
		//first check if this is an new state, otherwise we do not need to do any new reachability analysis
		if(transitionDynamics.containsKey(sih)){
			return false; //no need for additional reachability testing
		}
		
		DPrint.cl(this.debugCode, "Starting reachability analysis");
		
		//add to the open list
		LinkedList <StateHashTuple> openList = new LinkedList<StateHashTuple>();
		Set <StateHashTuple> openedSet = new HashSet<StateHashTuple>();
		openList.offer(sih);
		openedSet.add(sih);
		
		
		while(openList.size() > 0){
			StateHashTuple sh = openList.poll();
			
			//skip this if it's already been expanded
			if(transitionDynamics.containsKey(sh)){
				continue;
			}
			
			mapToStateIndex.put(sh, sh);
			
			//do not need to expand from terminal states
			if(this.tf.isTerminal(sh.s)){
				continue;
			}
			
			//otherwise do expansion
			//first get all grounded actions for this state
			List <GroundedAction> gas = new ArrayList<GroundedAction>();
			for(Action a : actions){
				gas.addAll(sh.s.getAllGroundedActionsFor(a));
			}
			
			//then get the transition dynamics for each action and queue up new states
			List <ActionTransitions> transitions = new ArrayList<ActionTransitions>();
			for(GroundedAction ga : gas){
				ActionTransitions at = new ActionTransitions(sh.s, ga, hashingFactory);
				transitions.add(at);
				for(HashedTransitionProbability tp : at.transitions){
					StateHashTuple tsh = tp.sh;
					if(!openedSet.contains(tsh) && !transitionDynamics.containsKey(tsh)){
						openedSet.add(tsh);
						openList.offer(tsh);
					}
				}
			}
			
			//now make entry for this in the transition dynamics
			transitionDynamics.put(sh, transitions);
			
			
		}
		
		DPrint.cl(this.debugCode, "Finished reachability analysis; # states: " + mapToStateIndex.size());
		
		this.foundReachableStates = true;
		
		return true;
		
	}
	
	
	

	
	
}
