package org.processmining.plugins.astar.algorithm.test;


public class AStarTest { // extends CostBasedCompletePruneAlg {
//
//	private static class Result {
//		PRecord record;
//		int states;
//		long milliseconds;
//		int trace;
//	}
//
//	private int states = 0;
//
//	public PNRepResult replayLogGUI(final PluginContext context, final PetrinetGraph net, final XLog log,
//			TransEvClassMapping mapping, IPNReplayParameter parameters) {
//
//		importParameters((CostBasedCompleteParam) parameters);
//		classifier = mapping.getEventClassifier();
//
//		if (maxNumOfStates != Integer.MAX_VALUE) {
//			context.log("Starting replay with max state " + maxNumOfStates + "...");
//		} else {
//			context.log("Starting replay with no limit for max explored state...");
//		}
//		maxNumOfStates = Integer.MAX_VALUE;
//
//		final XLogInfo summary = XLogInfoFactory.createLogInfo(log, classifier);
//		final XEventClasses classes = summary.getEventClasses();
//
//		//		try {
//		//			PackageManager.getInstance().findOrInstallPackages("LpSolve");
//		//		} catch (Exception e) {
//		//			e.printStackTrace();
//		//			return null;
//		//		}
//		//		System.loadLibrary("lpsolve55");
//		//		System.loadLibrary("lpsolve55j");
//
//		final PDelegate delegate = new PDelegate((Petrinet) net, log, classes, mapping, mapTrans2Cost, mapEvClass2Cost,
//				finalMarkings);
//
//		final AStarAlgorithm<PHead, PEmptyTail> aStar = new AStarAlgorithm<PHead, PEmptyTail>(delegate);
//
//		ExecutorService pool = Executors.newFixedThreadPool(2);
//
//		final List<Future<Result>> result = new ArrayList<Future<Result>>();
//
//		final TIntIntMap doneMap = new TIntIntHashMap();
//
//		long start = System.currentTimeMillis();
//
//		context.getProgress().setMaximum(log.size() + 1);
//
//		TObjectIntMap<TIntList> traces = new TObjectIntHashMap<TIntList>(log.size() / 2, 0.5f, -1);
//
//		final List<SyncReplayResult> col = new ArrayList<SyncReplayResult>();
//
//		for (int i = 0; i < log.size() && !context.getProgress().isCancelled(); i++) {
//			PHead initial = new PHead(delegate, initMarking, log.get(i));
//			TIntList trace = getListEventClass(log, i, delegate);
//			int first = traces.get(trace);
//			if (first >= 0) {
//				doneMap.put(i, first);
//				//System.out.println(i + "/" + log.size() + "-is the same as " + first);
//				continue;
//			} else {
//				traces.put(trace, i);
//			}
//			final AStarThread<PHead, PEmptyTail> thread = new AStarThread<PHead, PEmptyTail>(aStar, initial, trace, i,
//					maxNumOfStates);
//			final int j = i;
//			result.add(pool.submit(new Callable<Result>() {
//
//				public Result call() throws Exception {
//					try {
//						Result result = new Result();
//						result.trace = j;
//
//						long start = System.currentTimeMillis();
//						result.record = (PRecord) thread.run(new Canceller() {
//							public boolean isCancelled() {
//								return context.getProgress().isCancelled();
//							}
//						});
//						long end = System.currentTimeMillis();
//						context.log(j + "/" + log.size() + " visiting " + thread.getStates() + " states took "
//								+ (end - start) / 1000.0 + " seconds.");
//						states += thread.getStates();
//						result.states = thread.getStates();
//						result.milliseconds = end - start;
//
//						context.getProgress().inc();
//						return result;
//					} catch (Exception e) {
//						e.printStackTrace();
//						throw e;
//					}
//				}
//			}));
//		}
//		context.getProgress().inc();
//		pool.shutdown();
//		while (!pool.isTerminated()) {
//			try {
//				pool.awaitTermination(10, TimeUnit.SECONDS);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//
//		long maxStateCount = 0;
//		long ui = System.currentTimeMillis();
//		for (Future<Result> f : result) {
//			Result r;
//			try {
//				r = f.get();
//
//				maxStateCount = Math.max(maxStateCount, r.states);
//				SyncReplayResult srr = recordToResult(delegate, r.record, r.trace, r.states, r.states < maxNumOfStates);
//
//				for (int key : doneMap.keys()) {
//					if (doneMap.get(key) == r.trace) {
//						srr.addNewCase(key);
//					}
//				}
//				col.add(srr);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//
//		}
//		long end = System.currentTimeMillis();
//		// each PRecord uses 56 bytes in memory
//
//		maxStateCount *= 56;
//		context.log("Total time : " + (end - start) / 1000.0 + " seconds");
//		context.log("Time for A*: " + (ui - start) / 1000.0 + " seconds");
//		context.log("In total " + states + " unique states were visisted.");
//		context.log("In total " + aStar.getStatespace().size()
//				+ " marking-parikhvector pairs were stored in the statespace.");
//		context.log("In total " + aStar.getStatespace().getMemory() / (1024.0 * 1024.0)
//				+ " MB were needed for the statespace.");
//		context.log("At most " + maxStateCount / (1024.0 * 1024.0) + " MB was needed for a trace (overestimate).");
//		context.log("States / second:  " + states / ((ui - start) / 1000.0));
//		context.log("Storage / second: " + aStar.getStatespace().size() / ((ui - start) / 1000.0));
//		synchronized (col) {
//			return new PNRepResult(col);
//		}
//	}
//
//	protected SyncReplayResult recordToResult(AbstractPDelegate<?> d, PRecord r, int traceIndex, int stateCount,
//			boolean isReliable) {
//		List<PRecord> history = PRecord.getHistory(r);
//		int mmCost = 0;
//		int mlCost = 0;
//		int mmUpper = 0;
//		int mlUpper = 0;
//		List<StepTypes> stepTypes = new ArrayList<StepTypes>(history.size());
//		List<Object> nodeInstance = new ArrayList<Object>();
//		for (PRecord rec : history) {
//			if (rec.getModelMove() == Move.BOTTOM) {
//				// move log only
//				stepTypes.add(StepTypes.L);
//				short a = d.getActivityOf(traceIndex, rec.getLogMove());
//				nodeInstance.add(d.getEventClass(a));
//				mlCost += d.getCostForMoveLog(a);
//				mlUpper += d.getCostForMoveLog(a);
//			} else if (rec.getLogMove() == Move.BOTTOM) {
//				// move model only
//				TShortSet acts = d.getActivitiesFor((short) rec.getModelMove());
//				if (acts.isEmpty()) {
//					stepTypes.add(StepTypes.MINVI);
//				} else {
//					stepTypes.add(StepTypes.MREAL);
//				}
//				nodeInstance.add(d.getTransition((short) rec.getModelMove()));
//				mmCost += d.getCostForMoveModel((short) rec.getModelMove());
//				mmUpper += d.getCostForMoveModel((short) rec.getModelMove());
//			} else {
//				// sync move
//				stepTypes.add(StepTypes.LMGOOD);
//				nodeInstance.add(d.getTransition((short) rec.getModelMove()));
//				mlUpper += d.getCostForMoveLog(d.getActivityOf(traceIndex, rec.getLogMove()));
//				mmUpper += d.getCostForMoveModel((short) rec.getModelMove());
//			}
//		}
//		SyncReplayResult res = new SyncReplayResult(nodeInstance, stepTypes, traceIndex);
//
//		res.setReliable(isReliable);
//		Map<String, Double> info = new HashMap<String, Double>();
//		info.put(PNRepResult.RAWFITNESSCOST, (double) (r.getCostSoFar() - history.size()));
//
//		if (mlCost > 0) {
//			info.put(PNRepResult.MOVELOGFITNESS, 1 - ((double) mlCost / (double) mlUpper));
//		} else {
//			info.put(PNRepResult.MOVELOGFITNESS, 1.0);
//		}
//
//		if (mmCost > 0) {
//			info.put(PNRepResult.MOVEMODELFITNESS, 1 - ((double) mmCost / (double) mmUpper));
//		} else {
//			info.put(PNRepResult.MOVEMODELFITNESS, 1.0);
//		}
//		info.put(PNRepResult.NUMSTATEGENERATED, (double) stateCount);
//
//		res.setInfo(info);
//		return res;
//	}
//
//	/**
//	 * get list of event class. Record the indexes of non-mapped event classes.
//	 * 
//	 * @param trace
//	 * @param classes
//	 * @param mapEvClass2Trans
//	 * @param listMoveOnLog
//	 * @return
//	 */
//	private TIntList getListEventClass(XLog log, int trace, AbstractPDelegate delegate) {
//		int s = log.get(trace).size();
//		TIntList result = new TIntArrayList(s);
//		for (int i = 0; i < s; i++) {
//			int act = delegate.getActivityOf(trace, i);
//			assert (act != Move.BOTTOM);
//			result.add(act);
//		}
//		return result;
//	}
//
//	public String getHTMLInfo() {
//		return "<html>This is an algorithm to calculate cost-based fitness between a log and a Petri net. <br/><br/>"
//				+ "Given a trace and a Petri net, this algorithm "
//				+ "return a matching between the trace and an allowed firing sequence of the net with the"
//				+ "least deviation cost using the A* algorithm-based technique. The firing sequence has to reach proper "
//				+ "termination (possible final markings/dead markings) of the net. <br/><br/>"
//				+ "To minimize the number of explored state spaces, the algorithm prunes visited states/equally visited states. <br/><br/>"
//				+ "Cost for skipping (move on model) and inserting (move on log) "
//				+ "activities can be assigned uniquely for each move on model/log. </html>";
//	}
//
//	public String toString() {
//		return "A* (2) assuming at most " + Short.MAX_VALUE + " places, transitions and event classes.";
//	}
}
