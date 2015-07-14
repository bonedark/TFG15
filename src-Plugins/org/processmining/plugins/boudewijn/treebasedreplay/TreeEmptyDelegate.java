package org.processmining.plugins.boudewijn.treebasedreplay;

import gnu.trove.iterator.TIntIterator;

import java.util.List;
import java.util.Map;

import nl.tue.storage.Deflater;
import nl.tue.storage.Inflater;

import org.processmining.plugins.boudewijn.tree.Node;

public class TreeEmptyDelegate extends AbstractTreeDelegate<TreeEmptyTail> {

	public TreeEmptyDelegate(AStarAlgorithm algorithm, Node root,
			Map<Node, Integer> node2Cost, int threads) {
		super(algorithm, root, node2Cost, threads);

		int effectColumn = 2 * nodes + numEventClasses();
		// Set the modelMoves
		for (short node = 0; node < nodes; node++) {
			if (index2node[node].getClazz() == null) {
				// a node with at least 1 effect
				List<Effect> eftcs = node2effects.get(node);
				TIntIterator it = eftcs.get(0).iterator();
				while (it.hasNext()) {
					int n = it.next();
					if (n == node) {
						continue;
					}
				}
				eftcs.get(0).setColumnNumber(nodes + node);
				columnNumber2effect.put(nodes + node, eftcs.get(0));
				// set objective

				for (int e = 1; e < eftcs.size(); e++) {
					it = eftcs.get(e).iterator();
					effectColumn++;
					while (it.hasNext()) {
						int n = it.next();
						if (n == node) {
							continue;
						}
					}
					eftcs.get(e).setColumnNumber(effectColumn - 1);
					columnNumber2effect.put(effectColumn - 1, eftcs.get(e));
				}
			}
		}

	}

	public TreeEmptyTail createTail(TreeHead head) {
		return TreeEmptyTail.EMPTY;
	}

	public Inflater<TreeEmptyTail> getTailInflater() {
		return TreeEmptyTail.EMPTY;
	}

	public Deflater<TreeEmptyTail> getTailDeflater() {
		return TreeEmptyTail.EMPTY;
	}

}
