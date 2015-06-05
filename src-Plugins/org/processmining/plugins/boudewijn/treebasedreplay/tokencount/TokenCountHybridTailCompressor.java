package org.processmining.plugins.boudewijn.treebasedreplay.tokencount;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TokenCountHybridTailCompressor extends AbstractTokenCountHybridTailCompressor<TokenCountHybridTail> {

	public TokenCountHybridTailCompressor(short nodes, short activities, short leafs) {
		super(nodes, activities, leafs);
	}

	public void deflate(TokenCountHybridTail object, OutputStream stream) throws IOException {
		writeIntToByteArray(stream, object.getEstimate());
	}

	public int getMaxByteCount() {
		return 4;
	}

	public TokenCountHybridTail inflate(InputStream stream) throws IOException {
		int est = readIntFromStream(stream);
		// read the marking
		TokenCountHybridTail m = new TokenCountHybridTail(est);
		return m;
	}

}
