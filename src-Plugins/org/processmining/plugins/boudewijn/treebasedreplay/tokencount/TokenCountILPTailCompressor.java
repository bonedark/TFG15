package org.processmining.plugins.boudewijn.treebasedreplay.tokencount;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import nl.tue.storage.compressor.BitMask;

public class TokenCountILPTailCompressor extends AbstractTokenCountHybridTailCompressor<TokenCountILPTail> {

	private final int variableBytes;
	private final int variables;
	protected final int maxBytes;

	public TokenCountILPTailCompressor(short nodes, short activities, short leafs) {
		super(nodes, activities, leafs);
		this.variables = 3 * nodes - leafs + activities;
		this.variableBytes = BitMask.getNumBytes(variables);
		this.maxBytes = 4 + 2 * variableBytes + variables * 2;
	}

	public void deflate(TokenCountILPTail object, OutputStream stream) throws IOException {
		writeIntToByteArray(stream, object.getEstimate());

		writeVarsDoubleBitMask(stream, object.getVariables());
	}

	public TokenCountILPTail inflate(InputStream stream) throws IOException {
		int est = readIntFromStream(stream);

		// read the marking
		TokenCountILPTail m = new TokenCountILPTail(est, readVarsDoubleBitmask(stream));
		return m;
	}

	protected void writeVarsDoubleBitMask(OutputStream stream, byte[] vars) throws IOException {

		assert vars.length == variables;
		byte[] bitmask1 = new byte[variableBytes];
		byte[] bitmask2 = new byte[variableBytes];
		byte[] greater = new byte[variables];
		//iterating over all elements if possible indices is faster than 
		// first getting the relevant keys.
		int v = 0;
		for (int a = 0; a < variables; a++) {
			byte i = vars[a];
			if (i == 0) {
				continue;
			}
			int bte = a / 8;
			int bit = a % 8;
			if (i == 1) {
				bitmask1[bte] |= POWER[bit];

			} else if (i == 2) {
				bitmask2[bte] |= POWER[bit];

			} else if (i > 2) {
				bitmask1[bte] |= POWER[bit];
				bitmask2[bte] |= POWER[bit];
				greater[v++] = i;
			}
		}
		stream.write(bitmask1);
		stream.write(bitmask2);
		for (int i = 0; i < v; i++) {
			stream.write(greater[i]);
		}
		//		System.out.println("written:" + Arrays.toString(bitmask1) + Arrays.toString(bitmask2));
	}

	protected byte[] readVarsDoubleBitmask(InputStream stream) throws IOException {
		byte[] vars = new byte[variables];
		byte[] bitmask1 = readMask(stream, variables, variableBytes).getBytes();
		byte[] bitmask2 = readMask(stream, variables, variableBytes).getBytes();

		for (int i = 0; i < variableBytes; i++) {
			byte b1 = bitmask1[i];
			byte b2 = bitmask2[i];
			for (int j = 0; j < 8; j++) {
				int one1 = (b1 & (byte) POWER[j]);
				int one2 = (b2 & (byte) POWER[j]);
				if (one1 != 0 && one2 == 0) {
					assert i * 8 + j < variables;
					vars[i * 8 + j] = 1;
				} else if (one1 == 0 && one2 != 0) {
					assert i * 8 + j < variables;
					vars[i * 8 + j] = 2;
				} else if (one1 != 0 && one2 != 0) {
					assert i * 8 + j < variables;
					vars[i * 8 + j] = (byte) stream.read();
					assert vars[i * 8 + j] > 2;
				}
			}
		}
		//		System.out.println("read:   " + Arrays.toString(bitmask1) + Arrays.toString(bitmask2));

		return vars;
	}

	public int getMaxByteCount() {
		return maxBytes;
	}

}
