package org.processmining.plugins.boudewijn.treebasedreplay;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.processmining.plugins.astar.interfaces.Move;
import org.processmining.plugins.astar.interfaces.Tail;
import org.processmining.plugins.boudewijn.tree.Node;
import org.processmining.plugins.boudewijn.tree.Node.Type;
import org.processmining.plugins.boudewijn.treebasedreplay.AbstractTreeBasedAStar.VerboseLevel;

public class TreePostProcessor<H extends TreeHead, T extends Tail> {

	protected final Map<TIntList, TIntSet> marking2modelmove;
	protected final TObjectIntMap<TIntList> marking2visitCount;
	protected final AbstractTreeDelegate<T> delegate;
	protected TObjectIntMap<Node> node2occ;
	protected TObjectIntMap<Node> node2lowBo;
	protected TObjectIntMap<Node> node2upBo;
	protected final Node root;

	public TreePostProcessor(AbstractTreeDelegate<T> delegate, Node root, Map<TIntList, TIntSet> marking2modelmove,
			TObjectIntMap<TIntList> marking2visitCount) {
		this.delegate = delegate;
		this.root = root;
		this.marking2modelmove = marking2modelmove;
		this.marking2visitCount = marking2visitCount;

	}

	protected int calculateCostAndPostProcess(AbstractTreeBasedAStar<H, T, ?> aStar, VerboseLevel verbose,
			TreeRecord r, int frequency) {

		node2occ = new TObjectIntHashMap<Node>();
		node2lowBo = new TObjectIntHashMap<Node>();
		node2upBo = new TObjectIntHashMap<Node>();

		int cost = 0;
		cost += frequency * r.getCostSoFar();

		List<TreeRecord> alignment = getAlignment(r);

		// update behaviorcounters for precision
		updateNodes(alignment, frequency);

		for (Node n : root.getPreorder()) {
			BehaviorCounter cnt = n.getBehavior();
			if (n.isLeaf()) {
				if (node2occ.get(n) == 0) {
					cnt.notUsed += frequency;
				}
				continue;
			}
			int cl = node2occ.get(n.getLeft());
			int cr = node2occ.get(n.getRight());
			if (cl + cr == 0) {
				// node is not used in this trace
				cnt.notUsed += frequency;
				continue;
			}
			if (n.getType() == Type.LOOP) {
				if (node2occ.get(n) > 1) {
					cnt.behavedAsR += frequency * (node2occ.get(n) - 1);
				}
			}
		}

		cost += processAlignment(aStar, alignment, frequency);

		return cost;
	}

	protected int processAlignment(AbstractTreeBasedAStar<H, T, ?> aStar, List<TreeRecord> alignment, int frequency) {
		// update visisted nodes for generalization
		// and at the same time remove the base costs for moving, so only
		// the punishments remain.
		int cost = 0;
		for (TreeRecord rec : alignment) {
			if (rec.getModelMove() != Move.BOTTOM) {
				// get the marked nodes
				TreeHead head = aStar.getState(rec.getPredecessor().getState()).getHead();
				TIntList enabledMoves = head.getModelMoves(rec, delegate);
				TIntSet used = marking2modelmove.get(enabledMoves);
				if (used == null) {
					used = new TIntHashSet();
					marking2modelmove.put(enabledMoves, used);
				}
				marking2visitCount.adjustOrPutValue(enabledMoves, 1, 1);
				int m = rec.getModelMove();
				used.add(m);
				if (rec.getMovedEvent() == Move.BOTTOM) {
					if (rec.getModelMove() >= delegate.numNodes()) {
						Effect effect = delegate.getEffectForColumnNumber(rec.getModelMove());
						cost -= frequency * effect.moveCount();
					} else {
						cost -= frequency;
					}
				} else {
					cost -= frequency;
				}

			} else {
				// move on log only
				cost -= frequency;
			}
		}
		return cost;
	}

	protected List<TreeRecord> getAlignment(TreeRecord r) {
		return TreeRecord.getHistory(r);
	}

	protected void updateNodes(List<TreeRecord> alignment, int frequency) {

		//Traverse the alignment backwards
		for (int i = alignment.size(); i-- > 0;) {
			TreeRecord record = alignment.get(i);
			if (record.getModelMove() == Move.BOTTOM) {
				// log only move, irrelevant here
				continue;
			}

			if ((record.getMovedEvent() != Move.BOTTOM)) {
				// Synchronous move
				adjustBounds(delegate.getNode((short) record.getModelMove()), i);
				continue;
			}
			if ((record.getModelMove() < delegate.numNodes())) {
				// move model only on non-loop leaf node.
				delegate.getNode((short) record.getModelMove()).getBehavior().moveModelOnly++;
				adjustBounds(delegate.getNode((short) record.getModelMove()), i);
				continue;
			}

			Effect effect = delegate.getEffectForColumnNumber(record.getModelMove());
			Node n = delegate.getNode(effect.getNode());
			// The node n is either the root node, or a loop leaf.
			assert (n.getParent() == null || (n.getParent().getType() == Type.LOOP && n.getParent().getRight() == n));
			if (n.getParent() != null) {
				n = n.getParent();
			}

			// consolidate bounds.
			// For all nodes in the subtree of n, count the types of ordering on AND and OR nodes and reset the bounds.
			for (Node node : n.getPreorder()) {
				if (!node2upBo.containsKey(node)) {
					continue;
				}
				BehaviorCounter cnt = node.getBehavior();
				if ((node.getType() == Type.AND || node.getType() == Type.OR) && node2upBo.containsKey(node.getRight())
						&& node2upBo.containsKey(node.getLeft())) {
					// check the overlap of the children.
					if (node2upBo.get(node.getLeft()) < node2lowBo.get(node.getRight())) {
						cnt.behavedAsSEQLR += frequency;
					} else if (node2upBo.get(node.getRight()) < node2lowBo.get(node.getLeft())) {
						cnt.behavedAsSEQRL += frequency;
					} else {
						cnt.behavedAsAND += frequency;
					}
				}
				if (n.getType() == Type.SEQ) {
					cnt.behavedAsSEQLR += frequency;
				}
				if (!node2upBo.containsKey(node.getRight())) {
					cnt.behavedAsL += frequency;
				} else if (!node2lowBo.containsKey(node.getLeft())) {
					cnt.behavedAsR += frequency;
				}

				//				&& node2upBo.containsKey(node.getLeft()) && node2upBo
				//								.containsKey(node.getRight()))) {
				//				}

			}
			for (Node node : n.getPreorder()) {
				if (node == n) {
					continue;
				}
				node2upBo.remove(node);
				node2lowBo.remove(node);
			}
			// now, add one to each occurrance of a node on the path from the elements
			// of effect to the root or n.getParent().
			TIntIterator it = effect.iterator();
			Set<Node> nodes = new HashSet<Node>();
			while (it.hasNext()) {
				int e = it.next();
				if (e >= 0) {
					countNodes(delegate.getNode(e), n, nodes);
				}
			}
			for (Node node : nodes) {
				node2occ.adjustOrPutValue(node, 1, 1);
			}

		}

	}

	private void adjustBounds(Node node, int i) {
		if (!node2lowBo.containsKey(node)) {
			node2lowBo.put(node, i);
			node2upBo.put(node, i);
		} else {
			node2lowBo.put(node, i);
		}
		if (node.getParent() != null) {
			adjustBounds(node.getParent(), i);
		}
	}

	private void countNodes(Node node, Node stopAt, Set<Node> nodes) {
		nodes.add(node);
		if (!node.equals(stopAt)) {
			countNodes(node.getParent(), stopAt, nodes);
		}
	}
}
