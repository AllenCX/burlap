package burlap.shell;

import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.states.State;
import burlap.oomdp.stochasticgames.SGAgentType;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.World;
import burlap.shell.command.ShellCommand;
import burlap.shell.command.world.*;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author James MacGlashan.
 */
public class SGWorldShell extends BurlapShell {

	protected World world;

	public SGWorldShell(Domain domain, InputStream is, PrintStream os, World world) {
		super(domain, is, os);
		this.world = world;

		this.welcomeMessage = "Welcome to the BURLAP stochastic games world shell. Type the command 'help' to bring " +
				"up additional information about using this shell.";

		this.helpText = "Use the command help to bring up this message again. " +
				"Here is a list of standard reserved commands:\n" +
				"cmds - list all known commands.\n" +
				"aliases - list all known command aliases.\n" +
				"alias - set an alias for a command.\n" +
				"quit - terminate this shell.\n\n" +
				"Other useful, but non-reserved, commands are:\n" +
				"obs - print the current observation of the environment\n" +
				"ex - execute an action\n\n" +
				"Usually, you can get help on an individual command by passing it the -h option.";

	}

	public World getWorld() {
		return world;
	}

	public void setWorld(World world) {
		this.world = world;
	}

	@Override
	protected Collection<ShellCommand> generateStandard() {
		ManualAgentsCommands macs = new ManualAgentsCommands();
		return Arrays.<ShellCommand>asList(new WorldObservationCommand(), macs.getRegCommand(), macs.getLsActions(),
				macs.getLsAgents(), macs.getSetAction(), new GameCommand(), new JointActionCommand(),
				new RewardsCommand(), new LastJointActionCommand(), new IsTerminalSGCommand(),
				new GenerateStateCommand(), new AddRelationSGCommand(), new AddStateObjectSGCommand(domain),
				new RemoveRelationSGCommand(), new RemoveStateObjectSGCommand(), new SetAttributeSGCommand());
	}


	public static void main(String[] args) {

		GridGame gg = new GridGame();

		SGDomain domain = (SGDomain)gg.generateDomain();
		SGAgentType type = GridGame.getStandardGridGameAgentType(domain);
		State s = GridGame.getSimpleGameInitialState(domain);

		World w = new World(domain, new GridGame.GGJointRewardFunction(domain), new GridGame.GGTerminalFunction(domain), s);

		SGWorldShell shell = new SGWorldShell(domain, System.in, new PrintStream(System.out), w);

		shell.start();
		//shell.executeCommand("reg -r 2 agent");

	}
}
