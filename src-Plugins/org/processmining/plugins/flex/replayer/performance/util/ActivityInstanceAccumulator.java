/**
 * 
 */
package org.processmining.plugins.flex.replayer.performance.util;

/**
 * @author aadrians
 * 
 */
public class ActivityInstanceAccumulator {
	private int frequency = 0;

	private long sumIdleTimeSpan = 0;
	private long minIdleTimeSpan = Long.MAX_VALUE;
	private long maxIdleTimeSpan = Long.MIN_VALUE;

	// required to calculate standard deviation
	private double avgIdle = Double.NaN;
	private double sumIdle = 0;
	
	private long sumWorkTimeSpan = 0;
	private long minWorkTimeSpan = Long.MAX_VALUE;
	private long maxWorkTimeSpan = Long.MIN_VALUE;

	// required to calculate standard deviation
	private double avgWork = Double.NaN;
	private double sumWork = 0;

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
	
	public void addIdleTime(long idleTime) {
		sumIdleTimeSpan += idleTime;
		if (idleTime < minIdleTimeSpan){
			minIdleTimeSpan = idleTime;
		}
		if (idleTime > maxIdleTimeSpan){
			maxIdleTimeSpan = idleTime;
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
	 * @return the sumIdleTimeSpan
	 */
	public long getSumIdleTimeSpan() {
		return sumIdleTimeSpan;
	}
	/**
	 * @param sumIdleTimeSpan the sumIdleTimeSpan to set
	 */
	public void setSumIdleTimeSpan(long sumIdleTimeSpan) {
		this.sumIdleTimeSpan = sumIdleTimeSpan;
	}
	/**
	 * @return the minIdleTimeSpan
	 */
	public long getMinIdleTimeSpan() {
		return minIdleTimeSpan;
	}
	/**
	 * @param minIdleTimeSpan the minIdleTimeSpan to set
	 */
	public void setMinIdleTimeSpan(long minIdleTimeSpan) {
		this.minIdleTimeSpan = minIdleTimeSpan;
	}
	/**
	 * @return the maxIdleTimeSpan
	 */
	public long getMaxIdleTimeSpan() {
		return maxIdleTimeSpan;
	}
	/**
	 * @param maxIdleTimeSpan the maxIdleTimeSpan to set
	 */
	public void setMaxIdleTimeSpan(long maxIdleTimeSpan) {
		this.maxIdleTimeSpan = maxIdleTimeSpan;
	}
	/**
	 * @return the sumWorkTimeSpan
	 */
	public long getSumWorkTimeSpan() {
		return sumWorkTimeSpan;
	}
	/**
	 * @param sumWorkTimeSpan the sumWorkTimeSpan to set
	 */
	public void setSumWorkTimeSpan(long sumWorkTimeSpan) {
		this.sumWorkTimeSpan = sumWorkTimeSpan;
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
	
	public void addAllPerfMeasures(long workTime, long idleTime) {
		addWorkTime(workTime);
		addIdleTime(idleTime);
		incFrequency();
		
		// calculate for deviation 
		if (Double.isNaN(avgWork)){
			avgWork = workTime;
		}
		double newavgWork = avgWork + ( workTime - avgWork ) / ( getFrequency() + 1 );
        sumWork += ( workTime - avgWork ) * ( workTime - newavgWork ) ;
        avgWork = newavgWork;
		
		if (Double.isNaN(avgIdle)){
			avgIdle = idleTime;
		}
		double newavgIdle = avgIdle + ( idleTime - avgIdle) / ( getFrequency() + 1 );
        sumIdle += ( idleTime - avgIdle ) * ( idleTime - newavgIdle ) ;
        avgIdle = newavgIdle;
		
	}
	
	public double getStdDevWorkTime(){
		 return Math.sqrt( sumWork / ( getFrequency() - 1 ) );
	}
	
	public double getStdDevIdleTime(){
		 return Math.sqrt( sumIdle / ( getFrequency() - 1 ) );
	}
}
