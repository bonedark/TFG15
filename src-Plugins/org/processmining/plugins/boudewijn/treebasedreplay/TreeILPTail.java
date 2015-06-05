package org.processmining.plugins.boudewijn.treebasedreplay;

import java.io.IOException;
import java.io.InputStream;

import nl.tue.storage.CompressedStore;

import org.processmining.plugins.astar.interfaces.Delegate;
import org.processmining.plugins.astar.interfaces.Head;
import org.processmining.plugins.astar.interfaces.Move;
import org.processmining.plugins.astar.interfaces.Tail;
import org.processmining.plugins.astar.petrinet.impl.LPResult;

public class TreeILPTail implements Tail {

	// The maximum size of this.super:    24
	private final int estimate; //      8
	private final short[] variables; //   24 + 2 * length   

	public static int LPSolved = 0;
	public static int LPDerived = 0;

	public static int getSizeFor(int variables) {
		return 8 * (1 + (56 + 2 * variables - 1) / 8);
	}

	public TreeILPTail(TreeILPDelegate delegate, TreeHead h) {
		LPSolved++;
		//System.out.println("LpSolve called: " + calls++ + " times");
		LPResult res = delegate.estimate(h.getMarked(), h.getParikhVector());
		int cost = 0;
		variables = new short[res.getVariables().length];
		for (int i = 0; i < res.getVariables().length; i++) {
			variables[i] = (short) Math.round(res.getVariable(i));
			if (variables[i] == 0) {
				continue;
			}
			if (i < delegate.numNodes()) {
				cost += variables[i] * (delegate.getModelMoveCost(i));
			} else if (i < 2 * delegate.numNodes()) {
				// check for sync moves
				if ((delegate.numNodes() > 1 && i == delegate.numNodes())
						|| delegate.isLoopLeafOrLoop(i - delegate.numNodes())) {
					cost += variables[i] * delegate.getEffectForColumnNumber(i).moveCount();
				} else {
					cost += variables[i];
				}
			} else if (i < 2 * delegate.numNodes() + delegate.numEventClasses()) {
				cost += variables[i] * (delegate.getLogMoveCost(i - 2 * delegate.numNodes()));
			} else {
				cost += variables[i] * delegate.getEffectForColumnNumber(i).moveCount();
			}
		}
		assert (cost == (int) Math.round(res.getResult()));
		estimate = (int) Math.round(res.getResult());

	}

	TreeILPTail(int estimate, short[] variables) {
		LPDerived++;
		this.variables = variables;
		assert (estimate >= 0);
		this.estimate = estimate;

	}

	public Tail getNextTail(Delegate<? extends Head, ? extends Tail> d, Head head, int modelMove, int logMove,
			int activity) {

		TreeILPDelegate delegate = ((TreeILPDelegate) d);

		TreeILPTail newTail = null;
		// check if the is move was allowed according to the LP:
		if (modelMove == Move.BOTTOM) {
			// logMove only. The variable is at location 2*delegate.numNodes() + activity
			int var = 2 * delegate.numNodes() + activity;
			if (variables[var] >= 1) {
				// move was allowed according to LP.
				short[] newVars = new short[variables.length];
				System.arraycopy(variables, 0, newVars, 0, variables.length);
				newVars[var] -= 1;
				int newEst = estimate - (delegate.getLogMoveCost((short) activity));
				newTail = new TreeILPTail(newEst, newVars);
			}
		} else if (logMove == Move.BOTTOM) {
			// there was a modelMove only, determine the newly enabled moves.
			int var = modelMove;

			if (variables[var] >= 1) {

				// move was allowed according to LP.
				short[] newVars = new short[variables.length];
				System.arraycopy(variables, 0, newVars, 0, variables.length);
				newVars[var] -= 1;
				int newEst = estimate;

				if (modelMove < delegate.numNodes()) {
					// modelMove on leaf node
					newEst -= (delegate.getModelMoveCost((short) modelMove));
				} else {
					// modelMove on effect node
					newEst -= delegate.getEffectForColumnNumber(modelMove).moveCount();
				}
				newTail = new TreeILPTail(newEst, newVars);
			}

		} else {
			// Synchronous move. The variable is at location delegate.numNodes() + modelMove
			int var = delegate.numNodes() + modelMove;
			assert (modelMove < delegate.numNodes());
			if (variables[var] >= 1) {
				// move was allowed according to LP.
				short[] newVars = new short[variables.length];
				System.arraycopy(variables, 0, newVars, 0, variables.length);
				newVars[var] -= 1;
				int newEst = estimate;
				newEst -= 1;
				newTail = new TreeILPTail(newEst, newVars);
			}
		}
		if (newTail == null) {
			newTail = new TreeILPTail(delegate, (TreeHead) head);
		} else {
			//assert (Math.abs(newTail.estimate - new TreeILPTail(delegate, (TreeHead) head).estimate) < 1E-12);
		}

		return newTail;
	}

	public <S> Tail getNextTailFromStorage(Delegate<? extends Head, ? extends Tail> d, CompressedStore<S> store,
			long index, int modelMove, int logMove, int activity) throws IOException {
		InputStream in = store.getStreamForObject(index);
		((TreeILPDelegate) d).getTailDeflater().skipHead(in);
		return ((TreeILPDelegate) d).getTailInflater().inflate(in);
	}

	public double getEstimatedCosts(Delegate<? extends Head, ? extends Tail> d, Head head) {
		return getEstimate();
	}

	public boolean canComplete() {
		return true;
	}

	public int getEstimate() {
		return estimate;
	}

	public short[] getVariables() {
		return variables;
	}

}
