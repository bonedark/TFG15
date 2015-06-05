/**
 * 
 */
package org.processmining.models.connections.flexiblemodel;

import org.processmining.models.flexiblemodel.Flex;
import org.processmining.plugins.flex.analysis.result.FlexAnalysisInformation;

/**
 * @author aadrians
 *
 */
public class FlexSoundnessInfoConnection extends AbstractFlexAnalysisInfoConnection {

	public FlexSoundnessInfoConnection(String label, Flex causalNet, FlexAnalysisInformation.SOUNDNESS flexAnalysisInformation) {
		super(label, causalNet, flexAnalysisInformation);
	}

}
