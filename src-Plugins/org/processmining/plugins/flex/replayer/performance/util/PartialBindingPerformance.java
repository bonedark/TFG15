/**
 * 
 */
package org.processmining.plugins.flex.replayer.performance.util;

/**
 * @author aadrians
 * 
 */
public class PartialBindingPerformance {

	private short encodedInputBinding; // what is the binded input IO in ORIGINAL MODEL
	private long startTime; // when the binding starts
	private long waitingTime;
	private long syncTime;

	public PartialBindingPerformance(short encodedInputBinding, long startTime, long syncTime, long waitingTime) {
		this.encodedInputBinding = encodedInputBinding;
		this.startTime = startTime;
		this.waitingTime = waitingTime;
		this.syncTime = syncTime;
	}

	/**
	 * @return the encodedInputBinding
	 */
	public Short getEncodedInputBinding() {
		return encodedInputBinding;
	}

	/**
	 * @param encodedInputBinding
	 *            the encodedInputBinding to set
	 */
	public void setEncodedInputBinding(short encodedInputBinding) {
		this.encodedInputBinding = encodedInputBinding;
	}

	/**
	 * @return the startTime
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * @param startTime
	 *            the startTime to set
	 */
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	/**
	 * @return the waitingTime
	 */
	public long getWaitingTime() {
		return waitingTime;
	}

	/**
	 * @param waitingTime
	 *            the waitingTime to set
	 */
	public void setWaitingTime(long waitingTime) {
		this.waitingTime = waitingTime;
	}

	/**
	 * @return the syncTime
	 */
	public long getSyncTime() {
		return syncTime;
	}

	/**
	 * @param syncTime
	 *            the syncTime to set
	 */
	public void setSyncTime(long syncTime) {
		this.syncTime = syncTime;
	}

}
