/**
 * 
 */
package org.processmining.plugins.replayer.converter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.util.Pair;
import org.processmining.models.connections.flexiblemodel.FlexCancellationRegionConnection;
import org.processmining.models.connections.flexiblemodel.FlexCodecConnection;
import org.processmining.models.connections.flexiblemodel.FlexEndTaskNodeConnection;
import org.processmining.models.connections.flexiblemodel.FlexSpecialNodesConnection;
import org.processmining.models.connections.flexiblemodel.FlexStartTaskNodeConnection;
import org.processmining.models.connections.flexiblemodel.conversion.FlexOfYAWLConnection;
import org.processmining.models.flexiblemodel.CancellationRegion;
import org.processmining.models.flexiblemodel.EndTaskNodesSet;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.models.flexiblemodel.FlexFactory;
import org.processmining.models.flexiblemodel.FlexNode;
import org.processmining.models.flexiblemodel.SetFlex;
import org.processmining.models.flexiblemodel.StartTaskNodesSet;
import org.processmining.plugins.flex.replayer.performance.util.FlexSpecialNodes;
import org.processmining.plugins.flex.replayer.performance.util.YAWLNodeInstanceMapping;
import org.processmining.plugins.flex.replayer.util.FlexCodec;
import org.processmining.plugins.petrinet.replayer.util.CombinationGenerator;
import org.yawlfoundation.yawl.editor.elements.model.AtomicTask;
import org.yawlfoundation.yawl.editor.elements.model.CompositeTask;
import org.yawlfoundation.yawl.editor.elements.model.Condition;
import org.yawlfoundation.yawl.editor.elements.model.InputCondition;
import org.yawlfoundation.yawl.editor.elements.model.JoinDecorator;
import org.yawlfoundation.yawl.editor.elements.model.MultipleAtomicTask;
import org.yawlfoundation.yawl.editor.elements.model.MultipleCompositeTask;
import org.yawlfoundation.yawl.editor.elements.model.OutputCondition;
import org.yawlfoundation.yawl.editor.elements.model.SplitDecorator;
import org.yawlfoundation.yawl.editor.elements.model.VertexContainer;
import org.yawlfoundation.yawl.editor.elements.model.YAWLCell;
import org.yawlfoundation.yawl.editor.elements.model.YAWLFlowRelation;
import org.yawlfoundation.yawl.editor.elements.model.YAWLTask;
import org.yawlfoundation.yawl.editor.elements.model.YAWLVertex;
import org.yawlfoundation.yawl.editor.net.NetGraph;

/**
 * @author aadrians
 * 
 */
@Plugin(name = "Convert YAWL Model to Flexible model", parameterLabels = { "YAWL net" }, returnLabels = { "Flexible model",
		"Start task nodes set", "End task nodes set", "Cancelation region", "Node mapping", "Causal node codec", "Flexible model special nodes" }, returnTypes = {
		Flex.class, StartTaskNodesSet.class, EndTaskNodesSet.class, CancellationRegion.class,
		YAWLNodeInstanceMapping.class, FlexCodec.class, FlexSpecialNodes.class }, userAccessible = true)
public class FlexFromYAWL {
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "A. Adriansyah", email = "a.adriansyah@tue.nl", uiLabel = UITopiaVariant.USEPLUGIN, pack="Replayer")
	@PluginVariant(variantLabel = "Convert YAWL net to Flexible model", requiredParameterLabels = { 0 })
	public Object[] convertToFM(PluginContext context, NetGraph model) {
		StartTaskNodesSet startTaskNodesSet = new StartTaskNodesSet();
		EndTaskNodesSet endTaskNodesSet = new EndTaskNodesSet();
		CancellationRegion cancellationRegion = new CancellationRegion();
		Flex flex = FlexFactory.newFlex("Flexible model of " + model.getName());
		YAWLNodeInstanceMapping nodeInstanceMapping = new YAWLNodeInstanceMapping();

		// call main method to convert YAWL model to Flexible model
		FlexSpecialNodes specialNodes = new FlexSpecialNodes();
		FlexCodec codec = parseModel(model, flex, startTaskNodesSet, endTaskNodesSet, cancellationRegion,
				nodeInstanceMapping, null, specialNodes);

		// if unsupported construction is identified, notify it
		if (specialNodes.getCompositeTasks().size() > 0){
			context.log("Unsupported construction (composite task) is found. Plugin return empty result.");
			context.log("Please remove/unfold the following YAWL composite task nodes:");
			
			for (FlexNode node : specialNodes.getCompositeTasks()){
				context.log("- " + node.getLabel());
			}
			cancelAll(context);
			return null;
		}
		
		if (specialNodes.getMultipleCompositeTasks().size() > 0){
			context.log("Unsupported construction (composite task) is found. Plugin return empty result.");
			context.log("Please remove/unfold the following YAWL composite task nodes:");
			
			for (FlexNode node : specialNodes.getMultipleCompositeTasks()){
				context.log("- " + node.getLabel());
			}
			cancelAll(context);
			return null;
		}
		
		// create necessary connections
		context.addConnection(new FlexStartTaskNodeConnection("Connection to start task node of " + flex.getLabel(), flex,
				startTaskNodesSet));
		context.addConnection(new FlexEndTaskNodeConnection("Connection to end task node of " + flex.getLabel(), flex,
				endTaskNodesSet));
		context.addConnection(new FlexCancellationRegionConnection("Connection to cancellation region of " + flex.getLabel(), flex,
				cancellationRegion));
		context.addConnection(new FlexCodecConnection("Connection to codec of " + flex.getLabel(), flex, codec));

		context.addConnection(new FlexOfYAWLConnection("Connection to Flexible model of " + model.getName(), model, flex,
				nodeInstanceMapping));
		context.addConnection(new FlexSpecialNodesConnection("Connection to special nodes of " + model.getName(), flex, specialNodes));

		context.getFutureResult(0).setLabel("Flexible model of " + model.getName());
		context.getFutureResult(1).setLabel("Start task node set of " + flex.getLabel());
		context.getFutureResult(2).setLabel("End task node set of " + flex.getLabel());
		context.getFutureResult(3).setLabel("Cancellation region of " + flex.getLabel());
		context.getFutureResult(4).setLabel(
				"Mapping of encoded activity from " + flex.getLabel() + " to " + model.getName());
		context.getFutureResult(5).setLabel("Codec of " + flex.getLabel());

		return new Object[] { flex, startTaskNodesSet, endTaskNodesSet, cancellationRegion, nodeInstanceMapping, codec, specialNodes };
	}

	private void cancelAll(PluginContext context) {
		for (int i=0; i < 7; i++){
			context.getFutureResult(i).cancel(true);
		}
	}

	private FlexCodec parseModel(NetGraph model, Flex flex, StartTaskNodesSet startTaskNodesSet,
			EndTaskNodesSet endTaskNodesSet, CancellationRegion cancelationRegion,
			YAWLNodeInstanceMapping nodeInstanceMapping, FlexNode parentNode, FlexSpecialNodes specialNodes) {
		Object[] cells = model.getRoots();

		Set<YAWLTask> tasksWithCancellationSets = new HashSet<YAWLTask>();

		Set<FlexNode> multipleCompositeTasks = new HashSet<FlexNode>();
		Set<FlexNode> compositeTasks = new HashSet<FlexNode>();
		Set<FlexNode> multipleAtomicTasks = new HashSet<FlexNode>();

		Map<YAWLVertex, Set<YAWLVertex>> flows = new HashMap<YAWLVertex, Set<YAWLVertex>>();
		Map<YAWLVertex, Set<YAWLVertex>> invertflows = new HashMap<YAWLVertex, Set<YAWLVertex>>();

		Map<YAWLVertex, FlexNode> nodeMapping = new HashMap<YAWLVertex, FlexNode>();

		for (int i = 0; i < cells.length; i++) {
			// update if the content is Vertex
			if (cells[i] instanceof VertexContainer) {
				cells[i] = ((VertexContainer) cells[i]).getVertex();
			}

			// input condition
			if (cells[i] instanceof InputCondition && (parentNode == null)) {
				InputCondition inputCondition = (InputCondition) cells[i];

				FlexNode node = flex.addNode(inputCondition.getEngineId());
				node.setInvisible(true);

				SetFlex setFlex = new SetFlex();
				setFlex.add(node);
				startTaskNodesSet.add(setFlex); // only one start node

				nodeMapping.put(inputCondition, node);
				specialNodes.addLateBindingNodes(node);

			} else

			// output condition
			if (cells[i] instanceof OutputCondition && (parentNode == null)) {
				OutputCondition outputCondition = (OutputCondition) cells[i];

				FlexNode node = flex.addNode(outputCondition.getEngineId());
				node.setInvisible(true);

				SetFlex setFlex = new SetFlex();
				setFlex.add(node);
				endTaskNodesSet.add(setFlex); // only one end node

				nodeMapping.put(outputCondition, node);
			} else

			// condition (place) 
			if (cells[i] instanceof Condition) {
				Condition condition = (Condition) cells[i];

				FlexNode node = flex.addNode(condition.getEngineId());
				node.setInvisible(true);

				nodeMapping.put(condition, node);
				specialNodes.addLateBindingNodes(node);

			} else

			// atomic task (with/without cancellation sets)
			if (cells[i] instanceof AtomicTask) {
				AtomicTask task = (AtomicTask) cells[i];

				// create multiple nodes to represent lifecycle
				FlexNode node = flex.addNode(task.getEngineId());
				node.setInvisible(false);

				nodeMapping.put(task, node);

				if (task.getCancellationSet().getSetMembers().size() > 0) {
					tasksWithCancellationSets.add(task);
				}
			} else

			// multiple atomic task
			if (cells[i] instanceof MultipleAtomicTask) {
				MultipleAtomicTask multipleAtomicTask = (MultipleAtomicTask) cells[i];

				// create multiple nodes to represent lifecycle
				FlexNode node = flex.addNode(multipleAtomicTask.getEngineId());
				node.setInvisible(false);

				nodeMapping.put(multipleAtomicTask, node);

				// add multiple execution
				multipleAtomicTasks.add(node);

				if (multipleAtomicTask.getCancellationSet().getSetMembers().size() > 0) {
					tasksWithCancellationSets.add(multipleAtomicTask);
				}
			} else

			/**
			 * AA: composite tasks are not supported for now
			 */
			// composite tasks (need to handle subgraph)
			if (cells[i] instanceof CompositeTask) {
				CompositeTask compositeTask = (CompositeTask) cells[i];

				FlexNode node = flex.addNode(compositeTask.getEngineId());

				nodeMapping.put(compositeTask, node);

				compositeTasks.add(node);

				if (compositeTask.getCancellationSet().getSetMembers().size() > 0) {
					tasksWithCancellationSets.add(compositeTask);
				}
			} else

			/**
			 * AA: similar to composite tasks. Multiple composites are not supported 
			 */
			// multiple composite task	
			if (cells[i] instanceof MultipleCompositeTask) {
				MultipleCompositeTask multipleCompositeTask = (MultipleCompositeTask) cells[i];

				FlexNode node = flex.addNode(multipleCompositeTask.getEngineId());

				nodeMapping.put(multipleCompositeTask, node);

				multipleCompositeTasks.add(node);

				if (multipleCompositeTask.getCancellationSet().getSetMembers().size() > 0) {
					tasksWithCancellationSets.add(multipleCompositeTask);
				}
			} else

			// instance of FLOW
			if (cells[i] instanceof YAWLFlowRelation) {
				YAWLFlowRelation relation = (YAWLFlowRelation) cells[i];

				Set<YAWLVertex> setVertex = flows.get(relation.getSourceVertex());
				if (setVertex != null) {
					setVertex.add(relation.getTargetVertex());
				} else {
					setVertex = new HashSet<YAWLVertex>();
					setVertex.add(relation.getTargetVertex());
					flows.put(relation.getSourceVertex(), setVertex);
				}

				Set<YAWLVertex> setInVertex = invertflows.get(relation.getTargetVertex());
				if (setInVertex != null) {
					setInVertex.add(relation.getSourceVertex());
				} else {
					setInVertex = new HashSet<YAWLVertex>();
					setInVertex.add(relation.getSourceVertex());
					invertflows.put(relation.getTargetVertex(), setInVertex);
				}
			}
		}

		// build high level Flexible model by identifying semantics
		for (YAWLVertex vertex : flows.keySet()) {
			if (!(vertex instanceof YAWLTask)) {
				for (YAWLVertex outVertex : flows.get(vertex)) {
					SetFlex setFlex = new SetFlex();
					setFlex.add(nodeMapping.get(outVertex));
					nodeMapping.get(vertex).addOutputNodes(setFlex);
					flex.addArc(nodeMapping.get(vertex), nodeMapping.get(outVertex));
				}
			} else {
				// output semantics depends
				SplitDecorator splitDecorator = null;
				if (vertex instanceof AtomicTask) {
					splitDecorator = ((AtomicTask) vertex).getSplitDecorator();
				} else if (vertex instanceof MultipleAtomicTask) {
					splitDecorator = ((MultipleAtomicTask) vertex).getSplitDecorator();
				}

				if (splitDecorator == null || splitDecorator.getType() == SplitDecorator.AND_TYPE) {
					SetFlex setFlex = new SetFlex();
					for (YAWLVertex outVertex : flows.get(vertex)) {
						if (!(outVertex instanceof YAWLTask)) {
							setFlex.add(nodeMapping.get(outVertex));
							flex.addArc(nodeMapping.get(vertex), nodeMapping.get(outVertex));
						} else {
							setFlex.add(nodeMapping.get(outVertex));
							flex.addArc(nodeMapping.get(vertex), nodeMapping.get(outVertex));
						}
					}
					nodeMapping.get(vertex).addOutputNodes(setFlex);
				} else if (splitDecorator.getType() == SplitDecorator.OR_TYPE) {
					Set<YAWLVertex> flowToVertex = flows.get(vertex);
					YAWLVertex[] arrVertex = flowToVertex.toArray(new YAWLVertex[flowToVertex.size()]);

					for (int i = 1; i <= arrVertex.length; i++) {
						CombinationGenerator combGen = new CombinationGenerator(arrVertex.length, i);
						while (combGen.hasMore()) {
							SetFlex setFlex = new SetFlex();
							for (int j : combGen.getNext()) {
								if (!(arrVertex[j] instanceof YAWLTask)) {
									setFlex.add(nodeMapping.get(arrVertex[j]));
								} else {
									setFlex.add(nodeMapping.get(arrVertex[j]));
								}
							}
							nodeMapping.get(vertex).addOutputNodes(setFlex);
						}
					}

					for (int i = 0; i < arrVertex.length; i++) {
						if (!(arrVertex[i] instanceof YAWLTask)) {
							flex.addArc(nodeMapping.get(vertex), nodeMapping.get(arrVertex[i]));
						} else {
							flex.addArc(nodeMapping.get(vertex), nodeMapping.get(arrVertex[i]));
						}
					}
				} else if (splitDecorator.getType() == SplitDecorator.XOR_TYPE) {
					for (YAWLVertex outVertex : flows.get(vertex)) {
						SetFlex setFlex = new SetFlex();
						if (!(outVertex instanceof YAWLTask)) {
							setFlex.add(nodeMapping.get(outVertex));
							nodeMapping.get(vertex).addOutputNodes(setFlex);
							flex.addArc(nodeMapping.get(vertex), nodeMapping.get(outVertex));
						} else {
							setFlex.add(nodeMapping.get(outVertex));
							nodeMapping.get(vertex).addOutputNodes(setFlex);
							flex.addArc(nodeMapping.get(vertex), nodeMapping.get(outVertex));
						}
					}
				}
			}
		}

		for (YAWLVertex vertex : invertflows.keySet()) {
			if (!(vertex instanceof YAWLTask)) {
				for (YAWLVertex inVertex : invertflows.get(vertex)) {
					SetFlex setFlex = new SetFlex();
					setFlex.add(nodeMapping.get(inVertex));
					nodeMapping.get(vertex).addInputNodes(setFlex);
					flex.addArc(nodeMapping.get(inVertex), nodeMapping.get(vertex));
				}
			} else {
				// output semantics depends
				JoinDecorator joinDecorator = null;
				if (vertex instanceof AtomicTask) {
					joinDecorator = ((AtomicTask) vertex).getJoinDecorator();
				} else if (vertex instanceof MultipleAtomicTask) {
					joinDecorator = ((MultipleAtomicTask) vertex).getJoinDecorator();
				}

				if (joinDecorator == null || joinDecorator.getType() == JoinDecorator.AND_TYPE) {
					SetFlex setFlex = new SetFlex();
					for (YAWLVertex inVertex : invertflows.get(vertex)) {
						if (!(inVertex instanceof YAWLTask)) {
							setFlex.add(nodeMapping.get(inVertex));
							flex.addArc(nodeMapping.get(inVertex), nodeMapping.get(vertex));
						} else {
							setFlex.add(nodeMapping.get(inVertex));
							flex.addArc(nodeMapping.get(inVertex), nodeMapping.get(vertex));
						}
					}
					nodeMapping.get(vertex).addInputNodes(setFlex);
				} else if (joinDecorator.getType() == JoinDecorator.OR_TYPE) {
					Set<YAWLVertex> invertFlowsVertex = invertflows.get(vertex);
					YAWLVertex[] arrVertex = invertflows.get(vertex).toArray(new YAWLVertex[invertFlowsVertex.size()]);

					for (int i = 1; i <= arrVertex.length; i++) {
						CombinationGenerator combGen = new CombinationGenerator(arrVertex.length, i);
						while (combGen.hasMore()) {
							SetFlex setFlex = new SetFlex();
							for (int j : combGen.getNext()) {
								if (!(arrVertex[j] instanceof YAWLTask)) {
									setFlex.add(nodeMapping.get(arrVertex[j]));
								} else {
									setFlex.add(nodeMapping.get(arrVertex[j]));
								}
							}
							nodeMapping.get(vertex).addInputNodes(setFlex);
						}
					}

					for (int i = 0; i < arrVertex.length; i++) {
						if (!(arrVertex[i] instanceof YAWLTask)) {
							flex.addArc(nodeMapping.get(arrVertex[i]), nodeMapping.get(vertex));
						} else {
							flex.addArc(nodeMapping.get(arrVertex[i]), nodeMapping.get(vertex));
						}
					}
				} else if (joinDecorator.getType() == SplitDecorator.XOR_TYPE) {
					for (YAWLVertex inVertex : invertflows.get(vertex)) {
						SetFlex setFlex = new SetFlex();
						if (!(inVertex instanceof YAWLTask)) {
							setFlex.add(nodeMapping.get(inVertex));
							nodeMapping.get(vertex).addInputNodes(setFlex);
							flex.addArc(nodeMapping.get(inVertex), nodeMapping.get(vertex));
						} else {
							setFlex.add(nodeMapping.get(inVertex));
							nodeMapping.get(vertex).addInputNodes(setFlex);
							flex.addArc(nodeMapping.get(inVertex), nodeMapping.get(vertex));
						}
					}
				}
			}
		}
		
		// update arc for all atomic multiple execution
		for (FlexNode node : multipleAtomicTasks){
			SetFlex setFlex = new SetFlex();
			setFlex.add(node);
			node.addInputNodes(setFlex);
			node.addOutputNodes(setFlex);
			
			flex.addArc(node, node);
		}

		// finalize all nodes labeling
		for (FlexNode node : flex.getNodes()) {
			node.commitUpdates();
		}

		// create mapping from flexcodec
		FlexCodec codec = new FlexCodec(flex);

		for (YAWLVertex vertex : nodeMapping.keySet()) {
			nodeInstanceMapping.put(codec.encode(nodeMapping.get(vertex)), vertex);
		}

		// TODO: start refinement of composite tasks

		// handle cancellation region
		for (YAWLTask task : tasksWithCancellationSets) {
			Set<Pair<FlexNode, FlexNode>> remObligations = new HashSet<Pair<FlexNode, FlexNode>>();

			for (YAWLCell cell : task.getCancellationSet().getSetMembers()) {
				if (cell instanceof YAWLTask) {
					cancelationRegion.addNodeCancellationFor(nodeMapping.get(task), nodeMapping.get(cell));
				} else if (cell instanceof Condition) {
					cancelationRegion.addNodeCancellationFor(nodeMapping.get(task), nodeMapping.get(cell));
				} else if (cell instanceof YAWLFlowRelation) {
					YAWLFlowRelation flowRelation = (YAWLFlowRelation) cell;
					Pair<FlexNode, FlexNode> pair = new Pair<FlexNode, FlexNode>(nodeMapping.get(flowRelation.getSourceVertex()), nodeMapping.get(flowRelation.getTargetVertex()));
					remObligations.add(pair);
				}
			}
			cancelationRegion.put(nodeMapping.get(task), remObligations);
		}

		// store another info
		specialNodes.setCompositeTasks(compositeTasks);
		specialNodes.setMultipleCompositeTasks(multipleCompositeTasks);
		specialNodes.setMultipleAtomicTasks(multipleAtomicTasks);

		return codec;
	}
}
