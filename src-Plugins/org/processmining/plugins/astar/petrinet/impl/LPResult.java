package org.processmining.plugins.astar.petrinet.impl;

public class LPResult {

	public LPResult(int variableCount, double result) {
		this.variables = new double[variableCount];
		this.result = result;

	}

	public double[] getVariables() {
		return variables;
	}

	public double getResult() {
		return result;
	}

	private final double[] variables;

	private final double result;

	public double getVariable(int i) {
		return variables[i];
	}

}
