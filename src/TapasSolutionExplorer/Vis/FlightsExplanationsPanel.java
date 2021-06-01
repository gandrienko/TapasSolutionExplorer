package TapasSolutionExplorer.Vis;

import TapasDataReader.Explanation;
import TapasDataReader.ExplanationItem;
import TapasDataReader.Flight;
import TapasUtilities.RenderLabelBarChart;
import TapasUtilities.RenderLabel_ValueInSubinterval;


import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Hashtable;
import java.util.Vector;

public class FlightsExplanationsPanel extends JPanel {

  JTable tableList=null,
         tableExpl=null;
  FlightsSingleExplTableModel tableExplModel=null;
  int selectedRow=-1;

  public FlightsExplanationsPanel (Hashtable<String,int[]> attrsInExpl, Vector<Flight> vf, int minStep, int maxStep, boolean bShowZeroActions) {
    super();
    JFrame frame = new JFrame("Explanations for " + ((vf.size()==1) ? vf.elementAt(0).id : vf.size() + " flights") + " at steps ["+minStep+".."+maxStep+"]");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    JCheckBox cbExplCombine=new JCheckBox("Combine intervals",false);
    JCheckBox cbExplAsInt=new JCheckBox("Integer intervals",false);
    FlightsListOfExplTableModel tableListModel=new FlightsListOfExplTableModel(vf,minStep,maxStep,bShowZeroActions);
    tableExplModel=new FlightsSingleExplTableModel(attrsInExpl);

    tableList=new JTable(tableListModel);
    tableList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        selectedRow=tableList.getSelectedRow();
        Explanation expl=vf.elementAt(tableListModel.rowFlNs[selectedRow]).expl[tableListModel.rowFlSteps[selectedRow]];
        tableExpl.getColumnModel().getColumn(0).setCellRenderer(new RenderLabelBarChart(0,expl.eItems.length));
        setExpl(attrsInExpl,expl,cbExplCombine.isSelected(),cbExplAsInt.isSelected());
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

    tableExpl=new JTable(tableExplModel) {
      public String getToolTipText(MouseEvent e) {
        String s = "";
        java.awt.Point p = e.getPoint();
        int rowIndex = rowAtPoint(p);
        if (rowIndex>=0) {
          int realRowIndex = convertRowIndexToModel(rowIndex);
          s="<html><body style=background-color:rgb(255,255,204)><p align=center><b>"+tableExplModel.eItems[realRowIndex].attr+"</b></p>\n";
          s+="<table>\n";
          s+="<tr><td>Value</td><td>"+tableExplModel.eItems[realRowIndex].value+"</td></tr>\n";
          s+="<tr><td>Condition min..max</td><td>["+tableExplModel.eItems[realRowIndex].interval[0]+".."+tableExplModel.eItems[realRowIndex].interval[1]+"]</td></tr>\n";
          int minmax[]=attrsInExpl.get(tableExplModel.eItems[realRowIndex].attr);
          s+="<tr><td>Global min..max</td><td>["+minmax[0]+".."+minmax[1]+"]</td></tr>\n";
          s+="</table>\n";
          s+="</body></html>";
        }
        return s;
      }
    };
    tableExpl.setPreferredScrollableViewportSize(new Dimension(400, 300));
    tableExpl.setFillsViewportHeight(true);
    tableExpl.setAutoCreateRowSorter(true);
    tableExpl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    tableExpl.setRowSelectionAllowed(true);
    tableExpl.setColumnSelectionAllowed(false);
    tableExpl.getColumnModel().getColumn(2).setCellRenderer(new RenderLabel_ValueInSubinterval());
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

    cbExplCombine.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (selectedRow>=0) {
          Explanation expl=vf.elementAt(tableListModel.rowFlNs[selectedRow]).expl[tableListModel.rowFlSteps[selectedRow]];
          setExpl(attrsInExpl,expl,cbExplCombine.isSelected(),cbExplAsInt.isSelected());
        }
      }
    });
    cbExplAsInt.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (selectedRow>=0) {
          Explanation expl=vf.elementAt(tableListModel.rowFlNs[selectedRow]).expl[tableListModel.rowFlSteps[selectedRow]];
          setExpl(attrsInExpl,expl,cbExplCombine.isSelected(),cbExplAsInt.isSelected());
        }
      }
    });

    frame.getContentPane().add(splitPane, BorderLayout.CENTER);
    JPanel controlPanel=new JPanel(new FlowLayout());
    controlPanel.add(cbExplCombine);
    controlPanel.add(cbExplAsInt);
    frame.getContentPane().add(controlPanel, BorderLayout.SOUTH);
    frame.pack();
    frame.setVisible(true);
  }

  protected void setExpl (Hashtable<String,int[]> attrsInExpl, Explanation expl, boolean bCombine, boolean bInt) {
    ExplanationItem eItems[]=expl.eItems;
    if (bCombine)
      eItems=expl.getExplItemsCombined(eItems);
    if (bInt)
      eItems=expl.getExplItemsAsIntegeres(eItems,attrsInExpl);
    tableExplModel.setExpl(eItems);
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
    public ExplanationItem eItems[]=null;
    public FlightsSingleExplTableModel (Hashtable<String,int[]> attrsInExpl) {
      this.attrsInExpl=attrsInExpl;
    }
    public void setExpl (ExplanationItem eItems[]) {
      this.eItems=eItems;
      fireTableDataChanged();
    }
    private String columnNames[] = {"Level", "Feature", /* "Value", "min", "max", "interval_min", "interval-max",*/ "Value"};
    public String getColumnName(int col) {
      return columnNames[col];
    }
    public int getColumnCount() {
      return columnNames.length;
    }
    public int getRowCount() {
      return (eItems==null)?0:eItems.length;
    }
    public Class getColumnClass(int c) {
      return (getValueAt(0, c) == null) ? null : getValueAt(0, c).getClass();
    }
    public Object getValueAt(int row, int col) {
      switch (col) {
        case 0:
          return row;
        case 1:
          return eItems[row].attr;
/*
        case 2:
          return eItems[row].value;
        case 3:
          int minmax[]=attrsInExpl.get(eItems[row].attr);
          return minmax[0];
        case 4:
          minmax=attrsInExpl.get(eItems[row].attr);
          return minmax[1];
        case 5:
          return eItems[row].interval[0];
        case 6:
          return eItems[row].interval[1];
*/
        case 2:
          float v1=attrsInExpl.get(eItems[row].attr)[0], v2=attrsInExpl.get(eItems[row].attr)[1],
                v3=(float)eItems[row].interval[0], v4=(float)eItems[row].interval[1];
          if (v3==Float.NEGATIVE_INFINITY)
            v3=v1;
          if (v4==Float.POSITIVE_INFINITY)
            v4=v2;
          return new float[]{eItems[row].value,v1,v2,v3,v4};
      }
      return 0;
    }

  }

}
