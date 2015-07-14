package org.processmining.plugins.boudewijn.treebasedreplay;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;

import org.processmining.plugins.astar.algorithm.AStarException;
import org.processmining.plugins.astar.algorithm.AbstractAStarThread;
import org.processmining.plugins.astar.algorithm.State;
import org.processmining.plugins.astar.interfaces.Delegate;
import org.processmining.plugins.astar.interfaces.Head;
import org.processmining.plugins.astar.interfaces.Move;
import org.processmining.plugins.astar.interfaces.Record;
import org.processmining.plugins.astar.interfaces.Tail;

public abstract class AbstractGreedyAStarThread<H extends Head, T extends Tail> extends AbstractAStarThread<H, T> {

	public AbstractGreedyAStarThread(Delegate<H, T> delegate, TIntList trace, int maxStates) {
		super(delegate, trace, maxStates);
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
			int nextEvent = rec.getNextEvent(delegate);

			TIntList enabled = head.getModelMoves(rec, delegate);
			//			if (enabled.containsAll(new int[] { 0, 1 })) {
			//				TreeRecord.printRecord((TreeDelegate) delegate, trace, (TreeRecord) rec);
			//			}
			TIntList ml = null;

			if (nextEvent < trace.size()) {
				// move both log and model synchronously;
				int activity = trace.get(nextEvent);
				ml = head.getModelMoves(rec, delegate, enabled, activity);
				TIntIterator it = ml.iterator();
				while (it.hasNext()) {
					processMove(head, tail, rec, it.next(), nextEvent, activity);
				}

				if ((ml.isEmpty() && (rec.getPredecessor() == null || rec.getMovedEvent() != Move.BOTTOM))) {
					// move on log only
					// if greedy, only allow log-moves if:
					// 1) no synchronous move was possible
					// 2) the state was not reached through modelMoves
					processMove(head, tail, rec, Move.BOTTOM, nextEvent, activity);
				}

			}
			// if greedy, only allow model moves if no synchronous move was possible
			TIntIterator it = enabled.iterator();
			while (it.hasNext()) {
				processMove(head, tail, rec, it.next(), Move.BOTTOM, Move.BOTTOM);
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

}
