/**
 * 
 */
package org.processmining.plugins.petrinet.finalmarkingprovider;

import java.util.Collection;

import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.connections.petrinets.behavioral.FinalMarkingConnection;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.semantics.petrinet.Marking;

/**
 * @author aadrians
 * Nov 24, 2011
 *
 */
@Plugin(name = "Create final markings", returnLabels = { "Final marking" }, returnTypes = { Marking.class }, parameterLabels = {
		"Petri net", "Marking array" }, help = "Create final markings of a Petri net.", userAccessible = true)
public class FinalMarkingProducer {
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Arya Adriansyah", email = "a.adriansyah@tue.nl", pack = "Replayer")
	@PluginVariant(variantLabel = "Use GUI Editor", requiredParameterLabels = { 0 })
	public Marking constructFinalMarking(UIPluginContext context, PetrinetGraph net){
		FinalMarkingEditorPanel editor = new FinalMarkingEditorPanel();
		Marking[] finalMarkings = editor.getMarkings(context, net);
		
		if ((finalMarkings == null)||(finalMarkings.length == 0)){
			context.getFutureResult(0).cancel(true);
			return null;
		}
		
		// return the first final marking
		return constructFinalMarking(context, net, finalMarkings);
	}
	
	@PluginVariant(variantLabel = "From Petri net and Event Log", requiredParameterLabels = { 0,1 })
	public Marking constructFinalMarking(PluginContext context, PetrinetGraph net, Marking[] finalMarkings){
		if ((finalMarkings == null)||(finalMarkings.length == 0)){
			throw new IllegalArgumentException("No final marking is provided");
		}
		for (Marking m : finalMarkings){
			constructFinalMarking(context, net, m);
		}
		// return the first final marking
		return finalMarkings[0];
	}
	
	public Marking constructFinalMarking(PluginContext context, PetrinetGraph net, Marking finalMarking){
		if (finalMarking == null){
			throw new IllegalArgumentException("No final marking is provided");
		}
		
		Collection<Place> colPlaces = net.getPlaces();
		for (Place p : finalMarking){
			if (!colPlaces.contains(p)){
				throw new IllegalArgumentException("Final marking contains places outside of the net");
			}
		}
		context.addConnection(new FinalMarkingConnection(net, finalMarking));
		return finalMarking;
	}
}
