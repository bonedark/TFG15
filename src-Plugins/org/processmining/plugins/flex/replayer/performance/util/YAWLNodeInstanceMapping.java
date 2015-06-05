/**
 * 
 */
package org.processmining.plugins.flex.replayer.performance.util;

import java.util.HashMap;
import java.util.Map;

import org.yawlfoundation.yawl.editor.elements.model.YAWLVertex;

/**
 * Mapping from encoded Flexible model node (in short datatype) to pair of
 * original node and lifecycle of the node represented by the flexible node
 * 
 * @author aadrians
 * 
 */
public class YAWLNodeInstanceMapping extends HashMap<Short, YAWLVertex> {
	private static final long serialVersionUID = -1326187668761698859L;

	// a vertex in a YAWL model can be mapped to multiple nodes in a Flexible model 
	Map<YAWLVertex, Short> inverseMap = new HashMap<YAWLVertex, Short>();

	@Override
	public YAWLVertex put(Short key, YAWLVertex value) {
		YAWLVertex res = super.put(key, value);
		inverseMap.put(value, key);
		return res;
	}

	public Short getYAWLVertexRepresentation(YAWLVertex vertex) {
		return inverseMap.get(vertex);
	}

	@Override
	public YAWLVertex remove(Object key) {
		YAWLVertex res = super.remove(key);
		// remove inverse
		inverseMap.remove(res);
		return res;
	}	
}
