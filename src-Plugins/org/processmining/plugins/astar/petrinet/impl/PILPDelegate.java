package org.processmining.plugins.astar.petrinet.impl;

import gnu.trove.iterator.TShortIterator;
import gnu.trove.procedure.TShortShortProcedure;

import java.io.IOException;
import java.util.Map;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;
import nl.tue.storage.CompressedHashSet;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.model.XLog;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.astar.algorithm.State;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;

public class PILPDelegate extends AbstractPDelegate<PILPTail> {

	private final PILPTailCompressor tailCompressor;
	private final LPProblemProvider solvers;
	private final Marking[] finalMarkings;
	private final int threads;

	public PILPDelegate(Petrinet net, XLog log, XEventClasses classes, TransEvClassMapping map,
			Map<Transition, Integer> mapTrans2Cost, Map<XEventClass, Integer> mapEvClass2Cost, double delta,
			int threads, Marking... set) {
		super(net, log, classes, map, mapTrans2Cost, mapEvClass2Cost, delta, set);
		this.threads = threads;

		if (set.length == 0) {
			throw new IllegalArgumentException("Cannot use ILP without final markings");
			//			// assume the empty marking that needs to be reached.
			//			set = new Marking[1];
			//			set[0] = new Marking();
		}
		this.tailCompressor = new PILPTailCompressor(2 * transitions + activities + set.length, places, activities);
		//	createBaseLPProblem();
		// Setup the LP problem
		System.loadLibrary("lpsolve55");
		System.loadLibrary("lpsolve55j");

		boolean useInts = true;
		LpSolve solver = null;
		LPProblemProvider solvers = null;
		try {
			// The variables (columns):
			// The number of transitions for modelmove only
			// The number of transitions for sync move
			// The number of activities for log-move only
			// One transition foe each final marking 

			// The rows (constraints)
			// The number of places
			// The number of activities
			// one constraint summing the final marking transitions to 1

			// The structure of the LP matrix is as follows:
			//    A A 0 C        mc (current marking)
			//    0 B I 0 . x =  pv (parkikh vector)  
			//    0 0 0 1        1
			//
			// With A the negative incidence matrix, B the mapping between events and transitions and C the incidence matrix for final markings

			solver = LpSolve.makeLp(places + activities + 1, 2 * transitions + activities + set.length);

			// First, matrix A
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> e : net.getEdges()) {
				if (e instanceof Arc) {
					short p, t;
					int dir;
					if (e.getSource() instanceof Place) {
						p = place2int.get(e.getSource());
						t = trans2int.get(e.getTarget());
						dir = ((Arc) e).getWeight();
					} else {
						t = trans2int.get(e.getSource());
						p = place2int.get(e.getTarget());
						dir = -((Arc) e).getWeight();
					}
					if (set.length > 0) {
						// final markings are given, i.e. the
						// net should be emptied
						solver.setConstrType(p + 1, LpSolve.EQ);
					} else {
						// no final markings are given, i.e. any
						// final marking is acceptable
						solver.setConstrType(p + 1, LpSolve.LE);
					}
					solver.setMat(p + 1, t + 1, solver.getMat(p + 1, t + 1) + dir);
					solver.setMat(p + 1, transitions + t + 1, solver.getMat(p + 1, transitions + t + 1) + dir);
				}
			}
			for (short t = 0; t < transitions; t++) {
				solver.setInt(t + 1, useInts);
				solver.setInt(transitions + t + 1, useInts);
				solver.setLowbo(t + 1, 0);
				solver.setLowbo(transitions + t + 1, 0);
				solver.setUpbo(t + 1, Short.MAX_VALUE - 1);
				solver.setUpbo(transitions + t + 1, Short.MAX_VALUE - 1);
				solver.setMat(0, t + 1, getCostForMoveModel(t));
				solver.setMat(0, transitions + t + 1, getDelta());
			}

			// Then, matrix B
			for (short a = 0; a < activities; a++) {
				TShortIterator it = actIndex2trans.get(a).iterator();
				while (it.hasNext()) {
					short t = it.next();
					solver.setMat(places + a + 1, transitions + t + 1, 1.0);
				}
				solver.setInt(2 * transitions + a + 1, useInts);
				solver.setLowbo(2 * transitions + a + 1, 0);
				solver.setUpbo(2 * transitions + a + 1, Short.MAX_VALUE - 1);
				solver.setConstrType(places + a + 1, LpSolve.EQ);
				solver.setMat(places + a + 1, 2 * transitions + a + 1, 1.0);
				solver.setMat(0, 2 * transitions + a + 1, getCostForMoveLog(a));
			}

			// Then, matrix C and the last row
			int c = 2 * transitions + activities + 1;
			for (Marking m : set) {
				for (Place place : m) {
					short p = place2int.get(place);
					solver.setMat(p + 1, c, 1.0);
				}
				solver.setMat(places + activities + 1, c, 1.0);
				solver.setBinary(c, true);
				c++;
			}
			solver.setConstrType(places + activities + 1, LpSolve.EQ);

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
			solvers = new LPProblemProvider(solver, threads);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			this.solvers = solvers;
			this.finalMarkings = set;
		}
		System.out.println("Max state size: "
				+ (PHead.getSizeFor(places, activities) + PILPTail.getSizeFor(2 * transitions + activities
						+ finalMarkings.length)));
	}
	
	public void deleteLPs() {
		for (int i = 0; i < threads; i++) {
			LpSolve solver = solvers.firstAvailable();
			solver.deleteLp();
		}
	}


	public LPResult estimate(ShortShortMultiset marking, ShortShortMultiset parikh) {
		final double[] rhs = new double[places + activities + 2];

		marking.forEachEntry(new TShortShortProcedure() {

			public boolean execute(short a, short b) {
				rhs[a + 1] = b;
				return true;
			}
		});

		parikh.forEachEntry(new TShortShortProcedure() {

			public boolean execute(short a, short b) {
				rhs[places + a + 1] = b;
				return true;
			}
		});
		rhs[places + activities + 1] = 1;

		LpSolve solver = solvers.firstAvailable();
		try {
			solver.defaultBasis();
			solver.setRhVec(rhs);
			int r = solver.solve();
			if (r == LpSolve.OPTIMAL) {
				LPResult res = new LPResult(solver.getNcolumns(), solver.getObjective());
				solver.getVariables(res.getVariables());
				return res;
			} else if (r == LpSolve.NOMEMORY) {
				LPResult res = new LPResult(solver.getNcolumns(), parikh.numElts * getDelta());
				return res;
			}
			return null;
		} catch (LpSolveException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} finally {
			solvers.finished(solver);
		}

	}

	/**
	 * Loads the required jar and dll files (from the location) provided by the
	 * user via the settings if not loaded already and creates a solverfactory
	 * 
	 * @return solverfactory
	 * @throws IOException
	 */
	public PILPTail createTail(PHead head) {

		return new PILPTail(this, head);
	}

	public PILPTailCompressor getTailInflater() {
		return tailCompressor;
	}

	public PILPTailCompressor getTailDeflater() {
		return tailCompressor;
	}

	//	public List<Linear> getMarkingConstraints() {
	//		return markingConstraints;
	//	}

	//	public List<Linear> getParikhConstraints() {
	//		return parikhConstraints;
	//	}
	//
	//	public Problem getBaseILP(int upperbound) {
	//		Problem problem = new Problem();
	//		for (Constraint c : baseConstraints) {
	//			problem.add(c);
	//		}
	//		for (int i = 0; i < activities; i++) {
	//			problem.setVarType("x" + i, Integer.class);
	//			problem.setVarType("m" + i, Integer.class);
	//		}
	//		if (upperbound < Integer.MAX_VALUE) {
	//			problem.add(cost, Operator.LE, upperbound - 1);
	//		}
	//		problem.setObjective(cost, OptType.MIN);
	//		return problem;
	//	}
	//
	//	private void createBaseLPProblem() {
	//
	//		cost = new Linear();
	//
	//		// create a constraint for each place
	//		for (short p = 0; p < places; p++) {
	//			markingConstraints.add(new Linear());
	//		}
	//
	//		for (short t = 0; t < transitions; t++) {
	//			final int trans = t;
	//			final ShortShortMultiset in = transIndex2input.get(t);
	//			final ShortShortMultiset out = transIndex2output.get(t);
	//			for (int p = 0; p < places; p++) {
	//				markingConstraints.get(p).add(out.get((short) p) - in.get((short) p), "x" + trans);
	//			}
	//
	//			Linear l = new Linear();
	//			l.add(1, "m" + trans);
	//			baseConstraints.add(new Constraint(l, Operator.GE, 0));
	//
	//			l = new Linear();
	//			l.add(1, "x" + trans);
	//			l.add(-1, "m" + trans);
	//			baseConstraints.add(new Constraint(l, Operator.GE, 0));
	//
	//			cost.add(1 + getCostForMoveModel(t), "m" + trans);
	//		}
	//
	//		for (short a = 0; a < numEventClasses(); a++) {
	//			TShortIterator it = getTransitions(a).iterator();
	//			Linear l = new Linear();
	//			while (it.hasNext()) {
	//				int trans = it.next();
	//				l.add(1, "x" + trans);
	//				l.add(-1, "m" + trans);
	//
	//				cost.add(-1 - getCostForMoveLog(a), "x" + trans);
	//				cost.add(1 + getCostForMoveLog(a), "m" + trans);
	//			}
	//			parikhConstraints.add(l);
	//		}
	//	}

	public void setStateSpace(CompressedHashSet<State<PHead, PILPTail>> statespace) {

	}

	public int numFinalMarkings() {
		return finalMarkings.length;
	}

}
