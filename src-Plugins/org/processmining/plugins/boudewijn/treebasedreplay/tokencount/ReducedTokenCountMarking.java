package org.processmining.plugins.boudewijn.treebasedreplay.tokencount;

import gnu.trove.iterator.TShortIterator;

import java.util.Arrays;
import java.util.NoSuchElementException;

import org.processmining.plugins.boudewijn.tree.Node.Type;

public class ReducedTokenCountMarking {

	// This class makes use of essential properties:
	// - There are at most Short.MAXVALUE-1 nodes
	// - A not cannot be in "future", nor "enabled" with 0 tokens
	// - executing a loop does not remove tokens from itself, but
	//   puts itself from "enabled" to "future"

	// we use the possible values of short as follows:
	// - Short.MINVALUE indicates a loop-node which is enabled with token count 2
	// - Short.MINVALUE+1 indicates a loop-node which is enabled with token count 2
	// - 0..Short.MAXVALUE-1 indicate the token count
	// - -1..-Short.MAXVALUE-1 indicate the token count while in future.

	private short enabled;
	private short future;

	public static byte INENABLED = 1;
	public static byte INFUTURE = 2;
	public static byte POSITIVECOUNT = 3;
	public static byte NONE = 0;

	private byte[] marking;

	private transient short[] movedFromFutureToEnabledForPushDown;

	public ReducedTokenCountMarking(AbstractTokenCountDelegate<?> delegate, int[] enabled, int[] future, int nodes) {
		this.enabled = (short) enabled.length;
		this.future = (short) future.length;

		// when executing a node, at most 2 tokens are produced, hence we need 2 free slots in the marking. 
		this.marking = new byte[nodes];

		for (int e : enabled) {
			marking[e] = INENABLED;
		}
		for (int f : future) {
			marking[f] = INFUTURE;
		}

	}

	public ReducedTokenCountMarking(ReducedTokenCountMarking other) {
		this.marking = Arrays.copyOf(other.marking, other.marking.length);
		this.enabled = other.enabled;
		this.future = other.future;
	}

	public ReducedTokenCountMarking(byte[] marking, short future, short enabled) {
		this.marking = marking;
		this.future = future;
		this.enabled = enabled;
	}

	private int moveFutureToEnabled(short node, int bitsForParikh) {
		marking[node] = INENABLED;

		enabled++;
		future--;
		assert future >= 0;

		// New hashing, added node node to enabled.
		return 1 << (bitsForParikh + (node % (32 - bitsForParikh)));
	}

	//	private void moveFutureToEnabled(AbstractTokenCountDelegate<?> delegate, int bitsForParikh) {
	//		TShortIterator it = futureIterator();
	//		while (it.hasNext()) {
	//			short node = it.next();
	//			// node  is in future.
	//			short parent = delegate.getParent(node);
	//
	//			// the node has to be the right child of its parent. Only in case of a loop that is 
	//			// future enabled, this is not necessarily the case if the loop is awaiting completion of
	//			// it's own left subtree. However, in that case, the loop-leaf is also in future and 
	//			// will be considered first, after which a break occurs.
	//			if (delegate.getRightChild(parent) != node) {
	//				continue;
	//			}
	//
	//			// get the tokenCount of the left child of the parent.
	//			if (delegate.getFunctionType(node) == Type.LOOP && (marking[delegate.getRightChild(node)] == INFUTURE)) {
	//				// node cannot be moved, since its right child is stil in future. If 
	//				// this loop could be moved, then it's right child could have been moved
	//				// as well and hence we would've moved it already.
	//				continue;
	//			}
	//			// at this stage, a node can only be in moved if it is the left child of its parent
	//			// has a tokencount of 0.
	//			if (marking[delegate.getLeftChild(parent)] == NONE) {
	//				moveFutureToEnabled(node);
	//				if (node < delegate.numLeafs() && delegate.isLoopLeaf(node)) {
	//					assert (marking[parent] == INFUTURE);
	//					moveFutureToEnabled(parent);
	//				}
	//				break;
	//			}
	//		}
	//	}

	private int getTokenCount(AbstractTokenCountDelegate<?> delegate, short node) {
		if (marking[node] == NONE) {
			return 0;
		}
		if (marking[node] == INENABLED || marking[node] == INFUTURE) {
			if (delegate.getFunctionType(node) == Type.LOOP) {
				// node is enabled or in future, there's a token here and there may 
				// be tokens in the subtree.
				if (getTokenCount(delegate, delegate.getRightChild(node)) == 0) {
					// there is no token in the right-child, hence this is a loop node, 
					// ready to be executed for the first time (since the last cleanup)
					return 1;
				} else if (marking[node] == INENABLED) {
					// there is a token here, and 1 in the right child (total 2)
					return 2;
				} else {
					// there is a token in the right child, hence there is a token here,
					// as well as in the right child, and maybe some in the left child.
					return 2 + getTokenCount(delegate, delegate.getLeftChild(node));
				}
			} else {
				// node is enabled or in future, there's a token here and nothing in the subtree.
				return 1;
			}
		}
		// node is not a leaf and is not enabled. There's no token here, but maybe in the subtree
		return getTokenCount(delegate, delegate.getLeftChild(node))
				+ getTokenCount(delegate, delegate.getRightChild(node));

	}

	/**
	 * Execute a node. provide both children if a token is to be produced there.
	 * Othewise, provide -1.
	 * 
	 * Also, provide the type of node, to handle loops correctly
	 * 
	 * What is returned is an 32 bit pattern showing 1 for each bit that needs
	 * flipping in the head's hashCode
	 * 
	 * @param e
	 * @param leftChild
	 * @param rightChild
	 * @param type
	 */
	public int execute(AbstractTokenCountDelegate<?> delegate, short e, short leftChild, short rightChild, Type type,
			int bitsForParikh) {
		assert (marking[e] == INENABLED);
		movedFromFutureToEnabledForPushDown = null;

		int toFlip = 0;

		// New hashing, removed node e from enabled.
		toFlip ^= 1 << (bitsForParikh + (e % (32 - bitsForParikh)));

		if (type == Type.LOOP) {
			// New hashing, added node leftChild to enabled.
			toFlip ^= 1 << (bitsForParikh + (leftChild % (32 - bitsForParikh)));

			movedFromFutureToEnabledForPushDown = new short[] { leftChild };

			if ((marking[rightChild] == INENABLED)) {
				// right child is also enabled. This loop was executed before (since the last cleanup)
				// the tokencount increases by 1 on all parents. (the token produced to the left)

				// move e and rightchild to future and make leftchild enabled
				marking[e] = INFUTURE;
				marking[rightChild] = INFUTURE;
				marking[leftChild] = INENABLED;

				// New hashing, removed node rightChild to enabled.
				toFlip ^= 1 << (bitsForParikh + (rightChild % (32 - bitsForParikh)));

				//				inEnabled[e] = false;
				//				inEnabled[rightChild] = false;
				//				inEnabled[leftChild] = true;
				enabled--;

				//				inFuture[e] = true;
				//				inFuture[rightChild] = true;
				future += 2;

				//				BVD: OLD HASHING
				//				while (leftChild >= 0) {
				//					toFlip ^= 1 << (bitsForParikh + (leftChild % (32 - bitsForParikh)));
				//					leftChild = delegate.getParent(leftChild);
				//				}
			} else {
				// this loop is executed for the first time since the last cleanup
				// move e to future, put rightchild in future and make leftchild enabled
				marking[e] = INFUTURE;
				marking[rightChild] = INFUTURE;
				marking[leftChild] = INENABLED;

				future += 2;
				// the tokencount changes by 1 for the left and right childs, but by 2 for e and all parents, 
				// hence bit flipping is limited

				//				BVD: OLD HASHING
				//				toFlip ^= 1 << (bitsForParikh + (rightChild % (32 - bitsForParikh)));
				//				toFlip ^= 1 << (bitsForParikh + (leftChild % (32 - bitsForParikh)));
			}
		} else {
			// non-loop node
			// disabled e
			movedFromFutureToEnabledForPushDown = new short[(leftChild >= 0 ? 1 : 0)
					+ (rightChild >= 0 && type != Type.SEQ ? 1 : 0)];

			marking[e] = POSITIVECOUNT;
			enabled--;
			if (leftChild >= 0) {
				marking[leftChild] = INENABLED;
				enabled++;
				movedFromFutureToEnabledForPushDown[0] = leftChild;
				//				BVD: OLD HASHING
				//				toFlip ^= 1 << (bitsForParikh + (leftChild % (32 - bitsForParikh)));
				// New hashing, added node leftChild to enabled.
				toFlip ^= 1 << (bitsForParikh + (leftChild % (32 - bitsForParikh)));
			}
			if (rightChild >= 0) {
				if (type == Type.SEQ) {
					marking[rightChild] = INFUTURE;
					future++;
				} else {
					marking[rightChild] = INENABLED;
					enabled++;
					// New hashing, added node rightChild to enabled.
					toFlip ^= 1 << (bitsForParikh + (rightChild % (32 - bitsForParikh)));

					movedFromFutureToEnabledForPushDown[(leftChild >= 0 ? 1 : 0)] = rightChild;
				}
				//				BVD: OLD HASHING
				//				toFlip ^= 1 << (bitsForParikh + (rightChild % (32 - bitsForParikh)));
			}
			if (type == Type.LEAF && delegate.isLoopLeaf(e)) {
				// executed a loop-leaf. Remove the parent
				short p = delegate.getParent(e);
				assert (marking[p] == INENABLED);
				// we set the parent's status later.
				marking[p] = NONE;
				enabled--;
				// New hashing, removed node p from enabled.
				toFlip ^= 1 << (bitsForParikh + (p % (32 - bitsForParikh)));

				// tokencount decreases by 1 for e, but by 2 for parent and all parents.
				//				BVD: OLD HASHING
				//				toFlip ^= 1 << (bitsForParikh + (e % (32 - bitsForParikh)));

				//			} else if ((rightChild >= 0 && leftChild >= 0) || type == Type.LEAF) {
				// either both leafs have been activated, or node e is a non-loop leaf, either way
				// the tokencount changes by 1 for all parents.
				//				short p = e;
				//				BVD: OLD HASHING
				//				while (p >= 0) {
				//					toFlip ^= 1 << (bitsForParikh + (p % (32 - bitsForParikh)));
				//					p = delegate.getParent(p);
				//				}
			}
			if (type == Type.LEAF) {
				// tokencount is reduced
				// check tokencount in parents. 
				marking[e] = NONE;
				short p = delegate.getParent(e);
				while (p >= 0 && marking[delegate.getRightChild(p)] == NONE
						&& marking[delegate.getLeftChild(p)] == NONE) {
					assert getTokenCount(delegate, p) == 0;
					marking[p] = NONE;
					p = delegate.getParent(p);
				}
				// the last node for which we changed the tokencount to NONE is last.
				//
				// check if the right leaf of the parent of last should be moved from future to 
				// enabled.
				if (p >= 0) {
					short right = delegate.getRightChild(p);
					if (marking[right] == INFUTURE) {
						if (right < delegate.numLeafs() && delegate.isLoopLeaf(right)) {
							assert (marking[p] == INFUTURE);
							// move both right and it's parent to enabled
							toFlip ^= moveFutureToEnabled(right, bitsForParikh);
							toFlip ^= moveFutureToEnabled(p, bitsForParikh);
						} else if (right < delegate.numLeafs()) {
							// move right to enabled
							toFlip ^= moveFutureToEnabled(right, bitsForParikh);
						} else if (marking[delegate.getRightChild(right)] != INFUTURE) {
							// non-leaf node that was moved from future to enabled 
							// and was not a loop waiting for tokens in own subtree.
							toFlip ^= moveFutureToEnabled(right, bitsForParikh);
							movedFromFutureToEnabledForPushDown = new short[] { right };
						}
					}
				}

				// we executed a leaf, try to move nodes from future to enabled.
				//moveFutureToEnabled(delegate, bitsForParikh);
			}

		}
		return toFlip;
	}

	public short[] getMovedFutureToEnabledForPushDown() {
		return movedFromFutureToEnabledForPushDown;
	}

	public TShortIterator enabledIterator() {
		return new TShortIterator() {

			private short i = -1;
			private int cnt = 0;

			public void remove() {
				throw new UnsupportedOperationException();
			}

			public boolean hasNext() {
				return cnt < enabled;
			}

			public short next() {
				cnt++;
				i++;
				while (i < marking.length) {
					if (marking[i] == INENABLED) {
						return i;
					}
					i++;
				}
				throw new NoSuchElementException();
			}
		};
	}

	public TShortIterator reverseEnabledIterator() {
		return new TShortIterator() {

			private short i = (short) marking.length;
			private int cnt = 0;

			public void remove() {
				throw new UnsupportedOperationException();
			}

			public boolean hasNext() {
				return cnt < enabled;
			}

			public short next() {
				cnt++;
				i--;
				while (i >= 0) {
					if (marking[i] == INENABLED) {
						return i;
					}
					i--;
				}
				throw new NoSuchElementException();
			}
		};
	}

	public TShortIterator futureIterator() {
		return new TShortIterator() {

			private short i = -1;
			private int cnt = 0;

			public void remove() {
				throw new UnsupportedOperationException();
			}

			public boolean hasNext() {
				return cnt < future;
			}

			public short next() {
				cnt++;
				i++;
				while (i < marking.length) {
					if (marking[i] == INFUTURE) {
						return i;
					}
					i++;
				}
				throw new NoSuchElementException();
			}
		};
	}

	public int hashCode() {
		return Arrays.hashCode(marking);
	}

	public boolean equals(Object o) {
		return (o instanceof ReducedTokenCountMarking ? Arrays.equals(((ReducedTokenCountMarking) o).marking, marking)
				: false);
	}

	public boolean isEmpty() {
		return enabled == 0 && future == 0;
	}

	public short numEnabled() {
		return enabled;
	}

	public short numFuture() {
		return future;
	}

	public String toString() {

		StringBuilder b = new StringBuilder();
		b.append("e:[");

		TShortIterator it = enabledIterator();
		while (it.hasNext()) {
			b.append(it.next());
			if (it.hasNext()) {
				b.append(',');
			}
		}
		b.append("]f:[");

		it = futureIterator();
		while (it.hasNext()) {
			b.append(it.next());
			if (it.hasNext()) {
				b.append(',');
			}
		}
		b.append("]");
		return b.toString();
	}

	public TShortIterator iterator(final AbstractTokenCountDelegate<?> delegate) {

		return new TShortIterator() {

			private short i = 0;

			public void remove() {
				throw new UnsupportedOperationException();
			}

			public boolean hasNext() {
				return i < marking.length;
			}

			public short next() {
				return (short) getTokenCount(delegate, i++);
			}
		};
	}

	public String toTokenCountString(AbstractTokenCountDelegate<?> delegate) {
		StringBuilder b = new StringBuilder();
		b.append("tc:[");

		TShortIterator it = iterator(delegate);
		while (it.hasNext()) {
			b.append(it.next());
			if (it.hasNext()) {
				b.append(',');
			}
		}
		b.append("]");
		return b.toString();
	}

	public boolean isDisabled(short node) {
		return marking[node] == NONE || marking[node] == POSITIVECOUNT;
	}

	public boolean isEnabled(int node) {
		return marking[node] == INENABLED;
	}

	public byte[] getMarking() {
		return marking;
	}
}
