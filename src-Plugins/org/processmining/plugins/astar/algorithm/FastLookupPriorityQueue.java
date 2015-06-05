package org.processmining.plugins.astar.algorithm;

import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;

import java.util.Arrays;
import java.util.Collection;
import java.util.Queue;

import org.processmining.plugins.astar.interfaces.Record;

public class FastLookupPriorityQueue {

	protected final TLongIntMap locationMap;
	public final static int NEV = -1;

	/**
	 * Priority queue represented as a balanced binary heap: the two children of
	 * queue[n] are queue[2*n+1] and queue[2*(n+1)]. The priority queue is
	 * ordered by comparator, or by the elements' natural ordering, if
	 * comparator is null: For each node n in the heap and each descendant d of
	 * n, n <= d. The element with the lowest value is in queue[0], assuming the
	 * queue is nonempty.
	 */
	protected Record[] queue;

	/**
	 * The number of elements in the priority queue.
	 */
	private int size = 0;

	/**
	 * Creates a {@code PriorityQueue} with the specified initial capacity that
	 * orders its elements according to the specified comparator.
	 * 
	 * @param initialCapacity
	 *            the initial capacity for this priority queue
	 * @param comparator
	 *            the comparator that will be used to order this priority queue.
	 *            If {@code null}, the {@linkplain Comparable natural ordering}
	 *            of the elements will be used.
	 * @throws IllegalArgumentException
	 *             if {@code initialCapacity} is less than 1
	 */

	public FastLookupPriorityQueue(int initialCapacity) {
		locationMap = new TLongIntHashMap(initialCapacity, 0.5f, -1, NEV);
		this.queue = new Record[initialCapacity];
	}

	/**
	 * Inserts the specified element into this priority queue.
	 * 
	 * @return {@code true} (as specified by {@link Collection#add})
	 * @throws ClassCastException
	 *             if the specified element cannot be compared with elements
	 *             currently in this priority queue according to the priority
	 *             queue's ordering
	 * @throws NullPointerException
	 *             if the specified element is null
	 */
	public boolean add(Record newE) {
		// check if overwrite is necessary, i.e. only add if the object does not exist yet,
		// or exists, but with higher costs.
		double newCost = newE.getTotalCost();
		int location = locationMap.get(newE.getState());
		if (location == NEV) {
			// new element, add to queue and return
			offer(newE);
			return true;
		}

		// retrieve stored cost
		double storedCost;
		storedCost = peek(location).getTotalCost();

		if (storedCost > newCost || (storedCost == newCost && newE.getCostSoFar() > peek(location).getCostSoFar())) {
			// store only if storedCost are higher, of if storedCost are equal, but costSoFar are higher 
			updateToBetter(location, newE);
			return true;
		}
		return false;
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public int hashCode() {
		return locationMap.hashCode();
	}

	public boolean equals(Object o) {
		return (o instanceof FastLookupPriorityQueue) && ((FastLookupPriorityQueue) o).locationMap.equals(locationMap);
	}

	public double contains(Record newRec) {
		int location = locationMap.get(newRec.getState());
		if (location != NEV) {
			return peek(location).getTotalCost();
		} else {
			return -1;
		}
	}

	public boolean checkInv() {
		return checkInv(0);
	}

	protected boolean checkInv(int loc) {
		Record n = queue[loc];
		Record c1 = null;
		Record c2 = null;
		if (2 * loc + 1 < queue.length) {
			c1 = queue[2 * loc + 1];
		}
		if (2 * (loc + 1) < queue.length) {
			c2 = queue[2 * (loc + 1)];
		}

		return (c1 == null ? true
				: (c1.getTotalCost() > n.getTotalCost() || (c1.getTotalCost() == n.getTotalCost() && c1.getCostSoFar() >= n
						.getCostSoFar())) && checkInv(2 * loc + 1))
				&& (c2 == null ? true
						: (c2.getTotalCost() > n.getTotalCost() || (c2.getTotalCost() == n.getTotalCost() && c2
								.getCostSoFar() >= n.getCostSoFar())) && checkInv(2 * (loc + 1)));

	}

	/**
	 * Increases the capacity of the array.
	 * 
	 * @param minCapacity
	 *            the desired minimum capacity
	 */
	protected void grow(int minCapacity) {
		if (minCapacity < 0) // overflow
			throw new OutOfMemoryError();
		int oldCapacity = queue.length;
		// Double size if small; else grow by 50%
		int newCapacity = ((oldCapacity < 64) ? ((oldCapacity + 1) * 2) : ((oldCapacity / 2) * 3));
		if (newCapacity < 0) // overflow
			newCapacity = Integer.MAX_VALUE;
		if (newCapacity < minCapacity)
			newCapacity = minCapacity;
		queue = Arrays.copyOf(queue, newCapacity);
	}

	/**
	 * Inserts the specified element into this priority queue.
	 * 
	 * @return {@code true} (as specified by {@link Queue#offer})
	 * @throws ClassCastException
	 *             if the specified element cannot be compared with elements
	 *             currently in this priority queue according to the priority
	 *             queue's ordering
	 * @throws NullPointerException
	 *             if the specified element is null
	 */
	protected void offer(Record e) {
		if (e == null)
			throw new NullPointerException();
		int i = size;
		if (i >= queue.length)
			grow(i + 1);
		size = i + 1;
		if (i == 0) {
			queue[0] = e;
			locationMap.put(e.getState(), 0);
		} else
			siftUpUsingComparator(i, e);
	}

	public Record peek() {
		if (size == 0)
			return null;
		return queue[0];
	}

	public int size() {
		return size;
	}

	public Record poll() {
		if (size == 0)
			return null;
		int s = --size;
		Record result = queue[0];
		Record x = queue[s];
		queue[s] = null;
		locationMap.remove(result.getState());

		if (s != 0)
			siftDownUsingComparator(0, x);
		return result;
	}

	protected void updateToBetter(int index, Record newRecord) {
		assert index >= 0 && index < size;
		locationMap.put(newRecord.getState(), index);
		siftUpUsingComparator(index, newRecord);
		//siftDownUsingComparator(index, newRecord);
	}

	/**
	 * Inserts item x at position k, maintaining heap invariant by promoting x
	 * up the tree until it is greater than or equal to its parent, or is the
	 * root.
	 * 
	 * @param k
	 *            the position to fill
	 * @param x
	 *            the item to insert
	 */
	protected void siftUpUsingComparator(int k, Record x) {
		while (k > 0) {
			int parent = (k - 1) >>> 1;
			Record e = queue[parent];
			if (x.getTotalCost() > e.getTotalCost()) {
				break;
			} else if (x.getTotalCost() == e.getTotalCost()) {
				if (x.getCostSoFar() >= e.getCostSoFar()) {
					break;
				}
			}
			queue[k] = e;
			locationMap.put(e.getState(), k);
			k = parent;
		}
		queue[k] = x;
		locationMap.put(x.getState(), k);
	}

	/**
	 * Inserts item x at position k, maintaining heap invariant by demoting x
	 * down the tree repeatedly until it is less than or equal to its children
	 * or is a leaf.
	 * 
	 * @param k
	 *            the position to fill
	 * @param x
	 *            the item to insert
	 */
	protected void siftDownUsingComparator(int k, Record x) {
		int half = size >>> 1;
		while (k < half) {
			int child = (k << 1) + 1;
			Record c = queue[child];
			int right = child + 1;
			if (right < size && (c.getTotalCost() > queue[right].getTotalCost() || // 
					(c.getTotalCost() == queue[right].getTotalCost() && //
					c.getCostSoFar() > queue[right].getCostSoFar())))
				c = queue[child = right];
			if (x.getTotalCost() < c.getTotalCost()) {
				break;
			} else if (x.getTotalCost() == c.getTotalCost()) {
				if (x.getCostSoFar() <= c.getCostSoFar()) {
					break;
				}
			}
			queue[k] = c;
			// assert locationMap.get(c.getState()) == child;
			// i.e.  child + k -child == k, 
			// hence we use adjustValue instead of put here.
			locationMap.adjustValue(c.getState(), k - child);
			k = child;
		}
		queue[k] = x;
		locationMap.put(x.getState(), k);
	}

	protected Record peek(int location) {
		return queue[location];
	}

	public String toString() {
		return Arrays.toString(queue);
	}
}
