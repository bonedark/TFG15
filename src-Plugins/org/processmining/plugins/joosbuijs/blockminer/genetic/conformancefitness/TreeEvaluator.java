// =============================================================================
// Copyright 2006-2010 Daniel W. Dyer
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// =============================================================================
package org.processmining.plugins.joosbuijs.blockminer.genetic.conformancefitness;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.util.XTimer;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.PluginContextID;
import org.processmining.framework.plugin.PluginExecutionResult;
import org.processmining.framework.plugin.PluginParameterBinding;
import org.processmining.framework.plugin.events.Logger;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.astar.petrinet.PetrinetReplayerWithoutILP;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.joosbuijs.blockminer.genetic.conformancefitness.FunctionNode.FUNCTIONTYPE;
import org.processmining.plugins.petrinet.replayer.algorithms.IPNReplayAlgorithm;
import org.processmining.plugins.petrinet.replayer.algorithms.IPNReplayParameter;
import org.processmining.plugins.petrinet.replayer.algorithms.behavapp.BehavAppNaiveAlg;
import org.processmining.plugins.petrinet.replayer.algorithms.behavapp.BehavAppParam;
import org.processmining.plugins.petrinet.replayer.algorithms.costbasedcomplete.CostBasedCompleteParam;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;
import org.processmining.plugins.replayer.util.StepTypes;
import org.uncommons.watchmaker.framework.FitnessEvaluator;

/**
 * Fitness function for the genetic discovery of block structured process
 * models.
 * 
 * @author jbuijs
 */
public class TreeEvaluator implements FitnessEvaluator<Tree> {
	private boolean showDetails = false;

	private final XLog log;
	private final PluginContext context;

	private final HashMap<XEventClass, Integer> logCosts;
	//private final HashMap<XEventClass, Integer> modelCosts;
	//	private final int modelCost = 5;
	private final XEventClassifier standardClassifier;
	private final XLogInfo logInfo;
	private XEventClass dummyEventClass = new XEventClass("DUMMY", -1);
	private XEventClasses eventClassesInLog;

	private HashMap<Class<IPNReplayAlgorithm>, PluginParameterBinding> localPluginList;

	/**
	 * @param data
	 *            Each row is consists of a set of inputs and an expected output
	 *            (the last item in the row is the output).
	 */
	public TreeEvaluator(PluginContext context, XLog eventLog) {
		this.log = eventLog;
		this.context = context;
		this.localPluginList = new HashMap<Class<IPNReplayAlgorithm>, PluginParameterBinding>();

		standardClassifier = XLogInfoImpl.STANDARD_CLASSIFIER;
		logInfo = XLogInfoFactory.createLogInfo(log, standardClassifier);
		eventClassesInLog = logInfo.getEventClasses(standardClassifier);

		logCosts = new HashMap<XEventClass, Integer>();
		//modelCosts = new HashMap<XEventClass, Integer>();
		for (XEventClass eventClass : eventClassesInLog.getClasses()) {
			logCosts.put(eventClass, eventClass.size());
			//modelCosts.put(eventClass, eventClass.size());
		}
		logCosts.put(dummyEventClass, 0);
		//modelCosts.put(dummyEventClass,0);
	}

	/**
	 * The fitness value of the candidate is calculated combining different
	 * notions of fitness: First there is the replay fitness of Arya. Second, we
	 * punish for leaving out event classes (which might be cheating to get a
	 * fitness of 1 according to the replay fitness), or duplication of event
	 * classes (this last one could be covered by precision in the future).
	 * Third (NOT IMPLEMENTED YET) we should take precision into account to
	 * prevent the flower equivalent block structured net.
	 * 
	 * @param candidate
	 *            The program tree to evaluate.
	 * @param population
	 *            Ignored by this implementation.
	 * @return The fitness score for the specified candidate.
	 */
	public double getFitness(Tree candidate, List<? extends Tree> population) {
		//System.out.println(" Getting Fitness...");
		//FIXME keep a (clean) list of currently running child contexts
		//FIXME for the progress listener cancellation: cycle through this list and send cancellation messages
		//FIXME prevent serialization (e.g. serialize later) to drastically increase performance when serialization is on
		final PluginContext childContext = context.createChildContext("replay");
		context.getPluginLifeCycleEventListeners().firePluginCreated(childContext);
		childContext.getLoggingListeners().add(new Logger() {

			public void log(String message, PluginContextID contextID, MessageLevel messageLevel) {
				// ignore
			}

			public void log(Throwable t, PluginContextID contextID) {
				//ignore
				//context.log(t);
			}

		});

		//Catch empty trees...
		if (candidate.getLeafs().size() == 0) {
			candidate.setFitness(new Fitness());
			return 0;
		}

		//If the tree still knows how fit it was nothing has changed and we don't need to recalculate it
		if (candidate.getFitness() != null && candidate.getFitness().getFitness() > 0) {
			if (showDetails)
				System.out.println(" reusing fitness value of " + candidate.getFitness().getFitness());
			return candidate.getFitness().getFitness();
		}

		//We need a Petri Net to calculate replay fitness
		TreeToPNConvertor convertor = new TreeToPNConvertor();
		Petrinet net = convertor.buildPetrinet(candidate);

		//We don't have an initial marking but we can create one
		Marking initMarking = new Marking();
		Marking finalMarking = new Marking();
		for (Place place : net.getPlaces()) {
			//TODO improve by looking at the number of tokens?
			if (net.getInEdges(place).isEmpty()) {
				initMarking.add(place);
				//break;?
			}
			if (net.getOutEdges(place).isEmpty()) {
				finalMarking.add(place);
			}
		}

		//Build a LogPetrinetConnection
		//And a mapping between the log and the Petri Net and the move-on-model costs
		TransEvClassMapping mapping = new TransEvClassMapping(standardClassifier, dummyEventClass);
		HashMap<Transition, Integer> modelCosts = new HashMap<Transition, Integer>();
		//HashSet<Pair<Transition, XEventClass>>();
		for (Transition trans : net.getTransitions()) {
			//For each transition, find the event class with the exact same name
			//TODO improve getByIdentity?
			XEventClass eventClass = eventClassesInLog.getByIdentity(trans.getLabel());
			mapping.put(trans, eventClass);
			if (logCosts.containsKey(eventClass))
				modelCosts.put(trans, logCosts.get(eventClass));
			else {
				modelCosts.put(trans, 0);
			}
		}

		//And the parameters for the CostBasedCompletePNReplayAlgorithm
		//First the costs per transition for moving on model only and on log only
		/*-*/
		//		CostBasedCompletePNReplayAlgorithmStubbornRPST selectedFitnessAlg = new CostBasedCompletePNReplayAlgorithmStubbornRPST();
		IPNReplayAlgorithm selectedFitnessAlg = new PetrinetReplayerWithoutILP(); //OPTIMAL
		//IPNReplayAlgorithm selectedFitnessAlg = new CostBasedCompletePruneAlg(); //DONT USE

		//		CostBasedCompletePNReplayAlgorithmStubbornRPSTParam parametersFitnessAlg = new CostBasedCompletePNReplayAlgorithmStubbornRPSTParam();

		//CostBasedCompletePNReplayAlgorithmPruneParam parametersFitnessAlg = new ParametersWithoutILP();
		CostBasedCompleteParam parametersFitnessAlg = new CostBasedCompleteParam(logCosts, modelCosts);
		parametersFitnessAlg.setFinalMarkings(new Marking[] { finalMarking });
		parametersFitnessAlg.setInitialMarking(initMarking);
		//parametersFitnessAlg.setMapEvClass2Cost(logCosts); //move on log
		//parametersFitnessAlg.setMapTrans2Cost(modelCosts);
		parametersFitnessAlg.setMaxNumOfStates(50000);
		parametersFitnessAlg.setCreateConn(false);
		parametersFitnessAlg.setGUIMode(false);

		List<String> keyListFitness = new ArrayList<String>();
		keyListFitness.add(PNRepResult.MOVELOGFITNESS);
		keyListFitness.add(PNRepResult.MOVEMODELFITNESS);
		/**/

		/*
		 * We won't call the plugin through the framework since 60% of the time
		 * will be spend handling connections. We deeply call the required
		 * function.
		 */
		//PNRepResult fitnessRepResult = callReplayPlugin(net, log, mapping, selectedFitnessAlg, parametersFitnessAlg);
		XTimer fTimer1 = new XTimer();
		fTimer1.start();
		PNRepResult fitnessRepResult = selectedFitnessAlg.replayLog(context, net, log, mapping, parametersFitnessAlg);
		fTimer1.stop();
		/*-*/
		if (fTimer1.getDuration() > (60 * 1000)) {
			System.out.println("Fitness calculation for the following tree took more than 1 min.:"
					+ candidate.toString());
		}/**/

		double replayFitness = getFitnessFromReplayResult(fitnessRepResult, keyListFitness);

		if (0 > replayFitness || replayFitness > 1) {
			replayFitness = 0;
			System.out.println("###FOUND ILLEGAL REPLAY FITNESS VALUE, correcting to 0");
		}

		//We can ask the replayer plugin using the `behavioral replay algorithm' 
		//and ask for 'BehavioralAppropriateness', see the 4 parameters in that class

		/*
		 * We will now ask for behavioral appropriateness
		 */

		/*- */
		//IPNLogReplayAlgorithm behRepAlg = new BehavioralReplayAlgorithm();
		//IPNReplayAlgorithm behRepAlg = new BehavAppPruneAlg(); //DONT USE
		IPNReplayAlgorithm behRepAlg = new BehavAppNaiveAlg(); //OPTIMAL

		//And the parameters
		BehavAppParam behRepParam = new BehavAppParam();
		behRepParam.setFinalMarkings(new Marking[] { finalMarking });
		behRepParam.setInitialMarking(initMarking);
		//behRepParam.setLog(log);
		behRepParam.setMaxNumStates(50000);
		behRepParam.setUseLogWeight(true);
		behRepParam.setxEventClassWeightMap(new HashMap<XEventClass, Integer>());
		behRepParam.setCreateConn(false);
		behRepParam.setGUIMode(false);

		List<String> keyListBehavior = new ArrayList<String>();
		keyListBehavior.add(PNRepResult.BEHAVIORAPPROPRIATENESS);

		//Again, we directly call the function instead of through the framework
		//TODO this value can be 0.0 ?!?!?! (verify with Arya if this is desired)
		//PNRepResult behRepResult = callReplayPlugin(net, log, mapping, behRepAlg, behRepParam);
		XTimer fTimer2 = new XTimer();
		fTimer2.start();
		PNRepResult behRepResult = behRepAlg.replayLog(context, net, log, mapping, behRepParam);
		fTimer2.stop();
		/*-*/
		if (fTimer2.getDuration() > (60 * 1000)) {
			System.out.println("BehApp calculation for the following tree took more than 1 min.:"
					+ candidate.toString());
		}/**/

		double behavioralFitness = getFitnessFromReplayResult(behRepResult, keyListBehavior);

		if (0 > behavioralFitness || behavioralFitness > 1) {
			replayFitness = 0;
			System.out.println("###FOUND ILLEGAL BehApp FITNESS VALUE, correcting to 0");
		}

		/**/

		/*
		 * Now calculate some additional fitness metrics since we probably want
		 * to take several aspects into account
		 */

		//The # of events from the EL that appear in the tree (ignoring duplicates)
		int coveredEvents = 0;
		HashSet<XEventClass> classesInTree = candidate.getEventClasses();

		for (XEventClass eventClass : logInfo.getEventClasses().getClasses()) {
			if (classesInTree.contains(eventClass)) {
				coveredEvents += eventClass.size();
			}
		}

		double coverage;
		if (coveredEvents == 0)
			coverage = 0;
		else
			coverage = ((double) coveredEvents / logInfo.getNumberOfEvents());

		//Calculate the size of the tree in relation to the expected size
		double treeSizeFactor = candidate.countNodes() / ((eventClassesInLog.size() * 2) - 1);
		if (treeSizeFactor > 1)
			treeSizeFactor = 1 / treeSizeFactor;

		//double structuralFitness = calculateStructuralFitness(candidate);

		/*
		 * Now create a harmonic mean to get a balanced average
		 */

		//overall fitness including behavioral and fitness
		//double fitness = 2 * ((behavioralFitness*replayFitness)/(behavioralFitness+replayFitness));
		//overall fitness including behavioral and fitness and event class coverage
		List<Double> fitnessValues = new ArrayList<Double>();
		fitnessValues.add(replayFitness);
		fitnessValues.add(behavioralFitness);
		//fitnessValues.add(structuralFitness);
		//fitnessValues.add(punishment);

		double fitness = harmonicMean(fitnessValues);
		//double fitness = (replayFitness + behavioralFitness) / 2;
		//		double fitness = (replayFitness + structuralFitness) / 2;

		/*-* /
		if (fitness >= 1 || fitness < 0)
			System.out.println("Strange fitness value!!!");
			/**/

		//double behavioralFitness = 0;

		//Store the fitness in the root
		Fitness fitnessObject = new Fitness(fitness, replayFitness, behavioralFitness, coverage);
		candidate.setFitness(fitnessObject);

		//Output fitness and candidate for debugging
		//DecimalFormat df = new DecimalFormat("#.######");
		//System.out.println("   " + df.format(fitness) + " " + candidate.toString());

		/*-*/
		DecimalFormat df = new DecimalFormat("#.####");
		if (showDetails) {
			//			logger.debug(candidate.toString() + " " + df.format(replayFitness) + "*"
			//					+ df.format(behavioralFitness) + "(" + df.format(fitness) + ")");
			System.out.println(candidate.toString() + " " + df.format(replayFitness) + "*"
					+ df.format(behavioralFitness) + "(" + df.format(fitness) + ")");/**/
		}

		return fitness;
	}

	/**
	 * Asks the ProM framework for the replay plug-ins that we need only once,
	 * otherwise returns a local instance of the replay plugin
	 */
	@SuppressWarnings("unchecked")
	private PluginParameterBinding getReplayPluginFor(Petrinet net, XLog log, TransEvClassMapping mapping,
			IPNReplayAlgorithm selectedAlg, IPNReplayParameter parameters) {
		if (localPluginList.containsKey(selectedAlg.getClass()))
			return localPluginList.get(selectedAlg.getClass());

		//FIXME don't assume that there is a plugin (and what if there are more)
		//We don't use an init marking!
		Set<PluginParameterBinding> plugins = context.getPluginManager().getPluginsAcceptingOrdered(
				PluginContext.class, false, net.getClass(), log.getClass(), mapping.getClass(), selectedAlg.getClass(),
				parameters.getClass());

		//We need the second... (could be better but its a quick fix anywho) !!!!!!!!!!!
		Iterator<PluginParameterBinding> pIt = plugins.iterator();
		//pIt.next();
		PluginParameterBinding plugin = pIt.next();
		localPluginList.put((Class<IPNReplayAlgorithm>) selectedAlg.getClass(), plugin);

		return plugin;
	}

	/**
	 * Calls the provided selected algorithm in the Replayer package and
	 * calculates the requested fitness value
	 * 
	 * @param net
	 * @param mapping
	 * @param initMarking
	 * @param marking
	 * @param selectedAlg
	 * @param parameters
	 * @param fitness
	 * @return
	 */
	private PNRepResult callReplayPlugin(Petrinet net, XLog log, TransEvClassMapping mapping,
			IPNReplayAlgorithm selectedAlg, IPNReplayParameter parameters) {
		PNRepResult replayResult = null;
		//logReplayResult = context.tryToFindOrConstructFirstObject(PNRepResult.class, null, "", net, log,  mapping, initMarking, selectedAlg);

		/*-*/
		//FIXME keep a (clean) list of currently running child contexts
		//FIXME for the progress listener cancellation: cycle through this list and send cancellation messages
		//FIXME prevent serialization (e.g. serialize later) to drastically increase performance when serialization is on
		final PluginContext child = context.createChildContext("replay");
		context.getPluginLifeCycleEventListeners().firePluginCreated(child);
		child.getLoggingListeners().add(new Logger() {

			public void log(String message, PluginContextID contextID, MessageLevel messageLevel) {
				// ignore
			}

			public void log(Throwable t, PluginContextID contextID) {
				//ignore
				//context.log(t);
			}

		});
		/**/

		/*-
		//FIXME don't assume that there is a plugin (and what if there are more)
		//We don't use an init marking!
		Set<PluginParameterBinding> plugins = context.getPluginManager().getPluginsAcceptingOrdered(
				PluginContext.class, false, net.getClass(), log.getClass(), mapping.getClass(), selectedAlg.getClass(),
				parameters.getClass());

		//We need the second... (could be better but its a quick fix anywho) !!!!!!!!!!!
		Iterator<PluginParameterBinding> pIt = plugins.iterator();
		//pIt.next();
		PluginParameterBinding plugin = pIt.next();
		/**/

		PluginParameterBinding plugin = getReplayPluginFor(net, log, mapping, selectedAlg, parameters);

		PluginExecutionResult result = null;
		try {
			result = plugin.invoke(child, net, log, mapping, selectedAlg, parameters);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		//FIXME correctly handle exceptions
		try {
			result.synchronize();
			replayResult = result.getResult(0);
		} catch (CancellationException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
			//Lets see this candidate!
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return replayResult;
	}

	/**
	 * Extracts the fitness value from the replayResult object returned by the
	 * Replayer plugin
	 * 
	 * @param replayResult
	 * @param keys
	 * @return
	 */
	private double getFitnessFromReplayResult(PNRepResult replayResult, List<String> keys) {
		double returnValue = 0;

		if (replayResult != null) {
			List<Double> interimValues = new ArrayList<Double>();
			for (String key : keys) {
				interimValues.add(getFitnessFromReplayResult(replayResult, key));
			}
			returnValue = harmonicMean(interimValues);
		} else {
			//There was an error during replay which might indicate something bad for the PN, just return a low fitness value...
			//FIXME what could we do here? returning a 'random' fitness value seems bad
			returnValue = 0.25d;
		}

		//make sure the return value is positive (we encountered a negative behavioral appropriateness value)
		if (returnValue < 0) {
			//FIXME What should the new value be??? Small I guess
			returnValue = 0.25d;
		}

		return returnValue;
	}

	/**
	 * Helper function for getting the fitness value
	 * 
	 * @param replayResult
	 * @param key
	 * @return
	 */
	private double getFitnessFromReplayResult(PNRepResult replayResult, String key) {
		double totalFitness = 0.0000;
		int numCases = 0;
		for (SyncReplayResult repRes : replayResult) {
			int size = repRes.getTraceIndex().size();
			if (repRes.getInfo().containsKey(key)) {
				totalFitness += (size * repRes.getInfo().get(key));
				numCases += size;
			}
		}
		//We can now set the replay fitness value
		return totalFitness / numCases;
	}

	//UNUSED!!!
	/**
	 * Calculates the hotspots from the given replay result
	 * 
	 * @param replayResult
	 * @return
	 */
	private HashMap<Node, HotspotInfo> findHotspots(PNRepResult replayResult, Tree tree) {

		//		Fitness fitness = tree.getFitness();

		HashMap<Node, HotspotInfo> info = new HashMap<Node, HotspotInfo>();
		//Populate hotspot info

		for (SyncReplayResult repRes : replayResult) {
			//The node instances in the order in which the algorithm chose to execute them 
			List<Object> nodeInstances = repRes.getNodeInstance();
			//The step types taken for the corresponding node instance
			List<StepTypes> stepTypes = repRes.getStepTypes();
			//# traces in this trace cluster which this repRes is all about
			int size = repRes.getTraceIndex().size();
			/*
			 * Now walk through the steps and node instances (which are synced)
			 * to see which activity and step type provide the most cost
			 * effective sequence to adjust our candidate to
			 */
			for (int i = 0; i < stepTypes.size() && i < nodeInstances.size(); i++) {
				//Get the current node instance, which is an event class
				Object currentNodeInstance = nodeInstances.get(i);
				//And get the corresponding nodes
				List<EventClassNode> leafs = tree.getLeafsOfEventClass((XEventClass) currentNodeInstance);
				//Get the recorded hotspot info from our info map
				HotspotInfo hotspotInfo = info.get(currentNodeInstance);
				//Catch the null case
				if (hotspotInfo == null)
					hotspotInfo = new HotspotInfo();

				//Now add more info to the info of this hotspot
				StepTypes currentStepType = stepTypes.get(i);
				if (currentStepType != StepTypes.MINVI) {
					hotspotInfo.addStepTypeCount(currentStepType, size);
				}
			}

		}

		return info;

		/*-* /
		//int occurrences[][] = new int[StepTypes.values().length][];

		HashMap<Pair<Object, StepTypes>, Integer> deviations = new HashMap<Pair<Object, StepTypes>, Integer>();
		HashSet<Object> nodes = new HashSet<Object>();

		for (SyncReplayResult repRes : replayResult) {
			//The node instances in the order in which the algorithm chose to execute them 
			List<Object> nodeInstance = repRes.getNodeInstance();
			//The step types taken for the corresponding node instance
			List<StepTypes> stepTypes = repRes.getStepTypes();
			//SortedSet<Integer> traceIndex = repRes.getTraceIndex();
			//# traces in this trace cluster which this repRes is all about
			int size = repRes.getTraceIndex().size();
			for (int i = 0; i < stepTypes.size() && i < nodeInstance.size(); i++) {
				Object currentNodeInstance = nodeInstance.get(i);
				nodes.add(currentNodeInstance);
				StepTypes currentStepType = stepTypes.get(i);
				//TODO ignore LINVI (=and split/join)
				if (currentStepType != StepTypes.MINVI) {

					Pair<Object, StepTypes> pair = new Pair<Object, StepTypes>(currentNodeInstance, currentStepType);
					int weight = size;
					if (deviations.containsKey(pair)) {
						weight += deviations.get(pair).intValue();
					}
					deviations.put(pair, weight);
				}
			}
		}

		HashSet<XEventClass> badClasses = new HashSet<XEventClass>();

		//Now that we know all problematic points, select the top troublemakers
		//An activity is a troublemaker iff the #wrong occurences exceeds the correct one by a certain factor
		//TEMP: select the top troublemaker
		int minValue = 1000;
		XEventClass hotspot = null;

		for (Object node : nodes) {
			//Per node, calculate the fraction of L+M wrt LMGOOD
			Pair<Object, StepTypes> lmgoodKey = new Pair<Object, StepTypes>(node, StepTypes.LMGOOD);
			int lmgood = 0;
			if (deviations.containsKey(lmgoodKey))
				lmgood = deviations.get(lmgoodKey);

			Pair<Object, StepTypes> logKey = new Pair<Object, StepTypes>(node, StepTypes.L);
			int log = 0;
			if (deviations.containsKey(logKey))
				log = deviations.get(logKey);
			deviations.get(new Pair<Object, StepTypes>(node, StepTypes.L));

			Pair<Object, StepTypes> modelKey = new Pair<Object, StepTypes>(node, StepTypes.MREAL);
			int model = 0;
			if (deviations.containsKey(modelKey))
				model = deviations.get(modelKey);

			double badness = (double) lmgood / (log + model);

			if (badness < minValue) {
				if (node instanceof XEventClass) {
					hotspot = (XEventClass) node;
					badClasses.add((XEventClass) node);
				}
			}
		}

		//TEMP select only top hotspot
		badClasses.clear();
		badClasses.add(hotspot);

		return badClasses;
		/**/
	}

	/**
	 * Returns the harmonic mean of the given list of values
	 * 
	 * @param values
	 * @return
	 */
	private double harmonicMean(List<Double> values) {
		//A list of 1 returns 1.0
		if (values.size() == 1)
			return values.get(0);

		double sum = 0;
		double product = 1;
		for (Double dub : values) {
			sum += dub;
			product *= dub;
		}

		double numerator = values.size() * product;
		double denominator = sum;
		if (denominator == 0)
			return 0;
		return numerator / denominator;
	}

	//UNUSED
	/**
	 * Returns a (normalized) fitness value wrt the structure of the tree (e.g.
	 * punish for too much behavior)
	 * 
	 * @param tree
	 * @return structural fitness
	 */
	private double calculateStructuralFitness(Tree tree) {
		//The cost of this tree
		int costs = 0;
		//The minimal cost this tree could have given this number of fnodes
		int minCosts = 0;

		//Build the map of costs per function
		Map<FUNCTIONTYPE, Integer> functionCost = new HashMap<FUNCTIONTYPE, Integer>();
		functionCost.put(FUNCTIONTYPE.SEQ, 1);
		functionCost.put(FUNCTIONTYPE.XOR, 2);
		//We don't like unnecessary AND's (ants too)
		functionCost.put(FUNCTIONTYPE.AND, 10);
		//The minimal cost we could have for an fnode is 1
		int minimalCost = 1;

		List<Node> postorder = tree.getPostorder();

		for (Node node : postorder) {
			if (!node.isLeaf()) {
				FunctionNode fnode = (FunctionNode) node;
				switch (fnode.getFunction()) {
					case SEQ :
						costs += functionCost.get(FUNCTIONTYPE.SEQ);
						break;
					case XOR :
						costs += functionCost.get(FUNCTIONTYPE.XOR);
						break;
					case AND :
						costs += functionCost.get(FUNCTIONTYPE.AND);
						break;
					default :
						break;
				}
				minCosts += minimalCost;
			}
		}

		return (double) minCosts / costs;
	}

	/**
	 * This fitness evaluator is a natural function. A fitness of one indicates
	 * a perfect solution.
	 * 
	 * @return true
	 */
	public boolean isNatural() {
		return true;
	}
}
