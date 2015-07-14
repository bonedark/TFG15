package org.processmining.plugins.boudewijn.treebasedreplay;

import gnu.trove.list.array.TIntArrayList;

public class Effect extends TIntArrayList {

	private int columnNumber;

	private int node;

	private int moveCount;

	public Effect(int capacity, int node, int moveCount) {
		super(capacity);
		this.setNode(node);
		this.moveCount = moveCount;
	}

	public void setColumnNumber(int columnNumber) {
		this.columnNumber = columnNumber;
	}

	public int getColumnNumber() {
		return columnNumber;
	}

	public int getNode() {
		return node;
	}

	public void setNode(int node) {
		this.node = node;
	}

	public int moveCount() {
		return moveCount;
	}

	public String toString() {
		return super.toString() + " mc:" + moveCount;
	}

	public void incMoveCount() {
		moveCount++;
	}
}
