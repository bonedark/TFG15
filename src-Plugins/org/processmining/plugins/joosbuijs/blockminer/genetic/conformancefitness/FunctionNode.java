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
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.deckfour.xes.classification.XEventClass;

/**
 * Function node that contains the functiontype of the node and two child-nodes
 * 
 * @author jbuijs
 * @author Daniel Dyer (original)
 */
public class FunctionNode implements Node {
	//TODO prevent an empty tree so don't remove the last child from the root

	/**
	 * The function this node represents (XOR, AND, SEQ)
	 * 
	 * @author jbuijs
	 */
	public enum FUNCTIONTYPE {
		XOR, AND, SEQ;

		public static FUNCTIONTYPE getRandom(Random rng) {
			FUNCTIONTYPE[] types = values();
			return types[rng.nextInt(types.length)];
		}

		public static FUNCTIONTYPE getRandomExcept(Random rng, FUNCTIONTYPE notThisType) {
			FUNCTIONTYPE[] types = values();
			FUNCTIONTYPE selectedType = types[rng.nextInt(types.length)];
			while (selectedType == notThisType) {
				selectedType = types[rng.nextInt(types.length)];
			}
			return selectedType;
		}
	};

	//An ordered list of children
	/**
	 * PLEASE don't add children yourself to this list (even internally), use
	 * the addChild() functions for that, they know how to handle different
	 * kinds of children and get them to behave correctly
	 */
	private LinkedList<Node> children = new LinkedList<Node>();

	protected FUNCTIONTYPE function;

	private FunctionNode parent;

	/**
	 * @param left
	 *            The first argument to the binary function.
	 * @param right
	 *            The second argument to the binary function.
	 * @param function
	 *            The function this node represents
	 */
	public FunctionNode(List<Node> children, FUNCTIONTYPE function) {
		this.function = function;
		addChildren(children);
	}

	public FunctionNode(FunctionNode fnode) {
		this.function = fnode.getFunction();

		for (Node child : fnode.children) {
			Node newChild;
			if (child.isLeaf()) {
				//Create a new leaf node
				newChild = new EventClassNode((EventClassNode) child);
			} else {
				//Create a new function node
				newChild = new FunctionNode((FunctionNode) child);
			}

			newChild.setParent(this);
			addChild(newChild);
			//children.add(newChild);
		}
	}

	/**
	 * Adds the provided list of children to the END of our childrens list
	 * 
	 * @param children
	 */
	public void addChildren(List<Node> children) {
		//addChildren(children.size(), children);
		for (Node child : children) {
			addChild(child);
		}
	}

	/**
	 * Adds the list of children at the given childindex to its children.
	 * 
	 * @param index
	 * @param newChildren
	 */
	/*-public void addChildren(int index, List<Node> newChildren) {
		for (Node newChild : newChildren) {
			addChild(index, newChild);
			/*
	 * Since the tree might have changed in the mean time (since we limit the #children) we need to recalculate the new index
	 * /
			//If the newly added node is still a child then call ourselves
			index = children.
			//index++;//the next child should be added after the previous one...
		}
	}/**/

	/**
	 * Adds the given child to the end of our childrens list
	 * 
	 * @param child
	 */
	private void addChild(Node child) {
		addChild(children.size(), child);
	}

	/**
	 * Add the given node as a child at a random location in its children array
	 * 
	 * @param rng
	 * @param child
	 */
	public void addChildRandom(Random rng, Node child) {
		//If we don't have any children rng doesn't like 0 and the position doesn't matter...
		if (children.size() == 0)
			addChild(0, child);
		else {
			addChild(rng.nextInt(children.size()), child);
		}
	}

	/**
	 * Adds a new child at the given position, also performs checks to prevent
	 * nesting of function nodes with same function (i.e. 'absorbs' children
	 * which have the same function). Furthermore takes the arity of the
	 * function into account and if necessary introduces a new child function
	 * node
	 * 
	 * @param index
	 *            child index (NOT node index) to insert the child node at
	 * @param childNode
	 */
	public void addChild(int index, Node childNode) {
		addChild(index, childNode, true);
	}

	/**
	 * Adds a new child at the given position, also performs checks to prevent
	 * nesting of function nodes with same function (i.e. 'absorbs' children
	 * which have the same function). Furthermore takes the arity of the
	 * function into account and if necessary introduces a new child function
	 * node
	 * 
	 * @param index
	 *            child index (NOT node index) to insert the child node at
	 * @param childNode
	 * @param isLastChange
	 *            if true the node will absorb children with the same operator
	 *            (if possible) or delete itself when it has only 1 child
	 */
	public void addChild(int index, Node childNode, boolean isLastChange) {
		//FIXME implement 'lastChange' operator, also for removing nodes, we might want to delay the checks but need to perform them later
		//First things first: sanity check!
		if (childNode.equals(this)) {
			//We don't want to be parenting ourselves, we would eat candy all day (e.g. loop)
			throw new IndexOutOfBoundsException("Adding yourself as a child is not allowed.");
		} else if (childNode.getPostorder().contains(this)) {
			//We don't even remotely want to be a child of ourselves...
			throw new IndexOutOfBoundsException("The child is already in our subtree.");
		}

		/*
		 * Remember the old parent, we need to remove the child from there after
		 * adding it to the new parent. Since we don't allow for empty (even
		 * inbetween) we need to first add/move the node, then remove it.
		 */
		FunctionNode oldParent = childNode.getParent();

		/*
		 * If the child node is a function node AND its function type is the
		 * same as ours AND we don't exceed the max. nr. of children then
		 * absorb!
		 */
		/*-*/
		if (!childNode.isLeaf() && ((FunctionNode) childNode).getFunction().equals(this.getFunction())
				&& ((this.countChildren() + childNode.countChildren()) <= getArity())) {
			//Get the children of the child node
			List<Node> grandChildren = ((FunctionNode) childNode).children;
			//And call the addChildren function again for lists of children
			children.addAll(grandChildren);
			for (Node grandChild : grandChildren) {
				grandChild.setParent(this);
			}
		} else /**/{
			//Check if we can add the child node at the given position, otherwise correct the index
			if (index < 0)
				index = 0;
			if (index > children.size())
				index = children.size();

			//Now check if, by adding the new child, we exceed our max. arity
			if (countChildren() + 1 > getArity()) {
				/*
				 * If we do, we need to introduce a new function node, with the
				 * same function as ourselves, to add the new child to
				 */

				//Get out the original child at that index
				Node orgChild;
				if (index < countChildren()) {
					orgChild = children.get(index);
					children.remove(index);
				} else {
					//its the last one
					orgChild = children.getLast();
					children.removeLast();
				}
				orgChild.setParent(null);

				//Now build the child list of the new function node
				List<Node> newChildren = new LinkedList<Node>();
				//If the index is < arity-1 (e.g. not the last one) 
				if (index < getArity()) {
					//The new child node will go first
					newChildren.add(childNode);
					newChildren.add(orgChild);
				} else {
					newChildren.add(orgChild);
					newChildren.add(childNode);
				}

				//then insert a new function node at that index with the same function
				FunctionNode newFNode = new FunctionNode(newChildren, getFunction());
				addChild(index, newFNode);

				//Now tell everybody who their new parent is
				orgChild.setParent(newFNode);
				childNode.setParent(newFNode);
			} else {
				//Otherwise, set ourselves as the child node's new parent
				childNode.setParent(this);
				//And just add the child node at the given location
				children.add(index, childNode);
			}
		}

		//Now remove the child from the old parent
		if (oldParent != null && !oldParent.equals(this)) {
			oldParent.removeChild(childNode);
		}/**/
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.processmining.plugins.joosbuijs.blockminer.genetic.Node#getParent()
	 */
	public FunctionNode getParent() {
		return parent;
	}

	/**
	 * Returns the max arity (/#children) of this function node
	 * 
	 * @return
	 */
	public int getArity() {
		return 2;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.processmining.plugins.joosbuijs.blockminer.genetic.Node#setParent
	 * (org.processmining.plugins.joosbuijs.blockminer.genetic.FunctionNode)
	 */
	public void setParent(FunctionNode parent) {
		this.parent = parent;
	}

	/**
	 * @return the function type of this node
	 */
	public FUNCTIONTYPE getFunction() {
		return function;
	}

	/**
	 * The depth of a function node is the depth of its deepest sub-tree plus
	 * one.
	 * 
	 * @return The depth of the tree rooted at this node.
	 */
	public int getDepth() {
		int maxDepth = 0;
		for (Node child : children) {
			maxDepth = Math.max(maxDepth, child.getDepth());
		}
		return 1 + maxDepth;
	}

	/**
	 * The width of a node is the sum of the widths of its two sub-trees.
	 * 
	 * @return The width of the tree rooted at this node.
	 */
	/*-* /
	public int getWidth() {
		int width = 0;
		for (Node child : children) {
			width += child.getWidth();
		}
		return width;
	}/**/

	/**
	 * Counts the total number of nodes (including self) of this subtree
	 * {@inheritDoc}
	 */
	public int countNodes() {
		int count = 1;//count self
		for (Node child : children) {
			count += child.countNodes();
		}
		return count;
	}

	/**
	 * Counts the number of leafs in this subtree {@inheritDoc}
	 */
	/*-* /
	public int countLeafs() {
		int leafs = 0;
		for (Node child : children) {
			leafs += child.countLeafs();
		}
		return leafs;
	}/**/

	public int countChildren() {
		return children.size();
	}

	/**
	 * Returns the node at the given index in this subtree
	 * 
	 * {@inheritDoc}
	 */
	public Node getNode(int index) {
		//We are at index 0
		if (index == 0) {
			return this;
		}

		//Now loop through the children
		for (Node child : children) {
			//If the index is in the range of the current child
			if (child.countNodes() >= index) {
				//Ask the child
				return child.getNode(index - 1);
			} else {
				//Otherwise, decrease index with this child's size and one and continue to the next
				index -= (child.countNodes());
			}
		}

		//If the index was bigger than the total size of this subtree then throw an exception
		throw new IndexOutOfBoundsException("Invalid node index: " + index);
	}

	/**
	 * Gets the child at the given index [0,n)
	 * 
	 * {@inheritDoc}
	 */
	public Node getChild(int index) {
		if (index >= children.size())
			throw new IndexOutOfBoundsException("Invalid child index: " + index);
		else if (index < 0) {
			throw new IndexOutOfBoundsException("Invalid child index: " + index);
		} else
			return children.get(index);
	}

	/**
	 * Replaces the node at the provided index with the given newNode
	 * {@inheritDoc}
	 */
	public void replaceNode(int index, Node newNode) {
		Node oldNode = getNode(index);

		replaceNode(oldNode, newNode);
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		StringBuilder buffer = new StringBuilder("");
		buffer.append(function);
		buffer.append('(');

		for (Node child : children) {
			buffer.append(child.toString());
			buffer.append(',');
		}
		//And remove the last comma
		buffer.deleteCharAt(buffer.length() - 1);

		buffer.append(')');
		return buffer.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.processmining.plugins.joosbuijs.blockminer.genetic.Node#toString(int)
	 */
	public String toString(int level) {
		StringBuilder buffer = new StringBuilder("");
		StringBuilder tabs = new StringBuilder("");

		for (int i = 0; i < level; i++)
			tabs.append("\t");

		//Print our function, a nice ( and then go to a new line
		buffer.append(tabs);
		buffer.append(function);
		buffer.append('(' + System.getProperty("line.separator"));

		//Now print all our children
		for (Node child : children) {
			buffer.append(child.toString(level + 1));
		}

		//And now add an indented )
		buffer.append(tabs);
		buffer.append(')' + System.getProperty("line.separator"));
		return buffer.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.processmining.plugins.joosbuijs.blockminer.genetic.Node#getLabel()
	 */
	@Override
	public String getLabel() {
		return function.toString();
	}

	/**
	 * Returns a list of the event classes contained below this node
	 * 
	 * @return
	 */
	public List<XEventClass> getEventClasses() {
		List<XEventClass> list = new ArrayList<XEventClass>();

		//Ask our children, they know!
		for (Node child : children) {
			list.addAll(child.getEventClasses());
		}

		return list;
	}

	/**
	 * Returns a list of the event clas nodes contained below this node
	 * 
	 * @return
	 */
	public List<EventClassNode> getLeafs() {
		List<EventClassNode> list = new ArrayList<EventClassNode>();

		//Ask our children, they know!
		for (Node child : children) {
			list.addAll(child.getLeafs());
		}

		return list;
	}

	/**
	 * @param newFunction
	 */
	public void setFunction(FUNCTIONTYPE newFunction) {
		this.function = newFunction;
	}

	public List<Node> getPostorder() {
		List<Node> postorder = new LinkedList<Node>();

		//Ask for the post order of our children
		for (Node child : children) {
			postorder.addAll(child.getPostorder());
		}

		//Then add ourselves
		postorder.add(this);

		return postorder;
	}

	public List<EventClassNode> getLeafsOfEventClass(XEventClass eventClass) {
		LinkedList<EventClassNode> list = new LinkedList<EventClassNode>();
		for (Node node : children) {
			list.addAll(node.getLeafsOfEventClass(eventClass));
		}
		return list;
	}

	public boolean isLeaf() {
		return false;
	}

	/**
	 * Replaces the old node with the new one
	 * 
	 * @param oldNode
	 * @param newNode
	 */
	public void replaceNode(Node oldNode, Node newNode) {
		//Check if we are the parent
		if (children.contains(oldNode)) {
			//Then remove this child
			int index = children.indexOf(oldNode);
			//removeChild(oldNode);
			children.remove(oldNode);
			if (oldNode.getParent().equals(this))
				oldNode.setParent(null);
			//And adopt the new one
			children.add(index, newNode);
			newNode.setParent(this);
			//addChild(index, newNode);
		} else {
			//This is not one of our (direct) children...

			/*
			 * If the child doesn't know its parent or still thinks that we are
			 * the parent but we are not then don't do anything
			 */
			if (this == oldNode.getParent() || oldNode.getParent() == null || oldNode == oldNode.getParent())
				return;

			//Otherwise, call this function on the parent of the old node
			oldNode.getParent().replaceNode(oldNode, newNode);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.processmining.plugins.joosbuijs.blockminer.genetic.Node#getIndexOf
	 * (org.processmining.plugins.joosbuijs.blockminer.genetic.Node)
	 */
	public int getIndexOf(Node node) {
		//First check if we are the node
		if (node.equals(this))
			return 0;

		//Else, check the children
		if (children.contains(node)) {
			int index = 1; //we start with index 1
			//Loop through the children to get the index
			for (int i = 0; i < children.size(); i++) {
				if (children.get(i).equals(node)) {
					return index;
				} else {
					//Go to the next child but increase the index by the size of this child
					index += children.get(i).countNodes();
				}
			}
		} else {
			//Recursively call the children for the index in their subtree
			int index = 1;
			for (Node child : children) {
				int childIndex = child.getIndexOf(node);
				if (childIndex > 0) {
					return childIndex + index;
				} else {
					index += child.countNodes();
				}
			}
		}

		//We couldn't find the node!
		return -1;

	}

	/**
	 * Removes the given child node from this function node and moves out the
	 * function if only 1 child is left
	 * 
	 * @param node
	 */
	public void removeChild(Node node) {
		//Don't even remove a child if we are the root and don't have any other children left
		if (parent == null && children.size() <= 1) {
			//throw new IllegalStateException("You can not remove the last child of the root");
			return;
		} else {
			//Otherwise, remove child
			children.remove(node);
		}

		//Make sure the child does not refer back to us
		if (node.getParent() == this && node.getParent() != null) {
			node.setParent(null);
		}

		//If we are the root then never remove ourselves and stop here
		if (parent == null)
			return;

		//If we have 0 children then there is no use for us any more! (if there is 1 keep ourselves alive and spoil this kid)
		/*-if (children.size() == 1) {
			//Otherwise, we remove ourselves between our parent and our child
			Node child = children.get(0);
			//Give the child a new parent
			child.setParent(parent);
			//And our parent a new child
			parent.replaceNode(this, child);
		} else/**/
		if (children.size() == 0) {
			//Just for the special case where we failed to capture another removal somehow
			//Now we just need to remove our (empty) selves from our parent
			parent.removeChild(this);
		}

	}

	/**
	 * Replaces the order of the two children in its children list
	 * 
	 * @param node1
	 * @param node2
	 */
	public void swapChildren(Node node1, Node node2) {
		//First do a sanity check
		if (!children.contains(node1) || !children.contains(node2))
			return;
		if (node1.equals(node2))
			return;

		int index1 = children.indexOf(node1);
		int index2 = children.indexOf(node2);

		//Warning: special case: we have two children that need to be swapped, we don't want to end up empty
		if (children.size() == 2) {
			//Just swap them round
			removeChild(index1);
			addChild(index2, node1);
		} else {
			//Remove children in the correct order as to not influence index of the other
			if (index1 > index2) {
				removeChild(index1);
				removeChild(index2);
				addChild(index2, node1);
				addChild(index1, node2);
			} else {
				removeChild(index2);
				removeChild(index1);
				addChild(index1, node2);
				addChild(index2, node1);
			}
		}
	}

	/**
	 * Removes the child at the given index
	 * 
	 * @param index
	 */
	public void removeChild(int index) {
		if (index >= 0 && index < children.size()) {
			//Calls the smarter function for the removal of children
			removeChild(children.get(index));
		}
	}

	/**
	 * Add a new function node as one of its children and gives it at least 2 of
	 * its own children
	 * 
	 * @param rng
	 */
	public void addFNodeForSomeChildren(Random rng) {
		//We can only do this if we have 3 children or if we are allowed more than 2 children at all...
		if (children.size() < 3 || getArity() < 3)
			return;

		//Now build a new FNode
		//Select a functiontype that is different from the one we have (otherwise we would absorb it)
		FunctionNode newFNode = new FunctionNode(new LinkedList<Node>(), FUNCTIONTYPE.getRandomExcept(rng,
				this.function));

		/*
		 * First determine how many children to give to the new fnode with a
		 * minimum of 2 but leave at least one child for ourselves
		 */
		/*-*/
		int nrChildrenToMove = 0;
		//If we have 3 children then we would do rng.nextInt(0) which is not acceptable 
		if (children.size() == 3) {
			nrChildrenToMove = 2;
		} else {
			nrChildrenToMove = rng.nextInt(children.size() - 3) + 2;
		}/**/
		//		int nrChildrenToMove = rng.nextInt(children.size() - 3) + 2;

		for (int i = 0; i < nrChildrenToMove; i++) {
			//Select a child
			Node selected = children.get(rng.nextInt(children.size()));
			//Remove it
			removeChild(selected);
			//And add it to the new fnode
			newFNode.addChildRandom(rng, selected);
		}

		//Now we can add the new fnode as one of our children
		addChildRandom(rng, newFNode);
	}

	/**
	 * Returns the child index of the given node or -1 if the node is not a
	 * child
	 * 
	 * @param child
	 * @return
	 */
	public int getChildIndex(Node child) {
		return children.indexOf(child);
	}
}
