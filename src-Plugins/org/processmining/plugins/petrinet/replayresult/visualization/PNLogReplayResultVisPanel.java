/**
 * 
 */
package org.processmining.plugins.petrinet.replayresult.visualization;

import info.clearthought.layout.TableLayout;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.Progress;
import org.processmining.framework.util.collection.AlphanumComparator;
import org.processmining.framework.util.ui.widgets.ProMSplitPane;
import org.processmining.framework.util.ui.widgets.ProMTextField;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;
import org.processmining.plugins.replayer.replayresult.visualization.ProcessInstanceConformanceView;
import org.processmining.plugins.replayer.util.StepTypes;

import com.fluxicon.slickerbox.components.RoundedPanel;
import com.fluxicon.slickerbox.factory.SlickerFactory;

/**
 * 
 * @author aadrians
 * @mail a.adriansyah@tue.nl
 * @since Dec 30, 2010
 */
public class PNLogReplayResultVisPanel extends JPanel {
	private static final long serialVersionUID = -5897513989508944234L;

	/**
	 * Pointers to property variable
	 */
	private static int RELIABLEMIN = 0;
	private static int RELIABLEMAX = 1;
	private static int MIN = 2;
	private static int MAX = 3;

	// standard deviation is calculated based on http://mathcentral.uregina.ca/QQ/database/QQ.09.02/carlos1.html
	private static int SVAL = 4;
	private static int MVAL = 6;
	private static int SVALRELIABLE = 5;
	private static int MVALRELIABLE = 7;
	private static int PERFECTCASERELIABLECOUNTER = 8;

	// total calculated values
	private Map<String, Double[]> calculations = new HashMap<String, Double[]>();
	private final DefaultTableModel reliableCasesTModel = new DefaultTableModel() {
		private static final long serialVersionUID = -4303950078200984098L;

		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}
	};

	// whole stats
	int numSynchronized = 0;
	int numModelOnlyInvi = 0;
	int numModelOnlyReal = 0;
	int numLogOnly = 0;
	int numViolations = 0;

	int numReliableSynchronized = 0;
	int numReliableModelOnlyInvi = 0;
	int numReliableModelOnlyReal = 0;
	int numReliableLogOnly = 0;
	int numReliableViolations = 0;

	int numCaseInvolved = 0;
	int numReliableCaseInvolved = 0;

	public PNLogReplayResultVisPanel(PetrinetGraph net, XLog log, PNRepResult logReplayResult, Progress progress) {
		TableLayout mainLayout = new TableLayout(new double[][] { { TableLayout.FILL }, { TableLayout.FILL } });
		setLayout(mainLayout);
		setBorder(BorderFactory.createEmptyBorder());
		if (progress != null) {
			progress.setMaximum(logReplayResult.size());
		}
		add(createBottomPanel(net, log, logReplayResult, progress), "0,0");
	}

	private Component createBottomPanel(PetrinetGraph net, final XLog log, final PNRepResult logReplayResult,
			Progress progress) {
		Color bgColor = new Color(30, 30, 30);

		// clear maps
		calculations.clear();

		// add util
		SlickerFactory factory = SlickerFactory.instance();

		// for each case, create comparison panel

		final NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
		nf.setMinimumFractionDigits(2);

		final NumberFormat nfi = NumberFormat.getInstance();
		nfi.setMaximumFractionDigits(0);
		nfi.setMinimumFractionDigits(0);

		final ProMPropertiesPanelWithComp logAlignmentPanel = new ProMPropertiesPanelWithComp("Log-model Alignments");

		// searching for a case by id
		JButton searchButton = factory.createButton("Goto Case ID");
		final ProMTextField searchTerm = new ProMTextField("<type case id here>");
		logAlignmentPanel.addProperty(searchButton, searchTerm);

		int row = 0;

		for (SyncReplayResult res : logReplayResult) {
			if (progress != null) {
				progress.inc();
			}

			// reformat node instance list
			List<Object> result = new LinkedList<Object>();
			for (Object obj : res.getNodeInstance()) {
				if (obj instanceof Transition) {
					result.add(((Transition) obj).getLabel());
				} else if (obj instanceof String) {
					result.add(obj);
				} else {
					result.add(obj.toString());
				}
			}

			// create combobox
			SortedSet<String> caseIDSets = new TreeSet<String>(new AlphanumComparator());
			XConceptExtension ce = XConceptExtension.instance();
			for (int index : res.getTraceIndex()) {
				String name = ce.extractName(log.get(index));
				if (name == null) {
					name = String.valueOf(index);
				}
				caseIDSets.add(name);
			}
			int caseIDSize = caseIDSets.size();

			// create label for combobox
			JLabel lbl1 = factory.createLabel(caseIDSize + " case(s) :");
			lbl1.setForeground(Color.WHITE);

			JComboBox combo = factory.createComboBox(caseIDSets.toArray());
			combo.setPreferredSize(new Dimension(200, combo.getPreferredSize().height));
			combo.setMinimumSize(new Dimension(200, combo.getPreferredSize().height));
			combo.setMaximumSize(new Dimension(200, combo.getPreferredSize().height));

			// add conformance info
			for (StepTypes stepType : res.getStepTypes()) {
				switch (stepType) {
					case L :
						numLogOnly += caseIDSize;
						if (res.isReliable()) {
							numReliableLogOnly += caseIDSize;
						}
						;
						break;
					case MINVI :
						if (res.isReliable()) {
							numReliableModelOnlyInvi += caseIDSize;
						}
						;
						numModelOnlyInvi += caseIDSize;
						break;
					case MREAL :
						if (res.isReliable()) {
							numReliableModelOnlyReal += caseIDSize;
						}
						;
						numModelOnlyReal += caseIDSize;
						break;
					case LMNOGOOD :
						if (res.isReliable()) {
							numReliableViolations += caseIDSize;
						}
						;
						numViolations += caseIDSize;
						break;
					default :
						if (res.isReliable()) {
							numReliableSynchronized += caseIDSize;
						}
						;
						numSynchronized += caseIDSize;
				}
			}

			// to be shown in right side of case
			Map<String, Double> mapInfo = res.getInfo();
			Set<String> keySetMapInfo = mapInfo.keySet();

			// create table for map info
			Object[][] infoSingleTrace = new Object[keySetMapInfo.size() + 2][2];
			int propCounter = 0;
			infoSingleTrace[propCounter++] = new Object[] { "Num. Cases", caseIDSize };
			infoSingleTrace[propCounter++] = new Object[] { "Is Alignment Reliable?", res.isReliable() ? "Yes" : "No" };
			for (String property : keySetMapInfo) {
				Double value = mapInfo.get(property);
				if (Math.floor(value) == Math.ceil(value)) {
					infoSingleTrace[propCounter++] = new Object[] { property, nfi.format(value) };
				} else {
					infoSingleTrace[propCounter++] = new Object[] { property, nf.format(value) };
				}

				// use it to calculate property
				Double[] oldValues = calculations.get(property);
				if (oldValues == null) {
					oldValues = new Double[] { Double.MAX_VALUE, Double.MIN_VALUE, Double.MAX_VALUE, Double.MIN_VALUE,
							0.00000, 0.00000, 0.00000, 0.00000, 0.00000 };
					calculations.put(property, oldValues);
				}

				if (Double.compare(oldValues[MIN], value) > 0) {
					oldValues[MIN] = value;
				}
				if (Double.compare(oldValues[MAX], value) < 0) {
					oldValues[MAX] = value;
				}

				int counterCaseIDSize = 0;
				if (numCaseInvolved == 0) {
					oldValues[MVAL] += value;
					oldValues[SVAL] += 0;
					counterCaseIDSize++;
				}
				for (int i = counterCaseIDSize; i < caseIDSize; i++) {
					double oldMVal = oldValues[MVAL];
					oldValues[MVAL] += ((value - oldValues[MVAL]) / (i + numCaseInvolved));
					oldValues[SVAL] += ((value - oldMVal) * (value - oldValues[MVAL]));
				}

				if (res.isReliable()) {
					if (Double.compare(oldValues[RELIABLEMIN], value) > 0) {
						oldValues[RELIABLEMIN] = value;
					}
					if (Double.compare(oldValues[RELIABLEMAX], value) < 0) {
						oldValues[RELIABLEMAX] = value;
					}

					counterCaseIDSize = 0;
					if (numReliableCaseInvolved == 0) {
						oldValues[MVALRELIABLE] += value;
						oldValues[SVALRELIABLE] += 0;
						counterCaseIDSize++;
					}
					for (int i = counterCaseIDSize; i < caseIDSize; i++) {
						double oldMVal = oldValues[MVALRELIABLE];
						oldValues[MVALRELIABLE] += ((value - oldValues[MVALRELIABLE]) / (i + numReliableCaseInvolved));
						oldValues[SVALRELIABLE] += ((value - oldMVal) * (value - oldValues[MVALRELIABLE]));
					}

					if (Double.compare(value, 1.0000000) == 0) {
						oldValues[PERFECTCASERELIABLECOUNTER] += caseIDSize;
					}
				}
			}

			numCaseInvolved += caseIDSize;
			if (res.isReliable()) {
				numReliableCaseInvolved += caseIDSize;
			}
			;

			// ALIGNMENT STATISTICS PANEL
			DefaultTableModel tableModel = new DefaultTableModel(infoSingleTrace, new Object[] { "Property", "Value" }) {
				private static final long serialVersionUID = -4303950078200984098L;

				@Override
				public boolean isCellEditable(int row, int column) {
					return false;
				}
			};

			ProMTableWithoutHeader promTable = new ProMTableWithoutHeader(tableModel);
			promTable.setPreferredWidth(0, 180);
			promTable.setPreferredWidth(1, 100);
			RoundedPanel alignmentStatsPanel = new RoundedPanel(10);
			TableLayout leftPanelLayout = new TableLayout(new double[][] { { TableLayout.PREFERRED, TableLayout.FILL },
					{ 25, TableLayout.FILL } });
			alignmentStatsPanel.setLayout(leftPanelLayout);
			JLabel lblCaseId = new JLabel("Case id(s):");
			lblCaseId.setForeground(Color.white);
			alignmentStatsPanel.add(lblCaseId, "0,0");
			alignmentStatsPanel.add(combo, "1,0");
			alignmentStatsPanel.add(promTable, "0,1,1,1");
			alignmentStatsPanel.setPreferredSize(new Dimension(400, 100));
			alignmentStatsPanel.setMinimumSize(new Dimension(400, 100));
			alignmentStatsPanel.setMaximumSize(new Dimension(400, 100));
			if (res.isReliable()) {
				alignmentStatsPanel.setBackground(new Color(70, 70, 70, 210));
			} else {
				alignmentStatsPanel.setBackground(Color.RED);
			}

			// ALIGNMENT PANEL
			ProcessInstanceConformanceView alignmentPanel = new ProcessInstanceConformanceView("Alignment", result,
					res.getStepTypes());

			logAlignmentPanel.addProperty(alignmentStatsPanel, alignmentPanel);

			row++;
		}

		// set the action for searching
		searchButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!searchTerm.getText().equals("")) {
					int counter = 1;
					int candidate = 0;
					boolean lookingBetterCandidate = true;
					XConceptExtension ci = XConceptExtension.instance();
					for (SyncReplayResult res : logReplayResult) {
						if ((lookingBetterCandidate)
								&& (ci.extractName(log.get(res.getTraceIndex().first())).startsWith(searchTerm
										.getText()))) {
							candidate = counter;
							lookingBetterCandidate = false;
						}

						for (int index : res.getTraceIndex()) {
							String caseID = ci.extractName(log.get(index));
							if (caseID.equals(searchTerm.getText())) {
								logAlignmentPanel.setPosition((counter - 1) / (logReplayResult.size() + 0.333));
								return;
							} else if (lookingBetterCandidate && (caseID.startsWith(searchTerm.getText()))) {
								candidate = counter;
							}
						}
						counter++;
					}
					logAlignmentPanel.setPosition((candidate - 1) / (logReplayResult.size() + 0.333));
				}
				;
			}
		});

		// reliable replay result
		ProMTableWithoutHeader tableReliableResult = createTable(numReliableCaseInvolved, numReliableSynchronized,
				numReliableModelOnlyReal, numReliableModelOnlyInvi, numReliableLogOnly, numReliableViolations,
				logReplayResult.getInfo(), nfi);

		int lineNumberRight = 0;
		JPanel statisticPanel = new JPanel();
		statisticPanel.setBackground(bgColor);
		double[][] rightMainPanelSize = new double[][] {
				{ TableLayout.PREFERRED },
				{ TableLayout.PREFERRED, 30, TableLayout.PREFERRED, 30, TableLayout.PREFERRED, TableLayout.PREFERRED,
						60, TableLayout.PREFERRED } };

		statisticPanel.setLayout(new TableLayout(rightMainPanelSize));

		// add LEGEND
		statisticPanel.add(createLegendPanel(), "0, " + lineNumberRight++ + ", c, t");

		// add STATS FROM RELIABLE ALIGNMENTS
		JLabel lblReliable = factory.createLabel("STATS FROM RELIABLE ALIGNMENTS");
		lblReliable.setForeground(Color.WHITE);
		statisticPanel.add(lblReliable, "0, " + lineNumberRight++ + ", c, b");

		statisticPanel.add(tableReliableResult, "0, " + lineNumberRight++ + ", c, t");

		// add ALIGNMENT STATISTICS
		Set<String> allProp = calculations.keySet();
		final JComboBox comboAllCases = factory.createComboBox(allProp.toArray(new Object[allProp.size()]));
		comboAllCases.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				populateValue(comboAllCases.getSelectedItem().toString(), nf, nfi);
			}
		});
		comboAllCases.setPreferredSize(new Dimension(300, comboAllCases.getPreferredSize().height));
		comboAllCases.setMinimumSize(new Dimension(200, comboAllCases.getPreferredSize().height));

		JLabel lblStats = factory.createLabel("ALIGNMENT STATISTICS");
		lblStats.setForeground(Color.WHITE);
		statisticPanel.add(lblStats, "0, " + lineNumberRight++ + ", c, b");

		statisticPanel.add(comboAllCases, "0, " + lineNumberRight++ + ", c, t");

		// create statistics table
		populateValue(comboAllCases.getSelectedItem().toString(), nf, nfi);
		ProMTableWithoutHeader reliableCasesStatistics = new ProMTableWithoutHeader(this.reliableCasesTModel);
		reliableCasesStatistics.setPreferredSize(new Dimension(300, 100));
		reliableCasesStatistics.setMinimumSize(new Dimension(300, 100));
		reliableCasesStatistics.setPreferredWidth(0, 180);
		reliableCasesStatistics.setPreferredWidth(1, 20);

		statisticPanel.add(reliableCasesStatistics, "0, " + lineNumberRight++ + ", c, t");

		// add STATS INCLUDING UNRELIABLE RESULTS
		JLabel lblOverall = factory.createLabel("STATS INCLUDING UNRELIABLE ALIGNMENTS");
		lblOverall.setForeground(Color.WHITE);
		statisticPanel.add(lblOverall, "0, " + lineNumberRight++ + ", c, b");

		// all replay result
		ProMTableWithoutHeader tableAllResult = createTable(numCaseInvolved, numSynchronized, numModelOnlyReal,
				numModelOnlyInvi, numLogOnly, numViolations, null, nfi);

		statisticPanel.add(tableAllResult, "0, " + lineNumberRight++ + ", c, b");

		// MAIN PANEL
		ProMSplitPane splitPanel = new ProMSplitPane(ProMSplitPane.HORIZONTAL_SPLIT);
		splitPanel.setBorder(BorderFactory.createEmptyBorder());
		splitPanel.setOneTouchExpandable(true);
		splitPanel.setLeftComponent(logAlignmentPanel);
		splitPanel.setRightComponent(statisticPanel);
		splitPanel.setResizeWeight(1.0);
		return splitPanel;
	}

	protected void populateValue(String key, NumberFormat nf, NumberFormat nfi) {
		Object[][] data = new Object[5][2];

		Double[] values = calculations.get(key);
		data[0] = new Object[] { "Average/case",
				numReliableCaseInvolved == 0 ? "<NaN>" : nf.format(values[MVALRELIABLE]) };
		data[1] = new Object[] {
				"Max.",
				numReliableCaseInvolved == 0 ? "<NaN>" : Math.floor(values[RELIABLEMAX]) == Math
						.ceil(values[RELIABLEMAX]) ? nfi.format(values[RELIABLEMAX].intValue()) : nf
						.format(values[RELIABLEMAX]) };
		data[2] = new Object[] {
				"Min.",
				numReliableCaseInvolved == 0 ? "<NaN>" : Math.floor(values[RELIABLEMIN]) == Math
						.ceil(values[RELIABLEMIN]) ? nfi.format(values[RELIABLEMIN].intValue()) : nf
						.format(values[RELIABLEMIN]) };
		data[3] = new Object[] {
				"Std. Deviation",
				numReliableCaseInvolved == 0 ? "0" : nf.format(Math.sqrt(values[SVALRELIABLE]
						/ (numReliableCaseInvolved - 1))) };

		data[4] = new Object[] { "#Cases with value 1.00", nfi.format(values[PERFECTCASERELIABLECOUNTER].intValue()) };

		reliableCasesTModel.setDataVector(data, new Object[] { "Property", "Value" });

	}

	private ProMTableWithoutHeader createTable(int numCaseInvolved, int numSynchronized, int numModelOnlyReal,
			int numModelOnlyInvi, int numLogOnly, int numViolations, Map<String, Object> map, NumberFormat nfi) {
		int idx = 0;
		Object[][] infoTable;

		if (map != null) {
			infoTable = new Object[6 + map.size()][2];
		} else {
			infoTable = new Object[6][2];
		}
		infoTable[idx++] = new Object[] { "#Cases replayed", nfi.format(numCaseInvolved) };
		infoTable[idx++] = new Object[] { "#Synchronous ev.class (log+model)", nfi.format(numSynchronized) };
		infoTable[idx++] = new Object[] { "#Skipped ev.class", nfi.format(numModelOnlyReal) };
		infoTable[idx++] = new Object[] { "#Unobservable ev.class", nfi.format(numModelOnlyInvi) };
		infoTable[idx++] = new Object[] { "#Inserted ev.class", nfi.format(numLogOnly) };
		infoTable[idx++] = new Object[] { "#Violating synchronous ev.class", nfi.format(numViolations) };

		if (map != null) {
			for (String key : map.keySet()) {
				infoTable[idx++] = new Object[] { key, map.get(key) };
			}
		}

		DefaultTableModel tableModel = new DefaultTableModel(infoTable, new Object[] { "Property", "Value" }) {
			private static final long serialVersionUID = -4303950078200984098L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		ProMTableWithoutHeader promTable = new ProMTableWithoutHeader(tableModel);
		promTable.setPreferredSize(new Dimension(300, 150));
		promTable.setMinimumSize(new Dimension(300, 150));
		promTable.setPreferredWidth(0, 180);
		promTable.setPreferredWidth(1, 20);
		return promTable;
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
		JLabel syncLbl = factory.createLabel("-Synchronous move (move log+model)");
		syncLbl.setForeground(Color.WHITE);
		legendPanel.add(syncLbl, "1," + row++ + ",l, c");

		layout.insertRow(row, TableLayout.PREFERRED);
		JPanel greyPanel = new JPanel();
		greyPanel.setBackground(new Color(100, 100, 100));
		legendPanel.add(greyPanel, "0," + row + ",r, c");
		JLabel moveInviLbl = factory.createLabel("-Unobservable move (move model only)");
		moveInviLbl.setForeground(Color.WHITE);
		legendPanel.add(moveInviLbl, "1," + row++ + ",l, c");

		layout.insertRow(row, TableLayout.PREFERRED);
		JPanel purplePanel = new JPanel();
		purplePanel.setBackground(new Color(205, 106, 205));
		legendPanel.add(purplePanel, "0," + row + ",r, c");
		JLabel moveRealLbl = factory.createLabel("-Skipped event class (move model only)");
		moveRealLbl.setForeground(Color.WHITE);
		legendPanel.add(moveRealLbl, "1," + row++ + ",l, c");

		layout.insertRow(row, TableLayout.PREFERRED);
		JPanel yellowPanel = new JPanel();
		yellowPanel.setBackground(new Color(255, 255, 0));
		legendPanel.add(yellowPanel, "0," + row + ",r, c");
		JLabel moveLogLbl = factory.createLabel("-Inserted event class (move log only)");
		moveLogLbl.setForeground(Color.WHITE);
		legendPanel.add(moveLogLbl, "1," + row++ + ",l, c");

		layout.insertRow(row, TableLayout.PREFERRED);
		JPanel redPanel = new JPanel();
		redPanel.setBackground(new Color(255, 0, 0));
		legendPanel.add(redPanel, "0," + row + ",r, c");
		JLabel moveViolLbl = factory.createLabel("-Move log+model with missing tokens");
		moveViolLbl.setForeground(Color.WHITE);
		legendPanel.add(moveViolLbl, "1," + row++ + ",l, c");

		layout.insertRow(row, 0.2);

		return legendPanel;
	}
}
