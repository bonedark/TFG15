package org.processmining.plugins.joosbuijs.processtrees.geneticminer;

import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.Progress;
import org.uncommons.watchmaker.framework.PopulationData;
import org.uncommons.watchmaker.framework.TerminationCondition;

public class ProMCancelTerminationCondition implements TerminationCondition{
	private final Progress progress;
	
	public ProMCancelTerminationCondition(final PluginContext context){
		this.progress = context.getProgress();
	}
	
	public ProMCancelTerminationCondition(final Progress progress){
		this.progress = progress;
	}

	public boolean shouldTerminate(PopulationData<?> populationData) {
		return progress.isCancelled();
	}

}
