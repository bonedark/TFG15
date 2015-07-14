package org.processmining.plugins.boudewijn.treebasedreplay;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TShortIterator;
import gnu.trove.list.TIntList;
import gnu.trove.procedure.TShortShortProcedure;

import java.util.List;
import java.util.Map;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;

import org.processmining.plugins.astar.petrinet.impl.LPProblemProvider;
import org.processmining.plugins.astar.petrinet.impl.LPResult;
import org.processmining.plugins.astar.petrinet.impl.ShortShortMultiset;
import org.processmining.plugins.boudewijn.tree.Node;

public class TreeILPDelegate extends AbstractTreeDelegate<TreeILPTail> {

	private final TreeTailCompressor tailCompressor;
	private final LPProblemProvider solvers;
	private final boolean useInt = true;
	private final int threads;

	static {
		System.loadLibrary("lpsolve55");
		System.loadLibrary("lpsolve55j");
	}

	public TreeILPDelegate(AStarAlgorithm algorithm, Node root, Map<Node, Integer> node2Cost,
			int threads) {
		super(algorithm, root, node2Cost, threads);
		// Setup the LP problem
		this.threads = threads;

		LpSolve solver = null;
		LPProblemProvider solvers = null;
		int effectColumn = 2 * nodes + numEventClasses();
		try {
			// The variables (columns):
			// The number of nodes (for model move only. If the leaf is a loop-leaf
			//  the contains the non-penalized effect of removing it
			// The number of nodes (for sync move. If the leaf is a loop-leaf, the column
			//  contains the first effect  
			// The number of activities (for log-move only)
			// The remaining effects (are irrelevant)

			// The rows (constraints)
			// The number of nodes (for loop-leafs, the row stays 0)
			// The number of activities

			solver = LpSolve.makeLp(nodes + numEventClasses(), vars);

			// Set the modelMoves
			for (short node = 0; node < nodes; node++) {
				if (index2node[node].isLeaf()) {
					solver.setMat(node + 1, node + 1, 1);
				}
				if (index2node[node].getClazz() == null) {
					// a node with at least 1 effect
					List<Effect> eftcs = node2effects.get(node);
					TIntIterator it = eftcs.get(0).iterator();
					while (it.hasNext()) {
						int n = it.next();
						if (n == node) {
							continue;
						}
						if (n >= 0) {
							solver.setMat(n + 1, nodes + node + 1, -1);
						}
					}
					eftcs.get(0).setColumnNumber(nodes + node);
					columnNumber2effect.put(nodes + node, eftcs.get(0));
					// set objective
					solver.setMat(0, nodes + node + 1, eftcs.get(0).moveCount());
					if (node > 0) {
						solver.setMat(node + 1, nodes + node + 1, 0);
					} else {
						solver.setMat(node + 1, nodes + node + 1, 1);
					}

					for (int e = 1; e < eftcs.size(); e++) {
						it = eftcs.get(e).iterator();
						effectColumn++;
						while (it.hasNext()) {
							int n = it.next();
							if (n == node) {
								continue;
							}
							if (n >= 0) {
								solver.setMat(n + 1, effectColumn, -1);
							}
						}
						eftcs.get(e).setColumnNumber(effectColumn - 1);
						columnNumber2effect.put(effectColumn - 1, eftcs.get(e));
						// set lowbo
						solver.setLowbo(effectColumn, 0);
						// type
						solver.setInt(effectColumn, useInt);
						// set objective
						solver.setMat(0, effectColumn, eftcs.get(e).moveCount());
						if (node > 0) {
							solver.setMat(node + 1, effectColumn, 0);
						} else {
							solver.setMat(node + 1, effectColumn, 1);
						}
					}
				} else {
					// a mappable leaf-node
					solver.setMat(node + 1, nodes + node + 1, 1);
					// objective function for move model only
					solver.setMat(0, node + 1, getModelMoveCost(node));
					// objective function for synchronous move
					solver.setMat(0, nodes + node + 1, 1);
				}
				TShortIterator it = getActivitiesFor(node).iterator();
				while (it.hasNext()) {
					short a = it.next();
					solver.setMat(nodes + 1 + a, nodes + node + 1, 1);
				}
				// bounds
				solver.setLowbo(node + 1, 0);
				solver.setLowbo(nodes + node + 1, 0);
				// type
				solver.setInt(node + 1, useInt);
				solver.setInt(nodes + node + 1, useInt);
				solver.setLowbo(nodes + node + 1, 0);
				// row type
				solver.setConstrType(node + 1, LpSolve.EQ);

			}
			// Set the logMoves
			for (short a = 0; a < numEventClasses(); a++) {
				solver.setMat(nodes + 1 + a, 2 * nodes + 1 + a, 1);
				// objective function
				solver.setMat(0, 2 * nodes + 1 + a, getLogMoveCost(a));
				// bounds
				solver.setLowbo(2 * nodes + 1 + a, 0);
				// type
				solver.setInt(2 * nodes + 1 + a, useInt);
				// row type
				solver.setConstrType(nodes + 1 + a, LpSolve.EQ);
			}

			solver.setMinim();

			solver.setVerbose(1);

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

			//solver.printLp();
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

		this.tailCompressor = new TreeTailCompressor(solver.getNcolumns(), nodes, numEventClasses());

	}

	public void deleteLPs() {
		for (int i = 0; i < threads; i++) {
			LpSolve solver = solvers.firstAvailable();
			solver.deleteLp();
		}
	}

	public LPResult estimate(TIntList marked, ShortShortMultiset parikh) {
		final double[] rhs = new double[nodes + numEventClasses() + 1];
		TIntIterator it = marked.iterator();
		while (it.hasNext()) {
			rhs[it.next() + 1] = 1;
		}
		parikh.forEachEntry(new TShortShortProcedure() {

			public boolean execute(short a, short b) {
				rhs[nodes + a + 1] = b;
				return true;
			}
		});

		LpSolve solver = solvers.firstAvailable();
		// the stupid estimate is always a correct estimate
		LPResult res = new LPResult(solver.getNcolumns(), 0);
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
			assert (r != LpSolve.INFEASIBLE);

			if (r == LpSolve.OPTIMAL) {
				// the solution was optimal, hence a better solution is found.
				// From suboptimal solutions, no conclusions can be drawn.
				res = new LPResult(solver.getNcolumns(), solver.getObjective());
				solver.getVariables(res.getVariables());
			}
			return res;

		} catch (LpSolveException e) {
			return res;
		} finally {
			solvers.finished(solver);
		}
	}

	public TreeILPTail createTail(TreeHead head) {
		return new TreeILPTail(this, head);
	}

	public TreeTailCompressor getTailInflater() {
		return tailCompressor;
	}

	public TreeTailCompressor getTailDeflater() {
		return tailCompressor;
	}

	public int getScaling() {
		return scaling;
	}

}
