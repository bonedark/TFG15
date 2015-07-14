package org.processmining.plugins.boudewijn.treebasedreplay.tokencount;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import nl.tue.storage.CompressedStore;
import nl.tue.storage.Deflater;
import nl.tue.storage.Inflater;

import org.processmining.plugins.astar.interfaces.Delegate;
import org.processmining.plugins.astar.interfaces.Head;
import org.processmining.plugins.astar.interfaces.Tail;

public class TokenCountEmptyTail implements Tail, Inflater<TokenCountEmptyTail>, Deflater<TokenCountEmptyTail> {

	public static final TokenCountEmptyTail EMPTY = new TokenCountEmptyTail();

	private TokenCountEmptyTail() {

	}

	public Tail getNextTail(Delegate<? extends Head, ? extends Tail> d, Head oldHead, int modelMove, int logMove,
			int activity) {
		return EMPTY;
	}

	public <S> Tail getNextTailFromStorage(Delegate<? extends Head, ? extends Tail> d, CompressedStore<S> store,
			long index, int modelMove, int logMove, int activity) throws IOException {
		return EMPTY;
	}

	public double getEstimatedCosts(Delegate<? extends Head, ? extends Tail> d, Head h) {
		TokenCountHead head = (TokenCountHead) h;
		return head.getParikhVector().size();
	}

	public boolean canComplete() {
		return true;
	}

	public void deflate(TokenCountEmptyTail object, OutputStream stream) throws IOException {
	}

	public TokenCountEmptyTail inflate(InputStream stream) throws IOException {
		return EMPTY;
	}

	public int getMaxByteCount() {
		return 1;
	}
}
