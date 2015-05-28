package org.processmining.plugins.joosbuijs.blockminer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetFactory;
import org.processmining.plugins.joosbuijs.blockminer.clustering.ClusterRelationshipDiscovery;
import org.processmining.plugins.joosbuijs.blockminer.clustering.ClusterRelationshipDiscovery.RELATIONSHIPTYPE;
import org.processmining.plugins.joosbuijs.blockminer.clustering.FuzzyCMedoidsClustering;
import org.processmining.plugins.joosbuijs.blockminer.models.BinaryTreeNode;
import org.processmining.plugins.log.logfilters.LogFilter;
import org.processmining.plugins.log.logfilters.XEventCondition;

public class BlockMinerPlugin {
	private PluginContext context;
	private ClusterRelationshipDiscovery clusRelDisc;

	//Some standard settings
	private int NRCLUSTERS = 2; //FIXED, it does NOT make sense to search for more than 2 clusters
	private double FUZZIFIER = 2.0; //TODO experiment with settings?

	//private FuzzyCMedoidsClustering clusterer; //TODO make interface

	@Plugin(
			name = "Mine Block-structured Model",
				parameterLabels = { "Event log" },
				returnLabels = { "Block Structured Model" },
				returnTypes = { Petrinet.class },
				userAccessible = true,
				help = "Mine a block structured process model from an event log")
	@UITopiaVariant(
			uiLabel = "00JB Mine Block Structured Process Model",
				affiliation = "Eindhoven University of Technology",
				author = "J.C.A.M.Buijs",
				email = "j.c.a.m.buijs@tue.nl",
				pack = "JoosBuijs")
	public Petrinet miner(final PluginContext context, XLog eventlog) {
		//XLogInfo info = XLogInfoImpl.create(eventlog);
		clusRelDisc = new ClusterRelationshipDiscovery(eventlog);

		//net = PetrinetFactory.newPetrinet("Block-structured Petrinet");

		this.context = context;

		return buildPetrinet(topDownIterator(eventlog));

	}

	@Plugin(
			name = "Mine Block-structured Model",
				parameterLabels = {},
				returnLabels = { "Block Structured Model" },
				returnTypes = { Petrinet.class },
				userAccessible = true,
				help = "Mine a block structured process model from an event log")
	@UITopiaVariant(
			uiLabel = "00JB TEST binary tree to PN conversion",
				affiliation = "Eindhoven University of Technology",
				author = "J.C.A.M.Buijs",
				email = "j.c.a.m.buijs@tue.nl",
				pack = "JoosBuijs")
	public Petrinet treeConvertorTest(final PluginContext context) {
		BinaryTreeNode A = new BinaryTreeNode(new XEventClass("A", 0));
		BinaryTreeNode B = new BinaryTreeNode(new XEventClass("B", 0));
		BinaryTreeNode C = new BinaryTreeNode(new XEventClass("C", 0));
		BinaryTreeNode D = new BinaryTreeNode(new XEventClass("D", 0));

		BinaryTreeNode seq = new BinaryTreeNode(B, C, RELATIONSHIPTYPE.SEQFWD);
		BinaryTreeNode xor = new BinaryTreeNode(A, seq, RELATIONSHIPTYPE.XOR);
		BinaryTreeNode and = new BinaryTreeNode(xor, D, RELATIONSHIPTYPE.AND);

		return buildPetrinet(and);
	}

	/**
	 * Iterates over (parts of) an event log and produces (parts of) a process
	 * model
	 * 
	 * @param logPart
	 * @return process model (part)
	 */
	private BinaryTreeNode topDownIterator(XLog logPart) {
		/*
		 * If the log part contains only 1 event class we can not find any
		 * clusters so return a treeNode with this event class as content
		 */

		XLogInfo info = XLogInfoImpl.create(logPart);
		final XEventClasses allEventClasses = info.getEventClasses();
		if (allEventClasses.size() == 1) {
			return new BinaryTreeNode(allEventClasses.getByIndex(0));
		}

		//If not, we now try to find 2 clusters in this (part of) the event log:
		/*-
		 * (1): find clusters of event classes
		 *  IN: 1 cluster
		 *  OUT: best division in 2 clusters of events
		 */
		XLogInfo logPartInfo = XLogInfoImpl.create(logPart);
		FuzzyCMedoidsClustering clusterer = new FuzzyCMedoidsClustering(logPart, logPartInfo);
		Map<XEventClass, Set<XEventClass>> clusters = clusterer.getFuzzyCMedoidClusters(NRCLUSTERS, FUZZIFIER);

		//FIXME fixed for 2 clusters (either return just 2 clusters, or make generic for x clusters)
		Iterator<Set<XEventClass>> clustersIt = clusters.values().iterator();
		Set<XEventClass> cluster1 = clustersIt.next();
		Set<XEventClass> cluster2 = clustersIt.next();

		System.out.println("The log part " + printEventClasses(logPartInfo.getEventClasses().getClasses())
				+ " was split into " + printEventClasses(cluster1) + " and " + printEventClasses(cluster2));

		/*-
		 * (2): find the relationship between event classes
		 *  IN: matrix of prob's OR 2 clusters
		 *  OUT: relationship matrix OR single rel.
		 *
		 * Per combination of clusters we record the relationships
		 */

		//Store the relationships of the 2 clusters
		HashMap<RELATIONSHIPTYPE, Double> relationships = clusRelDisc.calculateRelationships(cluster1, cluster2);

		/*-
		 * (3): select strongest class/relationship combination
		 * NOT when in step 2 we only investigate 2 clusters
		 */

		double max = 0.0;
		RELATIONSHIPTYPE strongestType = RELATIONSHIPTYPE.AND;
		for (RELATIONSHIPTYPE type : RELATIONSHIPTYPE.values()) {
			if (relationships.get(type) > max)
				strongestType = type;
		}
		System.out.println("Relation between " + printEventClasses(cluster1) + " and " + printEventClasses(cluster2) + " is " + strongestType);

		/*-
		 * 4: Build intermediate treemodel and call ourselves on the left and right clusters
		 */

		return new BinaryTreeNode(topDownIterator(logSubset(logPart, cluster1)), topDownIterator(logSubset(logPart,
				cluster2)), strongestType);
	}

	private String printEventClasses(Collection<XEventClass> classes) {
		String output = "(";
		
		Iterator<XEventClass> it = classes.iterator();
		while(it.hasNext())
		{
			XEventClass clas = it.next();
			output += clas + ",";
		}
		output += ")";
		
		return output;
	}

	/**
	 * Filters an event log and only keeps the event classes provided in the
	 * toKeep set
	 * 
	 * @param eventlog
	 *            Event log to filter
	 * @param toKeep
	 *            Set of event classes to KEEP
	 * @return Filtered event log containing only the event classes in the
	 *         tokeep set
	 */
	private XLog logSubset(XLog eventlog, final Set<XEventClass> toKeep) {
		XLog sublog = (XLog) eventlog.clone();
		XLogInfo info = XLogInfoImpl.create(sublog);
		final XEventClasses allEventClasses = info.getEventClasses();

		return LogFilter.filter(context.getProgress(), 100, sublog, info, new XEventCondition() {
			//			return LogFilter.filter(sublog, new XEventCondition() {

			public boolean keepEvent(XEvent event) {
				// only keep the event if:
				// 1) its name is in toKeep
				XEventClass c = allEventClasses.getClassOf(event);
				if (!toKeep.contains(c)) {
					return false;
				}
				return true;
			}

		});
	}

	/**
	 * Builds a Petrinet from the given (root) node of a binary tree
	 * 
	 * @param root
	 * @return
	 */
	private Petrinet buildPetrinet(BinaryTreeNode root) {
		//Initialize a new PN
		Petrinet net = PetrinetFactory.newPetrinet("Block Structured Petrinet");

		//Request the part in between
		Pair<PetrinetNode, PetrinetNode> addedNodes = buildPetrinet(net, root);

		//Make sure the petrinet ends with a single source and sink place
		PetrinetNode firstNode = addedNodes.getFirst();
		if (firstNode.getClass() == Transition.class) {
			//Add source and sink places
			Place source = net.addPlace("Source");
			Transition firstTransition = (Transition) firstNode;
			net.addArc(source, firstTransition);
		}
		PetrinetNode lastNode = addedNodes.getSecond();
		if (lastNode.getClass() == Transition.class) {
			//Add source and sink places
			Place sink = net.addPlace("Sink");
			Transition firstTransition = (Transition) lastNode;
			net.addArc(firstTransition, sink);
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
	private Pair<PetrinetNode, PetrinetNode> buildPetrinet(Petrinet net, BinaryTreeNode treeNode) {
		/*
		 * If we found a leaf node then add a transition with that name and
		 * return that transition as the first and last node to connect to
		 */
		if (treeNode.isLeafNode()) {
			Transition trans = net.addTransition(treeNode.getEventClass().toString());
			return new Pair<PetrinetNode, PetrinetNode>(trans, trans);
		} else {
			//First we need to ask for the left and right parts to be added to the petri net
			Pair<PetrinetNode, PetrinetNode> leftPart = buildPetrinet(net, treeNode.getLeftChild());
			Pair<PetrinetNode, PetrinetNode> rightPart = buildPetrinet(net, treeNode.getRightChild());
			//Else, we have a node indicating a relationship so handle those correctly
			switch (treeNode.getRelation()) {
				case SEQFWD :
					//Sequence is easy, just connect the left and right parts...
					connectNodes(leftPart.getSecond(), rightPart.getFirst(), net);
					//And return the first and last nodes of the section we added
					return new Pair<PetrinetNode, PetrinetNode>(leftPart.getFirst(), rightPart.getSecond());
				case SEQBWD :
					//Even backwards sequential is easy...
					//Note since it is backwards sequential, we switch left and right here!!!
					connectNodes(rightPart.getSecond(), leftPart.getFirst(), net);
					//And return the first and last nodes of the section we added
					return new Pair<PetrinetNode, PetrinetNode>(rightPart.getFirst(), leftPart.getSecond());
				case XOR :
					//Ah, an XOR, now we need to add XOR-split and XOR-join places
					Place XORsplit = net.addPlace("XOR-split");
					Place XORjoin = net.addPlace("XOR-join");
					//Now connect the left and parts in between the XOR places
					connectNodes(XORsplit, leftPart.getFirst(), net);
					connectNodes(XORsplit, rightPart.getFirst(), net);
					connectNodes(leftPart.getSecond(), XORjoin, net);
					connectNodes(rightPart.getSecond(), XORjoin, net);
					//And for this section, the XOR places are what we return
					return new Pair<PetrinetNode, PetrinetNode>(XORsplit, XORjoin);
				case AND :
					//An AND means adding two silent transitions
					Transition ANDsplit = net.addTransition("AND-split");
					Transition ANDjoin = net.addTransition("AND-join");
					//These transitions can work very silently
					ANDsplit.setInvisible(true);
					ANDjoin.setInvisible(true);
					//Now connect the left and parts in between the AND transitions
					connectNodes(ANDsplit, leftPart.getFirst(), net);
					connectNodes(ANDsplit, rightPart.getFirst(), net);
					connectNodes(leftPart.getSecond(), ANDjoin, net);
					connectNodes(rightPart.getSecond(), ANDjoin, net);
					//And for this section, the AND transitions are what we return
					return new Pair<PetrinetNode, PetrinetNode>(ANDsplit, ANDjoin);
				default :
					return null; //We should never end up here but we should always return somthing
			}
		}
	}

	/**
	 * Connects two given nodes in the petrinet observing the place-transition
	 * alternating requirement
	 * 
	 * @param leftNode
	 * @param rightNode
	 * @param net
	 */
	private void connectNodes(PetrinetNode leftNode, PetrinetNode rightNode, Petrinet net) {
		//Now we need to connect those parts
		//There are four options (combis of place and transition)
		if (leftNode.getClass() == Place.class) {
			//If the left part ends with a place and the right one too, then merge them...
			if (rightNode.getClass() == Place.class) {
				//instantiate the last place of the left part
				Place leftPlace = (Place) leftNode;
				//get the edges going out of the place of the right part
				Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outEdges = net
						.getOutEdges(rightNode);
				//Now create new arcs from the left place to the t in the right part
				for (PetrinetEdge edge : outEdges) {
					Transition targetTrans = (Transition) edge.getTarget();
					net.addArc(leftPlace, targetTrans);
				}
				//And of course remove the first place in the right part
				net.removeNode(rightNode);
			} else {
				//Otherwise connect p-t
				Place leftPlace = (Place) leftNode;
				Transition rightTrans = (Transition) rightNode;
				net.addArc(leftPlace, rightTrans);
			}
		} else //the left part ends with a transition
		{
			if (rightNode.getClass() == Transition.class) { //If the right part also ends with a transition then add a place in between
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
	}
}
