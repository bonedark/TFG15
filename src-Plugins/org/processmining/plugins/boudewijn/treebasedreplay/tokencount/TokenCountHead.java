package org.processmining.plugins.boudewijn.treebasedreplay.tokencount;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TShortIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.TShortList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.linked.TShortLinkedList;

import org.processmining.plugins.astar.interfaces.Delegate;
import org.processmining.plugins.astar.interfaces.Head;
import org.processmining.plugins.astar.interfaces.Move;
import org.processmining.plugins.astar.interfaces.Record;
import org.processmining.plugins.astar.interfaces.Tail;
import org.processmining.plugins.astar.petrinet.impl.ShortShortMultiset;
import org.processmining.plugins.boudewijn.tree.Node.Type;
import org.processmining.plugins.boudewijn.treebasedreplay.FastCloneTIntArrayList;

public class TokenCountHead implements Head {

	// by using a greedy aStar, we can guarantee that the first time the head is reached,
	// this head is reached trough the cheapest path!

	//	private static TIntHashSet used = new TIntHashSet(10000);
	//	public static int uniqueHashCount = 0;

	public final static int L = 1;
	public final static int R = 2;
	public final static int B = 0;

	private final int bitsForParikh; //              4

	// contains the enabled nodes, i.e. the nodes for which a token is present
	// contains the nodes enabled in the future, i.e. the nodes for which a token is present, but which cannot
	// occur until the left sibling of its parent completes (seq and loop leafs)
	// contains the number of tokens on internal nodes and mapped leaf nodes in the subtree (including itself).
	private final ReducedTokenCountMarking marking;

	private final ShortShortMultiset parikh; //       8 +  2 * numactivities + 48

	private final int hashCode; //                    4

	/**
	 * The movesMade stores a list of moves that were made to generate this head
	 * in the last access to this head, i.e. it is part of the statespace for a
	 * trace, rather than a log. We store it in the head and use it in the
	 * construction of the tail ONLY.
	 */
	private transient TIntList movesMade;

	public static int computeBitsForParikh(short acts, int nodes) {
		// since activities are in a parikh vector and nodes are not, count the activities double
		// use at least 8 bits but at most the number of activities.
		return Math.min(acts, Math.max(8, (int) (32.0 * 1.5 * acts / (1.5 * acts + nodes))));
	}

	public TokenCountHead(AbstractTokenCountDelegate<?> delegate, short topNode, TIntList trace) {
		// the number of nodes is assumed to fit in a short and is final.

		int[] enabled = new int[1];
		int[] future = new int[0];

		// the root is enabled, i.e. it is in the set enabled
		// and there is 1 pending token.
		enabled[0] = topNode;
		movesMade = new TIntArrayList();

		this.parikh = new ShortShortMultiset(delegate.numEventClasses());
		TIntIterator it = trace.iterator();
		while (it.hasNext()) {
			this.parikh.adjustValue((short) it.next(), (short) 1);
		}

		bitsForParikh = computeBitsForParikh(delegate.numEventClasses(), delegate.numNodes());

		this.marking = new ReducedTokenCountMarking(delegate, enabled, future, delegate.numNodes());

		int hash = 1 << (bitsForParikh + (topNode % (32 - bitsForParikh)));

		assert (hash == hashMarking(marking.enabledIterator()));

		Type t = delegate.getFunctionType(topNode);
		if (t == Type.AND || t == Type.LOOP || t == Type.SEQ) {
			hash ^= pushDown(delegate, marking, movesMade, new short[] { topNode });
		}

		hashCode = hash + hashParikh(parikh, delegate.numEventClasses());

		assert (hashCode == hashMarking(marking.enabledIterator()) + hashParikh(parikh, delegate.numEventClasses()));

		//		int hash = hashParikh(parikh, delegate.numEventClasses());
		//		TShortIterator it2 = marking.enabledIterator();
		//		while (it2.hasNext()) {
		//			hash ^= 1 << (bitsForParikh + (it2.next() % (32 - bitsForParikh)));
		//		}
		//		hashCode = hash;
	}

	TokenCountHead(ShortShortMultiset parikh, ReducedTokenCountMarking marking, int hashCode, int bitsForParikh,
			TIntList movesMade) {
		this.marking = marking;
		this.bitsForParikh = bitsForParikh;
		this.parikh = parikh;
		this.movesMade = movesMade;

		this.hashCode = hashCode;
		//		int hash = hashParikh(parikh, parikh.getLength());
		//		TShortIterator it2 = marking.enabledIterator();
		//		while (it2.hasNext()) {
		//			hash ^= 1 << (bitsForParikh + (it2.next() % (32 - bitsForParikh)));
		//		}
		//		this.hashCode = hash;

	}

	protected int pushDown(AbstractTokenCountDelegate<?> delegate, ReducedTokenCountMarking marking,
			TIntList movesMade, short[] enabledForPushDown) {
		int toFlip = 0;

		TShortList todo = new TShortLinkedList((short) -1);
		todo.addAll(enabledForPushDown);

		while (!todo.isEmpty()) {
			short e = todo.removeAt(0);
			Type t = delegate.getFunctionType(e);
			if (t == Type.AND || t == Type.SEQ || t == Type.LOOP) {
				// execute this node as well.
				assert (t != Type.LOOP || marking.isDisabled(delegate.getRightChild(e)));
				toFlip ^= marking.execute(delegate, e, delegate.getLeftChild(e), delegate.getRightChild(e), t,
						bitsForParikh);
				todo.addAll(marking.getMovedFutureToEnabledForPushDown());
				movesMade.add(e * 3 - 2 * delegate.numLeafs());
				//				toFlip ^= pushDown(delegate, marking, movesMade);
				//				return toFlip;
			}
		}

		//		TShortIterator it2 = marking.enabledIterator();
		//		while (it2.hasNext()) {
		//			short e = it2.next();
		//			Type t = delegate.getFunctionType(e);
		//			if (t == Type.AND || t == Type.SEQ || (t == Type.LOOP && marking.isDisabled(delegate.getRightChild(e)))) {
		//				// execute this node as well.
		//				toFlip ^= marking.execute(delegate, e, delegate.getLeftChild(e), delegate.getRightChild(e), t,
		//						bitsForParikh);
		//				movesMade.add(e * 3 - 2 * delegate.numLeafs());
		//				toFlip ^= pushDown(delegate, marking, movesMade);
		//				return toFlip;
		//			}
		//		}
		return toFlip;
	}

	protected int hashParikh(ShortShortMultiset set, short max) {
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

	protected int hashMarking(TShortIterator it) {
		int h = 0;
		while (it.hasNext()) {
			short s = it.next();
			assert s >= 0;
			h ^= 1 << (bitsForParikh + (s % (32 - bitsForParikh)));
		}
		return h;
	}

	public Head getNextHead(Record rec, Delegate<? extends Head, ? extends Tail> d, int modelMove, int logMove,
			int activity) {
		AbstractTokenCountDelegate<?> delegate = (AbstractTokenCountDelegate<?>) d;

		final ShortShortMultiset newParikh;
		TIntList newMovesMade = null;

		int newHash = hashCode;
		if (logMove != Move.BOTTOM) {
			// there is a logMove
			newParikh = parikh.clone();
			assert (newParikh.get((short) activity) > 0);
			newParikh.adjustValue((short) activity, (short) -1);

			// update the hash for the parikh vector, by flipping a bit
			newHash ^= 1 << (activity % bitsForParikh);
		} else {
			newParikh = parikh;
		}

		assert (newHash == hashMarking(marking.enabledIterator()) + hashParikh(newParikh, delegate.numEventClasses()));

		final ReducedTokenCountMarking newMarking;
		if (modelMove != Move.BOTTOM) {

			newMarking = new ReducedTokenCountMarking(marking);

			int type;
			short node;
			if (modelMove < delegate.numLeafs()) {
				node = (short) modelMove;
				type = 0;
			} else {
				node = (short) (modelMove / 3);
				type = modelMove % 3;
			}

			short right = delegate.getRightChild(node);
			short left = delegate.getLeftChild(node);

			Type t = delegate.getFunctionType(node);
			if (t == Type.XOR || t == Type.OR) {
				if (type == L) {
					// only the left node will be executed
					right = -1;
				} else if (type == R) {
					// only the right node will be executed
					left = -1;
				}

			}

			assert (newHash == hashMarking(newMarking.enabledIterator())
					+ hashParikh(newParikh, delegate.numEventClasses()));

			int toFlip = newMarking.execute(delegate, node, left, right, t, bitsForParikh);

			newHash ^= toFlip;

			assert (newHash == hashMarking(newMarking.enabledIterator())
					+ hashParikh(newParikh, delegate.numEventClasses()));

			newMovesMade = new TIntArrayList();

			// sync move may enable subtree under node previously in future. 
			if (newMarking.getMovedFutureToEnabledForPushDown() != null) {
				newHash ^= pushDown(delegate, newMarking, newMovesMade, newMarking.getMovedFutureToEnabledForPushDown());
			}

			assert (newHash == hashMarking(newMarking.enabledIterator())
					+ hashParikh(newParikh, delegate.numEventClasses()));

		} else {
			newMarking = marking;
		}

		return new TokenCountHead(newParikh, newMarking, newHash, bitsForParikh, newMovesMade);
	}

	public TIntList getMovesMade() {
		return movesMade;
	}

	public TIntList getModelMoves(Record r, Delegate<? extends Head, ? extends Tail> d, TIntList enabled, int activity) {
		AbstractTokenCountDelegate<?> delegate = (AbstractTokenCountDelegate<?>) d;
		FastCloneTIntArrayList potential = new FastCloneTIntArrayList(10);

		TIntIterator it = enabled.iterator();
		while (it.hasNext()) {
			int move = it.next();
			if (move < delegate.numLeafs()) {
				short node = (short) move;

				assert delegate.isLeaf(node);

				if (delegate.isMapped(node, activity)) {
					potential.add(node);
					if (delegate.isGreedy()) {
						// do not allow a modelMove instead of a
						it.remove();
					}
				}
			} else {
				break;
			}
		}

		return potential;
	}

	public TIntList getModelMoves(Record rec, Delegate<? extends Head, ? extends Tail> d) {
		AbstractTokenCountDelegate<?> delegate = (AbstractTokenCountDelegate<?>) d;
		FastCloneTIntArrayList potential = new FastCloneTIntArrayList(10);
		boolean greedy = delegate.isGreedy();

		int modelMove = -1;
		if (greedy && (rec.getMovedEvent() == Move.BOTTOM) && (rec.getModelMove() >= delegate.numLeafs())) {
			modelMove = rec.getModelMove() / 3;
		}
		// if  we entered this head through a modelMove and there
		// are more modelmoves enabled, then we should first 
		// execute those internal moves that are larger than modelMove
		// note that we rely on the labelling which labels leafs first, then
		// loops and then the rest in order of the pre-order.
		// hence an internal node only enables more internal nodes that 
		// are larger than themselves.

		TShortIterator it = marking.reverseEnabledIterator();
		boolean addLeafsAndLoops = true;
		boolean addNonLoopLeafs = true;
		while (it.hasNext()) {
			short node = it.next();
			if (node >= delegate.numLeafsAndLoops() && node < modelMove) {
				// this node would have been enabled in the previous
				// state and hence does not have to be added.
				continue;
			}
			Type t = delegate.getFunctionType(node);
			switch (t) {
				case LEAF :
					if (addLeafsAndLoops && (addNonLoopLeafs || delegate.isLoopLeaf(node))) {
						potential.add(node);
					}
					break;
				case LOOP :
					if (addLeafsAndLoops) {
						potential.add(3 * node + B);
						// we added a loop, do not allow for non-loop leafs to be added, in case
						// we are greedy.
						addNonLoopLeafs = !greedy;
					}
					break;
				case AND :
				case SEQ :
					addLeafsAndLoops = !greedy;
					assert false;
					break;
				case XOR :
					potential.add(3 * node + R);
					potential.add(3 * node + L);
					addLeafsAndLoops = !greedy;
					break;
				case OR :
					potential.add(3 * node + R);
					potential.add(3 * node + L);
					potential.add(3 * node + B);
					addLeafsAndLoops = !greedy;
					break;
			}
		}
		return potential;
	}

	public boolean isFinal(Delegate<? extends Head, ? extends Tail> delegate) {
		return marking.isEmpty() && parikh.isEmpty();
	}

	ShortShortMultiset getParikhVector() {
		return parikh;
	}

	//	public FastCloneTShortArrayList getEnabled() {
	//		return marking.getEnabled();
	//	}
	//
	//	public FastCloneTShortArrayList getFuture() {
	//		return marking.getFuture();
	//	}

	public ReducedTokenCountMarking getMarking() {
		return marking;
	}

	public int hashCode() {
		return hashCode;
	}

	public boolean equals(Object o) {
		if (!(o instanceof TokenCountHead)) {
			return false;
		}
		ShortShortMultiset p = ((TokenCountHead) o).parikh;
		ReducedTokenCountMarking m = ((TokenCountHead) o).marking;
		return ((TokenCountHead) o).hashCode == hashCode && p.equals(parikh) && m.equals(marking);
	}

	public String toString() {
		return "[h:" + hashCode + ",m:" + marking + ",p:" + parikh + "]";
	}

}
