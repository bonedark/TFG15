package org.processmining.plugins.astar.petrinet.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import nl.tue.storage.compressor.BitMask;

public class PILPTailCompressor extends AbstractCompressor<PILPTail> implements TailCompressor<PILPTail> {

	private final short places;
	private final short activities;
	private final int columns;
	private final int bytesColumns;
	private final int bytesPlaces;
	private final int bytesActivities;
	private final int maxBytes;

	public PILPTailCompressor(int columns, short places, short activities) {
		this.columns = columns;
		this.places = places;
		this.activities = activities;
		this.bytesColumns = BitMask.getNumBytes(columns);
		this.bytesPlaces = BitMask.getNumBytes(places);
		this.bytesActivities = BitMask.getNumBytes(activities);
		this.maxBytes = 8 + bytesColumns + columns * 2;
	}

	public void deflate(PILPTail tail, OutputStream stream) throws IOException {
		writeDoubleToByteArray(stream, tail.getEstimate());

		BitMask mask = makeShortListBitMask(columns, tail.getVariables());
		stream.write(mask.getBytes());

		for (int i = 0; i < columns; i++) {
			if (tail.getVariables()[i] > 0) {
				//writeDoubleToByteArray(stream, object.getVariables()[i]);
				writeShortToByteArray(stream, tail.getVariables()[i]);
			}
		}
	}

	public PILPTail inflate(InputStream stream) throws IOException {
		double est = readDoubleFromStream(stream);

		// read the marking
		BitMask mask = readMask(stream, columns, bytesColumns);
		short[] variables = new short[columns];
		for (int i : BitMask.getIndices(mask)) {
			//variables[i] = readDoubleFromStream(stream);
			variables[i] = readShortFromStream(stream);
		}
		return new PILPTail(est, variables);
	}

	public void skipHead(InputStream stream) throws IOException {
		stream.skip(4);
		BitMask mask = readMask(stream, places, bytesPlaces);
		stream.skip(2 * mask.getOnes());
		mask = readMask(stream, activities, bytesActivities);
		stream.skip(2 * mask.getOnes());

	}

	public int getMaxByteCount() {
		return maxBytes;
	}
}
