/**
 * 
 */
package org.processmining.plugins.flex.replayer.algorithms;

import java.awt.Color;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.Progress;
import org.processmining.framework.util.Pair;
import org.processmining.framework.util.collection.HashMultiSet;
import org.processmining.framework.util.collection.MultiSet;
import org.processmining.models.connections.flexiblemodel.FlexCodecConnection;
import org.processmining.models.flexiblemodel.EndTaskNodesSet;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.models.flexiblemodel.FlexNode;
import org.processmining.models.flexiblemodel.SetFlex;
import org.processmining.models.flexiblemodel.StartTaskNodesSet;
import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.instancetree.flex.costbased.ExtendedCostBasedFIT;
import org.processmining.models.instancetree.flex.costbased.ExtendedCostBasedFITEdge;
import org.processmining.models.instancetree.flex.costbased.ExtendedCostBasedFITFactory;
import org.processmining.models.instancetree.flex.costbased.ExtendedCostBasedFITNode;
import org.processmining.plugins.flex.replayer.data.FlexExtendedEncodedState;
import org.processmining.plugins.flex.replayer.data.FlexNodeEncodedPredSuc;
import org.processmining.plugins.flex.replayer.ui.ParamSettingExtendedCostBasedReplay;
import org.processmining.plugins.flex.replayer.util.CFlexExtendedCostBasedAStar;
import org.processmining.plugins.flex.replayer.util.FlexBinding;
import org.processmining.plugins.flex.replayer.util.FlexCodec;
import org.processmining.plugins.flex.replayer.util.LogEncodedCounterSyncReplay;
import org.processmining.plugins.flex.replayresult.FlexRepResult;
import org.processmining.plugins.replayer.replayresult.CaseReplayResult;
import org.processmining.plugins.replayer.util.EventEncoder;
import org.processmining.plugins.replayer.util.StepTypes;

/**
 * @author aadrians
 */

public class ExtendedCostBasedAStarLogReplayAlgorithm implements IFlexLogReplayAlgorithm {
	/**
	 * Parameters to be exposed as input
	 */
	public static int ISTESTINGMODEINTVAL = 0; 
	// 0 if replay mode, 1 if testing mode
	
	public static int MAXEXPLOREDINSTANCESINTVAL = 1; 
	// max number of explored instances, before stop
	
	public static int INAPPROPRIATETRANSFIRECOST = 2; 
	// transitions that fire without proper predecessors
	
	public static int HEURISTICDISTANCECOST = 3; 
	// number of events still left to be replayed
	
	public static int SKIPPEDEVENTCOST = 4; 
	// number of events that are ignored in replay
	
	public static int REPLAYEDEVENTCOST = 5; 
	// number of replayed events
	
	public static int INITIATIVEINVISTASKCOST = 6; 
	// number of invisible tasks that are executed without any occurrence of its
	// corresponding events.
	
	public static int INITIATIVEREALTASKCOST = 7; 
	// number of tasks that are executed without any occurrence of its
	// corresponding events.

	public static int FILELOCATIONSTRVAL = 8; 
	// location of file to store testing result
	
	public static int ANALYSISTYPEINTVAL = 9; // analysis type
	
	// permittable replay actions
	public static int ALLOWINVITASK = 10;
	public static int ALLOWREALTASK = 11;
	public static int ALLOWEVENTSKIP = 12;
	public static int ALLOWTASKEXECWOTOKENS = 13;
	public static int ALLOWINAPPROPRIATEFIRING = 14;

	/**
	 * Internal variables, correspond to the ones exposed
	 */
	// replay parameters
	private int maxNumOfStates;

	// cost
	private int inappropriateTransFireCost; // event that executed without
	// proper predecessors
	private int replayedEventCost; // number of replayed events
	private int skippedEventCost; // number of events that are ignored in replay
	private int heuristicDistanceCost; // number of events still left to be replayed
	private int selfExecInviTaskCost; // number of invisible tasks that are
	// executed without any occurrence of its corresponding events.
	private int selfExecRealTaskCost; // number of tasks that are executed
	// without any occurrence of its corresponding events.

	// allowed moves
	private boolean allowInviTaskMove = true;
	private boolean allowRealTaskMove = true;
	private boolean allowEventSkip = true;
	private boolean allowExecWOTokens = false;
	private boolean allowExecViolating = true;

	/**
	 * Other variables
	 */
	// constant for result replaying a trace
	protected static final int LISTOFPAIR = 0;
	protected static final int ISRELIABLE = 1;

	// internal result of loopReplay
	private static int ID = 0;
	private static int NODE = 1;
	
	// COLORING THE INSTANCE TREE
	public static Color ORANGE = new Color(255, 191, 0);
	public static Color RED = new Color(255, 0, 0);
	public static Color GREEN = new Color(0, 255, 0);
	public static Color GRAY = new Color(100, 100, 100);
	public static Color YELLOW = new Color(255, 255, 0);
	public static Color PURPLE = new Color(205, 106, 205);

	public static Color LIGHTRED = new Color(255, 204, 204, 200);
	public static Color LIGHTGREEN = new Color(243, 255, 204, 200);
	public static Color LIGHTYELLOW = new Color(255, 255, 204, 200);
	public static Color LIGHTGRAY = new Color(240, 240, 240, 200);
	public static Color LIGHTPURPLE = new Color(236, 198, 236);

	// INPUT ABSTRACTION
	private Map<Short, FlexNodeEncodedPredSuc> encodedFlex = new HashMap<Short, FlexNodeEncodedPredSuc>();

	// UTILITY OBJECTS
	private Map<Short, Short> worstPossibilityMap = new HashMap<Short, Short>();
	private FlexCodec codec;
	private EventEncoder ee;
	private Map<Short, List<Short>> eventToFlex = new HashMap<Short, List<Short>>();
	private Map<Short, Short> flexToEvent = new HashMap<Short, Short>();

	// LIMIT
	public static int MAXNUMOFSTATES = 0; // during state exploration, if num of states of a trace is more than this, the trace is skipped	

	public String toString() {
		return "Cost-based A* heuristic log replay";
	}

	@Override
	public FlexRepResult replayLog(final PluginContext context, final Flex flex, final FlexCodec codec,
			final StartTaskNodesSet startTaskNodesSet, final EndTaskNodesSet endTaskNodesSet, final XLog log,
			final Collection<Pair<FlexNode, XEventClass>> mapping, final Object[] parameters) {

		// only executed if obligation requirement is satisfied
		importParameters(parameters);
		if (maxNumOfStates != Integer.MAX_VALUE) {
			context.log("Starting replay with max state " + maxNumOfStates + "...");
		} else {
			context.log("Starting replay with no limit for max explored state...");
		}

		// get classes
		final XLogInfo summary = XLogInfoFactory.createLogInfo(log);
		final XEventClasses classes = summary.getEventClasses();

		// prepare replay
		// use connection to get flex codec
		this.codec = codec;

		createEncodedFlexAndWorstPossibility(flex);
		createEncodedMappingAndEventEncoding(mapping);
		final Set<Short> setInviFlex = codec.getInvisNodes();

		// get initial start task node
		final Set<Short> setStartTaskNodes = new HashSet<Short>(2); // only one node initially enabled
		for (FlexNode initNode : startTaskNodesSet.iterator().next()) {
			setStartTaskNodes.add(codec.encode(initNode));
		}

		// required to produce correct output object to be visualized 
		final LogEncodedCounterSyncReplay counter = new LogEncodedCounterSyncReplay();

		// set context
		final Progress progress = context.getProgress();
		progress.setValue(0);

		int threads = Runtime.getRuntime().availableProcessors() / 2 + 1;
		ExecutorService executor = Executors.newFixedThreadPool(threads);

		int index = 0;
		final Map<List<Short>, List<Integer>> listTraces = new HashMap<List<Short>, List<Integer>>();

		for (final XTrace trace : log) {
			final List<Short> listTrace = createTraceAbstraction(trace, classes);

			if (listTraces.containsKey(listTrace)) {
				listTraces.get(listTrace).add(log.indexOf(trace));
			} else {
				listTraces.put(listTrace, new ArrayList<Integer>());
				context.log("Replaying trace: " + index++ + " of length " + trace.size());

				executor.execute(new Runnable() {

					@SuppressWarnings("unchecked")
					public void run() {
						Object[] replayRes = replayTraceForLog(setStartTaskNodes, setInviFlex,  new HashMultiSet<Pair<Short, Short>>(), listTrace);

						if (progress.isCancelled()) {
							return;
						}

						List<Pair<StepTypes, Object>> result = (List<Pair<StepTypes, Object>>) replayRes[ExtendedCostBasedAStarLogReplayAlgorithm.LISTOFPAIR];
						boolean isReliable = Boolean
								.valueOf(replayRes[ExtendedCostBasedAStarLogReplayAlgorithm.ISRELIABLE].toString());

						if (result == null) { // trace can NOT be replayed
							context.log("Trace " + XConceptExtension.instance().extractName(trace)
									+ " can't be replayed");
						} else { // trace can be replayed, its result may not be guaranteed to be correct (if heuristics applied)
							List<StepTypes> listStep = new LinkedList<StepTypes>();
							List<Object> nodeInstances = new LinkedList<Object>();

							for (Pair<StepTypes, Object> pair : result) {
								listStep.add(pair.getFirst());
								switch (pair.getFirst()) {
									case L :
										nodeInstances.add(((XEventClass) pair.getSecond()).toString());
										break;
									default :
										nodeInstances.add(pair.getSecond()); // FlexBinding class
										break;
								}
							}
							counter.add(listTrace, nodeInstances, listStep, log.indexOf(trace), isReliable);

							if (!isReliable) {
								context.log("Trace " + XConceptExtension.instance().extractName(trace)
										+ " result is unreliable");
							}
						}
						progress.inc();
					}
				});
			}
		}
		progress.setIndeterminate(false);
		progress.setMinimum(0);
		progress.setMaximum(listTraces.keySet().size());

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
		} catch (OutOfMemoryError memExc){
			context.log("Out of memory while synchronizing result. Continue processing what has been obtained.");
		}

		for (List<Short> listTrace : listTraces.keySet()) {
			for (Integer traceIndex : listTraces.get(listTrace)) {
				counter.inc(listTrace, traceIndex);
			}
		}
		return new FlexRepResult(counter.getResult(), skippedEventCost, selfExecInviTaskCost, selfExecRealTaskCost,
				inappropriateTransFireCost);
	}

	private void importParameters(Object[] parameters) {
		// replay parameters
		maxNumOfStates = Integer.parseInt(parameters[ParamSettingExtendedCostBasedReplay.MAXEXPLOREDINSTANCESINTVAL]
				.toString());
		// cost
		inappropriateTransFireCost = Integer
				.parseInt(parameters[ParamSettingExtendedCostBasedReplay.INAPPROPRIATETRANSFIRECOST].toString()); // event that executed without proper
		// predecessors
		replayedEventCost = Integer.parseInt(parameters[ParamSettingExtendedCostBasedReplay.REPLAYEDEVENTCOST].toString()); // number of replayed events
		skippedEventCost = Integer.parseInt(parameters[ParamSettingExtendedCostBasedReplay.SKIPPEDEVENTCOST].toString()); // number of events that are ignored in
		// replay
		heuristicDistanceCost = Integer.parseInt(parameters[ParamSettingExtendedCostBasedReplay.HEURISTICDISTANCECOST]
				.toString()); // number of events still left to be
		// replayed
		selfExecInviTaskCost = Integer.parseInt(parameters[ParamSettingExtendedCostBasedReplay.INITIATIVEINVISTASKCOST]
				.toString()); // number of invisible tasks that are
		// executed without any occurrence of
		// its corresponding events.
		selfExecRealTaskCost = Integer.parseInt(parameters[ParamSettingExtendedCostBasedReplay.INITIATIVEREALTASKCOST]
				.toString()); // number of tasks that are executed
		// without any occurrence of its
		// corresponding events.

		allowInviTaskMove = Boolean.parseBoolean(parameters[ParamSettingExtendedCostBasedReplay.ALLOWINVITASK].toString());
		allowRealTaskMove = Boolean.parseBoolean(parameters[ParamSettingExtendedCostBasedReplay.ALLOWREALTASK].toString());
		allowEventSkip = Boolean.parseBoolean(parameters[ParamSettingExtendedCostBasedReplay.ALLOWEVENTSKIP].toString());
		allowExecWOTokens = Boolean.parseBoolean(parameters[ParamSettingExtendedCostBasedReplay.ALLOWTASKEXECWOTOKENS]
				.toString());
		allowExecViolating = Boolean.parseBoolean(parameters[ParamSettingExtendedCostBasedReplay.ALLOWINAPPROPRIATEFIRING]
				.toString());

	}

	/**
	 * 
	 * @param setInviFlex
	 * @param obligation
	 * @param listTrace
	 * @return object[0] = list of pairs <step types, object> object[1] =
	 *         boolean, true if the result is reliable, false if it is not
	 */
	private Object[] replayTraceForLog(Set<Short> setStartTaskNodes, Set<Short> setInviFlex,
			MultiSet<Pair<Short, Short>> obligation, List<Short> listTrace) {
		PriorityQueue<CFlexExtendedCostBasedAStar> pq = new PriorityQueue<CFlexExtendedCostBasedAStar>();

		CFlexExtendedCostBasedAStar root = new CFlexExtendedCostBasedAStar();
		root.setCurrObligation(obligation);
		root.setCurrIndexOnTrace(0);
		root.setCost(heuristicDistanceCost * listTrace.size());
		pq.add(root);

		CFlexExtendedCostBasedAStar currNode; // pointer to current node

		// start replay
		int traceSize = listTrace.size();
		int id = 1;
		boolean reliable = true; // false if only the best so far is returned

		// only replay once, no reset
		try {
			Object[] loopResult = replayLoop(setStartTaskNodes, listTrace, setInviFlex, obligation, pq, traceSize, id);
	
			id = Integer.parseInt(loopResult[ID].toString());
			currNode = (CFlexExtendedCostBasedAStar) loopResult[NODE];
	
			if (id >= maxNumOfStates) {
				reliable = false;
			}
			return new Object[] { createShortListFromTreeNode(obligation, setInviFlex, currNode, listTrace), reliable };
		} catch (OutOfMemoryError outOfMemory){
			// if out of memory, return an empty list of pairs with unreliable result
			return new Object[] { new LinkedList<Pair<StepTypes, Object>>(), false };
		}
	}

	private List<Pair<StepTypes, Object>> createShortListFromTreeNode(MultiSet<Pair<Short, Short>> obligation,
			Set<Short> setInviFlex, CFlexExtendedCostBasedAStar currNode, List<Short> listTrace) {
		ListIterator<Pair<Integer, Short>> itDup = currNode.getDuplicatesOnlyStep().listIterator();
		ListIterator<Pair<Integer, Short>> itModOnly = currNode.getModelOnlyStep().listIterator();
		ListIterator<Pair<Integer, Short>> itModLogViolates = currNode.getTraceModelViolatingStep().listIterator();
		ListIterator<Integer> itTraceOnly = currNode.getMoveTraceOnlyStep().listIterator();

		List<Pair<StepTypes, Object>> res = new LinkedList<Pair<StepTypes, Object>>();

		int currIdx = 0; // pointer to the index currently replayed
		Pair<Integer, Short> currDup = itDup.hasNext() ? itDup.next() : null;
		Pair<Integer, Short> currModOnly = itModOnly.hasNext() ? itModOnly.next() : null;
		Pair<Integer, Short> currModLogViolates = itModLogViolates.hasNext() ? itModLogViolates.next() : null;
		int currTraceOnly = itTraceOnly.hasNext() ? itTraceOnly.next() : Integer.MAX_VALUE;

		// replay token games
		int currTraceIndex = 0;

		int resTraceSize = listTrace.size() + currNode.getModelOnlyStep().size();
		boolean loopFinish = false;

		while (currIdx < resTraceSize) {
			loopFinish = false;
			if (currDup != null) {
				if (currDup.getFirst() == currIdx) { // currently executing duplicate
					FlexBinding binding = codec.getFlexBindingFor(currDup.getSecond());

					// check if it's appropriate
					boolean isAppropriate = true;
					if ((binding.getEncodedInputBinding() != FlexCodec.BLANK)
							&& (binding.getEncodedInputBinding() != FlexCodec.EMPTYSET)) {
						for (Short in : codec.getIOBindingsFor(binding.getEncodedInputBinding())) {
							isAppropriate = isAppropriate
									&& obligation.remove(new Pair<Short, Short>(in, binding.getEncodedNode()));
						}
					}

					if ((binding.getEncodedOutputBinding() != FlexCodec.BLANK)
							&& (binding.getEncodedOutputBinding() != FlexCodec.EMPTYSET)) {
						for (Short out : codec.getIOBindingsFor(binding.getEncodedOutputBinding())) {
							obligation.add(new Pair<Short, Short>(binding.getEncodedNode(), out));
						}
					}

					if (isAppropriate) {
						// valid execution
						res.add(new Pair<StepTypes, Object>(StepTypes.LMGOOD, binding));
					} else { // invalid execution
						res.add(new Pair<StepTypes, Object>(StepTypes.LMNOGOOD, binding));
					}

					loopFinish = true;
					currTraceIndex++;
					currDup = itDup.hasNext() ? itDup.next() : null;
				}
			}

			if ((currModOnly != null) && (!loopFinish)) {
				if (currModOnly.getFirst() == currIdx) {
					FlexBinding binding = codec.getFlexBindingFor(currModOnly.getSecond());
					if (setInviFlex.contains(binding.getEncodedNode())) {
						res.add(new Pair<StepTypes, Object>(StepTypes.MINVI, binding));
					} else {
						res.add(new Pair<StepTypes, Object>(StepTypes.MREAL, binding));
					}

					if ((binding.getEncodedInputBinding() != FlexCodec.BLANK)
							&& (binding.getEncodedInputBinding() != FlexCodec.EMPTYSET)) {
						for (Short in : codec.getIOBindingsFor(binding.getEncodedInputBinding())) {
							obligation.remove(new Pair<Short, Short>(in, binding.getEncodedNode()));
						}
					}
					if ((binding.getEncodedOutputBinding() != FlexCodec.BLANK)
							&& (binding.getEncodedOutputBinding() != FlexCodec.EMPTYSET)) {
						for (Short out : codec.getIOBindingsFor(binding.getEncodedOutputBinding())) {
							obligation.add(new Pair<Short, Short>(binding.getEncodedNode(), out));
						}
					}
					loopFinish = true;
					currModOnly = itModOnly.hasNext() ? itModOnly.next() : null;
				}
			}

			if ((currModLogViolates != null) && (!loopFinish)) {
				if (currModLogViolates.getFirst() == currIdx) {
					FlexBinding binding = codec.getFlexBindingFor(currModLogViolates.getSecond());
					// invalid execution
					res.add(new Pair<StepTypes, Object>(StepTypes.LMNOGOOD, binding));

					if ((binding.getEncodedOutputBinding() != FlexCodec.BLANK)
							&& (binding.getEncodedOutputBinding() != FlexCodec.EMPTYSET)) {
						for (Short out : codec.getIOBindingsFor(binding.getEncodedOutputBinding())) {
							obligation.add(new Pair<Short, Short>(binding.getEncodedNode(), out));
						}
					}
					loopFinish = true;
					currModLogViolates = itModLogViolates.hasNext() ? itModLogViolates.next() : null;
					currTraceIndex++;
				}
			}

			if (currTraceOnly == currIdx) {
				res.add(new Pair<StepTypes, Object>(StepTypes.L, ee.decode(listTrace.get(currTraceIndex))));
				loopFinish = true;
				currTraceIndex++;
				currTraceOnly = itTraceOnly.hasNext() ? itTraceOnly.next() : Integer.MAX_VALUE;
			}

			if (!loopFinish) {
				FlexBinding binding = codec.getFlexBindingFor(codec.getPossibleNodeBindings(
						eventToFlex.get(listTrace.get(currTraceIndex)).get(0)).iterator().next());

				boolean isAppropriate = true;
				if ((binding.getEncodedInputBinding() != FlexCodec.BLANK)
						&& (binding.getEncodedInputBinding() != FlexCodec.EMPTYSET)) {
					for (Short in : codec.getIOBindingsFor(binding.getEncodedInputBinding())) {
						isAppropriate = isAppropriate
								&& obligation.remove(new Pair<Short, Short>(in, binding.getEncodedNode()));
					}
				}
				if ((binding.getEncodedOutputBinding() != FlexCodec.BLANK)
						&& (binding.getEncodedOutputBinding() != FlexCodec.EMPTYSET)) {
					for (Short out : codec.getIOBindingsFor(binding.getEncodedOutputBinding())) {
						obligation.add(new Pair<Short, Short>(binding.getEncodedNode(), out));
					}
				}

				if (isAppropriate) {
					// valid execution
					res.add(new Pair<StepTypes, Object>(StepTypes.LMGOOD, binding));
				} else { // invalid execution
					res.add(new Pair<StepTypes, Object>(StepTypes.LMNOGOOD, binding));
				}
				currTraceIndex++;
			}
			currIdx++;
		}
		return res;
	}

	private Object[] replayLoop(Set<Short> setStartTaskNodes, List<Short> listTrace, Set<Short> setInviFlex,
			MultiSet<Pair<Short, Short>> obligation, PriorityQueue<CFlexExtendedCostBasedAStar> pq, int traceSize,
			int id) throws OutOfMemoryError {
		CFlexExtendedCostBasedAStar currNode = pq.poll();

		while ((currNode.getCurrIndexOnTrace() < traceSize) && (id < maxNumOfStates)) {
			if (allowEventSkip) {
				// skip current event
				CFlexExtendedCostBasedAStar nodeT = createEncNodeByMoveTrace(traceSize, currNode);
				id++;
				if (!pq.contains(nodeT)) {
					pq.add(nodeT);
				}
			}

			// execute next event
			List<Short> candidateTrans = eventToFlex.get(listTrace.get(currNode.getCurrIndexOnTrace()));
			for (Short candidate : candidateTrans) {
				// create new pnCostBasedTreeNode
				id = createEncNodeByExecuteEvent(currNode, candidate, setStartTaskNodes, traceSize, pq, id,
						candidateTrans.size() > 1);

				if (allowExecWOTokens) {
					// execute without taking any tokens
					id = createEncNodeByExecuteWithoutTakingObligations(currNode, candidate, traceSize, pq, id);
				}
			}

			if (allowInviTaskMove || allowRealTaskMove) {
				// execute any possible transitions (including both invisible and
				// real transitions)
				id = createEncNodeByExecutingPossibleTransitions(currNode, setStartTaskNodes, setInviFlex, pq, id);
			}

			// continue to the next node
			currNode = pq.poll();

		}
		return new Object[] { id, currNode };
	}

	private int createEncNodeByExecutingPossibleTransitions(CFlexExtendedCostBasedAStar currNode,
			Set<Short> setStartTaskNodes, Set<Short> setInviFlex, PriorityQueue<CFlexExtendedCostBasedAStar> pq, int id) {
		Set<Short> possiblyExecutedBinding = getPossiblyExecutedBindings(currNode, setStartTaskNodes, codec
				.getBindingAnytimePossiblyFire());
		for (Short encBinding : possiblyExecutedBinding) {
			FlexBinding binding = codec.getFlexBindingFor(encBinding);
			if (!allowInviTaskMove && setInviFlex.contains(binding.getEncodedNode())) {
				continue;
			} else if (!allowRealTaskMove && !setInviFlex.contains(binding.getEncodedNode())) {
				continue;
			} else {
				// execute as usual
				CFlexExtendedCostBasedAStar res = new CFlexExtendedCostBasedAStar(currNode);

				if ((binding.getEncodedInputBinding() != FlexCodec.BLANK)
						&& (binding.getEncodedInputBinding() != FlexCodec.EMPTYSET)) {
					// take the input first
					for (Short input : codec.getIOBindingsFor(binding.getEncodedInputBinding())) {
						res.removeCurrObligation(new Pair<Short, Short>(input, binding.getEncodedNode()));
					}
				} else {
					res.addExecutedStartTaskNodes(binding.getEncodedNode());
				}

				if ((binding.getEncodedOutputBinding() != FlexCodec.BLANK)
						&& (binding.getEncodedOutputBinding() != FlexCodec.EMPTYSET)) {
					for (Short out : codec.getIOBindingsFor(binding.getEncodedOutputBinding())) {
						res.addCurrObligation(new Pair<Short, Short>(binding.getEncodedNode(), out));
					}
				}

				// cost
				int cost = res.getCost();
				if (setInviFlex.contains(binding.getEncodedNode())) {
					cost += selfExecInviTaskCost;
				} else {
					cost += selfExecRealTaskCost;
				}
				res.setCost(cost);

				// move only step update
				List<Pair<Integer, Short>> listPair = res.getModelOnlyStep();
				listPair.add(new Pair<Integer, Short>(listPair.size() + currNode.getCurrIndexOnTrace(), encBinding));
				res.setModelOnlyStep(listPair);

				id++;
				if (!pq.contains(res)) {
					pq.add(res);
				}
			}
		}
		return id;
	}

	private Set<Short> getPossiblyExecutedBindings(CFlexExtendedCostBasedAStar currNode, Set<Short> setStartTaskNodes,
			Set<Short> bindingAnytimePossiblyFire) {
		Set<Short> res = new HashSet<Short>();
		Set<Short> triedFlexNode = new HashSet<Short>();

		// try the start task nodes
		Set<Short> cpSetStartTaskNodes = new HashSet<Short>(setStartTaskNodes);
		cpSetStartTaskNodes.removeAll(currNode.getStartingTaskNodes());
		for (Short node : cpSetStartTaskNodes) {
			res.addAll(codec.getPossibleNodeBindings(node)); 
//			for (Short encBinding : codec.getPossibleNodeBindings(node)) {
//				if (codec.getFlexBindingFor(encBinding).getEncodedInputBinding() == FlexCodec.BLANK) {
//					res.add(encBinding);
//				}
//			}
//			;
		}

		// always add bindings without any precondition
		res.addAll(bindingAnytimePossiblyFire);

		// find enabled nodes
		for (Pair<Short, Short> pair : currNode.getCurrObligation().baseSet()) {

			if (!triedFlexNode.contains(pair.getSecond())) {
				// not contains second pair, still need to be explored
//				MultiSet<Pair<Short, Short>> bagPair = new HashMultiSet<Pair<Short, Short>>(currNode
//						.getCurrObligation());
				bindingLoop: for (Short encBinding : codec.getPossibleNodeBindings(pair.getSecond())) {
					MultiSet<Pair<Short, Short>> bagPair = new HashMultiSet<Pair<Short, Short>>(currNode
							.getCurrObligation());
					FlexBinding binding = codec.getFlexBindingFor(encBinding);
					if (binding.getEncodedInputBinding() != FlexCodec.BLANK) {
						for (Short input : codec.getIOBindingsFor(binding.getEncodedInputBinding())) {
							if (!bagPair.remove(new Pair<Short, Short>(input, pair.getSecond()))) {
								continue bindingLoop;
							}
						}
					}
					res.add(encBinding);
				}
				triedFlexNode.add(pair.getSecond());
			}
		}
		return res;
	}

	private int createEncNodeByExecuteWithoutTakingObligations(CFlexExtendedCostBasedAStar currNode, Short candidate,
			int traceSize, PriorityQueue<CFlexExtendedCostBasedAStar> pq, int id) {
		Set<Short> possibleNodeBindings = codec.getPossibleNodeBindings(candidate);
		for (Short nodeBinding : possibleNodeBindings) {
			CFlexExtendedCostBasedAStar res = new CFlexExtendedCostBasedAStar(currNode);
			FlexBinding binding = codec.getFlexBindingFor(nodeBinding);

			if ((binding.getEncodedOutputBinding() != FlexCodec.BLANK)
					&& (binding.getEncodedOutputBinding() != FlexCodec.EMPTYSET)) {
				for (Short out : codec.getIOBindingsFor(binding.getEncodedOutputBinding())) {
					res.addCurrObligation(new Pair<Short, Short>(candidate, out));
				}
			}

			res.setCurrIndexOnTrace(currNode.getCurrIndexOnTrace() + 1);

			res.getTraceModelViolatingStep().add(
					new Pair<Integer, Short>(currNode.getCurrIndexOnTrace() + currNode.getModelOnlyStep().size(),
							nodeBinding));

			// update cost
			int cost = currNode.getCost() + inappropriateTransFireCost + replayedEventCost - heuristicDistanceCost;
			res.setCost(cost);

			id++;
			if (!pq.contains(res)) {
				pq.add(res);
			}
		}

		return id;
	}

	private int createEncNodeByExecuteEvent(CFlexExtendedCostBasedAStar currNode, Short candidate,
			Set<Short> setStartTaskNodes, int traceSize, PriorityQueue<CFlexExtendedCostBasedAStar> pq, int id,
			boolean isDuplicate) {
		Set<Short> possibleNodeBindings = codec.getPossibleNodeBindings(candidate);
		iterateBinding : for (Short nodeBinding : possibleNodeBindings) {
			CFlexExtendedCostBasedAStar res = new CFlexExtendedCostBasedAStar(currNode);
			boolean isAppropriate = true;
			FlexBinding binding = codec.getFlexBindingFor(nodeBinding);
			if (binding.getEncodedInputBinding() == FlexCodec.BLANK) { // execute as starting task node
				// only start task nodes can have blank input binding 
				assert (setStartTaskNodes.contains(binding.getEncodedNode()));

				// any other node has ever been mapped to this node before?
				if (currNode.startTaskNodesContains(candidate)) {
					// hence, the same node has been used as starting task nodes before, is appropriate = false
					isAppropriate = false;
				} else {
					// the node has NOT been used as starting task nodes before, is appropriate = true
					res.addExecutedStartTaskNodes(candidate);
				}
			} else if (binding.getEncodedInputBinding() == FlexCodec.EMPTYSET) { // can be executed anytime
				// do nothing as it can be executed anytime
			} else { // normal input binding
				for (Short reqInput : codec.getIOBindingsFor(binding.getEncodedInputBinding())) {
					isAppropriate = isAppropriate
							&& res.removeCurrObligation(new Pair<Short, Short>(reqInput, binding.getEncodedNode()));
				}
			}

			if (!isAppropriate && !allowExecViolating) {
				continue iterateBinding;
			}

			res.setCurrIndexOnTrace(currNode.getCurrIndexOnTrace() + 1);

			if ((isDuplicate) || (possibleNodeBindings.size() > 1)) {
				res.getDuplicatesOnlyStep().add(
						new Pair<Integer, Short>(currNode.getCurrIndexOnTrace() + currNode.getModelOnlyStep().size(),
								nodeBinding));
			}

			if ((binding.getEncodedOutputBinding() != FlexCodec.BLANK)
					&& (binding.getEncodedOutputBinding() != FlexCodec.EMPTYSET)) {
				for (Short out : codec.getIOBindingsFor(binding.getEncodedOutputBinding())) {
					res.addCurrObligation(new Pair<Short, Short>(candidate, out));
				}
			}

			// update cost
			int cost = currNode.getCost();
			if (!isAppropriate) {
				cost += inappropriateTransFireCost;
			}
			cost = cost + replayedEventCost - heuristicDistanceCost;
			res.setCost(cost);

			id++;
			if (!pq.contains(res)) {
				pq.add(res);
			}
		}

		return id;
	}

	private CFlexExtendedCostBasedAStar createEncNodeByMoveTrace(int traceSize, CFlexExtendedCostBasedAStar currNode) {
		CFlexExtendedCostBasedAStar res = new CFlexExtendedCostBasedAStar(currNode);
		res.setCurrIndexOnTrace(currNode.getCurrIndexOnTrace() + 1);
		res.getMoveTraceOnlyStep().add(currNode.getCurrIndexOnTrace() + currNode.getModelOnlyStep().size());

		// cost
		int cost = currNode.getCost() + skippedEventCost - heuristicDistanceCost + replayedEventCost;
		res.setCost(cost);

		return res;
	}

	@Override
	public String replayLogForAnalysis(PluginContext context, Flex flex, StartTaskNodesSet startTaskNodesSet, XLog log,
			Collection<Pair<FlexNode, XEventClass>> mapping, Object[] parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CaseReplayResult replayXTrace(PluginContext context, Flex flex, StartTaskNodesSet startTaskNodesSet,
			XTrace trace, XLogInfo logInfo, XEventClasses classes, Collection<Pair<FlexNode, XEventClass>> mapping,
			Object[] parameters) {

		// check codec
		FlexCodec codec = null;
		try {
			codec = (FlexCodec) context.getConnectionManager().getFirstConnection(FlexCodecConnection.class, context,
					flex).getObjectWithRole(FlexCodecConnection.FLEXCODEC);
		} catch (ConnectionCannotBeObtained e) {
			codec = new FlexCodec(flex);
			context.addConnection(new FlexCodecConnection("Connection to FlexCodec of " + flex.getLabel(), flex, codec));
		} catch (Exception e) {
			e.printStackTrace();
			codec = new FlexCodec(flex);
			context.addConnection(new FlexCodecConnection("Connection to FlexCodec of " + flex.getLabel(), flex, codec));
		}
		this.codec = codec;

		// obtain parameters
		createEncodedFlexAndWorstPossibility(flex);
		createEncodedMappingAndEventEncoding(mapping);
		List<Short> encodedTrace = createTraceAbstraction(trace, classes);
		importParameters(parameters);
		final Set<Short> setInviTasks = codec.getInvisNodes();

		// get initial start task node
		final Set<Short> setStartTaskNodes = new HashSet<Short>(2); // only one node initially enabled
		for (FlexNode initNode : startTaskNodesSet.iterator().next()) {
			setStartTaskNodes.add(codec.encode(initNode));
		}

		if (maxNumOfStates != Integer.MAX_VALUE) {
			context.log("Starting replay with max state " + maxNumOfStates + "...");
		} else {
			context.log("Starting replay with no limit for max explored state...");
		}

		CaseReplayResult res = replayInEncodedFormForFITgraph(context, flex, encodedTrace, setInviTasks,
				setStartTaskNodes);
		res.setTrace(trace);
		res.setLogInfo(logInfo);

		context.log("Replay is finished");

		return res;
	}

	/**
	 * trace only consists events that are mappable on the flexible model
	 * 
	 * @param context
	 * @param trace
	 * @param maxNumOfStates
	 * @param unsatisfiedEventCost
	 * @param heuristicDistanceCost
	 * @param skippedEventCost
	 * @param replayedEventCost
	 * @param selfExecInviTaskCost
	 * @param selfExecRealTaskCost
	 * @param accUnhArcCost
	 * @param unhArcCost
	 * @param flex
	 * @param setInviTasks
	 * @return
	 */
	private CaseReplayResult replayInEncodedFormForFITgraph(PluginContext context, Flex flex, List<Short> trace,
			Set<Short> setInviTasks, Set<Short> encodedStartTaskNodesSet) {
		// utility variables
		int traceSize = trace.size();

		ExtendedCostBasedFIT costBasedFIT = ExtendedCostBasedFITFactory.newCostBasedFIT("FIT");
		ExtendedCostBasedFITNode costBasedFITNode = costBasedFIT.addNode("root");
		costBasedFITNode.setSequenceID("0");
		costBasedFITNode.setHeuristicDistance(traceSize);
		costBasedFITNode.setCurrIndexOnTrace(0);
		costBasedFITNode.setCost(traceSize * heuristicDistanceCost);

		assert (encodedStartTaskNodesSet.size() == 1); // only one set of start task nodes are possible

		PriorityQueue<ExtendedCostBasedFITNode> costBasedFITPQ = new PriorityQueue<ExtendedCostBasedFITNode>();

		// init
		int id = 1;
		long startTime = System.nanoTime();
		do {
			if (allowEventSkip) {
				// skip current event(Move on Log only)
				costBasedFITPQ.add(createNodeByMoveTrace(traceSize, costBasedFIT, costBasedFITNode, id));
				id++;
			}

			// execute current event(Move on both Log/Model)
			for (Short candidateNode : eventToFlex.get(trace.get(costBasedFITNode.getCurrIndexOnTrace()))) {
				FlexNodeEncodedPredSuc sucPred = encodedFlex.get(candidateNode);
				assert (sucPred != null);

				// assumption: start task nodes should not have any predecessor
				if (encodedStartTaskNodesSet.contains(candidateNode)) {
					boolean isUnsatisfied = costBasedFITNode.getStartingNodes().contains(candidateNode); 
					if (isUnsatisfied && !allowExecViolating) {
						// no need to proceed if executing this step is violating and is not allowed
					} else {// just execute, it can also be violating
						MultiSet<Pair<Short, Short>> newUnhandledArcs = new HashMultiSet<Pair<Short, Short>>();
						newUnhandledArcs.addAll(costBasedFITNode.getUnhandledArcs());
						if (sucPred.getEncodedSuccessor().size() > 0) {
							// no predecessor. yes successor.
							for (List<Short> succ : sucPred.getEncodedSuccessor()) {
								costBasedFITPQ.add(createNodeByExecuteEvent(candidateNode, null, succ, traceSize,
										costBasedFIT, costBasedFITNode, id, true, isUnsatisfied, newUnhandledArcs));
								id++;
							}
						} else {
							// no predecessor. no successor.
							costBasedFITPQ.add(createNodeByExecuteEvent(candidateNode, null, null, traceSize,
									costBasedFIT, costBasedFITNode, id, true, isUnsatisfied, newUnhandledArcs));
							id++;
						}
					}
				} else

				// not a starting task node
				if (sucPred.getEncodedPredecessor().size() > 0) {
					usePredecessor: for (List<Short> pred : sucPred.getEncodedPredecessor()) {
						MultiSet<Pair<Short, Short>> newUnhandledArcs = new HashMultiSet<Pair<Short, Short>>();
						newUnhandledArcs.addAll(costBasedFITNode.getUnhandledArcs());
						
						boolean isUnsatisfied = false;
						for (Short predElmt : pred) {
							if (!newUnhandledArcs.remove(new Pair<Short, Short>(predElmt, candidateNode))){
								isUnsatisfied = true;	
							}
							if (isUnsatisfied && !allowExecViolating) {
								continue usePredecessor;
							}
						}

						// does not care if it is violating
						if (sucPred.getEncodedSuccessor().size() > 0) {
							// yes predecessor. yes successor.
							for (List<Short> succ : sucPred.getEncodedSuccessor()) {
								MultiSet<Pair<Short, Short>> tempUnhandledArcs = new HashMultiSet<Pair<Short, Short>>();
								tempUnhandledArcs.addAll(newUnhandledArcs);
								costBasedFITPQ.add(createNodeByExecuteEvent(candidateNode, pred, succ, traceSize,
										costBasedFIT, costBasedFITNode, id, false, isUnsatisfied, tempUnhandledArcs));
								id++;
							}
						} else {
							// yes predecessor. no successor.
							costBasedFITPQ.add(createNodeByExecuteEvent(candidateNode, pred, null, traceSize,
									costBasedFIT, costBasedFITNode, id, false, isUnsatisfied, newUnhandledArcs));
							id++;
						}
					}
				}
			}

			// execute any possible transitions (including both invisible and real transitions) 
			if (allowInviTaskMove || allowRealTaskMove) {
				Collection<? extends ExtendedCostBasedFITNode> newExtendedCostBasedFITNodes = createNodeByExecutingPossibleTaskExec(
						flex, encodedStartTaskNodesSet, setInviTasks, costBasedFIT, costBasedFITNode, id);
				id += newExtendedCostBasedFITNodes.size();
				costBasedFITPQ.addAll(newExtendedCostBasedFITNodes);
			}

			// continue to the next node
			costBasedFITNode = costBasedFITPQ.poll();
		} while ((costBasedFITNode.getHeuristicDistance() != 0) && (id < maxNumOfStates));

		long period = System.nanoTime() - startTime;

		// create table info
		Map<String, String> infoTable = new HashMap<String, String>();

		infoTable.put("#Inserted activities", String.valueOf(costBasedFITNode.getNumberOfSkippedEvts()));
		infoTable.put("#Skipped activities", String.valueOf(costBasedFITNode.getNumberOfSelfExecRealTasks()));
		infoTable.put("#Unobservable activities", String.valueOf(costBasedFITNode.getNumberOfSelfExecInviTasks()));
		infoTable.put("#Synchronous activities (log+model)", String.valueOf(costBasedFITNode
				.getNumberOfReplayedEvents()
				- costBasedFITNode.getNumberOfSkippedEvts()));
		infoTable.put("#Unsatisfied events", String.valueOf(costBasedFITNode.getNumberOfUnsatisfiedEvents()));
		infoTable.put("#Explored instances", String.valueOf(id - 1));
		infoTable.put("Computation time (ns)", String.valueOf(period));

		int totalCostForFitness = 0;
		if (allowEventSkip) {
			infoTable.put("Cost of an inserted activity", String.valueOf(skippedEventCost));
			totalCostForFitness += (skippedEventCost * costBasedFITNode.getNumberOfSkippedEvts());
		}
		if (allowExecViolating) {
			infoTable.put("Cost of a violating sync. activity", String.valueOf(inappropriateTransFireCost));
			totalCostForFitness += (inappropriateTransFireCost * costBasedFITNode.getNumberOfUnsatisfiedEvents());
		}
		if (allowRealTaskMove) {
			infoTable.put("Cost of a skipped activity", String.valueOf(selfExecRealTaskCost));
			totalCostForFitness += (selfExecRealTaskCost * costBasedFITNode.getNumberOfSelfExecRealTasks());
		}
		if (allowInviTaskMove) {
			infoTable.put("Cost of an unobservable activity", String.valueOf(selfExecInviTaskCost));
		}
		infoTable.put("Heuristic cost for an event", String.valueOf(heuristicDistanceCost));

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(3);
		nf.setMinimumFractionDigits(3);

		// assumption: either skipping events, or violating events (or even both) must be possible;
		int worstCaseCost = skippedEventCost;
		if (allowEventSkip && allowExecViolating) {
			worstCaseCost = skippedEventCost < inappropriateTransFireCost ? skippedEventCost
					: inappropriateTransFireCost;
		} else if (allowEventSkip) {
			worstCaseCost = skippedEventCost;
		} else { // only allow violations, but no event skip
			worstCaseCost = inappropriateTransFireCost;
		}

		infoTable.put("Cost based fitness", nf
				.format(1 - ((double) (totalCostForFitness) / (double) (traceSize * worstCaseCost))));

		// case task ratio fitness
		infoTable.put("Ratio fitting events", nf
				.format((double) (costBasedFITNode.getNumberOfReplayedEvents() - costBasedFITNode
						.getNumberOfUnsatisfiedEvents())
						/ (double) (costBasedFITNode.getNumberOfReplayedEvents())));
		if (id > maxNumOfStates) {
			infoTable.put("Finish replay", "No");
		} else {
			infoTable.put("Finish replay", "Yes");
		}

		// calculate necessary information
		List<Object> nodeInstance = new LinkedList<Object>();
		List<StepTypes> stepTypes = new LinkedList<StepTypes>();

		// color the path to the root and accumulate result
		do {
			costBasedFITNode.getAttributeMap().put(AttributeMap.FILLCOLOR,
					ExtendedCostBasedAStarLogReplayAlgorithm.ORANGE);
			Collection<ExtendedCostBasedFITEdge<ExtendedCostBasedFITNode, ExtendedCostBasedFITNode>> edges = costBasedFIT
					.getInEdges(costBasedFITNode);
			if (edges.size() > 0) {
				for (ExtendedCostBasedFITEdge<ExtendedCostBasedFITNode, ExtendedCostBasedFITNode> edge : edges) { // there's only one incoming edge
					// print to console

					edge.getAttributeMap().put(AttributeMap.LINEWIDTH, new Float(5.0));
					edge.getAttributeMap().put(AttributeMap.SHOWLABEL, true);
					costBasedFITNode = edge.getSource();

					// update nodeInstance and stepTypes
					nodeInstance.add(0, edge.getSelectedNodeInstance());
					if (edge.getInfo().startsWith("M")) {
						if (edge.getSource().getNumberOfSelfExecRealTasks() < edge.getTarget()
								.getNumberOfSelfExecRealTasks()) {
							edge.getAttributeMap().put(AttributeMap.EDGECOLOR,
									ExtendedCostBasedAStarLogReplayAlgorithm.PURPLE);
							stepTypes.add(0, StepTypes.MREAL);
						} else {
							edge.getAttributeMap().put(AttributeMap.EDGECOLOR,
									ExtendedCostBasedAStarLogReplayAlgorithm.GRAY);
							stepTypes.add(0, StepTypes.MINVI);
						}
					} else if (edge.getInfo().startsWith("L/M")) {
						if (edge.getSource() != null) {
							if (edge.getSource().getNumberOfUnsatisfiedEvents() < edge.getTarget()
									.getNumberOfUnsatisfiedEvents()) {
								edge.getAttributeMap().put(AttributeMap.EDGECOLOR,
										ExtendedCostBasedAStarLogReplayAlgorithm.RED);
								stepTypes.add(0, StepTypes.LMNOGOOD);
							} else {
								edge.getAttributeMap().put(AttributeMap.EDGECOLOR,
										ExtendedCostBasedAStarLogReplayAlgorithm.GREEN);
								stepTypes.add(0, StepTypes.LMGOOD);
							}
						}
					} else {
						edge.getAttributeMap().put(AttributeMap.EDGECOLOR,
								ExtendedCostBasedAStarLogReplayAlgorithm.YELLOW);
						stepTypes.add(0, StepTypes.L);
					}
				}
			} else {
				costBasedFITNode = null;
			}
		} while (costBasedFITNode != null);

		CaseReplayResult res = new CaseReplayResult(costBasedFIT, infoTable);
		res.setNodeInstance(nodeInstance);
		res.setStepTypes(stepTypes);

		return res;
	}

	/**
	 * 
	 * @param flex
	 * @param setInviTasks
	 * @param costBasedFIT
	 * @param costBasedFITNode
	 * @param heuristicDistanceCost
	 * @param replayedEventCost
	 * @param accUnhArcCost
	 * @param unhArcCost
	 * @param selfExecInviTaskCost
	 * @param selfExecRealTaskCost
	 * @param givenID
	 * @return
	 */
	private Collection<? extends ExtendedCostBasedFITNode> createNodeByExecutingPossibleTaskExec(Flex flex,
			Set<Short> encodedStartTaskNodesSet, Set<Short> setInviTasks, ExtendedCostBasedFIT costBasedFIT,
			ExtendedCostBasedFITNode costBasedFITNode, int givenID) {

		// get all possible transitions
		Set<FlexExtendedEncodedState> possibleExtensions = getPossibleTaskExec(flex, encodedStartTaskNodesSet,
				costBasedFITNode.getUnhandledArcs(), costBasedFITNode);

		// result
		Set<ExtendedCostBasedFITNode> resultSet = new HashSet<ExtendedCostBasedFITNode>();
		int id = givenID;

		for (FlexExtendedEncodedState selectedState : possibleExtensions) {
			resultSet.add(createNodeByMoveModel(selectedState.getNewlySelectedFlexNode(), selectedState
					.getSelectedInputSet(), selectedState.getSelectedOutputSet(), costBasedFIT, costBasedFITNode, id,
					setInviTasks, selectedState.getIsMoveOnStartingNode()));
			id++;
		}
		return resultSet;
	}

	/**
	 * 
	 * @param flex
	 * @param unhandledArcs
	 * @return
	 */
	private Set<FlexExtendedEncodedState> getPossibleTaskExec(Flex flex, Set<Short> encodedStartTaskNodesSet,
			MultiSet<Pair<Short, Short>> unhandledArcs, ExtendedCostBasedFITNode costBasedFITNode) {
		Map<Short, Set<Short>> enablingTasks = new HashMap<Short, Set<Short>>();

		// possible task exec include executing tasks with no predecessor, and expected tasks from unhandledArcs
		if (unhandledArcs.size() > 0) {
			for (Pair<Short, Short> pair : unhandledArcs) {
				if (enablingTasks.containsKey(pair.getSecond())) {
					enablingTasks.get(pair.getSecond()).add(pair.getFirst());
				} else {
					Set<Short> newSet = new HashSet<Short>();
					newSet.add(pair.getFirst());
					enablingTasks.put(pair.getSecond(), newSet);
				}
			}
		}

		Set<FlexExtendedEncodedState> result = new HashSet<FlexExtendedEncodedState>();
		for (FlexNode node : flex.getNodes()) {
			short encodedNode = codec.encode(node);
			if ((!allowInviTaskMove && node.isInvisible()) || (!allowRealTaskMove && !node.isInvisible())) {
				continue;
			}

			Set<Short> providedTask = enablingTasks.get(encodedNode);
			/*
			 * if node is starting stask node, then always execute it(if....)
			 */
			if (encodedStartTaskNodesSet.contains(encodedNode)) {
				Set<Short> currentStartingNodes = costBasedFITNode.getStartingNodes();
				/*
				 * We only execute starting node if it has not been executed
				 * before.
				 */
				if (!currentStartingNodes.contains(encodedNode)) {

					for (SetFlex output : node.getOutputNodes()) {
						if (output.size() == 0) {
							FlexExtendedEncodedState flexState = new FlexExtendedEncodedState(encodedNode, null,
									null, null);
							flexState.setIsMoveOnStartingNode(true);
							result.add(flexState);
						} else {
							// encode first
							List<Short> encodedOutput = new LinkedList<Short>();
							for (FlexNode outputNode : output) {
								encodedOutput.add(codec.encode(outputNode));
							}

							FlexExtendedEncodedState flexState = new FlexExtendedEncodedState(encodedNode, null,
									encodedOutput, null);
							flexState.setIsMoveOnStartingNode(true);
							result.add(flexState);
						}
					}
				}
			}

			checkInputNodes: for (SetFlex input : node.getInputNodes()) {
				if (input.size() == 0) {
					//					if (!encodedStartTaskNodesSet.contains(encodedNode)) {
					// can be executed without predecessor
					// execute for all successors
					for (SetFlex output : node.getOutputNodes()) {
						if (output.size() == 0) {
							result.add(new FlexExtendedEncodedState(encodedNode, null, null, null));
						} else {
							// encode first
							List<Short> encodedOutput = new LinkedList<Short>();
							for (FlexNode outputNode : output) {
								encodedOutput.add(codec.encode(outputNode));
							}
							result.add(new FlexExtendedEncodedState(encodedNode, null, encodedOutput, null));
						}
					}
					//					}
				} else { // number of required input tasks is more than 0
					if (providedTask != null) {
						List<Short> encodedInput = new LinkedList<Short>();
						// encode required input tasks
						for (FlexNode inputNode : input) {
							if (!providedTask.contains(codec.encode(inputNode))) {
								continue checkInputNodes;
							} else {
								encodedInput.add(codec.encode(inputNode));
							}
							;
						}

						// all required input nodes are available

						// execute for all successors
						for (SetFlex output : node.getOutputNodes()) {
							if (output.size() == 0) {
								result.add(new FlexExtendedEncodedState(encodedNode, encodedInput, null, null));
							} else {
								// encode first
								List<Short> encodedOutput = new LinkedList<Short>();
								for (FlexNode outputNode : output) {
									encodedOutput.add(codec.encode(outputNode));
								}
								result
										.add(new FlexExtendedEncodedState(encodedNode, encodedInput, encodedOutput,
												null));
							}
						}
					}
				}
			}
		}

		return result;
	}

	/**
	 * 
	 * @param traceSize
	 * @param costBasedFIT
	 * @param costBasedFITNode
	 * @param heuristicDistanceCost
	 * @param skippedEventCost
	 * @param replayedEventCost
	 * @param accUnhArcCost
	 * @param id
	 * @return
	 */
	private ExtendedCostBasedFITNode createNodeByMoveTrace(int traceSize, ExtendedCostBasedFIT costBasedFIT,
			ExtendedCostBasedFITNode costBasedFITNode, int id) {
		// move event sequence forward, but not the model (skipping one event) 
		ExtendedCostBasedFITNode newExtendedCostBasedFITNode = costBasedFIT.addNode(String.valueOf(id));

		newExtendedCostBasedFITNode.setSequenceID(String.valueOf(id));
		newExtendedCostBasedFITNode.setNumberOfUnsatisfiedEvents(costBasedFITNode.getNumberOfUnsatisfiedEvents());
		newExtendedCostBasedFITNode.setNumberOfReplayedEvents(costBasedFITNode.getNumberOfReplayedEvents());
		newExtendedCostBasedFITNode.setNumberOfSkippedEvts(costBasedFITNode.getNumberOfSkippedEvts() + 1);
		newExtendedCostBasedFITNode.setNumberOfSelfExecRealTasks(costBasedFITNode.getNumberOfSelfExecRealTasks());
		newExtendedCostBasedFITNode.setNumberOfSelfExecInviTasks(costBasedFITNode.getNumberOfSelfExecInviTasks());

		Set<Short> newStartingNode = new HashSet<Short>(costBasedFITNode.getStartingNodes());
		newExtendedCostBasedFITNode.setStartingNodes(newStartingNode);
		
		newExtendedCostBasedFITNode.setHeuristicDistance(costBasedFITNode.getHeuristicDistance() - 1);
		newExtendedCostBasedFITNode.setCurrIndexOnTrace(costBasedFITNode.getCurrIndexOnTrace() + 1);

		newExtendedCostBasedFITNode.setUnhandledArcs(new HashMultiSet<Pair<Short, Short>>(costBasedFITNode
				.getUnhandledArcs()));

		// calculate costs
		int newCost = costBasedFITNode.getCost() + replayedEventCost + skippedEventCost - heuristicDistanceCost;
		newExtendedCostBasedFITNode.setCost(newCost);

		// attach the node as child of the previous node
		ExtendedCostBasedFITEdge<ExtendedCostBasedFITNode, ExtendedCostBasedFITNode> edge = costBasedFIT.addArc(
				costBasedFITNode, newExtendedCostBasedFITNode);
		edge.setInfo("L");
		edge.setSelectedNodeInstance("[" + (costBasedFITNode.getCurrIndexOnTrace()) + "]");
		edge.getAttributeMap().put(AttributeMap.EDGECOLOR, ExtendedCostBasedAStarLogReplayAlgorithm.LIGHTYELLOW);

		return newExtendedCostBasedFITNode;
	}

	/**
	 * 
	 * @param candidateNode
	 * @param pred
	 * @param succ
	 * @param costBasedFIT
	 * @param costBasedFITNode
	 * @param heuristicDistanceCost
	 * @param replayedEventCost
	 * @param accUnhArcCost
	 * @param unhArcCost
	 * @param selfExecInviTaskCost
	 * @param selfExecRealTaskCost
	 * @param id
	 * @param setInviTasks
	 * @param isMoveOnStartingNode
	 * @return
	 */
	private ExtendedCostBasedFITNode createNodeByMoveModel(short candidateNode, List<Short> pred, List<Short> succ,
			ExtendedCostBasedFIT costBasedFIT, ExtendedCostBasedFITNode costBasedFITNode, int id,
			Set<Short> setInviTasks, Boolean isMoveOnStartingNode) {

		// it is guaranteed before that no unhandled events occur because of executing candidateNode 
		// move model forward, but not the event sequence 
		int newCost = costBasedFITNode.getCost();
		ExtendedCostBasedFITNode newExtendedCostBasedFITNode = costBasedFIT.addNode(String.valueOf(id));
		newExtendedCostBasedFITNode.setSequenceID(String.valueOf(id));

		newExtendedCostBasedFITNode.setNumberOfUnsatisfiedEvents(costBasedFITNode.getNumberOfUnsatisfiedEvents());
		newExtendedCostBasedFITNode.setNumberOfReplayedEvents(costBasedFITNode.getNumberOfReplayedEvents());
		newExtendedCostBasedFITNode.setNumberOfSkippedEvts(costBasedFITNode.getNumberOfSkippedEvts());

		boolean inviTask = false;
		if (setInviTasks.contains(candidateNode)) {
			newExtendedCostBasedFITNode
					.setNumberOfSelfExecInviTasks(costBasedFITNode.getNumberOfSelfExecInviTasks() + 1);
			newExtendedCostBasedFITNode.setNumberOfSelfExecRealTasks(costBasedFITNode.getNumberOfSelfExecRealTasks());
			newCost += selfExecInviTaskCost;
			inviTask = true;
		} else {
			newExtendedCostBasedFITNode.setNumberOfSelfExecInviTasks(costBasedFITNode.getNumberOfSelfExecInviTasks());
			newExtendedCostBasedFITNode
					.setNumberOfSelfExecRealTasks(costBasedFITNode.getNumberOfSelfExecRealTasks() + 1);
			newCost += selfExecRealTaskCost;
		}

		newExtendedCostBasedFITNode.setHeuristicDistance(costBasedFITNode.getHeuristicDistance());
		newExtendedCostBasedFITNode.setCurrIndexOnTrace(costBasedFITNode.getCurrIndexOnTrace());

		// calculate new unhandled arcs
		MultiSet<Pair<Short, Short>> newUnhandledArcs = new HashMultiSet<Pair<Short, Short>>();
		newUnhandledArcs.addAll(costBasedFITNode.getUnhandledArcs());
		//Preserve starting nodes
		Set<Short> newStartingNodes = new HashSet<Short>();
		newStartingNodes.addAll(costBasedFITNode.getStartingNodes());

		if (pred != null) {
			for (Short predElmt : pred) {
				Pair<Short, Short> temp = new Pair<Short, Short>(predElmt, candidateNode);
				newUnhandledArcs.remove(temp);
			}
		}
		if (succ != null) {
			for (Short succElmt : succ) {
				Pair<Short, Short> temp = new Pair<Short, Short>(candidateNode, succElmt);
				newUnhandledArcs.add(temp);
			}
		}

		// attach the node as child of the previous node
		ExtendedCostBasedFITEdge<ExtendedCostBasedFITNode, ExtendedCostBasedFITNode> edge = costBasedFIT.addArc(
				costBasedFITNode, newExtendedCostBasedFITNode);
		edge.setInfo("M");
		if (isMoveOnStartingNode) {
			/*
			 * If we move on a starting node, then we should mention it
			 * explicitly in the edge label and we should update the FITNode
			 */
			newStartingNodes.add(candidateNode);
			Iterator<Short> it;
			String selectedString = "INIT {} " + codec.decode(candidateNode) + "  {";
			if (succ != null) {
				it = succ.iterator();
				while (it.hasNext()) {
					selectedString += codec.decode(it.next());
					if (it.hasNext()) {
						selectedString += ",";
					}
				}
			}
			selectedString += "}";
			edge.setSelectedNodeInstance(selectedString);
		} else {
			String selectedNodeInstance = "{";
			Iterator<Short> it;
			if (pred != null) {
				it = pred.iterator();
				while (it.hasNext()) {
					selectedNodeInstance += codec.decode(it.next());
					if (it.hasNext()) {
						selectedNodeInstance += ",";
					}
				}
			}
			selectedNodeInstance += "}  " + codec.decode(candidateNode) + "  {";
			if (succ != null) {
				it = succ.iterator();
				while (it.hasNext()) {
					selectedNodeInstance += codec.decode(it.next());
					if (it.hasNext()) {
						selectedNodeInstance += ",";
					}
				}
			}
			selectedNodeInstance += "}";

			edge.setSelectedNodeInstance(selectedNodeInstance);
		}
		/*
		 * Update new node's attributes
		 */
		newExtendedCostBasedFITNode.setStartingNodes(newStartingNodes);
		newExtendedCostBasedFITNode.setUnhandledArcs(newUnhandledArcs);
		newExtendedCostBasedFITNode.setCost(newCost);

		if (inviTask) {
			edge.getAttributeMap().put(AttributeMap.EDGECOLOR, ExtendedCostBasedAStarLogReplayAlgorithm.LIGHTGRAY);
		} else {
			edge.getAttributeMap().put(AttributeMap.EDGECOLOR, ExtendedCostBasedAStarLogReplayAlgorithm.LIGHTPURPLE);
		}
		return newExtendedCostBasedFITNode;
	}

	/**
	 * 
	 * @param candidateNode
	 * @param pred
	 * @param succ
	 * @param traceSize
	 * @param costBasedFIT
	 * @param costBasedFITNode
	 * @param unsatisfiedEventCost
	 * @param heuristicDistanceCost
	 * @param replayedEventCost
	 * @param accUnhArcCost
	 * @param unhArcCost
	 * @param id
	 * @return
	 */
	private ExtendedCostBasedFITNode createNodeByExecuteEvent(Short candidateNode, List<Short> pred, List<Short> succ,
			int traceSize, ExtendedCostBasedFIT costBasedFIT, ExtendedCostBasedFITNode costBasedFITNode, int id,
			Boolean isExecutingStartingNode, boolean isUnsatisfied, MultiSet<Pair<Short, Short>> newUnhandledArcs) {
		ExtendedCostBasedFITNode newExtendedCostBasedFITNode = costBasedFIT.addNode(String.valueOf(id));

		int newCost = costBasedFITNode.getCost();

		newExtendedCostBasedFITNode.setSequenceID(String.valueOf(id));
		newExtendedCostBasedFITNode.setNumberOfReplayedEvents(costBasedFITNode.getNumberOfReplayedEvents() + 1);
		newExtendedCostBasedFITNode.setNumberOfSkippedEvts(costBasedFITNode.getNumberOfSkippedEvts());
		newExtendedCostBasedFITNode.setNumberOfSelfExecRealTasks(costBasedFITNode.getNumberOfSelfExecRealTasks());
		newExtendedCostBasedFITNode.setNumberOfSelfExecInviTasks(costBasedFITNode.getNumberOfSelfExecInviTasks());

		newExtendedCostBasedFITNode.setHeuristicDistance(costBasedFITNode.getHeuristicDistance() - 1);
		newExtendedCostBasedFITNode.setCurrIndexOnTrace(costBasedFITNode.getCurrIndexOnTrace() + 1);
		newCost += (replayedEventCost - heuristicDistanceCost);

		/*
		 * Use the provided unhandledArcs
		 */

		/*
		 * Preserve the executed starting tasks
		 */
		Set<Short> newStartingNodes = new HashSet<Short>();
		newStartingNodes.addAll(costBasedFITNode.getStartingNodes());

		// update unhandled arcs 
		String selectedSuccessorsString = "";
		if (succ != null) {
			char limiter = ' ';
			for (Short succElmt : succ) {
				newUnhandledArcs.add(new Pair<Short, Short>(candidateNode, succElmt));
				selectedSuccessorsString += (limiter + codec.decode(succElmt).getLabel());
				limiter = ',';
			}
		} else {
			selectedSuccessorsString = "Empty";
		}

		// attach the node as child of the previous node
		ExtendedCostBasedFITEdge<ExtendedCostBasedFITNode, ExtendedCostBasedFITNode> edge = costBasedFIT.addArc(
				costBasedFITNode, newExtendedCostBasedFITNode);
		edge.setInfo("L/M");
		String selectedNodeInstance = "[" + costBasedFITNode.getCurrIndexOnTrace() + "]  ";
		Iterator<Short> it;
		if (isExecutingStartingNode) {
			newStartingNodes.add(candidateNode);
			selectedNodeInstance += "INIT- " + codec.decode(candidateNode) + " {";
		} else {
			if (pred != null) {
				selectedNodeInstance += "{";
				it = pred.iterator();
				while (it.hasNext()) {
					selectedNodeInstance += codec.decode(it.next()).getLabel();
					if (it.hasNext()) {
						selectedNodeInstance += ",";
					}
				}
			} else {
				selectedNodeInstance += "{Empty";
			}
			selectedNodeInstance += "}  " + codec.decode(candidateNode) + " {";
		}

		selectedNodeInstance += (selectedSuccessorsString + "}");
		edge.setSelectedNodeInstance(selectedNodeInstance);

		// penalty for unsatisfied events
		if (isUnsatisfied) {
			newExtendedCostBasedFITNode
					.setNumberOfUnsatisfiedEvents(costBasedFITNode.getNumberOfUnsatisfiedEvents() + 1);
			newCost += inappropriateTransFireCost;
			edge.getAttributeMap().put(AttributeMap.EDGECOLOR, ExtendedCostBasedAStarLogReplayAlgorithm.LIGHTRED);
		} else {
			newExtendedCostBasedFITNode.setNumberOfUnsatisfiedEvents(costBasedFITNode.getNumberOfUnsatisfiedEvents());
			edge.getAttributeMap().put(AttributeMap.EDGECOLOR, ExtendedCostBasedAStarLogReplayAlgorithm.LIGHTGREEN);
		}

		/*
		 * Update newExtendedCostBasedFITNode's attributes
		 */
		newExtendedCostBasedFITNode.setUnhandledArcs(newUnhandledArcs);
		newExtendedCostBasedFITNode.setStartingNodes(newStartingNodes);
		newExtendedCostBasedFITNode.setCost(newCost);

		return newExtendedCostBasedFITNode;
	}

	/**
	 * 
	 * @param flex
	 */
	private void createEncodedFlexAndWorstPossibility(Flex flex) {
		for (FlexNode node : flex.getNodes()) {
			// create flex abstraction
			Set<SetFlex> setSetFlexNodeIn = node.getInputNodes();
			List<List<Short>> setSetFlexNodeEncodedIn = new LinkedList<List<Short>>();
			for (Set<FlexNode> setFlexNode : setSetFlexNodeIn) {
				List<Short> setFlexNodeEncoded = new LinkedList<Short>();
				for (FlexNode inputnode : setFlexNode) {
					setFlexNodeEncoded.add(codec.encode(inputnode));
				}
				setSetFlexNodeEncodedIn.add(setFlexNodeEncoded);
			}

			Set<SetFlex> setSetFlexNodeOut = node.getOutputNodes();
			List<List<Short>> setSetFlexNodeEncodedOut = new LinkedList<List<Short>>();
			for (Set<FlexNode> setFlexNode : setSetFlexNodeOut) {
				List<Short> setFlexNodeEncoded = new LinkedList<Short>();
				for (FlexNode outputNode : setFlexNode) {
					setFlexNodeEncoded.add(codec.encode(outputNode));
				}
				setSetFlexNodeEncodedOut.add(setFlexNodeEncoded);
			}
			encodedFlex.put(codec.encode(node), new FlexNodeEncodedPredSuc(setSetFlexNodeEncodedIn,
					setSetFlexNodeEncodedOut));

			// calculate possibilities
			short possibilities = (short) setSetFlexNodeEncodedIn.size();
			if (possibilities == 0) {
				possibilities = 1;
			}
			if (setSetFlexNodeEncodedOut.size() > 1) {
				possibilities = (short) (possibilities * setSetFlexNodeEncodedOut.size());
			}
			worstPossibilityMap.put(codec.encode(node), possibilities);
		}
	}

	/**
	 * 
	 * @param mapping
	 */
	private void createEncodedMappingAndEventEncoding(Collection<Pair<FlexNode, XEventClass>> mapping) {
		short eventClassId = Short.MIN_VALUE;
		Map<XEventClass, Short> evtClassEncode = new HashMap<XEventClass, Short>();
		for (Pair<FlexNode, XEventClass> pair : mapping) {
			if (pair.getSecond() != null) {
				short encodedNode = codec.encode(pair.getFirst());
				short encodedEventClass;
				if (!evtClassEncode.containsKey(pair.getSecond())) {
					evtClassEncode.put(pair.getSecond(), eventClassId);
					encodedEventClass = eventClassId;
					eventClassId++;
				} else {
					encodedEventClass = evtClassEncode.get(pair.getSecond());
				}

				flexToEvent.put(encodedNode, encodedEventClass);

				if (eventToFlex.containsKey(encodedEventClass)) {
					eventToFlex.get(encodedEventClass).add(encodedNode);
				} else {
					List<Short> temp = new LinkedList<Short>();
					temp.add(encodedNode);
					eventToFlex.put(encodedEventClass, temp);
				}
			}
		}

		// create event encoder
		this.ee = new EventEncoder(evtClassEncode);
	}

	/**
	 * 
	 * @param trace
	 * @param classes
	 * @return
	 */
	private List<Short> createTraceAbstraction(XTrace trace, XEventClasses classes) {
		// assume that timestamp is integer
		List<Short> listTrace = new LinkedList<Short>();

		int timeCount = 1; // dummy, TODO: check if timestamp exist
		for (XEvent event : trace) {
			// if no flex node is mapped to this event, ignore the event
			XEventClass eventClass = classes.getClassOf(event);
			if (ee.encode(eventClass) != null) {
				listTrace.add(ee.encode(eventClass));
				timeCount++;
			}
		}

		return listTrace;
	}

}
