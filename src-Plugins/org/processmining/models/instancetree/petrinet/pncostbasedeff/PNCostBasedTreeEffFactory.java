/**
 * 
 */
package org.processmining.models.instancetree.petrinet.pncostbasedeff;


/**
 * @author aadrians
 *
 */
public class PNCostBasedTreeEffFactory {
	private PNCostBasedTreeEffFactory(){}
	
	public static PNCostBasedTreeEff newPNCostBasedTreeEff(String label) {
		PNCostBasedTreeEffImpl res = new PNCostBasedTreeEffImpl(label);
		return res;
	}
}
