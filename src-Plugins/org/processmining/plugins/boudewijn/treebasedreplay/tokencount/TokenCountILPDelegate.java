package org.processmining.plugins.boudewijn.treebasedreplay.tokencount;

import java.util.Map;

import org.processmining.plugins.boudewijn.tree.Node;
import org.processmining.plugins.boudewijn.treebasedreplay.AStarAlgorithm;

public class TokenCountILPDelegate extends AbstractTokenCountILPDelegate<TokenCountILPTail> {

	protected final TokenCountILPTailCompressor tailCompressor;

	public TokenCountILPDelegate(AStarAlgorithm algorithm, Node root, Map<Node, Integer> node2Cost, int threads, boolean greedy) {
		super(algorithm, root, node2Cost, threads, greedy);
		this.tailCompressor = new TokenCountILPTailCompressor(nodes, numEventClasses(), leafs);
	}

	public TokenCountILPTail createTail(TokenCountHead head) {
		return new TokenCountILPTail(this, head);
	}

	public TokenCountILPTailCompressor getTailInflater() {
		return tailCompressor;
	}

	public TokenCountILPTailCompressor getTailDeflater() {
		return tailCompressor;
	}

}
