package burlap.domain.singleagent.minecraft;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.PriorityQueue;
import java.util.Queue;

import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.PropositionalFunction;

import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.common.SingleGoalMultiplePFRF;
import burlap.oomdp.singleagent.common.SingleGoalPFRF;
import burlap.oomdp.singleagent.common.SinglePFTF;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.oomdp.visualizer.Visualizer;
import burlap.oomdp.core.State;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.singleagent.planning.ValueFunctionPlanner;
import burlap.behavior.singleagent.planning.stochastic.rtdp.RTDP;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.*;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.TerminalFunction;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.deterministic.TFGoalCondition;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.domain.singleagent.minecraft.MinecraftDomain.IsOnGrain;
import burlap.domain.singleagent.minecraft.MinecraftDomain.IsInLava;
import burlap.domain.singleagent.minecraft.MinecraftDomain.*;



public class MinecraftBehavior {

	MinecraftDomain				mcdg;
	Domain						domain;
	StateParser					sp;
	RewardFunction				rf;
	TerminalFunction			tf;
	StateConditionTest			goalCondition;
	State						initialState;
	DiscreteStateHashFactory	hashingFactory;
	
	PropositionalFunction		pfAgentAtGoal;
	PropositionalFunction		pfIsPlane;
	PropositionalFunction		pfIsAdjTrench;
	PropositionalFunction		pfIsAdjDoor;
	PropositionalFunction		pfIsThrQWay;
	PropositionalFunction		pfIsHalfWay;
	PropositionalFunction		pfIsOneQWay;
	PropositionalFunction		pfIsAgentYAt;
	PropositionalFunction		pfIsAtGoal;
	PropositionalFunction		pfIsAtLocation;
	PropositionalFunction		pfIsWalkable;
	PropositionalFunction 		pfIsAdjDstableWall;
	PropositionalFunction 		pfAgentHasBread;
	PropositionalFunction		pfIsAdjOven;
	PropositionalFunction		pfIsOnGrain;
	PropositionalFunction  		pfIsInLava;
	
	HashMap<PropositionalFunction, Double> rewardTable;
	
	// Timing stuff
//	private static long			timeStart;
//	private static long			timeEnd;
	private static double			timeDelta;
	
	public MinecraftBehavior(String mapfile) {
		MCStateGenerator mcsg = new MCStateGenerator(mapfile);
		mcdg = new MinecraftDomain();
		
//		Gets the maximum dimensions for the map. The first entry is the number of columns
//		and the second entry is the number of rows.
		int[] maxDims = mcsg.getDimensions();
		
		domain = mcdg.generateDomain(maxDims[0], maxDims[1]);
		
		sp = new MinecraftStateParser(domain);

		// === Build Initial State=== //



		initialState = mcsg.getCleanState(domain);

		// Set up the state hashing system
		hashingFactory = new DiscreteStateHashFactory();
		hashingFactory.setAttributesForClass(MinecraftDomain.CLASSAGENT, 
					domain.getObjectClass(MinecraftDomain.CLASSAGENT).attributeList); 
		
		// Create Propfuncs for use
		
		String ax = Integer.toString(this.initialState.getObject("agent0").getDiscValForAttribute(this.mcdg.ATTX));
		String ay = Integer.toString(this.initialState.getObject("agent0").getDiscValForAttribute(this.mcdg.ATTY));
		String az = Integer.toString(this.initialState.getObject("agent0").getDiscValForAttribute(this.mcdg.ATTZ));
		
		pfIsPlane = new IsAdjPlane(this.mcdg.ISPLANE, this.mcdg.DOMAIN,
				new String[]{this.mcdg.CLASSAGENT});
		
		pfIsAdjTrench = new IsAdjTrench(this.mcdg.ISADJTRENCH, this.mcdg.DOMAIN,
				new String[]{this.mcdg.CLASSAGENT});
		
		pfIsAdjDoor = new IsAdjDoor(this.mcdg.ISADJDOOR, this.mcdg.DOMAIN,
				new String[]{this.mcdg.CLASSAGENT});
		
		pfIsAdjOven = new IsAdjOven(this.mcdg.ISADJOVEN, this.mcdg.DOMAIN,
				new String[]{this.mcdg.CLASSAGENT});
		
		pfIsOnGrain = new IsOnGrain(this.mcdg.ISONGRAIN, this.mcdg.DOMAIN,
				new String[]{this.mcdg.CLASSAGENT});
		
		pfIsInLava = new IsInLava(this.mcdg.ISINLAVA, this.mcdg.DOMAIN,
				new String[]{this.mcdg.CLASSAGENT});
		
		pfIsAdjDstableWall = new IsAdjDstableWall(this.mcdg.ISADJDWALL, this.mcdg.DOMAIN,
				new String[]{this.mcdg.CLASSAGENT});
		
		pfIsThrQWay = new IsNthOfTheWay("IsThrQWay", this.mcdg.DOMAIN,
				new String[]{this.mcdg.CLASSAGENT, this.mcdg.CLASSGOAL}, new String[] {ax, ay, az}, 0.75);
		
		pfIsHalfWay = new IsNthOfTheWay("IsHalfWay", this.mcdg.DOMAIN,
				new String[]{this.mcdg.CLASSAGENT, this.mcdg.CLASSGOAL}, new String[] {ax, ay, az}, 0.5);
		
		pfIsOneQWay = new IsNthOfTheWay("IsOneQWay", this.mcdg.DOMAIN,
				new String[]{this.mcdg.CLASSAGENT, this.mcdg.CLASSGOAL}, new String[] {ax, ay, az}, 0.25);
		
		pfIsAtGoal = new AtGoalPF(this.mcdg.PFATGOAL, this.mcdg.DOMAIN,
				new String[]{this.mcdg.CLASSAGENT, this.mcdg.CLASSGOAL});
		
		pfIsAgentYAt = new IsAgentYAt("IsAgentOnBridge", this.mcdg.DOMAIN,
				new String[]{this.mcdg.CLASSAGENT}, (this.mcdg.MAXY + 1) / 2 - 1, 0);
		
		
		pfIsAtLocation = new IsAtLocationPF(this.mcdg.ISATLOC, this.mcdg.DOMAIN,
				new String[]{this.mcdg.CLASSAGENT, this.mcdg.CLASSGOAL}, new String[]{"13", "6", "1"});
		
		pfIsWalkable = new IsWalkablePF(this.mcdg.ISWALK, this.mcdg.DOMAIN,
				new String[]{"Integer", "Integer", "Integer"});

		pfAgentHasBread = new AgentHasBreadPF(this.mcdg.AGENTHASBREAD, this.mcdg.DOMAIN,
				new String[]{this.mcdg.CLASSAGENT});
		
		pfAgentAtGoal = new AtGoalPF(this.mcdg.PFATGOAL, this.mcdg.DOMAIN,
				new String[]{this.mcdg.CLASSAGENT, this.mcdg.CLASSGOAL});
		
		// Generate Goal Condition
//		rf = new SingleGoalPFRF(pfAgentHasBread, 10, -1); 
//		tf = new SinglePFTF(pfAgentHasBread); 
//		goalCondition = new TFGoalCondition(tf);
		
		rewardTable = new HashMap<PropositionalFunction, Double>();
		rewardTable.put(pfAgentAtGoal, (Double) 10.0);
		Double lavaRew = -100.0;
		rewardTable.put(pfIsInLava, lavaRew);
		
		rf = new SingleGoalMultiplePFRF(rewardTable, -1);
		
		tf = new SinglePFTF(pfAgentAtGoal); 
		goalCondition = new TFGoalCondition(tf);
		
		
	}

	// ---------- PLANNERS ---------- 
	
	// === VI Planner	===
	public int ValueIterationPlanner(){
		
		OOMDPPlanner planner = new ValueIteration(domain, rf, tf, 0.99, hashingFactory, 0.01, Integer.MAX_VALUE);
		
		int statePasses = planner.planFromState(initialState, this.mcdg);

		// Create a Q-greedy policy from the planner
		Policy p = new GreedyQPolicy((QComputablePlanner)planner);
		
		// Record the plan results to a file
		String actionSequence = p.evaluateBehavior(initialState, rf, tf).getActionSequenceString();
		System.out.println(actionSequence);
		
		return statePasses;
	}
	
	// === RTDP Planner	===
	public int RTDPPlanner(int numRollouts, int maxDepth){

		RTDP planner = new RTDP(domain, rf, tf, 0.99, hashingFactory, (10 / (1 - .99)), numRollouts, 0.01, maxDepth);
		
		int statePasses = planner.planFromStateAndCount(initialState);

		// Create a Q-greedy policy from the planner
		// Policy p = new GreedyQPolicy((QComputablePlanner)planner);
		
		// Record the plan results to a file
//		String actionSequence = p.evaluateBehavior(initialState, rf, tf).getActionSequenceString();
		
		return statePasses;
	}
	
	// === Subgoal Planner	===
	public int SubgoalPlanner(ArrayList<Subgoal> subgoals, int numRollouts, int maxDepth){
		
		// Initialize action plan
		String actionSequence = new String();
		
		// Build subgoal tree
		Node subgoalTree = generateGraph(subgoals);

		// Run BFS on subgoal tree (i.e. planning in subgoal space) 
		// returns a Node that is closer to the agent than the goal
		Node propfuncChain = BFS(subgoalTree);
		
		// Run VI between each subgoal in the chain
		State currState = initialState;
		boolean timeReachability = true;
		int numUpdates = 0;
		
		rewardTable.remove(pfIsAtGoal);
		while(propfuncChain != null) {
			System.out.println("Current goal: " + propfuncChain.getPropFunc().toString());
//			rf = new SingleGoalPFRF(propfuncChain.getPropFunc(), 10, -1);
			
			rewardTable.put(propfuncChain.getPropFunc(), (Double) 10.0);
			rf = new SingleGoalMultiplePFRF(rewardTable, -1);
//			rf = new SingleGoalPFRF(propfuncChain.getPropFunc(), 10, -1); 
			tf = new SinglePFTF(propfuncChain.getPropFunc()); 
//			
//			rf = new SingleGoalMultiplePFRF(rewardTable, -1);
			tf = new SinglePFTF(propfuncChain.getPropFunc()); 
			goalCondition = new TFGoalCondition(tf);
			
			OOMDPPlanner planner = new ValueIteration(domain, rf, tf, 0.99, hashingFactory, 0.01, Integer.MAX_VALUE);
//
			numUpdates += planner.planFromState(currState, this.mcdg);
//			RTDP planner = new RTDP(domain, rf, tf, 0.99, hashingFactory, (10 / (1 - .99)), numRollouts, 0.01, maxDepth);
			
//			numUpdates += planner.planFromStateAndCount(currState);

			Policy p = new GreedyQPolicy((QComputablePlanner)planner);

			EpisodeAnalysis ea = p.evaluateBehavior(currState, rf, tf);
			
			// Add low level plan to overall plan and update current state to the end of that subgoal plan
//			actionSequence += ea.getActionSequenceString() + "; ";
			currState = ea.getLastState();

			rewardTable.remove(propfuncChain.getPropFunc());
			propfuncChain = propfuncChain.getParent();
			
		}
		rewardTable.put(pfIsAtGoal, (Double) 10.0);
		return numUpdates;
		
	}
	
	private Node generateGraph(ArrayList<Subgoal> kb) {
		HashMap<PropositionalFunction,Node> nodes = new HashMap<PropositionalFunction,Node>();
		
		// Initialize Root of tree (based on final goal)
		Node root = new Node(kb.get(0).getPost(), null);
		nodes.put(kb.get(0).getPost(), root);
		
		// Create a node for each propositional function
		for (int i = 0; i < kb.size(); i++) {
			PropositionalFunction pre = kb.get(i).getPre();
			PropositionalFunction post = kb.get(i).getPost();
			
			Node postNode = new Node(post, null);
			Node preNode = new Node(pre, null);
			System.out.println("Post Node: " + post);
			System.out.println("Pre Node: " + pre);
			if (!nodes.containsKey(post)) {
				nodes.put(post, postNode);
			}
			
			if (!nodes.containsKey(preNode)) {
				nodes.put(pre, preNode);
			}
		}

		// Add edges between the nodes to form a tree of PropFuncs
		for (int i = 0; i < kb.size(); i++) {
			Subgoal edge = kb.get(i);
			
			PropositionalFunction edgeStart = edge.getPre();
			PropositionalFunction edgeEnd = edge.getPost();
			
			Node startNode = nodes.get(edgeStart);
			Node endNode = nodes.get(edgeEnd);
			
			if (startNode != null) {
				startNode.setParent(endNode);				
				endNode.addChild(startNode);
			}
						
		}
		
		return root;
	}
	
	private Node BFS(Node root) {
		ArrayDeque<Node> nodeQueue = new ArrayDeque<Node>();
		
		nodeQueue.add(root);
		Node curr = null;
		while (!nodeQueue.isEmpty()) {
			curr = nodeQueue.poll();
			if (curr.getPropFunc().isTrue(this.initialState)) {
				return curr;
			}
			if (curr.getChildren() != null) {
				nodeQueue.addAll(curr.getChildren());
			}
		}
		
		return curr;
	}
	
	private ArrayList<Subgoal> generateSubgoalKB() {
		// NOTE: ALWAYS add a subgoal with the FINAL goal first
		ArrayList<Subgoal> result = new ArrayList<Subgoal>();
		
		// Get agent starting coordinates
//		String ax = Integer.toString(this.initialState.getObject("agent0").getDiscValForAttribute(MinecraftDomain.ATTX));
//		String ay = Integer.toString(this.initialState.getObject("agent0").getDiscValForAttribute(MinecraftDomain.ATTY));
//		String az = Integer.toString(this.initialState.getObject("agent0").getDiscValForAttribute(MinecraftDomain.ATTZ));
		
		// Define desired subgoals here:
		
		// Flatworld subgoals
//		Subgoal sg3 = new Subgoal(this.pfIsHalfWay, this.pfIsAtGoal);
//		Subgoal sg2 = new Subgoal(this.pfIsHalfWay, this.pfIsThrQWay);
//		Subgoal sg1 = new Subgoal(this.pfIsOneQWay, this.pfIsHalfWay);
//		result.add(sg3);
//		result.add(sg2);
//		result.add(sg1);
		
		PropositionalFunction pfIsAgentBeforeBridge= new IsAgentYAt("IsBeforeBridge", this.mcdg.DOMAIN,
				new String[]{this.mcdg.CLASSAGENT}, (this.mcdg.MAXY + 1) / 2, 1);
		
		Subgoal bridge_sg = new Subgoal(this.pfIsAgentYAt, this.pfIsAtGoal);
		Subgoal before_bridge = new Subgoal(pfIsAgentBeforeBridge, this.pfIsAgentYAt);
		result.add(bridge_sg);
		result.add(before_bridge);
		// Jumpworld subgoals
//		Subgoal pastTrench = new Subgoal(this.pfIsAtLocation, this.pfIsAtGoal);
//		result.add(pastTrench);
//		
		// Breadworld subgoals
//		PropositionalFunction hasGrainPF = new AgentHasGrainPF(MinecraftDomain.ATTAGHASGRAIN, MinecraftDomain.DOMAIN,
//				new String[]{MinecraftDomain.CLASSAGENT});
//		Subgoal hasGrain = new Subgoal(hasGrainPF, pfAgentHasBread);
//		result.add(hasGrain);
		
		// Doorworld subgoals
//		PropositionalFunction doorOpenPF = new IsDoorOpen(MinecraftDomain.ISDOOROPEN, MinecraftDomain.DOMAIN,
//				new String[]{MinecraftDomain.CLASSAGENT}, new String[]{"2", "9", "1"});
//		Subgoal doorOpen = new Subgoal(doorOpenPF, this.pfIsAtGoal);
//		result.add(doorOpen);
		
		// Mazeworld subgoals
//		PropositionalFunction atEntrancePF = new IsAtLocationPF("ATENTRANCE", this.mcdg.DOMAIN,
//		new String[]{this.mcdg.CLASSAGENT, this.mcdg.CLASSGOAL}, new String[]{"6", "10", "1"});
//
//		PropositionalFunction midwayPF = new IsAtLocationPF("MIDWAY", this.mcdg.DOMAIN,
//		new String[]{this.mcdg.CLASSAGENT, this.mcdg.CLASSGOAL}, new String[]{"1", "10", "1"});
//
//		PropositionalFunction almostTherePF = new IsAtLocationPF("ALMOST", this.mcdg.DOMAIN,
//		new String[]{this.mcdg.CLASSAGENT, this.mcdg.CLASSGOAL}, new String[]{"5", "3", "1"});
//
//		Subgoal almostThere = new Subgoal(almostTherePF, this.pfIsAtGoal);
//		Subgoal halfwahThere = new Subgoal(midwayPF, almostTherePF);
//		Subgoal atEntrance = new Subgoal(atEntrancePF, midwayPF);
//		
//		result.add(almostThere);
//		result.add(halfwahThere);
//		result.add(atEntrance);

		// Hardworld subgoals
//		PropositionalFunction firstDoorOpenPF = new IsDoorOpen("FIRSTDOOROPEN", MinecraftDomain.DOMAIN,
//				new String[]{MinecraftDomain.CLASSAGENT}, new String[]{"10", "14", "1"});
//		PropositionalFunction secondDoorOpenPF = new IsDoorOpen("SECONDDOOROPEN", MinecraftDomain.DOMAIN,
//				new String[]{MinecraftDomain.CLASSAGENT}, new String[]{"1", "9", "1"});
//		
//		Subgoal secondDoor = new Subgoal(secondDoorOpenPF, this.pfIsAtGoal);
//		Subgoal firstDoor = new Subgoal(firstDoorOpenPF, secondDoorOpenPF);
//
//		result.add(secondDoor);
//		result.add(firstDoor);
		
		return result;
	}
	
	// ====== AFFORDANCE VERSIONS ======
	
	// === Affordance RTDP Planner	===
	public int AffordanceRTDPPlanner(int numRollouts, int maxDepth, ArrayList<Affordance> kb){
		
		RTDP planner = new RTDP(domain, rf, tf, 0.99, hashingFactory, 10 / (1 - 0.99), numRollouts, 0.01, maxDepth);
		
		int statePasses = planner.planFromStateAffordance(initialState, kb);

		// Create a Q-greedy policy from the planner
		 Policy p = new GreedyQPolicy((QComputablePlanner)planner);
		
		String actionSequence = p.evaluateAffordanceBehavior(initialState, rf, tf, kb).getActionSequenceString();
		System.out.println(actionSequence);
		return statePasses;	
	}
	
	// === Affordance VI Planner	===
	public int AffordanceVIPlanner(ArrayList<Affordance> kb){
		
		OOMDPPlanner planner = new ValueIteration(domain, rf, tf, 0.99, hashingFactory, .1, Integer.MAX_VALUE);
		
//		System.out.println((initialState.getStateDescription()));
		
		int statePasses = planner.planFromStateAffordance(initialState, kb);
		
		// Create a Q-greedy policy from the planner
		Policy p = new GreedyQPolicy((QComputablePlanner)planner);
		
		String actionSequence = p.evaluateAffordanceBehavior(initialState, rf, tf, kb).getActionSequenceString();
		System.out.println(actionSequence);
		return statePasses;	
	}
	
	public ArrayList<Affordance> generateAffordanceKB() {
		ArrayList<Affordance> affordances = new ArrayList<Affordance>();
		
		String ax = Integer.toString(this.initialState.getObject("agent0").getDiscValForAttribute(this.mcdg.ATTX));
		String ay = Integer.toString(this.initialState.getObject("agent0").getDiscValForAttribute(this.mcdg.ATTY));
		String az = Integer.toString(this.initialState.getObject("agent0").getDiscValForAttribute(this.mcdg.ATTZ));
		
		ArrayList<Action> isPlaneActions = new ArrayList<Action>();
		isPlaneActions.add(this.mcdg.forward);
		isPlaneActions.add(this.mcdg.backward);
		isPlaneActions.add(this.mcdg.left);
		isPlaneActions.add(this.mcdg.right);
//		isPlaneActions.add(this.mcdg.jumpF);
//		isPlaneActions.add(this.mcdg.jumpB);
//		isPlaneActions.add(this.mcdg.jumpR);
//		isPlaneActions.add(this.mcdg.jumpL);
		
		ArrayList<Action> isTrenchActions = new ArrayList<Action>();
//		isTrenchActions.add(this.mcdg.jumpF);
//		isTrenchActions.add(this.mcdg.jumpB);
//		isTrenchActions.add(this.mcdg.jumpR);
//		isTrenchActions.add(this.mcdg.jumpL);
//		
		isTrenchActions.add(this.mcdg.forward);
		isTrenchActions.add(this.mcdg.backward);
		isTrenchActions.add(this.mcdg.left);
		isTrenchActions.add(this.mcdg.right);
		
		isTrenchActions.add(this.mcdg.placeF);
		isTrenchActions.add(this.mcdg.placeB);
		isTrenchActions.add(this.mcdg.placeL);
		isTrenchActions.add(this.mcdg.placeR);
		
//		ArrayList<Action> isDoorActions = new ArrayList<Action>();
//		isDoorActions.add(this.mcdg.forward);
//		isDoorActions.add(this.mcdg.backward);
//		isDoorActions.add(this.mcdg.left);
//		isDoorActions.add(this.mcdg.right);
//		isDoorActions.add(this.mcdg.openF);
//		isDoorActions.add(this.mcdg.openB);
//		isDoorActions.add(this.mcdg.openR);
//		isDoorActions.add(this.mcdg.openL);
		
//		ArrayList<Action> isDstableWallActions = new ArrayList<Action>();
//		isDstableWallActions.add(this.mcdg.forward);
//		isDstableWallActions.add(this.mcdg.backward);
//		isDstableWallActions.add(this.mcdg.left);
//		isDstableWallActions.add(this.mcdg.right);
//		isDstableWallActions.add(this.mcdg.destF);
		
		ArrayList<Action> isOnGrainActions = new ArrayList<Action>();
		isOnGrainActions.add(this.mcdg.pickUpGrain);
		
		ArrayList<Action> isAdjOvenActions = new ArrayList<Action>();
		isAdjOvenActions.add(this.mcdg.useOvenF);
		isAdjOvenActions.add(this.mcdg.useOvenB);
		isAdjOvenActions.add(this.mcdg.useOvenR);
		isAdjOvenActions.add(this.mcdg.useOvenL);
		
		Affordance affIsPlane = new Affordance(this.pfIsPlane, this.pfIsAtGoal, isPlaneActions);
		Affordance affIsAdjTrench = new Affordance(this.pfIsAdjTrench, this.pfIsAtGoal, isTrenchActions);
//		Affordance affIsAdjDoor = new Affordance(this.pfIsAdjDoor, this.pfIsAtGoal, isDoorActions);
//		Affordance affIsAdjOven = new Affordance(this.pfIsAdjOven, this.pfIsAtGoal, isAdjOvenActions);
//		Affordance affIsOnGrain = new Affordance(this.pfIsOnGrain, this.pfIsAtGoal, isOnGrainActions);
//		Affordance affIsDstableWall = new Affordance(this.pfIsAdjDstableWall, this.pfIsAtGoal, isDstableWallActions);
		
		affordances.add(affIsPlane);
//		affordances.add(affIsAdjDoor);
		affordances.add(affIsAdjTrench);
//		affordances.add(affIsAdjOven);
//		affordances.add(affIsOnGrain);
//		affordances.add(affIsDstableWall);
		
		
		return affordances;
	}
	
	// === Affordance SG Planner (RTDP) ===
	public int AffordanceSubgoalPlanner(ArrayList<Affordance> kb, ArrayList<Subgoal> subgoals, int numRollouts, int maxDepth){
		
		// Initialize action plan
		String actionSequence = new String();
		
		// Build subgoal tree
		Node subgoalTree = generateGraph(subgoals);

		// Run BFS on subgoal tree (i.e. planning in subgoal space) 
		// returns a Node that is closer to the agent than the goal
		Node propfuncChain = BFS(subgoalTree);
		
		// Run VI between each subgoal in the chain
		State currState = initialState;
		int numUpdates = 0;
		
		rewardTable.remove(pfIsAtGoal);
		while(propfuncChain != null) {
			System.out.println("Current goal: " + propfuncChain.getPropFunc().toString());
//			rf = new SingleGoalPFRF(propfuncChain.getPropFunc(), 10, -1);
			
			rewardTable.put(propfuncChain.getPropFunc(), (Double) 10.0);
			rf = new SingleGoalMultiplePFRF(rewardTable, -1);
//			rf = new SingleGoalPFRF(propfuncChain.getPropFunc(), 10, -1); 
			tf = new SinglePFTF(propfuncChain.getPropFunc()); 
//			
//			rf = new SingleGoalMultiplePFRF(rewardTable, -1);
			tf = new SinglePFTF(propfuncChain.getPropFunc()); 
			goalCondition = new TFGoalCondition(tf);
			
			
//			OOMDPPlanner planner = new ValueIteration(domain, rf, tf, 0.99, hashingFactory, 0.01, Integer.MAX_VALUE);

//			numUpdates += planner.planFromStateAffordance(currState, kb);
			RTDP planner = new RTDP(domain, rf, tf, 0.99, hashingFactory, (5 / (1 - .99)), numRollouts, 0.01, maxDepth);
			
			numUpdates += planner.planFromStateAffordance(currState, kb);
			
			Policy p = new GreedyQPolicy((QComputablePlanner)planner);
			EpisodeAnalysis ea = p.evaluateAffordanceBehavior(currState, rf, tf, kb);
			
			// Add low level plan to overall plan and update current state to the end of that subgoal plan
			actionSequence += ea.getActionSequenceString() + "; ";
			currState = ea.getLastState();
			
			rewardTable.remove(propfuncChain.getPropFunc());
			propfuncChain = propfuncChain.getParent();
			
		}
		rewardTable.put(pfIsAtGoal, (Double) 10.0);
		System.out.println(actionSequence);
		return numUpdates;
		
	}
	
	
	public static void getResults() throws IOException {
		
		File fout = new File("test_map_results.txt");
		FileWriter fw = new FileWriter(fout.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		
		File[] files = new File("maps/testmaps").listFiles();
		String[] planners = {"VI","RTDP", "SG", "AFFVI", "AFFRTDP", "AFFSG"};
		int statePasses = 0;
		int numRollouts = 10000;
		int maxDepth = 250;
		
		for (File f: files) {
			System.out.println("Testing with map: " + f.getName());
			bw.write("Testing with map: " + f.getName() + "\n");
			
			// Minecraft world and knowledge base setup
			MinecraftBehavior mcb = new MinecraftBehavior(f.getName());
			ArrayList<Affordance> kb = mcb.generateAffordanceKB();
			ArrayList<Subgoal> subgoals = mcb.generateSubgoalKB();
			
			
			for(String planner : planners) {
				System.out.println("Using planner: " + planner);
			
				// Setup reward function
				
				// VANILLA OOMDP/VI
				if(planner.equals("VI")) {
					statePasses = mcb.ValueIterationPlanner();
				}
			
				// RTDP
				if(planner.equals("RTDP")) {
					statePasses = mcb.RTDPPlanner(numRollouts, maxDepth);
				}
				
				// SUBGOAL
				if(planner.equals("SG")) {
					statePasses = mcb.SubgoalPlanner(subgoals, numRollouts, maxDepth);
				}
				
				// AFFORDANCE - VI
				if(planner.equals("AFFVI")) {
					statePasses = mcb.AffordanceVIPlanner(kb);
				}
				
				// AFFORDANCE - RTDP
				if(planner.equals("AFFRTDP")) {
					statePasses = mcb.AffordanceRTDPPlanner(numRollouts, maxDepth, kb);
				}
				
				// AFFORDANCE - SUBGOAL
				if(planner.equals("AFFSG")) {
					statePasses = mcb.AffordanceSubgoalPlanner(kb, subgoals, numRollouts, maxDepth);
				}
				
				
				bw.write("\t" + planner + "," + statePasses + "\n");
				bw.flush();
			}
			bw.write("\n");
		}
		
		bw.close();

	}
	
	public static void main(String[] args) {
		
		// Collect Results
//		try {
//			getResults();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		// Setup Minecraft World
		MinecraftBehavior mcb = new MinecraftBehavior("4bridgeworld.map");
//		int numUpdates = 0;
		// VANILLA OOMDP/VI
//		 numUpdates = mcb.ValueIterationPlanner();
//		System.out.println("VI: " + numUpdates);

		// RTDP
//		String actionSequence = mcb.RTDPPlanner(numRollouts, maxDepth);
		
		// SUBGOALS
//		ArrayList<Subgoal> kb = mcb.generateSubgoalKB();
//		int numUpdates = mcb.SubgoalPlanner(kb, 1000, 200);
		
		// AFFORDANCE - VI
//		 ArrayList<Affordance> kb = mcb.generateAffordanceKB();
//		 numUpdates = mcb.AffordanceVIPlanner(kb);
		
		// AFFORDANCE - RTDP
//		 ArrayList<Affordance> kb = mcb.generateAffordanceKB();
//		 numUpdates = mcb.AffordanceRTDPPlanner(1000, 450, kb);
		
		// AFFORDANCE - SG
		 ArrayList<Affordance> kb = mcb.generateAffordanceKB();
		 ArrayList<Subgoal> subgoals = mcb.generateSubgoalKB();
		 int numUpdates = mcb.AffordanceRTDPPlanner(50, 50, kb);

		// END TIMER
//		timeEnd = System.nanoTime();
//		timeDelta = (double) (System.nanoTime()- timeStart) / 1000000000;
//		System.out.println("Took "+ timeDelta + " s"); 

//		System.out.println("AFFVI: " + numUpdates);

		
	}
	
	
}