/**
 * 
 */
package org.processmining.plugins.petrinet.generator;

import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetFactory;
import org.processmining.models.semantics.petrinet.Marking;

/**
 * @author aadrians Nov 30, 2011
 * 
 */
@Plugin(name = "Generate logs permutation", returnLabels = { "Petri net", "Initial Marking" }, returnTypes = {
		Petrinet.class, Marking.class }, parameterLabels = { "Event log" }, help = "Generate a precise net.", userAccessible = true)
public class PetrinetGenerator {
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Arya Adriansyah", email = "a.adriansyah@tue.nl", pack = "Replayer")
	@PluginVariant(variantLabel = "Generate overprecise net", requiredParameterLabels = { 0 })
	public Object[] createInterleavedLog(final PluginContext context, XLog log) {
		Petrinet net = PetrinetFactory.newPetrinet("Net of " + XConceptExtension.instance().extractName(log));
		
		// lib
		XLogInfo summary = XLogInfoFactory.createLogInfo(log, XLogInfoImpl.STANDARD_CLASSIFIER);
		XEventClasses eventClasses = summary.getEventClasses();
		
		// create one 
		for (XTrace trace : log){
			String stringRepresentation = createStringRep(trace, eventClasses);
			
		}
		
		
		// create intial place and marking
		Place initPlace = net.addPlace("init");
		Marking m = new Marking();
		m.add(initPlace);
		
		context.addConnection(new InitialMarkingConnection(net, m));
		
		return new Object[] {net, m};
	}

	private String createStringRep(XTrace trace, XEventClasses eventClasses) {
		for (XEvent event : trace){
			eventClasses.getClassOf(event);

		}
		return null;
	}

}
