// =============================================================================
// Copyright 2006-2010 Daniel W. Dyer
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// =============================================================================
package org.processmining.plugins.joosbuijs.blockminer.genetic.conformancefitness;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.plugins.joosbuijs.blockminer.genetic.conformancefitness.FunctionNode.FUNCTIONTYPE;
import org.uncommons.watchmaker.framework.factories.AbstractCandidateFactory;

import com.google.common.collect.Sets;

/**
 * {@link org.uncommons.watchmaker.framework.CandidateFactory} for generating
 * trees of {@link Node}s for the genetic programming example application. It is
 * also used a the central store for information such as the event classes (and
 * their #occurrence)
 * 
 * @author jbuijs
 */
public class TreeFactory extends AbstractCandidateFactory<Tree> {
	//List of event classes from which leafs should be chosen
	private final List<XEventClass> eventClasses;
	private final XLog log;
	private final XLogInfo logInfo;
	private final HashMap<XEventClass, Double> ecRelativeOccurence;

	/**
	 * Creates a new tree factory that creates new nodes given a list of event
	 * classes for leafs and the probability of introducing intermediate
	 * function nodes
	 * 
	 * @param maxNodes
	 *            Maximum number of nodes of the tree
	 * @param eventClasses
	 *            Candidates for leaf nodes to choose from
	 * @param functionProbability
	 *            Probability of introducing function nodes even though we did
	 *            not reach the maxNodes limit
	 */
	public TreeFactory(XLog log) {
		this(log, XLogInfoImpl.STANDARD_CLASSIFIER);
	}

	/**
	 * Create new treeFactory instance from the event log and eventClassifier
	 * 
	 * @param log
	 * @param eventClassifier
	 */
	public TreeFactory(XLog log, XEventClassifier eventClassifier) {
		if (log == null) {
			throw new IllegalArgumentException("The event log can not be empty");
		}

		if (eventClassifier == null) {
			throw new IllegalArgumentException("The event classifier can not be empty");
		}

		this.log = log;

		logInfo = XLogInfoFactory.createLogInfo(log, eventClassifier);

		eventClasses = new LinkedList<XEventClass>(logInfo.getEventClasses().getClasses());

		ecRelativeOccurence = calculateRelativeEventOccurrences(eventClassifier);
	}

	/**
	 * Returns a map of the relative occurrence of an eventclass per trace (e.g.
	 * 1 means 1 per trace)
	 * 
	 * @return
	 */
	public HashMap<XEventClass, Double> getEcRelativeOccurence() {
		return ecRelativeOccurence;
	}

	/**
	 * Calculate how often to include an activity based on the event log
	 * 
	 * @param eventClassifier
	 * @return
	 */
	private HashMap<XEventClass, Double> calculateRelativeEventOccurrences(XEventClassifier eventClassifier) {
		int nrTraces = logInfo.getNumberOfTraces();
		HashMap<XEventClass, Integer> counter = new HashMap<XEventClass, Integer>();

		for (XTrace trace : log) {
			for (XEvent event : trace) {
				XEventClass eventClass = logInfo.getEventClasses().getClassOf(event);
				if (counter.containsKey(eventClass)) {
					counter.put(eventClass, counter.get(eventClass) + 1);
				} else {
					counter.put(eventClass, 1);
				}
			}
		}

		//Now normalize the values
		HashMap<XEventClass, Double> relativeOccurrence = new HashMap<XEventClass, Double>();
		for (XEventClass eventclass : counter.keySet()) {
			relativeOccurrence.put(eventclass, ((double) counter.get(eventclass) / nrTraces));
		}

		return relativeOccurrence;
	}

	public List<XEventClass> getEventClasses() {
		return eventClasses;
	}

	public XLog getLog() {
		return log;
	}

	public XLogInfo getLogInfo() {
		return logInfo;
	}

	/**
	 * Returns a list of random event classes (might include duplicates)
	 * 
	 * @param rng
	 * @param nrCandidates
	 * @return
	 */
	public List<XEventClass> getRandomEventClasses(Random rng, int nrCandidates) {
		List<XEventClass> list = new LinkedList<XEventClass>();

		while (list.size() < nrCandidates) {
			//Add randomly selected candidates
			list.add(getRandomEventClass(rng));
		}

		return list;
	}

	/**
	 * Returns a single randomly selected event class
	 * 
	 * @return
	 */
	public XEventClass getRandomEventClass(Random rng) {
		return eventClasses.get(rng.nextInt(eventClasses.size()));
	}

	/**
	 * Generates a random tree where each event class occurs in a node
	 */
	public Tree generateRandomCandidate(Random rng) {
		Node root = makeSubtree(rng, eventClasses);

		Tree tree = new Tree(rng, this, root);

		tree.checkTree();

		return tree;
	}

	/**
	 * Generates a random subtree that contains the provided event classes once
	 * 
	 * @param rng
	 * @param eventClassesInSubtree
	 * @return
	 */
	private Node makeSubtree(Random rng, List<XEventClass> eventClassesInSubtree) {
		//If we should make a subtree of 1 event class then return a leaf
		if (eventClassesInSubtree.size() == 1) {
			return new EventClassNode(eventClassesInSubtree.get(0));
		} else {
			//Create a function node, divide the event classes in two groups and call ourselves twice

			//First, split the event classes in two lists
			List<XEventClass> ecInSubtree1 = new LinkedList<XEventClass>();
			List<XEventClass> ecInSubtree2 = new LinkedList<XEventClass>();

			for (XEventClass ec : eventClassesInSubtree) {
				//Add the current event class to set 1 if set 1 is empty
				//Don't add it to set 1 if set 2 is still empty
				//If both are non-empty then let rng decide
				if (ecInSubtree1.size() == 0 || (ecInSubtree2.size() > 0 && rng.nextBoolean())) {
					ecInSubtree1.add(ec);
				} else {
					ecInSubtree2.add(ec);
				}
			}

			//But we don't like empty sets so prevent that...

			//Now call ourselves and use the results as our children
			LinkedList<Node> children = new LinkedList<Node>();
			children.add(makeSubtree(rng, ecInSubtree1));
			children.add(makeSubtree(rng, ecInSubtree2));

			//Return a new function node
			/*
			 * Please note that the functionNode constructor 'absorbs' child
			 * function nodes of the same type. This way we will not necessarily
			 * generate binary trees, even though we only create 2 children.
			 */
			return new FunctionNode(children, FUNCTIONTYPE.getRandom(rng));
		}
	}

	/**
	 * Produces all possible combinations where each event class occurs exactly
	 * once in the tree
	 * 
	 * @return
	 */
	public LinkedHashSet<Tree> generateAllCombinations(Random rng) {
		LinkedHashSet<Tree> trees = new LinkedHashSet<Tree>();
		HashSet<XEventClass> ecSet = new HashSet<XEventClass>(eventClasses);

		//Create trees for each subtree of all event classes

		LinkedList<Node> roots = generateAllSubtrees(ecSet);

		for (Node root : roots) {
			trees.add(new Tree(rng, this, root));
		}

		return trees;
	}

	/**
	 * Returns a set of nodes/subtrees that cover all possible combinations
	 * given the event classes as leafs
	 * 
	 * @param classes
	 *            Leafs to include
	 * @return Set of subtrees that cover all possible combinations of function
	 *         nodes and leaf location
	 */
	public LinkedList<Node> generateAllSubtrees(Set<XEventClass> classes) {
		LinkedList<Node> nodes = new LinkedList<Node>();

		if (classes.size() == 0)
			return nodes;
		else if (classes.size() == 1) {
			//When we finally reach a set of size one then there is only one result
			nodes.add(new EventClassNode(classes.iterator().next()));
			//which we can return
			return nodes;
		}

		//First create the powerset of the event classes
		Set<Set<XEventClass>> powerset = Sets.powerSet(classes);

		//Then for each possible function type
		for (FUNCTIONTYPE ftype : FUNCTIONTYPE.values()) {
			//For each powerset
			for (Set<XEventClass> set : powerset) {
				/*
				 * Create the opposite of the selected set in the powerset (so
				 * that we still use each XEventClass provided to us)
				 */
				Set<XEventClass> opposite = Sets.difference(classes, set);

				//And then for each set in the powerset (except empty and all)
				if (set.size() > 0 && opposite.size() > 0) {
					//Ask for possible subtrees with the set and opposite set
					LinkedList<Node> leftNodes = generateAllSubtrees(set);
					LinkedList<Node> rightNodes = generateAllSubtrees(opposite);

					//Now for each combination create a new node instance
					for (Node leftNode : leftNodes) {
						for (Node rightNode : rightNodes) {
							//USE COPIES/CLONES of leftNode/rightNode...
							Node lNodeClone = cloneNode(leftNode);
							Node rNodeClone = cloneNode(rightNode);
							
							//Add a new function node to the set of possible nodes
							LinkedList<Node> children = new LinkedList<Node>();
							children.add(lNodeClone);
							children.add(rNodeClone);
							nodes.add(new FunctionNode(children, ftype));
						}
					}

					//TODO don't ignore indifference in order of children...
					//FIXME create new function, duplication of code is not good...
					//IFF the node is of type SEQ then swap left and right (but instantiate them again!)
					/*-* /
					if(ftype.equals(FUNCTIONTYPE.SEQ)){
						//Ask for possible subtrees with the set and opposite set
						HashSet<Node> rightNodesSeq = generateAllSubtrees(set);
						HashSet<Node> leftNodesSeq = generateAllSubtrees(opposite);
						
						//Now for each combination create a new node instance
						for(Node leftNode : leftNodesSeq){
							for(Node rightNode : rightNodesSeq){
								//Add a new function node to the set of possible nodes
								LinkedList<Node> children = new LinkedList<Node>();
								children.add(leftNode);
								children.add(rightNode);
								nodes.add(new FunctionNode(children, ftype));
							}
						}
					}/**/
				}
			}
		}

		return nodes;
	}

	private Node cloneNode(Node node) {
		if(node.isLeaf())
			return new EventClassNode((EventClassNode) node);
		else{
			return new FunctionNode((FunctionNode) node);
		}
	}
}
