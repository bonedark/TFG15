/**
 * 
 */
package org.processmining.plugins.flex.replayer.util;



/**
 * @author Arya Adriansyah
 * @email a.adriansyah@tue.nl
 * @version Feb 13, 2011
 */
public class FlexBinding {
	private Short encodedInputBinding;
	private Short encodedNode;
	private Short encodedOutputBinding;

	public FlexBinding(Short encInputBinding, Short encNode, Short encOutputBinding) {
		this.encodedInputBinding = encInputBinding;
		this.encodedNode = encNode;
		this.encodedOutputBinding = encOutputBinding;
	}

	/**
	 * @return the encodedInputBinding
	 */
	public Short getEncodedInputBinding() {
		return encodedInputBinding;
	}

	/**
	 * @param encodedInputBinding the encodedInputBinding to set
	 */
	public void setEncodedInputBinding(Short encodedInputBinding) {
		this.encodedInputBinding = encodedInputBinding;
	}

	/**
	 * @return the encodedNode
	 */
	public Short getEncodedNode() {
		return encodedNode;
	}

	/**
	 * @param encodedNode the encodedNode to set
	 */
	public void setEncodedNode(Short encodedNode) {
		this.encodedNode = encodedNode;
	}

	/**
	 * @return the encodedOutputBinding
	 */
	public Short getEncodedOutputBinding() {
		return encodedOutputBinding;
	}

	/**
	 * @param encodedOutputBinding the encodedOutputBinding to set
	 */
	public void setEncodedOutputBinding(Short encodedOutputBinding) {
		this.encodedOutputBinding = encodedOutputBinding;
	}
	
	
}
