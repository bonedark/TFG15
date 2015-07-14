package org.processmining.plugins.boudewijn.treebasedreplay.tokencount;

import gnu.trove.iterator.TShortIterator;
import gnu.trove.procedure.TShortShortProcedure;
import gnu.trove.set.TShortSet;

import java.util.Map;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;

import org.processmining.plugins.astar.interfaces.Tail;
import org.processmining.plugins.astar.petrinet.impl.LPProblemProvider;
import org.processmining.plugins.astar.petrinet.impl.LPResult;
import org.processmining.plugins.astar.petrinet.impl.ShortShortMultiset;
import org.processmining.plugins.boudewijn.tree.Node;
import org.processmining.plugins.boudewijn.tree.Node.Type;
import org.processmining.plugins.boudewijn.treebasedreplay.AStarAlgorithm;

public abstract class AbstractTokenCountILPDelegate<T extends Tail> extends AbstractTokenCountDelegate<T> {

	protected final LPProblemProvider solvers;
	protected final boolean useInt = true;
	protected final boolean useSemCon = false;
	protected int rows;
	protected int columns;
	private final int threads;

	// we use an upperbound of Byte.maxvalue in order to allow for more efficient storage

	static {
		System.loadLibrary("lpsolve55");
		System.loadLibrary("lpsolve55j");
	}

	public boolean isIntVariables() {
		return useInt;
	}

	public AbstractTokenCountILPDelegate(AStarAlgorithm algorithm, Node root, Map<Node, Integer> node2Cost,
			int threads, boolean greedy) {
		super(algorithm, root, node2Cost, threads, greedy);
		this.threads = threads;

		LpSolve solver = null;
		LPProblemProvider solvers = null;
		try {
			// The variables (columns):
			// all leafs for move model only
			// all non-leaf nodes (*3) for move model only
			// all leafs for sync move (and for loop leafs to allow the LP to remove pending tokens)
			// all activities for move log only

			// The rows (constraints)
			// The number of nodes 
			// The number of activities

			// count the number of loop nodes

			rows = nodes + classes + 1;

			columns = 3 * nodes - leafs + classes;
			solver = LpSolve.makeLp(rows, columns);
			int est = nodes + classes + 1;

			for (int i = leafs; i < nodes; i++) {
				int c = 1 + leafs + (i - leafs) * 3;
				// set the asynchronous move
				Type t = getFunctionType(i);
				if (t == Type.OR || t == Type.XOR) {
					// allow for right only and left only
					// set lowbo
					solver.setLowbo(c + TokenCountHead.L, 0);
					solver.setLowbo(c + TokenCountHead.R, 0);
					solver.setUpbo(c + TokenCountHead.L, Byte.MAX_VALUE);
					solver.setUpbo(c + TokenCountHead.R, Byte.MAX_VALUE);
					// type integer
					solver.setInt(c + TokenCountHead.L, useInt);
					solver.setInt(c + TokenCountHead.R, useInt);
					solver.setSemicont(c + TokenCountHead.L, useSemCon);
					solver.setSemicont(c + TokenCountHead.R, useSemCon);
					// set objective
					solver.setMat(0, c + TokenCountHead.L, getModelMoveCost(i));
					solver.setMat(0, c + TokenCountHead.R, getModelMoveCost(i));
					solver.setMat(est, c + TokenCountHead.L, getModelMoveCost(i));
					solver.setMat(est, c + TokenCountHead.R, getModelMoveCost(i));
					// set -effect on marking
					solver.setMat(i + 1, c + TokenCountHead.L, 1);
					solver.setMat(i + 1, c + TokenCountHead.R, 1);
					// non-leaf node
					// set the option to do left only
					solver.setMat(getLeftChild((short) i) + 1, c + TokenCountHead.L, -1);
					// set the option to do right only
					solver.setMat(getRightChild((short) i) + 1, c + TokenCountHead.R, -1);
				}
				if (t != Type.XOR) {
					// allow for both
					// set lowbo
					solver.setLowbo(c + TokenCountHead.B, 0);
					solver.setUpbo(c + TokenCountHead.B, Byte.MAX_VALUE);
					// type integer
					solver.setInt(c + TokenCountHead.B, useInt);
					solver.setSemicont(c + TokenCountHead.B, useSemCon);
					// set objective
					solver.setMat(0, c + TokenCountHead.B, getModelMoveCost(i));
					solver.setMat(est, c + TokenCountHead.B, getModelMoveCost(i));
					// set -effect on marking
					solver.setMat(i + 1, c + TokenCountHead.B, 1);
					// produce for both children
					solver.setMat(getRightChild((short) i) + 1, c + TokenCountHead.B, -1);
					solver.setMat(getLeftChild((short) i) + 1, c + TokenCountHead.B, -1);
				}
				if (t == Type.LOOP) {
					// do not remove your own input
					// set no effect on marking
					solver.setMat(i + 1, c + TokenCountHead.B, 0);

				}

			}
			for (int i = 0; i < leafs; i++) {
				int c = 1 + i;
				// set the asynchronous move
				// set lowbo
				solver.setLowbo(c, 0);
				solver.setUpbo(c, Byte.MAX_VALUE);
				// type integer
				solver.setInt(c, useInt);
				solver.setSemicont(c, true);
				// set objective
				solver.setMat(0, c, getModelMoveCost(i));
				solver.setMat(est, c, getModelMoveCost(i));
				// set -effect on marking
				solver.setMat(i + 1, c, 1);

				if (isLoopLeaf(i)) {
					// remove the parent's token (for free)
					solver.setMat(0, c, 0);
					solver.setMat(est, c, 0);
					solver.setMat(getParent(i) + 1, c, 1);
				}

				c = 3 * nodes - 2 * leafs + i + 1;
				// set the synchronous move
				// set lowbo
				solver.setLowbo(c, 0);
				solver.setUpbo(c, Byte.MAX_VALUE);
				// type integer
				solver.setInt(c, useInt);
				solver.setSemicont(c, useSemCon);
				// set objective
				solver.setMat(0, c, 1);
				solver.setMat(est, c, 1);
				// set -effect on marking
				solver.setMat(i + 1, c, 1);
				// set -effect on mapped activity (if any)
				TShortSet acts = getActivitiesFor(i);
				if (!acts.isEmpty()) {
					solver.setMat(acts.iterator().next() + nodes + 1, c, 1);
				}
				if (isLoopLeaf(i)) {
					// cleaning up the loop-leaf is free
					solver.setMat(0, c, 0);
					solver.setMat(est, c, 0);
				}

			}
			for (int i = 0; i < classes; i++) {
				int c = 3 * nodes - leafs + i + 1;
				// set the log moves
				// set lowbo
				solver.setLowbo(c, 0);
				solver.setUpbo(c, Byte.MAX_VALUE);
				// type integer
				solver.setInt(c, useInt);
				solver.setSemicont(c, useSemCon);
				// set objective
				solver.setMat(0, c, getLogMoveCost(i));
				solver.setMat(est, c, getLogMoveCost(i));
				// set -effect on parikh
				solver.setMat(i + nodes + 1, c, 1);
			}
			for (int i = 0; i < nodes + classes; i++) {
				// row type
				solver.setConstrType(i + 1, LpSolve.EQ);
			}
			solver.setConstrType(est, LpSolve.GE);

			solver.setMinim();

			solver.setScaling(LpSolve.SCALE_GEOMETRIC | LpSolve.SCALE_EQUILIBRATE | LpSolve.SCALE_INTEGERS);
			solver.setScalelimit(5);
			solver.setPivoting(LpSolve.PRICER_DEVEX | LpSolve.PRICE_ADAPTIVE);
			solver.setMaxpivot(250);
			solver.setBbFloorfirst(LpSolve.BRANCH_FLOOR);
			solver.setBbRule(LpSolve.NODE_PSEUDONONINTSELECT | LpSolve.NODE_GREEDYMODE | LpSolve.NODE_DYNAMICMODE
					| LpSolve.NODE_RCOSTFIXING);
			solver.setBbDepthlimit(-50);
			solver.setAntiDegen(LpSolve.ANTIDEGEN_FIXEDVARS | LpSolve.ANTIDEGEN_STALLING);
			solver.setImprove(LpSolve.IMPROVE_DUALFEAS | LpSolve.IMPROVE_THETAGAP);
			solver.setBasiscrash(LpSolve.CRASH_NOTHING);
			solver.setSimplextype(LpSolve.SIMPLEX_DUAL_PRIMAL);

			//solver.solve();
			//solver.printLp();

			solver.setVerbose(1);
			solvers = new LPProblemProvider(solver, threads);

		} catch (LpSolveException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			solver = null;
		} finally {
			this.solvers = solvers;
		}

		//		System.out.println("Max state size: "
		//				+ (16 + TreeHead.getSizeFor(nodes, numEventClasses()) + TreeTail.getSizeFor(vars)));

	}

	public AbstractTokenCountILPDelegate(AStarAlgorithm algorithm, Node root, Map<Node, Integer> node2Cost, int threads) {
		this(algorithm, root, node2Cost, threads, false);
	}

	public void deleteLPs() {
		for (int i = 0; i < threads; i++) {
			LpSolve solver = solvers.firstAvailable();
			solver.deleteLp();
		}
	}

	public void printLp() {
		LpSolve solver = solvers.firstAvailable();
		solver.printLp();
	}

	public boolean isFeasible(ReducedTokenCountMarking marking, ShortShortMultiset parikh, int minCost, byte[] vars,
			int estimate) {
		double[] rhs = setupRhs(marking, parikh, estimate, rows + columns + 1);
		for (int i = rows + 1; i < rows + columns + 1; i++) {
			rhs[i] = vars[i - rows - 1];
		}

		LpSolve solver = solvers.firstAvailable();
		try {
			return solver.isFeasible(rhs, solver.getEpsint());
		} catch (LpSolveException e) {
			return false;
		}
	}

	private double[] setupRhs(ReducedTokenCountMarking marking, ShortShortMultiset parikh, int minCost, int length) {
		final double[] rhs = new double[length];
		TShortIterator it = marking.enabledIterator();
		while (it.hasNext()) {
			rhs[it.next() + 1] = 1;
		}
		it = marking.futureIterator();
		while (it.hasNext()) {
			rhs[it.next() + 1] = 1;
		}
		parikh.forEachEntry(new TShortShortProcedure() {

			public boolean execute(short a, short b) {
				rhs[nodes + a + 1] = b;
				return true;
			}
		});
		rhs[nodes + numEventClasses() + 1] = minCost;
		return rhs;
	}

	public LPResult estimate(ReducedTokenCountMarking marking, ShortShortMultiset parikh, int minCost) {
		double[] rhs = setupRhs(marking, parikh, minCost, rows + 1);
		LpSolve solver = solvers.firstAvailable();

		// the stupid estimate is always a correct estimate
		LPResult res = new LPResult(columns, 0);
		try {
			// instead of setting the default basis, we call resetBasis() whenever we get
			// the INFEASIBLE answer. The model is guaranteed to be FEASIBLE, hence
			// such answer is wrong and resetting the basis in that case should do the
			// trick.
			//
			solver.defaultBasis();
			solver.setRhVec(rhs);
			int r = solver.solve();
			//			if (r == LpSolve.INFEASIBLE) {
			//				// reset basis and try again.
			//				solver.resetBasis();
			//				r = solver.solve();
			//			}
			if (r == LpSolve.INFEASIBLE) {
				// a node needs to be used more than Byte.MAX_VALUE times, which this IP does not allow
				// return the trivial estimate
				res = new LPResult(columns, Math.max(minCost, parikh.getNumElts()));
			}

			if (r == LpSolve.OPTIMAL || r == LpSolve.PRESOLVED) {
				// the solution was optimal, hence a better solution is found.
				// From suboptimal solutions, no conclusions can be drawn.
				//				double[] primalResult = new double[1 + rows + columns];
				//				solver.getPrimalSolution(primalResult);
				//				System.arraycopy(primalResult, 1, res.getVariables(), 0, columns);

				res = new LPResult(columns, solver.getObjective());
				solver.getVariables(res.getVariables());
			}
			return res;

		} catch (LpSolveException e) {
			return res;
		} finally {
			solvers.finished(solver);
		}
	}
}