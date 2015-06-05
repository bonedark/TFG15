package org.processmining.plugins.boudewijn.treebasedreplay;

import gnu.trove.list.TIntList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.set.TIntSet;

import java.util.Map;

import org.processmining.plugins.astar.algorithm.AStarThread.Canceller;
import org.processmining.plugins.boudewijn.tree.Node;

public class TreeBasedAStarWithoutILP extends AbstractTreeBasedAStar<TreeHead, TreeEmptyTail, TreeEmptyDelegate> {

	private TreePostProcessor<TreeHead, TreeEmptyTail> postProcessor;

	public TreeBasedAStarWithoutILP(AStarAlgorithm algorithm, Canceller canceller, Node root,
			Map<Node, Integer> node2Cost, Map<TIntList, TIntSet> marking2modelmove,
			TObjectIntMap<TIntList> marking2visitCount) {
		super(algorithm, canceller, root, node2Cost, false);
		postProcessor = new TreePostProcessor<TreeHead, TreeEmptyTail>(delegate, root, marking2modelmove,
				marking2visitCount);
	}

	protected TreeEmptyDelegate constructDelegate(AStarAlgorithm algorithm, Node root, Map<Node, Integer> node2Cost,
			int threads, boolean greedy) {
		return new TreeEmptyDelegate(algorithm, root, node2Cost, threads);
	}

	protected TreeHead createInitialHead(TIntList trace) {
		return new TreeHead(getDelegate(), getDelegate().getIndexOf(root), trace);
	}

	protected int calculateCostAndPostProcess(VerboseLevel verbose, TreeRecord r, int frequency) {
		return postProcessor.calculateCostAndPostProcess(this, verbose, r, frequency);
	}

}
