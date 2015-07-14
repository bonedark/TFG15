/**
 * 
 */
package org.processmining.plugins.petrinet.replayresult.visualization.projection;

import info.clearthought.layout.TableLayout;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.deckfour.xes.classification.XEventClass;
import org.processmining.framework.util.ui.widgets.ProMPropertiesPanel;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import com.fluxicon.slickerbox.factory.SlickerFactory;

/**
 * @author aadrians Nov 2, 2011
 * 
 */
public class AlignmentFilterPanel extends JPanel {

	private static final long serialVersionUID = -801212812906789799L;

	private JCheckBox[] moveModelOnly;
	private JCheckBox[] moveLogModel;
	private JCheckBox[] moveLogOnly;

	public AlignmentFilterPanel(final PNLogReplayProjectedVisPanel mainPanel, Transition[] transArray, XEventClass[] acArray) {

		// move on model, and move on log+model
		ProMPropertiesPanel moveOnModelPanel = new ProMPropertiesPanel("Move on Model");
		moveOnModelPanel.setPreferredSize(new Dimension(moveOnModelPanel.getWidth(), 200));
		moveModelOnly = new JCheckBox[transArray.length];
		final JCheckBox checkAllBoxMoveModel = moveOnModelPanel.addCheckBox("Select/deselect all", true);

		ProMPropertiesPanel moveLogModelPanel = new ProMPropertiesPanel("Move both Log+Model");
		moveLogModelPanel.setPreferredSize(new Dimension(moveLogModelPanel.getWidth(), 200));
		moveLogModel = new JCheckBox[transArray.length];
		final JCheckBox checkAllBoxLogModel = moveLogModelPanel.addCheckBox("Select/deselect all", true);

		for (int i=0; i < transArray.length; i++) {
			// move model only
			JCheckBox checkBox = moveOnModelPanel.addCheckBox(transArray[i].getLabel(), true);
			moveModelOnly[i] = checkBox;

			// move sync
			JCheckBox checkBoxLogModel = moveLogModelPanel.addCheckBox(transArray[i].getLabel(), true);
			moveLogModel[i] = checkBoxLogModel;
		}

		checkAllBoxMoveModel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (JCheckBox key : moveModelOnly) {
					key.setSelected(checkAllBoxMoveModel.isSelected());
				}
			}
		});

		checkAllBoxLogModel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (JCheckBox key : moveLogModel) {
					key.setSelected(checkAllBoxLogModel.isSelected());
				}
			}
		});

		// move log only
		ProMPropertiesPanel moveOnLogPanel = new ProMPropertiesPanel("Move on Log");
		moveOnLogPanel.setPreferredSize(new Dimension(moveOnLogPanel.getWidth(), 200));
		moveLogOnly = new JCheckBox[acArray.length];
		final JCheckBox checkAllBoxMoveLog = moveOnLogPanel.addCheckBox("Select/deselect all", true);

		for (int i=0; i < acArray.length; i++) {
			// move on log only
			JCheckBox checkBox = moveOnLogPanel.addCheckBox(acArray[i].toString(), true);
			moveLogOnly[i] = checkBox;
		}

		checkAllBoxMoveLog.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (JCheckBox key : moveLogOnly) {
					key.setSelected(checkAllBoxMoveLog.isSelected());
				}
			}
		});

		// add all components
		SlickerFactory factory = SlickerFactory.instance();
		double size[][] = new double[][] { { TableLayout.FILL },
				{ 30, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED } };
		setLayout(new TableLayout(size));

		JButton filterBtn = factory.createButton("Perform filtering");
		filterBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mainPanel.filterAlignment(getCheckBoxSelection(moveLogModel), getCheckBoxSelection(moveModelOnly),
						getCheckBoxSelection(moveLogOnly));
			}
		});
		add(filterBtn, "0,0");
		add(moveLogModelPanel, "0,1");
		add(moveOnModelPanel, "0,2");
		add(moveOnLogPanel, "0,3");
	}

	protected boolean[] getCheckBoxSelection(JCheckBox[] array) {
		boolean[] res = new boolean[array.length];
		for (int i=0; i < array.length; i++){
			res[i] = array[i].isSelected();
		}
		return res;
	}
}
