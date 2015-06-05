package org.processmining.plugins.boudewijn.tree;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

import org.deckfour.xes.classification.XEventClass;
import org.processmining.plugins.boudewijn.tree.Node.Type;
import org.processmining.plugins.joosbuijs.processtrees.geneticminer.TreeFactory;

public class MutatableTree extends Tree {

	public MutatableTree(Node root) {
		super(root);
	}

	public MutatableTree(Tree tree) {
		super(tree);
	}

	/**
	 * Randomly instantiates a leaf node and adds it to a randomly chosen parent
	 * which will add it to its children
	 * 
	 * @param mutationTree
	 */
	public boolean mutateAddNodeRandom(TreeFactory treeFactory, Random rng) {
		assert checkTree();
		String before = this.toString();

		//Create a random leaf node
		//		Node newLeafNode = new Node(treeFactory.getRandomEventClass(rng));
		//Select a random node to give a new parent
		//		Set<Node> notTheseLoops = new HashSet<Node>();
		//notTheseLeafsAndLoops.addAll(getRoot().getNodesOfType(Type.LEAF));
		//		notTheseLoops.addAll(getRoot().getNodesOfType(Type.LOOP));
		//		Node selectedNode = getRandomNode(notTheseLoops, rng);
		Node selectedNode = getRandomNode(rng);

		//Give the selected node a new parent, but not a loop if the current parent is a loop
		HashSet<Type> notTheseTypes = new HashSet<Node.Type>();
		//Iff the parent of the node is a LOOP or if the selected node is a loop
		if ((selectedNode.getParent() != null && selectedNode.getParent().getType() == Type.LOOP) || selectedNode.getType() == Type.LOOP) {
			//Then don't try to add a new loop, it will be useless (/absorbed)
			notTheseTypes.add(Type.LOOP);
		}
		Type parentType = Type.getRandomExcept(rng, notTheseTypes);

		//Now construct the new subtree
		Node oldParent = selectedNode.getParent();

		//And a sibling, which is not used if we selected a LOOP as parentType
		Node sibling;
		if (parentType == Type.LOOP) {
			sibling = new Node((XEventClass)null); //make it a loop tau
		} else {
			sibling = new Node(treeFactory.getRandomEventClass(rng));
		}

		//Always old = left just in case of loops 
		Node newParent = new Node(parentType, selectedNode,sibling);

		//If SEQ then sometimes swap
		if (newParent.getType() == Type.SEQ) {
			if (rng.nextBoolean()) {
				newParent.swapChildren();
			}
		}

		//Now attach to old parent OR set the newly created root
		if (oldParent == null) {
			setRoot(newParent);
		} else {
			oldParent.replaceChild(selectedNode, newParent);
		}

		assert checkTree();

		return true;
	}

	/**
	 * 
	 * @param mutationTree
	 * @param rng
	 * @return whether a node was removed. If the tree contains only LOOPS and
	 *         LEAFS (besides the root) then we can not remove a node
	 */
	public boolean mutateRemoveSubtreeRandom(Random rng) {
		String before = this.toString();
		//We cannot remove our single activity!
		if (getRoot().getType() == Type.LEAF)
			return false;

		assert checkTree();

		/*
		 * We can remove roots, leafs and LOOPS. We cannot remove our only
		 * 'real' leaf node but that was handled above. Furthermore, we can not
		 * remove the loop contents if that loop is the root...
		 */
		HashSet<Node> notThese = new HashSet<Node>();
		if (getRoot().getType() == Type.LOOP) {
			notThese.add(getRoot().getChild(0));
		}

		Node nodeToBeRemoved = getRandomNode(notThese, rng);

		//Just to be sure...
		if (nodeToBeRemoved == null)
			return false;

		//Get the parent of the node that is about to be removed
		Node parentOfNodeToBeRemoved = nodeToBeRemoved.getParent();

		/*-
		 * We have 4 cases:
		 * - If we selected the root which is a 
		 *   + leaf: do nothing
		 *   + loop: make left child new root
		 *   + other: make a child new root
		 * - If we selected a non-root node then we just remove the node from the parent
		 */
		if (parentOfNodeToBeRemoved == null) {
			//We have a ROOT
			if (nodeToBeRemoved.getType() == Type.LEAF) {
				//Which is a leaf, can't create empty trees so stop
				return false;
			} else { //not ROOT
				Node child;
				if (nodeToBeRemoved.getType() == Type.LOOP) {
					//For loops, we set the new root to be its left child
					child = nodeToBeRemoved.getChild(0);
				} else { //NOT LOOP
					//The root node is a 'normal' node
					//Select a child that survives
					child = nodeToBeRemoved.getChild(rng.nextInt(nodeToBeRemoved.countChildren()));
				}

				//Update root
				setRoot(child); //root is left child
				child.setParent(null); //parent of left child does not exist	
			}
		} else {
			//NOT ROOT

			//But if we are going to remove a leaf which parent is the root then do something special
			if (parentOfNodeToBeRemoved.getParent() == null) {
				//Determine the child to make the new king
				int childIndex = 0;
				if (parentOfNodeToBeRemoved.getChild(0).equals(nodeToBeRemoved)) {
					childIndex = 1;
				}

				Node child = parentOfNodeToBeRemoved.getChild(childIndex);
				setRoot(child);
				child.setParent(null);
			} else {
				Node newParent = parentOfNodeToBeRemoved.removeChild(nodeToBeRemoved);
				if (newParent != null && newParent.getParent() == null) {
					setRoot(newParent);
				}
			}
		}

		assert checkTree();

		return true;
	}

	public boolean mutateSingleNodeRandom(boolean swapChildren, TreeFactory treeFactory, Random rng) {
		//Select the node we will mutate
		Set<Node> notThese = new HashSet<Node>();
		notThese.addAll(getRoot().getNodesOfType(Type.LOOP));

		Node nodeToBeMutated = getRandomNode(notThese, rng);

		if (nodeToBeMutated == null)
			return false;

		/*
		 * We don't/can't/won't mutate LOOP operator nodes (these can either be
		 * added or removed but changing them is strange, where do we get right
		 * child???) AND the Loop's tau child
		 */
		while (nodeToBeMutated.getType() == Type.LEAF && nodeToBeMutated.getClazz() == null) {
			nodeToBeMutated = getRandomNode(notThese, rng);
		}

		//If the node is a leaf
		if (nodeToBeMutated.isLeaf()) {
			//assign new XEventClass reference
			XEventClass newClazz = treeFactory.getRandomEventClass(rng);
			//FIXME might go wrong for a single EC log
			while (newClazz.equals(nodeToBeMutated.getClazz()))
				newClazz = treeFactory.getRandomEventClass(rng);
			nodeToBeMutated.setClazz(newClazz);
		} else {
			//We don't want to change into a loop or our current operator type
			LinkedList<Type> notTheseTypes = new LinkedList<Node.Type>();
			notTheseTypes.add(Type.LOOP);
			notTheseTypes.add(nodeToBeMutated.getType());
			nodeToBeMutated.setType(Type.getRandomExcept(rng, notTheseTypes));
			if (swapChildren)
				nodeToBeMutated.swapChildren();
		}

		assert checkTree();

		return true;
	}

	/**
	 * Reduces this node's behavior if the observed behavior allows for this
	 * WITHOUT ANY DOUBT. Ensures unchanged fitness!!! For instance: Iff an AND
	 * node is only executed from left to right (and NEVER interleaved or right
	 * to left) then we change it to a SEQ.
	 * 
	 */
	public void reduceBehavior() {
		/*
		 * First keep reducing the root until it does not change anymore
		 */
		Node oldRoot;
		do {
			//Keep reducing the behavior of the root
			oldRoot = getRoot();
			setRoot(getRoot().reduceBehavior());
			//Until the returned root is the same node instance as before the call
		} while (oldRoot != getRoot());

		//Now call the reduce function on the children of the root
		LinkedList<Node> preorder = new LinkedList<Node>(getRoot().getPreorder());
		//We can skip the root so start i at 1
		for (int i = 1; i < preorder.size(); i++) {
			preorder.get(i).reduceBehavior();
		}
	}
}
