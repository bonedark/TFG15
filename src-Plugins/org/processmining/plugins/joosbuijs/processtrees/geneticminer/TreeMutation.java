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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.processmining.plugins.boudewijn.tree.MutatableTree;
import org.processmining.plugins.boudewijn.tree.Node;
import org.processmining.plugins.boudewijn.tree.Tree;
import org.uncommons.watchmaker.framework.EvolutionaryOperator;

/**
 * Mutation operator for the trees of {@link Node}s.
 * 
 * @author jbuijs
 */
public class TreeMutation implements EvolutionaryOperator<Tree> {
	private boolean showDetails = false;

	private final TreeFactory treeFactory;

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
	public TreeMutation(TreeFactory treeFactory) {
		this.treeFactory = treeFactory;
	}

	/**
	 * Applies mutation functions to the tree, depending on the tree's fitness
	 * characteristics and the provided probabilities
	 */
	public List<Tree> apply(List<Tree> selectedCandidates, Random rng) {
		if (showDetails)
			System.out.println(" Candidates for mutation: " + selectedCandidates.size());

		List<Tree> mutatedPopulation = new ArrayList<Tree>(selectedCandidates.size());

		for (Tree tree : selectedCandidates) {
			MutatableTree mutatedTree = new MutatableTree(tree);

			if (showDetails)
				System.out.println("   FROM " + mutatedTree.toString());

			//As long as we have a tree that we have already seen, we mutate
			while (treeFactory.testedTrees.contains(mutatedTree.toCanonicalString())) {
				assert mutatedTree.checkTree();
				mutatedTree.resetFitness();

				//TODO focus mutation on worst dimension

				//Now apply one of our mutation types at a time
				int action = rng.nextInt(3);

				if (showDetails)
					System.out.println("Action: " + action);

				switch (action) {
					case 0 :
						/*
						 * Change a single node but we might also want to change
						 * the order of some nodes (e.g. AND(B,A) should become
						 * SEQ(A,B) in some cases
						 */
						mutatedTree.mutateSingleNodeRandom(rng.nextBoolean(), treeFactory, rng);
						break;
					case 1 :
						//Remove 1 node with one of its children
						if (mutatedTree.countNodes() != 1) {
							mutatedTree.mutateRemoveSubtreeRandom(rng);
							break;
						}
						//In case of a leaf-tree, make it bigger
						//$FALL-THROUGH$
					case 2 :
						//ADD one function node with existing subtree and new leaf node
						mutatedTree.mutateAddNodeRandom(treeFactory, rng);
						break;
				}//switch

				assert mutatedTree.checkTree();

			}//if mutation probability

			//And add the mutated tree
			mutatedPopulation.add(mutatedTree);

			if (showDetails)
				System.out.println("   TO   " + mutatedTree.toString());

			//Remember this one
			treeFactory.testedTrees.add(mutatedTree.toCanonicalString());
		}//for each tree in incoming list

		return mutatedPopulation;
	}
}
