/**
 * 
 */
package org.processmining.plugins.flex.analysis;

import java.util.Set;

import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.connections.ConnectionManager;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.connections.flexiblemodel.FlexEndTaskNodeConnection;
import org.processmining.models.connections.flexiblemodel.FlexSoundnessInfoConnection;
import org.processmining.models.connections.flexiblemodel.FlexStartTaskNodeConnection;
import org.processmining.models.connections.flexiblemodel.conversion.PNOfFlexModelConnection;
import org.processmining.models.connections.petrinets.behavioral.NonRelaxedSoundTransitionsConnection;
import org.processmining.models.flexiblemodel.EndTaskNodesSet;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.models.flexiblemodel.FlexNode;
import org.processmining.models.flexiblemodel.SetFlex;
import org.processmining.models.flexiblemodel.StartTaskNodesSet;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.analysis.NonRelaxedSoundTransitionsSet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.flex.analysis.result.FlexAnalysisInformation;
import org.processmining.plugins.flex.analysis.result.FlexAnalysisInformation.UnDetBool;

/**
 * Soundness checking of flexible model A flexible model is sound if and only if for
 * each transition, there is a valid trace that include the transition.
 * Stepwise: 1. Construct a Petri net from the flex model 2. Check relaxed
 * soundness of the net using lola state predicate
 * 
 * @author aadrians
 * 
 */
@Plugin(name = "Check Soundness of Flexible model", returnLabels = { "Relaxed soundness info", "Start task node",
		"End task node" }, returnTypes = { FlexAnalysisInformation.SOUNDNESS.class, StartTaskNodesSet.class,
		EndTaskNodesSet.class }, parameterLabels = { "Flexible model", "Start task node", "End task node" }, userAccessible = true, mostSignificantResult = -1)
public class SoundnessChecking {

	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Arya Adriansyah", email = "a.adriansyah@tue.nl", pack = "Replayer")
	@PluginVariant(variantLabel = "Analyze soundness given only Flexible model", requiredParameterLabels = { 0 })
	public Object[] diagnose(PluginContext context, Flex flex) {
		ConnectionManager connManager = context.getConnectionManager();

		StartTaskNodesSet startTaskNodesSet = null;
		try {
			FlexStartTaskNodeConnection conn = connManager.getFirstConnection(FlexStartTaskNodeConnection.class,
					context, flex);
			startTaskNodesSet = conn.getObjectWithRole(FlexStartTaskNodeConnection.STARTTASKNODES);
		} catch (Exception e) {
			context.log("Artificial start task nodes are created as there is no start task node");

			// create start task nodes from nodes without any predecessors
			SetFlex setFlex = new SetFlex();
			for (FlexNode node : flex.getNodes()) {
				Set<SetFlex> setInputNodes = node.getInputNodes();
				if (setInputNodes.isEmpty()) {
					setFlex.add(node);
				}
			}

			if (setFlex.isEmpty()) {
				context.log("Soundness checking cannot be performed as start task node fail to be constructed");
				context.getFutureResult(0).cancel(true);
				return null;
			} else {
				startTaskNodesSet = new StartTaskNodesSet();
				startTaskNodesSet.add(setFlex);
				context.addConnection(new FlexStartTaskNodeConnection("Connection between " + flex.getLabel()
						+ " to its start task node", flex, startTaskNodesSet));
				context.getFutureResult(1).setLabel("Start task node of " + flex.getLabel());
			}
		}

		EndTaskNodesSet endTaskNodesSet = null;
		try {
			FlexEndTaskNodeConnection conn = connManager.getFirstConnection(FlexEndTaskNodeConnection.class, context,
					flex);
			endTaskNodesSet = conn.getObjectWithRole(FlexEndTaskNodeConnection.ENDTASKNODES);
		} catch (Exception e) {
			context.log("Artificial start task nodes are created as there is no end task node");

			// create start task nodes from nodes without any predecessors
			SetFlex setFlex = new SetFlex();
			for (FlexNode node : flex.getNodes()) {
				Set<SetFlex> setOutputNodes = node.getOutputNodes();
				if (setOutputNodes.isEmpty()) {
					setFlex.add(node);
				}
			}

			if (setFlex.isEmpty()) {
				context.log("Soundness checking cannot be performed as end task node fail to be constructed");
				context.getFutureResult(0).cancel(true);
				return null;

			} else {
				endTaskNodesSet = new EndTaskNodesSet();
				endTaskNodesSet.add(setFlex);
				context.addConnection(new FlexEndTaskNodeConnection("Connection between " + flex.getLabel()
						+ " to its end task node", flex, endTaskNodesSet));
				context.getFutureResult(2).setLabel("End task node of " + flex.getLabel());
			}
		}

		return diagnose(context, flex, startTaskNodesSet, endTaskNodesSet);
	}

	@PluginVariant(variantLabel = "Analyze soundness given Flexible model, start task nodes, and end task nodes", requiredParameterLabels = {
			0, 1, 2 })
	public Object[] diagnose(PluginContext context, Flex flex,
			StartTaskNodesSet startTaskNodesSet, EndTaskNodesSet endTaskNodesSet) {
		ConnectionManager connManager = context.getConnectionManager();
		// check start task node connection
		try {
			connManager.getFirstConnection(FlexStartTaskNodeConnection.class, context, flex, startTaskNodesSet);
		} catch (ConnectionCannotBeObtained exc) {
			context.log("Soundness checking cannot be performed as the provided start task nodes does not belong to the given flexible model.");
			context.getFutureResult(0).cancel(true);
			return null;
		}

		// check end task node connection
		try {
			connManager.getFirstConnection(FlexEndTaskNodeConnection.class, context, flex, endTaskNodesSet);
		} catch (ConnectionCannotBeObtained exc) {
			context.log("Soundness checking cannot be performed as the provided end task nodes does not belong to the given flexible model.");
			context.getFutureResult(0).cancel(true);
			return null;
		}

		FlexAnalysisInformation.SOUNDNESS soundness = checkSoundness(context, flex, startTaskNodesSet, endTaskNodesSet);
		context.getFutureResult(0).setLabel("Soundness of " + flex.getLabel());

		return new Object[] {soundness, startTaskNodesSet, endTaskNodesSet};
	}

	/**
	 * 
	 * @param context
	 * @param flex
	 * @param startTaskNodesSet
	 * @param endTaskNodesSet
	 * @return
	 */
	private FlexAnalysisInformation.SOUNDNESS checkSoundness(PluginContext context, Flex flex,
			StartTaskNodesSet startTaskNodesSet, EndTaskNodesSet endTaskNodesSet) {
		// convert to petri net
		Petrinet net = null;
		Marking m = null;
		try {
			net = context.tryToFindOrConstructFirstObject(Petrinet.class, PNOfFlexModelConnection.class,
					PNOfFlexModelConnection.PETRINET, flex, startTaskNodesSet);

			PNOfFlexModelConnection conn = context.getConnectionManager().getFirstConnection(
					PNOfFlexModelConnection.class, context, net);
			m = conn.getObjectWithRole(PNOfFlexModelConnection.MARKING);
		} catch (ConnectionCannotBeObtained exc) {
			context.log("Unable to create a petri net for this flexible model");
			context.getFutureResult(0).cancel(true);
			return null;
		}

		assert (net != null);

		// check relaxed soundness property of the net
		// make sure that there is only a single initial marking
		if (m.size() != 1) {
			FlexAnalysisInformation.SOUNDNESS soundness = new FlexAnalysisInformation.SOUNDNESS();
			soundness.setValue(UnDetBool.UNDETERMINED);
			context.log("No soundness checking is performed as there is no single initially marked place");
			context.addConnection(new FlexSoundnessInfoConnection("Connection to soundness info of " + flex.getLabel(),
					flex, soundness));
			context.getFutureResult(0).setLabel("Soundness info of " + flex.getLabel());
			return soundness;
		}

		try {
			NonRelaxedSoundTransitionsSet set = context.tryToFindOrConstructFirstObject(
					NonRelaxedSoundTransitionsSet.class, NonRelaxedSoundTransitionsConnection.class,
					NonRelaxedSoundTransitionsConnection.TRANSITIONS, net, m);
			if ((set != null) && (set.isEmpty())) {
				FlexAnalysisInformation.SOUNDNESS soundness = new FlexAnalysisInformation.SOUNDNESS();
				soundness.setValue(UnDetBool.TRUE);
				context.log("The Flexible model is sound");
				context.addConnection(new FlexSoundnessInfoConnection("Connection to soundness info of "
						+ flex.getLabel(), flex, soundness));
				context.getFutureResult(0).setLabel("Soundness info of " + flex.getLabel());
				return soundness;
			}
		} catch (Exception exc) {
			// not sound
		}

		FlexAnalysisInformation.SOUNDNESS soundness = new FlexAnalysisInformation.SOUNDNESS();
		soundness.setValue(UnDetBool.FALSE);
		context.log("The Flexible model is NOT sound");
		context.addConnection(new FlexSoundnessInfoConnection("Connection to soundness info of " + flex.getLabel(),
				flex, soundness));
		context.getFutureResult(0).setLabel("Soundness info of " + flex.getLabel());
		return soundness;
	}
}
