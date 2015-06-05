/**
 * 
 */
package org.processmining.plugins.alignment.conformance;

import java.text.NumberFormat;

import org.processmining.framework.util.HTMLToString;

/**
 * @author aadrians
 * Oct 16, 2011
 *
 */
public class AlignmentConformanceResult implements HTMLToString {
	private double precision = 0.0000;
	private double generalization = 0.0000;
	private double generalizationWOFreq = 0.0000;
	
	/**
	 * @return the precision
	 */
	public double getPrecision() {
		return precision;
	}
	/**
	 * @param precision the precision to set
	 */
	public void setPrecision(double precision) {
		this.precision = precision;
	}
	/**
	 * @return the generalization
	 */
	public double getGeneralization() {
		return generalization;
	}
	/**
	 * @param generalization the generalization to set
	 */
	public void setGeneralization(double generalization) {
		this.generalization = generalization;
	}
	/**
	 * @return the generalizationWOFreq
	 */
	public double getGeneralizationWOFreq() {
		return generalizationWOFreq;
	}
	/**
	 * @param generalizationWOFreq the generalizationWOFreq to set
	 */
	public void setGeneralizationWOFreq(double generalizationWOFreq) {
		this.generalizationWOFreq = generalizationWOFreq;
	}
	public String toHTMLString(boolean includeHTMLTags) {
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(5);
		nf.setMaximumFractionDigits(5);
		
		StringBuffer buffer = new StringBuffer();

		if (includeHTMLTags) {
			buffer.append("<html>");
		}
		buffer.append("<head>");
		buffer.append("<title>Alignment-based Conformance checking</title>");
		buffer.append("</head><body><font fact=\"Arial\">");
		
		buffer.append("<h2>Precision : " + nf.format(precision)+ "</h2>");
		buffer.append("<h2>Generalization : " + nf.format(generalization)+ "</h2>");
		buffer.append("<h2>Generalization (with normalized freq) : " + nf.format(generalizationWOFreq)+ "</h2>");
		
		buffer.append("</font></body>");
		if (includeHTMLTags) {
			buffer.append("</html>");
		}
		return buffer.toString();
	}
	
	
}
