package burlap.behavior.singleagent.vfa.common;

import burlap.behavior.singleagent.vfa.StateToFeatureVectorGenerator;
import burlap.oomdp.core.oo.state.OOState;
import burlap.oomdp.core.state.State;
import burlap.oomdp.core.oo.OODomain;
import burlap.oomdp.core.oo.propositional.GroundedProp;
import burlap.oomdp.core.oo.propositional.PropositionalFunction;

import java.util.LinkedList;
import java.util.List;

public class PFFeatureVectorGenerator implements StateToFeatureVectorGenerator {

	protected PropositionalFunction [] pfsToUse;
	
	
	/**
	 * Initializes using all propositional functions that belong to the domain
	 * @param domain the domain containing all the propositional functions to use
	 */
	public PFFeatureVectorGenerator(OODomain domain){
		
		this.pfsToUse = new PropositionalFunction[domain.getPropFunctions().size()];
		int i = 0;
		for(PropositionalFunction pf : domain.getPropFunctions()){
			this.pfsToUse[i] = pf;
			i++;
		}
		
	}
	
	/**
	 * Initializes using the list of given propositional functions.
	 * @param pfs the propositional functions to use.
	 */
	public PFFeatureVectorGenerator(List<PropositionalFunction> pfs){
		this.pfsToUse = new PropositionalFunction[pfs.size()];
		this.pfsToUse = pfs.toArray(this.pfsToUse);
	}
	
	
	/**
	 * Initializes using the array of given propositional functions.
	 * @param pfs the propositional functions to use.
	 */
	public PFFeatureVectorGenerator(PropositionalFunction [] pfs){
		this.pfsToUse = pfs.clone();
	}
	
	
	@Override
	public double[] generateFeatureVectorFrom(State s) {
		
		List<Double> featureValueList = new LinkedList<Double>();
		for(PropositionalFunction pf : this.pfsToUse){
			//List<GroundedProp> gps = s.getAllGroundedPropsFor(pf);
			List<GroundedProp> gps = pf.getAllGroundedPropsForState(s);
			for(GroundedProp gp : gps){
				if(gp.isTrue((OOState)s)){
					featureValueList.add(1.);
				}
				else{
					featureValueList.add(0.);
				}
			}
		}
		
		double [] fv = new double[featureValueList.size()];
		int i = 0;
		for(double f : featureValueList){
			fv[i] = f;
			i++;
		}
		
		return fv;
	}

}
