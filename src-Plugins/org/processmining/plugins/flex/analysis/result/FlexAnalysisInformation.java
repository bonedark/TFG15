/**
 * 
 */
package org.processmining.plugins.flex.analysis.result;


/**
 * Similar class as Net Analysis Information for Petri net
 * @author aadrians
 *
 */
public abstract class FlexAnalysisInformation<T> {

	public static enum UnDetBool {
		TRUE("True"), //
		FALSE("False"), //
		UNDETERMINED("Undetermined");
		private final String label;

		private UnDetBool(String label) {
			this.label = label;
		}

		public String toString() {
			return label;
		}
	}

	public static class SOUNDNESS extends FlexAnalysisInformation<UnDetBool> {
		public SOUNDNESS() {
			super("Flexible model is sound");
		}
	}

	private final String label;
	private T value;

	/**
	 * default constructor
	 */
	public FlexAnalysisInformation(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public boolean equals(Object o) {
		if (o.getClass().equals(this.getClass())) {
			FlexAnalysisInformation<?> info = (FlexAnalysisInformation<?>) o;
			return (value.equals(info.getValue()) && label.equals(info.getLabel()));
		} else {
			return false;
		}
	}

	public void setValue(T value) {
		this.value = value;
	}

	public T getValue() {
		return value;
	}

}
