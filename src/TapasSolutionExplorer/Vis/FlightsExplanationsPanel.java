package TapasSolutionExplorer.Vis;

import TapasDataReader.Flight;
import TapasUtilities.RenderLabelBarChart;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Hashtable;
import java.util.Vector;

public class FlightsExplanationsPanel extends JPanel {

  JTable table=null;

  public FlightsExplanationsPanel (Hashtable<String,int[]> attrsInExpl, Vector<Flight> vf, int minStep, int maxStep, boolean bShowZeroActions) {
    super();
    JFrame frame = new JFrame("Explanations for " + ((vf.size()==1) ? vf.elementAt(0).id : vf.size() + " flights") + " at steps ["+minStep+".."+maxStep+"]");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    table=new JTable(new FlightsExplTableModel(vf,minStep,maxStep,bShowZeroActions));
    table.setPreferredScrollableViewportSize(new Dimension(400, 300));
    table.setFillsViewportHeight(true);
    table.setAutoCreateRowSorter(true);
    table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    table.setRowSelectionAllowed(true);
    table.setColumnSelectionAllowed(false);
    DefaultTableCellRenderer centerRenderer=new DefaultTableCellRenderer();
    centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
    table.getColumnModel().getColumn(1).setCellRenderer(new RenderLabelBarChart(minStep,maxStep));
    table.getColumnModel().getColumn(2).setCellRenderer(new RenderLabelBarChart(0,10));
    JScrollPane scrollPane = new JScrollPane(table);
    scrollPane.setOpaque(true);
    frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
    frame.pack();
    frame.setVisible(true);
  }

  class FlightsExplTableModel extends AbstractTableModel {
    Vector<Flight> vf = null;
    int minStep, maxStep;
    boolean bShowZeroActions;
    int rowFlNs[] = null;
    int rowFlSteps[] = null;

    public FlightsExplTableModel(Vector<Flight> vf, int minStep, int maxStep, boolean bShowZeroActions) {
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

}
