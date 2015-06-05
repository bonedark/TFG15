package org.processmining.plugins.boudewijn.treebasedreplay;

import gnu.trove.list.TIntList;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import nl.tue.storage.StorageException;

import org.processmining.plugins.astar.algorithm.AStarException;
import org.processmining.plugins.astar.algorithm.AStarThread.Canceller;
import org.processmining.plugins.astar.algorithm.AbstractAStarThread;
import org.processmining.plugins.astar.algorithm.FastAStarThread;
import org.processmining.plugins.astar.algorithm.MemoryEfficientAStarAlgorithm;
import org.processmining.plugins.astar.algorithm.MemoryEfficientAStarThread;
import org.processmining.plugins.astar.algorithm.State;
import org.processmining.plugins.astar.interfaces.Head;
import org.processmining.plugins.astar.interfaces.Tail;
import org.processmining.plugins.boudewijn.tree.Node;

public abstract class AbstractTreeBasedAStar<H extends Head, T extends Tail, D extends TreeDelegate<H, T>> {

	public static enum VerboseLevel {
		NONE, ALL, SOME
	}

	public static enum PerformanceType {
		MEMORYEFFICIENT, CPUEFFICIENT
	}

	protected final Node root;
	protected final Canceller canceller;
	protected final D delegate;
	protected final int maxNumOfStates = 500 * 1000;
	protected final AStarAlgorithm aStarLogAbstraction;

	protected TObjectIntHashMap<H> head2int;
	protected ArrayList<State<H, T>> stateList;
	protected long statecount = 0;

	protected MemoryEfficientAStarAlgorithm<H, T> algorithm;
	private PerformanceType type;

	private long queuedStateCount;
	private long traversedArcCount;
	private final ExecutorService executor;
	private final int threads;
	private final boolean greedy;

	public AbstractTreeBasedAStar(AStarAlgorithm aStarLogAbstraction, Canceller canceller, Node root,
			Map<Node, Integer> node2Cost, boolean greedy) {
		this.aStarLogAbstraction = aStarLogAbstraction;
		this.greedy = greedy;
		threads = 1;
		this.delegate = constructDelegate(aStarLogAbstraction, root, node2Cost, threads, greedy);

		this.canceller = canceller;
		this.root = root;

		if (threads > 1) {
			executor = Executors.newFixedThreadPool(threads);
		} else {
			executor = null;
		}

		setType(PerformanceType.MEMORYEFFICIENT);
	}

	public void setType(PerformanceType type) {
		this.type = type;
		if (type == PerformanceType.MEMORYEFFICIENT) {
			algorithm = new MemoryEfficientAStarAlgorithm<H, T>(delegate);
			head2int = null;
			stateList = null;
		} else {
			algorithm = null;
			head2int = new TObjectIntHashMap<H>(100000);
			stateList = new ArrayList<State<H, T>>(1024);
		}
	}

	protected abstract D constructDelegate(AStarAlgorithm algorithm, Node root, Map<Node, Integer> node2Cost,
			int threads, boolean greedy);

	public double run(final VerboseLevel verbose, final double stopAt) throws AStarException {
		double cost = 0;

		/*
		 * Calculate the replay fitness and at the same time align the log and
		 * the model
		 */
		List<Future<Integer>> costsToAdd = null;
		if (threads > 1) {
			costsToAdd = new ArrayList<Future<Integer>>(aStarLogAbstraction.getDifferentTraceCount());
		}
		int i = 0;
		Iterator<TIntList> it = aStarLogAbstraction.traceIterator();
		while (it.hasNext()) {
			final TIntList trace = it.next();
			if (threads == 1) {
				cost += execute(trace, stopAt, verbose);
				if (verbose == VerboseLevel.SOME && ((++i) % 50 == 0)) {
					System.out.println();
				}
			} else {
				costsToAdd.add(executor.submit(new Callable<Integer>() {

					public Integer call() throws Exception {
						return execute(trace, stopAt, verbose);
					}

				}));
			}
			//Try to prevent a GC Overhead limit exceeded by explicitly calling the garbage truck to come by
			//			System.gc();
		}
		if (threads > 1) {
			for (Future<Integer> f : costsToAdd) {
				try {
					cost += f.get();
				} catch (Exception e) {
					continue;
				}
				if (verbose == VerboseLevel.SOME && ((++i) % 50 == 0)) {
					System.out.println();
				}
			}
		}

		if (verbose != VerboseLevel.NONE) {
			//System.out.println("The behavior of all nodes should be printed here but we did not implement this yet...");
			//System.out.println(root.printBehaviorRecursive());
			//System.out.println(behaviorMap);
		}

		return cost / delegate.getScaling();
	}

	private int execute(TIntList trace, double stopAt, VerboseLevel verbose) throws AStarException {
		//for (int i = 0; i < log.size(); i++) {
		int cost = 0;
		int frequency = aStarLogAbstraction.getTraceFreq(trace);

		H initial = createInitialHead(trace);

		long start = System.nanoTime();
		TreeRecord r;
		AbstractAStarThread<H, T> thread;
		if (greedy) {
			if (type == PerformanceType.MEMORYEFFICIENT) {
				thread = new MemoryEfficientGreedyAStarThread<H, T>(algorithm, initial, trace, maxNumOfStates);
				r = (TreeRecord) thread.run(canceller, stopAt);
			} else {
				thread = new FastGreedyAStarThread<H, T>(getDelegate(), head2int, stateList, initial, trace,
						maxNumOfStates);
				r = (TreeRecord) thread.run(canceller, stopAt);
			}
		} else {
			if (type == PerformanceType.MEMORYEFFICIENT) {
				thread = new MemoryEfficientAStarThread<H, T>(algorithm, initial, trace, maxNumOfStates);
				r = (TreeRecord) thread.run(canceller, stopAt);
			} else {
				thread = new FastAStarThread<H, T>(getDelegate(), head2int, stateList, initial, trace, maxNumOfStates);
				r = (TreeRecord) thread.run(canceller, stopAt);
			}
		}
		long end = System.nanoTime();

		if (r != null) {

			cost += calculateCostAndPostProcess(verbose, r, frequency);
		} else {
			// hmm, no fitness found, make expensive as this model is not even close to a fitting candidate
			cost += frequency * delegate.getScaling() * (trace.size() + root.countLeafNodes());
		}

		if (verbose != VerboseLevel.NONE) {
			if (r != null) {
				switch (verbose) {
					case ALL :
						System.out.println("Time: " + ((end - start) / 1000.0 / 1000.0 / 1000.0) + " seconds");
						TreeRecord.printRecord(getDelegate(), trace, r);
						System.out.println("States visited: " + thread.getQueuedStateCount());
						if (algorithm == null) {
							System.out.println("Unique states : " + stateList.size());
						} else {
							System.out.println("Unique states : " + algorithm.getStatespace().size());
						}
						break;
					case SOME :
						System.out.print('.');
						//$FALL-THROUGH$
					default :
				}
			} else {
				//System.err.println(root);
				System.out.print("X");
			}
		}
		queuedStateCount += thread.getQueuedStateCount();
		traversedArcCount += thread.getTraversedArcCount();
		return cost;
	}

	protected abstract int calculateCostAndPostProcess(VerboseLevel verbose, TreeRecord r, int frequency);

	protected abstract H createInitialHead(TIntList trace);

	public static List<TreeRecord> getHistory(TreeRecord r) {
		if (r == null) {
			return Collections.emptyList();
		}
		List<TreeRecord> history = new ArrayList<TreeRecord>(r.getBackTraceSize());
		while (r.getPredecessor() != null) {
			history.add(0, r);
			r = r.getPredecessor();
		}
		return history;
	}

	public State<H, T> getState(long index) {
		if (type == PerformanceType.MEMORYEFFICIENT) {
			try {
				return algorithm.getStatespace().getObject(index);
			} catch (StorageException e) {
				e.printStackTrace();
				return null;
			}
		} else {
			return stateList.get((int) index);
		}
	}

	public long getQueuedStateCount() {
		return queuedStateCount;
	}

	public long getTraversedArcCount() {
		return traversedArcCount;
	}

	public D getDelegate() {
		return delegate;
	}

	public long getNumStoredStates() {
		if (type == PerformanceType.MEMORYEFFICIENT) {
			return this.algorithm.getStatespace().size();
		} else {
			return this.stateList.size();
		}
	}

}
