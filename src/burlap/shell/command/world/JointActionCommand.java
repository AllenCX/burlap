package burlap.shell.command.world;

import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.World;
import burlap.mdp.stochasticgames.agentactions.SGAgentAction;
import burlap.mdp.stochasticgames.agentactions.SGAgentActionType;
import burlap.shell.BurlapShell;
import burlap.shell.SGWorldShell;
import burlap.shell.command.ShellCommand;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

/**
 * A {@link burlap.shell.command.ShellCommand} for manually setting and executing a {@link burlap.mdp.stochasticgames.JointAction}
 * for the shell's {@link burlap.mdp.stochasticgames.World}. Use the -h option for help information.
 * @author James MacGlashan.
 */
public class JointActionCommand implements ShellCommand{

	protected JointAction ja = new JointAction();
	protected OptionParser parser = new OptionParser("xcpvh*");

	@Override
	public String commandName() {
		return "ja";
	}

	@Override
	public int call(BurlapShell shell, String argString, Scanner is, PrintStream os) {

		OptionSet oset = this.parser.parse(argString.split(" "));
		List<String> args = (List<String>)oset.nonOptionArguments();

		if(oset.has("h")){
			os.println("[-x [-v]][-c][-p] [agentName actionName [actionParams*]]\n" +
					"Used to manually execute a joint action in the world. Joint actions cannot be manually executed if" +
					"the world is currently running a game with attached agents.\n\n" +
					"-x: execute the joint action (executes after setting specified agent action)\n" +
					"-c: clear the joint action (clears after execution if -x is specified)\n" +
					"-p: print the joint action (before execution and clearing)\n" +
					"-v: prints the new state rewards and terminal state condition after executing.");
			return 0;
		}

		if(!args.isEmpty()){
			String agentName = args.get(0);

			String aname = args.get(1);

			SGAgentActionType action = shell.getDomain().getSGAgentAction(aname);
			if(action == null){
				os.println("Cannot set action to " + aname + " because that action name is not known.");
				return 0;
			}

			SGAgentAction ga = action.associatedAction(agentName, this.actionArgs(args));

			ja.addAction(ga);
		}

		if(oset.has("p")){
			os.println(ja.toString());
		}

		World w = ((SGWorldShell)shell).getWorld();

		boolean changed = false;
		if(oset.has("x")){
			if(w.gameIsRunning()){
				os.println("Cannot manually execute joint action, because a game with attached agents is running in the world.");
			}
			else{
				changed = true;
				w.executeJointAction(ja);
				this.ja = ja.copy();

				if(oset.has("v")){

					os.println(w.getCurrentWorldState().toString());
					os.println(w.getLastRewards());
					if(w.worldStateIsTerminal()){
						os.println("IS a terminal state.");
					}
					else{
						os.println("is NOT a terminal state.");
					}

				}

			}
		}

		if(oset.has("c")){
			this.ja = new JointAction();
		}

		if(changed){
			return 1;
		}

		return 0;
	}

	/**
	 * Adds a {@link SGAgentAction} to the {@link burlap.mdp.stochasticgames.JointAction}
	 * being built and to be executed.
	 * @param action the {@link SGAgentAction} to add to the {@link burlap.mdp.stochasticgames.JointAction}.
	 */
	public void addGroundedActionToJoint(SGAgentAction action){
		this.ja.addAction(action);
	}


	protected String actionArgs(List<String> commandArgs){
		StringBuilder buf = new StringBuilder();
		for(int i = 2; i < commandArgs.size(); i++){
			if(i > 2){
				buf.append(" ");
			}
			buf.append(commandArgs.get(i));
		}
		return buf.toString();
	}
}
