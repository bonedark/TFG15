/**
 * 
 */
package org.processmining.plugins.flex.replayresult.repelements;

/**
 * @author Arya Adriansyah
 * @email a.adriansyah@tue.nl
 * @version Apr 23, 2010
 */
public class FlexEdgeReplayResult extends AbstractElementSetResult {
	private int takenFrequency = 0; // frequency of tokens taken from this edge

	/**
	 * @return the takenFrequency
	 */
	public int getTakenFrequency() {
		return takenFrequency;
	}

	/**
	 * @param takenFrequency
	 *            the takenFrequency to set
	 */
	public void setTakenFrequency(int takenFrequency) {
		this.takenFrequency = takenFrequency;
	}

	public void incTakenFrequency() {
		takenFrequency++;
	}
}
