package org.processmining.plugins.astar.petrinet.impl;

import java.io.IOException;
import java.io.InputStream;

import nl.tue.storage.CompressedStore;

import org.processmining.plugins.astar.interfaces.Delegate;
import org.processmining.plugins.astar.interfaces.Head;
import org.processmining.plugins.astar.interfaces.Move;
import org.processmining.plugins.astar.interfaces.Tail;

public class PILPTail implements Tail {

	// The maximum size of this.super:    16
	private final double estimate; //      8
	private final short[] variables; //   24 + 2 * length   

	public static int getSizeFor(int variables) {
		return 8 * (1 + (56 + 2 * variables - 1) / 8);
	}

	public PILPTail(PILPDelegate delegate, PHead h) {
		LPResult res = delegate.estimate(h.getMarking(), h.getParikhVector());
		if (res == null) {
			estimate = -1.0;
			variables = new short[delegate.numTransitions() * 2 + delegate.numEventClasses()
					+ delegate.numFinalMarkings()];
		} else {
			estimate = res.getResult();

			variables = new short[res.getVariables().length];
			for (int i = 0; i < res.getVariables().length; i++) {
				variables[i] = (short) Math.round(res.getVariable(i));
			}
		}
	}

	PILPTail(double estimate, short[] variables) {
		if (estimate < 0) {
			this.estimate = 0;
		} else {
			this.estimate = estimate;
		}
		this.variables = variables;
	}

	public PILPTail getNextTail(Delegate<? extends Head, ? extends Tail> d, Head newHead, int modelMove, int logMove,
			int activity) {

		PILPDelegate delegate = ((PILPDelegate) d);

		// check if the is move was allowed according to the LP:
		if (modelMove == Move.BOTTOM) {
			// logMove only. The variable is at location 2*delegate.numTransitions() + activity
			int var = 2 * delegate.numTransitions() + activity;
			if (variables[var] >= 1) {
				// move was allowed according to LP.
				short[] newVars = new short[variables.length];
				System.arraycopy(variables, 0, newVars, 0, variables.length);
				newVars[var] -= 1;
				double newEst = estimate - delegate.getCostForMoveLog((short) activity);
				return new PILPTail(newEst, newVars);
			}
		} else if (logMove == Move.BOTTOM) {
			// there was a modelMove only, determine the newly enabled moves.
			int var = modelMove;

			if (variables[var] >= 1.0) {

				// move was allowed according to LP.
				short[] newVars = new short[variables.length];
				System.arraycopy(variables, 0, newVars, 0, variables.length);
				newVars[var] -= 1;
				double newEst = estimate - delegate.getCostForMoveModel((short) modelMove);
				return new PILPTail(newEst, newVars);
			}

		} else {
			// Synchronous move. The variable is at location delegate.numTransitions() + modelMove
			int var = delegate.numTransitions() + modelMove;
			if (variables[var] >= 1.0) {
				// move was allowed according to LP.
				short[] newVars = new short[variables.length];
				System.arraycopy(variables, 0, newVars, 0, variables.length);
				newVars[var] -= 1;
				double newEst = estimate - delegate.getDelta();
				return new PILPTail(newEst, newVars);
			}
		}
		return new PILPTail(delegate, (PHead) newHead);
	}

	public <S> Tail getNextTailFromStorage(Delegate<? extends Head, ? extends Tail> d, CompressedStore<S> store,
			long index, int modelMove, int logMove, int activity) throws IOException {
		InputStream in = store.getStreamForObject(index);
		((PILPDelegate) d).getTailDeflater().skipHead(in);
		return ((PILPDelegate) d).getTailInflater().inflate(in);
	}

	public double getEstimatedCosts(Delegate<? extends Head, ? extends Tail> d, Head head) {
		return getEstimate();
	}

	public boolean canComplete() {
		return estimate >= 0;
	}

	public double getEstimate() {
		return estimate;
	}

	public short[] getVariables() {
		return variables;
	}

}
