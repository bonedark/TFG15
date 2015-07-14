/**
 * 
 */
package org.processmining.plugins.flex.replayresult;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.processmining.plugins.replayer.replayresult.SyncReplayResult;

/**
 * @author Arya Adriansyah
 * @email a.adriansyah@tue.nl
 * @version Feb 12, 2011
 */
public class FlexRepResult extends TreeSet<SyncReplayResult> {
	private static final long serialVersionUID = 7651153275954857033L;

	private int weightMoveOnLogOnly = 0;
	private int weightMoveOnModelOnlyInvi = 0;
	private int weightMoveOnModelOnlyReal = 0;
	private int weightViolation = 0;

	public FlexRepResult(Collection<SyncReplayResult> col, int weightMoveOnLogOnly, int weightMoveOnModelOnlyInvi,
			int weightMoveOnModelOnlyReal, int weightViolation) {
		super(new Comparator<SyncReplayResult>() {

			public int compare(SyncReplayResult o1, SyncReplayResult o2) {
				SortedSet<Integer> s1 = o1.getTraceIndex();
				SortedSet<Integer> s2 = o2.getTraceIndex();
				if (s1.size() != s2.size()) {
					return s2.size() - s1.size();
				}
				if (o1.equals(o2)) {
					return 0;
				}
				if (o1.getStepTypes().size() != o2.getStepTypes().size()) {
					return o2.getStepTypes().size() - o1.getStepTypes().size();
				}
				Iterator<Integer> it1 = s1.iterator();
				Iterator<Integer> it2 = s2.iterator();
				while (it1.hasNext()) {
					Integer ss1 = it1.next();
					Integer ss2 = it2.next();
					if (!ss1.equals(ss2)) {
						return ss1.compareTo(ss2);
					}
				}
				return 0;
			}

		});
		addAll(col);
		this.weightMoveOnLogOnly = weightMoveOnLogOnly;
		this.weightMoveOnModelOnlyInvi = weightMoveOnModelOnlyInvi;
		this.weightMoveOnModelOnlyReal = weightMoveOnModelOnlyReal;
		this.weightViolation = weightViolation;
	}

	/**
	 * @return the weightMoveOnLogOnly
	 */
	public int getWeightMoveOnLogOnly() {
		return weightMoveOnLogOnly;
	}

	/**
	 * @param weightMoveOnLogOnly
	 *            the weightMoveOnLogOnly to set
	 */
	public void setWeightMoveOnLogOnly(int weightMoveOnLogOnly) {
		this.weightMoveOnLogOnly = weightMoveOnLogOnly;
	}

	/**
	 * @return the weightMoveOnModelOnlyInvi
	 */
	public int getWeightMoveOnModelOnlyInvi() {
		return weightMoveOnModelOnlyInvi;
	}

	/**
	 * @param weightMoveOnModelOnlyInvi
	 *            the weightMoveOnModelOnlyInvi to set
	 */
	public void setWeightMoveOnModelOnlyInvi(int weightMoveOnModelOnlyInvi) {
		this.weightMoveOnModelOnlyInvi = weightMoveOnModelOnlyInvi;
	}

	/**
	 * @return the weightMoveOnModelOnlyReal
	 */
	public int getWeightMoveOnModelOnlyReal() {
		return weightMoveOnModelOnlyReal;
	}

	/**
	 * @param weightMoveOnModelOnlyReal
	 *            the weightMoveOnModelOnlyReal to set
	 */
	public void setWeightMoveOnModelOnlyReal(int weightMoveOnModelOnlyReal) {
		this.weightMoveOnModelOnlyReal = weightMoveOnModelOnlyReal;
	}

	/**
	 * @return the weightViolation
	 */
	public int getWeightViolation() {
		return weightViolation;
	}

	/**
	 * @param weightViolation
	 *            the weightViolation to set
	 */
	public void setWeightViolation(int weightViolation) {
		this.weightViolation = weightViolation;
	}

}
