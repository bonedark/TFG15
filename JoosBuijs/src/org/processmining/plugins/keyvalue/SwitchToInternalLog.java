package org.processmining.plugins.keyvalue;

import java.util.Set;

import javax.swing.JCheckBox;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryBufferedImpl;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.util.ui.widgets.ProMPropertiesPanel;

/**
 * @author michael
 * 
 */
@Plugin(
		name = "Switch Log Representation",
			parameterLabels = { "Internal", "Trove" },
			returnLabels = {},
			returnTypes = {},
			userAccessible = true)
public class SwitchToInternalLog {
	/**
	 * @param context
	 * @param internal
	 */
	@PluginVariant(variantLabel = "Default settings", requiredParameterLabels = { 0, 1 })
	public static void switchRepresentation(final PluginContext context, final Boolean internal, final Boolean trove) {
		if (internal) {
			if (trove) {
				XFactory factory = SwitchToInternalLog.getFactory(XFactoryRegistry.instance().getAvailable(),
						XFactoryLazyTrove.class);
				if (factory == null) {
					XFactoryRegistry.instance().register(factory = new XFactoryLazyTrove());
				}
				XFactoryRegistry.instance().setCurrentDefault(factory);
			} else {
				XFactoryRegistry.instance().setCurrentDefault(
						SwitchToInternalLog.getFactory(XFactoryRegistry.instance().getAvailable(),
								XFactoryNaiveImpl.class));
			}
		} else {
			XFactoryRegistry.instance().setCurrentDefault(
					SwitchToInternalLog.getFactory(XFactoryRegistry.instance().getAvailable(),
							XFactoryBufferedImpl.class));
		}
	}

	/**
	 * @param context
	 * @param set
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@UITopiaVariant(
			affiliation = UITopiaVariant.EHV,
				author = "M. Westergaard",
				email = "m.westergaard@tue.nl",
				uiLabel = UITopiaVariant.USEPLUGIN)
	@PluginVariant(variantLabel = "Default settings", requiredParameterLabels = {})
	public static void switchRepresentation(final UIPluginContext context) {
		final ProMPropertiesPanel properties = new ProMPropertiesPanel(null);
		final JCheckBox checkBox = properties.addCheckBox("Use internal representation", XFactoryRegistry.instance()
				.currentDefault().getClass() == XFactoryNaiveImpl.class);
		final JCheckBox trove = properties.addCheckBox("Use Trove map representation", false);
		final InteractionResult result = context.showConfiguration("Setup Log Representation", properties);
		if (result == InteractionResult.CONTINUE) {
			SwitchToInternalLog.switchRepresentation(context, checkBox.isSelected(), trove.isSelected());
		}
		context.getFutureResult(0).cancel(true);
	}

	private static XFactory getFactory(final Set<XFactory> available, final Class<?> clazz) {
		for (final XFactory factory : available) {
			if (factory.getClass().equals(clazz))
				return factory;
		}
		return null;
	}

}
