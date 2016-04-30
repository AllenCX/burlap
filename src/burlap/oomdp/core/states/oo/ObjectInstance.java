package burlap.oomdp.core.states.oo;

import burlap.oomdp.core.states.State;

/**
 * @author James MacGlashan.
 */
public interface ObjectInstance extends State {

	/**
	 * Returns the name of this OO-MDP object class
	 * @return the name of this OO-MDP object class
	 */
	String className();

	/**
	 * Returns the name of this object instance
	 * @return the name of this object instance
	 */
	String getName();

	/**
	 * Sets the name of this object instance
	 */
	void setName(String objectName);
}
