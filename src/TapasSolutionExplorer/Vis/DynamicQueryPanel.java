package TapasSolutionExplorer.Vis;

import TapasSolutionExplorer.UI.IntRangeCell;
import TapasSolutionExplorer.UI.IntSubRange;
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
  public boolean isAnyRowHiddenByQuery=false;

  JTable DQtbl=null;
  DQtblModel dqTblModel=null;
  final int columnMin=2, columnCount=4, columnRS=5;

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
    for (int i=columnMin; i<=columnMin+1; i++)
      DQtbl.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
    DQtbl.getColumnModel().getColumn(columnCount).setCellRenderer(new RenderLabelBarChart(0,tblModel.getRowCount()));
    //RendererRangeSlider rrs=new RendererRangeSlider();
    //rrs.getUI().trackListener
    DQtbl.getColumnModel().getColumn(columnRS).setCellRenderer(new IntRangeCell());
    DQtbl.getColumnModel().getColumn(columnRS).setCellEditor(new IntRangeCell());

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
    //System.out.println("* changed: row "+row+", col="+column);
    if ((column==columnMin || column==columnMin+1) && row>=0 && row<minmax.length && minmax[row]!=null) {
      for (int r = 0; r < tblModel.getRowCount(); r++) {
        int v = (int) tblModel.getValueAt(r, cols[row]);
        bQuery[r][row] = v >= query[row][0] && v <= query[row][1];
      }
      dqTblModel.fireTableCellUpdated(row,columnCount);
      dqTblModel.fireTableCellUpdated(row,columnRS);
      dqTblModel.fireTableCellUpdated(minmax.length,columnCount);
      tblModel.fireTableDataChanged();
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
  public boolean isBQtrue (int r) {
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
    isAnyRowHiddenByQuery=n<tblModel.getRowCount();
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
    String columnNames[]={"Feature","class","min","max","count","UI"};
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
      return (row<minmax.length && minmax[row]!=null && (col==columnMin || col==columnMin+1 || col==columnRS));
    }
    public Object getValueAt (int row, int col) {
      if (row==minmax.length)
        switch (col) {
          case 0:
            return "All conditions";
          case columnCount:
            return getCountBQtrue();
          case columnRS:
            return null;
          default:
            return "";
        }
      switch (col) {
        case 0: return tblModel.getColumnName(cols[row]);
        case 1:
          return tblModel.getColumnClass(cols[row])+
                  ((tblModel.getColumnClass(cols[row]).equals(Integer.class))?" ["+minmax[row][0]+" .. "+minmax[row][1]+"]":" Nvals="+lists[row].size());
        case columnMin:
          if (minmax[row]==null)
            return "";
          else
            return query[row][0];
        case columnMin+1:
          if (minmax[row]==null)
            return "";
          else
            return query[row][1];
        case columnCount:
          return getCountBQtrue(row);
        case columnRS:
          //return (minmax[row]==null) ? null : new int[]{minmax[row][0],minmax[row][1],query[row][0],query[row][1]};
          return (minmax[row]==null) ? null :
                     new IntSubRange(minmax[row][0],minmax[row][1],query[row][0],query[row][1]);
      }
      return 0;
    }
    public void setValueAt (Object value, int row, int col) {
      if (value==null)
        return;
      if (col==columnMin)
        try {
          int v = Integer.parseInt(value.toString());
          if (v >= minmax[row][0] && v <= query[row][1]) {
            query[row][0] = v;
            fireTableCellUpdated(row, col);
            fireTableCellUpdated(row, columnRS);
          }
        }
        catch (NumberFormatException nfe) {}
      else
      if (col==columnMin+1)
        try {
          int v=Integer.parseInt(value.toString());
          if (v>=query[row][0] && v<=minmax[row][1]) {
            query[row][1] = v;
            fireTableCellUpdated(row,col);
            fireTableCellUpdated(row, columnRS);
          }
        } catch (NumberFormatException nfe) {}
      else
      if (col==columnRS && (value instanceof IntSubRange)) {
        IntSubRange r = (IntSubRange) value;
        if (query[row][0]==r.currMin && query[row][1]==r.currMax)
          return;
        query[row][0]=r.currMin;
        query[row][1]=r.currMax;
        fireTableCellUpdated(row,col);
        fireTableCellUpdated(row, columnMin);
        fireTableCellUpdated(row, columnMin+1);
      }
    }
  }

}

