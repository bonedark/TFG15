/**
 * 
 */
package org.processmining.models.pnetprojection;

import java.awt.Graphics2D;

import org.processmining.models.graphbased.directed.AbstractDirectedGraph;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.ExpandableSubNet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.shapes.Decorated;

/**
 * @author aadrians Oct 26, 2011
 * 
 */
public class PlaceP extends Place implements Decorated {
	public PlaceP(String label,
			AbstractDirectedGraph<PetrinetNode, PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> net) {
		super(label, net);
	}

	public PlaceP(String label, PetrinetGraphP net, ExpandableSubNet parent) {
		super(label, net, parent);
	}

	protected IPlacePDecorator decorator;

	public void decorate(Graphics2D g2d, double x, double y, double width, double height) {
		if (decorator != null) {
			decorator.decorate(g2d, x, y, width, height);
		}
	}

}
