/**
 * 
 */
package org.processmining.plugins.petrinet.replayresult.visualization.projection;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;

import javax.swing.JLabel;

import org.processmining.models.pnetprojection.ITransitionPDecorator;

/**
 * @author aadrians Nov 4, 2011
 * 
 */
public class TransDecorator implements ITransitionPDecorator {
	private int moveOnModelFreq = 0;
	private int moveSyncFreq = 0;
	private String label;
	private Color moveLogModelColor;
	private Color moveOnModelColor;

	@SuppressWarnings("unused")
	private TransDecorator(){};
	
	public TransDecorator(int moveOnModelFreq, int moveSyncFreq, String label, Color moveLogModelColor, Color moveOnModelColor) {
		this.moveOnModelFreq = moveOnModelFreq;
		this.moveSyncFreq = moveSyncFreq;
		this.label = label;
		this.moveLogModelColor= moveLogModelColor;
		this.moveOnModelColor= moveOnModelColor; 
	}

	/**
	 * @return the moveOnModelFreq
	 */
	public int getMoveOnModelFreq() {
		return moveOnModelFreq;
	}

	/**
	 * @param moveOnModelFreq
	 *            the moveOnModelFreq to set
	 */
	public void setMoveOnModelFreq(int moveOnModelFreq) {
		this.moveOnModelFreq = moveOnModelFreq;
	}

	/**
	 * @return the moveSyncFreq
	 */
	public int getMoveSyncFreq() {
		return moveSyncFreq;
	}

	/**
	 * @param moveSyncFreq
	 *            the moveSyncFreq to set
	 */
	public void setMoveSyncFreq(int moveSyncFreq) {
		this.moveSyncFreq = moveSyncFreq;
	}

	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @param label
	 *            the label to set
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	public void decorate(Graphics2D g2d, double x, double y, double width, double height) {
		// fill in the semantic box
		GeneralPath path = new GeneralPath();

		// general stroke
		BasicStroke stroke = new BasicStroke((float) 1.0);
		g2d.setStroke(stroke);

		double syncWidth = 0;
		if (moveSyncFreq > 0) {
			// fill in sync move (log+model)
			syncWidth = (moveSyncFreq * width) / (moveSyncFreq + moveOnModelFreq);
			path.append(new Rectangle2D.Double(x, y, syncWidth, height), false);
			g2d.setColor(moveLogModelColor);
			g2d.fill(path);
			path.reset();
		}

		if (moveOnModelFreq > 0) {
			double moveModelWidth = (moveOnModelFreq * width) / (moveSyncFreq + moveOnModelFreq);
			path.append(new Rectangle2D.Double(x + syncWidth, y, moveModelWidth, height), false);
			g2d.setColor(moveOnModelColor);
			g2d.fill(path);
			path.reset();
		}

		// draw transition label
		final int labelX = (int) Math.round(x + 10);
		final int labelY = (int) Math.round(y);
		final int labelW = (int) Math.round(width - 20);
		final int labelH = (int) Math.round(height);

		JLabel nodeName = new JLabel(label);
		nodeName.setPreferredSize(new Dimension(labelW, labelH));
		nodeName.setSize(new Dimension(labelW, labelH));

		nodeName.setFont(new Font(nodeName.getFont().getFamily(), nodeName.getFont().getStyle(), 8));
		nodeName.validate();
		nodeName.paint(g2d.create(labelX, labelY, labelW, labelH));
	}

}
