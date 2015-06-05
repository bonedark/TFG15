package org.processmining.plugins.boudewijn.treebasedreplay;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TShortSet;
import gnu.trove.set.hash.TShortHashSet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.tue.storage.CompressedHashSet;
import nl.tue.storage.Deflater;
import nl.tue.storage.EqualOperation;
import nl.tue.storage.HashOperation;
import nl.tue.storage.Inflater;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.model.XEvent;
import org.processmining.plugins.astar.algorithm.State;
import org.processmining.plugins.astar.interfaces.Move;
import org.processmining.plugins.astar.interfaces.Record;
import org.processmining.plugins.astar.interfaces.Tail;
import org.processmining.plugins.boudewijn.tree.Node;
import org.processmining.plugins.boudewijn.tree.Node.Type;

public abstract class AbstractTreeDelegate<T extends Tail> implements TreeDelegate<TreeHead, T> {

	protected static final int NEVINT = -1;
	protected static final short NEVSHRT = -1;
	protected final TreeHeadCompressor<T> headCompressor;
	protected final XEventClasses classes;

	protected final TObjectIntMap<Node> node2index;
	protected final Node[] index2node;
	protected final TShortSet[] nodeIndex2act;

	//private final TIntIntMap node2act;
	protected final int nodes;

	protected int[] node2cost;

	//	private final int[] minimalNodeCost;
	protected final TIntIntMap loop2parent;

	protected final TIntObjectMap<List<Effect>> node2effects;
	protected final TIntObjectMap<TIntList> blockingIntervals;
	protected final TIntObjectMap<Effect> columnNumber2effect;
	protected int vars;
	private final AStarAlgorithm algorithm;
	protected final int scaling = 10;

	public AbstractTreeDelegate(AStarAlgorithm algorithm, Node root, Map<Node, Integer> node2Cost, int threads) {
		this.algorithm = algorithm;
		int leafs = root.countLeafNodes();
		this.nodes = (leafs + (root.isLeaf() ? 0 : 1));
		this.classes = algorithm.getClasses();
		this.headCompressor = new TreeHeadCompressor<T>(nodes, (short) classes.size());
		this.node2cost = new int[nodes];

		index2node = new Node[nodes];
		node2index = new TObjectIntHashMap<Node>(nodes, 0.5f, NEVINT);
		nodeIndex2act = new TShortSet[nodes];
		loop2parent = new TIntIntHashMap(10, 0.5f, NEVINT, NEVINT);
		blockingIntervals = new TIntObjectHashMap<TIntList>(nodes, 0.5f, NEVINT);
		columnNumber2effect = new TIntObjectHashMap<Effect>(nodes, 0.5f, NEVINT);

		int i = 0;
		for (Node n : root.getPreorder()) {
			if (n == root || n.isLeaf()) {
				index2node[i] = n;
				node2index.put(n, i);
				Integer cost = node2Cost.get(n);
				node2cost[i] = (cost == null ? 0 : 1 + scaling * cost);
				blockingIntervals.put(i, new TIntArrayList());

				TShortSet set = new TShortHashSet();

				if (n.isLeaf() && n.getClazz() != null) {

					short act = getIndexOf(n.getClazz());
					set.add(act);
				}

				nodeIndex2act[i] = set;

				i++;
			}
		}

		// defaults to false
		node2effects = new TIntObjectHashMap<List<Effect>>(nodes, 0.5f, NEVINT);
		for (Node n : root.getPreorder()) {
			if (n == root || n.isLeaf()) {
				int index = node2index.get(n);
				if (n.getType() == Type.LOOP) {
					loop2parent.put(node2index.get(n.getRight()), index);
				}
				if (n.getClazz() != null) {
					ArrayList<Effect> list = new ArrayList<Effect>(1);
					list.add(new Effect(0, index, 0));
					node2effects.put(index, list);
				}
			}
		}

		// compute the effects for this tree
		//		computeEffectsAndBlocking(root, node2index.get(root));

		getActivityLeafs(root, 0, new HashSet<Node>(), 0);

		for (int node = 0; node < nodes; node++) {
			TIntIterator it = blockingIntervals.get(node).iterator();
			while (it.hasNext()) {
				// The node "node" is blocked by the node "blockedBy".
				// If the node "node" occurs (or one of its effects occurs)
				int blockedBy = it.next();
				if (index2node[blockedBy].getClazz() == null) {
					it.remove();
					for (Effect effect : node2effects.get(node)) {
						int insPt = effect.binarySearch(blockedBy);
						if (insPt < 0) {
							effect.insert(-insPt - 1, -blockedBy);
						}
					}
				}
			}
		}

		vars = numEventClasses();
		for (int node = (nodes - 1); node >= 0; node--) {
			vars += 2;
			if (index2node[node].getClazz() == null) {
				vars += node2effects.get(node).size() - 1;
			}

			TIntList iv = blockingIntervals.get(node);
			for (int bbi = iv.size(); bbi-- > 0;) {
				int bb = iv.get(bbi);
				TIntList iv2 = blockingIntervals.get(bb);
				for (int li = bbi; li-- > 0;) {
					// remove the element at index li, if it is contained in the
					// blockedBy list of bb.
					if (iv2.binarySearch(iv.get(li)) >= 0) {
						iv.removeAt(li);
						bbi--;
					}
				}
			}
		}

	}

	private List<Effect> getActivityLeafs(Node n, int storeAt, Set<Node> blocked, int count) {
		List<Effect> result = null;
		if (n.isLeaf()) {
			int i = node2index.get(n);
			TIntList bl = new TIntArrayList(blocked.size());
			for (Node node : blocked) {
				bl.add(node2index.get(node));
			}
			bl.sort();
			blockingIntervals.put(i, bl);
			Effect list = new Effect(1, storeAt, 0);
			if (i != storeAt) {
				list.add(i);
			}
			result = new ArrayList<Effect>(1);
			result.add(list);
			if (storeAt >= 0) {
				// store the result for root.
				node2effects.put(storeAt, result);
			}
			return result;
		}

		List<Effect> left = getActivityLeafs(n.getLeft(), -1, blocked, count + 1);
		List<Effect> right = null;

		switch (n.getType()) {
			case LEAF :
				assert false;
				//$FALL-THROUGH$
			case LOOP :
				List<Effect> loopLeafs = getActivityLeafs(n.getLeft(), -1, blocked, count + 1);
				int r = node2index.get(n.getRight());
				for (Effect l : loopLeafs) {
					l.add(r);
					l.setNode(r);
				}
				node2effects.put(r, loopLeafs);
				//$FALL-THROUGH$
			case SEQ :
				HashSet<Node> newBlocked = new HashSet<Node>(blocked);
				newBlocked.addAll(n.getLeft().getLeafs(false));
				right = getActivityLeafs(n.getRight(), -1, newBlocked, count + 1);
				//$FALL-THROUGH$
			case OR :
			case AND :
				if (right == null) {
					right = getActivityLeafs(n.getRight(), -1, blocked, count + 1);
				}
				result = new ArrayList<Effect>(left.size() * right.size());
				for (Effect ll : left) {
					for (Effect rl : right) {
						Effect list = new Effect(ll.size() + rl.size(), storeAt, 1 + ll.moveCount() + rl.moveCount());
						list.addAll(ll);
						list.addAll(rl);
						result.add(list);
					}
				}
				if (n.getType() != Type.OR) {
					break;
				}
				//$FALL-THROUGH$
			case XOR :
				if (right == null) {
					right = getActivityLeafs(n.getRight(), -1, blocked, count + 1);
				}
				if (result == null) {
					result = new ArrayList<Effect>(left.size() + right.size());
				}
				result.addAll(0, left);
				result.addAll(0, right);
				for (Effect e : left) {
					e.incMoveCount();
				}
				for (Effect e : right) {
					e.incMoveCount();
				}
		}

		if (storeAt >= 0) {
			// store the result for root.
			for (Effect l : result) {
				l.setNode(storeAt);
			}
			node2effects.put(storeAt, result);
		}
		return result;
	}

	public Effect getEffectForColumnNumber(int number) {
		return columnNumber2effect.get(number);
	}

	//	public int getMinimalCost(int node) {
	//		return minimalNodeCost[node];
	//	}

	public Record createInitialRecord(TreeHead head) {
		return new TreeRecord(0, null);
	}

	public Inflater<TreeHead> getHeadInflater() {
		return headCompressor;
	}

	public Deflater<TreeHead> getHeadDeflater() {
		return headCompressor;
	}

	public void setStateSpace(CompressedHashSet<State<TreeHead, T>> statespace) {
	}

	public XEventClass getClassOf(XEvent e) {
		return classes.getClassOf(e);
	}

	public short getIndexOf(XEventClass c) {
		return algorithm.getIndexOf(c);
	}

	public short numEventClasses() {
		return (short) classes.size();
	}

	public Type getFunctionType(int c) {
		Node n = index2node[c];
		return n.getType();
	}

	public int[] getSuccessors(int node) {
		Node n = index2node[node];
		int[] children = new int[n.countChildren()];
		for (int i = 0; i < n.countChildren(); i++) {
			children[i] = node2index.get(n.getChild(i));
		}
		return children;
	}

	public int getCostFor(int node, int activity) {
		if (node == Move.BOTTOM) {
			// logMove only
			return getLogMoveCost(activity);
		}
		if (activity == Move.BOTTOM) {

			if (node >= nodes) {
				// effect node
				return getEffectForColumnNumber(node).moveCount();
			}
			// modelMove only
			Node n = index2node[node];

			// internal nodes are free.
			if (n.isLeaf()) {
				return getModelMoveCost(node);
			}
		}
		// synchronous move. Don't penalize that.
		return 1;
	}

	public TShortSet getActivitiesFor(int node) {
		return nodeIndex2act[node];
	}

	public Node getNode(int m) {
		return index2node[m];
	}

	public XEventClass getEventClass(short act) {
		return algorithm.getEventClass(act);
	}

	public int getIndexOf(Node root) {
		return node2index.get(root);
	}

	public boolean isLeafOrLoop(int modelMove) {
		return index2node[modelMove].isLeaf() || index2node[modelMove].getType() == Type.LOOP;
	}

	public boolean isLeaf(int modelMove) {
		return index2node[modelMove].isLeaf();
	}

	public boolean isLoopLeafOrLoop(int modelMove) {
		return isLeafOrLoop(modelMove) && index2node[modelMove].getClazz() == null;
	}

	public boolean isLoopLeaf(int modelMove) {
		return index2node[modelMove].isLeaf() && index2node[modelMove].getClazz() == null;
	}

	public int getLogMoveCost(int i) {
		return 1 + scaling * algorithm.getLogMoveCost(i);
	}

	public int getModelMoveCost(int node) {
		return node2cost[node];
	}

	int getParentOfLoop(int loopLeaf) {
		// return the parent of a leaf node that is under a loop node.
		return loop2parent.get(loopLeaf);
	}

	public int numEffects() {
		return vars - numNodes() - numEventClasses();
	}

	/**
	 * Returns the positive effect a node has on a marking. Each element in the
	 * returned list is a sorted list of node indices, such that for each of
	 * these nodes, a token is produced if the given node is executed.
	 * 
	 * @param node
	 * @return
	 */
	public List<Effect> getEffects(int node) {
		return node2effects.get(node);
	}

	public int numNodes() {
		return nodes;
	}

	public boolean isLoop(int node) {
		return index2node[node].getType() == Type.LOOP;
	}

	public boolean isBlockedModelMove(int modelMove, TIntList marked) {
		// The blocking intervals of each node suggest whether a node is blocked
		// by part of the marking.
		TIntList interval = blockingIntervals.get(modelMove);
		if (interval.isEmpty() || marked.isEmpty()) {
			// This node is never blocked.
			return false;
		}

		// See if the sorted list "marked" contains elements from the sorted "interval"
		TIntList l1, l2;
		if (interval.get(0) < marked.get(0)) {
			l1 = interval;
			l2 = marked;
		} else {
			l1 = marked;
			l2 = interval;
		}

		int from = l1.binarySearch(l2.get(0));
		int to = l2.binarySearch(l1.get(l1.size() - 1));
		if (from >= 0 || to >= 0) {
			return true;
		}

		// potential overlap is between l1[-from-1] to l1[size-1] and l2[0] to l2[-to-1]

		int idx = 0;
		for (int i = -from - 1; i < l1.size(); i++) {
			idx = l2.binarySearch(l1.get(i), idx, -to - 1);
			if (idx >= 0) {
				return true;
			}
			idx = -idx - 1;
		}
		// no overlap.
		return false;
	}

	public HashOperation<State<TreeHead, T>> getHeadBasedHashOperation() {
		return headCompressor;
	}

	public EqualOperation<State<TreeHead, T>> getHeadBasedEqualOperation() {
		return headCompressor;
	}

	public int getScaling() {
		return scaling;
	}

	public String toString(short modelMove, short activity) {
		return "(" + modelMove + ")";
	}

}
