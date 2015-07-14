package org.processmining.plugins.astar.algorithm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import nl.tue.storage.CompressedStore;
import nl.tue.storage.Deflater;
import nl.tue.storage.EqualOperation;
import nl.tue.storage.HashOperation;
import nl.tue.storage.Inflater;
import nl.tue.storage.StorageException;

import org.processmining.plugins.astar.interfaces.Delegate;
import org.processmining.plugins.astar.interfaces.Head;
import org.processmining.plugins.astar.interfaces.Tail;

public class StateCompressor<H extends Head, T extends Tail> implements Inflater<State<H, T>>, Deflater<State<H, T>>,
		EqualOperation<State<H, T>>, HashOperation<State<H, T>> {

	private Delegate<H, T> delegate;

	public StateCompressor(Delegate<H, T> delegate) {
		this.delegate = delegate;
	}

	public int getHashCode(State<H, T> state) {
		return delegate.getHeadBasedHashOperation().getHashCode(state);
	}

	public int getHashCode(CompressedStore<State<H, T>> store, long l) throws StorageException {
		return delegate.getHeadBasedHashOperation().getHashCode(store, l);
	}

	public boolean equals(State<H, T> object, CompressedStore<State<H, T>> store, long l) throws StorageException,
			IOException {
		return delegate.getHeadBasedEqualOperation().equals(object, store, l);
	}

	public void deflate(State<H, T> object, OutputStream stream) throws IOException {
		delegate.getHeadDeflater().deflate(object.getHead(), stream);
		delegate.getTailDeflater().deflate(object.getTail(), stream);
	}

	public State<H, T> inflate(InputStream stream) throws IOException {
		H head = delegate.getHeadInflater().inflate(stream);
		T tail = delegate.getTailInflater().inflate(stream);
		return new State<H, T>(head, tail);
	}

	public int getMaxByteCount() {
		return delegate.getHeadDeflater().getMaxByteCount() + delegate.getTailDeflater().getMaxByteCount();
	}

}
