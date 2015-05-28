package org.processmining.plugins.joosbuijs.blockminer.genetic;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.util.XTimer;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.uncommons.maths.random.MersenneTwisterRNG;
import org.uncommons.maths.random.Probability;
import org.uncommons.watchmaker.framework.EvolutionEngine;
import org.uncommons.watchmaker.framework.EvolutionaryOperator;
import org.uncommons.watchmaker.framework.GenerationalEvolutionEngine;
import org.uncommons.watchmaker.framework.islands.IslandEvolution;
import org.uncommons.watchmaker.framework.islands.RingMigration;
import org.uncommons.watchmaker.framework.operators.EvolutionPipeline;
import org.uncommons.watchmaker.framework.selection.RouletteWheelSelection;
import org.uncommons.watchmaker.framework.selection.SigmaScaling;
import org.uncommons.watchmaker.framework.termination.GenerationCount;
import org.uncommons.watchmaker.framework.termination.TargetFitness;

/**
 * ProM plugin class for the Genetic Block Miner
 * 
 * @author jbuijs
 * @since 16-8-2011
 * 
 */
public class BlockminerGeneticPlugin {

	//SETTINGS
	//TODO move to config class and build GUI on top of that
	/*
	 * How many leafs should we include in our initial collection? 1.0 is the
	 * number needed to include each event class once (in theory)
	 */
	private double duplicationfactor = 1.2;
	//Target fitness to stop at
	private double targetFitness = 1.0;
	//Maximum number of iterations(/generations) to try
	private int maxIterations = 100;
	//Population size
	//TODO fixed or depending on tree size?
	private int populationSize = 100;
	//Number of candidates to keep and mutate/crossover (at least 2 to allow crossover)
	private int eliteCount = Math.max(populationSize / 20, 2);
	/*
	 * ISLAND settings
	 */
	//Whether or not to use islands
	private boolean useIslands = true;
	//Number of islands
	private int nrIslands = 5;
	//Number of generations between migrations
	private int epochLength = 5;
	//Population size per island it total population size divided by the number of islands (under estimation!)
	private int populationSizePerIsland = populationSize / nrIslands;
	//Number of candidates that migrate
	private int migrantCount = (populationSize / 2);
	/*
	 * PROBABILITIES
	 */
	//Followed recommendations of http://www.obitko.com/tutorials/genetic-algorithms/recommendations.php
	/*
	 * Probability of a new node becoming a function (might introduce leafs when
	 * more than one node is required with 1-chance)
	 */
	private double functionProbability = 0.9d;
	//Probability of 2 random nodes mutating (
	private Probability crossoverProbability = Probability.ONE;// new Probability(0.95d);
	//Probability of a node mutating
	private Probability mutationProbability = new Probability(0.1d);
	//Number of trials to run (when in trial 'mode')
	private int nrTrials = 500;

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
				pack = "JoosBuijs")
	public Petrinet geneticBlockMiner(final PluginContext context, XLog eventlog) {
		context.getProgress().setMinimum(0);
		context.getProgress().setMaximum(maxIterations);
		context.getProgress().setIndeterminate(false);
		context.getProgress().setCaption("Starting genetic algorithm with " + maxIterations + " generations.");

		XEventClassifier classifier = XLogInfoImpl.STANDARD_CLASSIFIER;
		XLogInfo logInfo = XLogInfoFactory.createLogInfo(eventlog, classifier);

		//Create leaf nodes of each event class in the event log
		List<EventClassNode> eventClasses = new ArrayList<EventClassNode>();
		for (XEventClass eventClass : logInfo.getEventClasses().getClasses()) {
			eventClasses.add(new EventClassNode(eventClass));
		}

		/*
		 * The maximal number of nodes we need is the number of leafs times 2
		 * minus one. But we also allow for a certain amount of 'leaf
		 * duplication' so multiply by the duplicationfactor. Please note that
		 * the tree might be smaller!!!
		 */
		int maxNodes = (int) (((eventClasses.size() * 2) - 1) * duplicationfactor);
		//int nrLeafs = (int) Math.ceil(eventClasses.size() * duplicationfactor);
		TreeFactory factory = new TreeFactory(maxNodes, eventClasses, new Probability(functionProbability));

		List<EvolutionaryOperator<Node>> operators = new ArrayList<EvolutionaryOperator<Node>>(2);
		operators.add(new TreeCrossover(crossoverProbability));
		operators.add(new TreeMutation(factory, mutationProbability));

		TreeEvaluator evaluator = new TreeEvaluator(context, eventlog);

		Node node;

		if (useIslands) {
			/*
			 * Use islands
			 */
			/*-*/
			IslandEvolution<Node> engine = new IslandEvolution<Node>(nrIslands, new RingMigration(), factory,
					new EvolutionPipeline<Node>(operators), evaluator, new SigmaScaling(), new MersenneTwisterRNG());

			engine.addEvolutionObserver(new EvolutionLogger<Node>(context));

			node = engine.evolve(populationSizePerIsland, // Population size per island.
					eliteCount, epochLength, migrantCount, new TargetFitness(targetFitness, evaluator.isNatural()));
			/**/
		} else {

			/*
			 * Use simple evolution method
			 */
			/*-*/
			EvolutionEngine<Node> engine = new GenerationalEvolutionEngine<Node>(factory, new EvolutionPipeline<Node>(
					operators), evaluator, new SigmaScaling(), new MersenneTwisterRNG());

			engine.addEvolutionObserver(new EvolutionLogger<Node>(context));

			//Build a tree with the event classes of our event log at the leafs
			node = engine.evolve(populationSize, eliteCount, new TargetFitness(targetFitness, evaluator.isNatural()),
					new GenerationCount(maxIterations));
			/**/
		}

		System.out.println(node.toString(0));

		TreeToPNConvertor conv = new TreeToPNConvertor();

		//TODO apply Murata rules to beautify err simplify the petri net

		return conv.buildPetrinet(node);
	}

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
	public void geneticBlockMinerTrials(final PluginContext context, XLog eventlog) {
		/*-
		// initialize logging to go to rolling log file
		//From http://blogs.oracle.com/nickstephen/entry/java_redirecting_system_out_and
		LogManager logManager = LogManager.getLogManager();
		logManager.reset();

		// log file max size 50MB, 3 rolling files, append-on-open
		Handler fileHandler;
		try {
			fileHandler = new FileHandler("log", 50 * 1024 * 1024, 3, true);
			fileHandler.setFormatter(new SimpleFormatter());
			Logger.getLogger("").addHandler(fileHandler);

			// preserve old stdout/stderr streams in case they might be useful      
			PrintStream stdout = System.out;
			PrintStream stderr = System.err;

			// now rebind stdout/stderr to logger                                  
			Logger logger;
			LoggingOutputStream los;

			logger = Logger.getLogger("stdout");
			los = new LoggingOutputStream(logger, StdOutErrLevel.STDOUT);
			System.setOut(new PrintStream(los, true));

			logger = Logger.getLogger("stderr");
			los = new LoggingOutputStream(logger, StdOutErrLevel.STDERR);
			System.setErr(new PrintStream(los, true));
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/**/

		/*
		 * Now prepare everything
		 */
		XEventClassifier classifier = XLogInfoImpl.STANDARD_CLASSIFIER;
		XLogInfo logInfo = XLogInfoFactory.createLogInfo(eventlog, classifier);

		//Create leaf nodes of each event class in the event log
		List<EventClassNode> eventClasses = new ArrayList<EventClassNode>();
		for (XEventClass eventClass : logInfo.getEventClasses().getClasses()) {
			eventClasses.add(new EventClassNode(eventClass));
		}

		/*
		 * The maximal number of nodes we need is the number of leafs times 2
		 * minus one. But we also allow for a certain amount of 'leaf
		 * duplication' so multiply by the duplicationfactor. Please note that
		 * the tree might be smaller!!!
		 */
		int maxNodes = (int) (((eventClasses.size() * 2) - 1) * duplicationfactor);
		//int nrLeafs = (int) Math.ceil(eventClasses.size() * duplicationfactor);
		TreeFactory factory = new TreeFactory(maxNodes, eventClasses, new Probability(functionProbability));

		List<EvolutionaryOperator<Node>> operators = new ArrayList<EvolutionaryOperator<Node>>(2);
		operators.add(new TreeMutation(factory, mutationProbability));
		operators.add(new TreeCrossover());

		TreeEvaluator evaluator = new TreeEvaluator(context, eventlog);

		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		/*
		 * And repetitively run the genetic algorithm
		 */
		for (int i = 0; i < nrTrials; i++) {
			// Lets time our duration
			XTimer mappingTimer = new XTimer();
			mappingTimer.start();
			System.out.println("Starting trial #" + i + " at " + sdf.format(cal.getTime()));

			//targetFitness = 0.1;

			EvolutionEngine<Node> engine = new GenerationalEvolutionEngine<Node>(factory, new EvolutionPipeline<Node>(
					operators), evaluator, new RouletteWheelSelection(), new MersenneTwisterRNG());

			engine.addEvolutionObserver(new EvolutionLogger<Node>(context));

			//Build a tree with the event classes of our event log at the leafs
			Node node = engine.evolve(populationSize, eliteCount,
					new TargetFitness(targetFitness, evaluator.isNatural()), new GenerationCount(maxIterations));

			//Nicely print out the resulting tree
			System.out.println(node.toString(0));

			mappingTimer.stop();

			System.out.println("Ending trial #" + i + " at " + sdf.format(cal.getTime()) + " after "
					+ mappingTimer.getDurationString());
		}

		//return conv.buildPetrinet(node);

	}

}
