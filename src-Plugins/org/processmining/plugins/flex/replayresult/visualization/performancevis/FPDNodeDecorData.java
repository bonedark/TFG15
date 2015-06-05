/**
 * 
 */
package org.processmining.plugins.flex.replayresult.visualization.performancevis;

import java.awt.Color;

/**
 * @author aadrians
 *
 */
public class FPDNodeDecorData {
	private String name;
	
	private int frequency = 0;
	private int unfinishedActivityFrequency = 0;
	
	private int andJoinFreq = 0;
	private int orJoinFreq = 0;
	private int xorJoinFreq = 0;
	
	private int andSplitFreq = 0;
	private int orSplitFreq = 0;
	private int xorSplitFreq = 0;
	
	private double caseInvolvementRatio = 0;
	
	private double avgWorkingTime = Double.NaN;
	
	private Color workingColor = Color.GREEN;
	private Color waitColor = Color.GREEN;
	private Color syncColor = Color.GREEN;
	
	private boolean cancelingOther = false;
	private int cancelledFrequency = 0;
	
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
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
	 * @return the andJoinFreq
	 */
	public int getAndJoinFreq() {
		return andJoinFreq;
	}
	/**
	 * @param andJoinFreq the andJoinFreq to set
	 */
	public void setAndJoinFreq(int andJoinFreq) {
		this.andJoinFreq = andJoinFreq;
	}
	/**
	 * @return the orJoinFreq
	 */
	public int getOrJoinFreq() {
		return orJoinFreq;
	}
	/**
	 * @param orJoinFreq the orJoinFreq to set
	 */
	public void setOrJoinFreq(int orJoinFreq) {
		this.orJoinFreq = orJoinFreq;
	}
	/**
	 * @return the xorJoinFreq
	 */
	public int getXorJoinFreq() {
		return xorJoinFreq;
	}
	/**
	 * @param xorJoinFreq the xorJoinFreq to set
	 */
	public void setXorJoinFreq(int xorJoinFreq) {
		this.xorJoinFreq = xorJoinFreq;
	}
	/**
	 * @return the andSplitFreq
	 */
	public int getAndSplitFreq() {
		return andSplitFreq;
	}
	/**
	 * @param andSplitFreq the andSplitFreq to set
	 */
	public void setAndSplitFreq(int andSplitFreq) {
		this.andSplitFreq = andSplitFreq;
	}
	/**
	 * @return the orSplitFreq
	 */
	public int getOrSplitFreq() {
		return orSplitFreq;
	}
	/**
	 * @param orSplitFreq the orSplitFreq to set
	 */
	public void setOrSplitFreq(int orSplitFreq) {
		this.orSplitFreq = orSplitFreq;
	}
	/**
	 * @return the xorSplitFreq
	 */
	public int getXorSplitFreq() {
		return xorSplitFreq;
	}
	/**
	 * @param xorSplitFreq the xorSplitFreq to set
	 */
	public void setXorSplitFreq(int xorSplitFreq) {
		this.xorSplitFreq = xorSplitFreq;
	}
	/**
	 * @return the avgWorkingTime
	 */
	public double getAvgWorkingTime() {
		return avgWorkingTime;
	}
	/**
	 * @param avgWorkingTime the avgWorkingTime to set
	 */
	public void setAvgWorkingTime(double avgWorkingTime) {
		this.avgWorkingTime = avgWorkingTime;
	}
	/**
	 * @return the workingColor
	 */
	public Color getWorkingColor() {
		return workingColor;
	}
	/**
	 * @param workingColor the workingColor to set
	 */
	public void setWorkingColor(Color workingColor) {
		this.workingColor = workingColor;
	}
	/**
	 * @return the waitColor
	 */
	public Color getWaitColor() {
		return waitColor;
	}
	/**
	 * @param waitColor the waitColor to set
	 */
	public void setWaitColor(Color waitColor) {
		this.waitColor = waitColor;
	}
	/**
	 * @return the syncColor
	 */
	public Color getSyncColor() {
		return syncColor;
	}
	/**
	 * @return the caseInvolvementRatio
	 */
	public double getCaseInvolvementRatio() {
		return caseInvolvementRatio;
	}
	/**
	 * @param caseInvolvementRatio the caseInvolvementRatio to set
	 */
	public void setCaseInvolvementRatio(double caseInvolvementRatio) {
		this.caseInvolvementRatio = caseInvolvementRatio;
	}
	/**
	 * @param syncColor the syncColor to set
	 */
	public void setSyncColor(Color syncColor) {
		this.syncColor = syncColor;
	}
	public void incFrequency() {
		this.frequency++;
	}
	public void incAndJoin() {
		this.andJoinFreq++;
	}
	public void incXorJoin() {
		this.xorJoinFreq++;
	}
	public void incOrJoin() {
		this.orJoinFreq++;
	}
	public void incAndSplit() {
		this.andSplitFreq++;
	}
	public void incXorSplit() {
		this.xorSplitFreq++;
	}
	public void incOrSplit() {
		this.orSplitFreq++;
	}
	public String getLabel() {
		return name;
	}
	/**
	 * @return the cancelingOther
	 */
	public boolean isCancelingOther() {
		return cancelingOther;
	}
	/**
	 * @param cancelingOther the cancelingOther to set
	 */
	public void setCancelingOther(boolean cancelingOther) {
		this.cancelingOther = cancelingOther;
	}
	/**
	 * @return the cancelledFrequency
	 */
	public int getCancelledFrequency() {
		return cancelledFrequency;
	}
	/**
	 * @param cancelledFrequency the cancelledFrequency to set
	 */
	public void setCancelledFrequency(int cancelledFrequency) {
		this.cancelledFrequency = cancelledFrequency;
	}
	/**
	 * @return the unfinishedActivityFrequency
	 */
	public int getUnfinishedActivityFrequency() {
		return unfinishedActivityFrequency;
	}
	/**
	 * @param unfinishedActivityFrequency the unfinishedActivityFrequency to set
	 */
	public void setUnfinishedActivityFrequency(int unfinishedActivityFrequency) {
		this.unfinishedActivityFrequency = unfinishedActivityFrequency;
	}
	
}
