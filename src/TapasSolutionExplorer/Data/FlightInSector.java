package TapasSolutionExplorer.Data;

import java.time.LocalTime;

public class FlightInSector implements Comparable<FlightInSector>{
  /**
   * Sector identifier
   */
  public String sectorId=null;
  /**
   * Flight identifier
   */
  public String flightId=null;
  /**
   * Solution (simulation) step
   */
  public int step=0;
  /**
   * Times of entering and exiting the sector
   */
  public LocalTime entryTime =null, exitTime=null;
  /**
   * Whether the times are in the next day
   */
  public boolean entryNextDay=false, exitNextDay=false;
  /**
   * Minutes of the day of the entry and exit
   */
  public int entryMinute=0, exitMinute=0;
  /**
   * Delay, in minutes
   */
  public int delay=0;
  /**
   * Maximal hourly demand in the sector during the time when this flight variant crosses it.
   */
  public int maxHourlyDemand=0;
  /**
   * Maximal hourly demand in the sector in the previous step of the simulation
   * during the time when this flight variant crosses it.
   */
  public int maxHourlyDemandPrevStep =0;
  /**
   * Identifiers of the previous and next sectors
   */
  public String prevSectorId=null, nextSectorId=null;
  
  public boolean equals (Object obj) {
    if (obj==null)
      return false;
    if (!(obj instanceof FlightInSector))
      return false;
    if (this==obj)
      return true;
    FlightInSector f=(FlightInSector)obj;
    if (this.entryNextDay !=f.entryNextDay) return false;
    if (this.exitNextDay !=f.exitNextDay) return false;
    if (!this.flightId.equals(f.flightId)) return false;
    if (!this.sectorId.equals(f.sectorId)) return false;
    if (!this.entryTime.equals(f.entryTime)) return false;
    if (!this.exitTime.equals(f.exitTime)) return false;
    return true;
  }
  
  @Override
  public int compareTo(FlightInSector f2) {
    if (this==null || this.entryTime==null)
      return (f2==null || f2.entryTime==null)?0:1;
    if (f2==null || f2.entryTime==null)
      return -1;
    if (this.entryNextDay!=f2.entryNextDay)
      return (this.entryNextDay)?1:-1;
    int c=this.entryTime.compareTo(f2.entryTime);
    if (c!=0)
      return c;
    if (this.exitNextDay!=f2.exitNextDay)
      return (this.exitNextDay)?1:-1;
    c=this.exitTime.compareTo(f2.exitTime);
    if (c!=0)
      return c;
    return this.flightId.compareTo(f2.flightId);
  }
  
  public FlightInSector makeCopy() {
    FlightInSector f=new FlightInSector();
    f.flightId=flightId;
    f.sectorId=sectorId;
    f.entryTime=entryTime;
    f.entryNextDay=entryNextDay;
    f.exitTime=exitTime;
    f.exitNextDay=exitNextDay;
    f.delay=delay;
    f.prevSectorId=prevSectorId;
    f.nextSectorId=nextSectorId;
    return f;
  }
  
  public static boolean doesCrossSector(String sectorId, FlightInSector sectorSequence[]) {
    if (sectorId==null || sectorSequence==null)
      return false;
    for (int i=0; i<sectorSequence.length; i++)
      if (sectorSequence[i].sectorId.equals(sectorId))
        return true;
    return false;
  }
}
