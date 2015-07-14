package org.processmining.plugins.joosbuijs.processtrees.geneticminer;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
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
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.boudewijn.tree.Node;
import org.processmining.plugins.boudewijn.tree.Node.Type;
import org.processmining.plugins.boudewijn.tree.Tree;
import org.processmining.plugins.petrinet.reduction.Murata;

/**
 * Converts the given (sub)tree to a Petri net
 * 
 * @author jbuijs
 * 
 */
public class TreeToPNConvertor {
	//Contains a mapping from a deleted place to the correct place that should be used
	HashMap<Place, Place> deletedPlaces = new HashMap<Place, Place>();
	int replaceDeletedPlaceCounter = 0;

	//TODO use hierachical/groups with in/out places outside the group 

	/**
	 * Builds a Petrinet from the given (root) node of a binary tree
	 * 
	 * @param root
	 * @return
	 */
	public Petrinet buildPetrinet(Node root) {
		//Initialize a new PN
		Petrinet net = PetrinetFactory.newPetrinet("Block Structured Petrinet");

		//Request the part in between (e.g. everything)
		Pair<PetrinetNode, PetrinetNode> addedNodes = buildPetrinet(net, root);

		//Make sure the petrinet ends with a single source and sink place
		/*
		 * Please note the special case where the root is a loop, there we add
		 * aditional tau transitions to ensure the resulting PN to be a workflow
		 * net
		 */
		PetrinetNode firstNode = addedNodes.getFirst();
		if (firstNode instanceof Transition) {
			//Add source and sink places
			Place source = net.addPlace("Source");
			Transition firstTransition = (Transition) firstNode;
			net.addArc(source, firstTransition);
		} else if (net.getInEdges(firstNode).size() > 0) {
			//If the source place has incoming edges
			//Create a new source place
			Place sourcePlace = net.addPlace("Source");

			//And add a Tau trans. from source p. to input place of loop
			//Make sure it is called 'Source'
			Place loopPlace = (Place) firstNode;

			Transition startTrans = net.addTransition("Tau Start Net");
			startTrans.setInvisible(true);

			//Connect source to tau to loop-in
			net.addArc(sourcePlace, startTrans);
			net.addArc(startTrans, loopPlace);

		} else {//not LOOP root
			//Make sure the first place is called 'Source'
			Place place = (Place) firstNode;
			place.getAttributeMap().put(AttributeMap.LABEL, "Source");
		}

		PetrinetNode lastNode = addedNodes.getSecond();
		//If the last node is not a place OR if the place has outgoing arcs
		if (lastNode.getClass() == Transition.class) {
			//Add source and sink places
			Place sink = net.addPlace("Sink");
			Transition lasstTransition = (Transition) lastNode;
			net.addArc(lasstTransition, sink);
		} else if (net.getOutEdges(lastNode).size() > 0) {
			//Create a new source place
			Place sinkPlace = net.addPlace("Sink");

			//And add a Tau trans. from source p. to input place of loop
			//Make sure it is called 'Source'
			Place loopPlace = (Place) lastNode;

			Transition endTrans = net.addTransition("Tau End Net");
			endTrans.setInvisible(true);

			/*-
			Set<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> edges = net.getEdges();
			net.getInEdges(loopPlace);
			net.getOutEdges(loopPlace);*/

			//Connect source to tau to loop-in
			net.addArc(loopPlace, endTrans);
			net.addArc(endTrans, sinkPlace);
		} else {
			//Its a place with no outgoing edges
			//Make sure it is called 'Source'
			Place place = (Place) lastNode;
			place.getAttributeMap().put(AttributeMap.LABEL, "Sink");
		}

		//Put a token in the source place
		//TODO put a token in the source place

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
		if (treeNode.isLeaf()) {
			Transition trans = net.addTransition(treeNode.getClazz().toString());
			return new Pair<PetrinetNode, PetrinetNode>(trans, trans);
		} else {
			//First we need to ask for parts to be added to the petri net (e.g. the children of this node)
			LinkedList<Pair<PetrinetNode, PetrinetNode>> parts = new LinkedList<Pair<PetrinetNode, PetrinetNode>>();
			parts.add(buildPetrinet(net, treeNode.getChild(0)));
			//Only for non-loops ask the 2nd child
			if (treeNode.getType() != Type.LOOP)
				parts.add(buildPetrinet(net, treeNode.getChild(1)));
			/*-
			for (int i = 0; i < treeNode.countChildren(); i++) {
				//Add the parts of the petri net this block will consist of
				parts.add(buildPetrinet(net, treeNode.getChild(i)));
			}/**/

			//Before we really do anything check for sanity!

			//We could get an empty tree if there are no children
			if (treeNode.countChildren() == 0) {
				//return a ~empty part, places will be merged or are required anyway
				Place place = net.addPlace("");
				return new Pair<PetrinetNode, PetrinetNode>(place, place);
			}

			//We should handle the case of a single child where the operator does not really matter
			if (treeNode.countChildren() == 1) {
				//Then the first (and only) part is the thing we should return
				return parts.get(0);
			}

			//Now handle the functions correctly to connect the parts (there are at least 2, see above)
			switch (treeNode.getType()) {
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

					//Now connect all the parts in between the AND places
					for (Pair<PetrinetNode, PetrinetNode> part : parts) {
						connectNodes(ANDsplit, part.getFirst(), net);
						connectNodes(part.getSecond(), ANDjoin, net);
					}

					//And for this section, the AND transitions are what we return
					return new Pair<PetrinetNode, PetrinetNode>(ANDsplit, ANDjoin);
				case LOOP :
					/*
					 * A loop is a block with an input and output place with the
					 * left child of the loop operator in between. There is a
					 * tau transition that goes back from the output place to
					 * the input place to enable the containing block again.
					 */
					//So first, create the two places
					Place leftPlace = net.addPlace("Loop-in");
					Place rightPlace = net.addPlace("Loop-out");
					//And create a silent transition inbetween them
					Transition loopTau = net.addTransition("Loop-tau");
					loopTau.setInvisible(true); //Its a tau, you don't see those
					net.addArc(rightPlace, loopTau);
					net.addArc(loopTau, leftPlace);

					//Now connect the block created for the left child to the left Place
					connectNodes(leftPlace, parts.get(0).getFirst(), net);
					connectNodes(parts.get(0).getSecond(), rightPlace, net);

					//But these could have been merged into other places (especially the right one). So, ask for the correct one
					if (deletedPlaces.containsKey(leftPlace))
						leftPlace = deletedPlaces.get(leftPlace);
					if (deletedPlaces.containsKey(rightPlace))
						rightPlace = deletedPlaces.get(rightPlace);

					//And now return the left and right places of the loop block

					return new Pair<PetrinetNode, PetrinetNode>(leftPlace, rightPlace);
				case OR :
					/*-
					 * The OR is even more complex to translate into a Petri Net.
					 * We choose to create 3 splitting silent transitions in the beginning.
					 * The OR-join is a single silent transition.
					 * Each of the 3 OR-split transitions has 2 outgoing arcs. One enables both blocks.
					 * The other two enable one block and put a second token at the end of the skipped block.
					 * That way there are always 2 tokens in front of the OR-join, which in essence is an AND join.  
					 */
					//First, create the input and out places for the OR block
					Place ORin = net.addPlace("OR-in");
					Place ORout = net.addPlace("OR-out");
					//Then create the in and out places for the OR operator's children
					Place ORLeftIn = net.addPlace("In for left block");
					Place ORRightIn = net.addPlace("In for right block");
					Place ORLeftOut = net.addPlace("Out for left block");
					Place ORRightOut = net.addPlace("Out for right block");

					//Then, create our silent transitions
					Transition ORSplitLeft = net.addTransition("OR-split (Left branch)");
					Transition ORSplitRight = net.addTransition("OR-split (Right branch)");
					Transition ORSplitBoth = net.addTransition("OR-split (Both branches)");
					Transition ORJoin = net.addTransition("OR-Join");
					//Make them silent
					ORSplitLeft.setInvisible(true);
					ORSplitRight.setInvisible(true);
					ORSplitBoth.setInvisible(true);
					ORJoin.setInvisible(true);

					//Now connect these routing transitions
					//The left split goes to left in and right out
					net.addArc(ORin, ORSplitLeft);
					net.addArc(ORSplitLeft, ORLeftIn);
					net.addArc(ORSplitLeft, ORRightOut);
					//The right split...
					net.addArc(ORin, ORSplitRight);
					net.addArc(ORSplitRight, ORLeftOut); //NP
					net.addArc(ORSplitRight, ORRightIn);
					//The both split goes to both in's
					net.addArc(ORin, ORSplitBoth);
					net.addArc(ORSplitBoth, ORLeftIn);
					net.addArc(ORSplitBoth, ORRightIn);

					//And the join is an AND join
					net.addArc(ORLeftOut, ORJoin);
					net.addArc(ORRightOut, ORJoin);
					net.addArc(ORJoin, ORout);

					//Now connect the in and out places to the left/right children
					connectNodes(ORLeftIn, parts.get(0).getFirst(), net);
					connectNodes(ORRightIn, parts.get(1).getFirst(), net);
					connectNodes(parts.get(0).getSecond(), ORLeftOut, net);
					connectNodes(parts.get(1).getSecond(), ORRightOut, net);

					return new Pair<PetrinetNode, PetrinetNode>(ORin, ORout);
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
		//reset
		replaceDeletedPlaceCounter = 0;
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
				if (!rightPlace.equals(leftPlace))
					deletedPlaces.put(rightPlace, leftPlace);
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
		if (deletedPlaces.containsKey(node)) {
			//FIXME self-reference possible
			replaceDeletedPlaceCounter++;
			if (replaceDeletedPlaceCounter > 100) {
				System.out.println("Replace Deleted Place LOOP");
			}
			//Return the node it is replaced by
			return replaceDeletedPlace(deletedPlaces.get(node));
		} else
			return node;
	}

	/*-* /
	//TODO disabled plug-in annotation
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
	/** /
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
	 * /

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

		logger.info(root.toString(0));
		System.out.println(root.toString(0));

		return buildPetrinet(root);
	}/**/

	public Petrinet buildPetrinet(Tree tree) {
		return buildPetrinet(tree.getRoot());
	}

	/**
	 * Applies the murata rules directly on the provided petrinet
	 * 
	 * @param context
	 * @param pn
	 */
	public void applyMurata(PluginContext context, Petrinet pn) {
		/*-*/
		//We don't have an initial marking but we can create one
		Marking initMarking = new Marking();
		for (Place place : pn.getPlaces()) {
			if (pn.getInEdges(place).isEmpty()) {
				initMarking.add(place);
			}
		}

		Murata m = new Murata();
		try {
			m.run(context, pn, initMarking);
		} catch (ConnectionCannotBeObtained e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}/**/
	}

	/*-*/
	@Plugin(
			name = "Test process tree to PN convertor",
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
		XLogInfo logInfo = XLogInfoFactory.createLogInfo(eventlog, XLogInfoImpl.STANDARD_CLASSIFIER);

		Node node = Node
				.fromString(
						//"XOR( SEQ( LEAF: A+complete , LOOP( AND( LEAF: F+complete , LEAF: E+complete ) , LEAF: EXIT ) ) , SEQ( LEAF: B+complete , SEQ( LEAF: D+complete , LEAF: C+complete ) ) ))",
						//"SEQ( SEQ( SEQ( SEQ( LEAF: SSSTTTAAARRRTTT+complete , LEAF: A+complete ) , XOR( LEAF: C+complete , LEAF: B+complete ) ) , XOR( SEQ( LEAF: H+complete , LEAF: G+complete ) , XOR( LEAF: H+complete , LEAF: E+complete ) ) ) , XOR( SEQ( SEQ( LEAF: J+complete , LEAF: L+complete ) , LEAF: EEENNNDDD+complete ) , SEQ( SEQ( LEAF: I+complete , LEAF: L+complete ) , LEAF: EEENNNDDD+complete ) ) )",
						//"AND( AND( XOR( SEQ( LEAF: SSSTTTAAARRRTTT+complete , LEAF: C+complete ) , AND( LEAF: F+complete , LEAF: D+complete ) ) , XOR( SEQ( LEAF: B+complete , LEAF: E+complete ) , LEAF: H+complete ) ) , AND( XOR( SEQ( LEAF: I+complete , LEAF: K+complete ) , SEQ( LEAF: SSSTTTAAARRRTTT+complete , LEAF: J+complete ) ) , AND( LEAF: L+complete , SEQ( LEAF: A+complete , LEAF: EEENNNDDD+complete ) ) ) )",
//						"AND( AND( SEQ( SEQ( LEAF: SSSTTTAAARRRTTT+complete , LEAF: E+complete ) , LEAF: L+complete ) , SEQ( XOR( LEAF: G+complete , LEAF: D+complete ) , XOR( LEAF: EEENNNDDD+complete , LEAF: H+complete ) ) ) , AND( SEQ( LEAF: A+complete , XOR( LEAF: K+complete , LEAF: J+complete ) ) , XOR( SEQ( LEAF: B+complete , LEAF: C+complete ) , SEQ( LEAF: B+complete , LEAF: F+complete ) ) ) )",
						"AND( SEQ( SEQ( LEAF: SSSTTTAAARRRTTT+complete , LEAF: A+complete ) , OR( XOR( SEQ( LEAF: B+complete , SEQ( LEAF: F+complete , LEAF: J+complete ) ) , SEQ( LEAF: H+complete , LEAF: I+complete ) ) , LEAF: K+complete ) ) , LOOP( XOR( LEAF: EEENNNDDD+complete , XOR( XOR( LEAF: L+complete , LEAF: E+complete ) , XOR( LEAF: G+complete , XOR( LEAF: D+complete , LEAF: C+complete ) ) ) ) , LEAF: EXIT ) )",
						logInfo.getEventClasses());
		//OR( OR( AND( LEAF: A+complete , LEAF: E+complete ) , AND( LEAF: C+complete , LEAF: D+complete ) ) , AND( LEAF: B+complete , LEAF: F+complete ) )

		TreeToPNConvertor PNconvertor = new TreeToPNConvertor();
		Petrinet pn = PNconvertor.buildPetrinet(node);

		PNconvertor.applyMurata(context, pn);

		return pn;
	}
}
