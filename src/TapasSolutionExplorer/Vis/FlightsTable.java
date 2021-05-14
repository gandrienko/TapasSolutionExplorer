package TapasSolutionExplorer.Vis;

import TapasDataReader.Flight;
import TapasUtilities.*;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Vector;

public class FlightsTable extends JPanel {

  public FlightsTable (Vector<Flight> vf, int step) {
    super();
    setLayout(new GridLayout(1,0));
    float max=0;
    for (Flight fl:vf)
      if (fl.delays[step]>max)
        max=fl.delays[step];
    JTable table = new JTable(new FlightsTableModel(vf,step));
    table.setPreferredScrollableViewportSize(new Dimension(500, 500));
    table.setFillsViewportHeight(true);
    table.setAutoCreateRowSorter(true);
    DefaultTableCellRenderer centerRenderer=new DefaultTableCellRenderer();
    centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
    table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
    table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
    table.getColumnModel().getColumn(4).setCellRenderer(new RenderLabelBarChart(0,max));
    table.getColumnModel().getColumn(5).setCellRenderer(new RenderLabelTimeLine(max));
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
    private String[] columnNames={"Flight ID","From","To","CallSign","Delay","delays"};
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
    public Object getValueAt(int row, int col) {
      switch (col) {
        case 0:
          return vf.elementAt(row).id;
        case 1:
          String t[]=vf.elementAt(row).id.split("-");
          return t[0];
        case 2:
          t=vf.elementAt(row).id.split("-");
          return t[1];
        case 3:
          t=vf.elementAt(row).id.split("-");
          return t[2];
        case 4:
          return vf.elementAt(row).delays[step]; // new Integer(vf.elementAt(row).delays[step]);
        case 5:
          return vf.elementAt(row).delays;
      }
      return vf.elementAt(row).id;
    }
  }

}
