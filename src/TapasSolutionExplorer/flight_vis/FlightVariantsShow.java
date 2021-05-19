package TapasSolutionExplorer.flight_vis;

import TapasSolutionExplorer.Data.FlightInSector;

import javax.swing.*;
import java.awt.*;
import java.util.Hashtable;

public class FlightVariantsShow extends JPanel {
  /**
   * Sector sequences for all variants of all flights
   * dimension 0: flights
   * dimension 1: flight variants
   * dimension 2: visited sectors
   */
  public FlightInSector flights[][][]=null;
  /**
   * For each flight identifier contains the index of this flight
   */
  protected Hashtable<String,Integer> flightIndex=null;
  /**
   * Index of the currently shown flight
   */
  protected int shownFlightIdx=-1;
  
  public FlightVariantsShow(FlightInSector flights[][][]) {
    super();
    this.flights=flights;
    if (flights!=null) {
      flightIndex=new Hashtable<String,Integer>(Math.round(flights.length*1.3f));
      for (int i=0; i<flights.length; i++)
        if (flights[i]!=null && flights[i][0]!=null)
          flightIndex.put(flights[i][0][0].flightId,i);
    }
    Dimension size=Toolkit.getDefaultToolkit().getScreenSize();
    setPreferredSize(new Dimension(Math.round(0.7f*size.width), Math.round(0.8f*size.height)));
  }
  
  public boolean showFlightVariants(String flId) {
    if (flId==null || flightIndex==null)
      return  false;
    Integer idx=flightIndex.get(flId);
    if (idx==null)
      return false;
    shownFlightIdx=idx;
    System.out.println("Show flight #"+shownFlightIdx+", id="+flId+", "+flights[shownFlightIdx].length+" variants");
    return true;
  }
}
