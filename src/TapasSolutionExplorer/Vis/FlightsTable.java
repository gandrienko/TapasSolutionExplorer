package TapasSolutionExplorer.Vis;

import TapasDataReader.Flight;
import TapasSolutionExplorer.Data.DataKeeper;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.Hashtable;
import java.util.Vector;

public class FlightsTable extends JPanel {

  public FlightsTable (Vector<Flight> vf, int step) {
    super();
    setLayout(new GridLayout(1,0));

    JTable table = new JTable(new FlightsTableModel(vf,step));
    table.setPreferredScrollableViewportSize(new Dimension(500, 500));
    table.setFillsViewportHeight(true);
    table.setAutoCreateRowSorter(true);
    DefaultTableCellRenderer centerRenderer=new DefaultTableCellRenderer();
    centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
    table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
    table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
    table.getColumnModel().getColumn(4).setCellRenderer(new RenderBar());
    table.getColumnModel().getColumn(5).setCellRenderer(new RenderBarLabel());
    //Create the scroll pane and add the table to it.
    JScrollPane scrollPane = new JScrollPane(table);
    //Add the scroll pane to this panel.
    add(scrollPane);
  }

  class MyLabel extends JLabel {
    public MyLabel (String text) {
      super(text,Label.RIGHT);
      setHorizontalAlignment(SwingConstants.RIGHT);
    }
    public void paint (Graphics g) {
      super.paint(g);
      g.drawLine(0,0,getWidth(),getHeight());
      //super.paint(g);
    }
  }
  class RenderBarLabel extends MyLabel implements TableCellRenderer {
    public RenderBarLabel() {
      super("");
      setOpaque(false);
    }
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      int v=((Integer)value).intValue();
      //setValue(v);
      setText(""+v);
      if (isSelected)
        setBackground(table.getSelectionBackground());
      else
        setBackground(table.getBackground());
      return this;
    }
  }
  class RenderBar extends JProgressBar implements TableCellRenderer {
    public RenderBar() {
      super(0,100);
      setValue(0);
      setStringPainted(true);
      setOpaque(false);
      // https://stackoverflow.com/questions/25385700/how-to-set-position-of-string-painted-in-jprogressbar
    }
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      int v=((Integer)value).intValue();
      setValue(v);
      setString(""+v);
      return this;
    }
  }
  class FlightsTableModel extends AbstractTableModel {
    Vector<Flight> vf=null;
    int step;
    public FlightsTableModel (Vector<Flight> vf, int step) {
      this.vf=vf;
      this.step=step;
    }
    private String[] columnNames={"Flight ID","From","To","CallSign","Delay","Delay+"};
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
      return getValueAt(0, c).getClass();
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
        case 4: case 5:
          return new Integer(vf.elementAt(row).delays[step]);
      }
      return vf.elementAt(row).id;//data[row][col];
    }
  }

}