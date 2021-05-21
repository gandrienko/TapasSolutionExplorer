package TapasSolutionExplorer.flight_vis;

import TapasDataReader.Flight;
import TapasDataReader.Record;
import TapasSolutionExplorer.Data.FlightConstructor;
import TapasSolutionExplorer.Data.FlightInSector;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

public class FlightViewManager {
  /**
   * Information about flights in all simulation steps: crossed sectors, times, delays
   */
  protected Hashtable<String, Flight> flights=null;
  /**
   * Whether to show only flights that have at least 2 variants.
   */
  public boolean includeOnlyModifiedFlights=true;
  /**
   * All versions of flight plans
   */
  protected Hashtable<String, Vector<Record>> records=null;
  /**
   * Reconstructed sector sequences for all distinct versions of the flights
   */
  protected FlightInSector flightVariants[][][]=null;
  /**
   * Contains flight visualization. Created once when needed, after that just changes
   * what flight is shown.
   */
  public FlightVisPanel flShow=null;
  public JFrame showFrame=null;
  
  public FlightViewManager(Hashtable<String, Flight> flights,
                           Hashtable<String, Vector<Record>> records) {
    this.flights=flights;
    this.records=records;
  }
  
  public void setIncludeOnlyModifiedFlights(boolean includeOnlyModifiedFlights) {
    this.includeOnlyModifiedFlights = includeOnlyModifiedFlights;
  }
  
  public boolean showFlightVariants(String flId) {
    if (flId==null || flights==null || records==null)
      return false;
    if (flShow!=null)
      return flShow.showFlightVariants(flId);
    
    // For the flights that were changed, the numbers of the simulation steps corresponding to the plan changes
    // If includeOnlyModifiedFlights is false, creates also hashtable entries for the remaining flights.
    // The values in these entries are zero
    Hashtable<String, int[]> flightSteps=new Hashtable<String, int[]>(flights.size());
    for (Flight fl:flights.values()) {
      boolean wasDelayed=fl.delays!=null && fl.delays.length>1 &&
                             fl.delays[fl.delays.length-1]>0;
      if (!wasDelayed) {//this flight was never delayed
        if (!includeOnlyModifiedFlights)
          flightSteps.put(fl.id,null);
        continue;
      }
      ArrayList<Integer> fStepList=new ArrayList<Integer>(20);
      int iPrev=0;
      for (int i=1; i<fl.delays.length; i++)
        if (fl.delays[i]>fl.delays[iPrev]) {
          //store the step in which the delay was increased
          fStepList.add(i);
          iPrev=i;
        }
      if (!fStepList.isEmpty()) {
        int fSteps[]=new int[fStepList.size()];
        for (int i=0; i<fStepList.size(); i++)
          fSteps[i]=fStepList.get(i);
        flightSteps.put(fl.id,fSteps);
      }
      else
        if (!includeOnlyModifiedFlights)
          flightSteps.put(fl.id,null);
    }
    if (flightSteps.isEmpty()) {
      System.out.println("Failed to get steps of change for any flight!");
      return false;
    }
    System.out.println("Got the steps of changes for "+flightSteps.size()+" flights!");
  
    FlightInSector flightVariants[][][]= FlightConstructor.getFlightSectorSequences(flightSteps,records);
    if (flightVariants==null) {
      System.out.println("Failed to get flight plan variants!");
      return false;
    }
    System.out.println("Got the flight plan variants for "+flightVariants.length+" flights!");
    
    flShow=new FlightVisPanel(flightVariants);
    Dimension size=Toolkit.getDefaultToolkit().getScreenSize();
  
    showFrame=new JFrame("Flight variants");
    showFrame.getContentPane().add(flShow, BorderLayout.CENTER);
    showFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    showFrame.pack();
    showFrame.setLocation(size.width-showFrame.getWidth()-30,size.height-showFrame.getHeight()-50);
    showFrame.setVisible(true);
    
    return flShow.showFlightVariants(flId);
  }
}
