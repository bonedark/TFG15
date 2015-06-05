/**
 * 
 */
package org.processmining.plugins.flex.replayer.util;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.processmining.framework.util.Pair;
import org.processmining.framework.util.collection.HashMultiSet;
import org.processmining.framework.util.collection.MultiSet;
import org.processmining.plugins.petrinet.replayer.util.statespaces.CPNCostBasedTreeNode;

/**
 * @author aadrians
 *
 */
public class CFlexExtendedCostBasedAStar implements Comparable<CFlexExtendedCostBasedAStar>{
	// helping variables
	private int currIndexOnTrace = 0; // index of the next event to be replayed
	
	// util variables
	private MultiSet<Pair<Short, Short>> currObligation;
	private List<Pair<Integer, Short>> duplicatesOnlyStep;
	private List<Pair<Integer, Short>> modelOnlyStep;
	private List<Pair<Integer, Short>> traceModelViolatingStep;
	private List<Integer> moveTraceOnlyStep;
	private Set<Short> startingTaskNodes;
	
	private int cost = 0; // cost
	
	public CFlexExtendedCostBasedAStar (){
		currObligation = new HashMultiSet<Pair<Short,Short>>();
		duplicatesOnlyStep = new LinkedList<Pair<Integer,Short>>();
		modelOnlyStep = new LinkedList<Pair<Integer,Short>>();
		traceModelViolatingStep = new LinkedList<Pair<Integer,Short>>();
		moveTraceOnlyStep = new LinkedList<Integer>();
		startingTaskNodes = new HashSet<Short>();
	}
	
	public CFlexExtendedCostBasedAStar (CFlexExtendedCostBasedAStar otherNode){
		currObligation = new HashMultiSet<Pair<Short, Short>>(otherNode.getCurrObligation());
		duplicatesOnlyStep = new LinkedList<Pair<Integer,Short>>(otherNode.getDuplicatesOnlyStep());
		modelOnlyStep = new LinkedList<Pair<Integer,Short>>(otherNode.getModelOnlyStep());
		traceModelViolatingStep = new LinkedList<Pair<Integer,Short>>(otherNode.getTraceModelViolatingStep());
		moveTraceOnlyStep = new LinkedList<Integer>(otherNode.getMoveTraceOnlyStep());
		cost = otherNode.getCost();
		currIndexOnTrace = otherNode.getCurrIndexOnTrace();
		startingTaskNodes = new HashSet<Short>(otherNode.getStartingTaskNodes());
	}
	
	public boolean startTaskNodesContains(Short encFlexNode){
		return startingTaskNodes.contains(encFlexNode);
	}
	
	/**
	 * @return the startingTaskNodes
	 */
	public Set<Short> getStartingTaskNodes() {
		return startingTaskNodes;
	}

	/**
	 * @param startingTaskNodes the startingTaskNodes to set
	 */
	public void setStartingTaskNodes(Set<Short> startingTaskNodes) {
		this.startingTaskNodes = startingTaskNodes;
	}

	/**
	 * @return the currIndexOnTrace
	 */
	public int getCurrIndexOnTrace() {
		return currIndexOnTrace;
	}

	/**
	 * @param currIndexOnTrace the currIndexOnTrace to set
	 */
	public void setCurrIndexOnTrace(int currIndexOnTrace) {
		this.currIndexOnTrace = currIndexOnTrace;
	}

	/**
	 * @return the currObligation
	 */
	public MultiSet<Pair<Short, Short>> getCurrObligation() {
		return currObligation;
	}

	/**
	 * @param currObligation the currObligation to set
	 */
	public void setCurrObligation(MultiSet<Pair<Short, Short>> currObligation) {
		this.currObligation = currObligation;
	}
	
	public boolean addCurrObligation(Pair<Short, Short> newObligation){
		return this.currObligation.add(newObligation);
	}
	
	public boolean removeCurrObligation(Pair<Short, Short> obligationRemove){
		return this.currObligation.remove(obligationRemove);
	}
	
	public void removeAllOfThisObligation(Pair<Short, Short> obligationRemove){
		while (currObligation.contains(obligationRemove)){
			currObligation.remove(obligationRemove);
		}
	}

	/**
	 * @return the duplicatesOnlyStep
	 */
	public List<Pair<Integer, Short>> getDuplicatesOnlyStep() {
		return duplicatesOnlyStep;
	}

	/**
	 * @param duplicatesOnlyStep the duplicatesOnlyStep to set
	 */
	public void setDuplicatesOnlyStep(List<Pair<Integer, Short>> duplicatesOnlyStep) {
		this.duplicatesOnlyStep = duplicatesOnlyStep;
	}

	/**
	 * @return the modelOnlyStep
	 */
	public List<Pair<Integer, Short>> getModelOnlyStep() {
		return modelOnlyStep;
	}

	/**
	 * @param modelOnlyStep the modelOnlyStep to set
	 */
	public void setModelOnlyStep(List<Pair<Integer, Short>> modelOnlyStep) {
		this.modelOnlyStep = modelOnlyStep;
	}

	/**
	 * @return the traceModelViolatingStep
	 */
	public List<Pair<Integer, Short>> getTraceModelViolatingStep() {
		return traceModelViolatingStep;
	}

	/**
	 * @param traceModelViolatingStep the traceModelViolatingStep to set
	 */
	public void setTraceModelViolatingStep(List<Pair<Integer, Short>> traceModelViolatingStep) {
		this.traceModelViolatingStep = traceModelViolatingStep;
	}

	/**
	 * @return the moveTraceOnlyStep
	 */
	public List<Integer> getMoveTraceOnlyStep() {
		return moveTraceOnlyStep;
	}

	/**
	 * @param moveTraceOnlyStep the moveTraceOnlyStep to set
	 */
	public void setMoveTraceOnlyStep(List<Integer> moveTraceOnlyStep) {
		this.moveTraceOnlyStep = moveTraceOnlyStep;
	}

	/**
	 * @return the cost
	 */
	public int getCost() {
		return cost;
	}

	/**
	 * @param cost the cost to set
	 */
	public void setCost(int cost) {
		this.cost = cost;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof CPNCostBasedTreeNode){
			CFlexExtendedCostBasedAStar nodeX = (CFlexExtendedCostBasedAStar) o;
			int nodeXCost = nodeX.getCost();
			if (getCost() != nodeXCost){
				return false;
			} else {
				if (currIndexOnTrace != nodeX.getCurrIndexOnTrace()){
					return false;
				} else {
					// assume that obligation can never be null
					MultiSet<Pair<Short, Short>> obligations = nodeX.getCurrObligation();
					if (currObligation.size() == obligations.size()){
						for (Pair<Short, Short> pair : obligations){
							if (currObligation.occurrences(pair) != obligations.occurrences(pair)){
								return false;
							}
						}
						return true;
					} 
					return false;
				}
			}
		} else {
			return false;
		}
	}
	
	@Override
	public int compareTo(CFlexExtendedCostBasedAStar nodeX) {
		// instances are sorted based on cost and marking size.  
		int nodeXCost = nodeX.getCost();
		if (getCost() < nodeXCost){
			return -1;
		} else if (getCost() == nodeXCost){
			int currIndexOnTrace = getCurrIndexOnTrace(); 
			int nodeXcurrIndexOnTrace = nodeX.getCurrIndexOnTrace();
			if (currIndexOnTrace > nodeXcurrIndexOnTrace){
				return -1;
			} else if (currIndexOnTrace == nodeXcurrIndexOnTrace){
				int currMarkingSize = currObligation.size();
				int nodeXcurrMarkingSize = nodeX.getCurrObligation().size();
				if (currMarkingSize < nodeXcurrMarkingSize){ // less marking
					return -1;
				} else if (currMarkingSize > nodeXcurrMarkingSize){
					return 1;
				} else {
					if (currObligation.equals(nodeX.getCurrObligation())){ 
						return 0;
					} else {
						return -1;
					}
				}
			} else {
				return 1;
			}
		} else {
			return 1;
		}
	}

	public void addExecutedStartTaskNodes(Short candidate) {
		this.startingTaskNodes.add(candidate);
	}
}
