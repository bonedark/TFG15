/**
 * 
 */
package org.processmining.plugins.replayer.replayresult.visualization;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.ViewSpecificAttributeMap;
import org.processmining.models.jgraph.ProMJGraphVisualizer;
import org.processmining.models.jgraph.visualization.ProMJGraphPanel;
import org.processmining.plugins.log.ui.logdialog.LogViewUI;
import org.processmining.plugins.replayer.replayresult.CaseReplayResult;

import com.fluxicon.slickerbox.components.SlickerTabbedPane;
import com.fluxicon.slickerbox.factory.SlickerFactory;

/**
 * @author aadrians
 * 
 */
public class CaseRepResultVisPanel extends JPanel {
	private static final long serialVersionUID = -8071123403206953201L;

	// main panel
	protected JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
	protected LogViewUI logViewUI;

	// south panel
	protected SlickerTabbedPane tabPane;

	public CaseRepResultVisPanel(PluginContext context, CaseReplayResult caseReplayResult) {
		this(context, caseReplayResult, true);
	}

	public CaseRepResultVisPanel(PluginContext context, CaseReplayResult caseReplayResult, boolean withTree) {
		setLayout(new BorderLayout());

		if (!withTree) {
			this.add(createBottomPanel(caseReplayResult));
		} else {
			// setup split pane
			splitPane.setResizeWeight(0.5);
			splitPane.setOneTouchExpandable(true);

			if (caseReplayResult.getGraph() != null){
				ViewSpecificAttributeMap viewSpecificMap = new ViewSpecificAttributeMap();
				ProMJGraphPanel visPanel = ProMJGraphVisualizer.instance().visualizeGraph(context, caseReplayResult.getGraph(),
						viewSpecificMap);
				visPanel.setBackground(Color.WHITE);
				splitPane.setLeftComponent(visPanel);
			} else {
				splitPane.setLeftComponent(new JLabel("Visualization is not available for this replay result"));
			}
			splitPane.setRightComponent(createBottomPanel(caseReplayResult));

			this.add(splitPane, BorderLayout.CENTER);

			// signal the change and repaint graph
			repaint();
		}
	}

	public JComponent createBottomPanel(CaseReplayResult caseReplayResult) {
		// add panel below
		JPanel bgPanel = new JPanel();
		bgPanel.setBorder(BorderFactory.createEmptyBorder());
		bgPanel.setBackground(new Color(30, 30, 30));
		bgPanel.setLayout(new TableLayout(new double[][] { {0.50, 0.20, 0.30}, { TableLayout.PREFERRED }}));
		
		// from loginfo
		JPanel comparisonPanel = new JPanel();
		double[][] size = new double[][] { { TableLayoutConstants.FILL },
				{ TableLayoutConstants.FILL, TableLayoutConstants.FILL } };
		comparisonPanel.setPreferredSize(new Dimension(1000, 160));
		comparisonPanel.setBackground(new Color(30, 30, 30));
		TableLayout layout = new TableLayout(size);
		comparisonPanel.setLayout(layout);
		ProcessInstanceConformanceView realTrace = new ProcessInstanceConformanceView("Real trace",
				caseReplayResult.getTrace(), caseReplayResult.getLogInfo());
		ProcessInstanceConformanceView replayedTrace = new ProcessInstanceConformanceView("Replayed",
				caseReplayResult.getNodeInstance(), caseReplayResult.getStepTypes());
		comparisonPanel.add(realTrace, "0,0");
		comparisonPanel.add(replayedTrace, "0,1");

		bgPanel.add(comparisonPanel, "0,0,l,c");

		// add legend
		bgPanel.add(createLegendPanel(), "1,0,c,c");
		
		// from information
		Map<String, String> info = caseReplayResult.getInfoTable();
		if (info != null) {
			bgPanel.add(createGUIInfo(info), "2,0,r,c");
		}
		return bgPanel;
	}
	
	private JComponent createGUIInfo(Map<String, String> info) {
		int rowNumber= 0;
		JPanel guiInfo = new JPanel();
		guiInfo.setBorder(BorderFactory.createEmptyBorder());
		guiInfo.setBackground(new Color(30, 30, 30));
		guiInfo.setLayout(new TableLayout(new double[][] { {TableLayout.PREFERRED}, {30, 90, 30, 200}}));
		SlickerFactory factory = SlickerFactory.instance();
		
		Map<String, String> infoCpy = new HashMap<String, String>(info);

		JLabel lblRepParam = factory.createLabel("REPLAY PARAMETER");
		lblRepParam.setForeground(Color.WHITE);
		guiInfo.add(lblRepParam, "0," + rowNumber++ + ",c,b");
		
		// find cost information (if exists)
		Map<String, String> costMap = new HashMap<String, String>();
		Iterator<String> it = infoCpy.keySet().iterator();
		while(it.hasNext()){
			String key = it.next();
			if (key.startsWith("Cost of ")){
				costMap.put(key, infoCpy.get(key));
			}
		}
		for (String key : costMap.keySet()){
			infoCpy.remove(key);
		}
		
		guiInfo.add(createVisTablePane(costMap), "0," + rowNumber++ + ",c,t");
		
		JLabel lblCaseRepRes= factory.createLabel("CASE REPLAY RESULT");
		lblCaseRepRes.setForeground(Color.WHITE);
		guiInfo.add(lblCaseRepRes, "0," + rowNumber++ + ",c,b");
		
		guiInfo.add(createVisTablePane(infoCpy), "0," + rowNumber++ + ",c,t");
		
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
		redPanel.setBackground(new Color(255, 0,0));
		legendPanel.add(redPanel, "0," + row + ",r, c");
		JLabel moveViolLbl = factory.createLabel("-Violating synchronous activities");
		moveViolLbl.setForeground(Color.WHITE);
		legendPanel.add(moveViolLbl, "1," + row++ + ",l, c");

		layout.insertRow(row, 0.2);

		return legendPanel;
	}
	public JTable autoResizeColWidth(JTable table) {
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
