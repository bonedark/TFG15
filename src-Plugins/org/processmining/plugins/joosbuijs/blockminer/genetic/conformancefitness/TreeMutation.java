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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.uncommons.maths.random.Probability;
import org.uncommons.watchmaker.framework.EvolutionaryOperator;

/**
 * Mutation operator for the trees of {@link Node}s.
 * 
 * @author jbuijs
 * @author Daniel Dyer (original)
 */
public class TreeMutation implements EvolutionaryOperator<Tree> {
	private boolean showDetails = false;

	private final TreeFactory treeFactory;

	private final Probability mutationProbability;

	/**
	 * The tree mutation operator requires a {@link TreeFactory} because the
	 * process of mutation involves creating new sub-trees. The same TreeFactory
	 * that is used to create the initial population should be used.
	 * 
	 * @param treeFactory
	 *            Used to generate the new sub-trees required for mutation.
	 * @param mutationProbability
	 *            The probability that any given node in a tree is mutated by
	 *            this operator.
	 */
	public TreeMutation(TreeFactory treeFactory, Probability mutationProbability) {
		this.treeFactory = treeFactory;
		this.mutationProbability = mutationProbability;
	}

	/**
	 * Applies mutation functions to the tree, depending on the tree's fitness
	 * characteristics and the provided probabilities
	 */
	public List<Tree> apply(List<Tree> selectedCandidates, Random rng) {
		//		logger.debug(" Candidates for mutation: " + selectedCandidates.size());
		if (showDetails)
			System.out.println(" Candidates for mutation: " + selectedCandidates.size());

		if (showDetails) {
			//Output details on the selectedCandidates population
			/*-* /
			DescriptiveStatistics stats = new DescriptiveStatistics();
			HashMap<String, Integer> treemap = new HashMap<String, Integer>();

			for (Tree tree : selectedCandidates) {
				stats.addValue(tree.getFitness().getFitness());
				String treeString = tree.toString();
				if (treemap.containsKey(treeString)) {
					treemap.put(treeString, treemap.get(treeString) + 1);
				} else {
					treemap.put(treeString, 1);
				}
			}

			System.out.println("--- Mutation Candidates fitness info");
			System.out.println("  " + stats.getN() + " elements");
			System.out.println("  " + stats.getMax() + " fittest");
			System.out.println("  " + stats.getMean() + " average");

			for (String key : treemap.keySet()) {
				System.out.println(" " + treemap.get(key) + " * " + key);
			}/**/
		}

		int nrUnchangedTrees = 0;

		List<Tree> mutatedPopulation = new ArrayList<Tree>(selectedCandidates.size());
		for (Tree tree : selectedCandidates) {
			Tree mutatedTree = new Tree(tree);

			boolean firstTry = true;

			if (showDetails)
				System.out.println("   FROM " + mutatedTree.toString());

			//Mutate tree as long as it is the same as the original
			//FIXME shouldn't be necessary...
			while (tree.toString().equals(mutatedTree.toString())) {
				//Chance of mutation vs random creation
				if (mutationProbability.nextEvent(rng)) {

					/*-* /
					//If there are hotspots, act upon them
					Fitness mutatedTreeFitness = mutatedTree.getFitness();
					if (mutatedTreeFitness != null && !(mutatedTreeFitness.getHotspots().size() == 0)) {
						//Select a hotspot
						XEventClass hotspot = mutatedTreeFitness.getHotspots().iterator().next();
						System.out.println("  Focussing on hotspot " + hotspot.toString());
						List<EventClassNode> leafs = mutatedTree.getLeafsOfEventClass(hotspot);
						if(leafs.size() == 0)
						{
							System.out.println("bla");
						}
						EventClassNode leafToBeMoved = leafs.get(rng.nextInt(leafs.size()));
						
						//And move it action
						mutatedTree.mutateRemoveLeaf(leafToBeMoved);
						mutatedTree.mutateAddLeafRandom(leafToBeMoved);
					} else /**/{

						/*-* /
						//See if we need to focus on adding/removing leafs
						List<XEventClass> eventClasses = treeFactory.getEventClasses();
						List<XEventClass> classesToRemove = new LinkedList<XEventClass>();
						List<XEventClass> classesToAdd = new LinkedList<XEventClass>();
						HashMap<XEventClass, Double> ecMap = new HashMap<XEventClass, Double>();

						for (XEventClass eventClass : eventClasses) {
							//(#occurrence/#trace)-#inTree
							double relativeOccurrence = treeFactory.getEcRelativeOccurence().get(eventClass);
							int nrInTree = mutatedTree.getLeafsOfEventClass(eventClass).size();

							double occurrenceCorrection = relativeOccurrence - nrInTree;
							ecMap.put(eventClass, occurrenceCorrection);
							//TODO check if condition/threshold is good enough
							double threshold = 0.25;
							if (occurrenceCorrection > threshold || occurrenceCorrection < -threshold) {
								if (occurrenceCorrection > threshold)
									classesToAdd.add(eventClass);
								if (occurrenceCorrection < -(1 - threshold))
									classesToRemove.add(eventClass);
							}
						}//for eventclasses

						//Now first balance the tree
						if (classesToAdd.size() > 0 || classesToRemove.size() > 0) {
							if (classesToAdd.size() > 0)
								mutatedTree.mutateAddNodeRandom(classesToAdd);
							if (classesToRemove.size() > 0)
								mutatedTree.mutateRemoveNode(classesToRemove);
						}
						/**/

						mutatedTree.checkTree();

						//Now apply one of our mutation types at a time
						int action = rng.nextInt(4);
						//Exclude add and remove since we focus those
						//exclude addFNodeInBetween for now
						//action = 3;

						if (showDetails)
							System.out.println("Action: " + action);

						switch (action) {
							case 0 :
								/*
								 * Change a single node but we might also want
								 * to change the order of some nodes (e.g.
								 * AND(B,C) should become SEQ(A,B) in some cases
								 */
								mutatedTree.mutateSingleNodeRandom(rng.nextBoolean());
								break;
							case 1 :
								//Remove 1 node with one child
								mutatedTree.mutateRemoveNodeRandom();
								break;							
							case 2 :
								//ADD one function node with existing subtree and new leaf node
								mutatedTree.mutateAddNodeRandom();
								break;
							case 3 :
								//Swap 2 subtrees
								mutatedTree.mutateSwapSubtreesRandom();
								break;
							default :
								//Insert a new FNode and give it some children
								mutatedTree.mutateAddFNodeInBetweenRandom();
						}//switch
						mutatedTree.checkTree();

					}
				}//if mutation probability
				else {
					mutatedTree = treeFactory.generateRandomCandidate(rng);
					if (showDetails)
						System.out.println("   Creating random tree");
				}
				firstTry = false;
			}//while tree eq. mutated

			//And add the mutated tree
			mutatedPopulation.add(mutatedTree);

			if (showDetails)
				System.out.println("   TO   " + mutatedTree.toString());

			//check if we changed
			if (tree.toString().equals(mutatedTree.toString())) {
				if (showDetails)
					System.out.println("NO CHANGE");
				nrUnchangedTrees++;
			}

		}

		if (nrUnchangedTrees > 0) {
			System.out.println("We didn't change " + nrUnchangedTrees + " trees in this generation");
		}

		return mutatedPopulation;

		/*-
		//OLD CODE
		List<Node> mutatedPopulation = new ArrayList<Node>(selectedCandidates.size());
		for (Node tree : selectedCandidates) {
			mutatedPopulation.add(tree.mutate(rng, mutationProbability, treeFactory));
		}
		return mutatedPopulation;
		 */
	}
}
