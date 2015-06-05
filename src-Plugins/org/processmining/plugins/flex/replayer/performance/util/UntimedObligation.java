/**
 * 
 */
package org.processmining.plugins.flex.replayer.performance.util;

import java.util.Set;

import org.processmining.framework.util.Pair;
import org.processmining.models.flexiblemodel.FlexNode;

/**
 * @author aadrians
 * 
 */
public class UntimedObligation {
	private Set<Pair<FlexNode, FlexNode>> setObligations;
	private Long startObligationTime;
	private FlexNode currNodeReplay;
	private Short outputNodeBindingIO;
	private Short inputNodeBindingIO;

	public UntimedObligation(Set<Pair<FlexNode, FlexNode>> pairSet, Long startTime, FlexNode currNodeReplay,
			Short inputBindingIO, Short outputBindingIO) {
		this.setObligations = pairSet;
		this.startObligationTime = startTime;
		this.currNodeReplay = currNodeReplay;
		this.inputNodeBindingIO = inputBindingIO;
		this.outputNodeBindingIO = outputBindingIO;
	}

	/**
	 * @return the currNodeReplay
	 */
	public FlexNode getCurrNodeReplay() {
		return currNodeReplay;
	}

	/**
	 * @param currNodeReplay
	 *            the currNodeReplay to set
	 */
	public void setCurrNodeReplay(FlexNode currNodeReplay) {
		this.currNodeReplay = currNodeReplay;
	}

	/**
	 * @return the setObligations
	 */
	public Set<Pair<FlexNode, FlexNode>> getSetObligations() {
		return setObligations;
	}

	/**
	 * @param setObligations
	 *            the setObligations to set
	 */
	public void setSetObligations(Set<Pair<FlexNode, FlexNode>> setObligations) {
		this.setObligations = setObligations;
	}

	/**
	 * @return the startObligationTime
	 */
	public Long getStartObligationTime() {
		return startObligationTime;
	}

	/**
	 * @param startObligationTime
	 *            the startObligationTime to set
	 */
	public void setStartObligationTime(Long startObligationTime) {
		this.startObligationTime = startObligationTime;
	}

	/**
	 * @return the outputNodeBindingIO
	 */
	public Short getOutputNodeBindingIO() {
		return outputNodeBindingIO;
	}

	/**
	 * @param outputNodeBindingIO
	 *            the outputNodeBindingIO to set
	 */
	public void setOutputNodeBindingIO(Short outputNodeBindingIO) {
		this.outputNodeBindingIO = outputNodeBindingIO;
	}

	/**
	 * @return the inputNodeBindingIO
	 */
	public Short getInputNodeBindingIO() {
		return inputNodeBindingIO;
	}

	/**
	 * @param inputNodeBindingIO
	 *            the inputNodeBindingIO to set
	 */
	public void setInputNodeBindingIO(Short inputNodeBindingIO) {
		this.inputNodeBindingIO = inputNodeBindingIO;
	}

}
