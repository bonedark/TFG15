/**
 * 
 */
package org.processmining.plugins.flex.replayer.performance.util;

/**
 * @author aadrians
 * 
 */
public class CaseInstanceAccumulator {
	private int frequency = 0;

	private long sumTimeSpan = 0;
	private long minTimeSpan = Long.MAX_VALUE;
	private long maxTimeSpan = Long.MIN_VALUE;

	// required to calculate standard deviation
	// based on http://mathcentral.uregina.ca/QQ/database/QQ.09.02/carlos1.html
	private double mTime = Double.NaN;
	private double sTime = 0;

	public void incFrequency() {
		frequency++;
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
	 * @return the sumTimeSpan
	 */
	public long getSumTimeSpan() {
		return sumTimeSpan;
	}

	/**
	 * @param sumTimeSpan
	 *            the sumTimeSpan to set
	 */
	public void setSumTimeSpan(long sumTimeSpan) {
		this.sumTimeSpan = sumTimeSpan;
	}

	/**
	 * @return the minTimeSpan
	 */
	public long getMinTimeSpan() {
		return minTimeSpan;
	}

	/**
	 * @param minTimeSpan
	 *            the minTimeSpan to set
	 */
	public void setMinTimeSpan(long minTimeSpan) {
		this.minTimeSpan = minTimeSpan;
	}

	/**
	 * @return the maxTimeSpan
	 */
	public long getMaxTimeSpan() {
		return maxTimeSpan;
	}

	/**
	 * @param maxTimeSpan
	 *            the maxTimeSpan to set
	 */
	public void setMaxTimeSpan(long maxTimeSpan) {
		this.maxTimeSpan = maxTimeSpan;
	}

	public double getAvgTime() {
		if (frequency > 0) {
			return (double) sumTimeSpan / (double) frequency;
		} else {
			return 0;
		}
	}

	public double getStdDevTime() {
		if ((frequency - 1) > 0) {
			return Math.sqrt(sTime / (frequency - 1));
		} else {
			return 0;
		}
	}

	public void addCaseTime(long newCaseTime) {
		incFrequency();
		sumTimeSpan += newCaseTime;
		if (newCaseTime < minTimeSpan) {
			minTimeSpan = newCaseTime;
		}
		if (newCaseTime > maxTimeSpan) {
			maxTimeSpan = newCaseTime;
		}

		// calculate for deviation 
		if (Double.isNaN(mTime)) {
			mTime = newCaseTime;
			sTime = 0;
		} else {
			// not the first value
			double oldMTime = mTime;
			mTime += ((newCaseTime - mTime)/getFrequency());
			sTime += ((newCaseTime - oldMTime) * (newCaseTime - mTime)); 
		}
	}
}
