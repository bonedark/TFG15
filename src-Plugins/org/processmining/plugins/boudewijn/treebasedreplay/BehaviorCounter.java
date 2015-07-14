package org.processmining.plugins.boudewijn.treebasedreplay;

public class BehaviorCounter {
	/**
	 * the number of times first left, then right was executed.
	 */
	public int behavedAsSEQLR = 0;
	/**
	 * the number of times first right, then left was executed.
	 */
	public int behavedAsSEQRL = 0;
	/**
	 * the number of times left and right were executed in parallel (interleaved
	 * in time)
	 */
	public int behavedAsAND = 0;
	/**
	 * The number of times only left was executed. For leafs, the number of
	 * times the leaf was executed synchronously. For loops the number of times
	 * the left child was executed
	 */
	public int behavedAsL = 0;
	/**
	 * the number of times the right node was executed for loops, the number of
	 * times the "redo" activity was executed
	 */
	public int behavedAsR = 0;
	/**
	 * the number of traces in which this node was not used
	 */
	public int notUsed = 0;
	/**
	 * the number of times this node was executed as a move on model (leafs
	 * only).
	 */
	public int moveModelOnly = 0;

	public BehaviorCounter() {
	}

	public BehaviorCounter(BehaviorCounter behC) {
		this.behavedAsAND = behC.behavedAsAND;
		this.behavedAsL = behC.behavedAsL;
		this.behavedAsR = behC.behavedAsR;
		this.behavedAsSEQLR = behC.behavedAsSEQLR;
		this.behavedAsSEQRL = behC.behavedAsSEQRL;
		this.notUsed = behC.notUsed;
		this.moveModelOnly = behC.moveModelOnly;
	}

	public String toString() {
		return "    As SEQLR  : " + behavedAsSEQLR + " \n" + //
				"    As SEQRL  : " + behavedAsSEQRL + " \n" + //
				"    As AND    : " + behavedAsAND + " \n" + //
				"    As L      : " + behavedAsL + " \n" + //
				"    As R      : " + behavedAsR + " \n" + //
				"    Model only: " + moveModelOnly + " \n" + //
				"    Unused    : " + notUsed + " \n";
	}

	/**
	 * Returns true if at least one of the values is not 0, e.g. is set
	 * 
	 * @return
	 */
	public boolean isSet() {
		return !((behavedAsAND == 0) && (behavedAsL == 0) && (behavedAsR == 0) && (behavedAsSEQLR == 0)
				&& (behavedAsSEQRL == 0) && (notUsed == 0) && (moveModelOnly == 0));
	}
}
