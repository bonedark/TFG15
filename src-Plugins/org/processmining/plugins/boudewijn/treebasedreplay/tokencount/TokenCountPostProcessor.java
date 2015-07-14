package org.processmining.plugins.boudewijn.treebasedreplay.tokencount;

import gnu.trove.list.TIntList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.List;
import java.util.Map;

import org.processmining.plugins.astar.interfaces.Move;
import org.processmining.plugins.astar.interfaces.Tail;
import org.processmining.plugins.boudewijn.tree.Node;
import org.processmining.plugins.boudewijn.tree.Node.Type;
import org.processmining.plugins.boudewijn.treebasedreplay.AbstractTreeBasedAStar;
import org.processmining.plugins.boudewijn.treebasedreplay.AbstractTreeBasedAStar.VerboseLevel;
import org.processmining.plugins.boudewijn.treebasedreplay.BehaviorCounter;
import org.processmining.plugins.boudewijn.treebasedreplay.TreeRecord;

public class TokenCountPostProcessor<H extends TokenCountHead, T extends Tail> {

	protected final Map<TIntList, TIntSet> marking2modelmove;
	protected final TObjectIntMap<TIntList> marking2visitCount;
	protected final AbstractTokenCountDelegate<T> delegate;
	protected TObjectIntMap<Node> node2occ;
	protected TObjectIntMap<Node> node2lowBo;
	protected TObjectIntMap<Node> node2upBo;
	protected final Node root;

	public TokenCountPostProcessor(AbstractTokenCountDelegate<T> delegate, Node root,
			Map<TIntList, TIntSet> marking2modelmove, TObjectIntMap<TIntList> marking2visitCount) {
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
		int moveCount = updateNodes(aStar, alignment, frequency);

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
		}

		processAlignment(aStar, alignment, frequency);
		cost -= frequency * moveCount;
		assert cost >= 0;
		//System.out.println(root.printBehaviorRecursive());
		return cost;
	}

	protected List<TreeRecord> getAlignment(TreeRecord r) {
		return TreeRecord.getHistory(r);
	}

	protected void processAlignment(AbstractTreeBasedAStar<H, T, ?> aStar, List<TreeRecord> alignment, int frequency) {
		// update visisted nodes for precision
		// and at the same time remove the base costs for moving, so only
		// the punishments remain.
		for (TreeRecord rec : alignment) {
			if (rec.getModelMove() != Move.BOTTOM) {
				// get the marked nodes
				TokenCountHead head = aStar.getState(rec.getPredecessor().getState()).getHead();

				TIntList enabledMoves = head.getModelMoves(rec, delegate);
				TIntSet used = marking2modelmove.get(enabledMoves);
				if (used == null) {
					used = new TIntHashSet();
					marking2modelmove.put(enabledMoves, used);
				}
				marking2visitCount.adjustOrPutValue(enabledMoves, 1, 1);
				int m = rec.getModelMove();
				used.add(m);

			}
		}

	}

	protected int updateNodes(AbstractTreeBasedAStar<H, T, ?> aStar, List<TreeRecord> alignment, int frequency) {

		int moveCount = 0;

		//Traverse the alignment backwards
		int pos = Integer.MAX_VALUE;
		for (int i = alignment.size(); i-- > 0;) {
			assert (pos >= 0);
			TreeRecord record = alignment.get(i);
			if (record.getModelMove() != Move.BOTTOM) {
				processMove(record.getModelMove(), record.getMovedEvent(), frequency, i);
				if (record.getModelMove() >= 0 && record.getModelMove() < delegate.numLeafs()
						&& delegate.isLoopLeaf(record.getModelMove())) {
					// no change to the move count, as this is an internal "cleanup" move.
				} else {
					moveCount++;
				}
			} else {
				moveCount++;
			}
			pos--;
			for (int j = record.getPredecessor().getInternalMoves().length; j-- > 0;) {
				int move = record.getPredecessor().getInternalMoves()[j];
				processMove(move + 2 * delegate.numLeafs(), -1, frequency, -1);
				moveCount++;
			}
		}
		return moveCount;
	}

	private void processMove(int modelMove, int movedEvent, int frequency, int location) {
		if (modelMove < delegate.numLeafs()) {
			Node n = delegate.getNode(modelMove);
			if (location > 0) {
				adjustBounds(n, location);
			}
			node2occ.adjustOrPutValue(n, 1, 1);
			// moved leafNode.
			n.getBehavior().behavedAsL += frequency;
			if ((movedEvent == Move.BOTTOM)) {
				// move model only 
				n.getBehavior().moveModelOnly++;
			}

		} else {
			Node node = delegate.getNode(modelMove / 3);
			node2occ.adjustOrPutValue(node, 1, 1);
			// move model only 
			node.getBehavior().moveModelOnly += frequency;

			BehaviorCounter cnt = node.getBehavior();
			if (node.getType() == Type.AND || (node.getType() == Type.OR && modelMove % 3 == TokenCountHead.B)) {
				// check the overlap of the children.
				if (node2upBo.get(node.getLeft()) < node2lowBo.get(node.getRight())) {
					cnt.behavedAsSEQLR += frequency;
				} else if (node2upBo.get(node.getRight()) < node2lowBo.get(node.getLeft())) {
					cnt.behavedAsSEQRL += frequency;
				} else {
					cnt.behavedAsAND += frequency;
				}
			} else if (node.getType() == Type.SEQ) {
				cnt.behavedAsSEQLR += frequency;
			} else if (node.getType() == Type.LOOP) {
				cnt.behavedAsL += frequency;
			} else if (node.getType() == Type.XOR || node.getType() == Type.OR) {
				if (modelMove % 3 == TokenCountHead.L) {
					cnt.behavedAsL += frequency;
				} else if (modelMove % 3 == TokenCountHead.R) {
					cnt.behavedAsR += frequency;
				}
			}
			for (Node n : node.getPreorder()) {
				if (node == n) {
					continue;
				}
				node2upBo.remove(n);
				node2lowBo.remove(n);
			}
			if (location > 0) {
				adjustBounds(node, location);
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
}
