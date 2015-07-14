/**
 * 
 */
package org.processmining.models.pnetprojection;

import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetImpl;


/**
 * @author aadrians
 * Oct 26, 2011
 *
 */
public class PetrinetGraphP extends PetrinetImpl {
	public PetrinetGraphP(String label) {
		super(label);
	}
	
	public synchronized TransitionP addTransition(String label) {
		TransitionP t = new TransitionP(label, this);
		transitions.add(t);
		graphElementAdded(t);
		return t;
	}
	
	public synchronized PlaceP addPlace(String label) {
		PlaceP p = new PlaceP(label, this);
		places.add(p);
		graphElementAdded(p);
		return p;
	}
	
	
}
