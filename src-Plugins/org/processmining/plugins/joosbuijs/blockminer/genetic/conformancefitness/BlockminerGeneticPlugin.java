package org.processmining.plugins.joosbuijs.blockminer.genetic.conformancefitness;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.extension.XExtensionManager;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.buffered.XAttributeMapBufferedImpl;
import org.deckfour.xes.model.buffered.XTraceBufferedImpl;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.deckfour.xes.model.impl.XAttributeTimestampImpl;
import org.deckfour.xes.model.impl.XEventImpl;
import org.deckfour.xes.util.XTimer;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.Progress;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.plugins.astar.algorithm.AStarThread.Canceller;
import org.uncommons.maths.random.MersenneTwisterRNG;
import org.uncommons.maths.random.Probability;
import org.uncommons.watchmaker.framework.EvolutionEngine;
import org.uncommons.watchmaker.framework.EvolutionaryOperator;
import org.uncommons.watchmaker.framework.GenerationalEvolutionEngine;
import org.uncommons.watchmaker.framework.SelectionStrategy;
import org.uncommons.watchmaker.framework.islands.IslandEvolution;
import org.uncommons.watchmaker.framework.islands.RingMigration;
import org.uncommons.watchmaker.framework.operators.EvolutionPipeline;
import org.uncommons.watchmaker.framework.termination.GenerationCount;
import org.uncommons.watchmaker.framework.termination.Stagnation;
import org.uncommons.watchmaker.framework.termination.TargetFitness;

/**
 * ProM plugin class for the Genetic Block Miner
 * 
 * @author jbuijs
 * @since 16-8-2011
 * 
 */
public class BlockminerGeneticPlugin {

	/*-
	 * TODO general project todo's
	 * CODE
	 * - implement smart+Arya AND random+customFitness
	 * - use proper terms (e.g. function->~control flow logic)
	 * - add javadoc to each function
	 * - make use of functions
	 * - Move away from watchmaker framework to be able to ~combine fitness and mutation phases
	 * - Implement ProM cancellation
	 * - Implement other context plug-ins
	 * 
	 * PAPER/PRESENTATION:
	 * - use Arya's fitness metrics in 'the' genetic miner for 'fair' comparison
	 * - Argue that search space is limited but that we can still express all
	 *    'sensible' nets
	 * - 
	 */

	//SETTINGS
	/*
	 * TODO move to config class and build GUI on top of that. Make 'simple' and
	 * 'advanced' tab where the simple tab contains some 'natural language'
	 * options and advanced ALL options
	 */
	//TODO incrementally add less frequent event classes and see if we can find f=1?
	//Target fitness to stop at
	private double targetFitness = 1; //1.0; 
	//Maximum number of iterations(/generations) to try
	private int maxIterations = 1000;
	private int nrSteadyStates = 1000;
	//Population size
	//IDEA fixed or depending on tree size?
	private int populationSize = 10;
	//Number of candidates to keep and mutate/crossover (at least 2 to allow crossover)
	private int eliteCount = Math.max(populationSize / 10, 2);
	//Selection strategy
	//private SelectionStrategy<Object> selectionStrategy = new SigmaScaling();
	private SelectionStrategy<Object> selectionStrategy = new TournamentNSelection(5);
	/*
	 * ISLAND settings
	 */
	//Whether or not to use islands
	private boolean useIslands = false;
	//Number of islands
	private int nrIslands = 5;
	//Number of generations between migrations
	private int epochLength = 5;
	//Population size per island it total population size divided by the number of islands (under estimation!)
	private int populationSizePerIsland = populationSize / nrIslands;
	//Number of candidates that migrate
	private int migrantCount = (populationSizePerIsland / 2);
	/*
	 * PROBABILITIES
	 */
	//Followed recommendations of http://www.obitko.com/tutorials/genetic-algorithms/recommendations.php
	//Probability of 2 random nodes exchanging sub-trees
	private Probability crossoverProbability = Probability.ZERO; // Probability(0.1d);
	//Probability of a node mutating (otherwise random candidate generation...)
	private Probability mutationProbability = new Probability(1);//Probability.ONE;  //new Probability(0.95d);
	//Number of trials to run (when in trial 'mode')
	private int nrTrials = 100;

	/*-
	@Plugin(
			name = "Mine Block-structured Model using a Genetic Algorithm",
				parameterLabels = { "Event log" },
				returnLabels = { "Block Structured Model" },
				returnTypes = { Petrinet.class },
				userAccessible = true,
				help = "Mine a block structured process model from an event log using a genetic algorithm")
	@UITopiaVariant(
			uiLabel = "00JB Mine Block Structured Process Model using genetic algorithm",
				affiliation = "Eindhoven University of Technology",
				author = "J.C.A.M.Buijs",
				email = "j.c.a.m.buijs@tue.nl",
				pack = "JoosBuijs")/**/
	public Petrinet geneticBlockMiner(final PluginContext context, XLog eventlog) {

		Canceller canceller = new Canceller() {

			public boolean isCancelled() {
				return context.getProgress().isCancelled();
			}
		};
		context.getProgress().setMinimum(0);
		context.getProgress().setMaximum(maxIterations);
		context.getProgress().setIndeterminate(false);
		context.log("Starting genetic algorithm with (maximum) " + maxIterations + " generations.");

		//The tree factory to 
		TreeFactory factory = new TreeFactory(eventlog);

		//Building a list of operators for tree (crossover and mutation)
		List<EvolutionaryOperator<Tree>> operators = new ArrayList<EvolutionaryOperator<Tree>>(2);
		operators.add(new TreeCrossover(crossoverProbability));
		operators.add(new TreeMutation(factory, mutationProbability));

		//The evaluator will tell me how good (/bad) my trees are
		TreeEvaluator evaluator = new TreeEvaluator(context, eventlog);
		//TreeEvaluatorAStar evaluator = new TreeEvaluatorAStar(context, eventlog);

		//The future resulting tree
		Tree tree;

		//The evolution logger reports about the progress (not really required)
		EvolutionLogger<Tree> evolutionLogger = new EvolutionLogger<Tree>(context, true);

		if (useIslands) {
			/*
			 * Use islands
			 */
			/*-*/
			//MAIN ENGINE CALL for islands
			IslandEvolution<Tree> engine = new IslandEvolution<Tree>(nrIslands, new RingMigration(), factory,
					new EvolutionPipeline<Tree>(operators), evaluator, selectionStrategy, new MersenneTwisterRNG());

			//optionally add the observer
			engine.addEvolutionObserver(evolutionLogger);

			tree = engine.evolve(
					populationSizePerIsland, // Population size per island.
					eliteCount, epochLength, migrantCount, new ProMCancelTerminationCondition(canceller),
					new TargetFitness(targetFitness, evaluator.isNatural()));

			/**/
		} else {
			//Don't use islands

			/*-*/
			EvolutionEngine<Tree> engine = new GenerationalEvolutionEngine<Tree>(factory, new EvolutionPipeline<Tree>(
					operators), evaluator, selectionStrategy, new MersenneTwisterRNG());
			/**/

			engine.addEvolutionObserver(evolutionLogger);

			//Build a tree with the event classes of our event log at the leafs
			tree = engine.evolve(populationSize, eliteCount, new TargetFitness(targetFitness, evaluator.isNatural()),
					new ProMCancelTerminationCondition(canceller),
					//new GenerationCount(maxIterations)
					new Stagnation(nrSteadyStates, evaluator.isNatural(), true));
			/**/
		}

		//Lets see the candidate!
		System.out.println(tree.toString(0));

		//Close the logging file so the buffer is written
		evolutionLogger.closeFile();

		//Convert our tree to a PetriNet for now, later we will visualize trees directly
		TreeToPNConvertor conv = new TreeToPNConvertor();

		//TODO apply Murata rules to beautify err simplify the petri net
		//TODO also return an initial marking!!! (we know how to get one) 

		return conv.buildPetrinet(tree);
	}

	//Plug-in variant that will run 10 trials to see how fast the model is found
	/*-* /
	//TODO disabled plug-in annotation
	@Plugin(
			name = "Mine Block-structured Model using a Genetic Algorithm",
				parameterLabels = { "Event log" },
				returnLabels = { "Block Structured Model" },
				returnTypes = { Petrinet.class },
				userAccessible = true,
				help = "Mine a block structured process model from an event log using a genetic algorithm")
	@UITopiaVariant(
			uiLabel = "00JB Trial the Mine Block Structured Process Model using genetic algorithm",
				affiliation = "Eindhoven University of Technology",
				author = "J.C.A.M.Buijs",
				email = "j.c.a.m.buijs@tue.nl",
				pack = "JoosBuijs")
	/**/
	public Petrinet geneticBlockMinerTrials(final PluginContext context, XLog eventlog) {

		Canceller canceller = new Canceller() {

			public boolean isCancelled() {
				return context.getProgress().isCancelled();
			}
		};

		final Progress progress = context.getProgress();
		progress.setMinimum(0);
		progress.setIndeterminate(true);
		progress.setCaption("Trials");
		context.log("Starting genetic algorithm with " + nrTrials + " trials.");

		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat sdfFile = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");

		String filename = "./stats/stats_trials" + sdfFile.format(cal.getTime()) + ".csv";
		File statsFile = new File(filename);

		//			statsFile.createNewFile();
		statsFile.setWritable(true);
		statsFile.setReadable(true);

		DecimalFormat df = new DecimalFormat("#.######");
		FileOutputStream fos;
		PrintWriter out = null;
		try {
			fos = new FileOutputStream(statsFile);
			out = new PrintWriter(fos);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		out.println("trial; generation; fitness; repFit; behApp; tree");
		out.flush();
		//out.close();

		Tree tree = null;

		/*
		 * And repetitively run the genetic algorithm
		 */
		for (int i = 0; i < nrTrials; i++) {
			if (progress.isCancelled())
				return null;
			//Now call garbage collection manually
			System.gc();

			/*-
			//First check for cancellation
			if(progress.isCancelled()){
				return; //stop abruptly
			}/**/

			// Lets time our duration
			XTimer mappingTimer = new XTimer();
			mappingTimer.start();
			cal = Calendar.getInstance();
			String message = "Starting trial #" + i + " at " + sdf.format(cal.getTime());
			System.out.println(message);
			context.log(message);
			context.getProgress().inc();

			/*
			 * MAIN CALL
			 */
			//The tree factory to 
			TreeFactory factory = new TreeFactory(eventlog);

			//Building a list of operators for tree (crossover and mutation)
			List<EvolutionaryOperator<Tree>> operators = new ArrayList<EvolutionaryOperator<Tree>>(2);
			operators.add(new TreeCrossover(crossoverProbability));
			operators.add(new TreeMutation(factory, mutationProbability));

			//The evaluator will tell me how good (/bad) my trees are
			TreeEvaluator evaluator = new TreeEvaluator(context, eventlog);

			//The future resulting tree
			//Tree tree;

			//The evolution logger reports about the progress (not really required)
			EvolutionLogger<Tree> evolutionLogger = new EvolutionLogger<Tree>(context, false);

			/*-*/
			GenerationalEvolutionEngine<Tree> engine = new GenerationalEvolutionEngine<Tree>(factory,
					new EvolutionPipeline<Tree>(operators), evaluator, selectionStrategy, new Random()
			//new MersenneTwisterRNG()
			);
			/**/
			engine.setSingleThreaded(true);

			engine.addEvolutionObserver(evolutionLogger);

			//Build a tree with the event classes of our event log at the leafs
			tree = engine.evolve(populationSize, eliteCount, new TargetFitness(targetFitness, evaluator.isNatural()),
					new ProMCancelTerminationCondition(canceller), new GenerationCount(maxIterations));

			/*
			 * Processing of generation
			 */

			mappingTimer.stop();

			cal = Calendar.getInstance();
			System.out.println("Ending trial #" + i + " at " + sdf.format(cal.getTime()) + " after "
					+ mappingTimer.getDurationString());
			//System.out.println("Ending trial #" + i + " at " + sdf.format(cal.getTime()) + " after "
			//+ mappingTimer.getDurationString());

			//Log to a file
			/*-
			try {
				fos = new FileOutputStream(statsFile);
				out = new PrintWriter(fos);
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}/**/
			out.println(i + "; " + evolutionLogger.getNrGenerations() + "; "
					+ df.format(tree.getFitness().getFitness()) + "; "
					+ df.format(tree.getFitness().getReplayFitness()) + "; "
					+ df.format(tree.getFitness().getBehavioralAppropriateness()) + "; " + tree.toString());
			out.flush();

			//Try to clear some memory (desperate attempt)
			/*-
			factory = null;
			evaluator = null;
			tree = null;
			engine = null;
			/**/
		}

		//Close file
		out.close();
		TreeToPNConvertor conv = new TreeToPNConvertor();
		return conv.buildPetrinet(tree.getRoot());
		//return conv.buildPetrinet(new Tree(new MersenneTwisterRNG(), new TreeFactory(eventlog), "A"));
	}

	//Test plug-in for testing certain aspects of the code
	/*-* /
	//TODO disabled plug-in annotation
	@Plugin(
			name = "Mine Block-structured Model using a Genetic Algorithm",
				parameterLabels = { "Event log" },
				returnLabels = { "Block Structured Model" },
				returnTypes = { Petrinet.class },
				userAccessible = true,
				help = "Mine a block structured process model from an event log using a genetic algorithm")
	@UITopiaVariant(
			uiLabel = "00JB Genetic Block Miner - Test Mutation",
				affiliation = "Eindhoven University of Technology",
				author = "J.C.A.M.Buijs",
				email = "j.c.a.m.buijs@tue.nl",
				pack = "JoosBuijs")
	/**/
	public Petrinet geneticBlockMinerMutationTest(final PluginContext context, XLog eventlog) {
		/*
		 * This function pre-builds a tree that is almost correct but needs 1 or
		 * a few mutations to test the algorithm
		 */

		//		XEventClassifier classifier = XLogInfoImpl.STANDARD_CLASSIFIER;
		//		XLogInfo logInfo = XLogInfoFactory.createLogInfo(eventlog, classifier);

		TreeFactory factory = new TreeFactory(eventlog);

		TreeEvaluator evaluator = new TreeEvaluator(context, eventlog);

		Random rng = new MersenneTwisterRNG();

		DecimalFormat df = new DecimalFormat("#.######");

		/*-
		 * Building the tree
		 */
		//ABxBC, 14 hour tree
		//		String treeString = "SEQ(A+start,SEQ(A+complete,SEQ(XOR(B2+complete,XOR(B2+start,B1+start)),SEQ(B1+complete,SEQ(C+start,C+complete)))))";
		//ABaBC,
		//String treeString = "AND(SEQ(A+start,SEQ(A+complete,AND(B1+complete,SEQ(XOR(AND(B2+complete,B2+start),B1+start),SEQ(C+start,C+complete))))))";
		String treeString = "AND(XOR(A+complete,E+complete),AND(XOR(B+complete,D+complete),XOR(C+complete,F+complete)))";
		Tree almostThereTree = new Tree(rng, factory, treeString);

		System.out.println("Starting from:");
		System.out.println(almostThereTree.toString());

		//Now call mutation and fitness to see what happens
		double fitness = evaluator.getFitness(almostThereTree, null);

		Fitness fitnessObj = almostThereTree.getFitness();
		System.out.println("Fitness: " + df.format(fitnessObj.getReplayFitness()) + " BehApp "
				+ df.format(fitnessObj.getBehavioralAppropriateness()) + " Overall:"
				+ df.format(fitnessObj.getFitness()));

		//Apply random mutations
		/*-*/
		int counter = 0;
		while (fitness < 1 && counter < 100) {
			Tree mutatedTree = new Tree(almostThereTree);

			//Apply one of our mutation types at a time
			int action = rng.nextInt(4);
			action = 0;

			switch (action) {
				case 0 :
					//Change a single node but keep its type
					System.out.println("Mutating a single node");
					mutatedTree.mutateSingleNodeRandom(rng.nextBoolean());
					//We might want to change the order of some nodes
					break;
				case 1 :
					//Swap 2 subtrees
					System.out.println("Swapping subtrees");
					mutatedTree.mutateSwapSubtreesRandom();
					break;
				case 2 :
					//ADD one function node with existing subtree and new leaf node
					System.out.println("Adding a node");
					mutatedTree.mutateAddNodeRandom();
					break;
				case 3 :
					//Remove 1 node with one child
					System.out.println("Removing a node");
					mutatedTree.mutateRemoveNodeRandom();
					break;
				default :
					//Insert a new FNode and give it some children
					System.out.println("Adding an FNode");
					mutatedTree.mutateAddFNodeInBetweenRandom();
					break;
				case 6 :
					//Do exactly what needs to be done
					break;
			}

			System.out.println(" Mutated tree: " + mutatedTree.toString());

			if (almostThereTree.toString().equals(mutatedTree.toString())) {
				System.out.println(" >WHICH DIDN't CHANGE");
			}

			double fitnessMutation = evaluator.getFitness(mutatedTree, null);

			if (fitnessMutation > fitness) {
				almostThereTree = mutatedTree;
				fitness = fitnessMutation;
				System.out.println(">>>Which is better than the original! (fitness " + df.format(fitness) + "->"
						+ df.format(fitnessMutation) + " in try " + counter);
			}
			counter++;
		}/**/

		//Custom mutations

		/*
		 * Tree 1: The '14 hour stuck'-one on ABxBC
		 */
		/*-
		//Start: SEQ(A+start,SEQ(A+complete,SEQ(XOR(B2+complete,XOR(B2+start,B1+start)),SEQ(B1+complete,SEQ(C+start,C+complete)))))
		System.out.println("Moving B2s left of B2c");
		Node B2s = almostThereTree.getNode(8);
		almostThereTree.mutateRemoveNode(B2s);
		FunctionNode XorB2 = (FunctionNode) almostThereTree.getNode(5);
		XorB2.addChild(0, B2s);
		
		almostThereTree.invalidateFitness();
		evaluator.getFitness(almostThereTree, null);
		System.out.println(almostThereTree.toString());
		fitnessObj = almostThereTree.getFitness();
		System.out.println("Fitness: " + df.format(fitnessObj.getReplayFitness()) + " BehApp "
				+ df.format(fitnessObj.getBehavioralAppropriateness()) + " Overall:" + df.format(fitnessObj.getFitness()));

		System.out.println("Changing XOR parent of B1 to SEQ");
		FunctionNode XorB1 = (FunctionNode) almostThereTree.getNode(9); 
		XorB1.setFunction(FUNCTIONTYPE.SEQ);
		
		almostThereTree.invalidateFitness();
		evaluator.getFitness(almostThereTree, null);
		System.out.println(almostThereTree.toString());
		fitnessObj = almostThereTree.getFitness();
		System.out.println("Fitness: " + df.format(fitnessObj.getReplayFitness()) + " BehApp "
				+ df.format(fitnessObj.getBehavioralAppropriateness()) + " Overall:" + df.format(fitnessObj.getFitness()));

		System.out.println("Changing XOR parent of B2 to SEQ");
		FunctionNode topXor = (FunctionNode) almostThereTree.getNode(6);
		topXor.setFunction(FUNCTIONTYPE.SEQ);
		
		almostThereTree.invalidateFitness();
		evaluator.getFitness(almostThereTree, null);
		System.out.println(almostThereTree.toString());
		fitnessObj = almostThereTree.getFitness();
		System.out.println("Fitness: " + df.format(fitnessObj.getReplayFitness()) + " BehApp "
				+ df.format(fitnessObj.getBehavioralAppropriateness()) + " Overall:" + df.format(fitnessObj.getFitness()));
		
		System.out.println("Moving B1c right of B1s");
		Node B1c = almostThereTree.getNode(12);
		almostThereTree.mutateRemoveNode(B1c);
		FunctionNode XorPB1 = (FunctionNode) almostThereTree.getNode(9);
		XorPB1.addChild(1, B1c);

		almostThereTree.invalidateFitness();
		evaluator.getFitness(almostThereTree, null);
		System.out.println(almostThereTree.toString());
		fitnessObj = almostThereTree.getFitness();
		System.out.println("Fitness: " + df.format(fitnessObj.getReplayFitness()) + " BehApp "
				+ df.format(fitnessObj.getBehavioralAppropriateness()) + " Overall:" + df.format(fitnessObj.getFitness()));
		/**/

		/*
		 * Tree 2: ABaBC
		 */
		//From AND(SEQ(A+start,SEQ(A+complete,AND(B1+complete,SEQ(XOR(AND(B2+complete,B2+start),B1+start),SEQ(C+start,C+complete))))))

		/*-
		System.out.println("Move B1s next to B1c");
		FunctionNode ANDB1 = (FunctionNode) almostThereTree.getNode(5);
		Node B1s = almostThereTree.getNode(12);
		almostThereTree.mutateRemoveNode(B1s);
		ANDB1.addChild(0, B1s);

		almostThereTree.invalidateFitness();
		evaluator.getFitness(almostThereTree, null);
		System.out.println(almostThereTree.toString());
		fitnessObj = almostThereTree.getFitness();
		System.out.println("Fitness: " + df.format(fitnessObj.getReplayFitness()) + " BehApp "
				+ df.format(fitnessObj.getBehavioralAppropriateness()) + " Overall:"
				+ df.format(fitnessObj.getFitness()));

		System.out.println("Changing AND B1 to SEQ");
		ANDB1 = (FunctionNode) almostThereTree.getNode(6);
		ANDB1.setFunction(FUNCTIONTYPE.SEQ);

		almostThereTree.invalidateFitness();
		evaluator.getFitness(almostThereTree, null);
		System.out.println(almostThereTree.toString());
		fitnessObj = almostThereTree.getFitness();
		System.out.println("Fitness: " + df.format(fitnessObj.getReplayFitness()) + " BehApp "
				+ df.format(fitnessObj.getBehavioralAppropriateness()) + " Overall:"
				+ df.format(fitnessObj.getFitness()));

		System.out.println("Changing AND B2 to SEQ and swapping children");
		FunctionNode ANDB2 = (FunctionNode) almostThereTree.getNode(11);
		ANDB2.setFunction(FUNCTIONTYPE.SEQ);
		Node B2s = ANDB2.getChild(1);
		ANDB2.removeChild(1);
		ANDB2.addChild(0, B2s);

		almostThereTree.invalidateFitness();
		evaluator.getFitness(almostThereTree, null);
		System.out.println(almostThereTree.toString());
		fitnessObj = almostThereTree.getFitness();
		System.out.println("Fitness: " + df.format(fitnessObj.getReplayFitness()) + " BehApp "
				+ df.format(fitnessObj.getBehavioralAppropriateness()) + " Overall:"
				+ df.format(fitnessObj.getFitness()));

		System.out.println("Swapping AND and SEQC nodes");
		FunctionNode SEQC = (FunctionNode) almostThereTree.getNode(14);
		FunctionNode AND = (FunctionNode) almostThereTree.getNode(5);
		almostThereTree.mutateSwapSubtrees(SEQC, AND);

		almostThereTree.invalidateFitness();
		evaluator.getFitness(almostThereTree, null);
		System.out.println(almostThereTree.toString());
		fitnessObj = almostThereTree.getFitness();
		System.out.println("Fitness: " + df.format(fitnessObj.getReplayFitness()) + " BehApp "
				+ df.format(fitnessObj.getBehavioralAppropriateness()) + " Overall:"
				+ df.format(fitnessObj.getFitness()));
		/**/

		//Finalize and output
		System.out.println("Final: " + almostThereTree.toString() + " (" + df.format(fitnessObj.getFitness()) + ")");
		TreeToPNConvertor conv = new TreeToPNConvertor();

		return conv.buildPetrinet(almostThereTree);
	}

	//Generate all possible trees and output fitness of each candidate to a file
	/*-
	@Plugin(name = "Mine Block-structured Model using a Genetic Algorithm",
	//parameterLabels = { },
				parameterLabels = { "Event log" },
				returnLabels = { "Block Structured Model" },
				returnTypes = { Petrinet.class },
				userAccessible = true,
				help = "Mine a block structured process model from an event log using a genetic algorithm")
	@UITopiaVariant(
			uiLabel = "00JB Print out all possible trees and return fittest",
				affiliation = "Eindhoven University of Technology",
				author = "J.C.A.M.Buijs",
				email = "j.c.a.m.buijs@tue.nl",
				pack = "JoosBuijs")/**/
	public Petrinet geneticBlockMinerAllTreeGenerator(final PluginContext context, XLog eventLog) {
		context.getProgress().setMinimum(0);
		context.getProgress().setMaximum(maxIterations);
		context.getProgress().setIndeterminate(true);

		TreeFactory factory = new TreeFactory(eventLog);
		TreeEvaluator evaluator = new TreeEvaluator(context, eventLog);
		//TreeEvaluatorAStar evaluator = new TreeEvaluatorAStar(context, eventLog);

		XEventClassifier eventClassifier = XLogInfoImpl.STANDARD_CLASSIFIER;
		XLogInfo logInfo = XLogInfoFactory.createLogInfo(eventLog, eventClassifier);
		LinkedList<XEventClass> eventClasses = new LinkedList<XEventClass>(logInfo.getEventClasses().getClasses());

		String startMessage = " Generating all trees for " + eventClasses.size() + " event classes";
		context.log(startMessage);
		System.out.println(startMessage);

		XTimer generationTimer = new XTimer();
		generationTimer.start();

		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdfFile = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");
		String filename = "./stats/stats_treegen_" + sdfFile.format(cal.getTime()) + ".csv";
		File statsFile = new File(filename);

		//			statsFile.createNewFile();
		statsFile.setWritable(true);
		statsFile.setReadable(true);

		DecimalFormat df = new DecimalFormat("#.######");
		FileOutputStream fos;
		PrintWriter out = null;
		try {
			fos = new FileOutputStream(statsFile);
			out = new PrintWriter(fos);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		out.println("fitness; behapp; tree");

		/*
		 * The core call
		 */
		LinkedHashSet<Tree> trees = factory.generateAllCombinations(new MersenneTwisterRNG());

		generationTimer.stop();

		System.out.println("" + trees.size() + " Trees were generated:");
		System.out.println("Tree generation time: " + generationTimer.getDurationString());

		XTimer fitnessTimer = new XTimer();
		fitnessTimer.start();

		//Now loop through the trees and remember who was fittest...
		double fittestFitnes = Integer.MAX_VALUE;
		Tree fittestTree = null;

		int counter = 0;
		XTimer forLoopTimer = new XTimer();
		forLoopTimer.start();

		/*-*/
		for (Tree tree : trees) {
			if ((counter % 100) == 0) {
				forLoopTimer.stop();
				String proc = "Calc. fitness for tree #" + counter + " for " + forLoopTimer.getDurationString();
				context.log(proc);
				System.out.println(proc);
				out.flush();
				forLoopTimer.start();
			}
			//System.out.println(" " + tree.toString());

			double thisTreesFitness = evaluator.getFitness(tree, null);

			//Log to file
			Fitness fitness = tree.getFitness();
			out.println(df.format(fitness.getReplayFitness()) + "; "
					+ df.format(fitness.getBehavioralAppropriateness()) + "; " + tree.toString());

			if (thisTreesFitness < fittestFitnes) {
				fittestTree = tree;
				fittestFitnes = thisTreesFitness;
			}
			counter++;
		}
		fitnessTimer.stop();

		System.out.println(" Fittest tree: " + fittestTree.toString());
		System.out.println(" With fitness: " + fittestFitnes);
		System.out.println("Fitness calc. time: " + fitnessTimer.getDurationString());
		/**/

		out.close();

		TreeToPNConvertor conv = new TreeToPNConvertor();
		return conv.buildPetrinet(fittestTree);
	}

	//Plug-in variant that will randomly create trees and count the number with fitness = 1
	/*-* /
	//TODO disabled plug-in annotation
	@Plugin(
			name = "Mine Block-structured Model using a Genetic Algorithm",
				parameterLabels = { "Event log" },
				returnLabels = { "Block Structured Model" },
				returnTypes = { String.class },
				userAccessible = true,
				help = "Mine a block structured process model from an event log using a genetic algorithm")
	@UITopiaVariant(
			uiLabel = "00JB Count #correct random trees",
				affiliation = "Eindhoven University of Technology",
				author = "J.C.A.M.Buijs",
				email = "j.c.a.m.buijs@tue.nl",
				pack = "JoosBuijs")
	/**/
	public String geneticBlockMinerRandomTreeFitnessCounter(final PluginContext context, XLog eventLog) {
		int nrTries = 3240;
		int nrFit = 0;
		context.log("We will generate " + nrTries + " random trees and tell you how many have fitness 1.0");

		Progress progress = context.getProgress();
		progress.setMaximum(nrTries);

		TreeFactory factory = new TreeFactory(eventLog);
		TreeEvaluator evaluator = new TreeEvaluator(context, eventLog);
		Random rng = new MersenneTwisterRNG();

		XEventClassifier eventClassifier = XLogInfoImpl.STANDARD_CLASSIFIER;
		XLogInfo logInfo = XLogInfoFactory.createLogInfo(eventLog, eventClassifier);
		LinkedList<XEventClass> eventClasses = new LinkedList<XEventClass>(logInfo.getEventClasses().getClasses());

		String filename = "./stats/stats_treerandomgen.csv";
		File statsFile = new File(filename);

		//			statsFile.createNewFile();
		statsFile.setWritable(true);
		statsFile.setReadable(true);

		DecimalFormat df = new DecimalFormat("#.######");
		FileOutputStream fos;
		PrintWriter out = null;
		try {
			fos = new FileOutputStream(statsFile);
			out = new PrintWriter(fos);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		out.println("fitness; behapp; tree");

		for (int i = 0; i < nrTries; i++) {
			Tree tree = factory.generateRandomCandidate(rng);

			evaluator.getFitness(tree, null);

			//Log to file
			Fitness fitness = tree.getFitness();
			out.println(df.format(fitness.getReplayFitness()) + "; "
					+ df.format(fitness.getBehavioralAppropriateness()) + "; " + tree.toString());

			if (fitness.getReplayFitness() == 1.0) {
				nrFit++;
			}

			progress.inc();
		}

		System.out.println("Found " + nrFit + " fitting trees");

		out.close();

		return "After generating " + nrTries + " trees we found that " + nrFit
				+ " of these trees had a fitness of 1.0.";
	}

	//Testing other genetic framework
	/*-
	@Plugin(name = "Mine Block-structured Model using a Genetic Algorithm",
	//parameterLabels = { },
				parameterLabels = { "Event log" },
				returnLabels = { "Block Structured Model" },
				returnTypes = { Petrinet.class },
				userAccessible = true,
				help = "Mine a block structured process model from an event log using a genetic algorithm")
	@UITopiaVariant(
			uiLabel = "00JB ECJ test",
				affiliation = "Eindhoven University of Technology",
				author = "J.C.A.M.Buijs",
				email = "j.c.a.m.buijs@tue.nl",
				pack = "JoosBuijs")
	public void ECJgeneticBlockMiner(final PluginContext context, XLog eventLog) {
		//Evolve.main(new String[] {"-file","tutorial1.params"});
		//Evolve.main(new String[] { "-file", "block.params", "-p", "stat.gather-full=true" });
		//		Evolve.main(new String[] { "-file", "block.params", "-p", "stat.gather-full=true" });

	}

	//Small tests
	@Plugin(name = "Genetic Block Algorithm - small test",
	//parameterLabels = { },
				parameterLabels = { "Event log" },
				returnLabels = { "Block Structured Model" },
				returnTypes = { Petrinet.class },
				userAccessible = true,
				help = "GBA small test")
	@UITopiaVariant(
			uiLabel = "00JB GBA small test",
				affiliation = "Eindhoven University of Technology",
				author = "J.C.A.M.Buijs",
				email = "j.c.a.m.buijs@tue.nl",
				pack = "JoosBuijs")
	public Petrinet geneticBlockMinerSmallTest(final PluginContext context, XLog eventLog) {
		Random rng = new MersenneTwisterRNG();
		TreeFactory factory = new TreeFactory(eventLog);

		//String treeString = "XOR(XOR(B2+start,AND(SEQ(B2+start,SEQ(XOR(B2+complete),SEQ(C+start,C+complete))),SEQ(XOR(A+start),AND(SEQ(B1+start,B1+complete),A+complete)))),AND(SEQ(B2+start,SEQ(XOR(B2+complete),SEQ(C+start,C+complete))),SEQ(XOR(A+start),AND(SEQ(B1+start,B1+complete),A+complete))))";
		String treeString = "SEQ(A+complete,SEQ(B+complete,C+complete))";
		Tree tree = new Tree(rng, factory, treeString);

		//TreeEvaluator eval = new TreeEvaluator(context, eventLog);
		TreeEvaluatorAStar eval = new TreeEvaluatorAStar(context, eventLog);
		double fitness = eval.getFitness(tree, null);

		System.out.println("fitness: " + fitness);

		TreeToPNConvertor conv = new TreeToPNConvertor();
		return conv.buildPetrinet(tree);
	}

	@Plugin(name = "Genetic Block Algorithm - create testlogs",
	//parameterLabels = { },
				parameterLabels = {},
				returnLabels = { "Test Event Log" },
				returnTypes = { XLog.class },
				userAccessible = true,
				help = "GBA create testlogs")
	@UITopiaVariant(
			uiLabel = "00JB GBA create test logs",
				affiliation = "Eindhoven University of Technology",
				author = "J.C.A.M.Buijs",
				email = "j.c.a.m.buijs@tue.nl",
				pack = "JoosBuijs")
	/**
	 * Quick plug-in to create all combinations of a pure AND process
	 */
	public XLog createExperimentLogs(final PluginContext context) {
		// Create a Xfactory to produce the eventlog
		XFactory factory = XFactoryRegistry.instance().currentDefault();
		XLog xLog = factory.createLog();

		XExtensionManager extManager = XExtensionManager.instance();
		String[] uris = { "http://code.fluxicon.com/xes/lifecycle.xesext", "http://code.fluxicon.com/xes/time.xesext",
				"http://code.fluxicon.com/xes/concept.xesext", "http://code.fluxicon.com/xes/semantic.xesext",
				"http://code.fluxicon.com/xes/org.xesext", };
		for (String uri : uris) {
			try {
				xLog.getExtensions().add(extManager.getByUri(new URI(uri)));
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		xLog.getClassifiers().add(XLogInfoImpl.STANDARD_CLASSIFIER);

		int nrAct = 5;
		//Activity names
		String[] names = { "A", "B", "C", "D", "E", "F" };

		//AND mode
		PermutationGenerator x = new PermutationGenerator(nrAct);
		int[] indices;
		DateFormat df = new SimpleDateFormat();

		while (x.hasMore()) {
			StringBuilder traceName = new StringBuilder();
			LinkedList<XEvent> events = new LinkedList<XEvent>();
			int counter = 0;

			indices = x.getNext();
			for (int i = 0; i < indices.length; i++) {
				traceName.append(names[indices[i]]);

				XAttributeMap eventAtt = new XAttributeMapBufferedImpl();
				eventAtt.put("lifecycle:transition", new XAttributeLiteralImpl("lifecycle:transition", "complete"));
				eventAtt.put("concept:name", new XAttributeLiteralImpl("concept:name", names[indices[i]]));
				try {
					eventAtt.put("time:timestamp",
							new XAttributeTimestampImpl("time:timestamp", df.parse("01-01-2011 10:" + counter + ":00")));
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				XEvent event = new XEventImpl(eventAtt);

				events.add(event);
				counter++;
			}

			//			System.out.println("Tracename: " + traceName.toString());

			XAttributeMap traceAtt = new XAttributeMapBufferedImpl();
			traceAtt.put("concept:name", new XAttributeLiteralImpl("concept:name", traceName.toString()));

			XTraceBufferedImpl xTrace = (XTraceBufferedImpl) factory.createTrace(traceAtt);

			xTrace.addAll(events);
			xLog.add(xTrace);
		}

		return xLog;
	}
}
