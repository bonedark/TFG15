package org.processmining.plugins.boudewijn.treebasedreplay.tokencount;

import gnu.trove.iterator.TShortIterator;

import java.util.Arrays;
import java.util.NoSuchElementException;

import org.processmining.plugins.boudewijn.tree.Node.Type;

public class TokenCountMarking {

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

	private final static short ENABLED1 = Short.MIN_VALUE + 1;
	private final static short ENABLED2 = Short.MIN_VALUE;

	private final short[] marking;
	private short enabled;
	private short future;

	public TokenCountMarking(AbstractTokenCountDelegate<?> delegate, int[] enabled, int[] future, int nodes) {
		this.marking = new short[nodes];
		this.enabled = (short) enabled.length;
		this.future = (short) future.length;
		for (int e : enabled) {
			if (delegate.getFunctionType(e) == Type.LOOP && marking[delegate.getRightChild(e)] == ENABLED1) {
				marking[e] = ENABLED2;
			} else {
				marking[e] = ENABLED1;
			}
			e = delegate.getParent(e);
			while (e >= 0) {
				marking[e]++;
				e = delegate.getParent(e);
			}
		}
		for (int f : future) {
			marking[f] = (short) (-marking[f] - 1);
			f = delegate.getParent(f);
			while (f >= 0) {
				if (marking[f] < 0) {
					marking[f]--;
				} else {
					marking[f]++;
				}
				f = delegate.getParent(f);
			}
		}

	}

	public TokenCountMarking(TokenCountMarking other) {
		this.marking = Arrays.copyOf(other.marking, other.marking.length);
		this.enabled = other.enabled;
		this.future = other.future;
	}

	public TokenCountMarking(short[] tokenCount, short enabled, short future) {
		this.marking = tokenCount;
		this.enabled = enabled;
		this.future = future;
	}

	private void moveFutureToEnabled(short e, short state) {
		marking[e] = state;
		enabled++;
		future--;
		assert future >= 0;
	}

	private void moveFutureToEnabled(AbstractTokenCountDelegate<?> delegate, int bitsForParikh) {
		TShortIterator it = futureIterator();
		while (it.hasNext()) {
			short node = it.next();
			// node  is in future.
			short parent = delegate.getParent(node);
			if (marking[parent] == 1) { //
				assert (marking[node] == -1);
				//only one token pending in the subtree of parent, which 
				// is node, hence node can be moved.
				moveFutureToEnabled(node, ENABLED1);
				//
				break;
			} else if (marking[parent] == -2 && delegate.isLoopLeaf(node)) { //
				assert (marking[node] == -1);
				assert (marking[parent] == -2);
				// parent is a loop of which node is the loop leaf and is a future on 2 tokens, which are its own and mine.
				moveFutureToEnabled(node, ENABLED1);
				moveFutureToEnabled(parent, ENABLED2);
				break;
			}
		}
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
		assert checkConsistency(delegate);

		int toFlip = 0;
		// disable e
		short adjust = 1;
		if (type == Type.LOOP) {
			if (marking[e] == ENABLED1) {
				// the loop was executed for the first time.
				// set the marking to -1, it will be decreased by 2 later to -3 (this node and both children) 
				marking[e] = -1;
				// tokencount doesn't change, no flipping required

				future++;
				adjust = 2;
			} else {
				assert marking[e] == ENABLED2;
				// set the marking to -2, it will be decreased by 1 later to -3 (this node and both children)
				marking[e] = -2;
				// tokencount doesn't change, no flipping required

				future++;
				// remove the additionally enabled right leaf, which
				// will be placed in future later.
				// marking[rightchild] = -1;
				enabled--;
				// flip the bit, as it will be flipped again later due to the "increase" in tokencount
				toFlip ^= 1 << (bitsForParikh + (rightChild % (32 - bitsForParikh)));
			}
		} else {
			assert marking[e] == ENABLED1;
			marking[e] = 1;
			// tokencount doesn't change, no flipping required
		}
		// the node is no longer enabled
		enabled--;

		if (leftChild >= 0) {
			assert marking[leftChild] == 0;
			marking[leftChild] = ENABLED1;
			enabled++;
			// left tokencount increase, flip bit
			toFlip ^= 1 << (bitsForParikh + (leftChild % (32 - bitsForParikh)));
		}
		if (rightChild >= 0) {
			if (type == Type.LOOP || type == Type.SEQ) {
				marking[rightChild] = -1;
				future++;
			} else {
				assert marking[rightChild] == 0;
				marking[rightChild] = ENABLED1;
				enabled++;
			}
			// right tokencount increase, flip bit
			toFlip ^= 1 << (bitsForParikh + (rightChild % (32 - bitsForParikh)));
		}
		if (leftChild < 0 && rightChild < 0) {
			// a leaf node (both children < 0),
			// reduce the nodeCount
			if (delegate.isLoopLeaf(e)) {
				assert marking[e] == 1;
				marking[e] = 0;
				// tokencount decrease, flip bit
				toFlip ^= 1 << (bitsForParikh + (e % (32 - bitsForParikh)));
				adjust = 2;

				// disable the parent.
				e = delegate.getParent(e);
				assert (marking[e] == ENABLED2);
				marking[e] = 2; // will be reduced by 2 to 0 in following loop.
				enabled--;
				// tokencount unchanged, no flip required

			} else {
				assert marking[e] == 1;
				adjust = 1;
				marking[e] = 0;

				// tokencount decrease, flip bit
				toFlip ^= 1 << (bitsForParikh + (e % (32 - bitsForParikh)));
				e = delegate.getParent(e);
			}
			while (e >= 0) {
				assert marking[e] != ENABLED1 && marking[e] != ENABLED2;
				if (marking[e] < 0) {
					marking[e] += adjust;
				} else {
					marking[e] -= adjust;
				}
				// tokencount decrease, flip bit
				if (adjust == 1) {
					toFlip ^= 1 << (bitsForParikh + (e % (32 - bitsForParikh)));
				}
				e = delegate.getParent(e);
			}

			moveFutureToEnabled(delegate, bitsForParikh);

		} else if (leftChild >= 0 && rightChild >= 0) {
			// or a LOOP, SEQ, AND, or ORwithBoth node
			// update total tokenCount in node and
			// all its parents.
			while (e >= 0) {
				if (marking[e] < 0) {
					marking[e] -= adjust;
				} else {
					marking[e] += adjust;
				}
				if (adjust == 1) {
					// tokencount decrease, flip bit
					toFlip ^= 1 << (bitsForParikh + (e % (32 - bitsForParikh)));
				}
				e = delegate.getParent(e);
			}
		}

		assert checkConsistency(delegate);
		return toFlip;
	}

	private boolean checkConsistency(AbstractTokenCountDelegate<?> delegate) {
		boolean ok = true;

		int eCnt = 0;
		int fCnt = 0;
		short i;
		for (i = 0; ok && i < marking.length; i++) {
			if (marking[i] == ENABLED1) {
				eCnt++;
				if (!delegate.isLeaf(i)) {
					ok &= marking[delegate.getRightChild(i)] == 0;
					ok &= marking[delegate.getLeftChild(i)] == 0;
				}
			} else if (marking[i] == ENABLED2) {
				eCnt++;
				ok &= marking[delegate.getRightChild(i)] == ENABLED1;
				ok &= marking[delegate.getLeftChild(i)] == 0;
			} else if (marking[i] < 0) {
				fCnt++;
			} else if (marking[i] == 1 && !delegate.isLeaf(i)) {
				if (marking[delegate.getRightChild(i)] == 0 && marking[delegate.getLeftChild(i)] == 0) {
					eCnt++;
					ok = false;
				}
			} else if (marking[i] == 2 && delegate.getFunctionType(i) == Type.LOOP) {
				if (marking[delegate.getRightChild(i)] == 1 || marking[delegate.getRightChild(i)] == ENABLED1) {
					eCnt++;
					ok = false;
				}
			}
		}
		try {
			TShortIterator it = enabledIterator();
			while (it.hasNext()) {
				it.next();
			}
			it = futureIterator();
			while (it.hasNext()) {
				it.next();
			}
		} catch (NoSuchElementException e) {
			ok = false;
		}
		ok &= enabled == eCnt;
		ok &= future == fCnt;
		return ok;
	}

	public TShortIterator iterator() {
		return new TShortIterator() {

			private short i = -1;

			public boolean hasNext() {
				return i + 1 < marking.length;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}

			public short next() {
				short s = marking[++i];
				if (s == ENABLED1) {
					return 1;
				} else if (s == ENABLED2) {
					return 2;
				} else if (s < 0) {
					return (short) -s;
				} else {
					return s;
				}
			};

		};
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
					if (marking[i] <= ENABLED1) {
						return i;
					}
					i++;
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
					if (marking[i] < 0 && marking[i] > ENABLED1) {
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
		return (o instanceof TokenCountMarking ? Arrays.equals(((TokenCountMarking) o).marking, marking)
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

	short[] getInternalArray() {
		return marking;
	}

	public String toString() {

		StringBuilder b = new StringBuilder();
		b.append('[');
		for (int i = 0;; i++) {
			if (marking[i] > ENABLED1) {
				b.append(marking[i]);
			} else if (marking[i] == ENABLED1) {
				b.append("(1)");
			} else {// (marking[i] == ENABLED2) 
				b.append("(2)");
			}
			if (i == marking.length - 1) {
				b.append(']').toString();
				b.append(" e: " + enabled + " f: " + future);
				return b.toString();
			}
			b.append(", ");
		}
	}

}
