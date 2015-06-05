/**
 * 
 */
package org.processmining.models.connections.flexiblemodel;

import org.processmining.framework.connections.impl.AbstractConnection;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.plugins.flex.replayer.performance.util.FlexSpecialNodes;

/**
 * @author aadrians
 *
 */
public class FlexSpecialNodesConnection extends AbstractConnection {
	public final static String FLEX = "CausalNet";
	public final static String FLEXSPECIALNODES = "SpecialNodes";

	public FlexSpecialNodesConnection(String label, Flex flex, FlexSpecialNodes specialNodes) {
		super(label);
		put(FLEX, flex);
		put(FLEXSPECIALNODES, specialNodes);
	}
}