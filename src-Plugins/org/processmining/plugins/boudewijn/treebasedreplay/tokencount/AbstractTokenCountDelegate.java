package org.processmining.plugins.boudewijn.treebasedreplay.tokencount;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TObjectShortMap;
import gnu.trove.map.hash.TObjectShortHashMap;
import gnu.trove.set.TShortSet;
import gnu.trove.set.hash.TShortHashSet;

import java.util.Map;

import nl.tue.storage.CompressedHashSet;
import nl.tue.storage.Deflater;
import nl.tue.storage.EqualOperation;
import nl.tue.storage.HashOperation;
import nl.tue.storage.Inflater;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.model.XEvent;
import org.processmining.plugins.astar.algorithm.State;
import org.processmining.plugins.astar.interfaces.Move;
import org.processmining.plugins.astar.interfaces.Record;
import org.processmining.plugins.astar.interfaces.Tail;
import org.processmining.plugins.boudewijn.tree.Node;
import org.processmining.plugins.boudewijn.tree.Node.Type;
import org.processmining.plugins.boudewijn.treebasedreplay.AStarAlgorithm;
import org.processmining.plugins.boudewijn.treebasedreplay.TreeDelegate;
import org.processmining.plugins.boudewijn.treebasedreplay.TreeRecord;

public abstract class AbstractTokenCountDelegate<T extends Tail> implements TreeDelegate<TokenCountHead, T> {

	protected final short nodes;
	protected final AStarAlgorithm algorithm;
	protected final short[] leftChildren;
	protected final short[] rightChildren;
	protected final short[] parents;
	protected final boolean[] loopLeaf;
	protected final Node[] index2node;
	protected final short leafs;
	protected final TObjectShortMap<Node> node2index;
	protected final short classes;
	protected final int scaling = 1000;
	protected final int[] node2cost;
	protected final TShortSet[] nodeIndex2act;
	protected final TokenCountHeadCompressor<T> headCompressor;
	protected final boolean greedy;
	protected final int numLeafsAndLoops;

	public AbstractTokenCountDelegate(AStarAlgorithm algorithm, Node root, Map<Node, Integer> node2Cost, int threads,
			boolean greedy) {
		this.algorithm = algorithm;
		// labelling of nodes should be done leafs first
		// the number of nodes is maximum Short.MAX_VALUE -1
		this.greedy = greedy;

		this.nodes = (short) root.countNodes();
		if (nodes == Short.MAX_VALUE || nodes < 1) {
			throw new IllegalArgumentException("Tree too large. At most " + (Short.MAX_VALUE - 1) + " nodes allowed.");
		}

		this.node2index = new TObjectShortHashMap<Node>(nodes, 0.5f, (short) -1);
		this.leafs = (short) root.countLeafNodes();
		this.leftChildren = new short[nodes - leafs];
		this.rightChildren = new short[nodes - leafs];
		this.parents = new short[nodes];
		this.index2node = new Node[nodes];
		this.loopLeaf = new boolean[leafs];
		this.node2cost = new int[leafs];
		this.nodeIndex2act = new TShortSet[leafs];

		short i = 0;
		for (Node leaf : root.getLeafs(true)) {
			node2index.put(leaf, i);
			index2node[i] = leaf;

			TShortSet set = new TShortHashSet();
			// mapped leaf node
			Integer cost = node2Cost.get(leaf);
			node2cost[i] = (cost == null ? 1 : 1 + getScaling() * cost);

			short act = algorithm.getIndexOf(leaf.getClazz());
			set.add(act);
			nodeIndex2act[i] = set;
			i++;
		}
		TShortSet emptySet = new TShortHashSet();
		for (Node leaf : root.getLeafs(false)) {
			if (leaf.getClazz() != null) {
				continue;
			}
			node2index.put(leaf, i);
			index2node[i] = leaf;
			loopLeaf[i] = true;
			node2cost[i] = 0;
			nodeIndex2act[i] = emptySet;
			i++;
		}

		// label the remaining nodes breath first.
		if (!root.isLeaf()) {

			//			for (Node node : root.getNodesOfType(Type.XOR)) {
			//				node2index.put(node, i);
			//				index2node[i] = node;
			//				i++;
			//			}
			//			for (Node node : root.getNodesOfType(Type.OR)) {
			//				node2index.put(node, i);
			//				index2node[i] = node;
			//				i++;
			//			}
			for (Node node : root.getNodesOfType(Type.LOOP)) {
				node2index.put(node, i);
				index2node[i] = node;
				i++;
			}
			numLeafsAndLoops = i;
			//			for (Node node : root.getNodesOfType(Type.SEQ)) {
			//				node2index.put(node, i);
			//				index2node[i] = node;
			//				i++;
			//			}
			//			for (Node node : root.getNodesOfType(Type.AND)) {
			//				node2index.put(node, i);
			//				index2node[i] = node;
			//				i++;
			//			}

			if (root.getType() != Type.LOOP) {
				node2index.put(root, i);
				index2node[i] = root;
				label(root, i);
			} else {
				label(root, --i);
			}
		} else {
			numLeafsAndLoops = 1;
		}

		for (Node node : root.getPreorder()) {
			short id = node2index.get(node);
			if (!isLeaf(id)) {
				short l = node2index.get(node.getLeft());
				short r = node2index.get(node.getRight());
				parents[l] = id;
				parents[r] = id;
				rightChildren[id - leafs] = r;
				leftChildren[id - leafs] = l;
			}
			if (node.getParent() == null) {
				parents[id] = -1;
			}
		}

		this.classes = (short) algorithm.getClasses().size();
		this.headCompressor = new TokenCountHeadCompressor<T>(this, nodes, classes);
	}

	private short label(Node node, short i) {
		if (node.getType() == Type.LOOP) {
			if (!node.getLeft().isLeaf()) {
				i++;
				node2index.put(node.getLeft(), i);
				index2node[i] = node.getLeft();
				return label(node.getLeft(), i);
			} else {
				return i;
			}
		}
		assert (!node.isLeaf() && node.getType() != Type.LOOP);
		if (!node.getLeft().isLeaf() && node.getLeft().getType() != Type.LOOP) {
			i++;
			node2index.put(node.getLeft(), i);
			index2node[i] = node.getLeft();
		}
		if (!node.getLeft().isLeaf()) {
			i = label(node.getLeft(), i);
		}
		if (!node.getRight().isLeaf() && node.getRight().getType() != Type.LOOP) {
			i++;
			node2index.put(node.getRight(), i);
			index2node[i] = node.getRight();
		}
		if (!node.getRight().isLeaf()) {
			i = label(node.getRight(), i);
		}

		return i;
	}

	public Record createInitialRecord(TokenCountHead head) {
		int moves = head.getMovesMade() == null ? 0 : head.getMovesMade().size();

		return new TreeRecord(moves, head.getMovesMade());
	}

	public Inflater<TokenCountHead> getHeadInflater() {
		return headCompressor;
	}

	public Deflater<TokenCountHead> getHeadDeflater() {
		return headCompressor;
	}

	public HashOperation<State<TokenCountHead, T>> getHeadBasedHashOperation() {
		return headCompressor;
	}

	public EqualOperation<State<TokenCountHead, T>> getHeadBasedEqualOperation() {
		return headCompressor;
	}

	public void setStateSpace(CompressedHashSet<State<TokenCountHead, T>> statespace) {
	}

	public boolean isLeaf(int node) {
		return node < leafs;
	}

	public boolean isMapped(int node, int activity) {
		return nodeIndex2act[node].contains((short) activity);
	}

	public Type getFunctionType(int node) {
		return index2node[node].getType();
	}

	public short getParent(int node) {
		return parents[node];
	}

	/**
	 * returns the leaf count of the tree. All leafs are labelled with a label
	 * less than numleafs
	 * 
	 * @return
	 */
	public short numLeafs() {
		return leafs;
	}

	public boolean isLoopLeaf(int node) {
		return loopLeaf[node];
	}

	public short getLeftChild(int parent) {
		if (parent >= nodes || parent < leafs) {
			return -1;
		}
		return leftChildren[parent - leafs];
	}

	public short getRightChild(int e) {
		if (e >= nodes || e < leafs) {
			return -1;
		}
		return rightChildren[e - leafs];
	}

	public int numNodes() {
		return nodes;
	}

	public short numEventClasses() {
		return classes;
	}

	public int getCostFor(int modelMove, int activity) {
		if (modelMove == Move.BOTTOM) {
			// logMove only
			return getLogMoveCost(activity);
		}
		if (activity == Move.BOTTOM) {
			if (modelMove < leafs) {
				// a leaf node
				return getModelMoveCost(modelMove);
			} else {
				// a non-leaf node. divide by 3 to get the node from the move.
				return getModelMoveCost(modelMove / 3);
			}
		}
		// synchronous move. Don't penalize that.
		return 1;
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

	public int getLogMoveCost(int i) {
		return 1 + getScaling() * algorithm.getLogMoveCost(i);
	}

	public int getModelMoveCost(int node) {
		if (node >= leafs) {
			return 1;
		}
		return node2cost[node];
	}

	public int getScaling() {
		return scaling;
	}

	public XEventClass getClassOf(XEvent e) {
		return algorithm.getClasses().getClassOf(e);
	}

	public short getIndexOf(XEventClass c) {
		return algorithm.getIndexOf(c);
	}

	public TShortSet getActivitiesFor(int node) {
		return nodeIndex2act[node];
	}

	public String toString(short modelMove, short activity) {
		if (modelMove < leafs) {
			XEventClass c = index2node[modelMove].getClazz();
			return (c == null ? "STOP (" + modelMove + ")" : c.toString() + " (" + modelMove + ")");
		}
		return getFunctionType(modelMove / 3) + " (" + modelMove + ")";
	}

	public boolean isGreedy() {
		return greedy;
	}

	public short getCommonParent(short c1, short c2) {
		if (c1 == c2) {
			return c1;
		}
		if (parents[c1] == c2) {
			return c2;
		}
		if (parents[c2] == c1) {
			return c1;
		}
		if (c1 < c2) {
			return getCommonParent(parents[c1], c2);
		}
		if (c2 < c1) {
			return getCommonParent(c1, parents[c2]);
		}
		assert false;
		return 0;
	}

	public TIntList getChildren(short node) {
		TIntList list = new TIntArrayList();
		addChildren(node, list);
		return list;
	}

	private void addChildren(short node, TIntList list) {
		if (!isLeaf(node)) {
			addChildren(getRightChild(node), list);
			addChildren(getLeftChild(node), list);
			list.add(node);
		} else {
			list.insert(0, node);
		}
	}

	public int numLeafsAndLoops() {
		return numLeafsAndLoops;
	}

}
