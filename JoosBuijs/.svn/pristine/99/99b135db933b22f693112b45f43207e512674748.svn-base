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
package org.processmining.plugins.joosbuijs.blockminer.genetic;

import java.util.List;
import java.util.Random;

import org.processmining.plugins.joosbuijs.blockminer.genetic.FunctionNode.FUNCTIONTYPE;
import org.uncommons.maths.random.Probability;
import org.uncommons.watchmaker.framework.factories.AbstractCandidateFactory;

/**
 * {@link org.uncommons.watchmaker.framework.CandidateFactory} for generating
 * trees of {@link Node}s for the genetic programming example application.
 * 
 * @author jbuijs
 * @author Daniel Dyer (original)
 */
public class TreeFactory extends AbstractCandidateFactory<Node> {
	//List of event classes from which leafs should be chosen
	private final List<EventClassNode> leafNodeCandidates;
	private final Probability functionProbability;
	private int maxNodes;

	/**
	 * Creates a new tree factory that creates new nodes given a list of event
	 * classes for leafs and the probability of introducing intermediate
	 * function nodes
	 * 
	 * @param maxNodes
	 *            Maximum number of nodes of the tree
	 * @param leafNodeCandidates
	 *            Candidates for leaf nodes to choose from
	 * @param functionProbability
	 *            Probability of introducing function nodes even though we did
	 *            not reach the maxNodes limit
	 */
	public TreeFactory(int maxNodes, List<EventClassNode> leafNodeCandidates, Probability functionProbability) {
		if (maxNodes < 1) {
			throw new IllegalArgumentException("Nr of leafs must be at least 1.");
		}
		if (leafNodeCandidates == null || leafNodeCandidates.size() < 1) {
			throw new IllegalArgumentException("There must be at least one event class node to choose from.");
		}

		this.maxNodes = maxNodes;
		this.leafNodeCandidates = leafNodeCandidates;
		this.functionProbability = functionProbability;
	}

	/**
	 * 
	 */
	public Node generateRandomCandidate(Random rng) {
		//return generateRandomCandidate(rng, 1);
		Node node = makeNode(rng, maxNodes, leafNodeCandidates);
		//System.out.println(node.toString(0));
		return node;
	}

	/**
	 * Generates a random candidate taking into account the number of leafs this
	 * candidate should have
	 * 
	 * @param rng
	 * @param maxNodes
	 * @return
	 */
	/*-*/
	public Node generateRandomCandidate(Random rng, int maxNodes) {
		//This method only gets called to change leafs!!!
		return makeNode(rng, maxNodes, leafNodeCandidates);
	}/**/

	/**
	 * Recursively constructs a tree of Nodes, up to the specified maximum
	 * depth.
	 * 
	 * @param rng
	 *            The RNG used to random create nodes.
	 * @param maxNodes
	 *            The number of leafs of the generated tree.
	 * @param leafNodeCandidates
	 *            List of all the event class nodes from which leafs are chosen
	 * @return A tree of nodes.
	 */
	private Node makeNode(Random rng, int maxNodes, List<EventClassNode> leafNodeCandidates) {
		//Add one of our function nodes as long as we need more than one leaf
		if (functionProbability.nextEvent(rng) && maxNodes > 1) {
			//Disabled condition: && maxNodes > 1 to allow for trees growing
			maxNodes--; //we are a node so reduce with one
			
			// Randomly divide the number of remaining nodes over our left and right children
			int leftNodes = rng.nextInt(maxNodes);
			int rightNodes = maxNodes - leftNodes;

			//Create a new node with a random function
			FUNCTIONTYPE[] types = FUNCTIONTYPE.values();
			return new FunctionNode(makeNode(rng, leftNodes, leafNodeCandidates), makeNode(rng, rightNodes,
					leafNodeCandidates), types[rng.nextInt(types.length)]);
		} else {
			//If we need to return a leaf, choose one at random
			return leafNodeCandidates.get(rng.nextInt(leafNodeCandidates.size()));
		}
	}
}
