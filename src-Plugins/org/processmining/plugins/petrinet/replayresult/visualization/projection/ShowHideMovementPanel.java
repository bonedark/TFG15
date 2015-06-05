/**
 * 
 */
package org.processmining.plugins.petrinet.replayresult.visualization.projection;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.fluxicon.slickerbox.factory.SlickerFactory;

/**
 * @author aadrians
 * Nov 2, 2011
 *
 */
public class ShowHideMovementPanel extends JPanel{
	private static final long serialVersionUID = -801212812906789799L;
	private final JCheckBox logModel;
	private final JCheckBox moveModel;
	private final PNLogReplayProjectedVisPanel mainPanel; 
	
	public ShowHideMovementPanel(PNLogReplayProjectedVisPanel mainPanel){

		this.mainPanel = mainPanel;
		
		SlickerFactory factory = SlickerFactory.instance();
		logModel = factory.createCheckBox("Show log+model moves", true);
		moveModel = factory.createCheckBox("Show model only moves", true);
		
		DefaultAction defaultAction = new DefaultAction();
		logModel.addActionListener(defaultAction);
		moveModel.addActionListener(defaultAction);
		
		// add checkbox
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		logModel.setAlignmentX(Component.LEFT_ALIGNMENT);
		moveModel.setAlignmentX(Component.LEFT_ALIGNMENT);
		add(logModel);
		add(moveModel);
	}
	
	class DefaultAction implements ActionListener {
		public void actionPerformed(ActionEvent arg0) {
			mainPanel.constructVisualization(logModel.isSelected(), moveModel.isSelected());
			mainPanel.repaint();
		}
	}
}
