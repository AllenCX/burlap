package burlap.mdp.singleagent.pomdp.beliefstate.tabular;

import burlap.mdp.core.state.State;
import burlap.statehashing.HashableState;
import burlap.statehashing.HashableStateFactory;
import burlap.statehashing.WrappedHashableState;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Map;

/**
 * A {@link burlap.statehashing.HashableStateFactory} for {@link burlap.mdp.singleagent.pomdp.beliefstate.tabular.TabularBeliefState} instances.
 * @author James MacGlashan.
 */
public class HashableTabularBeliefStateFactory implements HashableStateFactory{

	@Override
	public HashableState hashState(State s) {

		if(!(s instanceof TabularBeliefState)){
			throw new RuntimeException("Cannot generate HashableState for input state, because it is a " + s.getClass().getName() + " instance and HashableTabularBeliefStateFactory only hashes TabularBeliefState instances.");
		}

		return new HashableTabularBeliefState(s);
	}


	public static class HashableTabularBeliefState extends WrappedHashableState{

		public HashableTabularBeliefState(State s) {
			super(s);
		}

		@Override
		public int hashCode() {

			HashCodeBuilder builder = new HashCodeBuilder(17, 31);
			for(Map.Entry<Integer, Double> e : ((TabularBeliefState)this.s).beliefValues.entrySet()){
				int entryHash = 31 * e.getKey().hashCode() + e.getValue().hashCode();
				builder.append(entryHash);
			}

			return builder.toHashCode();
		}

		@Override
		public boolean equals(Object obj) {

			if(!(obj instanceof HashableTabularBeliefState)){
				return false;
			}

			return this.s.equals(((HashableTabularBeliefState) obj).s);
		}

	}
}
