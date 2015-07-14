package org.processmining.plugins.boudewijn.treebasedreplay;

import gnu.trove.list.TIntList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.set.TIntSet;

import java.util.Map;

import org.processmining.plugins.astar.algorithm.AStarException;
import org.processmining.plugins.astar.algorithm.AStarThread.Canceller;
import org.processmining.plugins.boudewijn.tree.Node;

public class TreeBasedAStarWithILP extends AbstractTreeBasedAStar<TreeHead, TreeILPTail, TreeILPDelegate> {

	private TreePostProcessor<TreeHead, TreeILPTail> postProcessor;

	public TreeBasedAStarWithILP(AStarAlgorithm algorithm, Canceller canceller, Node root,
			Map<Node, Integer> node2Cost, Map<TIntList, TIntSet> marking2modelmove,
			TObjectIntMap<TIntList> marking2visitCount) {
		super(algorithm, canceller, root, node2Cost, false);
		postProcessor = new TreePostProcessor<TreeHead, TreeILPTail>(delegate, root, marking2modelmove,
				marking2visitCount);
	}

	protected TreeILPDelegate constructDelegate(AStarAlgorithm algorithm, Node root, Map<Node, Integer> node2Cost,
			int threads, boolean greedy) {
		return new TreeILPDelegate(algorithm, root, node2Cost, threads);
	}

	protected TreeHead createInitialHead(TIntList trace) {
		return new TreeHead(getDelegate(), getDelegate().getIndexOf(root), trace);
	}

	protected int calculateCostAndPostProcess(
			org.processmining.plugins.boudewijn.treebasedreplay.AbstractTreeBasedAStar.VerboseLevel verbose,
			TreeRecord r, int frequency) {
		return postProcessor.calculateCostAndPostProcess(this, verbose, r, frequency);
	}

	public double run(VerboseLevel verbose, double stopAt) throws AStarException {
		try {
			return super.run(verbose, stopAt);
		} finally {
			delegate.deleteLPs();
		}

	}

}
