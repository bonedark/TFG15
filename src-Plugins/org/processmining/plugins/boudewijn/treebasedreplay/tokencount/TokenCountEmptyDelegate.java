package org.processmining.plugins.boudewijn.treebasedreplay.tokencount;

import java.util.Map;

import nl.tue.storage.Deflater;
import nl.tue.storage.Inflater;

import org.processmining.plugins.boudewijn.tree.Node;
import org.processmining.plugins.boudewijn.treebasedreplay.AStarAlgorithm;

public class TokenCountEmptyDelegate extends AbstractTokenCountDelegate<TokenCountEmptyTail> {

	public TokenCountEmptyDelegate(AStarAlgorithm algorithm, Node root, Map<Node, Integer> node2Cost, int threads) {
		super(algorithm, root, node2Cost, threads, false);

	}

	public TokenCountEmptyTail createTail(TokenCountHead head) {
		return TokenCountEmptyTail.EMPTY;
	}

	public Inflater<TokenCountEmptyTail> getTailInflater() {
		return TokenCountEmptyTail.EMPTY;
	}

	public Deflater<TokenCountEmptyTail> getTailDeflater() {
		return TokenCountEmptyTail.EMPTY;
	}

}
