/**
 * 
 */
package org.processmining.plugins.petrinet.replayer.matchinstances.algorithms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.PluginContextID;
import org.processmining.framework.plugin.PluginExecutionResult;
import org.processmining.framework.plugin.PluginParameterBinding;
import org.processmining.framework.plugin.Progress;
import org.processmining.framework.plugin.events.Logger;
import org.processmining.framework.util.Pair;
import org.processmining.models.connections.petrinets.behavioral.FinalMarkingConnection;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.transitionsystem.AcceptStateSet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.petrinet.replayer.util.LogCounterPrecSyncReplay;
import org.processmining.plugins.petrinet.replayer.util.codec.EncPN;
import org.processmining.plugins.petrinet.replayer.util.statespaces.CPNCostBasedTreeNodeEncFitness;
import org.processmining.plugins.petrinet.replayresult.PNMatchInstancesRepResult;
import org.processmining.plugins.replayer.util.StepTypes;

/**
 * Replay based on the A* algorithm Cost of Move on model and move and log are
 * set for each transition.
 * 
 * This algorithm assumes there is only 1 final marking.
 * 
 * @author aadrians
 * 
 */
public class CompleteCostBasedPNMatchInstancesReplayAlgorithm implements IPNMatchInstancesLogReplayAlgorithm {

	/**
	 * Pointers to parameters in array of objects
	 */
	// static reference for GUI
	public static final int MAPTRANSTOCOST = 0;
	public static final int MAXEXPLOREDINSTANCES = 1;
	public static final int MAPXEVENTCLASSTOCOST = 2;

	/**
	 * Imported parameters
	 */
	// required parameters for replay
	private Map<Transition, Integer> mapTrans2Cost;
	private Map<XEventClass, Integer> mapEvClass2Cost;
	private int maxNumOfStates;

	public String toString() {
		return "Cost-based Petri net replay for completed trace (assuming one start place and one sink place)";
	}

	public PNMatchInstancesRepResult replayLog(final PluginContext context, final PetrinetGraph net,
			final Marking initMarking, Marking finalMarking, final XLog log,
			Collection<Pair<Transition, XEventClass>> mapping, Object[] parameters) {
		importParameters(parameters);

		if (maxNumOfStates != Integer.MAX_VALUE) {
			context.log("Starting replay with max state " + maxNumOfStates + "...");
		} else {
			context.log("Starting replay with no limit for max explored state...");
		}

		final XLogInfo summary = XLogInfoFactory.createLogInfo(log);
		final XEventClasses classes = summary.getEventClasses();

		// required to produce correct output object to be visualized 
		final LogCounterPrecSyncReplay counter = new LogCounterPrecSyncReplay();

		// if final marking is empty, use places in which no outgoing arcs exists
		final Marking editedFinalMarking;
		if (finalMarking.size() == 0) {
			editedFinalMarking = predictFinalMarking(context, net, initMarking);
		} else {
			editedFinalMarking = finalMarking;
		}

		// replay variables
		final EncPN encodedPN = new EncPN(net, initMarking, editedFinalMarking, mapTrans2Cost);

		// get helping variables
		final Map<XEventClass, Set<Integer>> mapEvClass2EncTrans = getMappingEventClass2EncTrans(mapping, encodedPN);
		final Map<Integer, Integer> mapEncTrans2Cost = getTransViolationCosts(encodedPN, mapTrans2Cost);
		final Map<Integer, Map<Integer, Integer>> mapArc2Weight = encodedPN.getMapArc2Weight();
		final Random numGenerator = new Random();

		// encode marking
		final Map<Integer, Map<Integer, Integer>> mapInt2Marking = Collections.synchronizedMap(new HashMap<Integer, Map<Integer, Integer>>());
		final Map<Map<Integer, Integer>, Integer> mapMarking2Int = Collections.synchronizedMap(new HashMap<Map<Integer, Integer>, Integer>());

		final Integer encInitMarking = numGenerator.nextInt();
		final Map<Integer, Integer> m = encodedPN.getEncInitialMarking();
		mapInt2Marking.put(encInitMarking, m);
		mapMarking2Int.put(m, encInitMarking);

		int temp = numGenerator.nextInt();
		final Map<Integer, Integer> fm = encodedPN.getEncFinalMarking();
		while (mapInt2Marking.get(temp) != null) {
			temp = numGenerator.nextInt();
		}
		final Integer encFinalMarking = temp;
		mapInt2Marking.put(encFinalMarking, fm);
		mapMarking2Int.put(fm, encFinalMarking);

		// update progress bar
		final Progress progress = context.getProgress();
		progress.setValue(0);
		progress.setIndeterminate(false);
		progress.setMinimum(0);
		progress.setMaximum(log.size());

		int threads = Runtime.getRuntime().availableProcessors() / 2 + 1;
		ExecutorService executor = Executors.newFixedThreadPool(threads);

		int index = 0;
		final Map<List<XEventClass>, List<Integer>> listTraces = new HashMap<List<XEventClass>, List<Integer>>();

		for (final XTrace trace : log) {
			// ignore event classes that does not have corresponding transition
			final List<XEventClass> listTrace = getListEventClass(trace, classes, mapEvClass2EncTrans);

			if (listTraces.containsKey(listTrace)) {
				listTraces.get(listTrace).add(log.indexOf(trace));
			} else {
				ArrayList<Integer> arrList = new ArrayList<Integer>();
				arrList.add(log.indexOf(trace));
				listTraces.put(listTrace, arrList);
				context.log("Replaying trace: " + index++ + " of length " + trace.size());

				executor.execute(new Runnable() {

					public void run() {
						Object[] resLoop = replayLoop(context, listTrace, encodedPN, mapArc2Weight, mapInt2Marking,
								mapMarking2Int, encInitMarking, encFinalMarking, numGenerator, maxNumOfStates,
								mapEvClass2EncTrans, mapEvClass2Cost, mapEncTrans2Cost, progress);
						@SuppressWarnings("unchecked")
						Set<CPNCostBasedTreeNodeEncFitness> setSolutionNodes = (Set<CPNCostBasedTreeNodeEncFitness>) resLoop[0];
						if (progress.isCancelled()) {
							return;
						}

						List<List<Object>> nodeInstanceLstOfLst = new LinkedList<List<Object>>();
						List<List<StepTypes>> stepTypesLstOfLst = new LinkedList<List<StepTypes>>();
						for (CPNCostBasedTreeNodeEncFitness currNode : setSolutionNodes) {
							createShortListFromTreeNode(encodedPN, currNode, nodeInstanceLstOfLst, stepTypesLstOfLst,
									listTrace);
						}

						counter.add(listTrace, nodeInstanceLstOfLst, stepTypesLstOfLst, log.indexOf(trace),
								(Boolean) resLoop[1]);
						progress.inc();
					}
				});
			}
		}

		executor.shutdown();
		try {
			while (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
				// try again if not terminated.
				if (progress.isCancelled()) {
					executor.shutdownNow();
				}
			}
		} catch (InterruptedException e) {
			context.log(e);
			return null;
		} catch (OutOfMemoryError memExc) {
			context.log("Out of memory while synchronizing result. Continue processing what has been obtained.");
		}

		for (List<XEventClass> listTrace : listTraces.keySet()) {
			for (Integer traceIndex : listTraces.get(listTrace)) {
				counter.inc(listTrace, traceIndex);
			}
		}

		return new PNMatchInstancesRepResult(counter.getResult());
	}

	/**
	 * Prediction for final marking. Use
	 * 
	 * @param net
	 * @return
	 */
	private Marking predictFinalMarking(PluginContext context, PetrinetGraph net, Marking initMarking) {
		Marking[] allFinalMarkings = getFinalMarking(context, net, initMarking);
		if (allFinalMarkings == null) {
			context.log("There is no final marking. Empty final marking will be used.");
			return new Marking();
		} else {
			context.log("There are more than one final markings/dead markings. One of them will be used.");
			return allFinalMarkings[0];
		}
	}

	/**
	 * Derive final markings from accepting states
	 * 
	 * @param context
	 * @param net
	 * @param initMarking
	 * @return
	 */
	private Marking[] getFinalMarking(PluginContext context, PetrinetGraph net, Marking initMarking) {
		// check if final marking exists
		Marking[] finalMarking = null;
		try {
			Collection<FinalMarkingConnection> finalMarkingConnections = context.getConnectionManager().getConnections(
					FinalMarkingConnection.class, context, net);
			if (finalMarkingConnections.size() == 0) {
				finalMarking = deriveFinalMarkingFromDeadMarkings(context, net, initMarking);
			} else {
				finalMarking = new Marking[finalMarkingConnections.size()];
				int index = 0;
				for (FinalMarkingConnection conn : finalMarkingConnections) {
					finalMarking[index] = (Marking) conn.getObjectWithRole(FinalMarkingConnection.MARKING);
					index++;
				}
			}
			return finalMarking;
		} catch (ConnectionCannotBeObtained exc) {
			// no final marking provided, try to derive it
			return deriveFinalMarkingFromDeadMarkings(context, net, initMarking);
		}
	}

	private Marking[] deriveFinalMarkingFromDeadMarkings(final PluginContext context, PetrinetGraph net,
			Marking initMarking) {
		try {
			AcceptStateSet acceptingStates = null;
			final PluginContext child = context.createChildContext("covGraph");
			context.getPluginLifeCycleEventListeners().firePluginCreated(child);
			child.getLoggingListeners().add(new Logger() {
				public void log(String message, PluginContextID contextID, MessageLevel messageLevel) {
					// ignore
				}

				public void log(Throwable t, PluginContextID contextID) {
					context.log(t);
				}

			});

			Set<PluginParameterBinding> plugins = context.getPluginManager().getPluginsAcceptingOrdered(
					PluginContext.class, true, net.getClass(), initMarking.getClass());

			// choose the one that calls CGGenerator
			for (PluginParameterBinding plugin : plugins) {
				if (plugin.getPlugin().getName().equals("Construct Coverability Graph of a Petri Net")) {
					context.log("Find final marking from dead markings");

					PluginExecutionResult result = plugin.invoke(child, net, initMarking);
					try {
						result.synchronize();
					} catch (CancellationException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					acceptingStates = result.getResult(3);

					// from accepting states, derive dead markings
					if (acceptingStates.size() > 0) {
						Marking[] res = new Marking[acceptingStates.size()];
						int index = 0;
						for (Object state : acceptingStates) {
							res[index] = (Marking) state;
							index++;
						}
						return res;
					} else {
						return new Marking[] { new Marking() };
					}
				}
			}
			// return an empty marking if no accepting states can be generated
			return new Marking[] { new Marking() };
		} catch (Exception exc2) {
			// return an empty marking if no accepting states can be generated
			return new Marking[] { new Marking() };
		}
	}

	/**
	 * provide solutions
	 * 
	 * @param encPN
	 * @param currNode
	 * @param nodeInstanceLstOfLst
	 * @param stepTypesLstOfLst
	 * @param listTrace
	 */
	private void createShortListFromTreeNode(EncPN encPN, CPNCostBasedTreeNodeEncFitness currNode,
			List<List<Object>> nodeInstanceLstOfLst, List<List<StepTypes>> stepTypesLstOfLst,
			List<XEventClass> listTrace) {
		List<Object> nodeInstanceLst = new LinkedList<Object>();
		List<StepTypes> stepTypesLst = new LinkedList<StepTypes>();

		while (currNode.getParent() != null) {
			stepTypesLst.add(0, currNode.getLatestStepType());
			if (currNode.getLatestStepType().equals(StepTypes.L)) {
				nodeInstanceLst.add(0, listTrace.get(currNode.getCurrIndexOnTrace() - 1));
			} else {
				nodeInstanceLst.add(0, encPN.getPetrinetNodeOf(currNode.getRelatedStepTypeObj()));
			}
			currNode = currNode.getParent();
		}

		nodeInstanceLstOfLst.add(nodeInstanceLst);
		stepTypesLstOfLst.add(stepTypesLst);
	}

	/**
	 * Get mapping from encoded transition to cost of move on model only
	 * 
	 * @param encodedPN
	 * @param mapTrans2Cost
	 * @return
	 */
	private Map<Integer, Integer> getTransViolationCosts(EncPN encodedPN, Map<Transition, Integer> mapTrans2Cost) {
		Set<Transition> setTrans = mapTrans2Cost.keySet();
		Map<Integer, Integer> res = new HashMap<Integer, Integer>(setTrans.size());
		for (Transition t : setTrans) {
			res.put(encodedPN.getEncOf(t), mapTrans2Cost.get(t));
		}
		return res;
	}

	/**
	 * Return the set of solutions, could also be empty. Solutions may be
	 * unreliable.
	 * 
	 */
	private Object[] replayLoop(PluginContext context, List<XEventClass> lstEvtClass, EncPN encodedPN,
			final Map<Integer, Map<Integer, Integer>> mapArc2Weight,
			Map<Integer, Map<Integer, Integer>> mapInt2Marking, Map<Map<Integer, Integer>, Integer> mapMarking2Int,
			Integer encInitMarking, Integer encFinalMarking, Random numGenerator, int maxNumOfStates,
			final Map<XEventClass, Set<Integer>> mapEvClass2EncTrans, final Map<XEventClass, Integer> mapEvClass2Cost,
			final Map<Integer, Integer> mapEncTrans2Cost, Progress progress) {
		// control variables
		int lstLength = lstEvtClass.size();

		// create tree 
		CPNCostBasedTreeNodeEncFitness stateSpaceRoot = new CPNCostBasedTreeNodeEncFitness(0, encInitMarking, null,
				null, 0, null);

		// explore state space
		int stateCounter = 1;
		CPNCostBasedTreeNodeEncFitness currStateSpaceNode = stateSpaceRoot;

		// result
		Integer solutionCost = null;
		Set<CPNCostBasedTreeNodeEncFitness> solutionNodes = new HashSet<CPNCostBasedTreeNodeEncFitness>();

		if (lstLength > 0) {
			PriorityQueue<CPNCostBasedTreeNodeEncFitness> costBasedPNPQ = new PriorityQueue<CPNCostBasedTreeNodeEncFitness>();
			while ((currStateSpaceNode != null)
					&& ((solutionCost == null) || (currStateSpaceNode.getCost() <= solutionCost))
					&& (stateCounter < maxNumOfStates) && (!progress.isCancelled())) {
				if (currStateSpaceNode.getCurrIndexOnTrace() < lstLength) {
					// do move on log
					CPNCostBasedTreeNodeEncFitness mvOnLogState = new CPNCostBasedTreeNodeEncFitness(
							currStateSpaceNode.getCurrIndexOnTrace() + 1, currStateSpaceNode.getCurrEncMarking(),
							StepTypes.L, null, currStateSpaceNode.getCost()
									+ mapEvClass2Cost.get(lstEvtClass.get(currStateSpaceNode.getCurrIndexOnTrace())),
							currStateSpaceNode);
					//					if (!costBasedPNPQ.contains(mvOnLogState)){
					costBasedPNPQ.add(mvOnLogState);
					//					}
					stateCounter++;
				}

				// do move on model
				Set<Integer> enabledTransitions = getEnabledTransitions(encodedPN,
						mapInt2Marking.get(currStateSpaceNode.getCurrEncMarking()), mapArc2Weight);
				for (Integer trans : enabledTransitions) {
					// execute the enabled transitions and change current marking
					Map<Integer, Integer> newMarking = new HashMap<Integer, Integer>(
							mapInt2Marking.get(currStateSpaceNode.getCurrEncMarking()));

					for (Integer predecessor : encodedPN.getPredecessorsOf(trans)) {
						int newNumToken = newMarking.get(predecessor) - mapArc2Weight.get(predecessor).get(trans);
						if (newNumToken > 0) {
							newMarking.put(predecessor, newNumToken);
						} else {
							newMarking.remove(predecessor);
						}
					}
					;

					Map<Integer, Integer> successorMap = mapArc2Weight.get(trans);
					if (successorMap != null) {
						for (Integer place : successorMap.keySet()) {
							Integer numTokens = newMarking.get(place);
							if (numTokens == null) {
								newMarking.put(place, successorMap.get(place));
							} else {
								newMarking.put(place, newMarking.get(place) + successorMap.get(place));
							}
						}
					}

					// set up marking index
					Integer newMarkingIndex = null;
					newMarkingIndex = getIntegerOfMarking(newMarking, mapMarking2Int, mapInt2Marking, numGenerator);

					CPNCostBasedTreeNodeEncFitness mvOnModelStateSpace = null;
					if (!((Transition) (encodedPN.getPetrinetNodeOf(trans))).isInvisible()) {
						// not invisible
						mvOnModelStateSpace = new CPNCostBasedTreeNodeEncFitness(
								currStateSpaceNode.getCurrIndexOnTrace(), newMarkingIndex, StepTypes.MREAL, trans,
								currStateSpaceNode.getCost() + mapEncTrans2Cost.get(trans), currStateSpaceNode);
					} else {
						// invisible
						mvOnModelStateSpace = new CPNCostBasedTreeNodeEncFitness(
								currStateSpaceNode.getCurrIndexOnTrace(), newMarkingIndex, StepTypes.MINVI, trans,
								currStateSpaceNode.getCost() + mapEncTrans2Cost.get(trans), currStateSpaceNode);
					}

					//					if (!costBasedPNPQ.contains(mvOnModelStateSpace)){
					costBasedPNPQ.add(mvOnModelStateSpace);
					//					}
					stateCounter++;

					// check for move synchronously
					if ((currStateSpaceNode.getCurrIndexOnTrace() < lstLength)
							&& (mapEvClass2EncTrans.get(lstEvtClass.get(currStateSpaceNode.getCurrIndexOnTrace()))
									.contains(trans))) {
						CPNCostBasedTreeNodeEncFitness mvSynchronous = new CPNCostBasedTreeNodeEncFitness(
								currStateSpaceNode.getCurrIndexOnTrace() + 1, newMarkingIndex, StepTypes.LMGOOD, trans,
								currStateSpaceNode.getCost(), currStateSpaceNode);
						//						if (!costBasedPNPQ.contains(mvSynchronous)){
						costBasedPNPQ.add(mvSynchronous);
						//						}
						stateCounter++;
					}
				}

				currStateSpaceNode = costBasedPNPQ.poll();

				while ((currStateSpaceNode.getCurrIndexOnTrace() == lstLength)
						&& (currStateSpaceNode.getCurrEncMarking() == encFinalMarking)) {
					if (solutionCost == null) { // check if solution had been found before
						solutionCost = currStateSpaceNode.getCost();
						solutionNodes.add(currStateSpaceNode);
						currStateSpaceNode = costBasedPNPQ.poll();
					} else {
						if (solutionCost.compareTo(currStateSpaceNode.getCost()) == 0) {
							// another solution is found
							solutionNodes.add(currStateSpaceNode);
						}
						currStateSpaceNode = costBasedPNPQ.poll();
					}
				}

			}
		}

		return new Object[] { solutionNodes, stateCounter < maxNumOfStates };
	}

	private synchronized Integer getIntegerOfMarking(Map<Integer, Integer> newMarking,
			Map<Map<Integer, Integer>, Integer> mapMarking2Int, Map<Integer, Map<Integer, Integer>> mapInt2Marking,
			Random numGenerator) {
		Integer newMarkingIndex = mapMarking2Int.get(newMarking);
		if (newMarkingIndex == null) {
			int index = numGenerator.nextInt();
			while (mapInt2Marking.get(index) != null) {
				index = numGenerator.nextInt();
			}
			mapMarking2Int.put(newMarking, index);
			mapInt2Marking.put(index, newMarking);
			newMarkingIndex = index;
		}
		return newMarkingIndex;
	}

	/**
	 * Get mapping from event class to encoded transition
	 * 
	 * @param mapping
	 * @param encPN
	 * @return
	 */
	private Map<XEventClass, Set<Integer>> getMappingEventClass2EncTrans(
			Collection<Pair<Transition, XEventClass>> mapping, EncPN encPN) {
		Map<XEventClass, Set<Integer>> mapEvClass2Trans = new HashMap<XEventClass, Set<Integer>>();
		for (Pair<Transition, XEventClass> pair : mapping) {
			Set<Integer> setTrans = mapEvClass2Trans.get(pair.getSecond());
			if (setTrans == null) {
				setTrans = new HashSet<Integer>();
				mapEvClass2Trans.put(pair.getSecond(), setTrans);
			}
			setTrans.add(encPN.getEncOf(pair.getFirst()));
		}
		return mapEvClass2Trans;
	}

	/**
	 * Return transitions that are enabled in particular marking
	 * 
	 * @param encodedPN
	 * @param marking
	 * @param mapArc2Weight
	 * @return
	 */
	private Set<Integer> getEnabledTransitions(EncPN encodedPN, Map<Integer, Integer> marking,
			Map<Integer, Map<Integer, Integer>> mapArc2Weight) {
		// get continuation from marking
		Set<Integer> enabledTransitions = new HashSet<Integer>(3);
		for (Integer place : marking.keySet()) {
			Map<Integer, Integer> succTrans = mapArc2Weight.get(place);
			if (succTrans != null) {
				enabledTransitions.addAll(succTrans.keySet());
			}
		}
		// filter out tobechecked
		Iterator<Integer> it = enabledTransitions.iterator();
		iterateTransition: while (it.hasNext()) {
			Integer transition = it.next();
			Set<Integer> predecessors = encodedPN.getPredecessorsOf(transition);
			if (predecessors != null) {
				for (Integer place : predecessors) {
					Integer numTokens = marking.get(place);
					if (numTokens != null) {
						if (numTokens < mapArc2Weight.get(place).get(transition)) {
							it.remove();
							continue iterateTransition;
						}
					} else {
						// not sufficient token
						it.remove();
						continue iterateTransition;
					}
				}
			}
		}
		return enabledTransitions;
	}

	/**
	 * get list of event class. Ignore non-mapped event class.
	 * 
	 * @param trace
	 * @param classes
	 * @param mapEvClass2Trans
	 * @return
	 */
	private List<XEventClass> getListEventClass(XTrace trace, XEventClasses classes,
			Map<XEventClass, Set<Integer>> mapEvClass2Trans) {
		List<XEventClass> res = new LinkedList<XEventClass>();
		for (XEvent evt : trace) {
			XEventClass evClass = classes.getClassOf(evt);
			if (mapEvClass2Trans.get(evClass) != null) {
				res.add(evClass);
			}
		}
		return res;
	}

	/**
	 * Assign values of private attributes as given in parameters
	 * 
	 * @param parameters
	 */
	@SuppressWarnings("unchecked")
	private void importParameters(Object[] parameters) {
		// replay parameters
		mapTrans2Cost = (Map<Transition, Integer>) parameters[MAPTRANSTOCOST];
		mapEvClass2Cost = (Map<XEventClass, Integer>) parameters[MAPXEVENTCLASSTOCOST];
		maxNumOfStates = (Integer) parameters[MAXEXPLOREDINSTANCES];
	}
}
