package org.processmining.plugins.astar.petrinet.impl;

import gnu.trove.set.TShortSet;

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

public class PRecord implements Record {

	//                           header: 24 bytes 
	private long state; //                8 bytes
	private double estimate; //           8 bytes
	private final double cost; //         8 bytes
	private final PRecord predecessor; // 8 bytes
	private final short transition; //    2 bytes
	private final int logMove; //         4 bytes 
	private final int nextEvent; //       4 bytes 
	private final int backtrace; //       4 bytes

	//                            total: 70 -> 72 bytes. 

	private PRecord(long state, double cost, PRecord predecessor, int logMove, short transition, int markingsize,
			int backtrace) {
		this.state = state;
		this.cost = cost;
		this.predecessor = predecessor;
		this.logMove = logMove;
		this.transition = transition;
		this.nextEvent = predecessor.nextEvent + (logMove == Move.BOTTOM ? 0 : 1);
		this.backtrace = backtrace;
	}

	public PRecord(double cost, PRecord predecessor, int markingsize) {
		this.cost = cost;
		this.predecessor = predecessor;
		this.transition = (short) Move.BOTTOM;
		this.logMove = Move.BOTTOM;
		this.nextEvent = 0;
		this.backtrace = -1;

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

	public PRecord getPredecessor() {
		return predecessor;
	}

	public double getTotalCost() {
		return cost + estimate;
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
	public PRecord getNextRecord(Delegate<? extends Head, ? extends Tail> d, Head nextHead, long state, int modelMove,
			int logMove) {
		AbstractPDelegate<? extends Tail> delegate = (AbstractPDelegate<?>) d;
		double c = delegate.getCostFor((short) modelMove, (short) logMove);

		PRecord r = new PRecord(state, cost + c, this, logMove, (short) modelMove, ((PHead) nextHead).getMarking()
				.getNumElts(), backtrace + 1);

		return r;
	}

	public double getEstimatedRemainingCost() {
		return estimate;
	}

	public void setEstimatedRemainingCost(double cost) {
		this.estimate = cost;

	}

	public boolean equals(Object o) {
		return (o instanceof Record) && ((Record) o).getState() == state;
	}

	public int hashCode() {
		return (int) state;
	}

	public String toString() {
		return "[s:" + state + " c:" + cost + " e:" + estimate + "]";
	}

	public int getModelMove() {
		return transition;
	}

	public static List<PRecord> getHistory(PRecord r) {
		if (r == null || r.getBackTraceSize() < 0) {
			return Collections.emptyList();
		}
		List<PRecord> history = new ArrayList<PRecord>(r.getBackTraceSize()+1);
		while (r.getPredecessor() != null) {
			history.add(0, r);
			r = r.getPredecessor();
		}
		return history;
	}

	public static void printRecord(AbstractPDelegate<?> delegate, int trace, PRecord r) {
		List<PRecord> history = getHistory(r);

		for (int i = 0; i < history.size(); i++) {
			r = history.get(i);
			String s = "(";
			short act = delegate.getActivityOf(trace, r.getMovedEvent());
			if (r.getModelMove() < 0) {
				s += "_";
			} else {
				short m = (short) r.getModelMove();
				// t is either a transition in the model, or Move.BOTTOM
				TShortSet acts = delegate.getActivitiesFor(m);
				if (acts == null || acts.isEmpty()) {
					s += delegate.getTransition(m);
				} else if (acts.contains(act)) {
					s += delegate.getEventClass(act);
				} else {
					s += delegate.getEventClass(acts.iterator().next());
				}
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

}
