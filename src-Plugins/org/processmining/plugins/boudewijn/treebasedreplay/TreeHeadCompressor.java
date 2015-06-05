package org.processmining.plugins.boudewijn.treebasedreplay;

import gnu.trove.list.TIntList;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import nl.tue.storage.CompressedStore;
import nl.tue.storage.EqualOperation;
import nl.tue.storage.HashOperation;
import nl.tue.storage.StorageException;
import nl.tue.storage.compressor.BitMask;

import org.processmining.plugins.astar.algorithm.State;
import org.processmining.plugins.astar.interfaces.Tail;
import org.processmining.plugins.astar.petrinet.impl.AbstractCompressor;
import org.processmining.plugins.astar.petrinet.impl.ShortShortMultiset;

public class TreeHeadCompressor<T extends Tail> extends AbstractCompressor<TreeHead> implements
		EqualOperation<State<TreeHead, T>>, HashOperation<State<TreeHead, T>> {

	private final short activities;
	private final int nodes;
	private final int bits;
	private final int bytesNodes;
	private final int bytesActivities;
	private final int maxBytes;

	public TreeHeadCompressor(int nodes, short activities) {
		this.nodes = nodes;
		this.activities = activities;
		this.bits = TreeHead.computeBitsForParikh(activities, nodes);

		this.bytesNodes = BitMask.getNumBytes(nodes);
		this.bytesActivities = BitMask.getNumBytes(activities);

		this.maxBytes = 4 + bytesNodes + bytesActivities + activities * 2;

	}

	public int getHashCode(State<TreeHead, T> object) {
		return object.getHead().hashCode();
	}

	public int getHashCode(CompressedStore<State<TreeHead, T>> store, long l) throws StorageException {
		try {
			InputStream stream = store.getStreamForObject(l);
			return readIntFromStream(stream);
		} catch (IOException e) {
			throw new StorageException(e);
		}
	}

	//	private static long inf = 0;
	//	private static long def = 0;
	//	private static int cnt = 0;

	public boolean equals(State<TreeHead, T> object, CompressedStore<State<TreeHead, T>> store, long l)
			throws StorageException, IOException {
		// The following test code showed that there is hardly any
		// difference between inflating or deflating
		//
		//		long s1 = System.nanoTime();
		//		boolean b1 = equalsDeflating(object.getHead(), store, l);
		//		long s2 = System.nanoTime();
		//		boolean b2 = equalsInflating(object, store, l);
		//		long s3 = System.nanoTime();
		//		assert (b1 == b2);
		//		inf += s3 - s2;
		//		def += s2 - s1;
		//		if (++cnt % 1000000 == 0) {
		//			System.out.println();
		//			System.out.println("inf: " + inf / 1000000.0 + "  def: " + def / 1000000.0);
		//			try {
		//				Thread.sleep(1000);
		//			} catch (InterruptedException e) {
		//			}
		//		}
		//		return b1;
		return equalsInflating(object, store, l);
	}

	private boolean equalsInflating(State<TreeHead, T> vector, CompressedStore<State<TreeHead, T>> store, long l)
			throws IOException {

		InputStream stream = store.getStreamForObject(l);

		int hashCode = readIntFromStream(stream);
		if (hashCode != vector.getHead().hashCode()) {
			return false;
		}

		BitMask m1 = readMask(stream, nodes, bytesNodes);
		TIntList l1 = vector.getHead().getMarked();
		if (m1.getOnes() != l1.size()) {
			return false;
		}

		BitMask m3 = readMask(stream, activities, bytesActivities);
		ShortShortMultiset p = vector.getHead().getParikhVector();

		if (m3.getOnes() != p.getNumElts()) {
			return false;
		}

		if (!l1.containsAll(BitMask.getIndices(m1))) {
			return false;
		}

		if (!inflateContent(stream, BitMask.getIndices(m3), activities).equals(p)) {
			return false;
		}
		return true;

	}

	public TreeHead inflate(InputStream stream) throws IOException {
		// skip the hashCode
		int hashCode = readIntFromStream(stream);

		// read the state
		BitMask mask = readMask(stream, nodes, bytesNodes);
		FastCloneTIntArrayList marked = new FastCloneTIntArrayList(BitMask.getIndices(mask));
		// read the parikh vector
		mask = readMask(stream, activities, bytesActivities);
		ShortShortMultiset parikh = inflateContent(stream, BitMask.getIndices(mask), activities);
		return new TreeHead(marked, parikh, hashCode, bits);
	}

	public void deflate(TreeHead object, OutputStream stream) throws IOException {
		// cache the hashcode for quick lookup
		writeIntToByteArray(stream, object.hashCode());
		// store the marking
		stream.write(makeIntListBitMask(nodes, object.getMarked(), bytesNodes).getBytes());
		// store the parikh vector
		deflate(object.getParikhVector(), stream, activities);
	}

	protected static BitMask makeIntListBitMask(int size, TIntList marking, int bitMaskSize) {
		byte[] bitmask = new byte[bitMaskSize];
		//iterating over all elements if possible indices is faster than 
		// first getting the relevant keys.
		for (short j = 0; j < marking.size(); j++) {
			int i = marking.get(j);
			int bte = i / 8;
			int bit = i % 8;
			bitmask[bte] = (byte) (bitmask[bte] | POWER[bit]);
		}
		return new BitMask(bitmask, marking.size());
	}

	public int getMaxByteCount() {
		return maxBytes;
	}

}
