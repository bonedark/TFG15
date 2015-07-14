package org.processmining.plugins.boudewijn.treebasedreplay;

public class TreeMove {

	private final int node;
	private final int[] successors;

	public TreeMove(int node, int... successors) {
		this.node = node;
		this.successors = successors;

	}

	public int getNode() {
		return node;
	}

	public int[] getSuccessors() {
		return successors;
	}
}
