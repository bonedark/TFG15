package org.processmining.plugins.boudewijn.treebasedreplay;

import gnu.trove.list.TIntList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nl.tue.storage.CompressedStore;
import nl.tue.storage.StorageException;

import org.processmining.plugins.astar.algorithm.State;
import org.processmining.plugins.astar.interfaces.Delegate;
import org.processmining.plugins.astar.interfaces.Head;
import org.processmining.plugins.astar.interfaces.Move;
import org.processmining.plugins.astar.interfaces.Record;
import org.processmining.plugins.astar.interfaces.Tail;
import org.processmining.plugins.boudewijn.treebasedreplay.tokencount.TokenCountHead;

import cern.colt.Arrays;

public class TreeRecord implements Record {
	//                              header: 16 bytes 
	private long state; //                   8 bytes
	private final int cost; //               4 bytes
	private final TreeRecord predecessor; // 8 bytes
	private final int node; //               4 bytes
	private final int logMove; //            4 bytes 
	private final int nextEvent; //          4 bytes 
	private final int backtrace; //          4 bytes
	private int totalCost; //                4 bytes   
	private final int[] internalMoves; //   >

	//                 

	private TreeRecord(long state, int cost, TreeRecord predecessor, int logMove, int node, int backtrace,
			int[] internalMoves) {
		this.state = state;
		this.cost = cost;
		this.predecessor = predecessor;
		this.logMove = logMove;
		this.node = node;
		this.nextEvent = predecessor.nextEvent + (logMove == Move.BOTTOM ? 0 : 1);
		this.backtrace = backtrace;
		this.internalMoves = internalMoves;
	}

	public TreeRecord(int cost, TIntList internalMoves) {
		this.cost = cost;
		this.predecessor = null;
		this.node = Move.BOTTOM;
		this.logMove = -1;
		this.nextEvent = 0;
		this.backtrace = cost;
		if (internalMoves != null) {
			this.internalMoves = new int[internalMoves.size()];
			for (int i = 0; i < internalMoves.size(); i++) {
				this.internalMoves[i] = internalMoves.get(i);
			}
		} else {
			this.internalMoves = null;
		}

	}

	public <H extends Head, T extends Tail> State<H, T> getState(CompressedStore<State<H, T>> storage)
			throws StorageException {
		return storage.getObject(state);
	}

	public long getState() {
		return state;
	}

	public double getCostSoFar() {
		return cost;
	}

	public double getTotalCost() {
		return totalCost;
	}

	public TreeRecord getPredecessor() {
		return predecessor;
	}

	public void setState(long index) {
		this.state = index;
	}

	/**
	 * In case of a LogMove only, then logMove>=0, modelMove == Move.BOTTOM,
	 * 
	 * In case of a ModelMove only, then logMove == Move.BOTTOM, modelMove >=0,
	 * 
	 * in case of both log and model move, then logMove>=0, modelMove>=0,
	 * 
	 */
	public TreeRecord getNextRecord(Delegate<? extends Head, ? extends Tail> d, Head nextHead, long state,
			int modelMove, int logMove) {
		TreeDelegate<?, ?> delegate = (TreeDelegate<?, ?>) d;

		int c = delegate.getCostFor(modelMove, logMove);
		
		int[] internalMoves = null;

		if (nextHead instanceof TokenCountHead) {

			TIntList moves = ((TokenCountHead) nextHead).getMovesMade();
			if (moves != null && moves.size() > 0) {
				internalMoves = new int[moves.size()];
				for (int i = 0; i < moves.size(); i++) {
					internalMoves[i] = moves.get(i);
					c += delegate.getModelMoveCost(internalMoves[i]);
				}
			}

		}

		//		if (modelMove == Move.BOTTOM) {
		//			TreeRecord r = new TreeRecord(state, Integer.MAX_VALUE / 4, this, logMove, (short) modelMove, backtrace + 1);
		//			return r;
		//		} else if (logMove == Move.BOTTOM && delegate.isLeaf((short) modelMove)) {
		//			TreeRecord r = new TreeRecord(state, Integer.MAX_VALUE / 4, this, logMove, (short) modelMove, backtrace + 1);
		//			return r;
		//		} else {
		TreeRecord r = new TreeRecord(state, cost + c, this, logMove, modelMove, backtrace + 1, internalMoves);
		return r;
		//		}
	}

	//	public int compareTo(Record o) {
	//		long s1 = (long) cost + (long) estimate;
	//		long s2 = (long) o.getCostSoFar() + (long) o.getEstimatedRemainingCost();
	//		if (s1 != s2) {
	//			return (s1 < s2 ? -1 : 1);
	//		}
	//		TreeRecord r = (TreeRecord) o;
	//		if (logMove != r.logMove) {
	//			// if same cost, prefer fewer remaining events
	//			return (logMove > r.logMove ? -1 : 1);
	//		}
	//		// no known ordering.
	//		return (state < r.state ? -1 : state > r.state ? 1 : 0);
	//
	//	}

	public double getEstimatedRemainingCost() {
		return this.totalCost - this.cost;
	}

	public void setEstimatedRemainingCost(double estimate) {
		this.totalCost = this.cost + (int) estimate;

	}

	public boolean equals(Object o) {
		return (o instanceof Record) && ((Record) o).getState() == state;
		//&& ((Record) o).getCostSoFar() == cost
		//&& ((Record) o).getEstimatedRemainingCost() == estimate;
	}

	public int hashCode() {
		return (int) state;
	}

	public String toString() {
		return "[s:" + state + " c:" + (cost) + " e:" + getEstimatedRemainingCost() + "]"
				+ (internalMoves == null ? "" : Arrays.toString(internalMoves));
	}

	public int getModelMove() {
		return node;
	}

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

	public static void printRecord(TreeDelegate<?, ?> delegate, TIntList trace, TreeRecord r) {
		List<TreeRecord> history = getHistory(r);

		for (int i = 0; i < history.size(); i++) {
			r = history.get(i);
			String s = "(";
			short act = -1;
			if (r.getMovedEvent() != Move.BOTTOM) {
				act = (short) trace.get(r.getMovedEvent());
			}
			if (r.getModelMove() == Move.BOTTOM) {
				s += "_";
			} else {
				short m = (short) r.getModelMove();
				s += delegate.toString(m, act);
			}

			s += ",";
			// r.getLogEvent() is the event that was moved, or Move.BOTTOM
			if (r.getMovedEvent() == Move.BOTTOM) {
				s += "_";
			} else {
				assert (act >= 0 || act < 0);
				s += delegate.getEventClass(act);
			}
			s += ")";
			s += "c:" + (int) r.getCostSoFar();
			s += ",e:" + (int) r.getEstimatedRemainingCost();
			s += ",b:" + r.getBackTraceSize();
			s += ",im: " + (r.internalMoves == null ? "[]" : Arrays.toString(r.internalMoves));
			s += (i < history.size() - 1 ? " --> " : " cost: " + (r.getCostSoFar()));

			System.out.print(s);
		}
		System.out.println();
	}

	public int getMovedEvent() {
		return (logMove == Move.BOTTOM ? Move.BOTTOM : nextEvent - 1);
	}

	public int getNextEvent(Delegate<? extends Head, ? extends Tail> delegate) {
		return nextEvent;
	}

	public int getBackTraceSize() {
		return backtrace;
	}

	public int[] getInternalMoves() {
		return internalMoves == null ? new int[0] : internalMoves;
	}

}
