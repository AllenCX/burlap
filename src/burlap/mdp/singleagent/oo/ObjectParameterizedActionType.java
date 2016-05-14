package burlap.mdp.singleagent.oo;

import burlap.mdp.core.Action;
import burlap.mdp.core.Domain;
import burlap.mdp.core.oo.ObjectParameterizedAction;
import burlap.mdp.core.oo.state.OOState;
import burlap.mdp.core.oo.state.OOStateUtilities;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.ActionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author James MacGlashan.
 */
public class ObjectParameterizedActionType implements ActionType {

	public String name;

	/**
	 * The object classes each parameter of this action can accept; empty list for a parameter-less action (which is the default)
	 */
	protected String []					parameterClasses;

	/**
	 * Specifies the parameter order group each parameter. Parameters in the same order group are order invariant; that is, if you swapped the parameter assignments for for parameters in the same group, the action would have
	 * the same effect. However, if you swapped the parameter assignments of two parameters in different order groups, the action would have a different effect.
	 */
	protected String []					parameterOrderGroup;


	/**
	 * Initializes the action with the name of the action
	 * @param name the name of the action
	 * @param parameterClasses a String array of the names of the object classes to which bound parameters must belong
	 */
	public ObjectParameterizedActionType(String name, String [] parameterClasses){
		this.parameterClasses = parameterClasses;
		this.parameterOrderGroup = new String[parameterClasses.length];
		//without parameter order group specified, all parameters are assumed to be in a different group
		for(int i = 0; i < parameterOrderGroup.length; i++){
			parameterOrderGroup[i] = name + ".P" + i;
		}

	}

	/**
	 * Initializes the action with the name of the action, the domain to which it belongs, the parameters it takes, and the parameter order groups.
	 * The action will also be automatically be added to the domain.
	 * @param name the name of the action
	 * @param domain the domain to which the action belongs
	 * @param parameterClasses a String array of the names of the object classes to which bound parameters must belong
	 * @param parameterOrderGroups the order group assignments for each of the parameters.
	 */
	public ObjectParameterizedActionType(String name, Domain domain, String [] parameterClasses, String [] parameterOrderGroups){
		this.parameterClasses = parameterClasses;
		this.parameterOrderGroup = parameterOrderGroups;
	}

	/**
	 * Returns a String array of the names of of the object classes to which bound parameters must belong
	 * @return a String array of the names of of the object classes to which bound parameters must belong. The array is empty if this action does not require parameters.
	 */
	public final String[] getParameterClasses(){
		return parameterClasses;
	}


	/**
	 * Returns the a String array specifying the parameter order group of each parameter.
	 * @return the a String array specifying the parameter order group of each parameter. The array is empty if this action does not require parameters.
	 */
	public final String[] getParameterOrderGroups(){
		return parameterOrderGroup;
	}


	@Override
	public String typeName() {
		return name;
	}

	@Override
	public Action associatedAction(String strRep) {
		return this.generateAction(strRep.split(" "));
	}

	@Override
	public List<Action> allApplicableActions(State s) {

		List <Action> res = new ArrayList<Action>();


		if(!(s instanceof OOState)){
			throw new RuntimeException("Cannot get object-parameterized grounded actions in state, because " + s.getClass().getName() + " does not implement OOState");
		}

		//otherwise need to do parameter binding
		List <List <String>> bindings = OOStateUtilities.getPossibleBindingsGivenParamOrderGroups((OOState)s, this.getParameterClasses(), this.getParameterOrderGroups());

		for(List <String> params : bindings){
			String [] aprams = params.toArray(new String[params.size()]);
			Action ga = this.generateAction(aprams);
			res.add(ga);
		}

		return res;

	}

	protected Action generateAction(String [] params){
		return new SAObjectParameterizedAction(this.typeName(), params);
	}






	public static class SAObjectParameterizedAction implements ObjectParameterizedAction {

		public String name;
		public String [] params;


		public SAObjectParameterizedAction() {
		}

		public SAObjectParameterizedAction(String name, String [] params) {
			this.name = name;
			this.params = params;
		}

		@Override
		public String[] getObjectParameters() {
			return params;
		}

		@Override
		public void setObjectParameters(String[] params) {
			this.params = params;
		}

		@Override
		public String actionName() {
			return name;
		}

		@Override
		public String toString() {
			StringBuilder buf = new StringBuilder();
			buf.append(name);
			for(int i = 0; i < params.length; i++){
				buf.append(" ").append(params[i]);
			}

			return buf.toString();
		}

		
		@Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + Arrays.hashCode(params);
            return result;
        }

		@Override
        public boolean equals(Object other) {
            if(this == other){
                return true;
            }

            if(!(other instanceof SAObjectParameterizedAction)){
                return false;
            }

            SAObjectParameterizedAction go = (SAObjectParameterizedAction)other;

			return Arrays.equals(params, go.params);
        }

		@Override
		public Action copy() {
			return new SAObjectParameterizedAction(this.name, params.clone());
		}
	}


}
