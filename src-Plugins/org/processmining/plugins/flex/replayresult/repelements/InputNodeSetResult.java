/**
 * 
 */
package org.processmining.plugins.flex.replayresult.repelements;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Arya Adriansyah
 * @email a.adriansyah@tue.nl
 * @version Apr 21, 2010
 */
public class InputNodeSetResult extends AbstractElementSetResult {
	private Map<Short, Integer> unsatisfiedEventsFrequency;
	private int freqUnsatisfiedNodeSet = 0;

	public InputNodeSetResult(Set<Short> inputNodeSet) {
		unsatisfiedEventsFrequency = new HashMap<Short, Integer>();
		for (Short node : inputNodeSet) {
			unsatisfiedEventsFrequency.put(node, 0);
		}
	}

	/**
	 * @param unsatisfiedEvents
	 */
	public void appendUnsatisfiedEvents(Set<Short> unsatisfiedEvents) {
		freqUnsatisfiedNodeSet++;
		for (Short missingNode : unsatisfiedEvents) {
			unsatisfiedEventsFrequency.put(missingNode, unsatisfiedEventsFrequency.get(missingNode) + 1);
		}
	}

	/**
	 * @return the missingNodesFrequency
	 */
	public Map<Short, Integer> getUnsatisfiedEventsFrequency() {
		return unsatisfiedEventsFrequency;
	}

	/**
	 * @param unsatisfiedEventsFrequency
	 *            the missingNodesFrequency to set
	 */
	public void setUnsatisfiedEventsFrequency(Map<Short, Integer> unsatisfiedEventsFrequency) {
		this.unsatisfiedEventsFrequency = unsatisfiedEventsFrequency;
	}

	/**
	 * @return the freqMissingNodeSet
	 */
	public int getFreqUnsatisfiedNodeSet() {
		return freqUnsatisfiedNodeSet;
	}

	/**
	 * @param freqUnsatisfiedNodeSet
	 *            the freqMissingNodeSet to set
	 */
	public void setFreqUnsatisfiedNodeSet(int freqUnsatisfiedNodeSet) {
		this.freqUnsatisfiedNodeSet = freqUnsatisfiedNodeSet;
	}
}
