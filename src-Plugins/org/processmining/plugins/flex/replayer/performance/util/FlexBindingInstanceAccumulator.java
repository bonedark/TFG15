/**
 * 
 */
package org.processmining.plugins.flex.replayer.performance.util;

/**
 * @author aadrians
 * 
 */
public class FlexBindingInstanceAccumulator {
	private int frequency = 0;
	private int incompleteFrequency = 0;
	
	private long sumWorkTimeSpan = 0;
	private long minWorkTimeSpan = Long.MAX_VALUE;
	private long maxWorkTimeSpan = Long.MIN_VALUE;
	
	private long sumSyncTimeSpan = 0;
	private long minSyncTimeSpan = Long.MAX_VALUE;
	private long maxSyncTimeSpan = Long.MIN_VALUE;
	
	private long sumWaitTimeSpan = 0;
	private long minWaitTimeSpan = Long.MAX_VALUE;
	private long maxWaitTimeSpan = Long.MIN_VALUE;
	
	// required to calculate standard deviation
	// based on http://mathcentral.uregina.ca/QQ/database/QQ.09.02/carlos1.html
	private double mWork = Double.NaN;
	private double sWork = 0;
	
	private double mSync = Double.NaN;
	private double sSync = 0;
	
	private double mWait = Double.NaN;
	private double sWait = 0;
	
	public void incFrequency(){
		frequency++;
	}


	public void addWorkTime(long newWorkTime) {
		sumWorkTimeSpan += newWorkTime;
		if (newWorkTime < minWorkTimeSpan){
			minWorkTimeSpan = newWorkTime;
		}
		if (newWorkTime > maxWorkTimeSpan){
			maxWorkTimeSpan = newWorkTime;
		}
	}

	public void addSyncTime(long newSyncTime) {
		sumSyncTimeSpan += newSyncTime;
		if (newSyncTime < minSyncTimeSpan){
			minSyncTimeSpan = newSyncTime;
		}
		if (newSyncTime > maxSyncTimeSpan){
			maxSyncTimeSpan = newSyncTime;
		}
	}

	public void addWaitTime(long newWaitTime) {
		sumWaitTimeSpan += newWaitTime;
		if (newWaitTime < minWaitTimeSpan){
			minWaitTimeSpan = newWaitTime;
		}
		if (newWaitTime > maxWaitTimeSpan){
			maxWaitTimeSpan = newWaitTime;
		}
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
	 * @return the minWorkTimeSpan
	 */
	public long getMinWorkTimeSpan() {
		return minWorkTimeSpan;
	}

	/**
	 * @param minWorkTimeSpan the minWorkTimeSpan to set
	 */
	public void setMinWorkTimeSpan(long minWorkTimeSpan) {
		this.minWorkTimeSpan = minWorkTimeSpan;
	}

	/**
	 * @return the maxWorkTimeSpan
	 */
	public long getMaxWorkTimeSpan() {
		return maxWorkTimeSpan;
	}

	/**
	 * @param maxWorkTimeSpan the maxWorkTimeSpan to set
	 */
	public void setMaxWorkTimeSpan(long maxWorkTimeSpan) {
		this.maxWorkTimeSpan = maxWorkTimeSpan;
	}

	/**
	 * @return the minSyncTimeSpan
	 */
	public long getMinSyncTimeSpan() {
		return minSyncTimeSpan;
	}

	/**
	 * @param minSyncTimeSpan the minSyncTimeSpan to set
	 */
	public void setMinSyncTimeSpan(long minSyncTimeSpan) {
		this.minSyncTimeSpan = minSyncTimeSpan;
	}

	/**
	 * @return the maxSyncTimeSpan
	 */
	public long getMaxSyncTimeSpan() {
		return maxSyncTimeSpan;
	}

	/**
	 * @param maxSyncTimeSpan the maxSyncTimeSpan to set
	 */
	public void setMaxSyncTimeSpan(long maxSyncTimeSpan) {
		this.maxSyncTimeSpan = maxSyncTimeSpan;
	}

	/**
	 * @return the minWaitTimeSpan
	 */
	public long getMinWaitTimeSpan() {
		return minWaitTimeSpan;
	}

	/**
	 * @param minWaitTimeSpan the minWaitTimeSpan to set
	 */
	public void setMinWaitTimeSpan(long minWaitTimeSpan) {
		this.minWaitTimeSpan = minWaitTimeSpan;
	}

	/**
	 * @return the maxWaitTimeSpan
	 */
	public long getMaxWaitTimeSpan() {
		return maxWaitTimeSpan;
	}

	/**
	 * @param maxWaitTimeSpan the maxWaitTimeSpan to set
	 */
	public void setMaxWaitTimeSpan(long maxWaitTimeSpan) {
		this.maxWaitTimeSpan = maxWaitTimeSpan;
	}

	/**
	 * @return the sumWorkTimeSpan
	 */
	public long getSumWorkTimeSpan() {
		return sumWorkTimeSpan;
	}

	/**
	 * @param sumWorkTimeSpan
	 *            the sumWorkTimeSpan to set
	 */
	public void setSumWorkTimeSpan(long sumWorkTimeSpan) {
		this.sumWorkTimeSpan = sumWorkTimeSpan;
	}

	/**
	 * @return the sumSyncTimeSpan
	 */
	public long getSumSyncTimeSpan() {
		return sumSyncTimeSpan;
	}

	/**
	 * @param sumSyncTimeSpan
	 *            the sumSyncTimeSpan to set
	 */
	public void setSumSyncTimeSpan(long sumSyncTimeSpan) {
		this.sumSyncTimeSpan = sumSyncTimeSpan;
	}

	/**
	 * @return the sumWaitTimeSpan
	 */
	public long getSumWaitTimeSpan() {
		return sumWaitTimeSpan;
	}

	/**
	 * @param sumWaitTimeSpan
	 *            the sumWaitTimeSpan to set
	 */
	public void setSumWaitTimeSpan(long sumWaitTimeSpan) {
		this.sumWaitTimeSpan = sumWaitTimeSpan;
	}

	public void addAllPerfMeasures(long workTime, long waitingTime, long syncTime) {
		addWorkTime(workTime);
		addWaitTime(waitingTime);
		addSyncTime(syncTime);
		incFrequency();
		
		// calculate for deviation 
		if (Double.isNaN(mWork)){
			mWork = workTime;
			sWork = 0;
		} else {
			// not the first value
			double oldMWorkTime = mWork;
			mWork += ((workTime - mWork)/getFrequency());
			sWork += ((workTime - oldMWorkTime) * (workTime - mWork));
		}
		
		if (Double.isNaN(mWait)){
			mWait = waitingTime;
			sWait = 0;
		} else {
			// not the first value
			double oldMWaitingTime = mWait;
			mWait += ((waitingTime - mWait)/getFrequency());
			sWait += ((waitingTime - oldMWaitingTime) * (waitingTime - mWait));
		}
		
		if (Double.isNaN(mSync)){
			mSync = syncTime;
			sSync = 0;
		} else {
			// not the first value
			double oldMSyncTime = mSync;
			mSync += ((syncTime - mSync)/getFrequency());
			sSync += ((syncTime - oldMSyncTime) * (syncTime - mSync));
		}
	}
	
	public double getStdDevWorkTime(){
		 return Math.sqrt( sWork / ( getFrequency() - 1 ) );
	}
	
	public double getStdDevWaitTime(){
		 return Math.sqrt( sWait / ( getFrequency() - 1 ) );
	}
	
	public double getStdDevSyncTime(){
		 return Math.sqrt( sSync / ( getFrequency() - 1 ) );
	}

	public void addIncompleteOccurrence(int numOccurrence) {
		incompleteFrequency += numOccurrence;
	}
}
