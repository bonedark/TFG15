package org.processmining.plugins.joosbuijs.blockminer.models;

import org.deckfour.xes.classification.XEventClass;
import org.processmining.plugins.joosbuijs.blockminer.clustering.ClusterRelationshipDiscovery.RELATIONSHIPTYPE;

public class BinaryTreeNode {
	private BinaryTreeNode left;
	private BinaryTreeNode right;

	/*
	 * Secretly, this can only be of type RELATIONSHIPTYPE or XEventClass
	 * enforced by the constructors. Furthermore, if this is a relationship
	 * then it is always between the left and right child. If it is an event
	 * class then it does not have children. (yes, its a strange and cruel
	 * world)
	 */
	private Object contents;

	public BinaryTreeNode(XEventClass eventClass) {
		contents = eventClass;
	}

	public BinaryTreeNode(BinaryTreeNode left, BinaryTreeNode right, RELATIONSHIPTYPE relation) {
		this.left = left;
		this.right = right;
		this.contents = relation;
	}

	public boolean isLeafNode() {
		if (left == null && right == null)
			return true;
		else
			return false;
	}

	public RELATIONSHIPTYPE getRelation() {
		try {
			return (RELATIONSHIPTYPE) contents;
		} catch (Exception e) {
			return null;
		}
	}

	public XEventClass getEventClass() {
		try {
			return (XEventClass) contents;
		} catch (Exception e) {
			return null;
		}
	}

	public BinaryTreeNode getLeftChild() {
		return left;
	}

	public BinaryTreeNode getRightChild() {
		return right;
	}

	public String toString() {
		if (this.isLeafNode()) {
			return getEventClass().toString();
		} else {
			return "(" + getLeftChild().toString() + ")" + getRelation().toString() + "("
					+ getRightChild().toString() + ")";
		}
	}
}