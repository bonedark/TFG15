/**
 * 
 */
package org.processmining.plugins.flex.replayer.performance.util;

/**
 * @author aadrians
 * 
 */
public class EdgeInstanceAccumulator {
	private int frequency = 0;
	private int numCaseInvolved = 0;

	private long sumMoveTimeSpan = 0;
	private long minMoveTimeSpan = Long.MAX_VALUE;
	private long maxMoveTimeSpan = Long.MIN_VALUE;

	// required to calculate standard deviation
	private double mMove = Double.NaN;
	private double sMove = 0;

	private int frequencyRemainingObligations = 0;

	public void incFrequency() {
		frequency++;
	}

	public void incFreqRemainingObligations(int remainingNumber) {
		frequencyRemainingObligations += remainingNumber;
	}

	/**
	 * @return the frequencyRemainingObligations
	 */
	public int getFrequencyRemainingObligations() {
		return frequencyRemainingObligations;
	}

	/**
	 * @param frequencyRemainingObligations
	 *            the frequencyRemainingObligations to set
	 */
	public void setFrequencyRemainingObligations(int frequencyRemainingObligations) {
		this.frequencyRemainingObligations = frequencyRemainingObligations;
	}

	public void addMoveTime(long newMoveTime) {
		frequency++;
		sumMoveTimeSpan += newMoveTime;
		if (newMoveTime < minMoveTimeSpan) {
			minMoveTimeSpan = newMoveTime;
		}
		if (newMoveTime > maxMoveTimeSpan) {
			maxMoveTimeSpan = newMoveTime;
		}

		// calculate for deviation 
		if (Double.isNaN(mMove)) {
			mMove = newMoveTime;
			sMove = 0;
		} else {
			// not the first value
			double oldMMove = mMove;
			mMove += ((newMoveTime - mMove) / frequency);
			sMove += ((newMoveTime - oldMMove) * (newMoveTime - mMove));
		}
	}

	public double getAvgTime() {
		return (double) sumMoveTimeSpan / (double) frequency;
	}

	public double getStdDevMoveTime() {
		if ((frequency - 1) > 0) {
			return Math.sqrt(sMove / (frequency - 1));
		} else {
			return 0;
		}
	}

	/**
	 * @return the frequency
	 */
	public int getFrequency() {
		return frequency;
	}

	/**
	 * @param frequency
	 *            the frequency to set
	 */
	public void setFrequency(int frequency) {
		this.frequency = frequency;
	}

	/**
	 * @return the sumMoveTimeSpan
	 */
	public long getSumMoveTimeSpan() {
		return sumMoveTimeSpan;
	}

	/**
	 * @param sumMoveTimeSpan
	 *            the sumMoveTimeSpan to set
	 */
	public void setSumMoveTimeSpan(long sumMoveTimeSpan) {
		this.sumMoveTimeSpan = sumMoveTimeSpan;
	}

	/**
	 * @return the minMoveTimeSpan
	 */
	public long getMinMoveTimeSpan() {
		return minMoveTimeSpan;
	}

	/**
	 * @param minMoveTimeSpan
	 *            the minMoveTimeSpan to set
	 */
	public void setMinMoveTimeSpan(long minMoveTimeSpan) {
		this.minMoveTimeSpan = minMoveTimeSpan;
	}

	/**
	 * @return the maxMoveTimeSpan
	 */
	public long getMaxMoveTimeSpan() {
		return maxMoveTimeSpan;
	}

	/**
	 * @param maxMoveTimeSpan
	 *            the maxMoveTimeSpan to set
	 */
	public void setMaxMoveTimeSpan(long maxMoveTimeSpan) {
		this.maxMoveTimeSpan = maxMoveTimeSpan;
	}

	public void incCaseInvolved() {
		numCaseInvolved++;
	}

	/**
	 * @return the numCaseInvolved
	 */
	public int getNumCaseInvolved() {
		return numCaseInvolved;
	}

	/**
	 * @param numCaseInvolved
	 *            the numCaseInvolved to set
	 */
	public void setNumCaseInvolved(int numCaseInvolved) {
		this.numCaseInvolved = numCaseInvolved;
	}

}
