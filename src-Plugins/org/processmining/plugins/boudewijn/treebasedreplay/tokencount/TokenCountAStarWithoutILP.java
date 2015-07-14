package org.processmining.plugins.boudewijn.treebasedreplay.tokencount;

import gnu.trove.list.TIntList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.set.TIntSet;

import java.util.Map;

import org.processmining.plugins.astar.algorithm.AStarException;
import org.processmining.plugins.astar.algorithm.AStarThread.Canceller;
import org.processmining.plugins.boudewijn.tree.Node;
import org.processmining.plugins.boudewijn.tree.Node.Type;
import org.processmining.plugins.boudewijn.treebasedreplay.AStarAlgorithm;
import org.processmining.plugins.boudewijn.treebasedreplay.AbstractTreeBasedAStar;
import org.processmining.plugins.boudewijn.treebasedreplay.TreeRecord;

public class TokenCountAStarWithoutILP extends
		AbstractTreeBasedAStar<TokenCountHead, TokenCountEmptyTail, TokenCountEmptyDelegate> {

	private final TokenCountPostProcessor<TokenCountHead, TokenCountEmptyTail> postProcessor;

	public TokenCountAStarWithoutILP(AStarAlgorithm algorithm, Canceller canceller, Node root,
			Map<Node, Integer> node2Cost, Map<TIntList, TIntSet> marking2modelmove,
			TObjectIntMap<TIntList> marking2visitCount, boolean greedy) {
		super(algorithm, canceller, root, node2Cost, greedy);
		this.postProcessor = new TokenCountPostProcessor<TokenCountHead, TokenCountEmptyTail>(delegate, root,
				marking2modelmove, marking2visitCount);
	}

	protected TokenCountEmptyDelegate constructDelegate(AStarAlgorithm algorithm, Node root,
			Map<Node, Integer> node2Cost, int threads, boolean greedy) {
		return new TokenCountEmptyDelegate(algorithm, root, node2Cost, threads);
	}

	protected int calculateCostAndPostProcess(VerboseLevel verbose, TreeRecord r, int frequency) {
		return postProcessor.calculateCostAndPostProcess(this, verbose, r, frequency);
	}

	protected TokenCountHead createInitialHead(TIntList trace) {
		return new TokenCountHead(delegate, (short) delegate.getIndexOf(root), trace);
	}

	public double run(VerboseLevel verbose, double stopAt) throws AStarException {
		double fitness = super.run(verbose, stopAt);
		//update the behavior counters of loops

		for (Node node : root.getPreorder()) {
			if (node.getType() == Type.LOOP) {
				node.getBehavior().behavedAsR = node.getBehavior().behavedAsL
						- node.getRight().getBehavior().behavedAsL;
			}
		}

		return fitness;

	}
}