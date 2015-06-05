/**
 * 
 */
package org.processmining.plugins.astar.algorithm;

import gnu.trove.list.TIntList;

import org.processmining.plugins.astar.interfaces.Head;
import org.processmining.plugins.astar.interfaces.Record;
import org.processmining.plugins.astar.interfaces.Tail;

/**
 * @author aadrians
 * Dec 22, 2011
 *
 */
public class MemoryEfficientAStarTreeStateSpaceThread <H extends Head, T extends Tail> extends MemoryEfficientAStarThread<H, T>{

	public MemoryEfficientAStarTreeStateSpaceThread(MemoryEfficientAStarAlgorithm<H, T> algorithm, H initialHead,
			TIntList trace, int maxStates) {
		super(algorithm, initialHead, trace, maxStates);
	}
	
	@SuppressWarnings("unchecked")
	protected void processMove(H head, T tail, Record rec, int modelMove, int movedEvent, int logMove)
			throws AStarException {
		// First, construct the next head from the old head
		H newHead = (H) head.getNextHead(rec, delegate, modelMove, movedEvent, logMove);

		// create a record for this new head
		final Record newRec = rec.getNextRecord(delegate, newHead, -1, modelMove, logMove);
		traversedArcCount++;

		double c = queue.contains(newRec);
		if (c >= 0 && c <= newRec.getTotalCost()) {
			// The new record is already in the queue, or a record with the same
			// state and lower cost exists.
			// this implies that newState was already fully present in the statespace
			return;
		}

		final T newTail;

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

}
