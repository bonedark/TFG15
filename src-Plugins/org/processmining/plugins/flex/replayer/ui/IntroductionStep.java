package org.processmining.plugins.flex.replayer.ui;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import org.processmining.plugins.flex.replayer.algorithms.IFlexLogReplayAlgorithm;

import com.fluxicon.slickerbox.factory.SlickerFactory;

/**
 * @author arya
 * @email arya.adriansyah@gmail.com
 * @version Dec 1, 2009
 */
public class IntroductionStep extends ReplayStep {
	private static final long serialVersionUID = 1858631217943805055L;

	public IntroductionStep() {
		initComponents();
	}

	public boolean precondition() {
		return true;
	}

	public void readSettings(IFlexLogReplayAlgorithm algorithm) {
	}

	private void initComponents() {
		double size[][] = { { TableLayoutConstants.FILL }, { TableLayoutConstants.FILL } };
		setLayout(new TableLayout(size));
		String body = "<p>This wizard will guide you to configure this log replay. ";
		body += "Configurating the replay consists of several tasks:";
		body += "<ol><li>Mapping Flexible model's nodes to event class (if there is no such mapping yet), and</li>";
		body += "<li>Select replay algorithm to be used.</li></ol>";
		body += "The wizard will allow you to perform these tasks in the given order.</p>";
		add(SlickerFactory.instance().createLabel("<html><h1>Introduction</h1>" + body), "0, 0, l, t");
	}
}
