package TapasSolutionExplorer.Vis;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

public class FlightVariantsTableModel extends AbstractTableModel {
  public String colNames[]={"Flight ID","N of changes","Steps of changes"};
  /**
   * for the flights that were changed, the numbers of the simulation steps corresponding to the plan changes
   */
  public Hashtable<String, int[]> flightSteps=null;
  /**
   * the identifiers of the flights sorted by the counts of their plan changes in the descending order
   */
  public String flightIds[]=null;
  /**
   * For each flight, a string with a list of the steps
   */
  public String stepLists[]=null;
  
  public int maxNSteps=0;
  
  /**
   * @param flightSteps - for the flights that were changed, the numbers of the simulation steps
   *                    corresponding to the plan changes
   */
  public FlightVariantsTableModel(Hashtable<String, int[]> flightSteps) {
    this.flightSteps=flightSteps;
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
    }
    maxNSteps=flightSteps.get(flightIdsSorted.get(0)).length;
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
  
  public Object getValueAt(int row, int col) {
    if (flightSteps==null)
      return null;
    if (col==0)
      return flightIds[row];
    if (col==2)
      return stepLists[row];
    if (col==1)
      return flightSteps.get(flightIds[row]).length;
    return null;
  }
  
}
