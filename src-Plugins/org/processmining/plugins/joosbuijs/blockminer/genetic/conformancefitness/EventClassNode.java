// =============================================================================
// Copyright 2006-2010 Daniel W. Dyer
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// =============================================================================
package org.processmining.plugins.joosbuijs.blockminer.genetic.conformancefitness;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.deckfour.xes.classification.XEventClass;
import org.processmining.plugins.joosbuijs.blockminer.genetic.conformancefitness.FunctionNode.FUNCTIONTYPE;

/**
 * The tree (leaf) nodes that represent an event class of the event log
 * 
 * @author jbuijs
 */
public class EventClassNode implements Node {
	//The event class this node represents
	private XEventClass eventClass;
	private FunctionNode parent;

	/**
	 * Instantiate a new instance
	 * 
	 * @param eventClass
	 *            The event class this node represents
	 */
	public EventClassNode(XEventClass eventClass) {
		this.eventClass = eventClass;
	}
	
	public EventClassNode(EventClassNode ecnode){
		this.eventClass = ecnode.getEventClass();
	}

	/**
	 * 
	 * @return The event class this node represents
	 */
	public XEventClass getEventClass() {
		return eventClass;
	}
	
	/**
	 * Sets a new event class for this node
	 * @param eventClass
	 */
	public void setEventClass(XEventClass eventClass){
		this.eventClass = eventClass;
	}

	@Override
	public String toString() {
		return eventClass.toString();
	}

	public String toString(int level) {
		StringBuilder buffer = new StringBuilder("");
		for (int i = 0; i < level; i++)
			buffer.append("\t");
		//System.getProperty("line.separator")
		buffer.append(eventClass.toString());
		buffer.append(System.getProperty("line.separator"));
		return buffer.toString();
	}

	public String getLabel() {
		return eventClass.toString();
	}

	/**
	 * The arity of a non-function node is always zero.
	 * 
	 * @return 0
	 */
	public int getArity() {
		return 0;
	}

	/**
	 * Leaf nodes always have a depth of 1 since they have no child nodes.
	 * 
	 * @return 1
	 */
	public int getDepth() {
		return 1;
	}

	/**
	 * Leaf nodes always have a width of 1 since they have no child nodes.
	 * 
	 * @return 1
	 */
	/*-* /
	public int getWidth() {
		return 1;
	}/**/

	/**
	 * {@inheritDoc}
	 */
	public int countNodes() {
		return 1;
	}

	/**
	 * {@inheritDoc}
	 */
	/*-* /
	public int countLeafs() {
		return 1;
	}/**/

	/**
	 * {@inheritDoc}
	 */
	public Node getNode(int index) {
		if (index != 0) {
			throw new IndexOutOfBoundsException("Invalid node index: " + index);
		}
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Node getChild(int index) {
		throw new IndexOutOfBoundsException("Leaf nodes have no children.");
	}

	public FUNCTIONTYPE getFunction() {
		return null;
	}

	/**
	 * Returns a list of the event classes contained below this node
	 * 
	 * @return
	 */
	public List<XEventClass> getEventClasses() {
		List<XEventClass> list = new ArrayList<XEventClass>();
		list.add(this.getEventClass());
		return list;
	}

	public List<EventClassNode> getLeafs() {
		List<EventClassNode> list = new ArrayList<EventClassNode>();
		list.add(this);
		return list;
}

	public void setParent(FunctionNode parent) {
		this.parent = parent;
	}

	public FunctionNode getParent() {
		return parent;
	}

	public Node deepClone() {
		return new EventClassNode(eventClass);
		//We don't deep clone event class nodes, they should not be changed
		//return this;
	}

	public List<Node> getPostorder() {
		LinkedList<Node> postorder = new LinkedList<Node>();
		postorder.add(this);
		return postorder;
	}
	
	public List<EventClassNode> getLeafsOfEventClass(XEventClass eventClass){
		LinkedList<EventClassNode> list = new LinkedList<EventClassNode>();
		if(this.eventClass.equals(eventClass)){
			list.add(this);
		}
		return list;
	}
	
	public boolean isLeaf(){
		return true;
	}

	public int getIndexOf(Node node) {
		if(node.equals(this)) return 0;
		else return -1;
	}
	
	public int countChildren(){
		return 0;
	}
}
