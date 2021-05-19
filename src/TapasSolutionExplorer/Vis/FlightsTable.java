package TapasSolutionExplorer.Vis;

import TapasDataReader.Flight;
import TapasUtilities.*;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Vector;

public class FlightsTable extends JPanel {

  public FlightsTable (Vector<Flight> vf, int step) {
    super();
    setLayout(new GridLayout(1,0));
    float max=0, maxAmpl=0;
    int maxNChanges=0;
    for (Flight fl:vf) {
      if (fl.delays[step] > max)
        max = fl.delays[step];
      int v[]=fl.delays;
      int n=0;
      float dv=0;
      for (int i=1; i<v.length; i++)
        if (v[i]>v[i-1]) {
          n++;
          dv=Math.max(dv,v[i]-v[i-1]);
        }
      maxNChanges=Math.max(maxNChanges,n);
      maxAmpl=Math.max(maxAmpl,dv);
    }
    JTable table = new JTable(new FlightsTableModel(vf,step)){
      public String getToolTipText (MouseEvent e) {
        String s="";
        java.awt.Point p = e.getPoint();
        int rowIndex = rowAtPoint(p);
        int colIndex = columnAtPoint(p);
        if (rowIndex>=0 && colIndex>=0) {
          int //realColumnIndex = convertColumnIndexToModel(colIndex),
              realRowIndex = convertRowIndexToModel(rowIndex);
          int v[]=vf.elementAt(realRowIndex).delays;
          s="<html><body style=background-color:rgb(255,255,204)>\n";
          s+="<p><b>"+vf.elementAt(realRowIndex).id+"</b>\n";
          s+="<table border=0 width=100%><tr align=right><td>step</td><td>delay</td><td>total</td></tr>\n";
          for (int i=0; i<v.length; i++)
            if (i==0 || i==v.length-1 || v[i]>v[i-1])
              s+="<tr align=right><td>"+i+"</td><td>"+((i==0)?v[0]:v[i]-v[i-1])+"</td><td>"+v[i]+"</tr>\n";
          s+="</table></body></html>";
        }
        return s;
      }

    };
    table.setPreferredScrollableViewportSize(new Dimension(500, 500));
    table.setFillsViewportHeight(true);
    table.setAutoCreateRowSorter(true);
    DefaultTableCellRenderer centerRenderer=new DefaultTableCellRenderer();
    centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
    for (int i=1; i<4; i++)
      table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
    //table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
    table.getColumnModel().getColumn(5).setCellRenderer(new RenderLabelBarChart(0,max));
    table.getColumnModel().getColumn(6).setCellRenderer(new RenderLabelTimeLine(max));
    table.getColumnModel().getColumn(7).setCellRenderer(new RenderLabelBarChart(0,maxNChanges));
    table.getColumnModel().getColumn(8).setCellRenderer(new RenderLabelTimeBars(maxAmpl));
    JScrollPane scrollPane = new JScrollPane(table);
    add(scrollPane);
  }

  class FlightsTableModel extends AbstractTableModel {
    Vector<Flight> vf=null;
    int step;
    public FlightsTableModel (Vector<Flight> vf, int step) {
      this.vf=vf;
      this.step=step;
    }
    private String[] columnNames={"Flight ID","From","To","Airline","CallSign","Delay","Cumulative delays","N changes","Added delays"};
    public int getColumnCount() {
      return columnNames.length;
    }
    public int getRowCount() {
      return vf.size();
    }
    public String getColumnName(int col) {
      return columnNames[col];
    }
    public Class getColumnClass(int c) {
      return (getValueAt(0, c)==null) ? null: getValueAt(0, c).getClass();
    }
    public Object getValueAt (int row, int col) {
      switch (col) {
        case 0:
          return vf.elementAt(row).id;
        case 3:
          String t[]=vf.elementAt(row).id.split("-");
          String s=t[2];
          int idxDigit=-1;
          for (int i=0; idxDigit==-1 && i<s.length(); i++)
            if (Character.isDigit(s.charAt(i)))
              idxDigit=i;
          return (idxDigit==-1) ? s : s.substring(0,idxDigit);
        case 1:
          t=vf.elementAt(row).id.split("-");
          return t[0];
        case 2:
          t=vf.elementAt(row).id.split("-");
          return t[1];
        case 4:
          t=vf.elementAt(row).id.split("-");
          return t[2];
        case 5:
          return vf.elementAt(row).delays[step]; // new Integer(vf.elementAt(row).delays[step]);
        case 6: case 8:
          setToolTipText(vf.elementAt(row).id);
          return vf.elementAt(row).delays;
        case 7:
          int v[]=vf.elementAt(row).delays;
          int n=0;
          for (int i=1; i<v.length; i++)
            if (v[i]>v[i-1])
              n++;
          return n;
      }
      return vf.elementAt(row).id;
    }
  }

}
