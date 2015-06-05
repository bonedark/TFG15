/**
 * 
 */
package org.processmining.plugins.flex.replayresult.performance;

import org.processmining.plugins.flex.replayer.performance.util.CaseInstanceAccumulator;

/**
 * @author aadrians
 *
 */
public class CaseInstance {

	private double avgTimeSpan;
	private int frequency;
	private long maxTimeSpan;
	private long minTimeSpan;
	private double stdDevTimeSpan;

	public CaseInstance(CaseInstanceAccumulator caseAcc) {
		this.avgTimeSpan = caseAcc.getAvgTime();
		this.frequency = caseAcc.getFrequency();
		this.maxTimeSpan = caseAcc.getMaxTimeSpan();
		this.minTimeSpan = caseAcc.getMinTimeSpan();
		this.stdDevTimeSpan = caseAcc.getStdDevTime();
	}

	/**
	 * @return the avgTimeSpan
	 */
	public double getAvgTimeSpan() {
		return avgTimeSpan;
	}

	/**
	 * @param avgTimeSpan the avgTimeSpan to set
	 */
	public void setAvgTimeSpan(double avgTimeSpan) {
		this.avgTimeSpan = avgTimeSpan;
	}

	/**
	 * @return the frequency
	 */
	public int getFrequency() {
		return frequency;
	}

	/**
	 * @param frequency the frequency to set
	 */
	public void setFrequency(int frequency) {
		this.frequency = frequency;
	}

	/**
	 * @return the maxTimeSpan
	 */
	public long getMaxTimeSpan() {
		return maxTimeSpan;
	}

	/**
	 * @param maxTimeSpan the maxTimeSpan to set
	 */
	public void setMaxTimeSpan(long maxTimeSpan) {
		this.maxTimeSpan = maxTimeSpan;
	}

	/**
	 * @return the minTimeSpan
	 */
	public long getMinTimeSpan() {
		return minTimeSpan;
	}

	/**
	 * @param minTimeSpan the minTimeSpan to set
	 */
	public void setMinTimeSpan(long minTimeSpan) {
		this.minTimeSpan = minTimeSpan;
	}

	/**
	 * @return the stdDevTimeSpan
	 */
	public double getStdDevTimeSpan() {
		return stdDevTimeSpan;
	}

	/**
	 * @param stdDevTimeSpan the stdDevTimeSpan to set
	 */
	public void setStdDevTimeSpan(double stdDevTimeSpan) {
		this.stdDevTimeSpan = stdDevTimeSpan;
	}
	
	

}
