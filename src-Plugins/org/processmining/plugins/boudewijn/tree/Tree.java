package org.processmining.plugins.boudewijn.tree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

import org.processmining.plugins.boudewijn.tree.Node.Type;

public class Tree implements Comparable<Tree> {

	private Node root;
	private double overallFitness;
	private double replayFitness;
	private double precision;
	private double simplicity;
	private double generalization;
	private int size;

	public Tree(Node root) {
		this.setRoot(root);
		resetFitness();
		size = root.countNodes();
	}

	/**
	 * Deep copies the provided tree into a new tree instance
	 * 
	 * @param tree
	 *            Tree to be deep copied
	 */
	public Tree(Tree tree) {
		this.setRoot(new Node(tree.getRoot()));
		this.replayFitness = tree.getReplayFitness();
		this.precision = tree.getPrecision();
		this.overallFitness = tree.getOverallFitness();
		this.simplicity = tree.getSimplicity();
		this.generalization = tree.getGeneralization();
		this.size = root.countNodes();
	}

	public int countNodes() {
		return size;
	}

	public Node getNode(int index) {
		return root.getNode(index);
	}

	public void replaceNode(int index, Node node) {
		Node oldNode = getNode(index);
		Node parent = oldNode.getParent();
		parent.replaceChild(oldNode, node);
	}

	public boolean checkTree() {
		//Check for duplication of node instances
		ArrayList<Node> preorder = new ArrayList<Node>(root.getPreorder());

		//If we have a tree of size one
		if (preorder.size() == 1) {
			Node root = preorder.get(0);
			if (root.getType() == Type.LEAF && root.getClazz() == null) {
				System.out.println("We have a Loop-Exit Tau as the tree root!!!");
			}
		}

		//Loop through the nodes in the preorder
		for (int i = 0; i < preorder.size(); i++) {
			Node nodei = preorder.get(i);
			//And then compare with the whole preorder again
			for (int j = 0; j < preorder.size(); j++) {
				Node nodej = preorder.get(j);
				//Check for duplications
				if (nodej.equals(nodei) && i != j) {
					return false;
				}
				//Check for child-parent mismatches
				if (nodei.hasChild(nodej) && (nodej.getParent() == null || !nodej.getParent().equals(nodei))) {
					return false;
				}
			}

			//Check for event class leafs with children
			if (nodei.isLeaf() && nodei.countChildren() > 0)
				return false;

			//Check for loops with not an EXIT child as right child
			if (nodei.getType() == Type.LOOP
					&& !(nodei.getChild(1).getType() == Type.LEAF && nodei.getChild(1).getClazz() == null))
				return false;
			//And EXIT nodes not as the right child of a loop
			if (nodei.getType() == Type.LEAF && nodei.getClazz() == null) {
				if (nodei.getParent() == null || nodei.getParent().getType() != Type.LOOP
						|| nodei.getParent().getChild(1) != nodei) {
					return false;
				}
			}

		}

		return true;
	}

	/**
	 * Returns a randomly selected node from the tree
	 * 
	 * @param rng
	 * @return
	 */
	public Node getRandomNode(Random rng) {
		return getRandomNode(null, rng);
	}

	/**
	 * Returns a randomly selected node from the tree
	 * 
	 * @param notThese
	 *            These nodes are never returned
	 * @param rng
	 * @return The selected node or NULL if there is node
	 */
	public Node getRandomNode(Set<Node> notThese, Random rng) {
		//Now build a list of all nodes
		LinkedList<Node> list = new LinkedList<Node>(root.getPreorder());

		//Now remove all nodes that are not allowed
		if (notThese != null)
			list.removeAll(notThese);

		//Also remove EXIT leafs, never select those
		Iterator<Node> it = list.iterator();
		while (it.hasNext()) {
			Node node = it.next();
			if (node.getType() == Type.LEAF && node.getClazz() == null) {
				it.remove();
			}
		}

		if (list.size() == 0) {
			//we can't find a candidate...
			return null;
		}//Otherwise

		//Return one node in the list
		return list.get(rng.nextInt(list.size()));
	}

	public String toString() {
		return String.format("[ f:%2.3f p:%2.3f s:%2.3f g:%2.3f ] ", getReplayFitness(), getPrecision(),
				getSimplicity(), getGeneralization()) + root.toString();
	}

	public String toCanonicalString() {
		return root.toString();
	}

	/*
	 * GETTERS AND SETTERS
	 */

	public void setRoot(Node root) {
		this.root = root;
	}

	public Node getRoot() {
		return root;
	}

	public void setReplayFitness(double fitness) {
		this.replayFitness = fitness;
	}

	public double getReplayFitness() {
		return replayFitness;
	}

	/**
	 * Invalidates the fitness, precision, simplicity and generalization values
	 * and resets the behavior counters of all nodes
	 */
	public void resetFitness() {
		this.overallFitness = -1;
		this.replayFitness = -1;
		this.precision = -1;
		this.simplicity = -1;
		this.generalization = -1;

		root.resetBehaviorCounter(true);
	}

	/**
	 * Returns true if all specific fitness dimensions are set to a value != -1.
	 * Assumes that the behavior counter then is also set
	 */
	public boolean isFitnessSet() {
		return overallFitness != -1 && replayFitness != -1 && precision != -1 && simplicity != -1
				&& generalization != -1;
	}

	public void setPrecision(double precision) {
		this.precision = precision;
	}

	public double getPrecision() {
		return precision;
	}

	public void setSimplicity(double simplicity) {
		this.simplicity = simplicity;
	}

	public double getSimplicity() {
		return simplicity;
	}

	public void setGeneralization(double generalization) {
		this.generalization = generalization;
	}

	public double getGeneralization() {
		return generalization;
	}

	public void setOverallFitness(double overallFitness) {
		this.overallFitness = overallFitness;
	}

	public double getOverallFitness() {
		return overallFitness;
	}

	public int compareTo(Tree o) {
		return size - o.size;
	}

}
