/**
 * 
 */
package org.processmining.plugins.replayer.converter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.connections.flexiblemodel.FlexStartTaskNodeConnection;
import org.processmining.models.connections.flexiblemodel.conversion.PNOfFlexModelConnection;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.models.flexiblemodel.FlexEdge;
import org.processmining.models.flexiblemodel.FlexNode;
import org.processmining.models.flexiblemodel.SetFlex;
import org.processmining.models.flexiblemodel.StartTaskNodesSet;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetFactory;
import org.processmining.models.semantics.petrinet.Marking;

/**
 * @author arya
 * 
 * This class convert Flexible model to Petri net
 */
@Plugin(name = "Convert Flexible model to Petri Net", parameterLabels = { "Flexible model", "Start task node" }, returnLabels = {
		"Petri net", "Initial marking" }, returnTypes = { Petrinet.class, Marking.class }, userAccessible = true)
public class PNFromFlex {
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "A. Adriansyah", email = "a.adriansyah@tue.nl", uiLabel = UITopiaVariant.USEPLUGIN, pack="Replayer")
	@PluginVariant(variantLabel = "Convert Flexible model to Petri net", requiredParameterLabels = { 0 })
	public Object[] convertToPN(PluginContext context, Flex net) {
		// check connection between net and marking
		FlexStartTaskNodeConnection conn = null;
		StartTaskNodesSet startTaskNodes = null;
		try {
			conn = context.getConnectionManager().getFirstConnection(FlexStartTaskNodeConnection.class, context, net);
			if (conn != null) {
				startTaskNodes = conn.getObjectWithRole(FlexStartTaskNodeConnection.STARTTASKNODES);
			}
		} catch (ConnectionCannotBeObtained e) {
			// nothing 
		}
		return convertToPN(context, net, startTaskNodes);
	}

	@PluginVariant(variantLabel = "Convert Flexible model to Petri net", requiredParameterLabels = { 0, 1 })
	public Object[] convertToPN(PluginContext context, Flex net, StartTaskNodesSet startTaskNodes) {
		// check connection between net and start task node
		if (startTaskNodes != null) {
			try {
				context.getConnectionManager().getFirstConnection(FlexStartTaskNodeConnection.class, context, net,
						startTaskNodes);
			} catch (ConnectionCannotBeObtained e) {
				context.log("Inappropriate starting task node is provided.");
				context.getFutureResult(0).cancel(true);
				context.getFutureResult(1).cancel(true);
				return null;
			}
		}
		return convertToPNPrivate(context, net, startTaskNodes);
	}

	private Object[] convertToPNPrivate(PluginContext context, Flex net, StartTaskNodesSet startTaskNodes) {
		// utility variables
		Map<FlexNode, Place> mapToInputPlace = new HashMap<FlexNode, Place>();
		Map<FlexNode, Place> mapToOutputPlace = new HashMap<FlexNode, Place>();
		
		// create petri net
		Petrinet pn = PetrinetFactory.newPetrinet("Petri net of " + net.getLabel());
		
		// create input and output place
		for (FlexNode node : net.getNodes()){
			Transition trans = pn.addTransition(node.getLabel());
			trans.setInvisible(node.isInvisible());
			Place inputPlace = pn.addPlace(trans.getLabel() + "_in");
			Place outputPlace = pn.addPlace(trans.getLabel() + "_out");
			pn.addArc(inputPlace, trans);
			pn.addArc(trans, outputPlace);
			mapToInputPlace.put(node, inputPlace);
			mapToOutputPlace.put(node, outputPlace);
		}
		
		// translate arcs to places
		Map<FlexEdge<? extends FlexNode, ? extends FlexNode>, Place> mapToEdge = new HashMap<FlexEdge<? extends FlexNode,? extends FlexNode>, Place>();
		for (FlexEdge<? extends FlexNode, ? extends FlexNode> edge : net.getEdges()){
			// create place
			Place pl = pn.addPlace(edge.getSource().getLabel() + "--" + edge.getTarget().getLabel());
			mapToEdge.put(edge, pl);
		};
		
		// translate io-bindings to transitions
		for (FlexNode node : net.getNodes()){
			// output io binding
			Set<SetFlex> setOutputNodes = node.getOutputNodes();
			if (!setOutputNodes.isEmpty()){
				for (SetFlex setFlex : setOutputNodes){
					if (!setFlex.isEmpty()){
						// create transition for this io-binding
						Transition trans = pn.addTransition(setFlex.toString());
						trans.setInvisible(true);
						
						for (FlexNode outputNode : setFlex){
							pn.addArc(trans, mapToEdge.get(net.getArc(node, outputNode)));
						}
						
						pn.addArc(mapToOutputPlace.get(node), trans);
					}
				}
			}
			
			// input io binding
			Set<SetFlex> setInputNodes = node.getInputNodes();
			if (!setInputNodes.isEmpty()){
				for (SetFlex setFlex : setInputNodes){
					if (!setFlex.isEmpty()){
						// create transition for io-binding
						Transition trans = pn.addTransition(setFlex.toString());
						trans.setInvisible(true);
						
						for (FlexNode inputNode : setFlex){
							pn.addArc(mapToEdge.get(net.getArc(inputNode, node)), trans);
						}
						
						pn.addArc(trans, mapToInputPlace.get(node));
					}
				}
			}
		}
		
		Marking m = new Marking();
		if (startTaskNodes != null){
			for (FlexNode node : startTaskNodes.iterator().next()){
				m.add(mapToInputPlace.get(node));
			}
		}

		// create connection
		context.addConnection(new InitialMarkingConnection(pn, m));
		context.addConnection(new PNOfFlexModelConnection("Connection to Petri net of " + net.getLabel(), net, startTaskNodes, pn, m));
		
		context.getFutureResult(0).setLabel("Petri net of " + net.getLabel());
		context.getFutureResult(1).setLabel("Initial marking of Petri net from " + net.getLabel());
		
		return new Object[] {pn, m};
	}
}
