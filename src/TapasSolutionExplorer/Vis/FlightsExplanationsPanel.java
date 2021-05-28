package TapasSolutionExplorer.Vis;

import TapasDataReader.Explanation;
import TapasDataReader.Flight;
import TapasUtilities.RenderLabelBarChart;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Hashtable;
import java.util.Vector;

public class FlightsExplanationsPanel extends JPanel {

  JTable tableList=null,
         tableExpl=null;

  public FlightsExplanationsPanel (Hashtable<String,int[]> attrsInExpl, Vector<Flight> vf, int minStep, int maxStep, boolean bShowZeroActions) {
    super();
    JFrame frame = new JFrame("Explanations for " + ((vf.size()==1) ? vf.elementAt(0).id : vf.size() + " flights") + " at steps ["+minStep+".."+maxStep+"]");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    FlightsListOfExplTableModel tableListModel=new FlightsListOfExplTableModel(vf,minStep,maxStep,bShowZeroActions);
    tableList=new JTable(tableListModel);
    tableList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        FlightsSingleExplTableModel tableExplModel=(FlightsSingleExplTableModel)tableExpl.getModel();
        tableExplModel.setExpl(vf.elementAt(tableListModel.rowFlNs[tableList.getSelectedRow()]).expl[tableListModel.rowFlSteps[tableList.getSelectedRow()]]);
      }
    });
    tableList.setPreferredScrollableViewportSize(new Dimension(400, 300));
    tableList.setFillsViewportHeight(true);
    tableList.setAutoCreateRowSorter(true);
    tableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    tableList.setRowSelectionAllowed(true);
    tableList.setColumnSelectionAllowed(false);
    DefaultTableCellRenderer centerRenderer=new DefaultTableCellRenderer();
    centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
    tableList.getColumnModel().getColumn(1).setCellRenderer(new RenderLabelBarChart(minStep,maxStep));
    tableList.getColumnModel().getColumn(2).setCellRenderer(new RenderLabelBarChart(0,10));
    JScrollPane scrollPaneList = new JScrollPane(tableList);
    scrollPaneList.setOpaque(true);

    tableExpl=new JTable(new FlightsSingleExplTableModel(attrsInExpl));
    tableExpl.setPreferredScrollableViewportSize(new Dimension(400, 300));
    tableExpl.setFillsViewportHeight(true);
    tableExpl.setAutoCreateRowSorter(true);
    tableExpl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    tableExpl.setRowSelectionAllowed(true);
    tableExpl.setColumnSelectionAllowed(false);
    //DefaultTableCellRenderer centerRenderer=new DefaultTableCellRenderer();
    //centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
    //tableList.getColumnModel().getColumn(1).setCellRenderer(new RenderLabelBarChart(minStep,maxStep));
    //tableList.getColumnModel().getColumn(2).setCellRenderer(new RenderLabelBarChart(0,10));
    JScrollPane scrollPaneExpl = new JScrollPane(tableExpl);
    scrollPaneExpl.setOpaque(true);

    JSplitPane splitPane=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,scrollPaneList,scrollPaneExpl);
    splitPane.setOneTouchExpandable(true);
    splitPane.setDividerLocation(500);
    Dimension minimumSize = new Dimension(100, 50);
    scrollPaneList.setMinimumSize(minimumSize);
    scrollPaneExpl.setMinimumSize(minimumSize);

    frame.getContentPane().add(splitPane, BorderLayout.CENTER);
    frame.pack();
    frame.setVisible(true);
  }

  class FlightsListOfExplTableModel extends AbstractTableModel {
    Vector<Flight> vf = null;
    int minStep, maxStep;
    boolean bShowZeroActions;
    public int rowFlNs[] = null;
    public int rowFlSteps[] = null;

    public FlightsListOfExplTableModel(Vector<Flight> vf, int minStep, int maxStep, boolean bShowZeroActions) {
      this.vf = vf;
      this.minStep = minStep;
      this.maxStep = maxStep;
      this.bShowZeroActions = bShowZeroActions;
      calcNexpl();
    }
    protected void calcNexpl() {
      int n = 0;
      for (Flight f : vf)
        for (int step = minStep; step <= maxStep; step++)
          if (f.expl[step] != null && (f.expl[step].action > 0 || bShowZeroActions))
            n++;
      rowFlNs = new int[n];
      rowFlSteps = new int[n];
      n = 0;
      for (int i=0; i<vf.size(); i++) {
        Flight f=vf.elementAt(i);
        for (int step = minStep; step <= maxStep; step++)
          if (f.expl[step] != null && (f.expl[step].action > 0 || bShowZeroActions)) {
            rowFlNs[n] = i;
            rowFlSteps[n] = step;
            n++;
          }
      }
    }
    private String columnNames[] = {"Flight ID", "Step", "Action"};
    public String getColumnName(int col) {
      return columnNames[col];
    }
    public int getColumnCount() {
      return columnNames.length;
    }
    public int getRowCount() {
      return rowFlNs.length;
    }
    public Class getColumnClass(int c) {
      return (getValueAt(0, c) == null) ? null : getValueAt(0, c).getClass();
    }
    public Object getValueAt(int row, int col) {
      Flight f=vf.elementAt(rowFlNs[row]);
      switch (col) {
        case 0:
          return f.id;
        case 1:
          return rowFlSteps[row];
        case 2:
          return f.expl[rowFlSteps[row]].action;
      }
      return 0;
    }
  }

  class FlightsSingleExplTableModel extends AbstractTableModel {
    Hashtable<String,int[]> attrsInExpl=null;
    Explanation expl=null;
    public FlightsSingleExplTableModel (Hashtable<String,int[]> attrsInExpl) {
      this.attrsInExpl=attrsInExpl;
    }
    public void setExpl (Explanation expl) {
      this.expl=expl;
      fireTableDataChanged();
    }
    private String columnNames[] = {"Level", "Feature", "Value", "min", "max", "interval_min", "interval-max"};
    public String getColumnName(int col) {
      return columnNames[col];
    }
    public int getColumnCount() {
      return columnNames.length;
    }
    public int getRowCount() {
      return (expl==null)?0:expl.eItems.length;
    }
    public Class getColumnClass(int c) {
      return (getValueAt(0, c) == null) ? null : getValueAt(0, c).getClass();
    }
    public Object getValueAt(int row, int col) {
      switch (col) {
        case 0:
          return row;
        case 1:
          return expl.eItems[row].attr;
        case 2:
          return expl.eItems[row].value;
        case 3:
          int minmax[]=attrsInExpl.get(expl.eItems[row].attr);
          return minmax[0];
        case 4:
          minmax=attrsInExpl.get(expl.eItems[row].attr);
          return minmax[1];
        case 5:
          return expl.eItems[row].interval[0];
        case 6:
          return expl.eItems[row].interval[1];
      }
      return 0;
    }

  }

}
