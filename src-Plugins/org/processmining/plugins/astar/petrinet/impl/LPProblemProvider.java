package org.processmining.plugins.astar.petrinet.impl;

import java.util.ArrayList;
import java.util.List;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;

public class LPProblemProvider {

	private final List<LpSolve> problems;

	public LPProblemProvider(LpSolve problem, int num) throws LpSolveException {
		problems = new ArrayList<LpSolve>(num);
		for (int i = 0; i < num; i++) {
			problems.add(problem.copyLp());
		}
	}

	/**
	 * Returns the first available solver. If no solver is available, the
	 * current thread is blocked until one becomes available.
	 * 
	 * The returned solver can be used without the need for synchronizing on it.
	 * Furthermore, once finished with a solver, it should be returned in the
	 * finished method.
	 * 
	 * It is good practice to call the finished() method from a finally block
	 * after catching any exception coming from the solver, to make sure no
	 * solvers ever get lost.
	 * 
	 * @return
	 */
	public LpSolve firstAvailable() {
		synchronized (this) {
			while (problems.isEmpty()) {
				try {
					this.wait();
				} catch (InterruptedException e) {
				}
			}
			return problems.remove(0);
		}
	}

	/**
	 * 
	 * Signals that this solver is done and can be used by another thread.
	 * 
	 * It is good practice to call the finished() method from a finally block
	 * after catching any exception coming from the solver, to make sure no
	 * solvers ever get lost.
	 * 
	 * @param solver
	 */
	public void finished(LpSolve solver) {
		synchronized (this) {
			problems.add(solver);
			this.notify();
		}
	}
}
