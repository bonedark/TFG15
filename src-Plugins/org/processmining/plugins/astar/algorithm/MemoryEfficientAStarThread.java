package org.processmining.plugins.astar.algorithm;

import gnu.trove.list.TIntList;

import java.io.IOException;

import nl.tue.storage.CompressedHashSet;
import nl.tue.storage.CompressedStore;
import nl.tue.storage.StorageException;
import nl.tue.storage.impl.CompressedStoreHashSetImpl.Result;

import org.processmining.plugins.astar.interfaces.Head;
import org.processmining.plugins.astar.interfaces.Record;
import org.processmining.plugins.astar.interfaces.Tail;

public class MemoryEfficientAStarThread<H extends Head, T extends Tail> extends AbstractAStarThread<H, T> {

	protected final MemoryEfficientAStarAlgorithm<H, T> algorithm;
	protected final CompressedStore<State<H, T>> store;
	private final CompressedHashSet<State<H, T>> statespace;

	public MemoryEfficientAStarThread(MemoryEfficientAStarAlgorithm<H, T> algorithm, H initialHead, TIntList trace,
			int maxStates) {
		super(algorithm.getDelegate(), trace, maxStates);
		this.algorithm = algorithm;
		this.statespace = algorithm.getStatespace();
		this.store = algorithm.getStore();

		// get the index where initialHead is stored
		try {
			initializeQueue(initialHead);
		} catch (Exception e) {
			new RuntimeException(e);
		}

	}

	@SuppressWarnings("unchecked")
	protected T getStoredTail(T tail, long index, int modelMove, int movedEvent, int logMove) throws AStarException {
		try {
			return (T) tail.getNextTailFromStorage(delegate, store, index, modelMove, movedEvent, logMove);
		} catch (IOException e) {
			throw new AStarException(e);
		}
	}

	protected void storeStateForRecord(State<H, T> state, Record newRec) throws AStarException {
		final Result<State<H, T>> r;
		synchronized (statespace) {
			try {
				r = statespace.add(state);
			} catch (StorageException e) {
				throw new AStarException(e);
			}
		}
		newRec.setState(r.index);
	}

	protected long getIndexOf(H head) throws AStarException {
		synchronized (statespace) {
			try {
				return statespace.contains(new State<H, T>(head, null));
			} catch (StorageException e) {
				throw new AStarException(e);
			}
		}

	}

	protected State<H, T> getStoredState(Record rec) throws AStarException {
		try {
			return rec.getState(store);
		} catch (StorageException e) {
			throw new AStarException(e);
		}
	}
}
