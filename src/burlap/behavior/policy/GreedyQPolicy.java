package burlap.behavior.policy;

import burlap.behavior.singleagent.MDPSolverInterface;
import burlap.behavior.valuefunction.QFunction;
import burlap.behavior.valuefunction.QValue;
import burlap.debugtools.RandomFactory;
import burlap.mdp.core.Action;
import burlap.mdp.core.state.State;

import javax.management.RuntimeErrorException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;



/**
 * A greedy policy that breaks ties by randomly choosing an action amongst the tied actions. This class requires a QComputablePlanner
 * @author James MacGlashan
 *
 */
public class GreedyQPolicy extends Policy implements SolverDerivedPolicy {

	protected QFunction qplanner;
	protected Random 					rand;
	
	
	public GreedyQPolicy(){
		qplanner = null;
		rand = RandomFactory.getMapped(0);
	}
	
	
	/**
	 * Initializes with a QComputablePlanner
	 * @param planner the QComputablePlanner to use
	 */
	public GreedyQPolicy(QFunction planner){
		qplanner = planner;
		rand = RandomFactory.getMapped(0);
	}
	
	@Override
	public void setSolver(MDPSolverInterface solver){
		
		if(!(solver instanceof QFunction)){
			throw new RuntimeErrorException(new Error("Planner is not a QComputablePlanner"));
		}
		
		this.qplanner = (QFunction) solver;
	}
	
	@Override
	public Action getAction(State s) {
		List<QValue> qValues = this.qplanner.getQs(s);
		List <QValue> maxActions = new ArrayList<QValue>();
		maxActions.add(qValues.get(0));
		double maxQ = qValues.get(0).q;
		for(int i = 1; i < qValues.size(); i++){
			QValue q = qValues.get(i);
			if(q.q == maxQ){
				maxActions.add(q);
			}
			else if(q.q > maxQ){
				maxActions.clear();
				maxActions.add(q);
				maxQ = q.q;
			}
		}
		int selected = rand.nextInt(maxActions.size());
		Action srcA = maxActions.get(selected).a;
		return srcA;
	}

	@Override
	public List<ActionProb> getActionDistributionForState(State s) {
		List<QValue> qValues = this.qplanner.getQs(s);
		int numMax = 1;
		double maxQ = qValues.get(0).q;
		for(int i = 1; i < qValues.size(); i++){
			QValue q = qValues.get(i);
			if(q.q == maxQ){
				numMax++;
			}
			else if(q.q > maxQ){
				numMax = 1;
				maxQ = q.q;
			}
		}
		
		List <ActionProb> res = new ArrayList<Policy.ActionProb>();
		double uniformMax = 1./(double)numMax;
		for(int i = 0; i < qValues.size(); i++){
			QValue q = qValues.get(i);
			double p = 0.;
			if(q.q == maxQ){
				p = uniformMax;
			}
			ActionProb ap = new ActionProb(q.a, p);
			res.add(ap);
		}
		
		
		return res;
	}

	@Override
	public boolean isStochastic() {
		return true; //although the policy is greedy, it randomly selects between tied actions
	}


	@Override
	public boolean isDefinedFor(State s) {
		return true; //can always find q-values with default value
	}
	
	
	
	
	
	

}
