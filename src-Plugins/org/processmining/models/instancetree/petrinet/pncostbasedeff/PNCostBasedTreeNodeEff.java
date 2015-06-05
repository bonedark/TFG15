/**
 * 
 */
package org.processmining.models.instancetree.petrinet.pncostbasedeff;

import java.awt.Dimension;

import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.directed.AbstractDirectedGraph;
import org.processmining.models.graphbased.directed.DirectedGraphNode;
import org.processmining.models.instancetree.ITEdge;
import org.processmining.models.instancetree.ITNode;

/**
 * @author aadrians
 * 
 */
public class PNCostBasedTreeNodeEff extends ITNode {
	private int currIndexOnTrace;
	private int currMarking;
	private int currCost;

	public PNCostBasedTreeNodeEff(
			AbstractDirectedGraph<? extends ITNode, ? extends ITEdge<? extends ITNode, ? extends ITNode>> graph,
			String label) {
		super(graph, label);
		getAttributeMap().put(AttributeMap.SIZE, new Dimension(25, 25));
	}
	
	public void init(int currIndexOnTrace, int currMarking, int currCost){
		this.currIndexOnTrace = currIndexOnTrace;
		this.currCost = currCost;
		this.currMarking = currMarking;
	}
	
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append("CurrIndexOnTrace = ");
		sb.append(this.currIndexOnTrace);
		sb.append("; CurrCost = ");
		sb.append(this.currCost);
		sb.append("; CurrMarking = ");
		sb.append(this.currMarking);
		return sb.toString();
	}
	
	@Override
	public String getLabel() {
		String label = "<html><table border='0'>";
		label += "<tr><td>ID</td><td>Idx.</td><td>cost</td></tr>";
		label += "<tr><td>" + sequenceID + "</td><td>" + currIndexOnTrace + "</td><td>" + currCost + "</td></tr>";
		label += "</table></html>";
		return label;
	}

	@Override
	public int compareTo(DirectedGraphNode node) {
		if (node instanceof PNCostBasedTreeNodeEff){
			PNCostBasedTreeNodeEff nodeX = (PNCostBasedTreeNodeEff) node;
			if (getCurrCost() == nodeX.getCurrCost()){
				return getCurrIndexOnTrace() < nodeX.getCurrIndexOnTrace() ? 1 : -1;
			} else {
				return getCurrCost() < nodeX.getCurrCost() ? -1 : 1;
			}
		} else {
			return super.compareTo(node);
		}
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
	 * @return the currMarking
	 */
	public int getCurrMarking() {
		return currMarking;
	}


	/**
	 * @param currMarking the currMarking to set
	 */
	public void setCurrMarking(int currMarking) {
		this.currMarking = currMarking;
	}


	/**
	 * @return the currCost
	 */
	public int getCurrCost() {
		return currCost;
	}


	/**
	 * @param currCost the currCost to set
	 */
	public void setCurrCost(int currCost) {
		this.currCost = currCost;
	}
	
	
}
