package TapasSolutionExplorer.Vis;

import TapasUtilities.RenderLabelBarChart;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.HashSet;

public class DynamicQueryPanel extends JPanel implements TableModelListener {

  AbstractTableModel tblModel=null;
  int cols[]=null;
  int minmax[][], query[][]=null;
  HashSet<String> lists[]=null;
  boolean bQuery[][]=null;

  JTable DQtbl=null;
  DQtblModel dqTblModel=null;


  public DynamicQueryPanel (AbstractTableModel tblModel, int cols[]) {
    super();
    this.tblModel=tblModel;
    this.cols=cols;
    dqTblModel=new DQtblModel();
    DQtbl=new JTable(dqTblModel);
    DQtbl.getModel().addTableModelListener(this);
    //DQtbl.setAutoCreateRowSorter(true);
    DefaultTableCellRenderer rightRenderer=new DefaultTableCellRenderer();
    rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
    for (int i=2; i<=3; i++)
      DQtbl.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
    DQtbl.getColumnModel().getColumn(4).setCellRenderer(new RenderLabelBarChart(0,tblModel.getRowCount()));

    JScrollPane scrollPane = new JScrollPane(DQtbl);
    scrollPane.setOpaque(true);
    setLayout(new BorderLayout());
    add(new JLabel("Dynamic Query",JLabel.CENTER),BorderLayout.NORTH);
    add(scrollPane,BorderLayout.CENTER);
    createBquery();
    calcMinMax();
  }

  public void tableChanged(TableModelEvent e) {
    int row = e.getFirstRow();
    int column = e.getColumn();
    System.out.println("* changed: row "+row+", col="+column);
    if ((column==2 || column==3) && row>=0 && row<minmax.length && minmax[row]!=null) {
      for (int r = 0; r < tblModel.getRowCount(); r++) {
        int v = (int) tblModel.getValueAt(r, cols[row]);
        bQuery[r][row] = v >= query[row][0] && v <= query[row][1];
      }
      dqTblModel.fireTableCellUpdated(row,4);
      dqTblModel.fireTableCellUpdated(minmax.length,4);
    }
  }

  private void createBquery() {
    bQuery=new boolean[tblModel.getRowCount()][];
    for (int r=0; r<bQuery.length; r++) {
      bQuery[r] = new boolean[cols.length];
      for (int c=0; c<bQuery[r].length; c++)
        bQuery[r][c]=true;
    }
  }
  private boolean isBQtrue (int r) {
    for (int c=0; c<bQuery[r].length; c++)
      if (!bQuery[r][c])
        return false;
    return true;
  }
  private int getCountBQtrue (int c) {
    int n=0;
    for (int r=0; r<tblModel.getRowCount(); r++)
      if (bQuery[r][c])
        n++;
    return n;
  }
  private int getCountBQtrue () {
    int n=0;
    for (int r=0; r<tblModel.getRowCount(); r++)
      if (isBQtrue(r))
        n++;
    return n;
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
    String columnNames[]={"Feature","class","min","max","count"};
    public String getColumnName(int col) {
      return columnNames[col];
    }
    public int getColumnCount() {
      return columnNames.length;
    }
    public int getRowCount() {
      return 1+cols.length;
    }
    public Class getColumnClass(int c) {
      return (getValueAt(0, c)==null) ? null: getValueAt(0, c).getClass();
    }
    public boolean isCellEditable(int row, int col) {
      return (row<minmax.length && minmax[row]!=null && (col==2 || col==3));
    }
    public Object getValueAt (int row, int col) {
      if (row==minmax.length)
        switch (col) {
          case 0:
            return "All conditions";
          case 4:
            return getCountBQtrue();
          default:
            return "";
        }
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
        case 4:
          return getCountBQtrue(row);
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
            //fireTableDataChanged();
          }
        }
        catch (NumberFormatException nfe) {}
      if (col==3)
        try {
          int v=Integer.valueOf((String)value).intValue();
          if (v>=query[row][0] && v<=minmax[row][1]) {
            query[row][1] = v;
            fireTableCellUpdated(row,col);
            //fireTableDataChanged();
          }
        } catch (NumberFormatException nfe) {}
    }
  }

}

