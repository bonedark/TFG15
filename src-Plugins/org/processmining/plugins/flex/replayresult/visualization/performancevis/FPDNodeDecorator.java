/**
 * 
 */
package org.processmining.plugins.flex.replayresult.visualization.performancevis;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;

import javax.swing.JLabel;

import org.processmining.models.flexiblemodel.IExposeNodeDecoration;

/**
 * @author aadrians
 * 
 */
public class FPDNodeDecorator implements IExposeNodeDecoration {
	/**
	 * GUI RELATED VARIABLES
	 */
	public static final int PADDINGFROMBOXTOTEXT = 3;
	public static final int VERTICALPADDINGFROMBOXTOTEXT = 7;
	public static final int TEXTHEIGHT = 5;
	public static final int BRANCHINGBOXWIDTH = 15;
	public static final int MINHEIGHT = 60;
	public static final int MAXLABELCHAR = 21;

	// utility
	private final NumberFormat nf;

	// visualization data
	FPDNodeDecorData decor = null;

	// for this node, no information can be obtained by replaying 
	public FPDNodeDecorator() {
		nf = null;
	}

	public FPDNodeDecorator(FPDNodeDecorData decor) {
		// init utility
		nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);

		// init data
		this.decor = decor;
	}

	public void decorate(Graphics2D g2d, double x, double y, double width, double height) {
		// fill in the semantic box
		GeneralPath path = new GeneralPath();

		// create semantic boxes
		double heightOfSplitBoxes = (height - (4 * VERTICALPADDINGFROMBOXTOTEXT)) / 3;

		// general stroke
		BasicStroke stroke = new BasicStroke((float) 1.0);
		g2d.setStroke(stroke);

		/**
		 * JOIN boxes
		 */
		// give color
		g2d.setColor(new Color(50, 50, 50));
		double xForJoinBoxes = x + PADDINGFROMBOXTOTEXT;
		double yForJoinBoxes = y + (2 * VERTICALPADDINGFROMBOXTOTEXT);
		java.awt.Shape[] joinBoxes = getBranchingIndicatorBoxes(xForJoinBoxes, yForJoinBoxes, heightOfSplitBoxes);
		path.append(joinBoxes[0], false);
		g2d.fill(path);
		path.reset();
		path.append(joinBoxes[1], false);
		g2d.fill(path);
		path.reset();
		path.append(joinBoxes[2], false);
		g2d.fill(path);
		g2d.drawString("AND", Math.round(xForJoinBoxes + BRANCHINGBOXWIDTH - 13), Math.round(y
				+ VERTICALPADDINGFROMBOXTOTEXT + TEXTHEIGHT));
		g2d.drawString("XOR", Math.round(xForJoinBoxes + BRANCHINGBOXWIDTH - 13), Math.round(y
				+ (2 * VERTICALPADDINGFROMBOXTOTEXT) + (2 * TEXTHEIGHT) + (3 * heightOfSplitBoxes)));

		/**
		 * SPLIT boxes
		 */
		double xForSplitBoxes = x + width - PADDINGFROMBOXTOTEXT - BRANCHINGBOXWIDTH;
		java.awt.Shape[] splitBoxes = getBranchingIndicatorBoxes(xForSplitBoxes, yForJoinBoxes, heightOfSplitBoxes);

		path.reset();
		path.append(splitBoxes[0], false);
		g2d.fill(path);
		path.reset();
		path.append(splitBoxes[1], false);
		g2d.fill(path);
		path.reset();
		path.append(splitBoxes[2], false);
		g2d.fill(path);
		g2d.drawString("AND", Math.round(xForSplitBoxes), Math.round(y + VERTICALPADDINGFROMBOXTOTEXT + TEXTHEIGHT));
		g2d.drawString("XOR", Math.round(xForSplitBoxes), Math.round(y + (2 * VERTICALPADDINGFROMBOXTOTEXT)
				+ (2 * TEXTHEIGHT) + (3 * heightOfSplitBoxes)));

		/**
		 * Draw JOIN semantic indicator
		 */
		// give color
		g2d.setColor(decor.getWaitColor());
		java.awt.Shape[] joinBoxContents = getBranchingBoxContents(xForJoinBoxes, yForJoinBoxes, heightOfSplitBoxes,
				getANDJOINrf(), getORJOINrf(), getXORJOINrf(), true);
		path.reset();
		path.append(joinBoxContents[0], false);
		g2d.fill(path);
		path.reset();
		path.append(joinBoxContents[1], false);
		g2d.fill(path);
		path.reset();
		path.append(joinBoxContents[2], false);
		g2d.fill(path);

		/**
		 * Draw SPLIT semantic indicator
		 */
		// give color
		g2d.setColor(decor.getSyncColor());
		java.awt.Shape[] splitBoxContents = getBranchingBoxContents(xForSplitBoxes, yForJoinBoxes, heightOfSplitBoxes,
				getANDSPLITrf(), getORSPLITrf(), getXORSPLITrf(), false);
		path.reset();
		path.append(splitBoxContents[0], false);
		g2d.fill(path);
		path.reset();
		path.append(splitBoxContents[1], false);
		g2d.fill(path);
		path.reset();
		path.append(splitBoxContents[2], false);
		g2d.fill(path);

		/**
		 * Draw throughput
		 */
		if (decor.getFrequency() > 0) {
			g2d.setColor(decor.getWorkingColor());
			path.reset();

			double throughputBoxHeight = (decor.getCaseInvolvementRatio() * height);
			java.awt.Shape throughputFilling = new Rectangle2D.Double(x + BRANCHINGBOXWIDTH
					+ (2 * PADDINGFROMBOXTOTEXT), y + height - throughputBoxHeight + 1, width - (2 * BRANCHINGBOXWIDTH)
					- (4 * PADDINGFROMBOXTOTEXT), throughputBoxHeight - 1 - 1);
			path.append(throughputFilling, false);
			g2d.fill(path);
		}
		path.reset();
		g2d.setColor(Color.BLACK);

		/**
		 * Label frequency and also throughput time
		 */
		FontMetrics fm = g2d.getFontMetrics();

		//		String textFrequency = getAggregatedEventClassFreq() + "/" + (getAggregatedEventClassFreq() + getUnfinishedActivityFrequency()); 
		// number of activity instances which is encountered in this node (note that this is different than activation frequency!)
		g2d.drawString(String.valueOf(getAggregatedEventClassFreq()), Math.round(x + BRANCHINGBOXWIDTH
				+ (2 * PADDINGFROMBOXTOTEXT)) + 5, Math.round(y + height) - 5);

		// throughput time

		if ((Double.compare(getAvgThroughput(), -1) == 0) || (Double.compare(getAvgThroughput(), Double.NaN) == 0)) {
			g2d.drawString("N/A", Math.round(x + width - BRANCHINGBOXWIDTH - (2 * PADDINGFROMBOXTOTEXT)
					- fm.stringWidth("N/A")), Math.round(y + height) - 5);
		} else {
			String text = getSuitableTimeStr(getAvgThroughput());
			g2d.drawString(text, Math.round(x + width - BRANCHINGBOXWIDTH - (2 * PADDINGFROMBOXTOTEXT)
					- fm.stringWidth(text)), Math.round(y + height) - 5);
		}

		// BVD: Added this code in order to correctly render HTML. 
		//
		// The label should be in a box from 
		// - Math.round(x + BRANCHINGBOXWIDTH + (2 * PADDINGFROMBOXTOTEXT) + 10), y to
		// - Math.round(x + width - (BRANCHINGBOXWIDTH + (2 * PADDINGFROMBOXTOTEXT) + 10), y+height)

		final int labelX = (int) Math.round(x + BRANCHINGBOXWIDTH + (2 * PADDINGFROMBOXTOTEXT) + 10);
		final int labelY = (int) Math.round(y);
		final int labelW = (int) Math.round(width - 2 * (BRANCHINGBOXWIDTH + (2 * PADDINGFROMBOXTOTEXT) + 10));
		final int labelH = (int) Math.round(height);

		JLabel label = new JLabel(getLabel());
		label.setPreferredSize(new Dimension(labelW, labelH));
		label.setSize(new Dimension(labelW, labelH));

		label.setFont(new Font(label.getFont().getFamily(), label.getFont().getStyle(), 8));
		label.validate();
		label.paint(g2d.create(labelX, labelY, labelW, labelH));

		/**
		 * Draw frequency it is cancelled
		 */
		g2d.setColor(Color.ORANGE);
		g2d.drawString(String.valueOf(getCancelledFrequency()), Math.round(x + BRANCHINGBOXWIDTH
				+ (2 * PADDINGFROMBOXTOTEXT)) + 5, Math.round(y + height) - 15);

		/**
		 * Draw cancelation region
		 */
		if (decor.isCancelingOther()) {
			g2d.setColor(Color.WHITE);
			g2d.fillOval((int) Math.round(x + width - BRANCHINGBOXWIDTH - (2 * PADDINGFROMBOXTOTEXT) - 11), (int) Math
					.round(y + 4), 7, 7);

			g2d.setColor(Color.RED);
			g2d.fillOval((int) Math.round(x + width - BRANCHINGBOXWIDTH - (2 * PADDINGFROMBOXTOTEXT) - 10), (int) Math
					.round(y + 5), 5, 5);

		}

		/**
		 * Draw frequency the node is started
		 */
		g2d.setColor(Color.BLACK);
		String text = String.valueOf(getAggregatedEventClassFreq() + getUnfinishedActivityFrequency());
		g2d.drawString(text, Math.round(x + width - BRANCHINGBOXWIDTH - (2 * PADDINGFROMBOXTOTEXT)
				- fm.stringWidth(text)) - 5, Math.round(y + 20));
	}

	private int getUnfinishedActivityFrequency() {
		return decor.getUnfinishedActivityFrequency();
	}

	private int getCancelledFrequency() {
		return decor.getCancelledFrequency();
	}

	private String getLabel() {
		return decor.getLabel();
	}

	private int getAggregatedEventClassFreq() {
		return decor.getFrequency();
	}

	private double getAvgThroughput() {
		return decor.getAvgWorkingTime();
	}

	private Shape[] getBranchingBoxContents(double x, double y, double heightOfBoxes, int andFreq, int orFreq,
			int xorFreq, boolean isRightAligned) {
		// result
		java.awt.Shape andShape;
		java.awt.Shape orShape;
		java.awt.Shape xorShape;

		// calculate relative length
		int totalFreq = andFreq + orFreq + xorFreq;
		double andWidth = ((double) andFreq / (double) totalFreq) * BRANCHINGBOXWIDTH;
		double orWidth = ((double) orFreq / (double) totalFreq) * BRANCHINGBOXWIDTH;
		double xorWidth = ((double) xorFreq / (double) totalFreq) * BRANCHINGBOXWIDTH;

		if (isRightAligned) {
			andShape = new Rectangle2D.Double(x + BRANCHINGBOXWIDTH - andWidth, y, andWidth, heightOfBoxes);
			orShape = new Rectangle2D.Double(x + BRANCHINGBOXWIDTH - orWidth, y + heightOfBoxes, orWidth, heightOfBoxes);
			xorShape = new Rectangle2D.Double(x + BRANCHINGBOXWIDTH - xorWidth, y + (2 * heightOfBoxes), xorWidth,
					heightOfBoxes);
		} else {
			andShape = new Rectangle2D.Double(x, y, andWidth, heightOfBoxes);
			orShape = new Rectangle2D.Double(x, y + heightOfBoxes, orWidth, heightOfBoxes);
			xorShape = new Rectangle2D.Double(x, y + (2 * heightOfBoxes), xorWidth, heightOfBoxes);
		}

		// result
		return new Shape[] { andShape, orShape, xorShape };
	}

	private int getXORSPLITrf() {
		return decor.getXorSplitFreq();
	}

	private int getORSPLITrf() {
		return decor.getOrSplitFreq();
	}

	private int getANDSPLITrf() {
		return decor.getAndSplitFreq();
	}

	private int getXORJOINrf() {
		return decor.getXorJoinFreq();
	}

	private int getORJOINrf() {
		return decor.getOrJoinFreq();
	}

	private int getANDJOINrf() {
		return decor.getAndJoinFreq();
	}

	public java.awt.Shape[] getBranchingIndicatorBoxes(double x, double y, double heightOfSplitBoxes) {
		java.awt.Shape[] result = new java.awt.Shape[3];

		java.awt.Shape AND = new Rectangle2D.Double(x, y, BRANCHINGBOXWIDTH, heightOfSplitBoxes);
		java.awt.Shape OR = new Rectangle2D.Double(x, y + heightOfSplitBoxes, BRANCHINGBOXWIDTH, heightOfSplitBoxes);
		java.awt.Shape XOR = new Rectangle2D.Double(x, y + (2 * heightOfSplitBoxes), BRANCHINGBOXWIDTH,
				heightOfSplitBoxes);

		result[0] = AND;
		result[1] = OR;
		result[2] = XOR;

		return result;
	}

	private String getSuitableTimeStr(double avgMoveTime) {
		NumberFormat nf = NumberFormat.getInstance();
		if (Double.compare(avgMoveTime, 1000 * 60 * 60 * 24) > 0) {
			// days...
			return (nf.format(avgMoveTime / (1000 * 60 * 60 * 24)) + " days");
		} else if (Double.compare(avgMoveTime, 1000 * 60 * 60) > 0) {
			// hours
			return (nf.format(avgMoveTime / (1000 * 60 * 60)) + " hours");
		} else if (Double.compare(avgMoveTime, 1000 * 60) > 0) {
			// minutes
			return (nf.format(avgMoveTime / (1000 * 60)) + " min");
		} else if (Double.compare(avgMoveTime, 1000) > 0) {
			// seconds
			return (nf.format(avgMoveTime / (1000)) + " sec");
		}
		return (nf.format(avgMoveTime) + " ms");
	}
}
