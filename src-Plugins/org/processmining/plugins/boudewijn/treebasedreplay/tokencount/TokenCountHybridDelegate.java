package org.processmining.plugins.boudewijn.treebasedreplay.tokencount;

import java.util.Map;

import org.processmining.plugins.boudewijn.tree.Node;
import org.processmining.plugins.boudewijn.treebasedreplay.AStarAlgorithm;

public class TokenCountHybridDelegate extends AbstractTokenCountILPDelegate<TokenCountHybridTail> {

	protected final TokenCountHybridTailCompressor tailCompressor;

	public TokenCountHybridDelegate(AStarAlgorithm algorithm, Node root, Map<Node, Integer> node2Cost, int threads,
			boolean greedy) {
		super(algorithm, root, node2Cost, threads, greedy);
		this.tailCompressor = new TokenCountHybridTailCompressor(nodes, numEventClasses(), leafs);
	}

	public TokenCountHybridTail createTail(TokenCountHead head) {
		return new TokenCountHybridTail(this, head);
	}

	public TokenCountHybridTailCompressor getTailInflater() {
		return tailCompressor;
	}

	public TokenCountHybridTailCompressor getTailDeflater() {
		return tailCompressor;
	}

}
