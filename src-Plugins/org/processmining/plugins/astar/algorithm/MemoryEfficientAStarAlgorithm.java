package org.processmining.plugins.astar.algorithm;

import nl.tue.storage.CompressedHashSet;
import nl.tue.storage.CompressedStore;
import nl.tue.storage.impl.CompressedStoreHashSetImpl;

import org.processmining.plugins.astar.interfaces.Delegate;
import org.processmining.plugins.astar.interfaces.Head;
import org.processmining.plugins.astar.interfaces.Tail;

public class MemoryEfficientAStarAlgorithm<H extends Head, T extends Tail> {

	private final CompressedHashSet<State<H, T>> statespace;
	private final CompressedStore<State<H, T>> store;
	private final Delegate<H, T> delegate;
	private final StateCompressor<H, T> compressor;

	public MemoryEfficientAStarAlgorithm(Delegate<H, T> delegate) {
		this.compressor = new StateCompressor<H, T>(delegate);
		this.delegate = delegate;
		this.statespace = new CompressedStoreHashSetImpl.Int32G<State<H, T>>(compressor, compressor, 2 * 1024 * 1024,
				compressor, compressor, 2 * 1024 * 1024);
		this.store = statespace.getBackingStore();
		delegate.setStateSpace(statespace);
	}

	public CompressedHashSet<State<H, T>> getStatespace() {
		return statespace;
	}

	public CompressedStore<State<H, T>> getStore() {
		return store;
	}

	public Delegate<H, T> getDelegate() {
		return delegate;
	}

}
