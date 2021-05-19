package TapasSolutionExplorer.Data;

import TapasDataReader.Record;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

/**
 * Reconstructs sequences of sector visits for different variants of flights
 */
public class FlightConstructor {
  /**
   * @param flightSteps - for the flights that were changed, the numbers of the simulation steps
   *                    corresponding to the plan changes
   * @param records - Contains records about sectors for different solution steps.
   *    The keys of the hashtable consist of sector identifiers and step numbers with underscore between them.
   * @return sector sequences for all variants of all flights
   * dimension 0: flights
   * dimension 1: flight variants
   * dimension 2: visited sectors
   */
  public static FlightInSector[][][] getFlightSectorSequences(Hashtable<String, int[]> flightSteps,
                                                            Hashtable<String, Vector<Record>> records) {
    if (flightSteps==null || flightSteps.isEmpty() || records==null || records.isEmpty())
      return null;
    Hashtable<String,ArrayList<FlightInSector>> flSectors=
        new Hashtable<String,ArrayList<FlightInSector>>(Math.round(flightSteps.size()*1.3f));
    for (Map.Entry<String,Vector<Record>> e:records.entrySet()) {
      Vector<Record> sectorRecords=e.getValue();
      for (int i=0; i<sectorRecords.size(); i++) {
        Record r=sectorRecords.elementAt(i);
        int steps[]=flightSteps.get(r.flight);
        if (steps==null)
          continue;
        boolean stepFound=r.step==0;
        for (int j=0; j<steps.length && !stepFound; j++)
          stepFound=steps[j]==r.step;
        if (!stepFound)
          continue;
        String key=r.flight+"_"+r.step;
        ArrayList<FlightInSector> flSeq=flSectors.get(key);
        if (flSeq==null) {
          flSeq=new ArrayList<FlightInSector>(25);
          flSectors.put(key,flSeq);
        }
        int idx=-1;
        for (int j=0; j<flSeq.size() && idx<0; j++)
          if (r.ToN<=flSeq.get(j).entryMinute)
            idx=j;
        if (idx<0)
          flSeq.add(getSectorVisitFromRecord(r));
        else
          flSeq.add(idx,getSectorVisitFromRecord(r));
      }
    }
    int idx=0;
    FlightInSector flights[][][]=new FlightInSector[flightSteps.size()][][];
    for (Map.Entry<String,int[]> e:flightSteps.entrySet()) {
      String flId=e.getKey();
      int steps[]=e.getValue();
      if (steps==null) {
        flights[idx++]=null;
        continue;
      }
      String key=flId+"_"+0;
      ArrayList<FlightInSector> flSeq=flSectors.get(key);
      if (flSeq==null) {
        flights[idx++]=null;
        continue;
      }
      flights[idx]=new FlightInSector[steps.length+1][];
      flights[idx][0]=flSeq.toArray(new FlightInSector[flSeq.size()]);
      for (int i=0; i<steps.length; i++) {
        key=flId+"_"+steps[i];
        flSeq=flSectors.get(key);
        if (flSeq==null)
          flights[idx][i+1]=null;
        else
          flights[idx][i+1]=flSeq.toArray(new FlightInSector[flSeq.size()]);
      }
      ++idx;
    }
    return flights;
  }
  
  public static FlightInSector getSectorVisitFromRecord(Record r) {
    if (r==null)
      return null;
    FlightInSector f=new FlightInSector();
    f.flightId=r.flight;
    f.sectorId=r.sector;
    f.step=r.step;
    f.delay=r.delay;
    f.prevSectorId=r.FromS;
    f.nextSectorId=r.ToS;
    f.entryMinute=r.FromN;
    f.exitMinute=r.ToN;
    if (r.FromT!=null)
      f.entryTime= LocalTime.parse(r.FromT);
    if (r.ToT!=null)
      f.exitTime=LocalTime.parse(r.ToT);
    return f;
  }
}
