package org.processmining.plugins.astar.petrinet;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.astar.algorithm.AStarException;
import org.processmining.plugins.astar.algorithm.AStarThread.Canceller;
import org.processmining.plugins.astar.algorithm.MemoryEfficientAStarAlgorithm;
import org.processmining.plugins.astar.algorithm.MemoryEfficientAStarThread;
import org.processmining.plugins.astar.interfaces.Move;
import org.processmining.plugins.astar.interfaces.Tail;
import org.processmining.plugins.astar.petrinet.impl.AbstractPDelegate;
import org.processmining.plugins.astar.petrinet.impl.PHead;
import org.processmining.plugins.astar.petrinet.impl.PILPDelegate;
import org.processmining.plugins.astar.petrinet.impl.PRecord;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayer.algorithms.IPNReplayParameter;
import org.processmining.plugins.petrinet.replayer.algorithms.costbasedcomplete.CostBasedCompleteParam;
import org.processmining.plugins.petrinet.replayer.algorithms.costbasedcomplete.CostBasedCompletePruneAlg;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;
import org.processmining.plugins.replayer.util.StepTypes;

public abstract class AbstractPetrinetReplayer<T extends Tail, D extends AbstractPDelegate<T>> extends
		CostBasedCompletePruneAlg {

	public static class Result {
		PRecord record;
		int states;
		long milliseconds;
		int trace;
		protected TIntList filteredTrace;
	}

	protected int states = 0;

	/**
	 * Return true if all replay inputs are correct
	 */
	public boolean isAllReqSatisfied(PluginContext context, PetrinetGraph net, XLog log, TransEvClassMapping mapping,
			IPNReplayParameter parameter) {
		// only petri net is allowed
		if (net instanceof Petrinet) {
			// check number of transitions, places, and event classes, should be less than Short.MAX_VALUE
			if ((net.getTransitions().size() < Short.MAX_VALUE) && (net.getPlaces().size() < Short.MAX_VALUE)) {
				// check the number of event classes, should be less than Short.MAX_VALUE
				XLogInfo summary = XLogInfoFactory.createLogInfo(log, mapping.getEventClassifier());
				if (summary.getEventClasses().getClasses().size() < Short.MAX_VALUE) {
					return super.isAllReqSatisfied(context, net, log, mapping, parameter);
				}

			}
		}

		return false;
	}

	/**
	 * Return true if input of replay without parameters are correct
	 */
	public boolean isReqWOParameterSatisfied(PluginContext context, PetrinetGraph net, XLog log,
			TransEvClassMapping mapping) {
		// only petri net is allowed
		if (net instanceof Petrinet) {
			// check number of transitions, places, and event classes, should be less than Short.MAX_VALUE
			if ((net.getTransitions().size() < Short.MAX_VALUE) && (net.getPlaces().size() < Short.MAX_VALUE)) {
				// check the number of event classes, should be less than Short.MAX_VALUE
				XLogInfo summary = XLogInfoFactory.createLogInfo(log, mapping.getEventClassifier());
				if (summary.getEventClasses().getClasses().size() < Short.MAX_VALUE) {
					return super.isReqWOParameterSatisfied(context, net, log, mapping);
				}

			}
		}

		return false;
	}

	protected SyncReplayResult recordToResult(AbstractPDelegate<?> d, XTrace trace, TIntList filteredTrace, PRecord r,
			int traceIndex, int stateCount, boolean isReliable, long milliseconds, double minCostMoveModel) {
		List<PRecord> history = PRecord.getHistory(r);
		double mmCost = 0;
		double mlCost = 0;
		double mmUpper = 0;
		double mlUpper = 0;
		//		double smCost = 0;
		int eventInTrace = -1;
		List<StepTypes> stepTypes = new ArrayList<StepTypes>(history.size());
		List<Object> nodeInstance = new ArrayList<Object>();
		for (PRecord rec : history) {
			if (rec.getMovedEvent() == Move.BOTTOM) {
				// move model only
				Transition t = d.getTransition((short) rec.getModelMove());
				if (t.isInvisible()) {
					stepTypes.add(StepTypes.MINVI);
				} else {
					stepTypes.add(StepTypes.MREAL);
				}
				nodeInstance.add(t);
				mmCost += d.getCostForMoveModel((short) rec.getModelMove()) - d.getDelta();
				mmUpper += d.getCostForMoveModel((short) rec.getModelMove()) - d.getDelta();
			} else {
				// a move occurred in the log. Check if class aligns with class in trace
				short a = (short) filteredTrace.get(rec.getMovedEvent());
				eventInTrace++;
				XEventClass clsInTrace = d.getClassOf(trace.get(eventInTrace));
				while (d.getIndexOf(clsInTrace) != a) {
					// The next event in the trace is not of the same class as the next event in the A-star result.
					// This is caused by the class in the trace not being mapped to any transition.
					// move log only
					stepTypes.add(StepTypes.L);
					nodeInstance.add(clsInTrace);
					mlCost += mapEvClass2Cost.get(clsInTrace);
					mlUpper += mapEvClass2Cost.get(clsInTrace);
					eventInTrace++;
					clsInTrace = d.getClassOf(trace.get(eventInTrace));
				}
				if (rec.getModelMove() == Move.BOTTOM) {
					// move log only
					stepTypes.add(StepTypes.L);
					nodeInstance.add(d.getEventClass(a));
					mlCost += d.getCostForMoveLog(a) - d.getDelta();
					mlUpper += d.getCostForMoveLog(a) - d.getDelta();
				} else {
					// sync move
					stepTypes.add(StepTypes.LMGOOD);
					nodeInstance.add(d.getTransition((short) rec.getModelMove()));
					mlUpper += d.getCostForMoveLog(a) - d.getDelta();
					mmUpper += d.getCostForMoveModel((short) rec.getModelMove()) - d.getDelta();
					//					smCost += d.getDelta();
				}
			}

		}

		// add the rest of the trace
		eventInTrace++;
		while (eventInTrace < trace.size()) {
			// move log only
			XEventClass a = d.getClassOf(trace.get(eventInTrace++));
			stepTypes.add(StepTypes.L);
			nodeInstance.add(a);
			mlCost += mapEvClass2Cost.get(a);
			mlUpper += mapEvClass2Cost.get(a);
		}
		SyncReplayResult res = new SyncReplayResult(nodeInstance, stepTypes, traceIndex);

		res.setReliable(isReliable);
		Map<String, Double> info = new HashMap<String, Double>();
		//		info.put(PNRepResult.RAWFITNESSCOST, (mmCost + mlCost + smCost));
		info.put(PNRepResult.RAWFITNESSCOST, (mmCost + mlCost));

		if (mlCost > 0) {
			info.put(PNRepResult.MOVELOGFITNESS, 1 - (mlCost / mlUpper));
		} else {
			info.put(PNRepResult.MOVELOGFITNESS, 1.0);
		}

		if (mmCost > 0) {
			info.put(PNRepResult.MOVEMODELFITNESS, 1 - (mmCost / mmUpper));
		} else {
			info.put(PNRepResult.MOVEMODELFITNESS, 1.0);
		}
		info.put(PNRepResult.NUMSTATEGENERATED, (double) stateCount);

		// set info fitness
		//		info.put(PNRepResult.TRACEFITNESS, 1 - ((mmCost + mlCost+smCost)/(mlUpper + minCostMoveModel)));
		info.put(PNRepResult.TRACEFITNESS, 1 - ((mmCost + mlCost) / (mlUpper + minCostMoveModel)));
		info.put(PNRepResult.TIME, (double) milliseconds);
		info.put(PNRepResult.ORIGTRACELENGTH, (double) eventInTrace);
		res.setInfo(info);
		return res;
	}

	/**
	 * get list of event class. Record the indexes of non-mapped event classes.
	 * 
	 * @param trace
	 * @param classes
	 * @param mapEvClass2Trans
	 * @param listMoveOnLog
	 * @return
	 */
	protected TIntList getListEventClass(XLog log, int trace, AbstractPDelegate<?> delegate) {
		int s = log.get(trace).size();
		TIntList result = new TIntArrayList(s);
		for (int i = 0; i < s; i++) {
			int act = delegate.getActivityOf(trace, i);
			if (act != Move.BOTTOM) {
				result.add(act);
			}
		}
		return result;
	}

	public String getHTMLInfo() {
		return "<html>This is an algorithm to calculate cost-based fitness between a log and a Petri net. <br/><br/>"
				+ "Given a trace and a Petri net, this algorithm "
				+ "return a matching between the trace and an allowed firing sequence of the net with the"
				+ "least deviation cost using the A* algorithm-based technique. The firing sequence has to reach proper "
				+ "termination (possible final markings/dead markings) of the net. <br/><br/>"
				+ "To minimize the number of explored state spaces, the algorithm prunes visited states/equally visited states. <br/><br/>"
				+ "Cost for skipping (move on model) and inserting (move on log) "
				+ "activities can be assigned uniquely for each move on model/log. </html>";
	}

	public PNRepResult replayLog(final PluginContext context, PetrinetGraph net, final XLog log,
			TransEvClassMapping mapping, final IPNReplayParameter parameters) {
		importParameters((CostBasedCompleteParam) parameters);
		classifier = mapping.getEventClassifier();

		if (parameters.isGUIMode()) {
			if (maxNumOfStates != Integer.MAX_VALUE) {
				context.log("Starting replay with max state " + maxNumOfStates + "...");
			} else {
				context.log("Starting replay with no limit for max explored state...");
			}
		}

		final XLogInfo summary = XLogInfoFactory.createLogInfo(log, classifier);
		final XEventClasses classes = summary.getEventClasses();

		//		try {
		//			PackageManager.getInstance().findOrInstallPackages("LpSolve");
		//		} catch (Exception e) {
		//			e.printStackTrace();
		//			return null;
		//		}
		//		System.loadLibrary("lpsolve55");
		//		System.loadLibrary("lpsolve55j");

		final double delta = 0.2;
		final int threads = 2;
		final D delegate = getDelegate((Petrinet) net, log, classes, mapping, delta, threads);

		final MemoryEfficientAStarAlgorithm<PHead, T> aStar = new MemoryEfficientAStarAlgorithm<PHead, T>(delegate);

		ExecutorService pool = Executors.newFixedThreadPool(threads);

		final List<Future<Result>> result = new ArrayList<Future<Result>>();

		final TIntIntMap doneMap = new TIntIntHashMap();

		long start = System.currentTimeMillis();

		context.getProgress().setMaximum(log.size() + 1);

		TObjectIntMap<TIntList> traces = new TObjectIntHashMap<TIntList>(log.size() / 2, 0.5f, -1);

		final List<SyncReplayResult> col = new ArrayList<SyncReplayResult>();

		// calculate first cost of empty trace
		double minCostMoveModel = getMinBoundMoveModel(context, log, net, mapping, classes, delta, threads, aStar);

		for (int i = 0; i < log.size() && !context.getProgress().isCancelled(); i++) {
			PHead initial = new PHead(delegate, initMarking, log.get(i));
			final TIntList trace = getListEventClass(log, i, delegate);
			int first = traces.get(trace);
			if (first >= 0) {
				doneMap.put(i, first);
				//System.out.println(i + "/" + log.size() + "-is the same as " + first);
				continue;
			} else {
				traces.put(trace, i);
			}
			final MemoryEfficientAStarThread<PHead, T> thread = new MemoryEfficientAStarThread<PHead, T>(aStar,
					initial, trace, maxNumOfStates);
			final int j = i;
			result.add(pool.submit(new Callable<Result>() {

				public Result call() throws Exception {
					Result result = new Result();
					result.trace = j;
					result.filteredTrace = trace;

					long start = System.currentTimeMillis();
					result.record = (PRecord) thread.run(new Canceller() {
						public boolean isCancelled() {
							return context.getProgress().isCancelled();
						}
					});
					long end = System.currentTimeMillis();
					synchronized (context) {
						if (parameters.isGUIMode()) {
							context.log(j + "/" + log.size() + " visiting " + thread.getQueuedStateCount()
									+ " states took " + (end - start) / 1000.0 + " seconds.");
						}
						context.getProgress().inc();
					}
					states += thread.getQueuedStateCount();
					result.states = thread.getQueuedStateCount();
					result.milliseconds = end - start;

					return result;

				}
			}));
		}
		context.getProgress().inc();
		pool.shutdown();
		while (!pool.isTerminated()) {
			try {
				pool.awaitTermination(10, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		if (delegate instanceof PILPDelegate) {
			((PILPDelegate) delegate).deleteLPs();
		}

		long maxStateCount = 0;
		long time = 0;
		long ui = System.currentTimeMillis();
		for (Future<Result> f : result) {
			Result r = null;
			try {
				while (r == null) {
					try {
						r = f.get();
					} catch (InterruptedException e) {
					}
				}
				XTrace trace = log.get(r.trace);
				int states = addReplayResults(delegate, trace, r, doneMap, log, col, r.trace, minCostMoveModel, null);
				maxStateCount = Math.max(maxStateCount, states);
				time += r.milliseconds;
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		long end = System.currentTimeMillis();
		// each PRecord uses 56 bytes in memory

		maxStateCount *= 56;
		if (parameters.isGUIMode()) {
			context.log("Total time : " + (end - start) / 1000.0 + " seconds");
			context.log("Time for A*: " + time / 1000.0 + " seconds");
			context.log("In total " + states + " unique states were visisted.");
			context.log("In total " + aStar.getStatespace().size()
					+ " marking-parikhvector pairs were stored in the statespace.");
			context.log("In total " + aStar.getStatespace().getMemory() / (1024.0 * 1024.0)
					+ " MB were needed for the statespace.");
			context.log("At most " + maxStateCount / (1024.0 * 1024.0) + " MB was needed for a trace (overestimate).");
			context.log("States / second:  " + states / (time / 1000.0));
			context.log("Storage / second: " + aStar.getStatespace().size() / ((ui - start) / 1000.0));
		}
		synchronized (col) {
			return new PNRepResult(col);
		}
	}

	/**
	 * get cost if an empty trace is replayed on a model
	 * 
	 * @param context
	 * @param net
	 * @param mapping
	 * @param classes
	 * @param delta
	 * @param threads
	 * @param aStar
	 * @return
	 */
	protected double getMinBoundMoveModel(final PluginContext context, XLog log, PetrinetGraph net,
			TransEvClassMapping mapping, final XEventClasses classes, final double delta, final int threads,
			final MemoryEfficientAStarAlgorithm<PHead, T> aStar) {
		// create a log 
		XFactory factory = XFactoryRegistry.instance().currentDefault();
		XTrace emptyTrace = factory.createTrace();

		final D delegateD = getDelegate((Petrinet) net, log, classes, mapping, delta, threads);
		PHead initialD = new PHead(delegateD, initMarking, emptyTrace);
		final MemoryEfficientAStarThread<PHead, T> threadD = new MemoryEfficientAStarThread<PHead, T>(aStar, initialD,
				new TIntArrayList(1), maxNumOfStates);
		try {
			PRecord recordD = (PRecord) threadD.run(new Canceller() {
				public boolean isCancelled() {
					return context.getProgress().isCancelled();
				}
			});
			if (recordD == null) {
				return 0.0;
			}
			// resolution due to numerical inconsistency problem of double data type
			double tempRes = recordD.getCostSoFar() - (delta * (recordD.getBackTraceSize() + 1));
			if (tempRes < 0.0) {
				return 0.0;
			} else {
				return Math.round(tempRes);
			}
		} catch (AStarException e1) {
			e1.printStackTrace();
			return 0;
		}
	}

	protected abstract D getDelegate(Petrinet net, XLog log, XEventClasses classes, TransEvClassMapping mapping,
			double delta, int threads);

	protected int addReplayResults(D delegate, XTrace trace, Result r, TIntIntMap doneMap, XLog log,
			List<SyncReplayResult> col, int traceIndex, double minCostMoveModel, Map<Integer, SyncReplayResult> mapRes) {
		SyncReplayResult srr = recordToResult(delegate, trace, r.filteredTrace, r.record, traceIndex, r.states,
				r.states < maxNumOfStates, r.milliseconds, minCostMoveModel);
		if (mapRes == null) {
			mapRes = new HashMap<Integer, SyncReplayResult>(4);
		}
		mapRes.put(traceIndex, srr);

		boolean done = false;
		forLoop: for (int key : doneMap.keys()) {
			if (doneMap.get(key) == r.trace) {
				// This should only be done for similar traces.
				XTrace keyTrace = log.get(key);
				// check if trace == keyTrace
				for (Integer keyMapRes : mapRes.keySet()) {
					if (compareEventClassList(delegate, log.get(keyMapRes), keyTrace)) {
						mapRes.get(keyMapRes).addNewCase(key);
						doneMap.put(key, -2);
						continue forLoop;
					}
				}
				if (!done) {
					// Now they are not the same.
					addReplayResults(delegate, keyTrace, r, doneMap, log, col, key, minCostMoveModel, mapRes);
					done = true;
				}
			}
		}
		col.add(srr);

		return r.states;
	}

	protected boolean compareEventClassList(D d, XTrace t1, XTrace t2) {
		if (t1.size() != t2.size()) {
			return false;
		}
		Iterator<XEvent> it = t2.iterator();
		for (XEvent e : t1) {
			if (!d.getClassOf(e).equals(d.getClassOf(it.next()))) {
				return false;
			}
		}
		return true;
	}
}
