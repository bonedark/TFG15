package org.processmining.plugins.boudewijn.treebasedreplay.tokencount;

import gnu.trove.list.TIntList;

import java.io.IOException;
import java.io.InputStream;

import nl.tue.storage.CompressedStore;

import org.processmining.plugins.astar.interfaces.Delegate;
import org.processmining.plugins.astar.interfaces.Head;
import org.processmining.plugins.astar.interfaces.Move;
import org.processmining.plugins.astar.interfaces.Tail;
import org.processmining.plugins.astar.petrinet.impl.LPResult;

public class TokenCountHybridTail implements Tail {

	// The maximum size of this.super:    24
	private final int estimate; //         8

	// the variables do not store an actual solution to the LP, instead, they store an approximation

	public static int LPSolved = 0;
	public static int LPDerived = 0;

	public TokenCountHybridTail(TokenCountHybridDelegate delegate, TokenCountHead h) {
		this(delegate, h, 0);
	}

	public TokenCountHybridTail(TokenCountHybridDelegate delegate, TokenCountHead h, int minCost) {
		LPSolved++;
		//System.out.println("LpSolve called: " + calls++ + " times");
		LPResult res = delegate.estimate(h.getMarking(), h.getParikhVector(), minCost);
		estimate = (int) (res.getResult() + 0.5);

		//System.out.println(h + " -> " + this);
	}

	public TokenCountHybridTail(int estimate) {
		this.estimate = estimate;
	}

	public Tail getNextTail(Delegate<? extends Head, ? extends Tail> d, Head h, int modelMove, int logMove, int activity) {
		TokenCountHybridDelegate delegate = ((TokenCountHybridDelegate) d);
		TokenCountHead head = (TokenCountHead) h;
		TIntList movesMade = head.getMovesMade();

		assert modelMove == Move.BOTTOM || movesMade != null;

		TokenCountHybridTail newTail = null;
		// check if the is move was allowed according to the LP:
		int costMade = (movesMade == null ? 0 : movesMade.size());
		if (modelMove == Move.BOTTOM) {
			costMade += delegate.getLogMoveCost((short) activity);
		} else if (logMove == Move.BOTTOM) {
			costMade += delegate.getModelMoveCost((short) modelMove);
		} else {
			costMade += 1;
		}

		if (estimate - costMade < head.getParikhVector().getNumElts()
				|| estimate - costMade < head.getMarking().numEnabled() + head.getMarking().numFuture()) {
			newTail = new TokenCountHybridTail(delegate, head, estimate - costMade);

		} else {
			LPDerived++;
			newTail = new TokenCountHybridTail(estimate - costMade);
		}

		return newTail;
	}

	public <S> Tail getNextTailFromStorage(Delegate<? extends Head, ? extends Tail> d, CompressedStore<S> store,
			long index, int modelMove, int logMove, int activity) throws IOException {
		InputStream in = store.getStreamForObject(index);
		((TokenCountHybridDelegate) d).getTailDeflater().skipHead(in);
		return ((TokenCountHybridDelegate) d).getTailInflater().inflate(in);
	}

	public double getEstimatedCosts(Delegate<? extends Head, ? extends Tail> d, Head head) {
		return estimate;
	}

	public boolean canComplete() {
		return true;
	}

	public int getEstimate() {
		return estimate;
	}

	public String toString() {
		return "e:" + estimate;
	}

}
