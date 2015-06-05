/**
 * 
 */
package org.processmining.plugins.flex.replayresult.performance;

/**
 * @author aadrians
 *
 */
public class EdgeInstance {
	private int frequency = 0;
	private int numCaseInvolved = 0;
	
	// moving time
	private long minMoveTime = Long.MAX_VALUE;
	private long maxMoveTime = Long.MIN_VALUE;
	private double avgMoveTime = Double.NaN;
	private double stdDevMoveTime = Double.NaN;
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
	 * @return the numCaseInvolved
	 */
	public int getNumCaseInvolved() {
		return numCaseInvolved;
	}

	/**
	 * @param numCaseInvolved the numCaseInvolved to set
	 */
	public void setNumCaseInvolved(int numCaseInvolved) {
		this.numCaseInvolved = numCaseInvolved;
	}
	/**
	 * @return the minMoveTime
	 */
	public long getMinMoveTime() {
		return minMoveTime;
	}
	/**
	 * @param minMoveTime the minMoveTime to set
	 */
	public void setMinMoveTime(long minMoveTime) {
		this.minMoveTime = minMoveTime;
	}
	/**
	 * @return the maxMoveTime
	 */
	public long getMaxMoveTime() {
		return maxMoveTime;
	}
	/**
	 * @param maxMoveTime the maxMoveTime to set
	 */
	public void setMaxMoveTime(long maxMoveTime) {
		this.maxMoveTime = maxMoveTime;
	}
	/**
	 * @return the avgMoveTime
	 */
	public double getAvgMoveTime() {
		return avgMoveTime;
	}
	/**
	 * @param avgMoveTime the avgMoveTime to set
	 */
	public void setAvgMoveTime(double avgMoveTime) {
		this.avgMoveTime = avgMoveTime;
	}
	/**
	 * @return the stdDevMoveTime
	 */
	public double getStdDevMoveTime() {
		return stdDevMoveTime;
	}
	/**
	 * @param stdDevMoveTime the stdDevMoveTime to set
	 */
	public void setStdDevMoveTime(double stdDevMoveTime) {
		this.stdDevMoveTime = stdDevMoveTime;
	}
	
	
	
}
