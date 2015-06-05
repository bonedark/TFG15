package org.processmining.plugins.astar.petrinet.impl;

import gnu.trove.procedure.TShortShortProcedure;

import java.util.Arrays;

public class ShortShortMultiset {

	// Size of this object.super // 16
	protected int numElts = 0; //    4
	private short size = 0; //       2
	private final short length; //   2
	private final short[] vals; //  24 + length * 2

	public static int getSizeInMemory(int length) {
		return 8 * (1 + ((48 + 2 * length - 1) / 8));
	}

	public ShortShortMultiset(short length) {
		super();
		this.length = length;
		this.vals = new short[length];
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof ShortShortMultiset)) {
			return false;
		}
		ShortShortMultiset that = (ShortShortMultiset) other;
		if (that.size != this.size) {
			return false;
		}
		if (that.numElts != this.numElts) {
			return false;
		}
		if (that.length != this.length) {
			return false;
		}
		int i = -1;
		while (++i < length && vals[i] == that.vals[i])
			;
		return i == length;
	}

	public int hashCode() {
		int result = 0;
		for (short element : vals)
			result = 31 * result + element;
		return result;
	}

	public short size() {
		return size;
	}

	public int getNumElts() {
		return numElts;
	}

	public void adjustValue(short key, short amount) {
		short v = vals[key];
		vals[key] += amount;
		numElts += amount;
		if (amount != 0) {
			if (v == 0) {
				size++;
			} else if (v + amount == 0) {
				size--;
			}
		}
	}

	public void increaseValue(short key) {
		vals[key]++;
		if (vals[key] == 0) {
			size--;
		} else {
			size++;
		}
	}

	public void put(short key, short value) {
		assert value >= 0;
		short v = vals[key];
		vals[key] = value;
		numElts += value - v;
		if (v != value) {
			if (v == 0) {
				size++;
			} else if (v + value == 0) {
				size--;
			}
		}
	}

	public short getLength() {
		return length;
	}

	public boolean forEachEntry(TShortShortProcedure tShortShortProcedure) {
		boolean b = true;
		for (short i = 0; i < length; i++) {
			if (vals[i] > 0) {
				b &= tShortShortProcedure.execute(i, vals[i]);
			}
		}
		return b;
	}

	public short get(short key) {
		return vals[key];
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public String toString() {
		return Arrays.toString(vals);
	}

	public ShortShortMultiset clone() {
		ShortShortMultiset newSet = new ShortShortMultiset(length);
		System.arraycopy(vals, 0, newSet.vals, 0, length);
		newSet.numElts = numElts;
		newSet.size = size;
		return newSet;
	}

}
