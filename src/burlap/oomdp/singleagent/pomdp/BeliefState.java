package burlap.oomdp.singleagent.pomdp;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import burlap.behavior.singleagent.auxiliary.StateEnumerator;
import burlap.debugtools.RandomFactory;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TransitionProbability;
import burlap.oomdp.singleagent.GroundedAction;
/**
 * 
 * @author jmacglashan & ngopalan
 * 
 * A belief state object consists of a mapping between states and their respective
 * belief probabilities. A domain and state enumerator needs to be defined for 
 * belief state object.
 *
 */
public class BeliefState extends BeliefStatistic{


	protected Map<Integer, Double> beliefValues = new HashMap<Integer, Double>();
	protected StateEnumerator stateEnumerator; 


	public BeliefState(PODomain domain, BeliefStatistic bsInit){
		super(domain);
		this.domain = domain;
		if(domain.getStateEnumerator()!=null){
			this.stateEnumerator = domain.getStateEnumerator();
		}
		else{
			System.out.println("BeliefState needs a state enumerator");
		}
		int numStates = this.stateEnumerator.numStatesEnumerated();
		
		for(int i=0;i<numStates;i++){
			this.beliefValues.put(i, bsInit.belief(this.stateEnumerator.getStateForEnumertionId(i)));
		}
	}



	public BeliefState(PODomain domain){
		super(domain);
		this.stateEnumerator = domain.getStateEnumerator();
	}


	@Override
	public List<State> getStatesWithNonZeroProbability(){
		List<State> states = new LinkedList<State>();
		for(int i : this.beliefValues.keySet()){
			states.add(this.stateEnumerator.getStateForEnumertionId(i));
		}
		return states;
	}

	public List<StateBelief> getStatesAndBeliefsWithNonZeroProbability(){
		List<StateBelief> result = new LinkedList<BeliefState.StateBelief>();
		for(Map.Entry<Integer, Double> e : this.beliefValues.entrySet()){
			StateBelief sb = new StateBelief(this.stateForId(e.getKey()), e.getValue());
			result.add(sb);
		}
		return result;
	}

	@Override
	public State sampleStateFromBelief(){
		double sumProb = 0.;
		double r = RandomFactory.getMapped(0).nextDouble();
		for(Map.Entry<Integer, Double> e : this.beliefValues.entrySet()){
			sumProb += e.getValue();
			if(r < sumProb){
				return this.stateEnumerator.getStateForEnumertionId(e.getKey());
			}
		}

		throw new RuntimeException("Error; could not sample from belief state because the beliefs did not sum to 1; they summed to: " + sumProb);
	}

	@Override
	public double belief(State s){
		int sid = this.stateEnumerator.getEnumeratedID(s);
		return this.belief(sid);
	}

	public double belief(int stateId){

		Double b = this.beliefValues.get(stateId);
		if(b == null){
			return 0.;
		}
		return b;
	}

	public double [] getBeliefVector(){
		double [] b = new double[this.numStates()];
		for(int i = 0; i < b.length; i++){
			b[i] = this.belief(i);
		}
		return b;
	}


	public void setBelief(State s, double b){
		int sid = this.stateEnumerator.getEnumeratedID(s);
		this.setBelief(sid, b);
	}

	public void setBelief(int stateId, double b){
		if(stateId < 0 || stateId > this.numStates()){
			throw new RuntimeException("Error; cannot set belief value for state id " + stateId + "; belief vector is of dimension " + this.numStates());
		}

		if(b != 0){
			this.beliefValues.put(stateId, b);
		}
		else{
			this.beliefValues.remove(stateId);
		}
//		beliefNorm();
	}


	public void setBeliefCollection(double [] b){
		if(b.length != this.numStates()){
			throw new RuntimeException("Error; cannot set belief state with provided vector because dimensionality does not match." +
					"Provided vector of dimension " + b.length + " need dimension " + this.numStates());
		}

		for(int i = 0; i < b.length; i++){
			this.setBelief(i, b[i]);
		}
//		beliefNorm();
	}

	@Override
	public void clearBeliefCollection(){
		this.beliefValues.clear();
	}

	public void initializeBeliefsUniformly(){
		double b = 1. / (double)this.numStates();
		this.initializeAllBeliefValuesTo(b);
	}

	public void initializeAllBeliefValuesTo(double initialValue){

		if(initialValue == 0){
			this.clearBeliefCollection();
		}
		else{
			for(int i = 0; i < this.numStates(); i++){
				this.setBelief(i, initialValue);
			}
		}
	}


	/**
	 * updates the belief vectors based on observation and action taken
	 */
	public BeliefState getUpdatedBeliefState(State observation, GroundedAction ga){


		ObservationFunction of = this.domain.getObservationFunction();
		double [] newBeliefStateVector = new double[this.numStates()];
		double sum = 0.;
		for(int i = 0; i < newBeliefStateVector.length; i++){
			State ns = this.stateForId(i);
			double op = of.getObservationProbability(observation, ns, ga);
			double transitionSum = 0.;
			for(Map.Entry<Integer, Double> srcStateEntry : this.beliefValues.entrySet()){
				double srcB = srcStateEntry.getValue();
				State srcState = this.stateEnumerator.getStateForEnumertionId(srcStateEntry.getKey());
				double tp = this.getTransitionProb(srcState, ga, ns);
				transitionSum += srcB * tp;
			}
			double numerator = op * transitionSum;
			sum += numerator;
			newBeliefStateVector[i] = numerator;

		}

		BeliefState newBeliefState = new BeliefState(this.domain);
		for(int i = 0; i < newBeliefStateVector.length; i++){
			double nb = newBeliefStateVector[i] / sum;
			newBeliefState.setBelief(i, nb);
		}


		return newBeliefState;
	}

	/**
	 *  returns probability of an observation given previous belief state
	 */
	public double probObservation(State observation, GroundedAction ga){
		ObservationFunction of = this.domain.getObservationFunction();
		double sum = 0.;
		for(int i = 0; i < this.numStates(); i++){
			State ns = this.stateForId(i);
			double op = of.getObservationProbability(observation, ns, ga);
			double transitionSum = 0.;
			for(Map.Entry<Integer, Double> srcStateEntry : this.beliefValues.entrySet()){
				double srcB = srcStateEntry.getValue();
				State srcState = this.stateEnumerator.getStateForEnumertionId(srcStateEntry.getKey());
				double tp = this.getTransitionProb(srcState, ga, ns);
				transitionSum += srcB * tp;
			}
			double numerator = op * transitionSum;
			sum += numerator;

		}

		return sum;
	}

	protected double getTransitionProb(State s, GroundedAction ga, State sp){
		List<TransitionProbability> tps = ga.action.getTransitions(s, ga.params);
		for(TransitionProbability tp : tps){
			if(tp.s.equals(sp)){
				return tp.p;
			}
		}
		return 0.;

	}
	
	
	public int numStates(){
		return this.stateEnumerator.numStatesEnumerated();
	}
	
	public State stateForId(int id){
		return this.stateEnumerator.getStateForEnumertionId(id);
	}
	
	
	
	private void beliefNorm() {
		double sum = 0.0;
		for(double d : this.beliefValues.values()){
			sum+=d;
		}
		
		for(int keyInput : this.beliefValues.keySet()){
			double tempDouble = this.beliefValues.get(keyInput);
			this.beliefValues.put(keyInput, tempDouble/sum);
		}
	}
	

	public class StateBelief{
		public State s;
		public double belief;

		public StateBelief(State s, double belief){
			this.s = s;
			this.belief = belief;
		}
	}


}
