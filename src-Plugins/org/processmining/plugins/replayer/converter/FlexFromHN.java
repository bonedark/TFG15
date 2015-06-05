///**
// * 
// */
package org.processmining.plugins.replayer.converter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.deckfour.xes.classification.XEventClass;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.connections.flexiblemodel.FlexStartTaskNodeConnection;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.models.flexiblemodel.FlexFactory;
import org.processmining.models.flexiblemodel.FlexNode;
import org.processmining.models.flexiblemodel.SetFlex;
import org.processmining.models.flexiblemodel.StartTaskNodesSet;
import org.processmining.models.heuristics.HeuristicsNet;
import org.processmining.models.heuristics.impl.HNSet;
import org.processmining.models.heuristics.impl.HNSubSet;




/**
 * @author Arya Adriansyah
 * @email a.adriansyah@tue.nl
 * @version Apr 15, 2010
 * 
 *          The class convert heuristic net to Flexible model
 */
//@Plugin(name = "Convert heuristic net to Flexible model", parameterLabels = { "Heuristic net", "Marking" }, returnLabels = {
//		"Flexible model", "Start task node" }, returnTypes = { Flex.class, StartTaskNodesSet.class }, userAccessible = true)
public class FlexFromHN {
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "A. Adriansyah", email = "a.adriansyah@tue.nl", uiLabel = UITopiaVariant.USEPLUGIN, pack="Replayer")
	@PluginVariant(variantLabel = "Convert Heuristic net to Flexible model", requiredParameterLabels = { 0 })
	public Object[] convertToFM(PluginContext context, HeuristicsNet net) {
		// check connection between net and marking
		return convertHeurNetToFMPrivate(context,net);
	}

	/**
	 * @param context
	 * @param net
	 * @return
	 */
	private Object[] convertHeurNetToFMPrivate(PluginContext context, HeuristicsNet net) {
		// check if there is only one starting task node
		HNSubSet startActHeur = net.getStartActivities();
		if (startActHeur.size() == 1) {
			// result Flexible model
			Flex flexModel = FlexFactory.newFlex("Flexible model of " + net.toString());

			Map<XEventClass, FlexNode> nodeMap = new HashMap<XEventClass, FlexNode>();

			//adding the activities and their input/output gateways
			XEventClass[] activities =  net.getActivitiesMappingStructures().getActivitiesMapping();

			for (int activityIndex = 0; activityIndex < activities.length; activityIndex++) {
				//adding the activities
				nodeMap.put(activities[activityIndex], flexModel.addNode(activities[activityIndex].getId()));
			}

			for (int activityIndex = 0; activityIndex < activities.length; activityIndex++) {
				// current node
				FlexNode currNode = nodeMap.get(activities[activityIndex]);

				//adding the output gateways
				HNSet outputActivitiesSet = net.getOutputSet(activityIndex);
				Queue<SetFlex> tempOutputSets = new LinkedList<SetFlex>();

				// pair all member of the set
				for (int outputSubsetIndex = 0; outputSubsetIndex < outputActivitiesSet.size(); outputSubsetIndex++) {
					HNSubSet subset = outputActivitiesSet.get(outputSubsetIndex);

					// take a set from tempOutputSets and add each elements of subset
					int size = tempOutputSets.size();
					if (size > 0) {
						while (size > 0) {
							Set<FlexNode> subsetoutflex = tempOutputSets.poll();
							for (int j = 0; j < subset.size(); j++) {
								SetFlex newoutflex = new SetFlex();
								newoutflex.addAll(subsetoutflex);
								newoutflex.add(nodeMap.get(activities[subset.get(j)]));
								tempOutputSets.add(newoutflex);
								flexModel.addArc(currNode, nodeMap.get(activities[subset.get(j)]));
							}
							size--;
						}
					} else {
						// size == 0, make a separate set for each member of subset
						for (int j = 0; j < subset.size(); j++) {
							SetFlex newoutflex = new SetFlex();
							newoutflex.add(nodeMap.get(activities[subset.get(j)]));
							tempOutputSets.add(newoutflex);
							flexModel.addArc(currNode, nodeMap.get(activities[subset.get(j)]));
						}
					}
				}

				while (tempOutputSets.peek() != null) {
					currNode.addOutputNodes(tempOutputSets.poll());
				}

				//adding the input gateways
				HNSet inputActivitiesSet = net.getInputSet(activityIndex);
				Queue<SetFlex> tempInputSets = new LinkedList<SetFlex>();
				for (int inputSubsetIndex = 0; inputSubsetIndex < inputActivitiesSet.size(); inputSubsetIndex++) {
					HNSubSet subset = inputActivitiesSet.get(inputSubsetIndex);
					int size = tempInputSets.size();
					if (size > 0) {
						while (size > 0) {
							Set<FlexNode> subsetinflex = tempInputSets.poll();
							for (int j = 0; j < subset.size(); j++) {
								SetFlex newinflex = new SetFlex();
								newinflex.addAll(subsetinflex);
								newinflex.add(nodeMap.get(activities[subset.get(j)]));
								tempInputSets.add(newinflex);
								flexModel.addArc(nodeMap.get(activities[subset.get(j)]), currNode);
							}
							size--;
						}
					} else {
						// size == 0, create new set for each subset
						for (int j = 0; j < subset.size(); j++) {
							SetFlex newinflex = new SetFlex();
							newinflex.add(nodeMap.get(activities[subset.get(j)]));
							tempInputSets.add(newinflex);
							flexModel.addArc(nodeMap.get(activities[subset.get(j)]), currNode);
						}
					}
				}

				while (tempInputSets.peek() != null) {
					currNode.addInputNodes(tempInputSets.poll());
				}
				currNode.commitUpdates();
			}

			SetFlex setFlex = new SetFlex();
			for (int i=0; i < startActHeur.size(); i++){
				setFlex.add(nodeMap.get(activities[startActHeur.get(i)]));
			}
			StartTaskNodesSet startTaskNodesSet = new StartTaskNodesSet();
			startTaskNodesSet.add(setFlex);

			// create connection
			context.addConnection(new FlexStartTaskNodeConnection("Connection to start task node of " + flexModel.getLabel(), flexModel, startTaskNodesSet));

			return new Object[] { flexModel, setFlex };

		} else {
			// start node is not equal to 1
			context.log("There are more than 1 start task node in this heuristic model");
			return null;
		}
	}
}
