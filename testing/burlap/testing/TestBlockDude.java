package burlap.testing;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.planning.SinglePFSCT;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.singleagent.planning.deterministic.SDPlannerPolicy;
import burlap.behavior.singleagent.planning.deterministic.informed.NullHeuristic;
import burlap.behavior.singleagent.planning.deterministic.informed.astar.AStar;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.common.SinglePFTF;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.testing.Domain.BlockDude;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TestBlockDude {

		@Test
		public void testDude() {

			BlockDude constructor = new BlockDude();
			Domain d = constructor.generateDomain();

			List<Integer> px = new ArrayList<Integer>();
			List <Integer> ph = new ArrayList<Integer>();

			ph.add(15);
			ph.add(3);
			ph.add(3);
			ph.add(3);
			ph.add(0);
			ph.add(0);
			ph.add(0);
			ph.add(1);
			ph.add(2);
			ph.add(0);
			ph.add(2);
			ph.add(3);
			ph.add(2);
			ph.add(2);
			ph.add(3);
			ph.add(3);
			ph.add(15);
			
			State s = BlockDude.getCleanState(d, px, ph, 6);
			s = BlockDude.setAgent(s, 9, 3, 1, 0);
			s = BlockDude.setExit(s, 1, 0);
			
			s = BlockDude.setBlock(s, 0, 5, 1);
			s = BlockDude.setBlock(s, 1, 6, 1);
			s = BlockDude.setBlock(s, 2, 14, 3);
			s = BlockDude.setBlock(s, 3, 16, 4);
			s = BlockDude.setBlock(s, 4, 17, 4);
			s = BlockDude.setBlock(s, 5, 17, 5);
			
			TerminalFunction tf = new SinglePFTF(d.getPropFunction(BlockDude.PFATEXIT));
			StateConditionTest sc = new SinglePFSCT(d.getPropFunction(BlockDude.PFATEXIT));

			RewardFunction rf = new UniformCostRF();

			AStar astar = new AStar(d, rf, sc, new DiscreteStateHashFactory(), new NullHeuristic());
			astar.toggleDebugPrinting(false);
			astar.planFromState(s);

			Policy p = new SDPlannerPolicy(astar);
			EpisodeAnalysis ea = p.evaluateBehavior(s, rf, tf, 100);

			State lastState = ea.stateSequence.get(ea.stateSequence.size() - 1);
			Assert.assertEquals(true, tf.isTerminal(lastState));
			Assert.assertEquals(true, sc.satisfies(lastState));
			Assert.assertEquals(-94.0, ea.getDiscountedReturn(1.0), 0.001);

		}
}
