package org.processmining.plugins.astar.petrinet.impl;

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

public class PHeadCompressor<T extends Tail> extends AbstractCompressor<PHead> implements
		EqualOperation<State<PHead, T>>, HashOperation<State<PHead, T>> {

	private final short activities;
	private final short places;
	private final int bits;
	private final int bytesPlaces;
	private final int bytesActivities;
	private final int maxBytes;

	/**
	 * Construct the compressor with a fixed length for all vectors.
	 * 
	 * @param length
	 */
	public PHeadCompressor(short places, short activities) {
		this.places = places;
		this.activities = activities;
		this.bits = PHead.computeBitsForParikh(activities, places);
		this.bytesPlaces = BitMask.getNumBytes(places);
		this.bytesActivities = BitMask.getNumBytes(activities);
		this.maxBytes = 4 + bytesPlaces + places * 2 + bytesActivities + activities * 2;
	}

	public PHead inflate(InputStream stream) throws IOException {
		// skip the hashCode
		int hashCode = readIntFromStream(stream);
		// read the marking
		BitMask mask = readMask(stream, places, bytesPlaces);
		ShortShortMultiset marking = inflateContent(stream, BitMask.getIndices(mask), places);
		// read the vector
		mask = readMask(stream, activities, bytesActivities);
		ShortShortMultiset parikh = inflateContent(stream, BitMask.getIndices(mask), activities);
		return new PHead(marking, parikh, hashCode, bits);
	}

	public void deflate(PHead object, OutputStream stream) throws IOException {
		// cache the hashcode for quick lookup
		writeIntToByteArray(stream, object.hashCode());
		// store the marking
		deflate(object.getMarking(), stream, places);
		// store the parikh vector
		deflate(object.getParikhVector(), stream, activities);
	}

	//	private static long inf = 0;
	//	private static long def = 0;
	//	private static int cnt = 0;

	public boolean equals(State<PHead, T> object, CompressedStore<State<PHead, T>> store, long l)
			throws StorageException, IOException {
		// The following test code showed that inflating
		// is about 25% faster than deflating
		//
		//		long s1 = System.nanoTime();
		//		boolean b1 = equalsDeflating(object.getHead(), store, l);
		//		long s2 = System.nanoTime();
		//		boolean b2 = equalsInflating(object, store, l);
		//		long s3 = System.nanoTime();
		//		assert (b1 == b2);
		//		inf += s3 - s2;
		//		def += s2 - s1;
		//		if (++cnt % 100000 == 0) {
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

	private boolean equalsInflating(State<PHead, T> vector, CompressedStore<State<PHead, T>> store, long l)
			throws IOException {
		InputStream stream = store.getStreamForObject(l);

		int hashCode = readIntFromStream(stream);
		if (hashCode != vector.getHead().hashCode()) {
			return false;
		}

		BitMask mask = readMask(stream, places, bytesPlaces);
		ShortShortMultiset m = vector.getHead().getMarking();
		if (mask.getOnes() != m.size()) {
			return false;
		}
		ShortShortMultiset marking = inflateContent(stream, BitMask.getIndices(mask), places);
		mask = readMask(stream, activities, bytesActivities);
		ShortShortMultiset p = vector.getHead().getParikhVector();
		if (mask.getOnes() != p.size()) {
			return false;
		}
		if (!marking.equals(m)) {
			return false;
		}
		if (!inflateContent(stream, BitMask.getIndices(mask), activities).equals(p)) {
			return false;
		}
		return true;

	}

	public int getHashCode(State<PHead, T> object) {
		return object.getHead().hashCode();
	}

	public int getHashCode(CompressedStore<State<PHead, T>> store, long l) throws StorageException {
		try {
			InputStream stream = store.getStreamForObject(l);
			return readIntFromStream(stream);
		} catch (IOException e) {
			throw new StorageException(e);
		}

	}

	public void skipMarking(InputStream stream) throws IOException {
		BitMask mask = readMask(stream, places, bytesPlaces);
		stream.skip(2 * mask.getOnes());
	}

	public void skipParikhVector(InputStream stream) throws IOException {
		BitMask mask = readMask(stream, activities, bytesActivities);
		stream.skip(2 * mask.getOnes());
	}

	public ShortShortMultiset inflateParikhVector(InputStream stream) throws IOException {
		BitMask mask = readMask(stream, activities, bytesActivities);
		return inflateContent(stream, BitMask.getIndices(mask), activities);
	}

	public int getMaxByteCount() {
		return maxBytes;
	}

}
