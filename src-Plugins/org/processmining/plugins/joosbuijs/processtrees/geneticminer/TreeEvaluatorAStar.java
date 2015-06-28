package org.processmining.plugins.joosbuijs.processtrees.geneticminer;

import gnu.trove.list.TIntList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.astar.algorithm.AStarException;
import org.processmining.plugins.astar.algorithm.AStarThread.Canceller;
import org.processmining.plugins.boudewijn.tree.MutatableTree;
import org.processmining.plugins.boudewijn.tree.Node;
import org.processmining.plugins.boudewijn.tree.Node.Type;
import org.processmining.plugins.boudewijn.tree.Tree;
import org.processmining.plugins.boudewijn.treebasedreplay.AStarAlgorithm;
import org.processmining.plugins.boudewijn.treebasedreplay.AbstractTreeBasedAStar;
import org.processmining.plugins.boudewijn.treebasedreplay.AbstractTreeBasedAStar.PerformanceType;
import org.processmining.plugins.boudewijn.treebasedreplay.AbstractTreeBasedAStar.VerboseLevel;
import org.processmining.plugins.boudewijn.treebasedreplay.BehaviorCounter;
import org.processmining.plugins.boudewijn.treebasedreplay.tokencount.TokenCountAStarWithILP;
import org.processmining.plugins.boudewijn.treebasedreplay.tokencount.TokenCountAStarWithoutILP;
import org.uncommons.watchmaker.framework.FitnessEvaluator;

public class TreeEvaluatorAStar implements FitnessEvaluator<Tree> {
  private boolean showDetails = false;
  private boolean recalculateFitnessAfterReduction = false; // TEMP setting

  private final XLog log;

  private final XEventClassifier standardClassifier;
  private final XLogInfo logInfo;
  private final XEventClasses eventClassesInLog;

  private final HashMap<XEventClass, Integer> logCosts = new HashMap<XEventClass, Integer>();

  private final EvolutionLogger<Tree> evolutionLogger;
  private final AStarAlgorithm algorithm;
  private List<Secuencia> cycles;

  /*
   * Create a canceler
   */
  private final Canceller c;
  private int traceCount;
  private double coincidenciaWeight;
  private double fitnessWeight;
  private double simplicityWeight;
  private double generalizationWeight;
  private double precisionWeight;

  /**
   * @param evolutionLogger
   * @param data
   *          Each row consists of a set of inputs and an expected output (the
   *          last item in the row is the output).
   */
  public TreeEvaluatorAStar(Canceller c, EvolutionLogger<Tree> evolutionLogger,
      XLog eventLog, double fitnessWeight, double simplicityWeight,
      double generalizationWeight, double precisionWeight) {
    this.c = c;
    this.evolutionLogger = evolutionLogger;
    this.log = eventLog;
    this.traceCount = log.size();
    this.fitnessWeight = fitnessWeight;
    this.simplicityWeight = simplicityWeight;
    this.generalizationWeight = generalizationWeight;
    this.precisionWeight = precisionWeight;
    this.coincidenciaWeight = 0;

    standardClassifier = XLogInfoImpl.STANDARD_CLASSIFIER;
    logInfo = XLogInfoFactory.createLogInfo(log, standardClassifier);

    /*
     * Calculate the Move On Log Costs for each EventClass
     */
    eventClassesInLog = logInfo.getEventClasses(standardClassifier);
    for (XEventClass eventClass : eventClassesInLog.getClasses()) {
      logCosts.put(eventClass, 1);
    }
    algorithm = new AStarAlgorithm(eventLog, eventClassesInLog, logCosts);
  }
  
  public TreeEvaluatorAStar(Canceller c, EvolutionLogger<Tree> evolutionLogger,
      XLog eventLog, double fitnessWeight, double simplicityWeight,
      double generalizationWeight, double precisionWeight, double coincidenciaWeight) {
    this.c = c;
    this.evolutionLogger = evolutionLogger;
    this.log = eventLog;
    this.traceCount = log.size();
    this.fitnessWeight = fitnessWeight;
    this.simplicityWeight = simplicityWeight;
    this.generalizationWeight = generalizationWeight;
    this.precisionWeight = precisionWeight;
    this.coincidenciaWeight = coincidenciaWeight;

    standardClassifier = XLogInfoImpl.STANDARD_CLASSIFIER;
    logInfo = XLogInfoFactory.createLogInfo(log, standardClassifier);

    /*
     * Calculate the Move On Log Costs for each EventClass
     */
    eventClassesInLog = logInfo.getEventClasses(standardClassifier);
    for (XEventClass eventClass : eventClassesInLog.getClasses()) {
      logCosts.put(eventClass, 1);
    }
    algorithm = new AStarAlgorithm(eventLog, eventClassesInLog, logCosts);
  }

  public double getFitness(Tree candidate, List<? extends Tree> population) {
    if (candidate.isFitnessSet()) {
      return candidate.getOverallFitness();
    }

    Map<TIntList, TIntSet> marking2modelmove = new HashMap<TIntList, TIntSet>();
    TObjectIntMap<TIntList> marking2visitCount = new TObjectIntHashMap<TIntList>();

    assert candidate.checkTree();
    double replayFitness = 0;
    if (fitnessWeight > 0) {
      replayFitness = calculateReplayFitness(candidate.getRoot(),
          Double.MAX_VALUE
          /* evolutionLogger.getWorstFitnessInLastPopulation() / log.size() */,
          marking2modelmove, marking2visitCount);

      // Correct replay fitness for log size
      replayFitness /= traceCount;
    }
    candidate.setReplayFitness(replayFitness);

    /*
     * Now calculate precision
     */
    double precision = 0;
    if (precisionWeight > 0) {
      precision = calculatePrecision(candidate.getRoot(), marking2modelmove,
          marking2visitCount);
    }
    candidate.setPrecision(precision);

    // System.out.println(candidate.toString());

    // Now reduce the tree where possible
    MutatableTree reducedCandidate = new MutatableTree(candidate);
    reducedCandidate.reduceBehavior();

    if (recalculateFitnessAfterReduction) {
      /*
       * Re-Calculate the fitness
       */
      for (Node n : reducedCandidate.getRoot().getPreorder()) {
        n.setBehavior(new BehaviorCounter());
      }
      marking2modelmove.clear();
      double replayFitnessReduced = calculateReplayFitness(
          reducedCandidate.getRoot(),
          evolutionLogger.getWorstFitnessInLastPopulation() / log.size(),
          marking2modelmove, marking2visitCount);

      /*
       * And check fitness before and after
       */
      if (replayFitness != replayFitnessReduced) {
        System.out.println(candidate.toString());
        System.out.println(reducedCandidate.toString());
        System.err.println("Oops, the replay fitness did change!!!");
        // System.out.println(mutatableCandidate.toString());
        System.err.println("From " + replayFitness + " to "
            + replayFitnessReduced);
      }/**/
    }// recalc

    // Assign the reduced root to the candidate tree
    candidate.setRoot(reducedCandidate.getRoot());

    /*
     * Simplicity
     */
    // The overall fitness for the algorithm is replay cost +
    // (simplicity*logsize)
    // double simplicity =
    // candidate.getRoot().calculateSimplicity(modelCosts(candidate.getRoot()))
    // * log.size();
    // simplicity++; //simplicity CAN be -1 if we calc. -1 simplicity for leafs
    // so add 1
    if (simplicityWeight > 0)
      candidate.setSimplicity(calculateSimplicity(candidate.getRoot()));
    else
      candidate.setSimplicity(0);

    /*
     * Generalization
     */
    if (generalizationWeight > 0)
      candidate.setGeneralization(calculateGeneralization(candidate.getRoot()));
    else
      candidate.setGeneralization(0);

    if (showDetails) {
      DecimalFormat df = new DecimalFormat("#.######");
      System.out.println(candidate.toString());
      System.out.println("f: " + df.format(candidate.getReplayFitness())
          + " p:" + df.format(candidate.getPrecision()) + " s:"
          + df.format(candidate.getSimplicity()) + " g:"
          + df.format(candidate.getGeneralization()) + " gsp:" +df.format(candidate.getCoincidenciaFitness()) );
    }

    // GSP
    double coincidenciaFitness = 0d;
    if (cycles != null && !cycles.isEmpty()) {
      List<Node> nodos = new ArrayList<Node>(candidate.getRoot()
          .getLeafs(false));
      double coincidencias = 0;
      for (int i = 0; i < nodos.size(); i++) {
        coincidencias += evaluarNodo(nodos.get(i));
      }
      coincidenciaFitness = coincidencias / cycles.size();
      candidate.setCoincidenciaFitness(coincidenciaFitness);

    }
    // Remember this tree (not its fitness)

    double overallFitness =
          fitnessWeight * candidate.getReplayFitness() + //
          precisionWeight * candidate.getPrecision() + //
          simplicityWeight * candidate.getSimplicity() + //
          generalizationWeight * candidate.getGeneralization() +
          coincidenciaWeight * coincidenciaFitness;
          
    
    // Normalize
    overallFitness /= (fitnessWeight + precisionWeight + simplicityWeight + generalizationWeight + coincidenciaWeight);
    
    double overallFitnessOld =
        fitnessWeight * candidate.getReplayFitness() + //
        precisionWeight * candidate.getPrecision() + //
        simplicityWeight * candidate.getSimplicity() + //
        generalizationWeight * candidate.getGeneralization();
        
  
  // Normalize
  overallFitnessOld /= (fitnessWeight + precisionWeight + simplicityWeight + generalizationWeight);

    candidate.setOverallFitness(overallFitness);
    candidate.setOverallFitnessOld(overallFitnessOld);
    return overallFitness;
  }
  
  public double evaluarNodo(Node nodo) {
    double valor = 0d;
    for (int i=0; i<  cycles.size();i++) {
      valor +=evaluarSecuencia(nodo, cycles.get(i));
    }
      
    return valor;
  }
  
  public double evaluarSecuencia(Node nodo, Secuencia secuencia) {
    boolean igual = false;
    final int max = secuencia.getNodo().length;
    int actual = 0;
    List<Node> nodos = new ArrayList<Node>(nodo.getLeafs(false));
    for (int i = 0; i < nodos.size(); i++) {
      String valorNodo = nodos.get(i).toString();
      valorNodo = valorNodo.substring(valorNodo.indexOf(":") > 0 ? valorNodo.indexOf(":")+2 : 0,
          valorNodo.indexOf("+") > 0 ? valorNodo.indexOf("+") : valorNodo.length() - 1);
      if (secuencia.getNodo()[actual++].equals(valorNodo)) {
        if (actual == max) {
          igual = true;
          actual = 0;
        }
      }
      else {
        actual = 0;
      }
        
    }
    
    return igual?secuencia.getValor():0d;
  }

  private double calculateReplayFitness(Node root, double stopAt,
      Map<TIntList, TIntSet> marking2modelmove,
      TObjectIntMap<TIntList> marking2visitCount) {
    HashMap<Node, Integer> modelCosts = modelCosts(root);

    /*
     * Instantiate the AStar
     */
    AbstractTreeBasedAStar<?, ?, ?> treeBasedAStar;
    // treeBasedAStar = new TreeBasedAStarWithoutILP(algorithm, c, root,
    // modelCosts,
    // marking2modelmove, marking2visitCount);
    if (root.countNodes() > 15) {
      treeBasedAStar = new TokenCountAStarWithILP(algorithm, c, root,
          modelCosts, marking2modelmove, marking2visitCount, true);
      if (root.countNodes() > 40) {
        treeBasedAStar.setType(PerformanceType.MEMORYEFFICIENT);
      } else {
        treeBasedAStar.setType(PerformanceType.CPUEFFICIENT);
      }
    } else {
      treeBasedAStar = new TokenCountAStarWithoutILP(algorithm, c, root,
          modelCosts, marking2modelmove, marking2visitCount, true);
      treeBasedAStar.setType(PerformanceType.CPUEFFICIENT);
    }

    /*
     * And run and return it
     */
    try {
      return treeBasedAStar.run(VerboseLevel.NONE, stopAt);
    } catch (AStarException e) {
      e.printStackTrace();
      return Double.MAX_VALUE;
    }
  }

  /*-
  private double calculatePrecision(Tree tree) {
  	double precision = 0;

  	////Loop through all the nodes
  	for (Node node : tree.getRoot().getPreorder()) {
  		BehaviorCounter behavior = node.getBehavior();
  		switch (node.getType()) {
  			case LEAF :
  				//Not executing leafs is handled in fitness already
  				break;
  			case SEQ :
  				//Executing a SEQ in the wrong order is handled in fitness already
  				break;
  			case OR :
  				//An OR is easily too generic since it behaves as an XOR or AND.
  				//So, combine the 2 punishments for XOR and AND
  			case XOR :
  				//An XOR is too generic if it favors one of its children over the other
  				precision += Math.abs(behavior.behavedAsL - behavior.behavedAsR);
  				if (node.getType() == Type.XOR) {
  					//Stop the XOR
  					break;
  				}
  				//Let the OR fall-through
  				//$FALL-THROUGH$
  			case AND :
  				//An AND is too generic if it behaves sequential in a preferred direction
  				//Calculate sequential preference
  				precision += Math.abs(behavior.behavedAsSEQLR - behavior.behavedAsSEQRL);
  				//If it did that more often than interleaved
  				break;
  			case LOOP :
  				//If a loop contains a leaf, SEQ or AND then all containing leafs should be executed.
  				//However, a XOR or OR in a loop allow for all kinds of behavior so punish!
  				Type childType = node.getChild(0).getType();
  				if (childType == Type.XOR || childType == Type.OR) {
  					Node loopTau = node.getChild(1);
  					//Punish for the number of re-loops....
  					precision += loopTau.getBehavior().behavedAsL;
  				}
  				break;
  			default :
  				break;
  		}
  	}

  	return precision / log.size();
  }/**/

  protected double calculateSimplicity(Node root) {
    // First, punish for duplicating or missing activities
    // int nrLeafs = root.countLeafNodes();
    // double simplicity = Math.abs(nrLeafs - logInfo.getEventClasses().size());

    // System.out.println("Tree: " + root.toString());

    // First, punish for duplicating or missing activities
    // Get all event classes in the tree
    int duplication = 0;
    Set<XEventClass> classes = new HashSet<XEventClass>();
    for (Node node : root.getNodesOfType(Type.LEAF)) {
      if (classes.contains(node.getClazz())) {
        duplication++;
      } else {
        classes.add(node.getClazz());
      }
    }
    // Now also count the number of missing event classes
    duplication += logInfo.getEventClasses().size() - classes.size();
    // System.out.println("Duplication: " + duplication);
    double simplicity = duplication;

    // Then add a punishment for unbalance which is the trees depth - the ideal
    // depth which is log2(nrLeafs)
    // simplicity += ((root.getDepth() - 1) - Math.ceil((Math.log(nrLeafs) /
    // Math.log(2))));
    // int opSimpl = calculateSimplicityOperator(root);
    // System.out.println("Operator simplicity: " + opSimpl);
    simplicity += calculateSimplicityOperator(root);

    // Now add a punishment for loops
    simplicity += root.countNodesOfType(Type.LOOP);

    // And ORs are doubly worse
    simplicity += 2 * root.countNodesOfType(Type.OR);

    return simplicity;
  }

  protected int calculateSimplicityOperator(Node node) {
    int punishment = 0;

    if (node.isLeaf())
      return punishment;

    if (node.getLeft().getType() != Type.LEAF) {
      if (node.getLeft().getType() != node.getType()) {
        // Punish for local disagreement
        punishment++;
      }
      // Ask for punishment left down
      punishment += calculateSimplicityOperator(node.getLeft());
    }

    if (node.getRight().getType() != Type.LEAF) {
      if (node.getRight().getType() != node.getType()) {
        // Punish for local disagreement
        punishment++;
      }
      // Ask for punishment right down
      punishment += calculateSimplicityOperator(node.getRight());
    }

    return punishment;
  }

  private double calculatePrecision(Node root,
      Map<TIntList, TIntSet> marking2modelmove,
      TObjectIntMap<TIntList> marking2visitCount) {

    // count the number of unused enabled nodes for all markings.
    double numerator = 0;
    double denominator = 0;

    for (Map.Entry<TIntList, TIntSet> entry : marking2modelmove.entrySet()) {
      // in the optimal case, the key equals the value, in which case
      // no generalization has taken place
      // assert entry.getKey().containsAll(entry.getValue());
      int f = marking2visitCount.get(entry.getKey());
      assert f > 0;
      numerator += f * (entry.getKey().size() - entry.getValue().size());
      denominator += f * entry.getKey().size();
    }
    assert denominator > 0;
    if (numerator == 0) {
      return 0;
    }
    // the numerator scales quadratically in the size of the tree.
    return Math.sqrt(numerator);
    // return numerator / denominator;
  }

  private double calculateGeneralization(Node root) {
    double generalization = 0;

    for (Node node : root.getPreorder()) {
      BehaviorCounter beh = node.getBehavior();
      double n = 0; // total executions
      double w = 0; // number of different behavior seen

      switch (node.getType()) {
        case LEAF: // leafs don't generalize
        case SEQ: // neither do sequences?
          continue;
        case OR:
        case XOR:
          // w is whether we have seen the L/R behavior at all (so 2 if both are
          // observed at least once)
          w += (beh.behavedAsL > 0) ? 1 : 0;
          w += (beh.behavedAsR > 0) ? 1 : 0;
          n += beh.behavedAsL + beh.behavedAsR;
          if (node.getType() == Type.XOR)
            break;
          // An OR Is both an XOR and an AND
          //$FALL-THROUGH$
        case AND:
          w += (beh.behavedAsAND > 0) ? 1 : 0;
          w += (beh.behavedAsSEQLR > 0) ? 1 : 0;
          w += (beh.behavedAsSEQRL > 0) ? 1 : 0;
          n += beh.behavedAsAND + beh.behavedAsSEQLR + beh.behavedAsSEQRL;
          break;
        case LOOP:
          w += (beh.behavedAsL > 0) ? 1 : 0;
          w += (beh.behavedAsR > 0) ? 1 : 0;
          n += beh.behavedAsL + beh.behavedAsR;
          break;
      }
      if (w == 5) {
        // we saw the full or, generalization is perfect
        generalization += 0;
      } else if (n < w + 2) {
        // node only used once.
        generalization += 1;
      } else {
        // Returns how certain we are that we won't see new unobserved behavior
        generalization += (w * (w + 1)) / (n * (n - 1));
      }
    }

    // Prevent almost 0 values, 0.0001 is close-enough almost zero
    if (generalization < 0.0001)
      generalization = 0;

    return generalization;
  }

  private HashMap<Node, Integer> modelCosts(Node root) {
    /*
     * Calculate the Move On Model costs for each Node
     */
    HashMap<Node, Integer> modelCosts = new HashMap<Node, Integer>();
    for (Node node : root.getPreorder()) {
      // EACH node... used in simplicity calculations
      // if (node.getClazz() != null) {
      modelCosts.put(node, 1); // Equal costs of 1 per tree node
      // }
    }
    return modelCosts;
  }

  public boolean isNatural() {
    // Good fitness is a low fitness
    return false;
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
