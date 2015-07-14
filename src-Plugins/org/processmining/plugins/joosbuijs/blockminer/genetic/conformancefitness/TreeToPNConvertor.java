package org.processmining.plugins.joosbuijs.blockminer.genetic.conformancefitness;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetFactory;
import org.processmining.plugins.joosbuijs.blockminer.genetic.conformancefitness.FunctionNode.FUNCTIONTYPE;

/**
 * Converts the given (sub)tree to a Petri net
 * 
 * @author jbuijs
 * 
 */
public class TreeToPNConvertor {
	//Contains a mapping from a deleted place to the correct place that should be used
	HashMap<Place, Place> deletedPlaces = new HashMap<Place, Place>();
	//FIXME counter should not be needed (is the map above actually required at all?)
	int replaceDeletedPlaceCounter = 0;

	/**
	 * Builds a Petrinet from the root node of the given tree
	 * 
	 * @param tree
	 * @return
	 */
	public Petrinet buildPetrinet(Tree tree) {
		return buildPetrinet(tree.getRoot());
	}
	
	/**
	 * Builds a Petrinet from the given (root) node of a tree
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
			Transition lasstTransition = (Transition) lastNode;
			net.addArc(lasstTransition, sink);
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
		if (treeNode instanceof EventClassNode) {
			EventClassNode eventClassNode = (EventClassNode) treeNode;
			Transition trans = net.addTransition(eventClassNode.getEventClass().toString());
			return new Pair<PetrinetNode, PetrinetNode>(trans, trans);
		} else {
			//Else, we have a node indicating a relationship so handle those correctly
			FunctionNode fnode = (FunctionNode) treeNode;

			//First we need to ask for parts to be added to the petri net (e.g. the children of this node
			LinkedList<Pair<PetrinetNode, PetrinetNode>> parts = new LinkedList<Pair<PetrinetNode, PetrinetNode>>();
			for (int i = 0; i < fnode.countChildren(); i++) {
				//Add the parts of the petri net this block will consist of
				parts.add(buildPetrinet(net, fnode.getChild(i)));
			}

			//Before we really do anything check for sanity!

			//We could get an empty tree if there are no children
			if (fnode.countChildren() == 0) {
				//return a ~empty part
				Place place = net.addPlace("");
				return new Pair<PetrinetNode, PetrinetNode>(place, place);
			}

			//We should handle the case of a single child where the operator does not really matter
			if (fnode.countChildren() == 1) {
				//Then the first part is the thing we should return
				return parts.get(0);
			}

			//Now handle the functions correctly to connect the parts
			//If there are any parts to handle...
			switch (fnode.getFunction()) {
				case SEQ :
					//Sequence is easy, just connect the parts in the order they come in...
					//For a sequence, we start with the first part
					Pair<PetrinetNode, PetrinetNode> mainPart = parts.get(0);

					//And then keep connecting all following parts
					for (int i = 1; i < parts.size(); i++) {
						Pair<PetrinetNode, PetrinetNode> part = parts.get(i);
						//So connect the last node of the main part to the first node of the new one
						connectNodes(mainPart.getSecond(), part.getFirst(), net);
						//Now update the main part so the following parts are connected to the end
						mainPart = new Pair<PetrinetNode, PetrinetNode>(mainPart.getFirst(), part.getSecond());
					}

					//And return the first and last nodes of the section we added
					return mainPart;
				case XOR :
					//Ah, an XOR, now we need to add XOR-split and XOR-join >places<
					Place XORsplit = net.addPlace("XOR-split");
					Place XORjoin = net.addPlace("XOR-join");

					//Now connect all the parts in between the XOR places
					for (Pair<PetrinetNode, PetrinetNode> part : parts) {
						connectNodes(XORsplit, part.getFirst(), net);
						connectNodes(part.getSecond(), XORjoin, net);
					}

					//And for this section, the XOR places are what we return
					return new Pair<PetrinetNode, PetrinetNode>(XORsplit, XORjoin);
				case AND :
					//An AND means adding two silent >transitions<
					Transition ANDsplit = net.addTransition("AND-split");
					Transition ANDjoin = net.addTransition("AND-join");
					//These transitions can work very silently
					ANDsplit.setInvisible(true);
					ANDjoin.setInvisible(true);

					//Now connect all the parts in between the XOR places
					for (Pair<PetrinetNode, PetrinetNode> part : parts) {
						connectNodes(ANDsplit, part.getFirst(), net);
						connectNodes(part.getSecond(), ANDjoin, net);
					}

					//And for this section, the AND transitions are what we return
					return new Pair<PetrinetNode, PetrinetNode>(ANDsplit, ANDjoin);
				default :
					return null; //We should never end up here but we should always return something
			}//switch
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
		replaceDeletedPlaceCounter = 0;
		leftNode = replaceDeletedPlace(leftNode);
		replaceDeletedPlaceCounter = 0;
		rightNode = replaceDeletedPlace(rightNode);

		//Now we need to connect those parts
		//There are four options (combinations of place and transition)
		if (leftNode instanceof Place) {
			//If the left part ends with a place and the right one too, then >merge< them...
			if (rightNode instanceof Place) {
				//instantiate the places
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
				//First, remember that the rightplace is replaced by the leftplace
				if (!rightPlace.equals(leftPlace))
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
				//If the right part also ends with a transition then add a place in between and >don't merge<!
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
		if (deletedPlaces.containsKey(node)) {
			//FIXME self-reference possible
			replaceDeletedPlaceCounter++;
			if (replaceDeletedPlaceCounter > 100) {
				//				System.out.println("Replace Deleted Place LOOP");
			}
			return replaceDeletedPlace(deletedPlaces.get(node));
		} else
			return node;
	}

	/*-* /
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
	/**/
	public Petrinet testConvertor(final PluginContext context, XLog eventlog) {
		XEventClassifier classifier = XLogInfoImpl.STANDARD_CLASSIFIER;
		XLogInfo logInfo = XLogInfoFactory.createLogInfo(eventlog, classifier);

		/*-
		 * Builds the tree
		 * AND(
				SEQ(
					B+complete
					XOR(
						B+complete
						B+start
					)
				)
				AND(
					B+start
					AND(
						SEQ(
							XOR(
								C+complete
								C+complete
							)
							C+complete
						)
						B+start
					)
				)
			)
		 */

		LinkedList<Node> fnodeXOR112Children = new LinkedList<Node>();
		fnodeXOR112Children.add(new EventClassNode(logInfo.getEventClasses().getByIndex(2)));
		fnodeXOR112Children.add(new EventClassNode(logInfo.getEventClasses().getByIndex(3)));
		Node fnodeXOR112 = new FunctionNode(fnodeXOR112Children, FUNCTIONTYPE.XOR);

		LinkedList<Node> fnode1Children = new LinkedList<Node>();
		fnode1Children.add(new EventClassNode(logInfo.getEventClasses().getByIndex(2)));
		fnode1Children.add(fnodeXOR112);
		Node fnode1 = new FunctionNode(fnode1Children, FUNCTIONTYPE.SEQ);

		//right half

		LinkedList<Node> fnodeXOR221Children = new LinkedList<Node>();
		fnodeXOR221Children.add(new EventClassNode(logInfo.getEventClasses().getByIndex(4)));
		fnodeXOR221Children.add(new EventClassNode(logInfo.getEventClasses().getByIndex(4)));
		Node fnodeXOR221 = new FunctionNode(fnodeXOR221Children, FUNCTIONTYPE.XOR);

		LinkedList<Node> fnodeSEQChildren = new LinkedList<Node>();
		fnodeSEQChildren.add(fnodeXOR221);
		fnodeSEQChildren.add(new EventClassNode(logInfo.getEventClasses().getByIndex(4)));
		Node fnodeSEQ = new FunctionNode(fnodeSEQChildren, FUNCTIONTYPE.SEQ);

		LinkedList<Node> fnodeAND122Children = new LinkedList<Node>();
		fnodeAND122Children.add(fnodeSEQ);
		fnodeAND122Children.add(new EventClassNode(logInfo.getEventClasses().getByIndex(3)));
		Node fnodeAND122 = new FunctionNode(fnodeAND122Children, FUNCTIONTYPE.AND);

		LinkedList<Node> fnode2Children = new LinkedList<Node>();
		fnode2Children.add(new EventClassNode(logInfo.getEventClasses().getByIndex(3)));
		fnode2Children.add(fnodeAND122);
		Node fnode2 = new FunctionNode(fnode2Children, FUNCTIONTYPE.AND);

		LinkedList<Node> rootChildren = new LinkedList<Node>();
		rootChildren.add(fnode1);
		rootChildren.add(fnode2);
		Node root = new FunctionNode(rootChildren, FUNCTIONTYPE.AND);

		System.out.println(root.toString(0));

		return buildPetrinet(root);
	}
}
