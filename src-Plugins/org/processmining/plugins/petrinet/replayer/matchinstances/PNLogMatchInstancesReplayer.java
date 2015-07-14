/**
 * 
 */
package org.processmining.plugins.petrinet.replayer.matchinstances;

import java.text.NumberFormat;
import java.util.Collection;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.util.Pair;
import org.processmining.models.connections.petrinets.PNMatchInstancesRepResultConnection;
import org.processmining.models.connections.petrinets.behavioral.FinalMarkingConnection;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.petrinet.replayer.matchinstances.algorithms.IPNMatchInstancesLogReplayAlgorithm;
import org.processmining.plugins.petrinet.replayer.matchinstances.ui.PNMatchInstancesReplayerUI;
import org.processmining.plugins.petrinet.replayresult.PNMatchInstancesRepResult;

/**
 * This class replay a log on a model and return the set of all best matching
 * alignments for all traces in the log.
 * 
 * NOTE: Some algorithms discard final markings, some are not.
 * 
 * @author aadrians
 * 
 */
@Plugin(name = "Replay a Log on Petri Net for Best Matching Alignments", returnLabels = { "Best matching alignments" }, returnTypes = { PNMatchInstancesRepResult.class }, parameterLabels = {
		"Petri net", "Event Log", "Mapping", "Initial Marking", "Final Marking", "Replay Algorithm", "Parameters" }, help = "Replay an event log on Petri net to obtain all best matching instances for each trace.", userAccessible = true)
public class PNLogMatchInstancesReplayer {

//	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Arya Adriansyah", email = "a.adriansyah@tue.nl", pack="Replayer")
	@PluginVariant(variantLabel = "From Petri net and Event Log", requiredParameterLabels = { 0, 1 })
	public PNMatchInstancesRepResult replayLog(final UIPluginContext context, Petrinet net, XLog log) {
		// check connection between petri net and marking
		Marking initMarking = null;
		try {
			initMarking = context.getConnectionManager()
					.getFirstConnection(InitialMarkingConnection.class, context, net)
					.getObjectWithRole(InitialMarkingConnection.MARKING);
		} catch (Exception exc) {
			initMarking = new Marking();
		}

		Marking finalMarking = null;
		try {
			finalMarking = context.getConnectionManager()
					.getFirstConnection(FinalMarkingConnection.class, context, net)
					.getObjectWithRole(FinalMarkingConnection.MARKING);
		} catch (Exception exc) {
			finalMarking = new Marking();
		}

		PNMatchInstancesReplayerUI pnReplayerUI = new PNMatchInstancesReplayerUI(context);
		Object[] resultConfiguration = pnReplayerUI.getConfiguration(net, log);
		if (resultConfiguration == null){
			context.getFutureResult(0).cancel(true);
			return null;
		}

		// if all parameters are set, replay log
		if (resultConfiguration[PNMatchInstancesReplayerUI.MAPPING] != null) {
			context.log("replay is performed. All parameters are set.");

			// get all parameters
			IPNMatchInstancesLogReplayAlgorithm selectedAlg = (IPNMatchInstancesLogReplayAlgorithm) resultConfiguration[PNMatchInstancesReplayerUI.ALGORITHM];

			@SuppressWarnings("unchecked")
			PNMatchInstancesRepResult res = replayLogPrivate(
					context,
					net,
					log,
					(Collection<Pair<Transition, XEventClass>>) resultConfiguration[PNMatchInstancesReplayerUI.MAPPING],
					initMarking, finalMarking, selectedAlg,
					(Object[]) resultConfiguration[PNMatchInstancesReplayerUI.PARAMETERS]);

			// add connection
			PNMatchInstancesRepResultConnection con = context.addConnection(new PNMatchInstancesRepResultConnection(
					"All results of replaying " + XConceptExtension.instance().extractName(log) + " on "
							+ net.getLabel(), net, initMarking, log, res));
			con.setLabel("Connection between " + net.getLabel() + ", " + XConceptExtension.instance().extractName(log)
					+ ", and matching instances");

			context.getFutureResult(0).setLabel(
					"All best matching instances of replaying log " + XConceptExtension.instance().extractName(log)
							+ " on " + net.getLabel() + " using " + selectedAlg.toString());

			return res;

		} else {
			context.log("replay is not performed because not enough parameter is submitted");
			context.getFutureResult(0).cancel(true);
			return null;
		}
	}

	@PluginVariant(variantLabel = "Replay Petri net on log, require complete parameters", requiredParameterLabels = { 0, 1, 2, 3, 4, 5, 6 })
	private PNMatchInstancesRepResult replayLogPrivate(PluginContext context, PetrinetGraph net, XLog log,
			Collection<Pair<Transition, XEventClass>> mapping, Marking initMarking, Marking finalMarking,
			IPNMatchInstancesLogReplayAlgorithm selectedAlg, Object[] parameters) {

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
		nf.setMinimumFractionDigits(2);
		
		long startTime = System.nanoTime();
		
		// for each trace, replay according to the algorithm. Only returns two objects
		PNMatchInstancesRepResult allReplayRes = selectedAlg.replayLog(context, net, initMarking, finalMarking, log, mapping,
				parameters);
		long duration = System.nanoTime() - startTime;
		
		context.log("Replay is finished in " + nf.format(duration / 1000000000) + " seconds");

		return allReplayRes;
	}
}
