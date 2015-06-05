/**
 * 
 */
package org.processmining.models.connections.flexiblemodel;

import org.processmining.framework.connections.impl.AbstractConnection;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.models.flexiblemodel.animation.GraphAnimation;

/**
 * Connection between flex and its animation
 * 
 * @author arya
 * @email arya.adriansyah@gmail.com
 * @version Nov 20, 2009
 */
public class FlexGraphAnimationConnection extends AbstractConnection {
	public final static String FLEX = "FlexibleModel";
	public final static String GRAPHANIMATION = "GraphAnimation";

	public FlexGraphAnimationConnection(String label, Flex flex, GraphAnimation graphAnimation) {
		super(label);
		put(FLEX, flex);
		put(GRAPHANIMATION, graphAnimation);
	}

}
