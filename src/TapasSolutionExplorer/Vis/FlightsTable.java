package TapasSolutionExplorer.Vis;

import TapasDataReader.Flight;
import TapasUtilities.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Hashtable;
import java.util.Vector;

public class FlightsTable extends JPanel {

  /**
   * Controls for selecting the steps range to view
   */
  protected RangeSlider stepFocuser=null;
  protected JTextField tfStart=null, tfEnd=null;
  //protected JButton bFullRange=null;
  protected JTable table=null;

  public FlightsTable (Vector<Flight> vf, Hashtable<String,int[]> flightsTimesInSector, int step) {
    super();
    setPreferredSize(new Dimension(1000,900));
    setLayout(new BorderLayout()); //(new GridLayout(1,0));
    float max=0, maxAmpl=0;
    int maxNChanges=0, maxStep=0;
    int maxNstep=vf.elementAt(0).delays.length-1;
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
      if (maxStep==0)
        maxStep=fl.delays.length;
    }
    table = new JTable(new FlightsTableModel(vf,flightsTimesInSector,step)) {
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
    table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    table.setRowSelectionAllowed(true);
    table.setColumnSelectionAllowed(false);
    DefaultTableCellRenderer centerRenderer=new DefaultTableCellRenderer();
    centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
    for (int i=1; i<4; i++)
      table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
    table.getColumnModel().getColumn(5).setCellRenderer(new RenderLabelBarChart(0,max));
    table.getColumnModel().getColumn(6).setCellRenderer(new RenderLabelTimeLine(max));
    table.getColumnModel().getColumn(7).setCellRenderer(new RenderLabelBarChart(0,maxNChanges));
    table.getColumnModel().getColumn(8).setCellRenderer(new RenderLabelTimeBars(maxAmpl));
    table.getColumnModel().getColumn(9).setCellRenderer(new RenderLabelBarChart(0,maxStep));
    table.getColumnModel().getColumn(10).setCellRenderer(new RenderLabelBarChart(0,maxStep));
    table.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
      public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting())
          return;
        for (int c : table.getSelectedRows())
          System.out.print(String.format(" %d(%d)", c, table.convertRowIndexToModel(c)));
        System.out.println();
      }
    });
    if (flightsTimesInSector!=null)
      for (int i=11; i<=13; i++) {
        int maxDuration=-1;
        if (i==11) {
          for (String s:flightsTimesInSector.keySet()) {
            int t[]=flightsTimesInSector.get(s);
            if (t[1]-t[0]>maxDuration)
              maxDuration=t[1]-t[0];
          }
        }
        RenderLabelBarChart rlbc=new RenderLabelBarChart(0,(i==11)?maxDuration:1440);
        if (i<11)
          rlbc.setbModeTimeOfDay();
        table.getColumnModel().getColumn(i).setCellRenderer(rlbc);
      }
    JScrollPane scrollPane = new JScrollPane(table);
    scrollPane.setOpaque(true);
    add(scrollPane,BorderLayout.CENTER);
    JPanel cp=new JPanel(new BorderLayout(5,2));
    cp.setBorder(BorderFactory.createLineBorder(Color.black));
    stepFocuser=new RangeSlider();
    stepFocuser.setPreferredSize(new Dimension(240,stepFocuser.getPreferredSize().height));
    stepFocuser.setMinimum(0);
    stepFocuser.setMaximum(maxNstep);
    stepFocuser.setValue(0);
    stepFocuser.setUpperValue(maxNstep);
    stepFocuser.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        tfStart.setText(""+stepFocuser.getValue());
        tfEnd.setText(""+stepFocuser.getUpperValue());
        ((FlightsTableModel)table.getModel()).setSteps(stepFocuser.getValue(),stepFocuser.getUpperValue());
      }
    });
    tfStart=new JTextField("0",3);
    tfEnd=new JTextField(""+maxNstep,3);
    tfStart.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          int n=Integer.valueOf(tfStart.getText()).intValue();
          if (n>=0 && n<=maxNstep && n<=stepFocuser.getUpperValue()) {
            stepFocuser.setValue(n);
            ((FlightsTableModel)table.getModel()).setSteps(stepFocuser.getValue(),stepFocuser.getUpperValue());
          }
        } catch (NumberFormatException nfe) {
          tfStart.setText(""+stepFocuser.getValue());
        }
      }
    });
    tfEnd.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          int n=Integer.valueOf(tfEnd.getText()).intValue();
          if (n>=0 && n<=maxNstep && n>=stepFocuser.getValue()) {
            stepFocuser.setUpperValue(n);
            ((FlightsTableModel)table.getModel()).setSteps(stepFocuser.getValue(),stepFocuser.getUpperValue());
          }
        } catch (NumberFormatException nfe) {
          tfEnd.setText(""+stepFocuser.getUpperValue());
        }
      }
    });
    cp.add(tfStart,BorderLayout.WEST);
    cp.add(stepFocuser,BorderLayout.CENTER);
    cp.add(tfEnd,BorderLayout.EAST);
    add(cp,BorderLayout.SOUTH);
  }
  
  public JTable getTable() {
    return table;
  }

  class FlightsTableModel extends AbstractTableModel {
    Vector<Flight> vf=null;
    Hashtable<String,int[]> flightsTimesInSector=null;
    int step, minStep, maxStep;
    public FlightsTableModel (Vector<Flight> vf, Hashtable<String,int[]> flightsTimesInSector, int step) {
      this.vf=vf;
      this.flightsTimesInSector=flightsTimesInSector;
      this.step=step;
      minStep=0;
      maxStep=vf.elementAt(0).delays.length-1;
    }
    public void setSteps (int min, int max) {
      minStep=min; maxStep=max;
      fireTableDataChanged();
      //System.out.println("* "+min+" "+max);
    }
    private String[] columnNames={"Flight ID","From","To","Airline","CallSign","Delay","Cumulative delays","N changes","Added delays","1st delay","last delay"},
                     extraColumnNames={"Entry","Exit","Duration"};
    public int getColumnCount() { return columnNames.length+((flightsTimesInSector==null)?0:3); }
    public int getRowCount() {
      return vf.size();
    }
    public String getColumnName(int col) {
      if (col<columnNames.length)
        return columnNames[col];
      else
        return extraColumnNames[col-columnNames.length];
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
          //setToolTipText(vf.elementAt(row).id);
          if (minStep==0 && maxStep==vf.elementAt(row).delays.length-1)
            return vf.elementAt(row).delays;
          else {
            int v[]=new int[maxStep-minStep+1];
            for (int i=0; i<v.length; i++)
              v[i]=vf.elementAt(row).delays[minStep+i];
            return v;
          }
        case 7:
          int v[]=vf.elementAt(row).delays;
          int n=0;
          for (int i=1; i<v.length; i++)
            if (v[i]>v[i-1])
              n++;
          return n;
        case 9:
          n=-1;
          Flight fl=vf.elementAt(row);
          for (int i=1; n==-1 && i<fl.delays.length; i++)
            if (fl.delays[i]>fl.delays[i-1])
              n=i;
          return (n==-1)?-1:n;
        case 10:
          n=-1;
          fl=vf.elementAt(row);
          for (int i=fl.delays.length-1; n==-1 && i>0; i--)
            if (fl.delays[i]>fl.delays[i-1])
              n=i;
          return (n==-1)?-1:n;
        case 11: case 12: case 13:
          if (flightsTimesInSector==null)
            return 0;
          String flightId=vf.elementAt(row).id;
          int times[]=flightsTimesInSector.get(flightId);
          if (times==null)
            return 0;
          return ((col==11)?times[0]:((col==12)?times[1]:times[1]-times[0]));
      }
      return vf.elementAt(row).id;
    }
  }

}
