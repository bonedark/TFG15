/**
 * 
 */
package org.processmining.models.instancetree.petrinet.pncostbasedeff;

import org.processmining.models.instancetree.ITEdge;
import org.processmining.models.instancetree.ITNode;

/**
 * @author aadrians
 *
 */
public class PNCostBasedTreeEdgeEff<X extends ITNode, Y extends ITNode> extends ITEdge<X, Y> {
	private int selectedTransition;
	
	public void init(int selectedTransition) {
		this.selectedTransition = selectedTransition;
	}
	
	public PNCostBasedTreeEdgeEff(X source, Y target) {
		super(source, target);
	}

	@Override
	public String getLabel(){
		return String.valueOf(selectedTransition);
	}
	
	/**
	 * @return the selectedTransition
	 */
	public int getSelectedTransition() {
		return selectedTransition;
	}

	/**
	 * @param selectedTransition the selectedTransition to set
	 */
	public void setSelectedTransition(int selectedTransition) {
		this.selectedTransition = selectedTransition;
	}
}
