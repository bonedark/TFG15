package org.processmining.plugins.boudewijn.treebasedreplay;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.procedure.TIntProcedure;

import org.processmining.plugins.astar.interfaces.Delegate;
import org.processmining.plugins.astar.interfaces.Head;
import org.processmining.plugins.astar.interfaces.Move;
import org.processmining.plugins.astar.interfaces.Record;
import org.processmining.plugins.astar.interfaces.Tail;
import org.processmining.plugins.astar.petrinet.impl.ShortShortMultiset;

/**
 * Represents the "state" in a fitness computation that is independent of the
 * trace that is currently investigated.
 * 
 * In case of a tree, this state is a set of edges, which can be represented by
 * a list of nodes to which these edges point.
 * 
 * @author bfvdonge
 * 
 */
public class TreeHead implements Head {

	//private static final TIntSet codes = new TIntHashSet();
	// size of super                                16
	private final int bitsForParikh; //              4
	private final FastCloneTIntArrayList marked; // 16 + 16 + 8 + 24 + 4 * numNodes
	private final ShortShortMultiset parikh; //      8 +  2 * numactivities + 48
	private final int hashCode; //                   4

	public static int computeBitsForParikh(short acts, int nodes) {
		// since activities are in a parikh vector and nodes are not, count the activities double
		// use at least 10 bits.
		return Math.max(8, (int) (32.0 * 1.5 * acts / (1.5 * acts + nodes)));
	}

	public static int getSizeFor(int nodes, short activities) {
		return 8 * (1 + (96 + ShortShortMultiset.getSizeInMemory(activities) + 4 * nodes - 1) / 8);
	}

	public TreeHead(TreeDelegate<?, ?> delegate, int topNode, TIntList t) {
		// the number of nodes is assumed to fit in a short and is final.
		//this.enabled = node2enabled(delegate, topNode);
		this.marked = new FastCloneTIntArrayList(delegate.numNodes());
		this.marked.add(topNode);

		this.parikh = new ShortShortMultiset(delegate.numEventClasses());
		TIntIterator it = t.iterator();
		while (it.hasNext()) {
			this.parikh.adjustValue((short) it.next(), (short) 1);
		}

		bitsForParikh = computeBitsForParikh(delegate.numEventClasses(), delegate.numNodes());

		hashCode = hash(marked) + hash(parikh, delegate.numEventClasses());
		//codes.add(hashCode);
		//System.out.println("codes: " + codes.size());
	}

	TreeHead(FastCloneTIntArrayList marked, ShortShortMultiset newParikh, int hashCode, int bitsForParikh) {
		this.marked = marked;
		this.parikh = newParikh;
		this.hashCode = hashCode;
		// CAREFUL: This assertion uses a constant that is valid only when testing with 6 event classes.
		//		assert (hashCode == hash(marked) + hash(parikh, (short) 6));
		//codes.add(hashCode);
		//System.out.println("codes: " + codes.size());
		this.bitsForParikh = bitsForParikh;

	}

	private int hash(TIntList list) {
		int h = 0;
		for (int i = list.size(); i-- > 0;) {
			// for the list, flip the corresponding bit in 
			// the first 2 bytes of the hashCode
			h ^= 1 << (bitsForParikh + (list.get(i) % (32 - bitsForParikh)));
		}
		return h;
	}

	private int hash(ShortShortMultiset set, short max) {
		int h = 0;
		for (short i = max; i-- > 0;) {
			// for the multiset, flip the corresponding bit as
			// often as given in the set.
			if (set.get(i) % 2 == 1) {
				h ^= 1 << (i % bitsForParikh);
			}
		}
		return h;
	}

	public TreeHead getNextHead(Record rec, Delegate<? extends Head, ? extends Tail> d, int modelMove, int logMove,
			int activity) {

		AbstractTreeDelegate<?> delegate = (AbstractTreeDelegate<?>) d;

		// the selected node is stored in "modelMove".

		final ShortShortMultiset newParikh;

		int hash = hashCode;
		if (logMove != Move.BOTTOM) {
			// there is a logMove
			newParikh = parikh.clone();
			newParikh.adjustValue((short) activity, (short) -1);

			// update the hash for the parikh vector, by flipping a bit
			hash ^= 1 << (activity % bitsForParikh);
		} else {
			newParikh = parikh;
		}

		if (modelMove != Move.BOTTOM) {
			// there was a modelMove, determine the newly enabled moves.
			int node;
			Effect effect;
			if (modelMove < delegate.numNodes()) {
				// the given modelMove points to a node.
				node = (short) modelMove;
				effect = delegate.getEffects(node).get(0);
			} else {
				// the given modelMove points to an effect
				effect = delegate.getEffectForColumnNumber(modelMove);
				node = effect.getNode();
			}

			final FastCloneTIntArrayList newMarked = new FastCloneTIntArrayList(marked);
			// the node removes it's own token.
			newMarked.remove(node);
			hash ^= 1 << (bitsForParikh + (node % (32 - bitsForParikh)));

			final IntPointer h = new IntPointer(hash);

			// handle the effect of the node.
			effect.forEach(new TIntProcedure() {

				public boolean execute(int key) {
					if (key < 0) {
						key = -key;
						if (newMarked.remove(key)) {
							h.i ^= 1 << (bitsForParikh + (key % (32 - bitsForParikh)));
						}
					} else {
						int index = newMarked.binarySearch(key);
						if (index < 0) {
							newMarked.insert(-index - 1, key);
							h.i ^= 1 << (bitsForParikh + (key % (32 - bitsForParikh)));
						}
					}
					return true;
				}
			});
			return new TreeHead(newMarked, newParikh, h.i, bitsForParikh);

		} else {
			// There was a log-move only
			return new TreeHead(marked, newParikh, hash, bitsForParikh);
		}
	}

	public TIntList getModelMoves(Record rec, Delegate<? extends Head, ? extends Tail> d, TIntList en,
			final int activity) {
		final TreeDelegate<?, ?> delegate = (TreeDelegate<?, ?>) d;
		// return a list of integers representing the nodes in the tree that are enabled,
		// where we encode the "edge", i.e. the succesor node in the first part of the integer.
		// remove nodes for which there exists a "larger" node with a SEQ-typed common predecessor  

		FastCloneTIntArrayList moves = new FastCloneTIntArrayList();

		int nodes = delegate.numNodes();
		for (int i = 0; i < en.size(); i++) {
			int m = en.get(i);
			if (m >= nodes) {
				break;
			}
			if (delegate.getActivitiesFor(m).contains((short) activity)) {
				moves.add(m);
			}
		}

		return moves;

		//		FastCloneTIntArrayList enabled = new FastCloneTIntArrayList(marked);
		//
		//		for (int i = enabled.size(); i-- > 0;) {
		//			if (!delegate.getActivitiesFor(enabled.get(i)).contains((short) activity)) {
		//				enabled.removeAt(i);
		//			} else if (delegate.isBlockedModelMove(enabled.get(i), marked)) {
		//				enabled.removeAt(i);
		//			}
		//		}
		//		replaceEffectNodes(delegate, enabled);

		//		return enabled;
	}

	public TIntList getModelMoves(Record rec, Delegate<? extends Head, ? extends Tail> d) {
		final AbstractTreeDelegate<?> delegate = (AbstractTreeDelegate<?>) d;
		// return a list of integers representing the nodes in the tree that are enabled,
		// where we encode the "edge", i.e. the succesor node in the first part of the integer.
		// remove nodes for which there exists a "larger" node with a SEQ-typed common predecessor  
		FastCloneTIntArrayList enabled = new FastCloneTIntArrayList(marked);

		for (int i = enabled.size(); i-- > 0;) {
			if (delegate.isBlockedModelMove(enabled.get(i), marked)) {
				enabled.removeAt(i);
			}
		}
		replaceEffectNodes(delegate, enabled);
		return enabled;

	}

	private void replaceEffectNodes(AbstractTreeDelegate<?> delegate, TIntList enabled) {
		// For all the loop-leafs and for node 0 if it is not a leaf:
		// replace them with the columnnumbers of all their effects.
		for (int i = enabled.size(); i-- > 0;) {
			short val = (short) enabled.get(i);
			if (delegate.isLoopLeaf(val) || (val == 0 && !delegate.isLeaf(val))) {
				// there is a non-empty effect, i.e. the node at get(i) is a loop-leaf, or the non-leaf root
				enabled.removeAt(i);
				for (Effect e : delegate.getEffects(val)) {
					enabled.add(e.getColumnNumber());
				}
			}
		}

	}

	public boolean isFinal(final Delegate<? extends Head, ? extends Tail> delegate) {
		final TreeDelegate<?, ?> d = ((TreeDelegate<?, ?>) delegate);
		// if the parikh vector is empty, and the only remaining marked nodes are loop nodes or their leafs, then we're done.
		return parikh.isEmpty() && marked.forEach(new TIntProcedure() {

			public boolean execute(int value) {
				return d.isLoopLeaf((short) value);
			}
		});
	}

	public ShortShortMultiset getParikhVector() {
		return parikh;
	}

	public TIntList getMarked() {
		return marked;
	}

	public int hashCode() {
		return hashCode;
	}

	public boolean equals(Object o) {
		if (!(o instanceof TreeHead)) {
			return false;
		}
		ShortShortMultiset p = ((TreeHead) o).parikh;
		FastCloneTIntArrayList e = ((TreeHead) o).marked;
		return ((TreeHead) o).hashCode == hashCode && p.equals(parikh) && e.equalsSorted(marked);
	}

	public String toString() {
		return "[m:" + marked + ",p:" + parikh + "]";
	}

	private static final class IntPointer {
		public int i;

		public IntPointer(int hash) {
			i = hash;
		}
	}
}
