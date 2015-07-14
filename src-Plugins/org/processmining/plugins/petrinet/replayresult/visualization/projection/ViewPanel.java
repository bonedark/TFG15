/**
 * 
 */
package org.processmining.plugins.petrinet.replayresult.visualization.projection;

import info.clearthought.layout.TableLayout;

import javax.swing.JPanel;


/**
 * @author aadrians
 * Nov 1, 2011
 *
 */
public class ViewPanel extends JPanel {

	private static final long serialVersionUID = 7931015104099746628L;
	private PIPPanel pip;
	private ZoomPanel zoom;
	public ViewPanel(PNLogReplayProjectedVisPanel mainPanel, int maxZoom){
		double[][] size = new double[][]{ {TableLayout.FILL}, {TableLayout.FILL, TableLayout.PREFERRED}} ;
		setLayout(new TableLayout(size));
		
		pip = new PIPPanel(mainPanel);
		zoom = new ZoomPanel(mainPanel, pip, maxZoom);

		add(pip, "0,0");
		add(zoom, "0,1");
	}
	
	public PIPPanel getPIP() {
		return pip;
	}
	
	public ZoomPanel getZoom(){
		return zoom;
	}
}
