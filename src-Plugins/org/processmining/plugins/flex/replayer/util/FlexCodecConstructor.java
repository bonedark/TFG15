/**
 * 
 */
package org.processmining.plugins.flex.replayer.util;

import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.connections.ConnectionManager;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.connections.flexiblemodel.FlexCodecConnection;
import org.processmining.models.flexiblemodel.Flex;

/**
 * @author arya
 *
 */
@Plugin(name = "Construct Codec of Flexible model", returnLabels = { "Flexible model codec" }, returnTypes = { FlexCodec.class }, parameterLabels = {
		"Flexible model" }, help = "Construct codec of Flexible model.", userAccessible = true)
public class FlexCodecConstructor {
	@PluginVariant(variantLabel = "From Flexible model and Log", requiredParameterLabels = { 0 })
	public FlexCodec constructCodec(final PluginContext context, Flex flex){
		// check connection
		ConnectionManager connManager = context.getConnectionManager();
		try {
			FlexCodecConnection conn = connManager.getFirstConnection(FlexCodecConnection.class, context, flex);
			return conn.getObjectWithRole(FlexCodecConnection.FLEXCODEC);
		} catch (ConnectionCannotBeObtained exc){
			FlexCodec codec = new FlexCodec(flex);
			context.addConnection(new FlexCodecConnection("Connection to codec of " + flex.getLabel(), flex, codec));
			return codec;
		}
	}
}
