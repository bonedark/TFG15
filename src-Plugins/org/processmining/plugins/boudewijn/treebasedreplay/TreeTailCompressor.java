package org.processmining.plugins.boudewijn.treebasedreplay;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import nl.tue.storage.compressor.BitMask;

import org.processmining.plugins.astar.petrinet.impl.AbstractCompressor;

public class TreeTailCompressor extends AbstractCompressor<TreeILPTail> {

	private final int columns;
	private final int nodes;
	private final int activities;
	private final int bytesColumn;
	private final int bytesNodes;
	private final int bytesActivities;
	private final int maxBytes;

	public TreeTailCompressor(int columns, int nodes, int activities) {
		this.columns = columns;
		this.nodes = nodes;
		this.activities = activities;

		this.bytesColumn = BitMask.getNumBytes(columns);
		this.bytesNodes = BitMask.getNumBytes(nodes);
		this.bytesActivities = BitMask.getNumBytes(activities);

		this.maxBytes = 4 + bytesColumn + columns * 2;
	}

	public void deflate(TreeILPTail object, OutputStream stream) throws IOException {
		writeIntToByteArray(stream, object.getEstimate());

		BitMask mask = makeShortListBitMask(columns, object.getVariables());
		stream.write(mask.getBytes());

		for (int i = 0; i < columns; i++) {
			if (object.getVariables()[i] > 0) {
				//writeDoubleToByteArray(stream, object.getVariables()[i]);
				writeShortToByteArray(stream, object.getVariables()[i]);
			}
		}
	}

	public TreeILPTail inflate(InputStream stream) throws IOException {
		int est = readIntFromStream(stream);

		// read the marking
		BitMask mask = readMask(stream, columns, bytesColumn);
		short[] variables = new short[columns];
		for (int i : BitMask.getIndices(mask)) {
			//variables[i] = readDoubleFromStream(stream);
			variables[i] = readShortFromStream(stream);
		}
		return new TreeILPTail(est, variables);
	}

	public void skipHead(InputStream stream) throws IOException {
		stream.skip(4);
		BitMask mask = readMask(stream, nodes, bytesNodes);
		mask = readMask(stream, activities, bytesActivities);
		stream.skip(2 * mask.getOnes());
	}

	protected static BitMask makeDoubleListBitMask(int size, double[] marking) {
		byte[] bitmask = new byte[BitMask.getNumBytes(size)];
		//iterating over all elements if possible indices is faster than 
		// first getting the relevant keys.
		for (short j = 0; j < marking.length; j++) {
			if (marking[j] > 0) {
				int bte = j / 8;
				int bit = j % 8;
				bitmask[bte] = (byte) (bitmask[bte] | POWER[bit]);
			}
		}
		return new BitMask(bitmask, marking.length);
	}

	public int getMaxByteCount() {
		return maxBytes;
	}

}
