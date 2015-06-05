/**
 * 
 */
package org.processmining.plugins.flex.replayresult.visualization.conformancevis;

import info.clearthought.layout.TableLayout;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.Progress;
import org.processmining.framework.util.collection.AlphanumComparator;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.plugins.flex.replayer.util.FlexBinding;
import org.processmining.plugins.flex.replayer.util.FlexCodec;
import org.processmining.plugins.flex.replayresult.FlexRepResult;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;
import org.processmining.plugins.replayer.replayresult.visualization.ProcessInstanceConformanceView;
import org.processmining.plugins.replayer.util.StepTypes;

import com.fluxicon.slickerbox.factory.SlickerFactory;

/**
 * @author Arya Adriansyah
 * @email a.adriansyah@tue.nl
 * @version Feb 15, 2011
 */
public class FlexLogReplayResultVisPanel extends JPanel {

	private static final long serialVersionUID = -5316808287015620715L;

	// whole stats
	private int numSynchronized = 0;
	private int numModelOnlyInvi = 0;
	private int numModelOnlyReal = 0;
	private int numLogOnly = 0;
	private int numViolations = 0;
	
	private int numReliableSynchronized = 0;
	private int numReliableModelOnlyInvi = 0;
	private int numReliableModelOnlyReal = 0;
	private int numReliableLogOnly = 0;
	private int numReliableViolations = 0;
	
	private double accConformance = 0.000000;
	private double accReliableConformance = 0.000000;
	private int numCaseInvolved = 0;	
	private int numReliableCaseInvolved;


	// filter case
	private enum FilterType {
		ALL, SYNCHONLY, WITHMODEL, WITHLOGONLY, WITHCONF, WITHVIOLATIONS
	};

	public FlexLogReplayResultVisPanel(Flex flex, XLog log, FlexRepResult logReplayResult, FlexCodec codec, Progress progress) {
		setLayout(new BorderLayout());
		if (progress != null) {
			progress.setMaximum(logReplayResult.size());
		}
		add(createBottomPanel(flex, log, logReplayResult, FilterType.ALL, codec, progress));
	}

	private Component createBottomPanel(Flex flex, XLog log, FlexRepResult logReplayResult, FilterType type, FlexCodec codec,
			Progress progress) {
		// add panel below
		TableLayout bgPanelLayout = new TableLayout(new double[][] { { TableLayout.FILL, 300 }, { TableLayout.FILL} });
		JPanel bgPanel = new JPanel(bgPanelLayout);
		bgPanel.setBorder(BorderFactory.createEmptyBorder());
		bgPanel.setBackground(new Color(30, 30, 30));

		// add util
		SlickerFactory factory = SlickerFactory.instance();

		// for each case, create comparison panel
		int cost;
		double fitness;

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(4);
		nf.setMinimumFractionDigits(4);

		JPanel caseView = new JPanel();
		caseView.setBorder(BorderFactory.createEmptyBorder());
		caseView.setBackground(new Color(30, 30, 30));
		TableLayout layout = new TableLayout(
				new double[][] { { TableLayout.FILL, 75, TableLayout.PREFERRED, 175 }, {} });
		caseView.setLayout(layout);

		int numEvtsInATrace;

		int row = 0;

		// calculation of minimum fitness, maximum, average, standard deviation
		double reliableMinFitness = 1.00;
		double reliableMaxFitness = -1.00;
		double mixedMinFitness = 1.00;
		double mixedMaxFitness = -1.00;

		// to calculate standard deviation
		double sumXsquare = 0.00000;
		double sumXsquareReliable = 0.00000;

		Map<FlexBinding, String> labelMapping = new HashMap<FlexBinding, String>();

		for (SyncReplayResult res : logReplayResult) {
			if (progress != null) {
				progress.inc();
			}
			layout.insertRow(row, TableLayout.PREFERRED);
			numEvtsInATrace = 0;

			// content and calculation
			cost = 0;

			// reformat as the nodeInstance is still FlexBinding
			List<Object> strNodeInstance = new LinkedList<Object>();
			for (Object obj : res.getNodeInstance()) {
				if (obj instanceof String) {
					strNodeInstance.add(obj);
				} else {
					if (labelMapping.get(obj) != null) {
						strNodeInstance.add(labelMapping.get(obj));
					} else {
						// create new labeling
						FlexBinding flexBinding = (FlexBinding) obj;
						String resultStr = "{";
						Short encodedInputBinding = flexBinding.getEncodedInputBinding();
						if (encodedInputBinding == FlexCodec.BLANK) {
							resultStr += "Empty} ";
						} else if (encodedInputBinding == FlexCodec.EMPTYSET) {
							resultStr += "} ";
						} else {
							String lim = "";
							for (short input : codec.getIOBindingsFor(encodedInputBinding)) {
								resultStr += codec.decode(input);
								resultStr += lim;
								lim = ",";
							}
							resultStr += "} ";
						}
						resultStr += codec.decode(flexBinding.getEncodedNode());
						resultStr += " {";
						Short encodedOutputBinding = flexBinding.getEncodedOutputBinding();
						if (encodedOutputBinding == FlexCodec.BLANK) {
							resultStr += "Empty} ";
						} else if (encodedOutputBinding == FlexCodec.EMPTYSET) {
							resultStr += "} ";
						} else {
							String lim = "";
							for (short output : codec.getIOBindingsFor(encodedOutputBinding)) {
								resultStr += codec.decode(output);
								resultStr += lim;
								lim = ",";
							}
							resultStr += "} ";
						}
						labelMapping.put(flexBinding, resultStr);
						strNodeInstance.add(resultStr);
					}
				}

			}

			ProcessInstanceConformanceView confView = new ProcessInstanceConformanceView("Replayed", strNodeInstance,
					res.getStepTypes());
			caseView.add(confView, "0," + row + " l, c");

			// create combobox
			SortedSet<String> caseIDSets = new TreeSet<String>(new AlphanumComparator());
			XConceptExtension ce = XConceptExtension.instance();
			for (int index : res.getTraceIndex()){
				caseIDSets.add(ce.extractName(log.get(index)));
			}
			int caseIDSize = caseIDSets.size();

			// create label for combobox
			JLabel lbl1 = factory.createLabel(caseIDSize + " case(s) :");
			lbl1.setForeground(Color.WHITE);
			caseView.add(lbl1, "1," + row + " r, c");
			JComboBox combo = factory.createComboBox(caseIDSets.toArray());
			combo.setPreferredSize(new Dimension(200, combo.getPreferredSize().height));
			combo.setMinimumSize(new Dimension(200, combo.getPreferredSize().height));
			combo.setMaximumSize(new Dimension(200, combo.getPreferredSize().height));
			caseView.add(combo, "2," + row + " l, c");

			// add conformance info
			for (StepTypes stepType : res.getStepTypes()) {
				switch (stepType) {
					case L :
						cost += logReplayResult.getWeightMoveOnLogOnly();
						numLogOnly++;
						if (res.isReliable()){numReliableLogOnly++;};
						numEvtsInATrace++;
						break;
					case MINVI :
						// cost += logReplayResult.getWeightMoveOnModelOnlyInvi(); // invisible move should not be penalized
						if (res.isReliable()){numReliableModelOnlyInvi++;};
						numModelOnlyInvi++;
						break;
					case MREAL :
						cost += logReplayResult.getWeightMoveOnModelOnlyReal();
						if (res.isReliable()){numReliableModelOnlyReal++;};
						numModelOnlyReal++;
						break;
					case LMNOGOOD :
						cost += logReplayResult.getWeightViolation();
						numEvtsInATrace++;
						if (res.isReliable()){numReliableViolations++;};
						numViolations++;
						break;
					default :
						numEvtsInATrace++;
						if (res.isReliable()){numReliableSynchronized++;};
						numSynchronized++;
				}
			}
			numCaseInvolved += caseIDSize;
			if (res.isReliable()){ numReliableCaseInvolved += caseIDSize; };

			int positiveMinimumPenalty = 0;
			if (logReplayResult.getWeightViolation() > 0 && logReplayResult.getWeightMoveOnLogOnly() > 0) {
				positiveMinimumPenalty = logReplayResult.getWeightViolation() < logReplayResult
						.getWeightMoveOnLogOnly() ? logReplayResult.getWeightViolation() : logReplayResult
						.getWeightMoveOnLogOnly();
			} else {
				positiveMinimumPenalty = logReplayResult.getWeightViolation() > 0 ? logReplayResult
						.getWeightViolation() : logReplayResult.getWeightMoveOnLogOnly();
			}
			;
			fitness = 1 - (((double) cost) / (double) (numEvtsInATrace * positiveMinimumPenalty));

			if (Double.compare(mixedMinFitness, fitness) > 0) {
				mixedMinFitness = fitness;
			}
			if (Double.compare(mixedMaxFitness, fitness) < 0) {
				mixedMaxFitness = fitness;
			}
			
			accConformance += (fitness * caseIDSize);
			sumXsquare += (fitness * fitness) * caseIDSize;

			if (res.isReliable()){
				if (Double.compare(reliableMinFitness, fitness) > 0) {
					reliableMinFitness = fitness;
				}
				if (Double.compare(reliableMaxFitness, fitness) < 0) {
					reliableMaxFitness = fitness;
				}
				
				accReliableConformance += (fitness * caseIDSize);
				sumXsquareReliable += (fitness * fitness) * caseIDSize;
			}


			JLabel lbl2 = factory.createLabel("Fitness: " + nf.format(fitness) + (res.isReliable() ? "" : "(unreliable)"));
			lbl2.setForeground(Color.WHITE);
			caseView.add(lbl2, "3," + row + " l, c");

			row++;
		}

		JScrollPane scp = new JScrollPane(caseView, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scp.setBackground(new Color(30, 30, 30));
		scp.setPreferredSize(new Dimension(1000, 160));
		scp.setMaximumSize(new Dimension(1000, 320));
		scp.setBorder(BorderFactory.createEmptyBorder());
		bgPanel.add(scp, "0,0");

		// create info
		Map<String, String> genericInfoTable = new HashMap<String, String>();
		genericInfoTable.put("Cost of a skipped activity", String.valueOf(logReplayResult.getWeightMoveOnModelOnlyReal()));
		genericInfoTable.put("Cost of an unobservable activity",
				String.valueOf(logReplayResult.getWeightMoveOnModelOnlyInvi()));
		genericInfoTable.put("Cost of an inserted activity", String.valueOf(logReplayResult.getWeightMoveOnLogOnly()));
		
		// mixed replay result
		Map<String, String> infoTable = new HashMap<String, String>();
		infoTable.put("#Cases replayed", String.valueOf(numCaseInvolved));
		infoTable.put("#Synchronous activities (log+model)", String.valueOf(numSynchronized));
		infoTable.put("#Skipped activities", String.valueOf(numModelOnlyReal));
		infoTable.put("#Unobservable activities", String.valueOf(numModelOnlyInvi));
		infoTable.put("#Inserted activities", String.valueOf(numLogOnly));
		infoTable.put("#Violating synchronous activities", String.valueOf(numViolations));

		infoTable.put("Cost-based fitness/case", nf.format(accConformance / numCaseInvolved));
		infoTable.put("Minimum fitness", nf.format(mixedMinFitness));
		infoTable.put("Maximum fitness", nf.format(mixedMaxFitness));
		double dev = Math.sqrt(((numCaseInvolved * sumXsquare) - (accConformance * accConformance))
				/ (numCaseInvolved * (numCaseInvolved - 1)));
		infoTable.put("Std. deviation fitness value", Double.compare(dev, Double.NaN) == 0 ? "<NaN>" : nf.format(dev));

		// only reliable replay result
		Map<String, String> reliableInfoTable = new HashMap<String, String>();
		reliableInfoTable.put("#Cases replayed", String.valueOf(numReliableCaseInvolved));
		reliableInfoTable.put("#Synchronous activities (log+model)", String.valueOf(numReliableSynchronized));
		reliableInfoTable.put("#Skipped activities", String.valueOf(numReliableModelOnlyReal));
		reliableInfoTable.put("#Unobservable activities", String.valueOf(numReliableModelOnlyInvi));
		reliableInfoTable.put("#Inserted activities", String.valueOf(numReliableLogOnly));	
		reliableInfoTable.put("Cost-based fitness/case", numReliableCaseInvolved == 0 ? "<NaN>" : nf.format(accReliableConformance / numReliableCaseInvolved));
		reliableInfoTable.put("Minimum fitness", numReliableCaseInvolved == 0 ? "<NaN>" :nf.format(reliableMinFitness));
		reliableInfoTable.put("Maximum fitness", numReliableCaseInvolved == 0 ? "<NaN>" :nf.format(reliableMaxFitness));
		double reliableDev = Math.sqrt(((numReliableCaseInvolved * sumXsquare) - (accReliableConformance * accReliableConformance))
				/ (numReliableCaseInvolved * (numReliableCaseInvolved - 1)));
		 
		reliableInfoTable.put("Std. deviation fitness value", Double.compare(reliableDev, Double.NaN) == 0 ? "<NaN>" : nf.format(reliableDev));

		// on the case where violation might occur
		if (logReplayResult.getWeightViolation() > 0) {
			reliableInfoTable.put("#Violating synchronous activities", String.valueOf(numReliableViolations));
			genericInfoTable.put("Cost of a violating sync. activity", String.valueOf(logReplayResult.getWeightViolation()));
		}

		JComponent infoPanel = createGUIInfo(genericInfoTable, infoTable, reliableInfoTable);
		infoPanel.setBackground(new Color(30, 30, 30));
		infoPanel.setBorder(BorderFactory.createEmptyBorder());
		bgPanel.add(infoPanel, "1,0,c,t");
		return bgPanel;
	}

	private JComponent createGUIInfo(Map<String, String> genericInfo, Map<String, String> info, Map<String, String> reliableInfo) {
		int rowNumber = 0;
		JPanel guiInfo = new JPanel();
		guiInfo.setLayout(new TableLayout(new double[][] { { TableLayout.PREFERRED }, { 100, 30, 90, 30, 180, 30, 180 } }));

		// add to GUI info
		guiInfo.add(createLegendPanel(), "0," + rowNumber++ + ",c,c");
		SlickerFactory factory = SlickerFactory.instance();
		
		JLabel lblRepParam = factory.createLabel("REPLAY PARAMETER");
		lblRepParam.setForeground(Color.WHITE);
		guiInfo.add(lblRepParam, "0," + rowNumber++ + ",c,b");
		
		guiInfo.add(createVisTablePane(genericInfo), "0," + rowNumber++ + ",c,t");

		JLabel lblReliable = factory.createLabel("ONLY CASES WITH RELIABLE REPLAY RESULTS");
		lblReliable.setForeground(Color.WHITE);
		guiInfo.add(lblReliable, "0," + rowNumber++ + ",c,b");
		
		guiInfo.add(createVisTablePane(reliableInfo), "0," + rowNumber++ + ",c,t");
		
		JLabel lblOverall = factory.createLabel("ALL CASES (INCLUDING UNRELIABLE ONES)");
		lblOverall.setForeground(Color.WHITE);
		guiInfo.add(lblOverall, "0," + rowNumber++ + ",c,b");
		
		guiInfo.add(createVisTablePane(info), "0," + rowNumber++ + ",c,t");
		
		return guiInfo;
	}

	private Component createVisTablePane(Map<String, String> info) {
		//form vector of array of string
		Vector<Object> columnNames = new Vector<Object>();
		columnNames.add("Property");
		columnNames.add("Value");

		Vector<Vector<Object>> data = new Vector<Vector<Object>>();
		for (String key : info.keySet()) {
			Vector<Object> datum = new Vector<Object>();
			datum.add(key);
			datum.add(info.get(key));
			data.add(datum);
		}

		// add the list, set it to non editable
		JTable table = new JTable(data, columnNames) {
			private static final long serialVersionUID = -7116637591741809191L;

			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		table.setAutoCreateRowSorter(true);
		JTable visualizedTable = autoResizeColWidth(table);
		visualizedTable.setPreferredScrollableViewportSize(visualizedTable.getPreferredSize());

		RowSorter<TableModel> sorter = new TableRowSorter<TableModel>(visualizedTable.getModel());
		table.setRowSorter(sorter);

		JScrollPane visTablePane = new JScrollPane(visualizedTable);

		return visTablePane;
	}

	private Component createLegendPanel() {
		SlickerFactory factory = SlickerFactory.instance();

		JPanel legendPanel = new JPanel();
		legendPanel.setBorder(BorderFactory.createEmptyBorder());
		legendPanel.setBackground(new Color(30, 30, 30));
		TableLayout layout = new TableLayout(new double[][] { { 0.10, TableLayout.FILL }, {} });
		legendPanel.setLayout(layout);

		layout.insertRow(0, 0.2);

		int row = 1;

		layout.insertRow(row, TableLayout.PREFERRED);
		JLabel legend = factory.createLabel("LEGEND");
		legend.setForeground(Color.WHITE);
		legendPanel.add(legend, "0,1,1,1,c, c");
		row++;

		layout.insertRow(row, 0.2);

		layout.insertRow(row, TableLayout.PREFERRED);
		JPanel greenPanel = new JPanel();
		greenPanel.setBackground(Color.GREEN);
		legendPanel.add(greenPanel, "0," + row + ",r, c");
		JLabel syncLbl = factory.createLabel("-Synchronous activities (log+model)");
		syncLbl.setForeground(Color.WHITE);
		legendPanel.add(syncLbl, "1," + row++ + ",l, c");

		layout.insertRow(row, TableLayout.PREFERRED);
		JPanel greyPanel = new JPanel();
		greyPanel.setBackground(new Color(100, 100, 100));
		legendPanel.add(greyPanel, "0," + row + ",r, c");
		JLabel moveInviLbl = factory.createLabel("-Unobservable activities");
		moveInviLbl.setForeground(Color.WHITE);
		legendPanel.add(moveInviLbl, "1," + row++ + ",l, c");

		layout.insertRow(row, TableLayout.PREFERRED);
		JPanel purplePanel = new JPanel();
		purplePanel.setBackground(new Color(205, 106, 205));
		legendPanel.add(purplePanel, "0," + row + ",r, c");
		JLabel moveRealLbl = factory.createLabel("-Skipped activities");
		moveRealLbl.setForeground(Color.WHITE);
		legendPanel.add(moveRealLbl, "1," + row++ + ",l, c");

		layout.insertRow(row, TableLayout.PREFERRED);
		JPanel yellowPanel = new JPanel();
		yellowPanel.setBackground(new Color(255, 255, 0));
		legendPanel.add(yellowPanel, "0," + row + ",r, c");
		JLabel moveLogLbl = factory.createLabel("-Inserted activities");
		moveLogLbl.setForeground(Color.WHITE);
		legendPanel.add(moveLogLbl, "1," + row++ + ",l, c");

		layout.insertRow(row, TableLayout.PREFERRED);
		JPanel redPanel = new JPanel();
		redPanel.setBackground(new Color(255, 0, 0));
		legendPanel.add(redPanel, "0," + row + ",r, c");
		JLabel moveViolLbl = factory.createLabel("-Violating synchronous activities");
		moveViolLbl.setForeground(Color.WHITE);
		legendPanel.add(moveViolLbl, "1," + row++ + ",l, c");

		layout.insertRow(row, 0.2);

		return legendPanel;
	}

	private JTable autoResizeColWidth(JTable table) {
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setModel(table.getModel());

		int margin = 5;

		for (int i = 0; i < table.getColumnCount(); i++) {
			int vColIndex = i;
			DefaultTableColumnModel colModel = (DefaultTableColumnModel) table.getColumnModel();
			TableColumn col = colModel.getColumn(vColIndex);
			int width = 0;

			// Get width of content header
			TableCellRenderer renderer = col.getHeaderRenderer();

			if (renderer == null) {
				renderer = table.getTableHeader().getDefaultRenderer();
			}

			Component comp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, 0);

			width = comp.getPreferredSize().width;

			// Get maximum width of column data
			for (int r = 0; r < table.getRowCount(); r++) {
				renderer = table.getCellRenderer(r, vColIndex);
				comp = renderer.getTableCellRendererComponent(table, table.getValueAt(r, vColIndex), false, false, r,
						vColIndex);
				width = Math.max(width, comp.getPreferredSize().width);
			}

			// Add margin
			width += 2 * margin;

			// Set the width
			col.setPreferredWidth(width);
		}

		((DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer())
				.setHorizontalAlignment(SwingConstants.LEFT);

		// table.setAutoCreateRowSorter(true);
		table.getTableHeader().setReorderingAllowed(false);

		return table;
	}
}
