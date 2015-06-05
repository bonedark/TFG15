package org.processmining.plugins.astar.algorithm;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;

import org.processmining.plugins.astar.interfaces.Delegate;
import org.processmining.plugins.astar.interfaces.Head;
import org.processmining.plugins.astar.interfaces.Move;
import org.processmining.plugins.astar.interfaces.Record;
import org.processmining.plugins.astar.interfaces.Tail;

public abstract class AbstractAStarThread<H extends Head, T extends Tail> implements AStarThread<H, T> {

	protected final FastLookupPriorityQueue queue;
	protected final TIntList trace;
	protected final int maxStates;
	protected int queuedStateCount = 0;
	protected int traversedArcCount = 0;
	protected final Delegate<H, T> delegate;

	public AbstractAStarThread(Delegate<H, T> delegate, TIntList trace, int maxStates) {
		super();
		this.delegate = delegate;
		this.trace = trace;
		this.maxStates = maxStates;
		this.queue = new FastLookupPriorityQueue(1000);
	}

	public Record run(Canceller c) throws AStarException {
		return run(c, Double.MAX_VALUE);
	}

	public Record run(Canceller c, double stopAt) throws AStarException {
		State<H, T> state;
		Record rec = null;
		H head = null;
		T tail = null;
		while (!queue.isEmpty() && !c.isCancelled()) {
			// Debug code for checking the queue invariant before every poll.
			//			if (!queue.checkInv()) {
			//				System.err.println(queue.checkInv());
			//			}
			rec = queue.poll();
			try {
				state = getStoredState(rec);
			} catch (Exception e) {
				throw new AStarException(e);
			}
			head = state.getHead();
			tail = state.getTail();

			//			System.out.println(rec.getCostSoFar() + "  " + rec.getTotalCost() + "  " + head + "  "
			//					+ tail.getEstimatedCosts(delegate, head));
			//System.out.println(states);
			if (queuedStateCount >= maxStates || rec.getTotalCost() >= stopAt) {
				return null;
			}

			if (head.isFinal(delegate)) {
				return rec;
			}

			// move model only
			TIntList enabled = head.getModelMoves(rec, delegate);
			TIntList ml = null;

			int nextEvent = rec.getNextEvent(delegate);
			if (nextEvent < trace.size()) {
				// move both log and model synchronously;
				int activity = trace.get(nextEvent);
				ml = head.getModelMoves(rec, delegate, enabled, activity);
				TIntIterator it = ml.iterator();
				while (it.hasNext()) {
					processMove(head, tail, rec, it.next(), nextEvent, activity);
				}

				// move log.
				//				if (rec.getPredecessor() == null || rec.getMovedEvent() != Move.BOTTOM) {
				processMove(head, tail, rec, Move.BOTTOM, nextEvent, activity);
				//				}

			}

			if (rec.getPredecessor() == null || rec.getModelMove() != Move.BOTTOM) {
				TIntIterator it = enabled.iterator();
				while (it.hasNext()) {
					// move model
					processMove(head, tail, rec, it.next(), Move.BOTTOM, Move.BOTTOM);
				}
			}

		}

		//		 unreliable, best guess: rec
		if (head != null) {
			System.out.println(head);
			System.out.println(head.isFinal(delegate));
			System.out.println(tail);
			System.out.println(head.getModelMoves(rec, delegate));
		}
		return rec;

	}

	@SuppressWarnings("unchecked")
	protected void processMove(H head, T tail, Record rec, int modelMove, int movedEvent, int logMove)
			throws AStarException {
		// First, construct the next head from the old head
		H newHead = (H) head.getNextHead(rec, delegate, modelMove, movedEvent, logMove);
		long index;
		try {
			index = getIndexOf(newHead);
		} catch (Exception e) {
			throw new AStarException(e);
		}

		// create a record for this new head
		final Record newRec = rec.getNextRecord(delegate, newHead, index, modelMove, logMove);
		traversedArcCount++;

		double c = queue.contains(newRec);
		if (c >= 0 && c <= newRec.getTotalCost()) {
			// The new record is already in the queue, or a record with the same
			// state and lower cost exists.
			// this implies that newState was already fully present in the statespace
			return;
		}

		final T newTail;
		if (index >= 0) {
			// so far, we did the cheap stuff, now get the new tail from storage, knowing that
			// a state exists at index 
			try {
				newTail = getStoredTail(tail, index, modelMove, movedEvent, logMove);
			} catch (Exception e) {
				throw new AStarException(e);
			}

			double h = newTail.getEstimatedCosts(delegate, newHead);

			newRec.setState(index);
			newRec.setEstimatedRemainingCost(h);
			if (queue.add(newRec)) {
				queuedStateCount++;
			}

			return;

		}

		// the statespace doesn't contain a corresponding state, hence we need to compute the tail.
		newTail = (T) tail.getNextTail(delegate, newHead, modelMove, movedEvent, logMove);

		if (!newTail.canComplete()) {
			return;
		}

		double h = newTail.getEstimatedCosts(delegate, newHead);

		// Check if the head is in the store and add if it isn't.
		final State<H, T> newState = new State<H, T>(newHead, newTail);

		try {
			storeStateForRecord(newState, newRec);
		} catch (Exception e) {
			throw new AStarException(e);
		}

		//          State<H, T> ret = store.getObject(r.index);
		//			if (!ret.equals(newState)) {
		//				System.err.println("Retrieval error");
		//			}

		//assert (r.isNew);
		newRec.setEstimatedRemainingCost(h);
		if (queue.add(newRec)) {
			queuedStateCount++;
		}

	}

	@SuppressWarnings({ "cast" })
	protected void initializeQueue(H head) throws AStarException {
		//	time = System.currentTimeMillis();
		// First, find the location of head
		final long index = getIndexOf(head);
		// note that the contains method may give false negatives. However, 
		// it is assumed to be more expensive to synchronize on (algorithm) than to 
		// just recompute the tail.

		// create a record for this new head
		final Record newRec = delegate.createInitialRecord(head);
		traversedArcCount++;

		if (index < 0) {
			// the statespace doesn't contain a corresponding state
			final T tail = (T) delegate.createTail(head);
			final State<H, T> newState = new State<H, T>(head, tail);

			storeStateForRecord(newState, newRec);

		} else {
			newRec.setState(index);
		}
		if (queue.add(newRec)) {
			queuedStateCount++;
		}

	}

	public int getQueuedStateCount() {
		return queuedStateCount;
	}

	public int getTraversedArcCount() {
		return traversedArcCount;
	}

	public String toString() {
		return queue.size() + ":" + queue.toString();
	}

	protected abstract T getStoredTail(T tail, long index, int modelMove, int movedEvent, int logMove)
			throws AStarException;

	protected abstract void storeStateForRecord(State<H, T> state, Record newRec) throws AStarException;

	protected abstract long getIndexOf(H head) throws AStarException;

	protected abstract State<H, T> getStoredState(Record rec) throws AStarException;

}