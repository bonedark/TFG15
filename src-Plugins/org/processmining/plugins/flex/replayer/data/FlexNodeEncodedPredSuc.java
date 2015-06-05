package org.processmining.plugins.flex.replayer.data;

import java.util.LinkedList;
import java.util.List;

/**
 * @author aadrians
 * 
 */
public class FlexNodeEncodedPredSuc {
	private List<List<Short>> encodedPredecessor = new LinkedList<List<Short>>();
	private List<List<Short>> encodedSuccessor = new LinkedList<List<Short>>();

	public FlexNodeEncodedPredSuc(List<List<Short>> setSetFlexNodeEncodedIn,
			List<List<Short>> setSetFlexNodeEncodedOut) {
		setEncodedPredecessor(setSetFlexNodeEncodedIn);
		setEncodedSuccessor(setSetFlexNodeEncodedOut);
	}

	public List<List<Short>> getEncodedPredecessor() {
		return encodedPredecessor;
	}

	public void setEncodedPredecessor(List<List<Short>> encodedPredecessor) {
		this.encodedPredecessor = encodedPredecessor;
	}

	public List<List<Short>> getEncodedSuccessor() {
		return encodedSuccessor;
	}

	public void setEncodedSuccessor(List<List<Short>> encodedSuccessor) {
		this.encodedSuccessor = encodedSuccessor;
	}
}
