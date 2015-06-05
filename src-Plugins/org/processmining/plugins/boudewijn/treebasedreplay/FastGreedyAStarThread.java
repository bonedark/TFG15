package org.processmining.plugins.boudewijn.treebasedreplay;

import gnu.trove.list.TIntList;
import gnu.trove.map.TObjectIntMap;

import java.util.List;

import org.processmining.plugins.astar.algorithm.AStarException;
import org.processmining.plugins.astar.algorithm.State;
import org.processmining.plugins.astar.interfaces.Delegate;
import org.processmining.plugins.astar.interfaces.Head;
import org.processmining.plugins.astar.interfaces.Record;
import org.processmining.plugins.astar.interfaces.Tail;

public class FastGreedyAStarThread<H extends Head, T extends Tail> extends AbstractGreedyAStarThread<H, T> {

	private final TObjectIntMap<H> head2int;
	private final List<State<H, T>> stateList;
	private int newIndex;

	public FastGreedyAStarThread(Delegate<H, T> delegate, TObjectIntMap<H> head2long, List<State<H, T>> stateList,
			H initialHead, TIntList trace, int maxStates) throws AStarException {
		super(delegate, trace, maxStates);
		this.head2int = head2long;
		this.stateList = stateList;
		this.newIndex = stateList.size() + 1;
		// get the index where initialHead is stored
		initializeQueue(initialHead);
	}

	protected T getStoredTail(T tail, long index, int modelMove, int movedEvent, int logMove) {
		return stateList.get((int) index).getTail();
	}

	protected void storeStateForRecord(State<H, T> state, Record newRec) {
		synchronized (head2int) {
			stateList.add(state);
			head2int.put(state.getHead(), newIndex);
			newRec.setState(newIndex - 1);
			newIndex++;
		}
	}

	protected long getIndexOf(H head) {
		synchronized (head2int) {
			return head2int.get(head) - 1;
		}
	}

	protected State<H, T> getStoredState(Record rec) {
		return stateList.get((int) rec.getState());
	}

}
