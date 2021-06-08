package TapasSolutionExplorer.Vis;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.HashSet;

public class DynamicQueryPanel extends JPanel {

  AbstractTableModel tblModel=null;
  int cols[]=null;
  int minmax[][], query[][]=null;
  HashSet<String> lists[]=null;

  JTable DQtbl=null;

  public DynamicQueryPanel (AbstractTableModel tblModel, int cols[]) {
    super();
    this.tblModel=tblModel;
    this.cols=cols;
    DQtbl=new JTable(new DQtblModel());
    //DQtbl.setAutoCreateRowSorter(true);
    DefaultTableCellRenderer rightRenderer=new DefaultTableCellRenderer();
    rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
    for (int i=2; i<=3; i++)
      DQtbl.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);

    JScrollPane scrollPane = new JScrollPane(DQtbl);
    scrollPane.setOpaque(true);
    setLayout(new BorderLayout());
    add(new JLabel("Dynamic Query",JLabel.CENTER),BorderLayout.NORTH);
    add(scrollPane,BorderLayout.CENTER);
    calcMinMax();
  }

  private void calcMinMax() {
    minmax=new int[cols.length][];
    query=new int[cols.length][];
    lists=new HashSet[cols.length];
    for (int c=0; c<cols.length; c++)
      if (tblModel.getColumnClass(c).equals(Integer.class)) {
        //System.out.println("* col "+c+" is integer");
        minmax[c]=new int[2];
        minmax[c][0]=Integer.MAX_VALUE;
        minmax[c][1]=Integer.MIN_VALUE;
        for (int r=0; r<tblModel.getRowCount(); r++) {
          int v=(Integer)tblModel.getValueAt(r,c);
          minmax[c][0]=Math.min(minmax[c][0],v);
          minmax[c][1]=Math.max(minmax[c][1],v);
        }
        query[c]=new int[2];
        query[c][0]=minmax[c][0];
        query[c][1]=minmax[c][1];
      }
      else {
        //System.out.println("* col " + c + " is " + tblModel.getColumnClass(c));
        lists[c]=new HashSet<>();
        for (int r=0; r<tblModel.getRowCount(); r++) {
          String v=(String)tblModel.getValueAt(r,c);
          lists[c].add(v);
        }

      }
  }

  class DQtblModel extends AbstractTableModel {
    public DQtblModel () {
      super();
    }
    String columnNames[]={"Feature","class","min","max"};
    public String getColumnName(int col) {
      return columnNames[col];
    }
    public int getColumnCount() {
      return columnNames.length;
    }
    public int getRowCount() {
      return cols.length;
    }
    public Class getColumnClass(int c) {
      return (getValueAt(0, c)==null) ? null: getValueAt(0, c).getClass();
    }
    public boolean isCellEditable(int row, int col) {
      return (minmax[row]!=null && (col==2 || col==3));
    }
    public Object getValueAt (int row, int col) {
      switch (col) {
        case 0: return tblModel.getColumnName(cols[row]);
        case 1:
          return tblModel.getColumnClass(cols[row])+
                  ((tblModel.getColumnClass(cols[row]).equals(Integer.class))?" ["+minmax[row][0]+" .. "+minmax[row][1]+"]":" Nvals="+lists[row].size());
        case 2:
          if (minmax[row]==null)
            return "";
          else
            return query[row][0];
        case 3:
          if (minmax[row]==null)
            return "";
          else
            return query[row][1];
      }
      return 0;
    }
    public void setValueAt (Object value, int row, int col) {
      if (col==2)
        try {
          int v = Integer.valueOf((String) value).intValue();
          if (v >= minmax[row][0] && v <= query[row][1]) {
            query[row][0] = v;
            fireTableCellUpdated(row, col);
          }
        }
        catch (NumberFormatException nfe) {}
      if (col==3)
        try {
          int v=Integer.valueOf((String)value).intValue();
          if (v>=query[row][0] && v<=minmax[row][1]) {
            query[row][1] = v;
            fireTableCellUpdated(row,col);
          }
        } catch (NumberFormatException nfe) {}
    }
  }

}

