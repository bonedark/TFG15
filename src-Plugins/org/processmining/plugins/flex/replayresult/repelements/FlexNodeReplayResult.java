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
public class FlexNodeReplayResult extends AbstractElementSetResult {
	private int missingNodeFrequence = 0;
	
	private Map<Set<Short>, InputNodeSetResult> inputSetResult;
	private Map<Set<Short>, OutputNodeSetResult> outputSetResult;

	public FlexNodeReplayResult(Set<Set<Short>> setInputSets, Set<Set<Short>> setOutputSets) {
		inputSetResult = new HashMap<Set<Short>, InputNodeSetResult>();
		for (Set<Short> setInput : setInputSets) {
			InputNodeSetResult newInputNodeSetResult = new InputNodeSetResult(setInput);
			inputSetResult.put(setInput, newInputNodeSetResult);
		}

		outputSetResult = new HashMap<Set<Short>, OutputNodeSetResult>();
		for (Set<Short> setOutput : setOutputSets) {
			OutputNodeSetResult newOutputNodeSetResult = new OutputNodeSetResult();
			outputSetResult.put(setOutput, newOutputNodeSetResult);
		}
	}

	/**
	 * @return the inputSetResult
	 */
	public Map<Set<Short>, InputNodeSetResult> getInputSetResult() {
		return inputSetResult;
	}

	/**
	 * @param inputSetResult
	 *            the inputSetResult to set
	 */
	public void setInputSetResult(Map<Set<Short>, InputNodeSetResult> inputSetResult) {
		this.inputSetResult = inputSetResult;
	}

	/**
	 * @return the outputSetResult
	 */
	public Map<Set<Short>, OutputNodeSetResult> getOutputSetResult() {
		return outputSetResult;
	}

	/**
	 * @param outputSetResult
	 *            the outputSetResult to set
	 */
	public void setOutputSetResult(Map<Set<Short>, OutputNodeSetResult> outputSetResult) {
		this.outputSetResult = outputSetResult;
	}

	public void incFrequency(Set<Short> inputSet, Set<Short> missingNodes, Set<Short> outputSet) {
		incFrequency();
		if (inputSet != null) {
			inputSetResult.get(inputSet).incFrequency();
		}
		if (missingNodes.size() > 0) {
			missingNodeFrequence++;
			inputSetResult.get(inputSet).appendUnsatisfiedEvents(missingNodes);
		}
		if (outputSet != null) {
			outputSetResult.get(outputSet).incFrequency();
		}
	}

	public void incNumFitCaseInvolved() {
		incNumFitCaseInvolved();
		for (Set<Short> key : inputSetResult.keySet()) {
			inputSetResult.get(key).incNumFitCaseInvolved();
		}
		for (Set<Short> key : outputSetResult.keySet()) {
			outputSetResult.get(key).incNumFitCaseInvolved();
		}
	}

	public void incNumUnfitCaseInvolved() {
		incNumUnfitCaseInvolved();
		for (Set<Short> key : inputSetResult.keySet()) {
			inputSetResult.get(key).incNumUnfitCaseInvolved();
		}
		for (Set<Short> key : outputSetResult.keySet()) {
			outputSetResult.get(key).incNumUnfitCaseInvolved();
		}
	}
}
