package org.processmining.plugins.joosbuijs.processtrees.geneticminer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.deckfour.xes.in.XesXmlGZIPParser;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.util.XTimer;
import org.processmining.contexts.cli.CLIContext;
import org.processmining.contexts.cli.CLIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.packages.PackageManager;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.Progress;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.plugins.astar.algorithm.AStarThread.Canceller;
import org.processmining.plugins.boudewijn.tree.Node;
import org.processmining.plugins.boudewijn.tree.Tree;
import org.uncommons.maths.random.Probability;

import weka.associations.GeneralizedSequentialPatterns;
import weka.core.Instances;

/**
 * Plug-in definition for the Genetic Process Discovery Algorithm based on
 * Process Trees
 * 
 * @author jbuijs
 * 
 */
public class GeneticMinerPlugin {

  private List<Secuencia> cycles;

  @Plugin(name = "Mine Block-structured Model using a Genetic Algorithm", parameterLabels = { "Event log" }, returnLabels = { "Block Structured Model" }, returnTypes = { Petrinet.class }, userAccessible = true, help = "Mine a block structured process model from an event log using a genetic algorithm")
  @UITopiaVariant(uiLabel = "00JB Mine Block Structured Process Model using genetic algorithm", affiliation = "Eindhoven University of Technology", author = "J.C.A.M.Buijs", email = "j.c.a.m.buijs@tue.nl", pack = "JoosBuijs")
  public Petrinet PTGeneticMinerPlugin(final PluginContext context,
      XLog eventlog) {

    final Progress progress = context.getProgress();
    Canceller canceller = new Canceller() {

      public boolean isCancelled() {
        return progress.isCancelled();
      }
    };

    progress.setCaption("Starting Genetic Algorithm...");
    progress.setMinimum(0);
    // progress.setIndeterminate(true); //set indeterminate for now...

    // eventlog = TreeTest.createInterleavedLog("a", "b", "c", "d", "e", "f",
    // "g");

    GeneticAlgorithm ga = new GeneticAlgorithm(context, canceller, eventlog);

    ga.setPopulationSize(200);
    ga.setTargetFitness(0.05);
    ga.setMaxIterations(100);
    ga.setEliteCount(20);
    ga.setCrossoverProbability(new Probability(0.2d));
    ga.setRandomCandidateCount(20);
    ga.setSteadyStates(100); // disable steady states

    progress.setMaximum(ga.getMaxIterations() + 2);

    progress.inc();
    try {
      PackageManager.getInstance().findOrInstallPackages("LpSolve");
    } catch (Exception e) {
      e.printStackTrace();
    }
    Tree tree = ga.run(null);

    TreeToPNConvertor PNconvertor = new TreeToPNConvertor();
    Petrinet pn = PNconvertor.buildPetrinet(tree);

    // PNconvertor.applyMurata(context, pn);

    return pn;
  }

  @Plugin(name = "Genetic Miner GSP Plugin", parameterLabels = {
      "Event log", "Instances" }, returnLabels = { "Block Structured Model" }, returnTypes = { ArrayList.class }, userAccessible = true, help = "Mine a block structured process model Listando candidatos")
  @UITopiaVariant(uiLabel = "Genetic Miner GSP Plugin", affiliation = "Universidad de La Laguna", author = "D.F.C.S", email = "alu0100463057@ull.edu.es", pack = "Process and Data Mining integrated on BI")
  public List<PetriFitness> PTGeneticMinerPluginCandidatos(
      final PluginContext context, XLog eventlog, final Instances arff) {

    final Progress progress = context.getProgress();

    Canceller canceller = new Canceller() {

      public boolean isCancelled() {
        return progress.isCancelled();
      }
    };
    // Set<String> attr = eventlog.getAttributes().keySet();
    // ArrayList<Attribute> atributos = new ArrayList<Attribute>();
    // Iterator<String> it = attr.iterator();
    // while (it.hasNext()) {
    // String valor = it.next();
    // XAttribute asdf = eventlog.getAttributes().get(valor);
    // atributos.add(new Attribute(valor));
    // }
    // Instances data = new Instances("GSP", atributos, 2);
    //
    GeneralizedSequentialPatterns gsp = new GeneralizedSequentialPatterns();
    try {
      gsp.buildAssociations(arff);
    } catch (Exception e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    readFile(gsp.toString());
    System.out.println(cycles.toString());
    progress.setCaption("Starting Genetic Algorithm...");
    progress.setMinimum(0);
    // progress.setIndeterminate(true); //set indeterminate for now...

    // eventlog = TreeTest.createInterleavedLog("a", "b", "c", "d", "e", "f",
    // "g");

    GeneticAlgorithm ga = new GeneticAlgorithm(context, canceller, eventlog);

    ga.setPopulationSize(2000);
    ga.setTargetFitness(0.05);
    ga.setMaxIterations(10);
    ga.setEliteCount(5);
    ga.setCrossoverProbability(new Probability(0.2d));
    ga.setRandomCandidateCount(20);
    ga.setSteadyStates(100); // disable steady states

    progress.setMaximum(ga.getMaxIterations() + 2);
    ga.setCycles(cycles);

    progress.inc();
    try {
      PackageManager.getInstance().findOrInstallPackages("LpSolve");
    } catch (Exception e) {
      e.printStackTrace();
    }
    List<Tree> tree = ga.runCandidatos(null,false);

    TreeToPNConvertor PNconvertor = new TreeToPNConvertor();
    List<PetriFitness> pn = new ArrayList<PetriFitness>();
    for (int i = 0; i < tree.size(); i++) {
      PetriFitness petri = new PetriFitness(PNconvertor.buildPetrinet(tree.get(i)), tree.get(i).getOverallFitnessOld(), tree.get(i).getOverallFitness());
      pn.add(petri);
    }

    // PNconvertor.applyMurata(context, pn);

    return pn;
  }
  
  @Plugin(name = "Genetic Miner GSP Plugin Inverso", parameterLabels = {
      "Event log", "Instances" }, returnLabels = { "Block Structured Model" }, returnTypes = { ArrayList.class }, userAccessible = true, help = "Mine a block structured process model Listando candidatos")
  @UITopiaVariant(uiLabel = "Genetic Miner GSP Plugin Inverso", affiliation = "Universidad de La Laguna", author = "D.F.C.S", email = "alu0100463057@ull.edu.es", pack = "Process and Data Mining integrated on BI")
  public List<PetriFitness> PTGeneticMinerPluginCandidatosInverso(
      final PluginContext context, XLog eventlog, final Instances arff) {

    final Progress progress = context.getProgress();

    Canceller canceller = new Canceller() {

      public boolean isCancelled() {
        return progress.isCancelled();
      }
    };
    // Set<String> attr = eventlog.getAttributes().keySet();
    // ArrayList<Attribute> atributos = new ArrayList<Attribute>();
    // Iterator<String> it = attr.iterator();
    // while (it.hasNext()) {
    // String valor = it.next();
    // XAttribute asdf = eventlog.getAttributes().get(valor);
    // atributos.add(new Attribute(valor));
    // }
    // Instances data = new Instances("GSP", atributos, 2);
    //
    GeneralizedSequentialPatterns gsp = new GeneralizedSequentialPatterns();
    try {
      gsp.buildAssociations(arff);
    } catch (Exception e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    readFile(gsp.toString());
    System.out.println(cycles.toString());
    progress.setCaption("Starting Genetic Algorithm...");
    progress.setMinimum(0);
    // progress.setIndeterminate(true); //set indeterminate for now...

    // eventlog = TreeTest.createInterleavedLog("a", "b", "c", "d", "e", "f",
    // "g");

    GeneticAlgorithm ga = new GeneticAlgorithm(context, canceller, eventlog);

    ga.setPopulationSize(200);
    ga.setTargetFitness(0.05);
    ga.setMaxIterations(10);
    ga.setEliteCount(5);
    ga.setCrossoverProbability(new Probability(0.2d));
    ga.setRandomCandidateCount(20);
    ga.setSteadyStates(100); // disable steady states

    progress.setMaximum(ga.getMaxIterations() + 2);
    ga.setCycles(cycles);

    progress.inc();
    try {
      PackageManager.getInstance().findOrInstallPackages("LpSolve");
    } catch (Exception e) {
      e.printStackTrace();
    }
    List<Tree> tree = ga.runCandidatos(null,true);

    TreeToPNConvertor PNconvertor = new TreeToPNConvertor();
    List<PetriFitness> pn = new ArrayList<PetriFitness>();
    for (int i = 0; i < tree.size(); i++) {
      PetriFitness petri = new PetriFitness(PNconvertor.buildPetrinet(tree.get(i)), tree.get(i).getOverallFitnessOld(), tree.get(i).getOverallFitness());
      pn.add(petri);
    }

    // PNconvertor.applyMurata(context, pn);

    return pn;
  }
  

  protected void readFile(String archivo) {

    boolean primera = false;
    boolean segunda = false;
    System.out.println(archivo);
    List<Secuencia> ciclo = new ArrayList<Secuencia>();
    String[] lines = archivo.split("\n");
    String linea;
    int numSecuencia = 1;
    for (int i = 0; i < lines.length; i++) {

      linea = lines[i];

      if (linea.contains("-sequences") && !segunda) {
        i++;
      } else if (linea.contains("<{")) {
        Secuencia secu =  new Secuencia(linea.substring(linea.indexOf("<")+1 , linea.indexOf(">")),linea.substring(linea.indexOf("(")+1, linea.indexOf(")")));
        ciclo.add(secu);
      }
    }
    setCycles(ciclo);
  }

  @Plugin(name = "Mine Block-structured Model using a Genetic Algorithm", parameterLabels = { "Event log" }, returnLabels = { "Block Structured Model" }, returnTypes = { Petrinet.class }, userAccessible = true, help = "Mine a block structured process model from an event log using a genetic algorithm")
  @UITopiaVariant(uiLabel = "00JB TRIAL Mine Block Structured Process Model using genetic algorithm", affiliation = "Eindhoven University of Technology", author = "J.C.A.M.Buijs", email = "j.c.a.m.buijs@tue.nl", pack = "JoosBuijs")
  public Petrinet PTGeneticMinerPluginTrials(final PluginContext context,
      XLog eventlog) {
    final Progress progress = context.getProgress();
    Canceller canceller = new Canceller() {

      public boolean isCancelled() {
        return progress.isCancelled();
      }
    };
    progress.setCaption("Starting Genetic Algorithm TRIALS...");
    progress.setMinimum(0);
    progress.setIndeterminate(true); // set indeterminate for now...

    /*
     * SETTING UP LOG FILE
     */
    Calendar cal = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    SimpleDateFormat sdfFile = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");
    SimpleDateFormat sdfDuration = new SimpleDateFormat("HH:mm:ss.SSS");

    String filename = "./stats/stats_trials" + sdfFile.format(cal.getTime())
        + ".csv";
    File statsFile = new File(filename);

    // statsFile.createNewFile();
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
    out.println("trial; generation; fitness; tree; duration");
    out.flush();
    // out.close();

    /*
     * FINISHED SETTING UP THE LOG FILE
     */

    Petrinet pn = null; // place holder for the last discovered PN
    for (int i = 0; i < 100; i++) {
      // eventlog = TreeTest.createInterleavedLog("a", "b", "c", "d", "e", "f",
      // "g");

      // Lets time our duration
      XTimer mappingTimer = new XTimer();
      mappingTimer.start();
      cal = Calendar.getInstance();
      String message = "Starting trial #" + i + " at "
          + sdf.format(cal.getTime());
      System.out.println(message);

      /*
       * MAIN CALL
       */
      GeneticAlgorithm ga = new GeneticAlgorithm(context, canceller, eventlog);
      ga.setPopulationSize(200);
      ga.setSteadyStates(ga.getMaxIterations()); // no steady state stop this
                                                 // time

      try {
        PackageManager.getInstance().findOrInstallPackages("LpSolve");
      } catch (Exception e) {
        e.printStackTrace();
      }
      Tree tree = ga.run(null);

      TreeToPNConvertor PNconvertor = new TreeToPNConvertor();
      pn = PNconvertor.buildPetrinet(tree);

      /*
       * LOG THIS TRIAL
       */
      mappingTimer.stop();

      cal = Calendar.getInstance();
      System.out.println("Ending trial #" + i + " at "
          + sdf.format(cal.getTime()) + " after " + mappingTimer.getDuration());

      EvolutionLogger<Tree> evolutionLogger = ga.getEvolutionLogger();
      out.println(i + "; " + evolutionLogger.getNrGenerations() + "; "
          + df.format(tree.getReplayFitness()) + "; " + tree.toString() + "; "
          + sdfDuration.format(new Date(mappingTimer.getDuration())));
      out.flush();
    }

    out.close();
    // PNconvertor.applyMurata(context, pn);

    return pn;
  }

  @Plugin(name = "Mine Block-structured Model using a Genetic Algorithm", parameterLabels = { "Event log" }, returnLabels = { "Block Structured Model" }, returnTypes = { Petrinet.class }, userAccessible = true, help = "Mine a block structured process model from an event log using a genetic algorithm")
  @UITopiaVariant(uiLabel = "00JB Block Miner - TEST plugin (Tree to PN)", affiliation = "Eindhoven University of Technology", author = "J.C.A.M.Buijs", email = "j.c.a.m.buijs@tue.nl", pack = "JoosBuijs")
  public Petrinet PTGeneticMinerPluginTEST(final PluginContext context,
      XLog eventlog) {
    XLogInfo logInfo = XLogInfoFactory.createLogInfo(eventlog,
        XLogInfoImpl.STANDARD_CLASSIFIER);

    Node node = Node
        .fromString(
            // a12 Tree:
            "SEQ( LEAF: SSSTTTAAARRRTTT+complete , SEQ( SEQ( SEQ( LEAF: A+complete , XOR( SEQ( LEAF: B+complete , SEQ( AND( SEQ( LEAF: D+complete , LEAF: E+complete ) , LEAF: F+complete ) , LEAF: J+complete ) ) , SEQ( LEAF: C+complete , SEQ( XOR( LEAF: G+complete , SEQ( LEAF: H+complete , LEAF: I+complete ) ) , LEAF: K+complete ) ) ) ) , LEAF: L+complete ) , LEAF: EEENNNDDD+complete ) )",
            logInfo.getEventClasses());
    // Extremely large tree (20 OR nodes which result in ~4 million enabled
    // transitions. Test on SEQ6
    // LOOP( SEQ( OR( LEAF: A+complete , OR( OR( LEAF: C+complete , LEAF:
    // D+complete ) , LEAF: D+complete ) ) , OR( OR( AND( LEAF: B+complete , OR(
    // OR( SEQ( OR( LEAF: A+complete , XOR( OR( LEAF: C+complete , LEAF:
    // D+complete ) , LEAF: D+complete ) ) , OR( OR( AND( LEAF: B+complete , OR(
    // OR( LEAF: A+complete , OR( LEAF: F+complete , LEAF: D+complete ) ) ,
    // LEAF: E+complete ) ) , OR( LEAF: F+complete , LEAF: D+complete ) ) ,
    // LEAF: E+complete ) ) , OR( OR( OR( LEAF: F+complete , LEAF: A+complete )
    // , LEAF: D+complete ) , LEAF: E+complete ) ) , LEAF: E+complete ) ) , OR(
    // LEAF: F+complete , LEAF: D+complete ) ) , LEAF: E+complete ) ) , LEAF:
    // EXIT )

    /*-*/
    EvolutionLogger<Tree> evolutionLogger = new EvolutionLogger<Tree>(context);
    final Progress progress = context.getProgress();
    Canceller canceller = new Canceller() {

      public boolean isCancelled() {
        return progress.isCancelled();
      }
    };
    try {
      PackageManager.getInstance().findOrInstallPackages("LpSolve");
    } catch (Exception e) {
      e.printStackTrace();
    }

    TreeEvaluatorAStar eval = new TreeEvaluatorAStar(canceller,
        evolutionLogger, eventlog, 1.0, 1.0, 1.0, 1.0);

    TreeFactory fact = new TreeFactory(eventlog, 1);

    /*-* /
    Tree tree = null;
    for (int i = 0; i < 10; i++) {
    	Tree tempTree = fact.generateRandomCandidate(new MersenneTwisterRNG());

    	eval.calculateSimplicity(tempTree.getRoot());
    	tree = tempTree;
    }/**/

    Tree tree = new Tree(node);
    eval.getFitness(tree, null);

    // double fitness = eval.getFitness(tree, null);
    /**/

    System.out.println(node.printBehaviorRecursive());

    TreeToPNConvertor PNconvertor = new TreeToPNConvertor();
    Petrinet pn = PNconvertor.buildPetrinet(node);

    PNconvertor.applyMurata(context, pn);

    return pn;
  }

  /**
   * 
   * @param args
   */
  public static void main(String[] args) {
    int i = 0;
    String eventlogFile = args[i++];

    XesXmlGZIPParser logparser = new XesXmlGZIPParser();
    XLog eventlog;
    try {
      eventlog = logparser.parse(new File(eventlogFile)).get(0);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

    double targetFitness = Double.parseDouble(args[i++]);
    int polulationSize = Integer.parseInt(args[i++]);
    int eliteCount = Integer.parseInt(args[i++]);
    Probability crossoverProbability = new Probability(
        Double.parseDouble(args[i++]));
    int randomCandidateCount = Integer.parseInt(args[i++]);
    int maxIterations = Integer.parseInt(args[i++]);
    int steadyStates = Math.max(Integer.parseInt(args[i++]), 2);
    double wf = Double.parseDouble(args[i++]);
    double wp = Double.parseDouble(args[i++]);
    double wg = Double.parseDouble(args[i++]);
    double ws = Double.parseDouble(args[i++]);

    String path = args[i++];
    CLIContext global = new CLIContext();
    CLIPluginContext clic = new CLIPluginContext(global, "CLI ngrid context");

    Canceller canceller = new Canceller() {

      public boolean isCancelled() {
        return false;
      }
    };

    GeneticAlgorithm ga = new GeneticAlgorithm(clic, canceller, eventlog);

    ga.setPopulationSize(polulationSize);
    ga.setTargetFitness(targetFitness); // allow for a very small deviation from
                                        // perfection
    ga.setEliteCount(eliteCount);
    ga.setCrossoverProbability(crossoverProbability);
    ga.setRandomCandidateCount(randomCandidateCount);
    ga.setMaxIterations(maxIterations);
    ga.setSteadyStates(steadyStates);
    ga.setFitnessWeight(wf);
    ga.setPrecisionWeight(wp);
    ga.setGeneralizationWeight(wg);
    ga.setSimplicityWeight(ws);

    Tree tree = ga.run(path);

    // System.out.println(tree.toString());
  }
  
  public List<Secuencia> getCycles() {
    return cycles;
  }

  public void setCycles(List<Secuencia> cycles) {
    this.cycles = cycles;
  }
}
