/**
 * 
 */
package org.processmining.plugins.flex.replayresult.performance;

/**
 * @author aadrians
 *
 */
public class FlexBindingInstance {
	private short encodedBinding; 
	private int frequency = 0;
	private int numCaseInvolved = 0;
	
	// working time
	private long minWorkTime = Long.MAX_VALUE;
	private long maxWorkTime = Long.MIN_VALUE;
	private double avgWorkTime = Double.NaN;
	private double stdDevWorkTime = Double.NaN;
	
	// synchronization time
	// the moment the first input activity finish until this task can be executed
	private long minSyncTime = Long.MAX_VALUE;
	private long maxSyncTime = Long.MIN_VALUE;
	private double avgSyncTime = Double.NaN;
	private double stdDevSyncTime = Double.NaN;
	
	// waiting time
	// the moment this task can be executed until it is really executed
	private long minWaitTime = Long.MAX_VALUE;
	private long maxWaitTime = Long.MIN_VALUE;
	private double avgWaitTime = Double.NaN;
	private double stdDevWaitTime = Double.NaN;
	
	@SuppressWarnings("unused")
	private FlexBindingInstance(){}
	
	public FlexBindingInstance(short encodedBinding){
		this.encodedBinding = encodedBinding;
	}

	public void incNumCaseInvolved() {
		numCaseInvolved++;
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
	 * @return the encodedBinding
	 */
	public short getEncodedBinding() {
		return encodedBinding;
	}

	/**
	 * @param encodedBinding the encodedBinding to set
	 */
	public void setEncodedBinding(short encodedBinding) {
		this.encodedBinding = encodedBinding;
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
	 * @return the minWorkTime
	 */
	public long getMinWorkTime() {
		return minWorkTime;
	}
	/**
	 * @param minWorkTime the minWorkTime to set
	 */
	public void setMinWorkTime(long minWorkTime) {
		this.minWorkTime = minWorkTime;
	}
	/**
	 * @return the maxWorkTime
	 */
	public long getMaxWorkTime() {
		return maxWorkTime;
	}
	/**
	 * @param maxWorkTime the maxWorkTime to set
	 */
	public void setMaxWorkTime(long maxWorkTime) {
		this.maxWorkTime = maxWorkTime;
	}
	/**
	 * @return the avgWorkTime
	 */
	public double getAvgWorkTime() {
		return avgWorkTime;
	}
	/**
	 * @param avgWorkTime the avgWorkTime to set
	 */
	public void setAvgWorkTime(double avgWorkTime) {
		this.avgWorkTime = avgWorkTime;
	}
	/**
	 * @return the stdDevWorkTime
	 */
	public double getStdDevWorkTime() {
		return stdDevWorkTime;
	}
	/**
	 * @param stdDevWorkTime the stdDevWorkTime to set
	 */
	public void setStdDevWorkTime(double stdDevWorkTime) {
		this.stdDevWorkTime = stdDevWorkTime;
	}
	/**
	 * @return the minSyncTime
	 */
	public long getMinSyncTime() {
		return minSyncTime;
	}
	/**
	 * @param minSyncTime the minSyncTime to set
	 */
	public void setMinSyncTime(long minSyncTime) {
		this.minSyncTime = minSyncTime;
	}
	/**
	 * @return the maxSyncTime
	 */
	public long getMaxSyncTime() {
		return maxSyncTime;
	}
	/**
	 * @param maxSyncTime the maxSyncTime to set
	 */
	public void setMaxSyncTime(long maxSyncTime) {
		this.maxSyncTime = maxSyncTime;
	}
	/**
	 * @return the avgSyncTime
	 */
	public double getAvgSyncTime() {
		return avgSyncTime;
	}
	/**
	 * @param avgSyncTime the avgSyncTime to set
	 */
	public void setAvgSyncTime(double avgSyncTime) {
		this.avgSyncTime = avgSyncTime;
	}
	/**
	 * @return the stdDevSyncTime
	 */
	public double getStdDevSyncTime() {
		return stdDevSyncTime;
	}
	/**
	 * @param stdDevSyncTime the stdDevSyncTime to set
	 */
	public void setStdDevSyncTime(double stdDevSyncTime) {
		this.stdDevSyncTime = stdDevSyncTime;
	}
	/**
	 * @return the minWaitTime
	 */
	public long getMinWaitTime() {
		return minWaitTime;
	}
	/**
	 * @param minWaitTime the minWaitTime to set
	 */
	public void setMinWaitTime(long minWaitTime) {
		this.minWaitTime = minWaitTime;
	}
	/**
	 * @return the maxWaitTime
	 */
	public long getMaxWaitTime() {
		return maxWaitTime;
	}
	/**
	 * @param maxWaitTime the maxWaitTime to set
	 */
	public void setMaxWaitTime(long maxWaitTime) {
		this.maxWaitTime = maxWaitTime;
	}
	/**
	 * @return the avgWaitTime
	 */
	public double getAvgWaitTime() {
		return avgWaitTime;
	}
	/**
	 * @param avgWaitTime the avgWaitTime to set
	 */
	public void setAvgWaitTime(double avgWaitTime) {
		this.avgWaitTime = avgWaitTime;
	}
	/**
	 * @return the stdDevWaitTime
	 */
	public double getStdDevWaitTime() {
		return stdDevWaitTime;
	}
	/**
	 * @param stdDevWaitTime the stdDevWaitTime to set
	 */
	public void setStdDevWaitTime(double stdDevWaitTime) {
		this.stdDevWaitTime = stdDevWaitTime;
	}

	
	

	
}
