/**
 * 
 */
package org.processmining.plugins.flex.replayresult.repelements;

/**
 * @author Arya Adriansyah
 * @email a.adriansyah@tue.nl
 * @version Apr 23, 2010
 */
public abstract class AbstractElementSetResult {
	private int frequency = 0;
	private int numFitCaseInvolved = 0;
	private int numUnfitCaseInvolved = 0;

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
	 * @return the numFitCaseInvolved
	 */
	public int getNumFitCaseInvolved() {
		return numFitCaseInvolved;
	}

	/**
	 * @param numFitCaseInvolved
	 *            the numFitCaseInvolved to set
	 */
	public void setNumFitCaseInvolved(int numFitCaseInvolved) {
		this.numFitCaseInvolved = numFitCaseInvolved;
	}

	/**
	 * @return the numUnfitCaseInvolved
	 */
	public int getNumUnfitCaseInvolved() {
		return numUnfitCaseInvolved;
	}

	/**
	 * @param numUnfitCaseInvolved
	 *            the numUnfitCaseInvolved to set
	 */
	public void setNumUnfitCaseInvolved(int numUnfitCaseInvolved) {
		this.numUnfitCaseInvolved = numUnfitCaseInvolved;
	}

	public void incFrequency() {
		frequency++;
	}

	public void incNumUnfitCaseInvolved() {
		numUnfitCaseInvolved++;
	}

	public void incNumFitCaseInvolved() {
		numFitCaseInvolved++;
	}

}
