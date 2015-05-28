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

import java.util.Random;

import org.uncommons.maths.random.Probability;

/**
 * Function node that contains the functiontype of the node and two child-nodes
 * 
 * @author jbuijs
 * @author Daniel Dyer (original)
 */
public class FunctionNode implements Node {
	/**
	 * The function this node represents (XOR, AND, SEQ)
	 * 
	 * @author jbuijs
	 */
	public enum FUNCTIONTYPE {
		XOR, AND, SEQ
	};

	/** The first argument to the binary function. */
	protected final Node left;
	/** The second argument to the binary function. */
	protected final Node right;

	protected final FUNCTIONTYPE function;

	/**
	 * @param left
	 *            The first argument to the binary function.
	 * @param right
	 *            The second argument to the binary function.
	 * @param function
	 *            The function this node represents
	 */
	protected FunctionNode(Node left, Node right, FUNCTIONTYPE function) {
		this.left = left;
		this.right = right;
		this.function = function;
	}

	/**
	 * @return the function type of this node
	 */
	public FUNCTIONTYPE getFunction() {
		return function;
	}

	/**
	 * The arity of a function node is two.
	 * 
	 * @return 2
	 */
	public int getArity() {
		return 2;
	}

	/**
	 * The depth of a function node is the depth of its deepest sub-tree plus
	 * one.
	 * 
	 * @return The depth of the tree rooted at this node.
	 */
	public int getDepth() {
		return 1 + Math.max(left.getDepth(), right.getDepth());
	}

	/**
	 * The width of a binary node is the sum of the widths of its two sub-trees.
	 * 
	 * @return The width of the tree rooted at this node.
	 */
	public int getWidth() {
		return left.getWidth() + right.getWidth();
	}

	/**
	 * {@inheritDoc}
	 */
	public int countNodes() {
		return 1 + left.countNodes() + right.countNodes();
	}

	/**
	 * {@inheritDoc}
	 */
	public int countLeafs() {
		return left.countLeafs() + right.countLeafs();
	}

	/**
	 * {@inheritDoc}
	 */
	public Node getNode(int index) {
		if (index == 0) {
			return this;
		}
		int leftNodes = left.countNodes();
		if (index <= leftNodes) {
			return left.getNode(index - 1);
		} else {
			return right.getNode(index - leftNodes - 1);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Node getChild(int index) {
		switch (index) {
			case 0 :
				return left;
			case 1 :
				return right;
			default :
				throw new IndexOutOfBoundsException("Invalid child index: " + index);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Node replaceNode(int index, Node newNode) {
		if (index == 0) {
			return newNode;
		}

		int leftNodes = left.countNodes();
		if (index <= leftNodes) {
			return newInstance(left.replaceNode(index - 1, newNode), right);
		} else {
			return newInstance(left, right.replaceNode(index - leftNodes - 1, newNode));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		StringBuilder buffer = new StringBuilder("");
		buffer.append(function);
		buffer.append('(');
		buffer.append(left.toString());
		buffer.append(',');
		buffer.append(right.toString());
		buffer.append(')');
		return buffer.toString();
	}

	public String toString(int level) {
		StringBuilder buffer = new StringBuilder("");
		StringBuilder tabs = new StringBuilder("");

		for (int i = 0; i < level; i++)
			tabs.append("\t");

		buffer.append(tabs);
		//System.getProperty("line.separator")
		buffer.append(function);
		buffer.append('(' + System.getProperty("line.separator"));
		buffer.append(left.toString(level + 1));
		//buffer.append(',' + System.getProperty("line.separator"));
		buffer.append(right.toString(level + 1));
		buffer.append(tabs);
		buffer.append(')' + System.getProperty("line.separator"));
		return buffer.toString();
	}

	@Override
	public String getLabel() {
		return function.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public Node mutate(Random rng, Probability mutationProbability, TreeFactory treeFactory) {
		//The code below generates a whole new sub-tree
		/*-*/
		if (mutationProbability.nextEvent(rng)) {
			//We should mutate

			//Now decide 50/50 if we should change ourselves or create a whole new subtree
			if (Probability.EVENS.nextEvent(rng)) {
				//Create a complete new node 
				return treeFactory.generateRandomCandidate(rng, countNodes());
			}

			//Change this node's function
			FUNCTIONTYPE[] types = FUNCTIONTYPE.values();
			FUNCTIONTYPE newFunction = types[rng.nextInt(types.length)];

			Node newLeft = left.mutate(rng, mutationProbability, treeFactory);
			Node newRight = right.mutate(rng, mutationProbability, treeFactory);
			return newInstance(newLeft, newRight, newFunction);
			/**/

		} else {
			//We should not mutate but ask our children to do so
			Node newLeft = left.mutate(rng, mutationProbability, treeFactory);
			Node newRight = right.mutate(rng, mutationProbability, treeFactory);
			if (newLeft != left || newRight != right) {
				return newInstance(newLeft, newRight);
			} else {
				// Tree has not changed.
				return this;
			}
		}/**/
	}

	/**
	 * Returns a new instance of this node with new left and right children
	 * 
	 * @param newLeft
	 *            New left child
	 * @param newRight
	 *            New right child
	 * @return New function node with the same function but new children
	 */
	private Node newInstance(Node newLeft, Node newRight) {
		//TODO check
		return new FunctionNode(newLeft, newRight, function);
		/*-
		Constructor<? extends FunctionNode> constructor = ReflectionUtils.findKnownConstructor(this.getClass(),
				Node.class, Node.class, FUNCTIONTYPE.class);
		return ReflectionUtils.invokeUnchecked(constructor, newLeft, newRight, function);
		/**/
	}

	/**
	 * Returns a new instance of this node with new left and right children
	 * 
	 * @param newLeft
	 *            New left child
	 * @param newRight
	 *            New right child
	 * @param newFunction
	 *            New function of this node
	 * @return New function node with the same function but new children
	 */
	private Node newInstance(Node newLeft, Node newRight, FUNCTIONTYPE function) {
		//TODO check
		return new FunctionNode(newLeft, newRight, function);
		/*-
		Constructor<? extends FunctionNode> constructor = ReflectionUtils.findKnownConstructor(this.getClass(),
				Node.class, Node.class, FUNCTIONTYPE.class);
		return ReflectionUtils.invokeUnchecked(constructor, newLeft, newRight, function);
		/**/
	}
}
