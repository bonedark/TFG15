/**
 * 
 */
package org.processmining.plugins.petrinet.replayresult.visualization.projection;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;

import org.freehep.util.export.ExportDialog;
import org.processmining.framework.util.ui.scalableview.ScalableComponent;

import com.fluxicon.slickerbox.components.SlickerButton;

/**
 * @author aadrians
 * Nov 1, 2011
 *
 */
public class ExportPanel extends JPanel {

	private static final long serialVersionUID = 7153768335241741777L;

	private SlickerButton exportButton;
	
	public ExportPanel(final ScalableComponent graph){
		
		double size[][] = { { 10, TableLayoutConstants.FILL, 10 }, { 10, TableLayoutConstants.FILL, 10 } };
		setLayout(new TableLayout(size));
		exportButton = new SlickerButton("Export view...");
		exportButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ExportDialog export = new ExportDialog();
				export.showExportDialog(null, "Export view as ...", graph.getComponent(), "View");
			}
		});
		this.add(exportButton, "1, 1");
		
		this.setPreferredSize(new Dimension(100,50));
	}
	
}