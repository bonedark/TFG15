/**
 * 
 */
package org.processmining.models.connections.flexiblemodel.conversion;

import org.processmining.framework.connections.impl.AbstractConnection;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.models.flexiblemodel.StartTaskNodesSet;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;

/**
 * @author arya
 *
 */
public class PNOfFlexModelConnection extends AbstractConnection {
	public final static String CAUSALNET = "CausalNet";
	public final static String STARTTASKNODES = "StartTaskNodes";
	public final static String PETRINET = "PetriNet";
	public final static String MARKING = "InitialMarking";	
		

	public PNOfFlexModelConnection(String label, Flex causalNet, StartTaskNodesSet startTaskNodesSet, Petrinet net, Marking marking) {
		super(label);
		put(CAUSALNET, causalNet);
		put(STARTTASKNODES, startTaskNodesSet);
		put(PETRINET, net);
		put(MARKING, marking);
	}
}