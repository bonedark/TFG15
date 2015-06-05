/**
 * 
 */
package org.processmining.plugins.petrinet.replayresult.visualization.projection;

import javax.swing.JComponent;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.connections.petrinets.EvClassLogPetrinetConnection;
import org.processmining.models.connections.petrinets.PNRepResultAllRequiredParamConnection;
import org.processmining.models.connections.petrinets.PNRepResultConnection;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;

/**
 * @author aadrians
 * Oct 26, 2011
 *
 */
@Plugin(name = "View Projection to Model", returnLabels = { "Projected Log-Model Alignment to Model" }, returnTypes = { JComponent.class }, parameterLabels = { "Log-Model alignment" }, userAccessible = false)
@Visualizer
public class PNLogReplayProjectedVis {
	@PluginVariant(requiredParameterLabels = { 0 })
	public JComponent visualize(PluginContext context, PNRepResult logReplayResult) {
		System.gc();
		PetrinetGraph net = null;
		XLog log = null;
		TransEvClassMapping map = null;
		Marking initMarking = null;
		try {
			PNRepResultAllRequiredParamConnection conn = context.getConnectionManager().getFirstConnection(PNRepResultAllRequiredParamConnection.class, context, logReplayResult);
			
			net = conn.getObjectWithRole(PNRepResultConnection.PN);
			log = conn.getObjectWithRole(PNRepResultConnection.LOG);
			
			// get mapping between the log and the net
			EvClassLogPetrinetConnection mapConn = context.getConnectionManager().getFirstConnection(EvClassLogPetrinetConnection.class, context, net, log);
			map = mapConn.getObjectWithRole(EvClassLogPetrinetConnection.TRANS2EVCLASSMAPPING);
			
		} catch (Exception exc){
			context.log("No net can be found for this log replay result");
			context.getFutureResult(0).cancel(true);
			return null;
		}
		
		try {
			// get initial marking of the net
			InitialMarkingConnection initMarkingConn = context.getConnectionManager().getFirstConnection(InitialMarkingConnection.class, context, net);
			initMarking = initMarkingConn.getObjectWithRole(InitialMarkingConnection.MARKING);
		} catch (Exception exc){
			// it's okay not to have any initial marking
			initMarking = new Marking();
		}
		return new PNLogReplayProjectedVisPanel(context, net, initMarking, log, map, logReplayResult);
	}
}
