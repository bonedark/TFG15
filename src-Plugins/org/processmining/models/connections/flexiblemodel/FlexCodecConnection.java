/**
 * 
 */
package org.processmining.models.connections.flexiblemodel;

import org.processmining.framework.connections.impl.AbstractConnection;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.plugins.flex.replayer.util.FlexCodec;

/**
 * @author Arya Adriansyah
 * @email a.adriansyah@tue.nl
 * @version Mar 4, 2011
 */
public class FlexCodecConnection extends AbstractConnection {
	public final static String FLEX = "Flex";
	public final static String FLEXCODEC = "FlexCodec";

	public FlexCodecConnection(String label, Flex flex, FlexCodec codec) {
		super(label);
		put(FLEX, flex);
		put(FLEXCODEC, codec);
	}
}
