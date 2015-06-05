/**
 * 
 */
package org.processmining.plugins.replayer.converter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.connections.flexiblemodel.FlexEndTaskNodeConnection;
import org.processmining.models.connections.flexiblemodel.FlexSpecialNodesConnection;
import org.processmining.models.connections.flexiblemodel.FlexStartTaskNodeConnection;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.flexiblemodel.EndTaskNodesSet;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.models.flexiblemodel.FlexFactory;
import org.processmining.models.flexiblemodel.FlexNode;
import org.processmining.models.flexiblemodel.SetFlex;
import org.processmining.models.flexiblemodel.StartTaskNodesSet;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.flex.replayer.performance.util.FlexSpecialNodes;



/**
 * @author Arya Adriansyah
 * @email a.adriansyah@tue.nl
 * @version Feb 16, 2011
 * 
 * This class convert petri net to Flexible model
 */
@Plugin(name = "Convert Petri Net to Flexible model", parameterLabels = { "Petri net", "Marking" }, returnLabels = {
		"Flexible model", "Start task node", "End task node", "Special nodes (places)" }, returnTypes = { Flex.class, StartTaskNodesSet.class, EndTaskNodesSet.class, FlexSpecialNodes.class  }, userAccessible = true)
public class FlexFromPN {
	@PluginVariant(variantLabel = "Convert Petri net to Flexible model", requiredParameterLabels = { 0, 1 })
	public Object[] convertToFM(PluginContext context, Petrinet net, Marking marking) {
		// check connection between net and marking
		try {
			context.getConnectionManager().getFirstConnection(InitialMarkingConnection.class, context, net, marking);
			return convertToFMPrivate(context, net, marking);
		} catch (ConnectionCannotBeObtained e) {
			context.log("Unable to convert to Flexible model. No connection between the net and its marking");
			context.getFutureResult(0).cancel(true);
			context.getFutureResult(1).cancel(true);
			context.getFutureResult(2).cancel(true);
			context.getFutureResult(3).cancel(true);
			return null;
		}
	}

	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "A. Adriansyah", email = "a.adriansyah@tue.nl", uiLabel = UITopiaVariant.USEPLUGIN, pack="Replayer")
	@PluginVariant(variantLabel = "Convert Petri net to Flexible model", requiredParameterLabels = { 0 })
	public Object[] convertToFM(PluginContext context, Petrinet net) {
		// check connection between net and marking
		try {
			InitialMarkingConnection conn = context.getConnectionManager().getFirstConnection(
					InitialMarkingConnection.class, context, net);
			Marking marking = (Marking) conn.getObjectWithRole(InitialMarkingConnection.MARKING);
			return convertToFMPrivate(context, net, marking);
		} catch (ConnectionCannotBeObtained e) {
			context.log("Unable to convert to Flexible model. No marking is found");
			context.getFutureResult(0).cancel(true);
			context.getFutureResult(1).cancel(true);
			context.getFutureResult(2).cancel(true);
			context.getFutureResult(3).cancel(true);
			return null;
		}
	}

	private Object[] convertToFMPrivate(PluginContext context, Petrinet net, Marking marking) {
		// create Flexible model, start task nodes, and special nodes
		Flex flexModel = FlexFactory.newFlex("Flexible model of " + net.getLabel());

		// all places have XOR-join and XOR-split semantics
		Map<PetrinetNode, FlexNode> mapNetFlex = new HashMap<PetrinetNode, FlexNode>();
		Map<FlexNode, PetrinetNode> mapFlexNet = new HashMap<FlexNode, PetrinetNode>();

		Collection<Transition> transCol = net.getTransitions();
		for (Transition t : transCol) {
			FlexNode task = flexModel.addNode(t.getLabel());
			task.setInvisible(t.isInvisible());

			// map transitions
			mapNetFlex.put(t, task);
			mapFlexNet.put(task, t);

			// get input places for a transition
			SetFlex inputTasks = new SetFlex();
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : net.getInEdges(t)) {
				PetrinetNode inputPlace = edge.getSource();
				FlexNode flexPlaceNode;
				if ((flexPlaceNode = mapNetFlex.get(inputPlace)) == null) {
					// create new node for this place
					flexPlaceNode = flexModel.addNode(inputPlace.getLabel());
					flexPlaceNode.setInvisible(true);
					
					// add to mapping
					mapNetFlex.put(inputPlace, flexPlaceNode);
				}

				// input of trans
				inputTasks.add(flexPlaceNode);

				// output of the place
				SetFlex outputOfPlace = new SetFlex();
				outputOfPlace.add(task);
				flexPlaceNode.addOutputNodes(outputOfPlace);
				
				// create arcs
				flexModel.addArc(flexPlaceNode, task);
			}
			task.addInputNodes(inputTasks);

			// get output places for a transition
			SetFlex outputTasks = new SetFlex();
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : net.getOutEdges(t)) {
				PetrinetNode outputPlace = edge.getTarget();
				FlexNode flexPlaceNode;
				if ((flexPlaceNode = mapNetFlex.get(outputPlace)) == null) {
					// create new node for this place
					flexPlaceNode = flexModel.addNode(outputPlace.getLabel());
					flexPlaceNode.setInvisible(true);

					// add to mapping
					mapNetFlex.put(outputPlace, flexPlaceNode);
				}

				// input of trans
				outputTasks.add(flexPlaceNode);

				// output of the place
				SetFlex inputOfPlace = new SetFlex();
				inputOfPlace.add(task);
				flexPlaceNode.addInputNodes(inputOfPlace);
				
				// create arcs
				flexModel.addArc(task, flexPlaceNode);
			}
			task.addOutputNodes(outputTasks);
		}

		// identify start task nodes and end task nodes
		
		StartTaskNodesSet startTaskNodesSet = new StartTaskNodesSet();
		SetFlex startTaskNodes = new SetFlex();
		SetFlex endTaskNodes = new SetFlex();
		for (Place place : net.getPlaces()) {
			if (marking.contains(place)) {
				FlexNode startNode = mapNetFlex.get(place);
				if (startNode != null) {
					startTaskNodes.add(startNode);
				}
			}

			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> edges = net.getOutEdges(place);
			if (edges.isEmpty()){
				endTaskNodes.add(mapNetFlex.get(place));
			}
		}
		
		if (startTaskNodes.size() > 1){
			// preprocess such that only one start task nodes exist
			FlexNode dummyStartTaskNode = flexModel.addNode("start");
			dummyStartTaskNode.setInvisible(true);
			SetFlex inputSet = new SetFlex();
			inputSet.add(dummyStartTaskNode);
			
			for (FlexNode node : startTaskNodes){
				// add arc
				flexModel.addArc(dummyStartTaskNode, node);
				
				// update output set for dummy start task
				SetFlex outputSet = new SetFlex();
				outputSet.add(node);
				dummyStartTaskNode.addOutputNodes(outputSet);
				
				// update input set
				node.addInputNodes(inputSet);
			}
			
			SetFlex newStartTaskNodes = new SetFlex();
			newStartTaskNodes.add(dummyStartTaskNode);
			
			startTaskNodesSet.add(newStartTaskNodes);
		} else if (startTaskNodes.size() == 1){
			startTaskNodesSet.add(startTaskNodes);
		}

		EndTaskNodesSet endTaskNodesSet = new EndTaskNodesSet();
		if (endTaskNodes.size() > 1){
			// preprocess such that only one end task node exist
			FlexNode dummyEndTaskNode = flexModel.addNode("end");
			dummyEndTaskNode.setInvisible(true);
			SetFlex outputSet = new SetFlex();
			outputSet.add(dummyEndTaskNode);
			
			for (FlexNode node : endTaskNodes){
				// add arc
				flexModel.addArc(node, dummyEndTaskNode);
				
				// update input set for dummy end task
				SetFlex inputSet = new SetFlex();
				inputSet.add(node);
				dummyEndTaskNode.addInputNodes(inputSet);
				
				// update output set
				node.addOutputNodes(outputSet);
			}
			
			SetFlex newEndTaskNodes = new SetFlex();
			newEndTaskNodes.add(dummyEndTaskNode);
			
			endTaskNodesSet.add(newEndTaskNodes);
		} else if (endTaskNodes.size() == 1){
			endTaskNodesSet.add(endTaskNodes);
		}
		
		// update tooltip
		for (FlexNode node : flexModel.getNodes()){
			node.commitUpdates();
		}
		
		// create connection between Flexible model and start task node
		context.addConnection(new FlexStartTaskNodeConnection("Connection to start task node of " + flexModel.getLabel(), flexModel, startTaskNodesSet));

		// create connection between Flexible model and end task node
		context.addConnection(new FlexEndTaskNodeConnection("Connection to end task node of " + flexModel.getLabel(), flexModel, endTaskNodesSet));
		
		// create special nodes
		FlexSpecialNodes specialNodes = new FlexSpecialNodes();
		for (Place place : net.getPlaces()){
			specialNodes.addLateBindingNodes(mapNetFlex.get(place));
		}
		
		// create connection between Flexible model and special nodes
		context.addConnection(new FlexSpecialNodesConnection("Connection to special nodes of " + flexModel.getLabel(), flexModel, specialNodes));
		
		return new Object[] { flexModel, startTaskNodesSet, endTaskNodesSet, specialNodes };
	}
}
