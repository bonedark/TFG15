/**
 * 
 */
package org.processmining.plugins.petrinet.replayer;

import java.text.NumberFormat;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.connections.petrinets.EvClassLogPetrinetConnection;
import org.processmining.models.connections.petrinets.PNRepResultAllRequiredParamConnection;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayer.algorithms.IPNReplayAlgorithm;
import org.processmining.plugins.petrinet.replayer.algorithms.IPNReplayParameter;
import org.processmining.plugins.petrinet.replayer.ui.PNReplayerUI;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;

/**
 * @author aadrians
 * 
 */
@Plugin(name = "Replay a Log on Petri Net for Conformance Analysis", returnLabels = { "Petrinet log replay result" }, returnTypes = { PNRepResult.class }, parameterLabels = {
		"Petri net", "Event Log", "Mapping", "Replay Algorithm", "Parameters" }, help = "Replay an event log on Petri net to check conformance.", userAccessible = true)
public class PNLogReplayer {
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Arya Adriansyah", email = "a.adriansyah@tue.nl", pack = "Replayer")
	@PluginVariant(variantLabel = "From Petri net and Event Log", requiredParameterLabels = { 0, 1 })
	public PNRepResult replayLog(final UIPluginContext context, PetrinetGraph net, XLog log)
			throws ConnectionCannotBeObtained {
		PNReplayerUI pnReplayerUI = new PNReplayerUI();
		Object[] resultConfiguration = pnReplayerUI.getConfiguration(context, net, log);
		if (resultConfiguration == null) {
			context.getFutureResult(0).cancel(true);
			return null;
		}

		// if all parameters are set, replay log
		if (resultConfiguration[PNReplayerUI.MAPPING] != null) {
			context.log("replay is performed. All parameters are set.");

			// This connection MUST exists, as it is constructed by the configuration if necessary
			context.getConnectionManager().getFirstConnection(EvClassLogPetrinetConnection.class, context, net, log);

			// get all parameters
			IPNReplayAlgorithm selectedAlg = (IPNReplayAlgorithm) resultConfiguration[PNReplayerUI.ALGORITHM];
			IPNReplayParameter algParameters = (IPNReplayParameter) resultConfiguration[PNReplayerUI.PARAMETERS];

			// since based on GUI, create connection
			algParameters.setCreateConn(true);
			algParameters.setGUIMode(true);
			
			PNRepResult res = replayLog(context, net, log,
					(TransEvClassMapping) resultConfiguration[PNReplayerUI.MAPPING], selectedAlg, algParameters);

			context.getFutureResult(0).setLabel(
					"Result of replaying log " + XConceptExtension.instance().extractName(log) + " on "
							+ net.getLabel() + " using " + selectedAlg.toString());

			return res;

		} else {
			context.log("replay is not performed because not enough parameter is submitted");
			context.getFutureResult(0).cancel(true);
			return null;
		}
	}

	/**
	 * Main method to replay log.
	 * 
	 * @param context
	 * @param net
	 * @param log
	 * @param mapping
	 * @param selectedAlg
	 * @param parameters
	 * @return
	 */
	@PluginVariant(variantLabel = "Complete parameters", requiredParameterLabels = { 0, 1, 2, 3, 4 })
	public PNRepResult replayLog(PluginContext context, PetrinetGraph net, XLog log, TransEvClassMapping mapping,
			IPNReplayAlgorithm selectedAlg, IPNReplayParameter parameters) {
		if (selectedAlg.isAllReqSatisfied(context, net, log, mapping, parameters)) {
			// for each trace, replay according to the algorithm. Only returns two objects
			PNRepResult replayRes = null;

			if (parameters.isGUIMode()) {
				long start = System.nanoTime();

				replayRes = selectedAlg.replayLog(context, net, log, mapping, parameters);

				long period = System.nanoTime() - start;
				NumberFormat nf = NumberFormat.getInstance();
				nf.setMinimumFractionDigits(2);
				nf.setMaximumFractionDigits(2);

				context.log("Replay is finished in " + nf.format(period / 1000000000) + " seconds");
			} else {
				replayRes = selectedAlg.replayLog(context, net, log, mapping, parameters);
			}

			// add connection
			if (replayRes != null) {
				if (parameters.isCreatingConn()) {
					context.addConnection(new PNRepResultAllRequiredParamConnection(
							"Connection between replay result, " + XConceptExtension.instance().extractName(log)
									+ ", and " + net.getLabel(), net, log, mapping, selectedAlg, parameters, replayRes));
				}
			}

			return replayRes;
		} else {
			context.log("The provided parameters is not valid for the selected algorithm.");
			context.getFutureResult(0).cancel(true);
			return null;
		}
	}
}
