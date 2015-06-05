/**
 * 
 */
package org.processmining.models.instancetree.petrinet.pncostbasedeff;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import javax.swing.SwingConstants;

import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.directed.AbstractDirectedGraph;
import org.processmining.models.graphbased.directed.DirectedGraph;
import org.processmining.models.graphbased.directed.DirectedGraphEdge;
import org.processmining.models.graphbased.directed.DirectedGraphElement;
import org.processmining.models.graphbased.directed.DirectedGraphNode;
import org.processmining.models.instancetree.AbstractIT;

/**
 * @author aadrians
 *
 */
public class PNCostBasedTreeEffImpl extends AbstractIT<PNCostBasedTreeNodeEff, PNCostBasedTreeEdgeEff<PNCostBasedTreeNodeEff,PNCostBasedTreeNodeEff>> implements PNCostBasedTreeEff {

	public PNCostBasedTreeEffImpl(String label) {
		super();
		getAttributeMap().put(AttributeMap.LABEL, label);
		getAttributeMap().put(AttributeMap.PREF_ORIENTATION, SwingConstants.NORTH);
		
		nodes = new LinkedHashSet<PNCostBasedTreeNodeEff>();
		arcs = new LinkedHashSet<PNCostBasedTreeEdgeEff<PNCostBasedTreeNodeEff,PNCostBasedTreeNodeEff>>();
	}
	
	@Override
	public PNCostBasedTreeNodeEff addNode(String label) {
		PNCostBasedTreeNodeEff node = new PNCostBasedTreeNodeEff(this, label);
		nodes.add(node);
		graphElementAdded(node);
		return node;
	}

	@Override
	public PNCostBasedTreeEdgeEff<PNCostBasedTreeNodeEff, PNCostBasedTreeNodeEff> addArc(PNCostBasedTreeNodeEff source,
			PNCostBasedTreeNodeEff target) {
		PNCostBasedTreeEdgeEff<PNCostBasedTreeNodeEff, PNCostBasedTreeNodeEff> edge = new PNCostBasedTreeEdgeEff<PNCostBasedTreeNodeEff, PNCostBasedTreeNodeEff>(source, target);
		arcs.add(edge);
		graphElementAdded(edge);
		return edge;
	}

	@Override
	public PNCostBasedTreeEdgeEff<PNCostBasedTreeNodeEff, PNCostBasedTreeNodeEff> removeArc(
			PNCostBasedTreeNodeEff source, PNCostBasedTreeNodeEff target) {
		return removeFromEdges(source, target, arcs);
	}

	@Override
	public void removeNode(DirectedGraphNode cell) {
		if (cell instanceof PNCostBasedTreeNodeEff){
			removeSurroundingEdges((PNCostBasedTreeNodeEff) cell);
			removeNodeFromCollection(nodes, (PNCostBasedTreeNodeEff) cell);
		}
	}

	@Override
	protected AbstractDirectedGraph<PNCostBasedTreeNodeEff, PNCostBasedTreeEdgeEff<PNCostBasedTreeNodeEff, PNCostBasedTreeNodeEff>> getEmptyClone() {
		return new PNCostBasedTreeEffImpl(this.getGraph().getLabel());
	}

	@Override
	protected Map<? extends DirectedGraphElement, ? extends DirectedGraphElement> cloneFrom(
			DirectedGraph<PNCostBasedTreeNodeEff, PNCostBasedTreeEdgeEff<PNCostBasedTreeNodeEff, PNCostBasedTreeNodeEff>> graph) {
		HashMap<DirectedGraphElement, DirectedGraphElement> mapping = new HashMap<DirectedGraphElement, DirectedGraphElement>();

		for (PNCostBasedTreeNodeEff a : graph.getNodes()) {
			mapping.put(a, addNode(a.getLabel()));
		}

		getAttributeMap().clear();
		AttributeMap map = graph.getAttributeMap();
		for (String key : map.keySet()) {
			getAttributeMap().put(key, map.get(key));
		}
		return mapping;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void removeEdge(DirectedGraphEdge edge) {
		if (edge instanceof PNCostBasedTreeEdgeEff<?,?>){
			arcs.remove(edge);
		} else {
			assert(false);
		}
		graphElementRemoved(edge);
		
	}

}
