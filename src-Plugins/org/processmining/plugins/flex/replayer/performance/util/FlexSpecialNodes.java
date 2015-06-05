/**
 * 
 */
package org.processmining.plugins.flex.replayer.performance.util;

import java.util.HashSet;
import java.util.Set;

import org.processmining.models.flexiblemodel.FlexNode;

/**
 * @author aadrians
 * 
 */
public class FlexSpecialNodes {

	// set of places which are binded with timestamp of successor node 
	Set<FlexNode> setLateBindingNodes = new HashSet<FlexNode>();

	// set of multiple composite tasks
	Set<FlexNode> multipleCompositeTasks = new HashSet<FlexNode>();
	
	// set of composite tasks
	Set<FlexNode> compositeTasks = new HashSet<FlexNode>();
	
	// set of multiple atomic tasks
	Set<FlexNode> multipleAtomicTasks= new HashSet<FlexNode>();

	public FlexSpecialNodes(){}
	
	public void addLateBindingNodes(FlexNode newNode){
		setLateBindingNodes.add(newNode);
	}
	
	public void addMultipleCompositeTasks(FlexNode newNode){
		multipleCompositeTasks.add(newNode);
	}
	
	public void addCompositeTasks(FlexNode newNode){
		compositeTasks.add(newNode);
	}
	
	public void addMultipleAtomicTasks(FlexNode newNode){
		multipleAtomicTasks.add(newNode);
	}
	
	/**
	 * @return the setLateBindingNodes
	 */
	public Set<FlexNode> getSetLateBindingNodes() {
		return setLateBindingNodes;
	}

	/**
	 * @param setLateBindingNodes the setLateBindingNodes to set
	 */
	public void setSetLateBindingNodes(Set<FlexNode> setLateBindingNodes) {
		this.setLateBindingNodes = setLateBindingNodes;
	}

	/**
	 * @return the multipleCompositeTasks
	 */
	public Set<FlexNode> getMultipleCompositeTasks() {
		return multipleCompositeTasks;
	}

	/**
	 * @param multipleCompositeTasks the multipleCompositeTasks to set
	 */
	public void setMultipleCompositeTasks(Set<FlexNode> multipleCompositeTasks) {
		this.multipleCompositeTasks = multipleCompositeTasks;
	}

	/**
	 * @return the compositeTasks
	 */
	public Set<FlexNode> getCompositeTasks() {
		return compositeTasks;
	}

	/**
	 * @param compositeTasks the compositeTasks to set
	 */
	public void setCompositeTasks(Set<FlexNode> compositeTasks) {
		this.compositeTasks = compositeTasks;
	}

	/**
	 * @return the multipleAtomicTasks
	 */
	public Set<FlexNode> getMultipleAtomicTasks() {
		return multipleAtomicTasks;
	}

	/**
	 * @param multipleAtomicTasks the multipleAtomicTasks to set
	 */
	public void setMultipleAtomicTasks(Set<FlexNode> multipleAtomicTasks) {
		this.multipleAtomicTasks = multipleAtomicTasks;
	}

	/**
	 * true if teh node is an multiple atomic task
	 * @param node
	 * @return
	 */
	public boolean containsMultipleAtomicTasks(FlexNode node) {
		return multipleAtomicTasks.contains(node);
	}
	
	
}
