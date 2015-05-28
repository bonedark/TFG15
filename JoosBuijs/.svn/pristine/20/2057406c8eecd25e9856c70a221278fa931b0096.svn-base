package org.processmining.plugins.joosbuijs.blockminer.genetic;

import java.util.Collection;
import java.util.HashMap;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetFactory;
import org.processmining.plugins.joosbuijs.blockminer.genetic.FunctionNode.FUNCTIONTYPE;

/**
 * Converts the given (sub)tree to a Petri net
 * 
 * @author jbuijs
 * 
 */
public class TreeToPNConvertor {

	//Contains a mapping from a deleted place to the correct place that should be used
	HashMap<Place, Place> deletedPlaces = new HashMap<Place, Place>();

	/**
	 * Builds a Petrinet from the given (root) node of a binary tree
	 * 
	 * @param root
	 * @return
	 */
	public Petrinet buildPetrinet(Node root) {
		//Initialize a new PN
		Petrinet net = PetrinetFactory.newPetrinet("Block Structured Petrinet");

		//Request the part in between
		Pair<PetrinetNode, PetrinetNode> addedNodes = buildPetrinet(net, root);

		//Make sure the petrinet ends with a single source and sink place
		PetrinetNode firstNode = addedNodes.getFirst();
		if (firstNode instanceof Transition) {
			//Add source and sink places
			Place source = net.addPlace("Source");
			Transition firstTransition = (Transition) firstNode;
			net.addArc(source, firstTransition);
		} else {
			//Make sure it is called 'Source'
			Place place = (Place) firstNode;
			place.getAttributeMap().put(AttributeMap.LABEL, "Source");
		}

		PetrinetNode lastNode = addedNodes.getSecond();
		if (lastNode.getClass() == Transition.class) {
			//Add source and sink places
			Place sink = net.addPlace("Sink");
			Transition firstTransition = (Transition) lastNode;
			net.addArc(firstTransition, sink);
		} else {
			//Make sure it is called 'Source'
			Place place = (Place) lastNode;
			place.getAttributeMap().put(AttributeMap.LABEL, "Sink");
		}

		return net;
	}

	/**
	 * Iteratively extends on the petri net added constructs and connection them
	 * to the previous first and last nodes
	 * 
	 * @param net
	 * @return
	 */
	private Pair<PetrinetNode, PetrinetNode> buildPetrinet(Petrinet net, Node treeNode) {
		/*
		 * If we found a leaf node then add a transition with that name and
		 * return that transition as the first and last node to connect to
		 */
		if (treeNode.countNodes() == 1) {
			EventClassNode eventClassNode = (EventClassNode) treeNode;
			Transition trans = net.addTransition(eventClassNode.getEventClass().toString());
			return new Pair<PetrinetNode, PetrinetNode>(trans, trans);
		} else {
			//Else, we have a node indicating a relationship so handle those correctly

			//But first we need to ask for the left and right parts to be added to the petri net
			Pair<PetrinetNode, PetrinetNode> leftPart = buildPetrinet(net, treeNode.getChild(0));
			Pair<PetrinetNode, PetrinetNode> rightPart = buildPetrinet(net, treeNode.getChild(1));

			//Now handle the functions correctly
			switch (treeNode.getFunction()) {
				case SEQ :
					//Sequence is easy, just connect the left and right parts...
					connectNodes(leftPart.getSecond(), rightPart.getFirst(), net);
					//And return the first and last nodes of the section we added
					return new Pair<PetrinetNode, PetrinetNode>(leftPart.getFirst(), rightPart.getSecond());
				case XOR :
					//Ah, an XOR, now we need to add XOR-split and XOR-join >places<
					Place XORsplit = net.addPlace("XOR-split");
					Place XORjoin = net.addPlace("XOR-join");
					//Now connect the left and right parts in between the XOR places
					connectNodes(XORsplit, leftPart.getFirst(), net);
					connectNodes(XORsplit, rightPart.getFirst(), net);
					connectNodes(leftPart.getSecond(), XORjoin, net);
					connectNodes(rightPart.getSecond(), XORjoin, net);
					//And for this section, the XOR places are what we return
					return new Pair<PetrinetNode, PetrinetNode>(XORsplit, XORjoin);
				case AND :
					//An AND means adding two silent >transitions<
					Transition ANDsplit = net.addTransition("AND-split");
					Transition ANDjoin = net.addTransition("AND-join");
					//These transitions can work very silently
					ANDsplit.setInvisible(true);
					ANDjoin.setInvisible(true);
					//Now connect the left and right parts in between the AND transitions
					connectNodes(ANDsplit, leftPart.getFirst(), net);
					connectNodes(ANDsplit, rightPart.getFirst(), net);
					connectNodes(leftPart.getSecond(), ANDjoin, net);
					connectNodes(rightPart.getSecond(), ANDjoin, net);
					//And for this section, the AND transitions are what we return
					return new Pair<PetrinetNode, PetrinetNode>(ANDsplit, ANDjoin);
				default :
					return null; //We should never end up here but we should always return something
			}
		}
	}

	/**
	 * Connects two given nodes in the petrinet observing the place-transition
	 * alternating requirement (merges places).
	 * 
	 * @param leftNode
	 * @param rightNode
	 * @param net
	 */
	//Pair<PetrinetNode, PetrinetNode>
	private void connectNodes(PetrinetNode leftNode, PetrinetNode rightNode, Petrinet net) {
		//First translate possibly deleted places with the merged place
		leftNode = replaceDeletedPlace(leftNode);
		rightNode = replaceDeletedPlace(rightNode);

		//Now we need to connect those parts
		//There are four options (combinations of place and transition)
		if (leftNode instanceof Place) {
			//If the left part ends with a place and the right one too, then >merge< them...
			if (rightNode instanceof Place) {
				//instantiate the last place of the left part
				Place leftPlace = (Place) leftNode;
				Place rightPlace = (Place) rightNode;
				//get the edges going out of the place of the right part
				Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outEdges = net
						.getOutEdges(rightPlace);
				//Now create new arcs from the left place to the t in the right part
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : outEdges) {
					Transition targetTrans = (Transition) edge.getTarget();
					net.addArc(leftPlace, targetTrans);
				}
				//We should also get the edges coming into the place of the right part
				Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inEdges = net
						.getInEdges(rightPlace);
				//Now create new arcs from the t in the right part to the left place
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : inEdges) {
					Transition sourceTrans = (Transition) edge.getSource();
					net.addArc(sourceTrans, leftPlace);
				}
				//And of course remember to remove the first place in the right part
				deletedPlaces.put(rightPlace, leftPlace);
				//rightPlace = leftPlace;
				net.removePlace(rightPlace);
			} else {
				//Otherwise connect p-t without merging
				Place leftPlace = (Place) leftNode;
				Transition rightTrans = (Transition) rightNode;
				net.addArc(leftPlace, rightTrans);
			}
		} else {//the left part ends with a transition
			if (rightNode instanceof Transition) {
				//If the right part also ends with a transition then add a place in between and don't merge!
				Place intermediatePlace = net.addPlace("");
				Transition leftTrans = (Transition) leftNode;
				Transition rightTrans = (Transition) rightNode;
				net.addArc(leftTrans, intermediatePlace);
				net.addArc(intermediatePlace, rightTrans);
			} else {
				//Otherwise connect t-p
				Transition leftTrans = (Transition) leftNode;
				Place rightPlace = (Place) rightNode;
				net.addArc(leftTrans, rightPlace);
			}
		}
		//return returnPair;
	}

	/**
	 * Checks if the provided node is in our list of deleted nodes. If so it
	 * returns the replacing node, otherwise it returns the original node
	 * 
	 * @param node
	 * @return
	 */
	private PetrinetNode replaceDeletedPlace(PetrinetNode node) {
		if (deletedPlaces.containsKey(node))
			return replaceDeletedPlace(deletedPlaces.get(node));
		else
			return node;
	}

	@Plugin(
			name = "Test block to PN convertor",
				parameterLabels = { "Event log" },
				returnLabels = { "Petrinet" },
				returnTypes = { Petrinet.class },
				userAccessible = true,
				help = "Test block to PN convertor")
	@UITopiaVariant(
			uiLabel = "00JB Test block to PN convertor",
				affiliation = "Eindhoven University of Technology",
				author = "J.C.A.M.Buijs",
				email = "j.c.a.m.buijs@tue.nl",
				pack = "JoosBuijs")
	public Petrinet geneticBlockMiner(final PluginContext context, XLog eventlog) {
		XEventClassifier classifier = XLogInfoImpl.STANDARD_CLASSIFIER;
		XLogInfo logInfo = XLogInfoFactory.createLogInfo(eventlog, classifier);

		Node fnodeXOR112 = new FunctionNode(new EventClassNode(logInfo.getEventClasses().getByIndex(2)),
				new EventClassNode(logInfo.getEventClasses().getByIndex(3)), FUNCTIONTYPE.XOR);

		Node fnode1 = new FunctionNode(new EventClassNode(logInfo.getEventClasses().getByIndex(2)), fnodeXOR112,
				FUNCTIONTYPE.SEQ);

		//right half

		Node fnodeXOR221 = new FunctionNode(new EventClassNode(logInfo.getEventClasses().getByIndex(4)),
				new EventClassNode(logInfo.getEventClasses().getByIndex(4)), FUNCTIONTYPE.XOR);

		Node fnodeSEQ = new FunctionNode(fnodeXOR221, new EventClassNode(logInfo.getEventClasses().getByIndex(4)),
				FUNCTIONTYPE.SEQ);

		Node fnodeAND122 = new FunctionNode(fnodeSEQ, new EventClassNode(logInfo.getEventClasses().getByIndex(3)),
				FUNCTIONTYPE.AND);

		Node fnode2 = new FunctionNode(new EventClassNode(logInfo.getEventClasses().getByIndex(3)), fnodeAND122,
				FUNCTIONTYPE.AND);

		Node root = new FunctionNode(fnode1, fnode2, FUNCTIONTYPE.AND);

		return buildPetrinet(root);
	}

}
