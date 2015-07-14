/**
 * 
 */
package org.processmining.plugins.connectionfactories.logpetrinet;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.connections.annotations.ConnectionObjectFactory;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.connections.petrinets.EvClassLogPetrinetConnection;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;

/**
 * @author aadrians
 *
 */
@ConnectionObjectFactory
@Plugin(name = "Event class of a Log/Petrinet connection factory", parameterLabels = { "Log", "Petrinet" }, returnLabels = "connection", returnTypes = EvClassLogPetrinetConnection.class, userAccessible = false)
public class EvClassLogPetrinetConnectionFactory {
	@PluginVariant(variantLabel = "Petrinet", requiredParameterLabels = { 0, 1 })
	public EvClassLogPetrinetConnection connect(UIPluginContext context, XLog log, PetrinetGraph net) {

		// list possible classifiers
		Object[] availableEventClass = new Object[4];
		availableEventClass[0] = XLogInfoImpl.STANDARD_CLASSIFIER; 
		availableEventClass[1] = XLogInfoImpl.NAME_CLASSIFIER; 
		availableEventClass[2] = XLogInfoImpl.LIFECYCLE_TRANSITION_CLASSIFIER; 
		availableEventClass[3] = XLogInfoImpl.RESOURCE_CLASSIFIER; 
		
		// build and show the UI to make the mapping
		EvClassLogPetrinetConnectionFactoryUI ui = new EvClassLogPetrinetConnectionFactoryUI(log, net, availableEventClass);
		InteractionResult result = context.showWizard("Mapping Petrinet - Event Class of Log", true, true, ui);

		// create the connection or not according to the button pressed in the UI
		EvClassLogPetrinetConnection con = null;
		if (result == InteractionResult.FINISHED) {
			con = new EvClassLogPetrinetConnection("Connection between " + net.getLabel() + " and " + XConceptExtension.instance().extractName(log), net, log, ui.getSelectedClassifier(), ui.getMap());

			// return the connection (or null if the connection hasn't been created)
			return con;
		} else {
			return null;
		}

	}
}
