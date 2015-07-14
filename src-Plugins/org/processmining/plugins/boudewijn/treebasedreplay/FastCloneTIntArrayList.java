package org.processmining.plugins.boudewijn.treebasedreplay;

import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;

public class FastCloneTIntArrayList extends TIntArrayList {

	public FastCloneTIntArrayList(int capacity) {
		super(capacity, -1);
	}

	public FastCloneTIntArrayList(FastCloneTIntArrayList list) {
		_data = new int[list._data.length];
		System.arraycopy(list._data, 0, _data, 0, list._pos);
		_pos = list._pos;
		this.no_entry_value = list.no_entry_value;
	}

	public FastCloneTIntArrayList() {
		super(DEFAULT_CAPACITY, -1);
	}

	public FastCloneTIntArrayList(int[] indices) {
		_data = indices;
		_pos = indices.length;
		this.no_entry_value = -1;
	}

	public boolean addAllIfNew(TIntCollection collection) {
		boolean changed = false;
		TIntIterator iter = collection.iterator();
		while (iter.hasNext()) {
			int element = iter.next();
			if (!contains(element)) {
				changed = add(element);
			}
		}
		return changed;
	}

	/** {@inheritDoc} */
	// Note that this implementation assumes sorted lists
	public boolean equalsSorted(FastCloneTIntArrayList that) {
		if (that._pos != this._pos)
			return false;

		int i = -1;
		while (++i < _pos && _data[i] == that._data[i])
			;
		return i == _pos;
	}

}
