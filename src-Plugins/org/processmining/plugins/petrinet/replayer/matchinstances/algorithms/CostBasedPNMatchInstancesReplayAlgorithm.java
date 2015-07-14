/**
 * 
 */
package org.processmining.plugins.petrinet.replayer.matchinstances.algorithms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.Progress;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.petrinet.replayer.algorithms.costbasedprefix.OptimizedCostBasedPNReplayAlgorithm;
import org.processmining.plugins.petrinet.replayer.util.LogCounterPrecSyncReplay;
import org.processmining.plugins.petrinet.replayer.util.codec.PNCodec;
import org.processmining.plugins.petrinet.replayer.util.statespaces.CPNCostBasedTreeNodeX;
import org.processmining.plugins.petrinet.replayresult.PNMatchInstancesRepResult;
import org.processmining.plugins.replayer.util.StepTypes;

/**
 * Cost-based replay using the A*-algorithm to produce all best matching instances
 * 
 * NOTE: This algorithm ignores final marking
 * 
 * @author aadrians
 * 
 */
public class CostBasedPNMatchInstancesReplayAlgorithm implements IPNMatchInstancesLogReplayAlgorithm {

	/**
	 * Pointers to parameters in array of objects
	 */
	// name of variable
	public static int MAXEXPLOREDINSTANCESINTVAL = 0; // maximum possible instance explored before exploration stops

	// cost variables
	public static int MOVEONLOGCOST = 1; // cost of move on log
	public static int MOVEONMODELINVICOST = 2; // cost of move on model (invi)
	public static int MOVEONMODELREALCOST = 3; // cost of move on model (real)

	// permittable replay actions
	public static int ALLOWMOVEONMODELINVI = 4;
	public static int ALLOWMOVEONMODELREAL = 5;
	public static int ALLOWMOVEONLOG = 6;

	
	/**
	 * Imported parameters
	 */
	// other parameters
	private boolean isAllowMoveOnLog;
	private boolean isAllowMoveOnModelInvi;
	private boolean isAllowMoveOnModelReal;
	private int maxNumOfStates;
	private int cstMoveOnLog;
	private int cstMoveOnModelInvi;
	private int cstMoveOnModelReal;

	public String toString() {
		return "Cost-based Petri net replay";
	}

	public PNMatchInstancesRepResult replayLog(PluginContext context, PetrinetGraph net, Marking initMarking, Marking finalMarking, XLog log,
			Collection<Pair<Transition, XEventClass>> mapping, Object[] parameters) {
		importParameters(parameters);

		if (maxNumOfStates != Integer.MAX_VALUE) {
			context.log("Starting replay with max state " + maxNumOfStates + "...");
		} else {
			context.log("Starting replay with no limit for max explored state...");
		}

		// use encoding
		PNCodec codec = new PNCodec(net);
		Set<Short> setInviTrans = getInviTransCodec(net, mapping, codec);
		Map<XEventClass, List<Short>> transitionMapping = getEncodedEventMapping(mapping, codec);
		Bag<Short> encInitMarking = encodeSetPlaces(initMarking, codec);

		PNMatchInstancesRepResult res = replayLogInEncodedForm(context, net, log, transitionMapping, setInviTrans,
				codec, encInitMarking);
		
		return res;
	}

	@SuppressWarnings("unchecked")
	private PNMatchInstancesRepResult replayLogInEncodedForm(final PluginContext context, final PetrinetGraph net,
			final XLog log, final Map<XEventClass, List<Short>> transitionMapping, final Set<Short> setInviTrans,
			final PNCodec codec, final Bag<Short> encInitMarking) {
		final XLogInfo summary = XLogInfoFactory.createLogInfo(log);
		final XEventClasses classes = summary.getEventClasses();

		// required to produce correct output object to be visualized 
		final LogCounterPrecSyncReplay counter = new LogCounterPrecSyncReplay();

		// update progress bar
		final Progress progress = context.getProgress();
		progress.setValue(0);

		int threads = Runtime.getRuntime().availableProcessors() / 2 + 1;
		ExecutorService executor = Executors.newFixedThreadPool(threads);

		int index = 0;
		final Map<List<XEventClass>, List<Integer>> listTraces = new HashMap<List<XEventClass>, List<Integer>>();
		for (final XTrace trace : log) {

			// ignore event classes that does not have corresponding transition
			final List<XEventClass> listTrace = getListEventClass(trace, classes, transitionMapping);

			if (listTraces.containsKey(listTrace)) {
				listTraces.get(listTrace).add(log.indexOf(trace));
			} else {
				listTraces.put(listTrace, new ArrayList<Integer>());
				context.log("Replaying trace: " + index++ + " of length " + trace.size());

				executor.execute(new Runnable() {

					public void run() {
						PriorityQueue<CPNCostBasedTreeNodeX> pq = new PriorityQueue<CPNCostBasedTreeNodeX>();

						Object[] replayRes = OptimizedCostBasedPNMatchInstancesReplayAlgorithm
								.replayTraceInEncodedForm(progress, listTrace, transitionMapping, setInviTrans, codec,
										encInitMarking, maxNumOfStates, cstMoveOnLog, cstMoveOnModelInvi,
										cstMoveOnModelReal, isAllowMoveOnLog, isAllowMoveOnModelInvi,
										isAllowMoveOnModelReal, pq);

						if (progress.isCancelled()) {
							return;
						}

						List<List<Pair<StepTypes, Object>>> result = (List<List<Pair<StepTypes, Object>>>) replayRes[OptimizedCostBasedPNMatchInstancesReplayAlgorithm.LISTSOFPAIR];
						boolean isReliable = Boolean.valueOf(replayRes[OptimizedCostBasedPNReplayAlgorithm.ISRELIABLE]
								.toString());

						if (result == null) { // trace can NOT be replayed
							context.log("Trace " + XConceptExtension.instance().extractName(trace)
									+ " can't be replayed");
						} else { // trace can be replayed, its result may not be guaranteed to be correct (if heuristics applied)
							Object[] splitResult = splitReplayResult(result, codec);
							counter.add(listTrace, (List<List<Object>>) splitResult[0],
									(List<List<StepTypes>>) splitResult[1], log.indexOf(trace), isReliable);
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
		}  catch (OutOfMemoryError memExc){
			context.log("Out of memory while synchronizing result. Continue processing what has been obtained.");
		}

		for (List<XEventClass> listTrace : listTraces.keySet()) {
			for (Integer traceIndex : listTraces.get(listTrace)) {
				counter.inc(listTrace, traceIndex);
			}
		}
		return new PNMatchInstancesRepResult(counter.getResult());
	}

	private Object[] splitReplayResult(List<List<Pair<StepTypes, Object>>> result, PNCodec codec) {

		List<List<Object>> lstOfNodeInstances = new LinkedList<List<Object>>();
		List<List<StepTypes>> lstOfListStep = new LinkedList<List<StepTypes>>();

		for (List<Pair<StepTypes, Object>> list : result) {
			List<StepTypes> listStep = new LinkedList<StepTypes>();
			List<Object> nodeInstances = new LinkedList<Object>();

			for (Pair<StepTypes, Object> pair : list) {

				listStep.add(pair.getFirst());
				switch (pair.getFirst()) {
					case L :
						nodeInstances.add(((XEventClass) pair.getSecond()).toString());
						break;
					default :
						nodeInstances.add(pair.getSecond());
						break;
				}
			}

			lstOfListStep.add(listStep);
			lstOfNodeInstances.add(nodeInstances);
		}
		return new Object[] { lstOfNodeInstances, lstOfListStep };
	}

	private List<XEventClass> getListEventClass(XTrace trace, XEventClasses classes,
			Map<XEventClass, List<Short>> transitionMapping) {
		List<XEventClass> res = new LinkedList<XEventClass>();
		for (XEvent evt : trace) {
			XEventClass ec = classes.getClassOf(evt);
			if (transitionMapping.get(ec) != null) {
				res.add(ec);
			}
		}
		return res;
	}

	private Set<Short> getInviTransCodec(PetrinetGraph net, Collection<Pair<Transition, XEventClass>> mapping,
			PNCodec codec) {
		Set<Short> setInviTrans = new HashSet<Short>(codec.getMapShortTrans().keySet());

		for (Pair<Transition, XEventClass> pair : mapping) {
			setInviTrans.remove(codec.getEncodeOfTransition(pair.getFirst()));
		}

		return setInviTrans;
	}

	private Map<XEventClass, List<Short>> getEncodedEventMapping(Collection<Pair<Transition, XEventClass>> mapping,
			PNCodec codec) {
		Map<XEventClass, List<Short>> res = new HashMap<XEventClass, List<Short>>();

		for (Pair<Transition, XEventClass> pair : mapping) {
			short mappedTrans = codec.getEncodeOfTransition(pair.getFirst());
			XEventClass event = pair.getSecond();
			if (res.containsKey(event)) {
				res.get(event).add(mappedTrans);
			} else {
				// create new list
				List<Short> listShort = new LinkedList<Short>();
				listShort.add(mappedTrans);
				res.put(pair.getSecond(), listShort);
			}
		}

		return res;
	}

	private Bag<Short> encodeSetPlaces(Marking initMarking, PNCodec codec) {
		Bag<Short> res = new HashBag<Short>();
		for (Place place : initMarking) {
			res.add(codec.getEncodeOfPlace(place), initMarking.occurrences(place));
		}
		return res;
	}

	private void importParameters(Object[] parameters) {
		// replay parameters
		isAllowMoveOnLog = (Boolean) parameters[ALLOWMOVEONLOG];
		isAllowMoveOnModelInvi = (Boolean) parameters[ALLOWMOVEONMODELINVI];
		isAllowMoveOnModelReal = (Boolean) parameters[ALLOWMOVEONMODELREAL];
		maxNumOfStates = (Integer) parameters[MAXEXPLOREDINSTANCESINTVAL];
		cstMoveOnLog = (Integer) parameters[MOVEONLOGCOST];
		cstMoveOnModelInvi = (Integer) parameters[MOVEONMODELINVICOST];
		cstMoveOnModelReal = (Integer) parameters[MOVEONMODELREALCOST];

	}

}
