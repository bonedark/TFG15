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
package org.processmining.plugins.joosbuijs.processtrees.geneticminer;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.boudewijn.tree.MutatableTree;
import org.processmining.plugins.boudewijn.tree.Node;
import org.processmining.plugins.boudewijn.tree.Node.Type;
import org.processmining.plugins.boudewijn.tree.Tree;
import org.uncommons.watchmaker.framework.EvolutionaryOperator;
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
public class TreeFactory extends AbstractCandidateFactory<Tree> implements EvolutionaryOperator<Tree> {
	//List of event classes from which leafs should be chosen
	private final List<XEventClass> eventClasses;
	private final XLog log;
	private final XLogInfo logInfo;
	private final int randomCandidateCount;
	public HashSet<String> testedTrees;

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
	public TreeFactory(XLog log, int randomCandidateCount) {
		this(log, XLogInfoImpl.STANDARD_CLASSIFIER, randomCandidateCount);
	}

	/**
	 * Create new treeFactory instance from the event log and eventClassifier
	 * 
	 * @param log
	 * @param eventClassifier
	 */
	public TreeFactory(XLog log, XEventClassifier eventClassifier, int randomCandidateCount) {
		if (log == null) {
			throw new IllegalArgumentException("The event log can not be empty");
		}

		if (eventClassifier == null) {
			throw new IllegalArgumentException("The event classifier can not be empty");
		}

		this.log = log;

		logInfo = XLogInfoFactory.createLogInfo(log, eventClassifier);

		eventClasses = new LinkedList<XEventClass>(logInfo.getEventClasses().getClasses());

		this.randomCandidateCount = randomCandidateCount;

		testedTrees = new HashSet<String>();
	}

	public XLog getLog() {
		return log;
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
	/*-
	public Tree generateRandomCandidate(Random rng) {
		Node root = makeSubtree(rng, eventClasses);

		Tree tree = new Tree(root);

		tree.checkTree();

		return tree;
	}/**/

	/**
	 * Generates a random tree where each event class occurs in a node
	 */
	public MutatableTree generateRandomCandidate(Random rng) {
		Node root = makeSubtree(rng, eventClasses);

		MutatableTree tree = new MutatableTree(root);

		assert tree.checkTree();

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
			//return new EventClassNode(eventClassesInSubtree.get(0));
			return new Node(eventClassesInSubtree.get(0));
		} else {
			Type selectedType = Node.Type.getRandomExceptBiased(rng);

			if (selectedType == Type.LOOP) {
				//Loops are special...
				return new Node(selectedType, makeSubtree(rng, eventClassesInSubtree), new Node((XEventClass) null));
			} else {
				/*
				 * If we don't create a loop then create a function node, divide
				 * the event classes in two groups and call ourselves twice
				 */

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

				return new Node(selectedType, makeSubtree(rng, ecInSubtree1), makeSubtree(rng, ecInSubtree2));
			}
		}
	}

	/**
	 * Generates subtrees using the given list of event classes based on leaf
	 * probability and a maximum depth. This will not guarantee each event class
	 * to occur exactly once
	 * 
	 * @param random
	 * @param cls
	 *            List of event classes used as leafs
	 * @param leafProb
	 *            Probability of a single node being a leaf
	 * @param maxDepth
	 *            Maximum depth of the generated tree
	 * @return
	 */
	private Node makeSubtree(Random random, List<XEventClass> cls, double leafProb, int maxDepth) {
		return makeSubtree(random, cls, leafProb, maxDepth, Node.Type.values());
	}

	/**
	 * Generates subtrees using the given list of event classes based on leaf
	 * probability and a maximum depth. This will not guarantee each event class
	 * to occur exactly once. Restricts the tree to use only the provided
	 * operator types
	 * 
	 * @param random
	 * @param cls
	 *            List of event classes used as leafs
	 * @param leafProb
	 *            Probability of a single node being a leaf
	 * @param maxDepth
	 *            Maximum depth of the generated tree
	 * @param allowedTypes
	 *            Allowed operator types in the tree
	 * @return
	 */
	private Node makeSubtree(Random random, List<XEventClass> cls, double leafProb, int maxDepth, Type[] allowedTypes) {
		Node root;
		if (random.nextDouble() < leafProb || maxDepth == 1) {
			// random leaf node
			root = new Node(cls.get(random.nextInt(cls.size())));
		} else {
			Node left = makeSubtree(random, cls, leafProb, maxDepth - 1, allowedTypes);

			Type type = allowedTypes[random.nextInt(allowedTypes.length - 1)];

			//Whether or not we want a right subtree depends on the operator
			Node right;
			if (type == Type.LOOP) {
				//The loop is a special one, it does not want a right child
				right = new Node((XEventClass) null);
			} else {
				right = makeSubtree(random, cls, leafProb, maxDepth - 1, allowedTypes);
			}

			root = new Node(type, left, right);
		}

		return root;
	}

	/**
	 * Produces all possible combinations where each event class occurs exactly
	 * once in the tree
	 * 
	 * @return
	 */
	public LinkedHashSet<Tree> generateAllCombinations(Random rng) {
		//TODO improve?!?!?!?
		LinkedHashSet<Tree> trees = new LinkedHashSet<Tree>();
		HashSet<XEventClass> ecSet = new HashSet<XEventClass>(eventClasses);

		//Create trees for each subtree of all event classes

		LinkedList<Node> roots = generateAllSubtrees(ecSet);

		for (Node root : roots) {
			trees.add(new Tree(root));
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
		//TODO improve?!?!?!?
		LinkedList<Node> nodes = new LinkedList<Node>();

		if (classes.size() == 0)
			return nodes;
		else if (classes.size() == 1) {
			//When we finally reach a set of size one then there is only one result
			nodes.add(new Node(classes.iterator().next()));
			//which we can return
			return nodes;
		}

		//First create the powerset of the event classes
		Set<Set<XEventClass>> powerset = Sets.powerSet(classes);

		//Then for each possible function type
		for (Node.Type type : Node.Type.values()) {
			//FIXME correctly create LOOP trees
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
							Node lNodeClone = new Node(leftNode);
							Node rNodeClone = new Node(rightNode);

							//Add a new function node to the set of possible nodes
							nodes.add(new Node(type, lNodeClone, rNodeClone));
						}
					}
				}
			}
		}

		return nodes;
	}

	/**
	 * When the treeFactory is applied as an evolutionary operator it means
	 * introducing random candidate(s)
	 */
	public List<Tree> apply(List<Tree> selectedCandidates, Random rng) {
		List<Tree> changedCandidates = new LinkedList<Tree>();
		changedCandidates.addAll(selectedCandidates);

		TreeSet<Tree> trees = new TreeSet<Tree>(new Comparator<Tree>() {
			public int compare(Tree o1, Tree o2) {
				return (int) Math.signum(o2.getOverallFitness() - o1.getOverallFitness());
			}
		});
		trees.addAll(selectedCandidates);

		//		//Loop through all candidates and remember the worst one
		//		Tree worstTree = null;
		//		for (Tree tree : selectedCandidates) {
		//			changedCandidates.add(tree);
		//			if (worstTree == null
		//					|| ((tree.getOverallFitness() > worstTree.getOverallFitness()) && tree.getOverallFitness() != -1))
		//				worstTree = tree;
		//		}

		int i = 0;
		for (Tree tree : trees) {
			if (i++ >= randomCandidateCount) {
				break;
			}
			changedCandidates.remove(tree);
			changedCandidates.add(generateRandomCandidate(rng));
		}

		return changedCandidates;
	}
}
