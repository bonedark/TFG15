package org.processmining.plugins.boudewijn.treebasedreplay.tokencount;

import gnu.trove.iterator.TShortIterator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import nl.tue.storage.CompressedStore;
import nl.tue.storage.EqualOperation;
import nl.tue.storage.HashOperation;
import nl.tue.storage.StorageException;
import nl.tue.storage.compressor.BitMask;

import org.processmining.plugins.astar.algorithm.State;
import org.processmining.plugins.astar.interfaces.Tail;
import org.processmining.plugins.astar.petrinet.impl.AbstractCompressor;
import org.processmining.plugins.astar.petrinet.impl.ShortShortMultiset;

public class TokenCountHeadCompressor<T extends Tail> extends AbstractCompressor<TokenCountHead> implements
		EqualOperation<State<TokenCountHead, T>>, HashOperation<State<TokenCountHead, T>> {

	private final short activities;
	private final short nodes;
	private int bytesMarking;
	private int bytesParikh;
	private int bitsForParikh;
	private final int maxBytes;

	public TokenCountHeadCompressor(AbstractTokenCountDelegate<?> delegate, short nodes, short activities) {
		this.nodes = nodes;
		this.activities = activities;
		this.bytesMarking = BitMask.getNumBytes(nodes);
		this.bytesParikh = BitMask.getNumBytes(activities);
		this.bitsForParikh = TokenCountHead.computeBitsForParikh(activities, nodes);
		// we use double bitmasks
		this.maxBytes = 4 + 2 * bytesMarking + 2 * bytesParikh + activities * 2;

	}

	public int getHashCode(State<TokenCountHead, T> object) {
		return object.getHead().hashCode();
	}

	public int getHashCode(CompressedStore<State<TokenCountHead, T>> store, long l) throws StorageException {
		try {
			InputStream stream = store.getStreamForObject(l);
			return readIntFromStream(stream);
		} catch (IOException e) {
			throw new StorageException(e);
		}
	}

	public boolean equals(State<TokenCountHead, T> object, CompressedStore<State<TokenCountHead, T>> store, long l)
			throws StorageException, IOException {
		return equalsInflating(object, store, l);
	}

	//	public static int CALLS = 0;
	//	public static int FALSEANSWER = 0;
	//	public static int FALSEONHASH = 0;

	private boolean equalsInflating(State<TokenCountHead, T> vector, CompressedStore<State<TokenCountHead, T>> store,
			long l) throws IOException {
		//		CALLS++;
		InputStream stream = store.getStreamForObject(l);

		int hashCode = readIntFromStream(stream);
		if (hashCode != vector.getHead().hashCode()) {
			//			FALSEANSWER++;
			//			FALSEONHASH++;
			return false;
		}

		ReducedTokenCountMarking marking = vector.getHead().getMarking();

		if (!checkMarkingDoubleBitMask(stream, marking)) {

			//			System.out.println("HashCollision marking: " + vector.getHead().toString() + " & "
			//					+ inflate(store.getStreamForObject(l)));
			//			FALSEANSWER++;
			return false;
		}

		// check the parikh vector
		if (checkParikhDoubleBitMask(stream, vector.getHead().getParikhVector())) {
			return true;
		} else {
			//			System.out.println("HashCollision parikh: " + vector.getHead().toString() + " & "
			//					+ inflate(store.getStreamForObject(l)));
			//			FALSEANSWER++;
			return false;
		}

	}

	public TokenCountHead inflate(InputStream stream) throws IOException {
		// read the hashCode
		int hashCode = readIntFromStream(stream);
		// read marking
		ReducedTokenCountMarking marking = readMarkingDoubleBitMask(stream);
		//		
		//		// read enabled;
		//		BitMask mask = readMask(stream, nodes);
		//		int[] enabled = BitMask.getIndices(mask);
		//		// read future
		//		mask = readMask(stream, nodes);
		//		int[] future = BitMask.getIndices(mask);
		// read the parikh vector
		ShortShortMultiset parikh = readParikhDoubleBitmask(stream);

		//new ReducedTokenCountMarking(delegate, enabled, future, nodes)
		return new TokenCountHead(parikh, marking, hashCode, bitsForParikh, null);
	}

	public void deflate(TokenCountHead object, OutputStream stream) throws IOException {
		// cache the hashcode for quick lookup
		writeIntToByteArray(stream, object.hashCode());
		writeMarkingDoubleBitMask(stream, object.getMarking());

		//		// store the enabled nodes
		//		ReducedTokenCountMarking m = object.getMarking();
		//		stream.write(makeShortListBitMask(m.numEnabled(), m.enabledIterator(), bytesMarking).getBytes());
		//		// store the future nodes
		//		stream.write(makeShortListBitMask(m.numFuture(), m.futureIterator(), bytesMarking).getBytes());

		// store the parikh vector
		writeParikhDoubleBitMask(stream, object.getParikhVector());
	}

	protected static BitMask makeShortListBitMask(int size, TShortIterator it, int bitMaskSize) {
		byte[] bitmask = new byte[bitMaskSize];
		//iterating over all elements if possible indices is faster than 
		// first getting the relevant keys.
		int ones = 0;
		while (it.hasNext()) {
			ones++;
			int i = it.next();
			int bte = i / 8;
			int bit = i % 8;
			bitmask[bte] = (byte) (bitmask[bte] | POWER[bit]);
		}
		return new BitMask(bitmask, ones);
	}

	protected void writeMarkingDoubleBitMask(OutputStream stream, ReducedTokenCountMarking m) throws IOException {

		byte[] bitmask1 = new byte[bytesMarking];
		byte[] bitmask2 = new byte[bytesMarking];

		byte[] marking = m.getMarking();

		for (short n = 0; n < nodes; n++) {
			if (marking[n] == ReducedTokenCountMarking.NONE) {
				continue;
			}
			int bte = n / 8;
			int bit = n % 8;

			if (marking[n] == ReducedTokenCountMarking.INENABLED) {
				bitmask1[bte] = (byte) (bitmask1[bte] | POWER[bit]);
			} else if (marking[n] == ReducedTokenCountMarking.INFUTURE) {
				bitmask2[bte] = (byte) (bitmask2[bte] | POWER[bit]);
			} else if (marking[n] == ReducedTokenCountMarking.POSITIVECOUNT) {
				bitmask1[bte] = (byte) (bitmask1[bte] | POWER[bit]);
				bitmask2[bte] = (byte) (bitmask2[bte] | POWER[bit]);
			}
		}
		stream.write(bitmask1);
		stream.write(bitmask2);
	}

	protected ReducedTokenCountMarking readMarkingDoubleBitMask(InputStream stream) throws IOException {

		byte[] bitmask1 = readMask(stream, nodes, bytesMarking).getBytes();
		byte[] bitmask2 = readMask(stream, nodes, bytesMarking).getBytes();

		byte[] marking = new byte[nodes];

		short enabled = 0;
		short future = 0;
		for (short n = 0; n < nodes; n++) {
			int bte = n / 8;
			int bit = n % 8;
			int one1 = (bitmask1[bte] & POWER[bit]);
			int one2 = (bitmask2[bte] & POWER[bit]);
			if (one1 == 0 && one2 == 0) {
				marking[n] = ReducedTokenCountMarking.NONE;
			} else if (one1 != 0 && one2 == 0) {
				marking[n] = ReducedTokenCountMarking.INENABLED;
				enabled++;
			} else if (one1 == 0 && one2 != 0) {
				marking[n] = ReducedTokenCountMarking.INFUTURE;
				future++;
			} else if (one1 != 0 && one2 != 0) {
				marking[n] = ReducedTokenCountMarking.POSITIVECOUNT;
			}
		}
		return new ReducedTokenCountMarking(marking, future, enabled);
	}

	protected boolean checkMarkingDoubleBitMask(InputStream stream, ReducedTokenCountMarking m) throws IOException {

		byte[] bitmask1 = readMask(stream, nodes, bytesMarking).getBytes();
		byte[] bitmask2 = readMask(stream, nodes, bytesMarking).getBytes();

		byte[] marking = m.getMarking();

		boolean ok = true;
		for (short n = 0; ok && n < nodes; n++) {
			int bte = n / 8;
			int bit = n % 8;
			int one1 = (bitmask1[bte] & POWER[bit]);
			int one2 = (bitmask2[bte] & POWER[bit]);
			if (marking[n] == ReducedTokenCountMarking.NONE) {
				ok &= one1 == 0;
				ok &= one2 == 0;
			} else if (marking[n] == ReducedTokenCountMarking.INENABLED) {
				ok &= one1 != 0;
				ok &= one2 == 0;
			} else if (marking[n] == ReducedTokenCountMarking.INFUTURE) {
				ok &= one1 == 0;
				ok &= one2 != 0;
			} else if (marking[n] == ReducedTokenCountMarking.POSITIVECOUNT) {
				ok &= one1 != 0;
				ok &= one2 != 0;
			}
		}
		return ok;
	}

	protected void writeParikhDoubleBitMask(OutputStream stream, ShortShortMultiset set) throws IOException {

		byte[] bitmask1 = new byte[bytesParikh];
		byte[] bitmask2 = new byte[bytesParikh];
		short[] greater = new short[activities];
		//iterating over all elements if possible indices is faster than 
		// first getting the relevant keys.
		int v = 0;
		for (short a = 0; a < activities; a++) {
			short i = set.get(a);
			if (i == 0) {
				continue;
			}
			int bte = a / 8;
			int bit = a % 8;
			if (i == 1) {
				bitmask1[bte] = (byte) (bitmask1[bte] | POWER[bit]);

			} else if (i == 2) {
				bitmask2[bte] = (byte) (bitmask2[bte] | POWER[bit]);

			} else if (i > 2) {
				bitmask1[bte] = (byte) (bitmask1[bte] | POWER[bit]);
				bitmask2[bte] = (byte) (bitmask2[bte] | POWER[bit]);
				greater[v++] = i;
			}
		}
		stream.write(bitmask1);
		stream.write(bitmask2);
		for (int i = 0; i < v; i++) {
			writeShortToByteArray(stream, greater[i]);
		}

	}

	protected boolean checkParikhDoubleBitMask(InputStream stream, ShortShortMultiset set) throws IOException {

		byte[] bitmask1 = new byte[bytesParikh];
		byte[] bitmask2 = new byte[bytesParikh];
		short[] greater = new short[activities];
		//iterating over all elements if possible indices is faster than 
		// first getting the relevant keys.
		int v = 0;
		for (short a = 0; a < activities; a++) {
			short i = set.get(a);
			if (i == 0) {
				continue;
			}
			int bte = a / 8;
			int bit = a % 8;
			if (i == 1) {
				bitmask1[bte] = (byte) (bitmask1[bte] | POWER[bit]);

			} else if (i == 2) {
				bitmask2[bte] = (byte) (bitmask2[bte] | POWER[bit]);

			} else if (i > 2) {
				bitmask1[bte] = (byte) (bitmask1[bte] | POWER[bit]);
				bitmask2[bte] = (byte) (bitmask2[bte] | POWER[bit]);
				greater[v++] = i;
			}
		}
		BitMask m1 = readMask(stream, activities, bytesParikh);
		boolean ok = Arrays.equals(m1.getBytes(), bitmask1);
		BitMask m2 = readMask(stream, activities, bytesParikh);
		ok &= Arrays.equals(m2.getBytes(), bitmask2);

		for (int i = 0; ok && (i < v); i++) {
			ok &= greater[i] == readShortFromStream(stream);
		}
		return ok;
	}

	protected ShortShortMultiset readParikhDoubleBitmask(InputStream stream) throws IOException {
		ShortShortMultiset parikh = new ShortShortMultiset(activities);
		BitMask bitmask1 = readMask(stream, activities, bytesParikh);
		BitMask bitmask2 = readMask(stream, activities, bytesParikh);

		for (int i = 0; i < bitmask1.getBytes().length; i++) {
			byte b1 = bitmask1.getBytes()[i];
			byte b2 = bitmask2.getBytes()[i];
			for (int j = 0; j < 8; j++) {
				int one1 = (b1 & POWER[j]);
				int one2 = (b2 & POWER[j]);
				if (one1 != 0 && one2 == 0) {
					parikh.put((short) (i * 8 + j), (short) 1);
				} else if (one1 == 0 && one2 != 0) {
					parikh.put((short) (i * 8 + j), (short) 2);
				} else if (one1 != 0 && one2 != 0) {
					parikh.put((short) (i * 8 + j), readShortFromStream(stream));
				}
			}
		}

		return parikh;
	}

	public int getMaxByteCount() {
		return maxBytes;
	}
}
