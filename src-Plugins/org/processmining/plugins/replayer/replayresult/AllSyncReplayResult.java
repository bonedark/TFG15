/**
 * 
 */
package org.processmining.plugins.replayer.replayresult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.processmining.framework.util.collection.AlphanumComparator;
import org.processmining.plugins.replayer.util.StepTypes;

/**
 * This class extend the SyncReplayResult to return all best matching sequences,
 * given a trace from a log and a process model
 * 
 * @author aadrians
 * 
 */
public class AllSyncReplayResult {
	private List<List<Object>> nodeInstanceLst = null;
	private List<List<StepTypes>> stepTypesLst = null;
	private SortedSet<Integer> traceIndex = new TreeSet<Integer>();
	private boolean isReliable = false;
	private Map<String, Double> info = new HashMap<String, Double>(2);

	@SuppressWarnings("unused")
	private AllSyncReplayResult() {
	}
		
	/**
	 * Deprecated: only exists due to ETConformance plugin
	 * @return the traceIndex
	 */
	public SortedSet<String> getCaseIDs() {
		SortedSet<String> res = new TreeSet<String>(new AlphanumComparator());
		for (Integer index : traceIndex){
			res.add(String.valueOf(index));
		}
		return res;
	}


	public AllSyncReplayResult(List<List<Object>> nodeInstanceLst, List<List<StepTypes>> stepTypesLst, int traceIndex,
			boolean isReliable) {
		this.nodeInstanceLst = nodeInstanceLst;
		this.stepTypesLst = stepTypesLst;
		this.traceIndex.add(traceIndex);
		this.isReliable = isReliable;
	}

	public void addNewCase(int traceIndex) {
		this.traceIndex.add(traceIndex);
	}

	public void addInfo(String property, Double value){
		info.put(property, value);
	}
	
	/**
	 * @return the info
	 */
	public Map<String, Double> getInfo() {
		return info;
	}

	/**
	 * @param info the info to set
	 */
	public void setInfo(Map<String, Double> info) {
		this.info = info;
	}

	/**
	 * @return the nodeInstanceLst
	 */
	public List<List<Object>> getNodeInstanceLst() {
		return nodeInstanceLst;
	}

	/**
	 * @param nodeInstanceLst
	 *            the nodeInstanceLst to set
	 */
	public void setNodeInstanceLst(List<List<Object>> nodeInstanceLst) {
		this.nodeInstanceLst = nodeInstanceLst;
	}

	/**
	 * @return the stepTypesLst
	 */
	public List<List<StepTypes>> getStepTypesLst() {
		return stepTypesLst;
	}

	/**
	 * @param stepTypesLst
	 *            the stepTypesLst to set
	 */
	public void setStepTypesLst(List<List<StepTypes>> stepTypesLst) {
		this.stepTypesLst = stepTypesLst;
	}

	/**
	 * @return the traceIndex
	 */
	public SortedSet<Integer> getTraceIndex() {
		return traceIndex;
	}

	/**
	 * @param traceIndex
	 *            the traceIndex to set
	 */
	public void setTraceIndex(SortedSet<Integer> traceIndex) {
		this.traceIndex = traceIndex;
	}

	/**
	 * @return the isReliable
	 */
	public boolean isReliable() {
		return isReliable;
	}

	/**
	 * @param isReliable
	 *            the isReliable to set
	 */
	public void setReliable(boolean isReliable) {
		this.isReliable = isReliable;
	}

}
