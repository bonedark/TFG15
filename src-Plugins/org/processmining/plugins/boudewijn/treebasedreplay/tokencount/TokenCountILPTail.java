package org.processmining.plugins.boudewijn.treebasedreplay.tokencount;

import gnu.trove.list.TIntList;
import gnu.trove.procedure.TIntProcedure;

import java.io.IOException;
import java.io.InputStream;

import nl.tue.storage.CompressedStore;

import org.processmining.plugins.astar.interfaces.Delegate;
import org.processmining.plugins.astar.interfaces.Head;
import org.processmining.plugins.astar.interfaces.Move;
import org.processmining.plugins.astar.interfaces.Tail;
import org.processmining.plugins.astar.petrinet.impl.LPResult;
import org.processmining.plugins.boudewijn.tree.Node.Type;

import cern.colt.Arrays;

public class TokenCountILPTail implements Tail {

	// The maximum size of this.super:    24
	private final int estimate; //         8
	private final byte[] variables; //   24 + 2 * length   

	// the variables do not store an actual solution to the LP, instead, they store an approximation

	public static int LPSolved = 0;
	public static int LPDerived = 0;

	public TokenCountILPTail(TokenCountILPDelegate delegate, TokenCountHead h) {
		this(delegate, h, 0);
	}

	public TokenCountILPTail(TokenCountILPDelegate delegate, TokenCountHead h, int minCost) {
		LPSolved++;
		//System.out.println("LpSolve called: " + calls++ + " times");
		LPResult res = delegate.estimate(h.getMarking(), h.getParikhVector(), minCost);
		variables = new byte[res.getVariables().length];
		for (int i = 0; i < res.getVariables().length; i++) {
			int j = (int) (res.getVariable(i) + 0.5);
			variables[i] = (byte) j;
			assert variables[i] >= 0;
			if (variables[i] == 0) {
				continue;
			}
		}

		estimate = (int) (res.getResult() + 0.5);

		//System.out.println(h + " -> " + this);
	}

	public TokenCountILPTail(int estimate, byte[] variables) {
		this.estimate = estimate;
		this.variables = variables;

	}

	public Tail getNextTail(Delegate<? extends Head, ? extends Tail> d, Head head, int modelMove, int logMove,
			int activity) {
		TokenCountILPDelegate delegate = ((TokenCountILPDelegate) d);
		TIntList movesMade = ((TokenCountHead) head).getMovesMade();
		assert modelMove == Move.BOTTOM || movesMade != null;

		boolean isLoop = false;
		TokenCountILPTail newTail = null;
		// check if the is move was allowed according to the LP:
		int costMade = (movesMade == null ? 0 : movesMade.size());

		if (modelMove == Move.BOTTOM) {
			costMade += delegate.getLogMoveCost((short) activity);
			// logMove only. The variable is at location 3*delegate.numNodes()-delegate.numLeafs() + activity
			int var = 3 * delegate.numNodes() - delegate.numLeafs() + activity;
			if (variables[var] >= 1) {
				// move was allowed according to LP.
				byte[] newVars = new byte[variables.length];
				System.arraycopy(variables, 0, newVars, 0, variables.length);
				newVars[var] -= 1;
				newTail = new TokenCountILPTail(estimate - costMade, newVars);
			}
		} else {
			int var;
			if (logMove == Move.BOTTOM) {
				costMade += delegate.getModelMoveCost((short) modelMove);
				// there was a modelMove only, determine the newly enabled moves.
				var = modelMove;
				if (modelMove > delegate.numLeafs()) {
					var -= 2 * delegate.numLeafs();
					isLoop = delegate.getFunctionType(modelMove / 3) == Type.LOOP;
					// for loops, there needs to be a separate check.
				}
			} else {
				costMade += 1;
				// Synchronous move.
				var = 3 * delegate.numNodes() - 2 * delegate.numLeafs() + modelMove;
				assert (modelMove < delegate.numNodes());
			}

			// a loop can only be executed, as long as after executing it the 
			// remaining number of terminations is still less or equal to the
			// remaining number of loop executions.
			if ((variables[var] >= 1) && (!isLoop || variables[delegate.getRightChild(modelMove / 3)] > variables[var])) {

				// all moves made by the model, were allowed according to the LP
				// move was allowed according to LP.
				assert movesMade.forEach(new TIntProcedure() {
					public boolean execute(int value) {
						return variables[value] >= 1;
					}
				});

				final byte[] newVars = new byte[variables.length];
				System.arraycopy(variables, 0, newVars, 0, variables.length);

				newVars[var]--;
				if (movesMade != null) {
					movesMade.forEach(new TIntProcedure() {
						public boolean execute(int value) {
							newVars[value]--;
							assert newVars[value] >= 0;
							return true;
						}
					});
				}

				newTail = new TokenCountILPTail(estimate - costMade, newVars);
			}

		}

		if (newTail == null) {
			newTail = new TokenCountILPTail(delegate, (TokenCountHead) head, estimate - costMade);
		} else {
			//System.out.println(head + " -> " + newTail);
			LPDerived++;
			//			TokenCountILPTail t2 = new TokenCountILPTail(delegate, (TokenCountHead) head);
			//			assert (isLoop && newTail.estimate <= t2.estimate) || (newTail.estimate == t2.estimate);
		}

		assert newTail.getEstimate() >= estimate - costMade;
		return newTail;
	}

	public <S> Tail getNextTailFromStorage(Delegate<? extends Head, ? extends Tail> d, CompressedStore<S> store,
			long index, int modelMove, int logMove, int activity) throws IOException {
		InputStream in = store.getStreamForObject(index);
		((TokenCountILPDelegate) d).getTailDeflater().skipHead(in);
		return ((TokenCountILPDelegate) d).getTailInflater().inflate(in);
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

	public byte[] getVariables() {
		return variables;
	}

	public String toString() {
		return "e:" + estimate + " " + Arrays.toString(variables);
	}

}
