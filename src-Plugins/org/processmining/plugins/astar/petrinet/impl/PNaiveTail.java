package org.processmining.plugins.astar.petrinet.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import nl.tue.storage.CompressedStore;
import nl.tue.storage.Deflater;
import nl.tue.storage.Inflater;

import org.processmining.plugins.astar.interfaces.Delegate;
import org.processmining.plugins.astar.interfaces.Head;
import org.processmining.plugins.astar.interfaces.Tail;

/**
 * Implementation of the tail that implements the estimate equal to the cost of all synchronous moves
 * 
 * @author aadrians
 * Dec 22, 2011
 *
 */
public class PNaiveTail implements Tail, Deflater<PNaiveTail>, Inflater<PNaiveTail> {

	public static final PNaiveTail EMPTY = new PNaiveTail();

	private PNaiveTail() {

	}

	public Tail getNextTail(Delegate<? extends Head, ? extends Tail> d, Head oldHead, int modelMove, int logMove,
			int activity) {
		return EMPTY;
	}

	public <S> Tail getNextTailFromStorage(Delegate<? extends Head, ? extends Tail> d, CompressedStore<S> store,
			long index, int modelMove, int logMove, int activity) throws IOException {
		return EMPTY;
	}

	public double getEstimatedCosts(Delegate<? extends Head, ? extends Tail> d, Head head) {
		return ((PHead) head).getParikhVector().getNumElts() * ((AbstractPDelegate<?>) d).getDelta();
	}

	public boolean canComplete() {
		return true;
	}

	public void deflate(PNaiveTail object, OutputStream stream) throws IOException {
	}

	public PNaiveTail inflate(InputStream stream) throws IOException {
		return EMPTY;
	}

	public int getMaxByteCount() {
		return -1;
	}

}
