package org.processmining.plugins.joosbuijs.processtrees.geneticminer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.plugins.astar.algorithm.AStarThread.Canceller;
import org.processmining.plugins.boudewijn.tree.Tree;
import org.processmining.plugins.joosbuijs.blockminer.genetic.conformancefitness.ProMCancelTerminationCondition;
import org.uncommons.maths.random.MersenneTwisterRNG;
import org.uncommons.maths.random.Probability;
import org.uncommons.watchmaker.framework.AbstractEvolutionEngine;
import org.uncommons.watchmaker.framework.EvaluatedCandidate;
import org.uncommons.watchmaker.framework.EvolutionaryOperator;
import org.uncommons.watchmaker.framework.GenerationalEvolutionEngine;
import org.uncommons.watchmaker.framework.SelectionStrategy;
import org.uncommons.watchmaker.framework.islands.IslandEvolution;
import org.uncommons.watchmaker.framework.islands.RingMigration;
import org.uncommons.watchmaker.framework.operators.EvolutionPipeline;
import org.uncommons.watchmaker.framework.selection.SigmaScaling;
import org.uncommons.watchmaker.framework.termination.GenerationCount;
import org.uncommons.watchmaker.framework.termination.Stagnation;
import org.uncommons.watchmaker.framework.termination.TargetFitness;

/**
 * Main Genetic Algorithm flow class, also contains its parameters!
 * 
 * @author jbuijs
 * 
 */
public class GeneticAlgorithm {

	private Canceller canceller;
	private TreeFactory factory;
	private List<Secuencia> cycles;

	/**
	 * Evolution Parameters
	 */
	//Target fitness to stop at
	private double targetFitness = 0.05;
	//Population size
	private int populationSize = 100;
	//Number of candidates to keep and mutate/crossover (at least 2 to allow crossover)
	private int eliteCount = Math.max(populationSize / 5, 2);
	//Selection strategy
	private SelectionStrategy<Object> selectionStrategy = new SigmaScaling();
	//private SelectionStrategy<Object> selectionStrategy = new TournamentNSelection(5);
	//Whether or not to enforce a single thread for the fitness calculations
	private boolean singleThreaded = false;

	/**
	 * ISLAND
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

	/**
	 * PROBABILITIES
	 */
	//Followed recommendations of http://www.obitko.com/tutorials/genetic-algorithms/recommendations.php
	//Probability of 2 random nodes exchanging sub-trees
	private Probability crossoverProbability = new Probability(0.2d);
	//Probability that the worst candidate is replaced by a random tree. 
	private int randomCandidateCount = 1; //new Probability(1/populationSize); 

	/**
	 * TERMINATION
	 */
	//Maximum number of iterations(/generations) to try
	private int maxIterations = 10;
	//Nr of steady states
	private int steadyStates = Math.max((maxIterations / 100), 2);

	private double fitnessWeight = 1.0;
	private double simplicityWeight = 1.0;
	private double generalizationWeight = 1.0;
	private double precisionWeight = 1.0;
	private double coincidenciaWeight = 1.0;

	/*
	 * OBJECTS
	 */
	//The evolution logger reports about the progress (not really required)
	private EvolutionLogger<Tree> evolutionLogger;

	/**
	 * Instantiates a new genetic algorithm instance with default parameters
	 * 
	 * @param canceller
	 * @param eventlog
	 */
	public GeneticAlgorithm(PluginContext context, final Canceller canceller, XLog eventlog) {
		//The tree factory that contains information about the event log, activities etc.
		factory = new TreeFactory(eventlog, randomCandidateCount);
		this.canceller = canceller;
		this.evolutionLogger = new EvolutionLogger<Tree>(context, false);
	}

	public Tree run(final String path) {

		//Building a list of operators for tree (crossover and mutation)
		List<EvolutionaryOperator<Tree>> operators = new ArrayList<EvolutionaryOperator<Tree>>(2);
		operators.add(factory); //First introduce a random candidate instead of the worst
		operators.add(new TreeCrossover(crossoverProbability)); //Then let them mate
		operators.add(new TreeMutation(factory)); //Then mutate

		//The evaluator will tell me how good (/bad) my trees are
		TreeEvaluatorAStar evaluator = new TreeEvaluatorAStar(canceller, evolutionLogger, factory.getLog(),
				fitnessWeight, simplicityWeight, generalizationWeight, precisionWeight);
		evaluator.setCycles(cycles);

		//The future resulting tree
		Tree tree;

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
		  System.out.println("ELSE");
			/*-*/
			AbstractEvolutionEngine<Tree> engine = new GenerationalEvolutionEngine<Tree>(factory,
					new EvolutionPipeline<Tree>(operators), evaluator, selectionStrategy, new MersenneTwisterRNG()) {
				private int generation = 0;

				protected List<EvaluatedCandidate<Tree>> evaluatePopulation(List<Tree> population) {
					List<EvaluatedCandidate<Tree>> result = super.evaluatePopulation(population);
					if (path != null) {
						File log = new File(path + File.separator + "generation" + generation++ + ".log");
						try {
							if (log.getParent() != null) {
								log.getParentFile().mkdirs();
							}
							log.createNewFile();
							FileWriter writer = new FileWriter(log);
							for (EvaluatedCandidate<Tree> cand : result) {
								writer.append("f:" + cand.getFitness() + "  " + cand.getCandidate().toString());
								writer.append("\n");
							}
							writer.close();
						} catch (IOException e) {
							System.err.println("LOST: " + path + File.separator + "generation" + generation++ + ".log");
						}
					}
					return result;
				}

			};

			/**/
			engine.setSingleThreaded(singleThreaded);

			engine.addEvolutionObserver(evolutionLogger);

			//Build a tree with the event classes of our event log at the leafs
			tree = engine.evolve(populationSize, eliteCount, new TargetFitness(targetFitness, evaluator.isNatural()),
					new ProMCancelTerminationCondition(canceller), new GenerationCount(maxIterations), new Stagnation(
							steadyStates, evaluator.isNatural(), true));
			/**/
		}

		//Close the logging file so the buffer is written
		evolutionLogger.closeFile();

		return tree;
	}
	
	public List<Tree> runCandidatos(final String path) {

    //Building a list of operators for tree (crossover and mutation)
    List<EvolutionaryOperator<Tree>> operators = new ArrayList<EvolutionaryOperator<Tree>>(2);
    operators.add(factory); //First introduce a random candidate instead of the worst
    operators.add(new TreeCrossover(crossoverProbability)); //Then let them mate
    operators.add(new TreeMutation(factory)); //Then mutate

    //The evaluator will tell me how good (/bad) my trees are
    TreeEvaluatorAStar evaluator = new TreeEvaluatorAStar(canceller, evolutionLogger, factory.getLog(),
        fitnessWeight, simplicityWeight, generalizationWeight, precisionWeight, coincidenciaWeight);
    evaluator.setCycles(cycles);

    //The future resulting tree
    Tree tree;

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
      System.out.println("ELSE");
      /*-*/
      AbstractEvolutionEngine<Tree> engine = new GenerationalEvolutionEngine<Tree>(factory,
          new EvolutionPipeline<Tree>(operators), evaluator, selectionStrategy, new MersenneTwisterRNG()) {
        private int generation = 0;

        protected List<EvaluatedCandidate<Tree>> evaluatePopulation(List<Tree> population) {
          List<EvaluatedCandidate<Tree>> result = super.evaluatePopulation(population);
          if (path != null) {
            File log = new File(path + File.separator + "generation" + generation++ + ".log");
            try {
              if (log.getParent() != null) {
                log.getParentFile().mkdirs();
              }
              log.createNewFile();
              FileWriter writer = new FileWriter(log);
              for (EvaluatedCandidate<Tree> cand : result) {
                writer.append("f:" + cand.getFitness() + "  " + cand.getCandidate().toString());
                writer.append("\n");
              }
              writer.close();
            } catch (IOException e) {
              System.err.println("LOST: " + path + File.separator + "generation" + generation++ + ".log");
            }
          }
          return result;
        }

      };

      /**/
      engine.setSingleThreaded(singleThreaded);

      engine.addEvolutionObserver(evolutionLogger);

      //Build a tree with the event classes of our event log at the leafs
      tree = engine.evolve(populationSize, eliteCount, new TargetFitness(targetFitness, evaluator.isNatural()),
          new ProMCancelTerminationCondition(canceller), new GenerationCount(maxIterations), new Stagnation(
              steadyStates, evaluator.isNatural(), true));
      /**/
    }

    //Close the logging file so the buffer is written
    evolutionLogger.closeFile();

    return evolutionLogger.getCandidatos();
  }

	/**
	 * GETTERS AND SETTERS
	 */

	//TODO add checks and javadoc explaining purpose!

	public double getTargetFitness() {
		return targetFitness;
	}

	public void setTargetFitness(double targetFitness) {
		this.targetFitness = targetFitness;
	}

	public int getPopulationSize() {
		return populationSize;
	}

	/**
	 * Set the population size. Elite count is automatically set to
	 * max(population size,2)
	 * 
	 * @param populationSize
	 */
	public void setPopulationSize(int populationSize) {
		this.populationSize = populationSize;
		//Make sure the eliteCount is less than population size
		eliteCount = Math.max(populationSize / 10, 2);
	}

	public int getEliteCount() {
		return eliteCount;
	}

	public void setEliteCount(int eliteCount) {
		this.eliteCount = eliteCount;
	}

	public SelectionStrategy<Object> getSelectionStrategy() {
		return selectionStrategy;
	}

	public void setSelectionStrategy(SelectionStrategy<Object> selectionStrategy) {
		this.selectionStrategy = selectionStrategy;
	}

	public boolean isUseIslands() {
		return useIslands;
	}

	public void setUseIslands(boolean useIslands) {
		this.useIslands = useIslands;
	}

	public int getNrIslands() {
		return nrIslands;
	}

	public void setNrIslands(int nrIslands) {
		this.nrIslands = nrIslands;
	}

	public int getEpochLength() {
		return epochLength;
	}

	public void setEpochLength(int epochLength) {
		this.epochLength = epochLength;
	}

	public int getPopulationSizePerIsland() {
		return populationSizePerIsland;
	}

	public void setPopulationSizePerIsland(int populationSizePerIsland) {
		this.populationSizePerIsland = populationSizePerIsland;
	}

	public int getMigrantCount() {
		return migrantCount;
	}

	public void setMigrantCount(int migrantCount) {
		this.migrantCount = migrantCount;
	}

	public Probability getCrossoverProbability() {
		return crossoverProbability;
	}

	public void setCrossoverProbability(Probability crossoverProbability) {
		this.crossoverProbability = crossoverProbability;
	}

	public int getMaxIterations() {
		return maxIterations;
	}

	public void setMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
	}

	public EvolutionLogger<Tree> getEvolutionLogger() {
		return evolutionLogger;
	}

	public void setEvolutionLogger(EvolutionLogger<Tree> evolutionLogger) {
		this.evolutionLogger = evolutionLogger;
	}

	public void setSingleThreaded(boolean singleThreaded) {
		this.singleThreaded = singleThreaded;
	}

	public boolean isSingleThreaded() {
		return singleThreaded;
	}

	public int getSteadyStates() {
		return steadyStates;
	}

	public void setSteadyStates(int steadyStates) {
		this.steadyStates = steadyStates;
	}

	public void setRandomCandidateCount(int randomCandidateProbability) {
		this.randomCandidateCount = randomCandidateProbability;
	}

	public void setFitnessWeight(double fitnessWeight) {
		this.fitnessWeight = fitnessWeight;
	}

	public void setPrecisionWeight(double precisionWeight) {
		this.precisionWeight = precisionWeight;
	}

	public void setGeneralizationWeight(double generalizationWeight) {
		this.generalizationWeight = generalizationWeight;
	}

	public void setSimplicityWeight(double simplicityWeight) {
		this.simplicityWeight = simplicityWeight;
	}

  public List<Secuencia> getCycles() {
    return cycles;
  }

  public void setCycles(List<Secuencia> cycles) {
    this.cycles = cycles;
  }

  public double getCoincidenciaWeight() {
    return coincidenciaWeight;
  }

  public void setCoincidenciaWeight(double coincidenciaWeight) {
    this.coincidenciaWeight = coincidenciaWeight;
  }

}
