/**
 * 
 */
package org.processmining.models.connections.flexiblemodel;

import org.processmining.framework.connections.impl.AbstractConnection;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.plugins.flex.analysis.result.FlexAnalysisInformation;

/**
 * @author aadrians
 *
 */
public abstract class AbstractFlexAnalysisInfoConnection extends AbstractConnection {

	public final static String NET = "CausalNet";
	public final static String ANALYSISINFORMATION = "AnalysisInformation";
	
	protected AbstractFlexAnalysisInfoConnection(String label, Flex causalNet, FlexAnalysisInformation<?> netAnalysisInformation) {
		super(label);
		put(NET, causalNet);
		put(ANALYSISINFORMATION, netAnalysisInformation);
	}
}
