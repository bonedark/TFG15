/**
 * 
 */
package org.processmining.plugins.replayer.converter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetFactory;
import org.processmining.models.semantics.petrinet.Marking;


/**
 * @author Arya Adriansyah
 * @email a.adriansyah@tue.nl
 * @version Apr 8, 2010
 * 
 */
//@Plugin(name = "Transform PN to 1-Initially Marked Place PN", returnLabels = { "Petri net", "Marking" }, returnTypes = {
//		Petrinet.class, Marking.class }, parameterLabels = { "Net", "Marking" }, userAccessible = true)
public class PNModifier {
//	@PluginVariant(variantLabel = "Transform PN to 1-Initially Marked Place PN", requiredParameterLabels = { 0, 1 })
	public Object[] transformToInitMarkedPN(PluginContext context, Petrinet net, Marking marking) {
		// check connection between net and marking
		try {
			context.getConnectionManager().getFirstConnection(InitialMarkingConnection.class, context, net, marking);
			Object[] res = transformToInitMarkedPNPrivate(context, net, marking);
			if (res != null) {
				context.getFutureResult(0).setLabel("Transformed " + net.getLabel());
				context.getFutureResult(1).setLabel("Marking of transformed " + net.getLabel());
				return res;
			}
			return null;
		} catch (ConnectionCannotBeObtained e) {
			context.log("Unable to convert to Flexible model. No connection between the net and its marking");
			return null;
		}
	}

	//@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "A. Adriansyah", email = "a.adriansyah@tue.nl", uiLabel = UITopiaVariant.USEPLUGIN, pack="Replayer")
	@PluginVariant(variantLabel = "Transform PN to 1-Initially Marked Place PN", requiredParameterLabels = { 0 })
	public Object[] transformToInitMarkedPN(PluginContext context, Petrinet net) {
		// check connection between net and marking
		try {
			InitialMarkingConnection conn = context.getConnectionManager().getFirstConnection(
					InitialMarkingConnection.class, context, net);
			Marking marking = (Marking) conn.getObjectWithRole(InitialMarkingConnection.MARKING);
			Object[] res = transformToInitMarkedPNPrivate(context, net, marking);
			if (res != null) {
				context.getFutureResult(0).setLabel("Transformed " + net.getLabel());
				context.getFutureResult(1).setLabel("Marking of transformed " + net.getLabel());
				return res;
			}
			return null;
		} catch (ConnectionCannotBeObtained e) {
			context.log("Unable to convert to Flexible model. No marking is found");
			return null;
		}
	}

	private Object[] transformToInitMarkedPNPrivate(PluginContext context, Petrinet net, Marking marking) {
		if (marking.size() > 0) {
			// enlist places that are marked
			List<Set<Place>> placeTobeAdded = new LinkedList<Set<Place>>();
			for (Place place : marking) {
				int index = marking.occurrences(place);
				if (placeTobeAdded.size() < index) {
					// add sets
					int origSize = placeTobeAdded.size();
					for (int i = origSize; i < index - 1; i++) {
						Set<Place> tempSet = new HashSet<Place>();
						placeTobeAdded.add(tempSet);
					}
					Set<Place> newSet = new HashSet<Place>();
					newSet.add(place);
					placeTobeAdded.add(newSet);
				} else {
					placeTobeAdded.get(index - 1).add(place);
				}
			}

			// copy the old petri net
			Petrinet newNet = PetrinetFactory.newPetrinet("Converted " + net.getLabel());
			Map<PetrinetNode, PetrinetNode> netMap = new HashMap<PetrinetNode, PetrinetNode>();
			for (Place p : net.getPlaces()) {
				netMap.put(p, newNet.addPlace(p.getLabel()));
			}
			for (Transition t : net.getTransitions()) {
				netMap.put(t, newNet.addTransition(t.getLabel()));
			}
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : net.getEdges()) {
				if (edge.getSource() instanceof Place) {
					newNet.addArc((Place) netMap.get(edge.getSource()), (Transition) netMap.get(edge.getTarget()));
				} else {
					newNet.addArc((Transition) netMap.get(edge.getSource()), (Place) netMap.get(edge.getTarget()));
				}
			}
			;

			List<Place> placeToContribute = new LinkedList<Place>();

			// copy all old net property
			Place lastAddedPlace = newNet.addPlace("tau");

			// new start place
			Marking newMarking = new Marking();
			newMarking.add(lastAddedPlace);

			for (int i = placeTobeAdded.size() - 1; i > 1; i--) {
				// add to place to contribute
				for (Place p : placeTobeAdded.get(i)) {
					placeToContribute.add((Place) netMap.get(p));
				}

				Transition newTransition = newNet.addTransition("tau");
				newNet.addArc(lastAddedPlace, newTransition);

				for (Place p : placeToContribute) {
					newNet.addArc(newTransition, p);
				}
				lastAddedPlace = newNet.addPlace("tau");
				newNet.addArc(newTransition, lastAddedPlace);
			}

			// last addition
			for (Place p : placeTobeAdded.get(0)) {
				placeToContribute.add((Place) netMap.get(p));
			}

			Transition newTransition = newNet.addTransition("tau");
			newNet.addArc(lastAddedPlace, newTransition);

			for (Place p : placeToContribute) {
				newNet.addArc(newTransition, p);
			}

			// add connection
			context.addConnection(new InitialMarkingConnection(newNet, newMarking));
			return new Object[] { newNet, newMarking };
		} else { // marking size is not bigger than zero
			return null;
		}
	}
}
