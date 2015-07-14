package org.processmining.plugins.boudewijn.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.processmining.plugins.boudewijn.treebasedreplay.BehaviorCounter;

public class Node {
	private boolean showDetails = false;

	private Type type;
	private Node left;
	private Node right;
	private Node parent;
	private XEventClass eventClass;
	private BehaviorCounter behavior = new BehaviorCounter();

	public enum Type {
		//LOOP: left child is the loop contents, right child is DUMMY (eventclass with null class)
		//LEAF: no children but eventClass is set

		//There is some meaning to this ordering: SEQ is easiest, LEAF is weirdest and LOOP is kinda weird
		SEQ, AND, XOR, OR, LOOP, LEAF;

		/**
		 * Returns a random operator type EXCEPT LEAF
		 * 
		 * @param rng
		 * @return random Type EXCEPT LEAF
		 */
		public static Type getRandom(Random rng) {
			LinkedList<Type> notTheseTypes = new LinkedList<Type>();
			return getRandomExcept(rng, notTheseTypes);
		}

		public static Type getRandomExcept(Random rng, Collection<Type> notTheseTypes) {
			if (notTheseTypes == null)
				notTheseTypes = new LinkedList<Node.Type>();

			//Exclude the LEAF always since it is not a real operator type
			notTheseTypes.add(Type.LEAF);
			//			notTheseTypes.add(Type.OR); //temporary
			//notTheseTypes.add(Type.LOOP); //temporary

			if (notTheseTypes.size() == Type.values().length)
				return null; //There is no solution

			Type[] types = values();
			Type selectedType = types[rng.nextInt(types.length)];
			while (notTheseTypes.contains(selectedType)) {
				selectedType = types[rng.nextInt(types.length)];
			}
			return selectedType;
		}

		public static Type getRandomExceptBiased(Random rng) {
			Type[] types = values();

			Type selectedType = null;
			boolean doAgain = true;
			while (doAgain) {
				selectedType = types[rng.nextInt(types.length)];
				//LEAF are not allowed
				if (selectedType == Type.LEAF)
					doAgain = true;
				//ORs and LOOPs only 1/10 times
				else if (selectedType == Type.OR || selectedType == Type.LOOP) {
					if (rng.nextInt(10) > 8)
						doAgain = false;
				} else
					//The rest is OK
					doAgain = false;
			}
			return selectedType;
		}
	}

	/**
	 * Instantiate a node of a given type with 2 children
	 * 
	 * @param type
	 *            Node type (NOT LEAF)
	 * @param left
	 * @param right
	 */
	public Node(Type type, Node left, Node right) {
		//Check for leafs with children
		if (type.equals(Type.LEAF))
			throw new IllegalArgumentException("You can not instantiate a leaf node with 2 children.");
		//And loops with a right child which is not a leaf node without an event class
		else if (type.equals(Type.LOOP) && (!right.isLeaf() || right.getClazz() != null))
			throw new IllegalArgumentException(
					"For loops only left childs can be set, right child should be a leaf node with event class 'null'");

		//If we are a loop and the provided left child is also a loop then absorb
		if (type.equals(Type.LOOP) && left.getType().equals(Type.LOOP)) {
			//Tell the provided loop node that he has been orphaned
			left.setParent(null);
			//Our left child will be the left child of the provided loop
			left = left.getChild(0);
		}

		this.type = type;
		this.left = left;
		left.setParent(this);
		this.right = right;
		right.setParent(this);
		this.parent = null;
		this.eventClass = null;
	}

	/**
	 * Instantiates a leaf node representing a given event class
	 * 
	 * @param clazz
	 */
	public Node(XEventClass clazz) {
		this.type = Type.LEAF;
		this.eventClass = clazz;
		this.left = null;
		this.right = null;
		this.parent = null;
	}

	/**
	 * Creates a new node instance by DEEP cloning the provided node
	 * 
	 * @param node
	 */
	public Node(Node node) {
		//Start with parent is null
		this.parent = null;
		this.type = node.type;
		//Be ~smart here, there might not be a left/right child
		this.left = node.left == null ? null : new Node(node.left);
		this.right = node.left == null ? null : new Node(node.right);
		this.eventClass = node.eventClass;
		this.behavior = new BehaviorCounter(node.behavior);

		//Now set the parents of the children
		if (left != null)
			left.setParent(this);
		if (right != null)
			right.setParent(this);
	}

	public Type getType() {
		return type;
	}

	/**
	 * Change the operator type of the given node. Can only be done from/to non
	 * LEAF and LOOP nodes
	 * 
	 * @param type
	 */
	public void setType(Type type) {
		if (this.type == Type.LEAF)
			throw new IllegalArgumentException("This node is currently a leaf, you can not change this type");
		if (this.type == Type.LOOP)
			throw new IllegalArgumentException("This node is currently a loop, you can not change this type");

		//otherwise adopt the new type
		this.type = type;
	}

	public Node getLeft() {
		return left;
	}

	public Node getRight() {
		return right;
	}

	public Node getParent() {
		return parent;
	}

	public XEventClass getClazz() {
		return eventClass;
	}

	public void setClazz(XEventClass newClazz) {
		this.eventClass = newClazz;
	}

	public boolean isLeaf() {
		return type == Type.LEAF;
	}

	/**
	 * Sets the parent of this node ONLY when the provided parent actually has
	 * us as a child (e.g. first set the child relation, then call parent
	 * setting)
	 * 
	 * @param parent
	 * @return TRUE if the parent has been updated, false when the above
	 *         condition was not met
	 */
	public boolean setParent(Node parent) {
		if (parent == null || parent.hasChild(this)) {
			this.parent = parent;
			return true;
		} else
			return false;
	}

	/**
	 * Counts the total number of nodes, including this node
	 */
	public int countNodes() {
		//TODO improve: only really count when subtree changes, otherwise return saved count

		if (isLeaf()) {
			//For a leaf only count ourselves
			return 1;
		} else {
			//For all others count ourselves and the size of both children
			return 1 + left.countNodes() + right.countNodes();
		}
	}

	/**
	 * Returns a pre-order (e.g. this-left-right) of this node
	 * 
	 * @return
	 */
	public Collection<Node> getPreorder() {
		List<Node> preOrder = new ArrayList<Node>();
		addToPreOrder(preOrder);
		return preOrder;
	}

	/**
	 * Recursive function to addd this-left-right to the provided collection
	 * 
	 * @param preOrder
	 */
	private void addToPreOrder(Collection<Node> preOrder) {
		preOrder.add(this);
		if (left != null) {
			left.addToPreOrder(preOrder);
			right.addToPreOrder(preOrder);
		}
	}

	/**
	 * Counts the number of (direct) children
	 * 
	 * @return
	 */
	public int countChildren() {
		return isLeaf() ? 0 : 2;
	}

	/**
	 * Returns the child at index i
	 * 
	 * @param i
	 *            child index 0=left, 1=right
	 * @return
	 */
	public Node getChild(int i) {
		return i == 0 ? left : right;
	}

	/**
	 * Returns a string representation of this whole subtree
	 */
	public String toString() {
		return type
				+ (isLeaf() ? ": " + (eventClass == null ? "EXIT" : eventClass.toString()) : "( " + left.toString()
						+ " , " + right.toString() + " )");
	}

	/**
	 * Returns the node at the provided index of this subtree
	 * 
	 * @param index
	 *            0 <= index < countNodes()
	 * @return
	 */
	public Node getNode(int index) {
		//We are node #0
		if (index == 0)
			return this;
		//If the left child contains more than index-1 we ask our left child
		else if (index <= left.countNodes())
			return left.getNode(index - 1);
		//Otherwise, ask our right child if he contains enough nodes.
		//The index is of course now minus the size of the left subtree and ourselves
		else if (index <= (left.countNodes() + right.countNodes()))
			return right.getNode(index - left.countNodes() - 1);
		else
			//Overflow...
			return null;
	}

	/**
	 * Tries to replace the oldNode with the provided newNode. Will fail if this
	 * node does not recognize the oldNode OR when this node is a loop and the
	 * provided oldNode is its right (dummy) child.
	 * 
	 * @param oldNode
	 * @param newNode
	 * @return whether we performed the replacement
	 */
	public boolean replaceChild(Node oldNode, Node newNode) {
		if (left.equals(oldNode)) {
			//If we are a loop and the provided new child is also a loop
			if (type.equals(Type.LOOP) && newNode.getType().equals(Type.LOOP)) {
				//Then we absorb the loop (since looping a loop is nonsense)
				//The provided newNode will be orphaned
				newNode.setParent(null);
				//And we take its left child as our own
				newNode = newNode.getChild(0);
				//And continue as normal
			}

			//Replace our left child
			left = newNode;
			//Only reset the parent if it is still pointing to us, otherwise it was already updated (we hope)
			if (oldNode.getParent() != null && oldNode.getParent().equals(this))
				oldNode.setParent(null);
			left.setParent(this);
			return true;
		} else if (right.equals(oldNode) && type != Type.LOOP) {
			//Replace our right child IFF we are not a loop thingy
			right = newNode;
			//Only reset the parent if it is still pointing to us, otherwise it was already updated (we hope)
			if (oldNode.getParent() != null && oldNode.getParent().equals(this))
				oldNode.setParent(null);
			right.setParent(this);
			return true;
		}

		return false;
	}

	/**
	 * Returns true if the provided node is a direct child of this node
	 * 
	 * @param childNode
	 * @return
	 */
	public boolean hasChild(Node childNode) {
		if (type == Type.LEAF)
			return false;
		else if (left.equals(childNode) || right.equals(childNode))
			return true;
		return false;
	}

	public int getDepth() {
		if (this.type == Type.LEAF)
			return 1;
		else
			return Math.max(left.getDepth(), right.getDepth()) + 1;
	}

	/**
	 * Adds a new child to the END of the list of current children. The node
	 * WILL SPLIT to make sure each node has exactly 2 children
	 * 
	 * @param newChild
	 */
	public boolean addChild(Node newChild) {
		return addChild(countChildren(), newChild);
	}

	/**
	 * Adds a new child at the provided index. The node WILL SPLIT to make sure
	 * each node has exactly 2 children
	 * 
	 * @param childIndex
	 * @param newChild
	 */
	public boolean addChild(int childIndex, Node newChild) {
		//If we are a loop then we don't know what to do with a new child...
		if (type == Type.LOOP)
			return false;

		//index is 0, 1 or more
		switch (childIndex) {
			case 0 :
				//Add new child to the left of the left node
				left = new Node(type, newChild, left);
				left.setParent(this);
				return true;
			case 1 :
				//Add the new child to the right of the left node
				//(we don't add it to the left of the right node, we prefer left here)
				left = new Node(type, left, newChild);
				left.setParent(this);
				return true;
			default :
				//Add the new child to the right of the right node
				right = new Node(type, right, newChild);
				right.setParent(this);
				return true;
		}
	}

	/**
	 * Removes the child at the provided child index. Afterwards removes itself
	 * from between its parent and the remaining child. Returns the new parent
	 * of the left-over child
	 * 
	 * @param childIndex
	 *            Child index of the node to be removed
	 * @return the new parent node of the surviving child (which MIGHT BE the
	 *         new root)
	 */
	public Node removeChild(int childIndex) {
		//If we are a loop and the left child needs to be removed
		if (type == Type.LOOP && childIndex == 0 && parent != null) {
			//then we have no purpose at all so remove ourselves from our parent
			return parent.removeChild(this);
		}

		//Otherwise, continue as normal

		Node survivingChild;
		if (childIndex == 0)
			survivingChild = right;
		else
			survivingChild = left;

		//If we are/became root
		if (parent == null) {
			//Make the surviving child the new root
			survivingChild.setParent(null);
			return survivingChild;
		} else {
			parent.replaceChild(this, survivingChild);
		}

		return parent;
	}

	/**
	 * Removes the given child. Afterwards removes itself from between its
	 * parent and the remaining child. Returns the new parent of the left-over
	 * child which is possibly the new root of the tree.
	 * 
	 * @param child
	 *            node to be removed
	 * @return the new parent node of the surviving child (which MIGHT BE the
	 *         new root, so check for parent == NULL and then update the tree)
	 */
	public Node removeChild(Node child) {
		if (left.equals(child))
			return removeChild(0);
		else if (right.equals(child))
			return removeChild(1);
		else
			return null;
	}

	public static Node fromString(String s, XEventClasses classes) {
		return fromString(s.toCharArray(), new IntPointer(), classes);
	}

	public static Node fromString(char[] buf, IntPointer p, XEventClasses classes) {
		String val = new String();
		while (buf[p.i] != '(' && buf[p.i] != ':') {
			val += buf[p.i++];
		}
		// read the type
		Type type = Type.valueOf(val);
		// skip 2 chars
		p.i += 2;
		val = "";
		if (type == Type.LEAF) {
			while (p.i + 1 < buf.length && buf[p.i + 1] != ',' && buf[p.i + 1] != ')') {
				val += buf[p.i++];
			}
			XEventClass clazz = classes.getByIdentity(val);
			if (buf.length > p.i + 1) {
				p.i += (buf[p.i + 1] == ',' ? 3 : 2);
			}
			return new Node(clazz);
		}
		Node n = new Node(type, fromString(buf, p, classes), fromString(buf, p, classes));
		if (buf.length > p.i + 1) {
			p.i += (buf[p.i + 1] == ',' ? 3 : 2);
		}
		return n;
	}

	private static final class IntPointer {
		public int i = 0;
	}

	/**
	 * Count all leaf nodes, including LOOP tau's
	 * 
	 * @return
	 */
	public int countLeafNodes() {
		if (isLeaf()) {
			return 1;
		} else {
			return getRight().countLeafNodes() + getLeft().countLeafNodes();
		}
	}

	/**
	 * Counts the leaf nodes that are representing 'real' activities and are not
	 * LOOP tau's
	 * 
	 * @return
	 */
	public int countActivityLeafNodes() {
		if (isLeaf()) {
			if (eventClass == null)
				return 0;
			else
				return 1;
		} else {
			return getRight().countActivityLeafNodes() + getLeft().countActivityLeafNodes();
		}
	}

	public int countNodesOfType(Type type) {
		int count = this.type == type ? 1 : 0;

		if (left != null) {
			count += left.countNodesOfType(type);
		}

		if (right != null) {
			count += right.countNodesOfType(type);
		}

		return count;
	}

	/**
	 * Returns a all child-leafs mapped to activities
	 * 
	 * @param mapped
	 * 
	 * @return
	 */
	public Collection<Node> getLeafs(boolean mapped) {
		List<Node> leafs = new ArrayList<Node>();
		addToLeafs(leafs, mapped);
		return leafs;
	}

	/**
	 * Recursive function to add this-left-right to the provided collection
	 * 
	 * @param preOrder
	 */
	private void addToLeafs(Collection<Node> leafs, boolean mapped) {
		if (isLeaf()) {
			if (!mapped || eventClass != null) {
				leafs.add(this);
			}
		} else {
			left.addToLeafs(leafs, mapped);
			right.addToLeafs(leafs, mapped);
		}
	}

	public void setBehavior(BehaviorCounter behavior) {
		this.behavior = behavior;
	}

	public BehaviorCounter getBehavior() {
		return behavior;
	}

	/**
	 * Resets the behavior counter for this node
	 */
	public void resetBehaviorCounter() {
		resetBehaviorCounter(false);
	}

	/**
	 * Resets the behavior counter for this node and possibly the whole subtree
	 * 
	 * @param cascade
	 *            if TRUE also reset the behavior Counter of the whole subtree.
	 */
	public void resetBehaviorCounter(boolean cascade) {
		behavior = new BehaviorCounter();
		if (cascade) {
			if (left != null)
				left.resetBehaviorCounter(cascade);
			if (right != null)
				right.resetBehaviorCounter(cascade);
		}
	}

	/**
	 * Reduces this nodes behavior if the observed behavior allows for this
	 * WITHOUT ANY DOUBT. Ensures unchanged fitness!!! For instance: Iff an AND
	 * node is only executed from left to right (and NEVER interleaved or right
	 * to left) then we change it to a SEQ.
	 * 
	 * @return Node the new node
	 */
	public Node reduceBehavior() {
		//Determine the type of this node
		switch (type) {
			case LEAF :
				//We never do anything.
				//Even if we were always skipped, we don't remove ourselves since this would improve the fitness
				break;
			case SEQ :
				//We never change.
				break;
			case OR :
				//We are always special, so also here...
				//We are an XOR, AND and our self: an OR
				/*
				 * For simplicity (well, not according to our own simplicity but
				 * anywho) we duplicate the code from above here...
				 */

				//So, first handle our explicit, own, private, OR stuff
				//Check if we only performed as an AND
				if (behavior.behavedAsL == 0 && behavior.behavedAsR == 0) {
					if (behavior.behavedAsAND > 0 || (behavior.behavedAsSEQLR > 0 && behavior.behavedAsSEQRL > 0)) {
						if (showDetails) {
							System.out.println("Changed " + type + " to AND");
						}
						type = Type.AND;
					}
				}

				//Now see if we have the same symptoms a XOR and AND can have...
				//$FALL-THROUGH$
			case AND :
				//IFF we executed our children non-interleaved and always in a particular order
				if (behavior.behavedAsAND == 0 && behavior.behavedAsL == 0 && behavior.behavedAsR == 0) {
					//We were never a real AND
					if (behavior.behavedAsSEQLR > 0 && behavior.behavedAsSEQRL == 0) {
						//And always executed left->right
						if (showDetails) {
							System.out.println("Changed " + type + " to SEQ");
						}
						type = Type.SEQ;
						break;
					} else if (behavior.behavedAsSEQRL > 0 && behavior.behavedAsSEQLR == 0) {
						//And always executed right->left
						//So, swap children :)
						Node intermediate = left;
						left = right;
						right = intermediate;
						//And become a sequence
						if (showDetails) {
							System.out.println("Changed " + type + " to SEQ and swapped children");
						}
						type = Type.SEQ;
						break;
					}
				}
				if (type == Type.AND) {
					break;
				}
				//$FALL-THROUGH$
			case XOR :
				/*-*/
				//IFF we only executed one of the branches and never the other, we should remove ourselves.
				//We should not check this for nodes that have AND-like behavior
				if (behavior.behavedAsAND == 0 && behavior.behavedAsSEQLR == 0 && behavior.behavedAsSEQRL == 0) {
					if (behavior.behavedAsL > 0 && behavior.behavedAsR == 0) {
						//We only executed our left child so remove ourselves and let that one survive
						if (parent != null) {
							parent.replaceChild(this, left);
							//And make ourselves disappear
							if (showDetails) {
								System.out.println(type + ": Removed R child");
							}
							parent = null;
						} else {
							left.setParent(null);
							if (showDetails) {
								System.out.println(type + ": made left child root");
							}
							return left;
						}
						break;
					} else if (behavior.behavedAsR > 0 && behavior.behavedAsL == 0) {
						//We only executed our right child so remove ourselves and let that one survive
						if (parent != null) {
							parent.replaceChild(this, right);
							//And make ourselves disappear
							if (showDetails) {
								System.out.println(type + ": removed L root");
							}
							parent = null;
						} else {
							right.setParent(null);
							if (showDetails) {
								System.out.println(type + ": made right child root");
							}
							return right;
						}
						break;
					}
				}/**/
				//Stop here for OR, AND and XOR
				break;
			case LOOP :
				//If a loop is executed only once (and never not) then it should not be a loop...
				//This is open for discussion by the way....... (why 1? What if always executed X times?)
				if (behavior.behavedAsR == 0 && behavior.notUsed == 0) {
					if (parent != null) {
						parent.replaceChild(this, left);
						//And make ourselves disappear
						if (showDetails) {
							System.out.println(type + ": removed");
						}
						parent = null;
					} else {
						left.setParent(null);
						if (showDetails) {
							System.out.println(type + ": removed and L is new root");
						}
						return left;
					}
				}
				break;
			default :
				break;
		}

		//We always return ourselves if not told otherwise
		return this;
	}

	/**
	 * Returns all nodes in the tree of the given type
	 * 
	 * @param type
	 *            Type to search for
	 * @return Collection of nodes of that type
	 */
	public Collection<Node> getNodesOfType(Type type) {
		HashSet<Type> set = new HashSet<Type>();
		set.add(type);
		return getNodesOfType(set);
	}

	/**
	 * Returns all nodes in the tree of the given types
	 * 
	 * @param types
	 *            Types to search for
	 * @return Collection of nodes of the given types
	 */
	public Collection<Node> getNodesOfType(Collection<Type> types) {
		List<Node> set = new ArrayList<Node>();
		if (types.contains(type)) {
			if (!(type == Type.LEAF && eventClass == null))
				set.add(this);
		}

		if (type != Type.LEAF) {
			set.addAll(left.getNodesOfType(types));
			set.addAll(right.getNodesOfType(types));
		}

		return set;
	}

	public String printBehaviorRecursive() {
		String output = this.toString() + "\n";
		output += behavior.toString();
		output += "------------------ \n ";

		if (left != null) {
			output += left.printBehaviorRecursive();
		}
		if (right != null) {
			output += right.printBehaviorRecursive();
		}

		return output;
	}

	/**
	 * Left becomes right and right left... right?
	 */
	public void swapChildren() {
		if (!(type == Type.LOOP)) {
			Node oldLeft = left;
			left = right;
			right = oldLeft;
		}
	}
}
