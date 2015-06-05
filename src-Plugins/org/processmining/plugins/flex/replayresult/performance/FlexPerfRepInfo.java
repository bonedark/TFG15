/**
 * 
 */
package org.processmining.plugins.flex.replayresult.performance;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.processmining.framework.util.Pair;
import org.processmining.models.flexiblemodel.FlexNode;

/**
 * @author aadrians
 * 
 */
public class FlexPerfRepInfo {
	// short represent a node of Flexible model
	private Map<Short, Set<FlexBindingInstance>> nodeInstancesMap = new HashMap<Short, Set<FlexBindingInstance>>();
	private Map<Pair<Short, Short>, EdgeInstance> edgeInstancesMap = new HashMap<Pair<Short, Short>, EdgeInstance>();
	private Map<Short, Integer> nodeCaseInvolvement = new HashMap<Short, Integer>();
	private Map<Short, Integer> unfinishedFrequency = new HashMap<Short, Integer>(); // nodes that are executed, but not finished
	private Map<Short, Integer> cancelationFrequency = new HashMap<Short, Integer>(); // the number of times a node is canceled
	private CaseInstance caseInstance = null;
	private Map<Pair<FlexNode, FlexNode>, Integer> canceledArcFrequency;
	private SortedSet<Integer> unreliableTraceIndexes = new TreeSet<Integer>();
	
	/**
	 * @return the unreliableTraceIndexes
	 */
	public SortedSet<Integer> getUnreliableTraceIndexes() {
		return unreliableTraceIndexes;
	}

	/**
	 * @param unreliableTraceIndexes the unreliableTraceIndexes to set
	 */
	public void setUnreliableTraceIndexes(SortedSet<Integer> unreliableTraceIndexes) {
		this.unreliableTraceIndexes = unreliableTraceIndexes;
	}

	/**
	 * @return the unfinishedFrequency
	 */
	public Map<Short, Integer> getUnfinishedFrequency() {
		return unfinishedFrequency;
	}

	/**
	 * @return the nodeInstancesMap
	 */
	public Map<Short, Set<FlexBindingInstance>> getNodeInstancesMap() {
		return nodeInstancesMap;
	}

	/**
	 * @param nodeInstancesMap
	 *            the nodeInstancesMap to set
	 */
	public void setNodeInstancesMap(Map<Short, Set<FlexBindingInstance>> nodeInstancesMap) {
		this.nodeInstancesMap = nodeInstancesMap;
	}

	/**
	 * @return the edgeInstancesMap
	 */
	public Map<Pair<Short, Short>, EdgeInstance> getEdgeInstancesMap() {
		return edgeInstancesMap;
	}

	/**
	 * @param edgeInstancesMap
	 *            the edgeInstancesMap to set
	 */
	public void setEdgeInstancesMap(Map<Pair<Short, Short>, EdgeInstance> edgeInstancesMap) {
		this.edgeInstancesMap = edgeInstancesMap;
	}

	/**
	 * @return the nodeCaseInvolvement
	 */
	public Map<Short, Integer> getNodeCaseInvolvement() {
		return nodeCaseInvolvement;
	}

	/**
	 * @param nodeCaseInvolvement the nodeCaseInvolvement to set
	 */
	public void setNodeCaseInvolvement(Map<Short, Integer> nodeCaseInvolvement) {
		this.nodeCaseInvolvement = nodeCaseInvolvement;
	}

	/**
	 * @return the accCaseInstance
	 */
	public CaseInstance getCaseInstance() {
		return caseInstance;
	}

	/**
	 * @param accCaseInstance the accCaseInstance to set
	 */
	public void setCaseInstance(CaseInstance caseInstance) {
		this.caseInstance = caseInstance;
	}

	/**
	 * @param convertToShortMapping
	 */
	public void setUnfinishedFrequency(Map<Short, Integer> convertToShortMapping) {
		this.unfinishedFrequency  = convertToShortMapping;
	}

	/**
	 * @return the cancelationFrequency
	 */
	public Map<Short, Integer> getCancelationFrequency() {
		return cancelationFrequency;
	}

	/**
	 * @param cancelationFrequency the cancelationFrequency to set
	 */
	public void setCancelationFrequency(Map<Short, Integer> cancelationFrequency) {
		this.cancelationFrequency = cancelationFrequency;
	}

	/**
	 * @param node
	 * @return
	 */
	public int getCancellationFrequency(Short node){
		Integer res = cancelationFrequency.get(node);
		return res == null ? 0 : res;
	}
	
	/**
	 * @param canceledArcFrequency
	 */
	public void setArcCancelationFrequency(Map<Pair<FlexNode, FlexNode>, Integer> canceledArcFrequency) {
		this.canceledArcFrequency = canceledArcFrequency;
	}
	
	/**
	 * @param nodePair
	 * @return
	 */
	public int getArcCancelationFrequency(Pair<FlexNode, FlexNode> nodePair){
		Integer res = canceledArcFrequency.get(nodePair);
		return res == null ? 0 : res;
	}
	

}
