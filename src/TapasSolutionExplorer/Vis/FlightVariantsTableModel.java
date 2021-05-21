package TapasSolutionExplorer.Vis;

import TapasDataReader.Flight;
import TapasUtilities.RenderLabelBarChart;
import TapasUtilities.RenderLabelTimeBars;
import TapasUtilities.RenderLabelTimeLine;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

public class FlightVariantsTableModel extends AbstractTableModel {
  public static final int Type_String=0, Type_String_Centered=1, Type_Count=2,
      Type_Time_Line=3, Type_Time_Bars=4;
  //public String colNames[]={"Flight ID","N of changes","Steps of changes"};
  public String colNames[]={"Flight ID","N of changes","Steps of changes",
      "Final delay","Added delays","Cumulative delays",
      "Airline","CallSign","Origin","Destination"};
  public int colTypes[]={Type_String_Centered,Type_Count,Type_String,
                Type_Count, Type_Time_Bars, Type_Time_Line,
                Type_String_Centered,Type_String_Centered,Type_String_Centered,Type_String_Centered};
  /**
   * for the flights that were changed, the numbers of the simulation steps corresponding to the plan changes
   */
  public Hashtable<String, int[]> flightSteps=null;
  /**
   * Contains information about the delays of the flights at each step
   */
  protected Hashtable<String, Flight> flights=null;
  /**
   * the identifiers of the flights sorted by the counts of their plan changes in the descending order
   */
  public String flightIds[]=null;
  /**
   * For each flight, a string with a list of the steps
   */
  public String stepLists[]=null;
  
  public int maxNChanges =0;
  public int maxStepDelay=0, maxFinalDelay=0;
  /**
   * Focuser for the steps
   */
  public int maxStep=0, minStepToShow =0, maxStepToShow =0;
  
  /**
   * @param flightSteps - for the flights that were changed, the numbers of the simulation steps
   *                    corresponding to the plan changes
   */
  public FlightVariantsTableModel(Hashtable<String, int[]> flightSteps, Hashtable<String, Flight> flights) {
    this.flightSteps=flightSteps;
    this.flights=flights;
    if (flightSteps==null || flightSteps.isEmpty())
      return;
    ArrayList<String> flightIdsSorted=new ArrayList<String>(flightSteps.size());
    for (Map.Entry<String,int[]> e:flightSteps.entrySet()) {
      int nChanges=e.getValue().length;
      int fIdx=-1;
      if (!flightIdsSorted.isEmpty())
        for (int i=0; i<flightIdsSorted.size() && fIdx<0; i++)
          if (nChanges>flightSteps.get(flightIdsSorted.get(i)).length)
            fIdx=i;
      if (fIdx<0)
        flightIdsSorted.add(e.getKey());
      else
        flightIdsSorted.add(fIdx,e.getKey());
    }
    flightIds=new String[flightIdsSorted.size()];
    flightIds=flightIdsSorted.toArray(flightIds);
    stepLists=new String[flightIds.length];
    for (int i=0; i<flightIds.length; i++) {
      int steps[]=flightSteps.get(flightIds[i]);
      stepLists[i]=String.valueOf(steps[0]);
      for (int j=1; j<steps.length; j++)
        stepLists[i]+=", "+String.valueOf(steps[j]);
      Flight fl=flights.get(flightIds[i]);
      if (fl!=null && fl.delays!=null && fl.delays.length>0 && fl.delays[fl.delays.length-1]>0) {
        if (maxStep <fl.delays.length-1)
          maxStep =fl.delays.length-1;
        if (maxFinalDelay<fl.delays[fl.delays.length-1])
          maxFinalDelay=fl.delays[fl.delays.length-1];
        for (int j=1; j<fl.delays.length; j++)
          if (maxStepDelay<fl.delays[j]-fl.delays[j-1])
            maxStepDelay=fl.delays[j]-fl.delays[j-1];
      }
    }
    maxNChanges =flightSteps.get(flightIdsSorted.get(0)).length;
    maxStepToShow=maxStep;
  }
  
  public void setStepsToShow (int min, int max) {
    if (min==minStepToShow && max==maxStepToShow)
      return;
    minStepToShow =min; maxStepToShow =max;
    fireTableDataChanged();
  }
  
  public int getColumnCount() {
    if (colNames==null)
      return 0;
    return colNames.length;
  }
  
  public int getRowCount() {
    if (flightIds==null)
      return 0;
    return flightIds.length;
  }
  
  public String getColumnName(int col) {
    if (colNames!=null)
      return colNames[col];
    return null;
  }
  
  public Class getColumnClass(int c) {
    return getValueAt(0, c).getClass();
  }
  
  public TableCellRenderer getRendererForColumn(int col) {
    switch (colTypes[col]) {
      case Type_String: case Type_String_Centered:
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
        if (colTypes[col]==Type_String_Centered)
          cellRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        return cellRenderer;
      case Type_Count:
        int max=0;
        if (colNames[col].equalsIgnoreCase("N of changes"))
          max= maxNChanges;
        else
        if (colNames[col].equalsIgnoreCase("Final delay"))
          max=maxFinalDelay;
        else
          return null;
        return new RenderLabelBarChart(0, max);
      case Type_Time_Line:
        return new RenderLabelTimeLine(maxFinalDelay);
      case Type_Time_Bars:
        return new RenderLabelTimeBars(maxStepDelay);
    }
    return null;
  }
  
  public Object getValueAt(int row, int col) {
    if (flightSteps==null)
      return null;
    if (col==0)
      return flightIds[row];
    if (colNames[col].equalsIgnoreCase("Steps of changes"))
      return stepLists[row];
    if (colNames[col].equalsIgnoreCase("N of changes"))
      return flightSteps.get(flightIds[row]).length;
    if (colNames[col].equalsIgnoreCase("Final delay")) {
      Flight fl=flights.get(flightIds[row]);
      if (fl!=null && fl.delays!=null && fl.delays.length>0)
        return fl.delays[fl.delays.length-1];
      return 0;
    }
    if (colNames[col].equalsIgnoreCase("Cumulative delays") ||
            colNames[col].equalsIgnoreCase("Added delays")) {
      Flight fl=flights.get(flightIds[row]);
      if (fl==null)
      return null;
      if (minStepToShow==0 && maxStepToShow>=maxStep)
        return fl.delays;
      int v[]=new int[maxStepToShow-minStepToShow+1];
      for (int i=0; i<v.length; i++)
        v[i]=fl.delays[minStepToShow+i];
      return v;
    }
    
    //extract column value from the flight identifier
    String t[]=flightIds[row].split("-");
    if (colNames[col].equalsIgnoreCase("Origin"))
      return t[0];
    if (colNames[col].equalsIgnoreCase("Destination"))
      return t[1];
    if (colNames[col].equalsIgnoreCase("CallSign"))
      return t[2];
    if (colNames[col].equalsIgnoreCase("Airline")) {
      String s=t[2];
      int idxDigit=-1;
      for (int i=0; idxDigit==-1 && i<s.length(); i++)
        if (Character.isDigit(s.charAt(i)))
          idxDigit=i;
      return (idxDigit==-1) ? s : s.substring(0,idxDigit);
    }
    return null;
  }
  
}
