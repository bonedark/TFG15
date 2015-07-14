package org.processmining.plugins.joosbuijs.blockminer.genetic.conformancefitness;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.deckfour.xes.classification.XEventClass;
import org.processmining.plugins.joosbuijs.blockminer.genetic.conformancefitness.FunctionNode.FUNCTIONTYPE;

/**
 * A tree representation of a process model. This tree tries to implement the
 * RPST structure meaning it is a canonical n-ary tree with control flow nodes
 * and activities as leafs.
 * 
 * @author jbuijs
 * 
 */
public class Tree implements Comparable<Tree> {
	private boolean showMutationOperations = false;

	//The tree has a fitness
	private Fitness fitness;
	private Node root;
	private TreeFactory factory;
	private Random rng;

	private int size;

	public Tree(Random rng, TreeFactory factory, Node root) {
		setRoot(root);
		setFitness(null);
		setFactory(factory);
		this.rng = rng;
		fitness = new Fitness();
		size = root.countNodes();
	}

	public Tree(Tree tree) {
		//First clone the root
		Node newRoot;
		Node oldRoot = tree.root;
		if (oldRoot.isLeaf()) {
			newRoot = new EventClassNode((EventClassNode) oldRoot);
		} else {
			newRoot = new FunctionNode((FunctionNode) oldRoot);
		}

		//Then set everything
		setRoot(newRoot);
		setFactory(tree.getFactory());
		this.rng = tree.rng;
		setFitness(new Fitness(tree.getFitness()));
		size = newRoot.countNodes();
	}

	/**
	 * Instantiates a new tree from a given string. Returns null if the string
	 * contains errors
	 * 
	 * @param rng
	 * @param factory
	 * @param treeString
	 */
	public Tree(Random rng, TreeFactory factory, String treeString) {
		setFitness(null);
		setFactory(factory);
		this.rng = rng;
		fitness = new Fitness();

		this.root = instantiateFromString(treeString);
	}

	public void setFitness(Fitness fitness) {
		if (fitness == null)
			invalidateFitness();
		else
			this.fitness = fitness;
	}

	public Fitness getFitness() {
		return fitness;
	}

	/**
	 * Call this to invalidate the fitness of this tree (e.g. whenever you
	 * change something!)
	 */
	public void invalidateFitness() {
		this.fitness = null;
	}

	public void setRoot(Node root) {
		if (root != null)
			root.setParent(null);
		this.root = root;
	}

	public Node getRoot() {
		return root;
	}

	public String toString() {
		return root.toString();
	}

	public String toString(int level) {
		return root.toString(level);
	}

	/**
	 * Method that returns a set of all event classes under a certain node
	 * 
	 * @param candidate
	 * @return
	 */
	public HashSet<XEventClass> getEventClasses() {
		return new HashSet<XEventClass>(root.getEventClasses());
	}

	private Node instantiateFromString(String treeString) {
		//First cut the string in parts (e.g. from X(A,B) or A)
		//Get index of first and last ( ) and the , at same level

		//Test for an event node or operator node
		if (!treeString.contains("(")) {
			//Event node
			//Get the eventclass
			XEventClass eventClass = null;

			for (XEventClass ec : factory.getEventClasses()) {
				if (ec.toString().equals(treeString))
					eventClass = ec;
			}

			return new EventClassNode(eventClass);
		} else {
			//Operator node

			String firstpart = treeString.substring(0, treeString.indexOf('('));
			//Instantiate function
			FUNCTIONTYPE function = null;
			for (FUNCTIONTYPE type : FUNCTIONTYPE.values()) {
				if (type.toString().equals(firstpart))
					function = type;
			}

			//Create list of children
			String children = treeString.substring(treeString.indexOf('('), treeString.lastIndexOf(')') + 1);
			LinkedList<String> childList = splitString(children);

			//Now call ourselves for all children
			LinkedList<Node> childNodes = new LinkedList<Node>();
			for (String childString : childList) {
				childNodes.add(instantiateFromString(childString));
			}

			return new FunctionNode(childNodes, function);
		}
	}

	private LinkedList<String> splitString(String childString) {
		LinkedList<String> children = new LinkedList<String>();

		//First make sure the the string does not begin and and with ( or )
		/**/
		if (childString.charAt(0) == '(' && childString.charAt(childString.length() - 1) == ')') {
			childString = childString.substring(1, childString.length());
			childString = childString.substring(0, childString.length() - 1);
		}
		/**/

		int level = 0; //We start at level 0
		int groupStart = 0;
		//Loop throuh the string and return all parts split on , not inside ()
		for (int i = 0; i < childString.length(); i++) {
			switch (childString.charAt(i)) {
				case '(' :
					level++;
					break;
				case ')' :
					level--;
					break;
				case ',' :
					if (level == 0) {
						children.add(childString.substring(groupStart, i));
						groupStart = i + 1;
					}
			}
		}

		//And add the last part
		children.add(childString.substring(groupStart));

		return children;
	}

	/**
	 * Replaces the subtree at the given index
	 * 
	 * @param index
	 * @param subTree2
	 */
	public void replaceNode(int index, Node subTree2) {
		//ONLY USED BY CROSSOVER
		if (index == 0)
			setRoot(subTree2);
		else if (root.countNodes() < index) {
			FunctionNode fnode = (FunctionNode) root;
			fnode.replaceNode(index, subTree2);
		}
	}

	/**
	 * Returns the node at the provided index
	 * 
	 * @param index
	 * @return
	 */
	public Node getNode(int index) {
		if (root.countNodes() < index) {
			throw new IndexOutOfBoundsException("The provided index is out of bounds for this tree");
			//System.out.println("Invalid index");
		} else if (index == 0)
			//Be smart, return the root if they ask for it
			return root;
		else
			return root.getNode(index);
	}

	public int countNodes() {
		if (root == null)
			return 0;
		return root.countNodes();
	}

	public List<EventClassNode> getLeafs() {
		return root.getLeafs();
	}

	public Node getRandomNode() {
		if (this.countNodes() == 0)
			return null;
		return getNode(rng.nextInt(this.countNodes()));
	}

	public void setFactory(TreeFactory factory) {
		this.factory = factory;
	}

	public TreeFactory getFactory() {
		return factory;
	}

	/**
	 * Selects a random node
	 * 
	 * @param rng
	 * @param excludeRoot
	 *            Does the selected node need to have a parent?
	 * @param excludeLeafs
	 *            Does the selected node need to have a child?
	 * @return
	 */
	public Node selectRandomNode(boolean excludeRoot, boolean excludeLeafs) {
		//We cant exclude the root if there is no other choice
		if (excludeRoot && root.countChildren() == 0)
			return null;
		//We can't return something else than leafs if the root is a leaf
		if (excludeLeafs && root.isLeaf())
			return null;
		//If we should exclude both then one of the children of the root should be a non leaf
		FunctionNode fnode = (FunctionNode) root;
		if (excludeRoot && excludeLeafs && fnode.getDepth() < 2)
			return null;

		//Now its safe to try
		Node node = getRandomNode();

		//Now find a new node if this node does not have a parent and a child
		while ((node instanceof EventClassNode && excludeLeafs) || (node.getParent() == null && excludeRoot)) {
			//Select a new node
			node = getRandomNode();
		}

		return node;
	}

	/*
	 * HERE COME THE MUTATION FUNCTIONS!
	 */

	/**
	 * Mutates a random single node and changes its contents (not the children).
	 * In essence selects a random node and calls mutateSingleNode(node) on it
	 */
	public void mutateSingleNodeRandom() {
		mutateSingleNodeRandom(false);
	}

	/**
	 * Mutates a random single node and changes its contents (not the children).
	 * In essence selects a random node and calls mutateSingleNode(node) on it
	 * 
	 * @param changeOrderOfChildren
	 *            Whether to change the order of the children
	 */
	public void mutateSingleNodeRandom(boolean changeOrderOfChildren) {
		//Sanity check!
		if (root == null)
			return;

		invalidateFitness();
		// We will change a single node
		//Select a random node
		Node mutationNode = getRandomNode();
		//And call mutation function
		mutateSingleNode(mutationNode, changeOrderOfChildren);
	}

	/**
	 * Mutates the given node. If the node is a function node, the function WILL
	 * change. If the node is an eventclassnode then the eventclass WILL change
	 * 
	 * @param mutationNode
	 */
	public void mutateSingleNode(Node mutationNode) {
		mutateSingleNode(mutationNode, false);
	}

	private void mutateSingleNode(Node mutationNode, boolean changeOrderOfChildren) {
		if (showMutationOperations)
			System.out.println("   Mutating " + mutationNode.toString());
		invalidateFitness();
		checkTree();

		//Detect type and act accordingly
		if (mutationNode instanceof FunctionNode) {
			/*
			 * Create a new function node, add a new leaf and add the current
			 * node as the other child
			 */
			FUNCTIONTYPE newFunction = FUNCTIONTYPE.getRandom(rng);

			//Keep trying to select a different one
			FunctionNode mutationFNode = (FunctionNode) mutationNode;
			while (mutationFNode.getFunction() == newFunction) {
				newFunction = FUNCTIONTYPE.getRandom(rng);
			}

			mutationFNode.setFunction(newFunction);

			checkTree();

			//Now change order of children if necessary (and possible)
			if (changeOrderOfChildren && mutationFNode.countChildren() > 1) {
				if (mutationFNode.countChildren() > 1) {

					Node child1 = mutationFNode.getChild(rng.nextInt(mutationFNode.countChildren()));
					Node child2 = mutationFNode.getChild(rng.nextInt(mutationFNode.countChildren()));

					mutationFNode.swapChildren(child1, child2);
				}
				checkTree();
			}
		} else {
			EventClassNode ecNode = (EventClassNode) mutationNode;
			//We should change the contents of the ecnode but start with an identical one
			XEventClass newClass = ecNode.getEventClass();
			//Keep trying to select a different one
			while (newClass.equals(ecNode.getEventClass())) {
				newClass = factory.getRandomEventClass(rng);
			}
			//PS before you start wining, I KNOW I SHOULD DO A DO-WHILE... I just dont wanna

			//Update the ecNode
			ecNode.setEventClass(newClass);

			/*-* /
			EventClassNode newNode = new EventClassNode(factory.getRandomEventClass(rng));
			EventClassNode oldNode = (EventClassNode) mutationNode;
			//Keep trying to select a different one
			while (newNode.getEventClass().equals(oldNode.getEventClass())) {
				newNode = new EventClassNode(factory.getRandomEventClass(rng));
			}

			//If the old node is the root of our tree then we should handle it!
			if (root == oldNode) {
				root = newNode;
			} else {
				//Change the nodes
				FunctionNode parent = oldNode.getParent();

				//Replace the child
				parent.replaceNode(oldNode, newNode);
			}/**/
		}
	}

	/**
	 * Selects two random nodes and swaps them (one node might be a (grand)child
	 * of the other but this is ok)
	 */
	public void mutateSwapSubtreesRandom() {
		invalidateFitness();
		if (countNodes() > 2) {
			Node node1 = selectRandomNode(true, false);
			Node node2 = selectRandomNode(true, false);

			while (node1.equals(node2)) {
				node2 = selectRandomNode(true, false);
			}

			mutateSwapSubtrees(node1, node2);
		}
	}

	/**
	 * Swaps the two provided nodes within the tree. If nodes are nested then
	 * the lowest subtree will become a sibling of the larger subtree with a
	 * randomly chosen position before or after the larger subtree
	 * 
	 * @param node1
	 *            Swap point 1
	 * @param node2
	 *            Swap point 2
	 */
	public void mutateSwapSubtrees(Node node1, Node node2) {
		if (showMutationOperations)
			System.out.println("   Swapping " + node1.toString() + " - " + node2.toString());
		invalidateFitness();

		checkTree();

		//Sanity check: if node1 == node2 then don't attempt to do anything...
		if (node1.equals(node2))
			return;

		/*-
		 * There are different things that we should consider:
		 * First: are the 2 nodes nested, e.g. is node1 part 
		 *   of the subtree of node2 of vice versa?
		 * IF YES: remove the nested part and add that to the parent of the larger subtree
		 * IF NO: just swap the 2 nodes from their parents, operator type does NOT matter
		 */

		//Okay, so first test for nestedness
		if (node1.getPostorder().contains(node2) || node2.getPostorder().contains(node1)) {
			/*
			 * Iff nodes are nested then we must distinguish between the larger
			 * and smaller tree
			 */
			Node largerSubtree;
			Node smallerSubtree;

			/*
			 * Since we know they're nested, we can get the hierarchy by finding
			 * the larger tree that must contain the smaller tree
			 */
			if (node1.countNodes() > node2.countNodes()) {
				largerSubtree = node1;
				smallerSubtree = node2;
			} else {
				largerSubtree = node2;
				smallerSubtree = node1;
			}

			/*
			 * Okay, now we can have an additional special case: the larger
			 * subtree is actually the root of our tree! We need to handle this
			 * slightly differently
			 */
			if (largerSubtree.equals(root)) {
				//First, remove the smaller subtree from the tree
				mutateRemoveNode(smallerSubtree);

				//Now, we will create a new root that will have the smaller and larger subtrees as children
				LinkedList<Node> children = new LinkedList<Node>();
				children.add(smallerSubtree);
				children.add(rng.nextInt(2), largerSubtree); //add other child at pos 0 or 1

				FunctionNode newRoot = new FunctionNode(children, FUNCTIONTYPE.getRandom(rng));
				this.root = newRoot;
				checkTree();
			} else //larger subtree is not the root 
			{
				//First, remember where the larger subtree is attached to its parent
				FunctionNode parent = largerSubtree.getParent();
				int index = parent.getChildIndex(largerSubtree);

				/*
				 * Now determine whether we will add the smallersubtree to the
				 * left or right of the larger subtree
				 */
				if (rng.nextBoolean())
					index++; //we will add it to the right

				if (root.getLeafs().size() == 1) {
					//System.out.println("Wait, we want to follow how this goes!");
				}

				//System.out.println(toString());
				//Now we move the smaller tree around
				mutateRemoveNode(smallerSubtree);
				//System.out.println(toString());
				//checkTree();
				parent.addChild(index, smallerSubtree);

				/*
				 * It could be that the tree contains only one leaf, if then
				 * this only leaf is first removed and then added (as we do
				 * above) it will not be removed since empty trees are not
				 * allowed. If we then add the leaf to another node it will be
				 * removed.
				 */
				if (!largerSubtree.isLeaf() && largerSubtree.countNodes() == 1)
					mutateRemoveNode(largerSubtree);

				//System.out.println(toString());
				checkTree();
			}

			//Alternative implementation
			/*-* /
			//Remember where the smaller subtree is attached to the larger subtree
			FunctionNode smallsubTreeParent = smallerSubtree.getParent();
			int smallsubtreeIndex = smallsubTreeParent.getChildIndex(smallerSubtree);
			
			
			//set the smaller subtree at the location of the larger
			if(largerSubtree.equals(root)){
				this.root = smallerSubtree;
			}else{
				FunctionNode parent = largerSubtree.getParent();
				int index = parent.getChildIndex(largerSubtree);
				parent.removeChild(index);
				parent.addChild(index, smallerSubtree);
			}

			//now attach the 'larger' to the smaller subtree
			
			//We pick a leaf at whos location we add the larger subtree
			List<EventClassNode> leafs = smallerSubtree.getLeafs();
			EventClassNode swapleaf = leafs.get(rng.nextInt(leafs.size()));
			FunctionNode swapleafparent = swapleaf.getParent();
			int swapleafindex = swapleafparent.getChildIndex(swapleaf);
			
			//Now remove the smaller tree from the larger and add the swapped leaf
			swapleafparent.addChild(swapleafindex, largerSubtree);
			/**/

		} else//Non-nested nodes 
		{
			/*
			 * Just remove the two nodes from their parents and add them to the
			 * other parent, at the same location as the previous child
			 */
			FunctionNode parent1 = node1.getParent();
			FunctionNode parent2 = node2.getParent();

			/*
			 * And again a special case, if the parents are the same then
			 * removing one child influences the index of the other. We do have
			 * a special function that we can call on the parent to swap the
			 * children, lets do that instead...
			 */
			if (parent1.equals(parent2)) {
				parent1.swapChildren(node1, node2);
				checkTree();
			} else //parent1 <> parent2 
			{
				int index1 = parent1.getChildIndex(node1);
				int index2 = parent2.getChildIndex(node2);

				parent1.replaceNode(node1, node2);
				parent2.replaceNode(node2, node1);

				node1.setParent(parent2);
				node2.setParent(parent1);

				/*-
				parent1.addChild(index1, node2);
				parent2.addChild(index2, node1);
				
				parent1.removeChild(index1);
				parent2.removeChild(index2);
				/**/

				checkTree();
			}
		}
	}

	public void mutateSwapSubtreesOLD(Node node1, Node node2) {
		if (showMutationOperations)
			System.out.println("   Swapping " + node1.toString() + " - " + node2.toString());
		invalidateFitness();

		//If the nodes are of different type (e.g. 1 fnode and 1 ec node)
		if (!(node1.getClass() == node2.getClass())) {
			//Then check if they are nested. If so, do something special
			FunctionNode fnode;
			EventClassNode ecnode;

			if (node1 instanceof FunctionNode) {
				fnode = (FunctionNode) node1;
				ecnode = (EventClassNode) node2;
			} else {
				fnode = (FunctionNode) node2;
				ecnode = (EventClassNode) node1;
			}

			//If these nodes are nested then do something smart
			if (fnode.getLeafs().contains(ecnode)) {
				//remember the function node's parent
				FunctionNode fNodeParent = fnode.getParent();
				int fNodeParentIndex = fNodeParent.getChildIndex(fnode);

				//remove the ecnode and fnode from its parent
				ecnode.getParent().removeChild(ecnode);
				fNodeParent.removeChild(fnode);

				//Create a random function node with the ecnode and the former root as children
				LinkedList<Node> children = new LinkedList<Node>();
				children.add(ecnode);
				children.add(rng.nextInt(2), fnode); //at a random location

				FunctionNode intermediateFNode = new FunctionNode(children, FUNCTIONTYPE.getRandom(rng));

				//There is an additional special case: the fnode can be the root
				if (fnode.equals(root)) {
					//Now set this node as the new root
					root = intermediateFNode;
				} else {
					//Otherwise set the intermediateFNode as a new child at the location of the original FNode
					fNodeParent.addChild(fNodeParentIndex, intermediateFNode);
				}

				return;
			}//if nested

		}//if !=

		//If one of the nodes is the root then we should handle that one (correctly of course)
		//We make the non-root the new root and add the current root as a child of the new root at a random location
		if (node1.equals(root)) {
			((FunctionNode) node1).removeChild(node2);
			setRoot(node2);
			((FunctionNode) node2).addChildRandom(rng, node1);
			return;//we're done!
		} else if (node2.equals(root)) {
			//The other way round!
			((FunctionNode) node2).removeChild(node1);
			setRoot(node1);
			((FunctionNode) node1).addChildRandom(rng, node2);
			return;//we're done!
		}

		//Otherwise, swap!

		//Get the parents
		FunctionNode parent1 = node1.getParent();
		FunctionNode parent2 = node2.getParent();

		/*
		 * But wait, what if they share the same parent? Then we will replace
		 * node1 with node2. But if we then search for node2 to put node1 there
		 * we end up with node1 in its original place... There, we delegate this
		 * special case to the single parent
		 */
		if (parent1.equals(parent2)) {
			parent1.swapChildren(node1, node2);
		} else {

			//Now replace nodes by replacing the child nodes at each parent individually
			/*
			 * NOTE: we can not use the 'smarter' replace oldNode by newNode
			 * function since it will change nodes (e.g. set their parents) so
			 * equality will fail. Furthermore, we do this on the root node to
			 * do the swapping in the correct order
			 */

			//First get the indexes of the nodes in the tree
			int index1 = parent1.getChildIndex(node1);
			int index2 = parent2.getChildIndex(node2);

			//Then replace the children across parents
			parent1.removeChild(index1);
			//If node2 is currently part of
			parent2.removeChild(index2);

			//Since we just removed node2 its parent is removed so reset it
			//FIXME addChild refuses if there is nesting... How can we prevent nesting?
			parent1.addChild(index1, node2);
			node2.setParent(parent1);
			parent2.addChild(index2, node1);
		}
	}

	/**
	 * Adds a function node AND random eventclass node somewhere in the tree
	 */
	public void mutateAddNodeRandom() {
		invalidateFitness();
		mutateAddNodeRandom(factory.getRandomEventClass(rng));
	}

	/**
	 * Adds the provided classes as new nodes at random locations in the tree
	 * including operator nodes as parent
	 * 
	 * @param classesToAdd
	 */
	public void mutateAddNodeRandom(List<XEventClass> classesToAdd) {
		//For each class
		for (XEventClass eventClass : classesToAdd) {
			//And add it at a random location
			mutateAddNodeRandom(eventClass);
		}
	}

	/**
	 * Adds an event class node of the given eventClass at a random location in
	 * the tree
	 * 
	 * @param eventClass
	 */
	public void mutateAddNodeRandom(XEventClass eventClass) {
		invalidateFitness();

		//Select a random child
		EventClassNode newChild = new EventClassNode(eventClass);

		/*
		 * We will add ourselves below the selected node so he/she doesn't need
		 * a parent but does need children
		 */
		FunctionNode selNode = (FunctionNode) selectRandomNode(false, true);

		//If we can not find a suitable node then just introduce a new root
		if (selNode == null) {
			List<Node> children = new LinkedList<Node>();
			children.add(newChild);
			children.add(rng.nextInt(2), root);
			//Select a random function
			FUNCTIONTYPE newFunction = FUNCTIONTYPE.getRandom(rng);
			root = new FunctionNode(children, newFunction);
		} else
			selNode.addChildRandom(rng, newChild);
	}

	/**
	 * Inserts a new function (of newFunction-type) as child of the selected
	 * node with the newChild node as the other child and the current child
	 * survives
	 * 
	 * @param parentNode
	 *            The parent of the inserted node
	 * @param newChild
	 *            One of the children of the inserted node
	 * @param newFunction
	 *            The function of the inserted node
	 */
	public void mutateAddNode(FunctionNode parentNode, Node newChild, FUNCTIONTYPE newFunction) {
		//TODO test/validate/verify
		invalidateFitness();

		if (parentNode != null) {
			if (showMutationOperations)
				System.out.println("   Adding " + newChild.toString() + " to " + parentNode.toString() + " with "
						+ newFunction);
			//Select the position of the new child in the parent node
			int insertionChildIndex = 0;
			if (parentNode.countChildren() > 0) {
				insertionChildIndex = rng.nextInt(parentNode.countChildren());
			}

			/*-*/
			//Build a list with the new child node and the existing children at the pointed index
			LinkedList<Node> childList = new LinkedList<Node>();
			childList.add(newChild);

			//Get the child that is currently in that position
			Node oldChild = null;
			try {
				oldChild = parentNode.getChild(insertionChildIndex);
			} catch (IndexOutOfBoundsException e) {
				//Don't do anything, we just don't use the oldChild and keep it null
			}/**/

			//If there was an old child
			if (oldChild != null) {
				//Add it to the list at a random location
				childList.add(rng.nextInt(2), oldChild);
			}

			//Now update the parent node
			FunctionNode newNode = new FunctionNode(childList, newFunction);
			//Now replace the node at the index of the old one
			parentNode.replaceNode(oldChild, newNode);

			if (oldChild != null) {
				//Set the parent of the oldnode to the newnode
				oldChild.setParent(newNode);
			}

			/*-
			//OLD CODE (for correctness reference)
			if (rng.nextBoolean()) {
				//Err, left!
				Node oldLeft = parentNode.getLeft();
				//But where to place the old subtree and the new leaf child?
				if (rng.nextBoolean()) {
					//New child will be left
					parentNode.setLeft();
				} else {
					parentNode.setLeft(new FunctionNode(oldLeft, newChild, newFunction));
				}
			} else {
				//Err, right child!
				Node oldRight = parentNode.getRight();
				//But where to place the old subtree and the new leaf child?
				if (rng.nextBoolean()) {
					//New child will be left
					parentNode.setRight(new FunctionNode(newChild, oldRight, newFunction));
				} else {
					parentNode.setRight(new FunctionNode(oldRight, newChild, newFunction));
				}
			}/**/
		}
	}

	/**
	 * Removes a random node from the tree and replaces it with one of its
	 * children. Net effect: 1 function node less with one of its children also
	 * removed. Selects a random node and then call mutateRemoveNode on this
	 * node.
	 * 
	 * @param rng
	 */
	public void mutateRemoveNodeRandom() {
		invalidateFitness();
		//We need at least two leafs to be able to remove a node other than the root
		if (getRoot().getLeafs().size() > 1) {
			Node selectedNode = selectRandomNode(true, false);
			/*
			 * As long as we got a node that, even though its not the root,
			 * contains all the leafs of the tree, search for another node.
			 * There are 2 leafs that can be picked for instance... We made sure
			 * of that!
			 */
			while (selectedNode.getLeafs().size() == getRoot().getLeafs().size()) {
				selectedNode = selectRandomNode(true, false);
			}
			mutateRemoveNode(selectedNode);
		}
	}

	/**
	 * Removes randomly selected instances of the event class from the tree
	 * 
	 * @param classesToRemove
	 */
	public void mutateRemoveNode(List<XEventClass> classesToRemove) {
		for (XEventClass eventClass : classesToRemove) {
			//Get a node that 'implements' this event class
			List<EventClassNode> removalCandidates = root.getLeafsOfEventClass(eventClass);
			//And order the removal of a randomly selected node
			if (removalCandidates.size() > 0) {
				mutateRemoveNode(removalCandidates.get(rng.nextInt(removalCandidates.size())));
			} else {
				System.out.println("something wrong here...");
			}
		}
	}

	/**
	 * Removes the selected node from the tree
	 * 
	 * @param rng
	 * @param node
	 *            A node to be removed (!root)
	 */
	public void mutateRemoveNode(Node node) {
		checkTree();
		//Don't create empty trees (not good for nature) or trees without leafs (so depressing)
		if (getRoot().getDepth() <= 1 || getRoot().getLeafs().size() <= 1 || getRoot().getLeafs().size() <= 1)
			return;

		if (showMutationOperations)
			System.out.println("   Removing " + node.toString());
		invalidateFitness();

		FunctionNode parent = node.getParent();
		//We can't remove the root node so do nothing
		if (parent == null)
			return;

		//Now remove the given node from the parent
		parent.removeChild(node);

		checkTree();

		/*-
		//OLD CODE for correctness reference
		if (parent.getLeft() == node) {
				//Now select one of the children of the fnode
				if (keepLeftChild) {
					parent.setLeft(node.getLeft());
				} else {
					parent.setLeft(node.getRight());
				}
			} else {
				//Now select one of the children of the fnode
				if (keepLeftChild) {
					parent.setRight(node.getLeft());
				} else {
					parent.setRight(node.getRight());
				}
			}
		}/**/

	}

	/**
	 * Tries to find a function node with 3+ children, inserts a new fnode as a
	 * child of this node and takes some of its children
	 */
	public void mutateAddFNodeInBetweenRandom() {
		List<FunctionNode> candidateNodes = new LinkedList<FunctionNode>();
		for (Node node : root.getPostorder()) {
			if (node instanceof FunctionNode) {
				FunctionNode fnode = (FunctionNode) node;

				/*
				 * If the function node has more than 2 children, then we can
				 * add a new function node and take some children
				 */
				if (fnode.countChildren() > 2) {
					candidateNodes.add(fnode);
				}
			}
		}

		//Stop here if there are no candidates
		if (candidateNodes.size() == 0)
			return;

		//If there are candidates then select a random one
		FunctionNode fnode = candidateNodes.get(rng.nextInt(candidateNodes.size()));
		//And order that fnode to accept a new parent of some of it children (it can choose which ones itself)
		fnode.addFNodeForSomeChildren(rng);
	}

	/**
	 * Returns the post order traversal of the nodes in the tree
	 * 
	 * @return
	 */
	public List<Node> getPostorder() {
		if (root == null)
			return new LinkedList<Node>();
		return root.getPostorder();
	}

	/**
	 * Returns all the leaf event class nodes that represent the given event
	 * class
	 * 
	 * @param eventClass
	 * @return
	 */
	public List<EventClassNode> getLeafsOfEventClass(XEventClass eventClass) {
		return root.getLeafsOfEventClass(eventClass);
	}

	/**
	 * Checks the tree for (in)consistency
	 * 
	 * @return true if tree is correct
	 */
	public boolean checkTree() {
		boolean correct = true;
		List<Node> postorder = getPostorder();

		//Loop through the postorder
		for (Node node : postorder) {
			//Check for null parent and not beeing the root
			if (node.getParent() == null && node != getRoot()) {
				correct = false;
				System.out.println("ERROR: We found a child that does not refer to its parent");
				System.out.println("   Tree: " + toString());
				System.out.println("   Problematic node: " + node.toString() + " at tree index "
						+ getRoot().getIndexOf(node));
			}

			//Check for node INSTANCES occurring twice (or more) in the tree
			int occOfNode = 0;
			for (Node node2 : postorder) {
				if (node.equals(node2))
					occOfNode++;
			}
			if (occOfNode > 1) {
				correct = false;
				System.out.println("ERROR: This node occurs twice!!!" + node.toString());
			}

			//Check for function nodes with 0 children
			if (!node.isLeaf() && node.countChildren() == 0) {
				correct = false;
				System.out.println("ERROR: We found a function node without any children");
			}
		}
		return correct;
	}

	public int compareTo(Tree o) {
		return size -  o.size;
	}
}
