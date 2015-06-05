package org.processmining.plugins.boudewijn.treebasedreplay;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TShortIterator;
import gnu.trove.set.TShortSet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import nl.tue.storage.CompressedStore;
import nl.tue.storage.Deflater;
import nl.tue.storage.Inflater;

import org.processmining.plugins.astar.interfaces.Delegate;
import org.processmining.plugins.astar.interfaces.Head;
import org.processmining.plugins.astar.interfaces.Tail;
import org.processmining.plugins.astar.petrinet.impl.ShortShortMultiset;

public class TreeEmptyTail implements Tail, Inflater<TreeEmptyTail>, Deflater<TreeEmptyTail> {

	public static final TreeEmptyTail EMPTY = new TreeEmptyTail();

	private TreeEmptyTail() {

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
		TreeHead head = (TreeHead) h;
		TreeDelegate<?, ?> delegate = (TreeDelegate<?, ?>) d;

		// the marked nodes which are not mapped to anything in the parikh vector have
		// to be "moved on model"
		double minCost = 0;
		ShortShortMultiset parikh = head.getParikhVector();
		TIntIterator it = head.getMarked().iterator();
		markedNodeLoop: while (it.hasNext()) {
			int node = it.next();
			TShortSet acts = delegate.getActivitiesFor(node);
			// check if any of the activities appears in the parikhvector
			TShortIterator it2 = acts.iterator();
			while (it2.hasNext()) {
				if (parikh.get(it2.next()) > 0) {
					continue markedNodeLoop;
				}
			}
			minCost += delegate.getModelMoveCost(node);
		}
		return minCost;
	}

	public boolean canComplete() {
		return true;
	}

	public void deflate(TreeEmptyTail object, OutputStream stream) throws IOException {
	}

	public int getMaxByteCount() {
		return 1;
	}

	public TreeEmptyTail inflate(InputStream stream) throws IOException {
		return EMPTY;
	}
}
