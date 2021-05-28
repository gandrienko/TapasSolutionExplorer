package TapasSolutionExplorer.Data;

import TapasDataReader.ExTreeNode;
import TapasDataReader.ExplanationItem;
import TapasDataReader.Flight;
import TapasDataReader.Record;

import java.time.LocalTime;
import java.util.*;

/**
 * Reconstructs sequences of sector visits for different variants of flights
 */
public class FlightConstructor {
  public static final int minutesInDay=1440;
  /**
   * @param flightSteps - for the flights that were changed, the numbers of the simulation steps
   *                    corresponding to the plan changes
   * @param flightPlans - Contains records about sectors for different solution steps.
   *    The keys of the hashtable consist of sector identifiers and step numbers with underscore between them.
   * @return sector sequences for all variants of all flights
   * dimension 0: flights
   * dimension 1: flight variants
   * dimension 2: visited sectors
   */
  public static FlightInSector[][][] getFlightSectorSequences(Hashtable<String, int[]> flightSteps,
                                                            Hashtable<String, Vector<Record>> flightPlans) {
    if (flightSteps==null || flightSteps.isEmpty() || flightPlans==null || flightPlans.isEmpty())
      return null;
    Hashtable<String,ArrayList<FlightInSector>> flSectors=
        new Hashtable<String,ArrayList<FlightInSector>>(Math.round(flightSteps.size()*1.3f));
    for (Map.Entry<String,Vector<Record>> e:flightPlans.entrySet()) {
      Vector<Record> sectorRecords=e.getValue();
      for (int i=0; i<sectorRecords.size(); i++) {
        Record r=sectorRecords.elementAt(i);
        int steps[]=flightSteps.get(r.flight);
        boolean stepFound=r.step==0;
        if (steps!=null)
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
  
  /**
   * Computes hourly counts of sector entries with the given time step, in minutes.
   * @param flightPlans  - Contains records about sectors for different solution steps.
   *    The keys of the hashtable consist of sector identifiers and step numbers with underscore between them.
   * @param sectorId - the sector for which to count the entries
   * @param solutionStep - the step in solution for which to take the flight plans for counting
   * @param tStep - time step (shift) between consecutive time intervals
   * @param ignoreReEntries - whether to ingore repeated entries within the same interval
   * @return
   */
  public static int[] getHourlyCountsOfSectorEntries(Hashtable<String, Vector<Record>> flightPlans,
                                                     String sectorId, int solutionStep,
                                                     int tStep,  boolean ignoreReEntries) {
    if (flightPlans==null || sectorId==null || tStep<=0)
      return null;
    String key=sectorId+"_"+solutionStep;
    Vector<Record> sFlights=flightPlans.get(key);
    if (sFlights==null || sFlights.isEmpty())
      return null;
    int nSteps=minutesInDay/tStep;
    if (nSteps*tStep<minutesInDay)
      ++nSteps;
    int counts[]=new int[nSteps];
    for (int i=0; i<nSteps; i++)
      counts[i]=0;
    HashSet<String> repeated=null;
    for (int i=0; i<sFlights.size(); i++) {
      Record r=sFlights.elementAt(i);
      if (repeated!=null && repeated.contains(r.flight))
        continue;
      int idx1=(r.FromN-60)/tStep+1, idx2=r.FromN/tStep;
      if (idx1>=counts.length)
        continue;
      for (int j=Math.max(0,idx1); j<=idx2 && j<counts.length; j++)
        ++counts[j];
      if (ignoreReEntries) {
        HashSet<Integer> idxs=null;
        for (int j=i+1; j<sFlights.size(); j++)
          if (r.flight.equals(sFlights.elementAt(j).flight)) {
            if (repeated==null)
              repeated=new HashSet<String>(sFlights.size()/2);
            repeated.add(r.flight);
            Record r2=sFlights.elementAt(j);
            int idx1_2=(r2.FromN-60)/tStep+1, idx2_2=r2.FromN/tStep;
            if (idx1_2==idx1 || idx1_2>=counts.length)
              continue;
            if (idxs==null) {
              idxs = new HashSet<Integer>(10);
              for (int k=idx1; k<=idx2; k++)
                idxs.add(k);
            }
            for (int k=Math.max(0,idx1_2); k<=idx2_2 && k<counts.length; k++)
              if (!idxs.contains(k)) {
                ++counts[k];
                idxs.add(k);
              }
          }
      }
    }
    return counts;
  }
  
  /**
   * Computes hourly counts of flights present in the given sector with the given time step, in minutes.
   * @param flightPlans  - Contains records about sectors for different solution steps.
   *    The keys of the hashtable consist of sector identifiers and step numbers with underscore between them.
   * @param sectorId - the sector for which to count the presence
   * @param solutionStep - the step in solution for which to take the flight plans for counting
   * @param tStep - time step (shift) between consecutive time intervals
   * @return
   */
  public static int[] getHourlyFlightCounts(Hashtable<String, Vector<Record>> flightPlans,
                                            String sectorId, int solutionStep,int tStep) {
    if (flightPlans==null || sectorId==null || tStep<=0)
      return null;
    String key=sectorId+"_"+solutionStep;
    Vector<Record> sFlights=flightPlans.get(key);
    if (sFlights==null || sFlights.isEmpty())
      return null;
    int nSteps=minutesInDay/tStep;
    if (nSteps*tStep<minutesInDay)
      ++nSteps;
    int counts[]=new int[nSteps];
    for (int i=0; i<nSteps; i++)
      counts[i]=0;
    HashSet<String> repeated=null;
    for (int i=0; i<sFlights.size(); i++) {
      Record r=sFlights.elementAt(i);
      if (repeated!=null && repeated.contains(r.flight))
        continue;
      int idx1=(r.FromN-60)/tStep+1, idx2=r.ToN/tStep;
      if (idx1>=counts.length)
        continue;
      for (int j=Math.max(0,idx1); j<=idx2 && j<counts.length; j++)
        ++counts[j];
      HashSet<Integer> idxs=null;
      for (int j=i+1; j<sFlights.size(); j++)
        if (r.flight.equals(sFlights.elementAt(j).flight)) {
          if (repeated==null)
            repeated=new HashSet<String>(sFlights.size()/2);
          repeated.add(r.flight);
          Record r2=sFlights.elementAt(j);
          int idx1_2=(r2.FromN-60)/tStep+1, idx2_2=r2.ToN/tStep;
          if (idx1_2==idx1 || idx1_2>=counts.length)
            continue;
          if (idxs==null) {
            idxs = new HashSet<Integer>(10);
            for (int k=idx1; k<=idx2; k++)
              idxs.add(k);
          }
          for (int k=Math.max(0,idx1_2); k<=idx2_2 && k<counts.length; k++)
            if (!idxs.contains(k)) {
              ++counts[k];
              idxs.add(k);
            }
        }
    }
    return counts;
  }
  
  public static Hashtable<Integer, ExTreeNode> reconstructExplTreeForFlight(String flightId,
                                                                            Hashtable<String, Flight> flightData,
                                                                            TreeSet<Integer> solutionSteps) {
    if (flightId==null || flightData==null || flightData.isEmpty())
      return null;
    Flight f=flightData.get(flightId);
    if (f==null || f.expl==null || f.expl.length<1 || !flightId.equals(f.id))
      return null;
    Hashtable<Integer,ExTreeNode> topNodes=null;
    ArrayList<Integer> stepList=(solutionSteps==null || solutionSteps.isEmpty())?null:
                                    new ArrayList<Integer>(solutionSteps);
    for (int i=0; i<f.expl.length; i++)
      if (f.expl[i] != null && f.expl[i].action>0 && f.expl[i].eItems != null) {
        ExplanationItem combItems[] = f.expl[i].getExplItemsCombined(f.expl[i].eItems);
        if (combItems != null) {
          if (topNodes==null)
            topNodes=new Hashtable<Integer,ExTreeNode>(20);
          ExTreeNode currNode=topNodes.get(f.expl[i].action);
          if (currNode == null) {
            currNode = new ExTreeNode();
            topNodes.put(f.expl[i].action, currNode);
            currNode.attrName = "Action = " + f.expl[i].action;
            currNode.level=-1;
          }
          currNode.addUse();
          if (f.expl[i].step>=0)
            if (solutionSteps==null || !solutionSteps.contains(f.expl[i].step))
              currNode.addStep(f.expl[i].step);
            else
              currNode.addStep(stepList.indexOf(f.expl[i].step));
          for (int j = 0; j < combItems.length; j++) {
            ExplanationItem eIt = combItems[j];
            if (eIt == null)
              continue;
            ExTreeNode child = currNode.findChild(eIt.attr, eIt.interval);
            if (child == null) {
              child = new ExTreeNode();
              child.attrName = eIt.attr;
              child.level = currNode.level+1;
              child.condition = eIt.interval.clone();
              child.isInteger=eIt.isInteger;
              currNode.addChild(child);
            }
            child.addUse();
            currNode = child;
            if (f.expl[i].step>=0)
              if (solutionSteps==null || !solutionSteps.contains(f.expl[i].step))
                currNode.addStep(f.expl[i].step);
              else
                currNode.addStep(stepList.indexOf(f.expl[i].step));
          }
        }
      }
    return topNodes;
  }
}
