/**
 * 
 */
package org.processmining.models.connections.flexiblemodel.conversion;

import org.processmining.framework.connections.impl.AbstractConnection;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.plugins.flex.replayer.performance.util.YAWLNodeInstanceMapping;
import org.yawlfoundation.yawl.editor.net.NetGraph;

/**
 * @author aadrians
 *
 */
public class FlexOfYAWLConnection extends AbstractConnection {
	public final static String YAWLMODEL = "YAWLModel";
	public final static String FLEX = "FlexibleModel";
	public final static String NODEINSTANCEMAPPING = "NodeInstanceMapping";

	public FlexOfYAWLConnection(String label, NetGraph yawlModel, Flex flex, YAWLNodeInstanceMapping nodeInstanceMapping) {
		super(label);
		put(YAWLMODEL, yawlModel);
		put(FLEX, flex);
		put(NODEINSTANCEMAPPING, nodeInstanceMapping);
	}
}
