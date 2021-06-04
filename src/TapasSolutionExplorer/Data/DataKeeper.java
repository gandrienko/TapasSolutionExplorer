package TapasSolutionExplorer.Data;

import TapasDataReader.Flight;
import TapasDataReader.Record;
import TapasSolutionExplorer.flight_vis.FlightViewManager;

import javax.swing.*;
import java.io.*;
import java.util.*;

public class DataKeeper {

  //protected HashSet<String> flights=new HashSet(1000);
  protected HashSet<String> sectors=new HashSet(50);
  public HashSet getSectors() { return sectors; }
  public Vector<SectorData> sectorsWithData=new Vector<>(50);
  Vector<String> sectorsSorted=null;

  public Vector<String> getSectorsSorted() {
    return sectorsSorted;
  }

  public String stepLabels[]=null;

  protected int hotspotMode=0,  // 0: by entries, 1: by presence
                hotspotRatio=0; // 0: ratio=1.1; 1: ratio=0;
  public boolean bHideSectorsWithUndefinedCapacity=true;

  public String sortMode="";
  public void sortSectors (String mode) {
    sortSectors(mode,null);
  }
  public void sortSectors (String mode, HashSet<String> selection) {
    sortMode=mode;
    for (SectorData sd:sectorsWithData)
      sd.compMode=mode;
    Collections.sort(sectorsWithData);
    sectorsSorted=new Vector<String>(sectors.size());
    for (SectorData sd:sectorsWithData)
      if ((!bHideSectorsWithUndefinedCapacity || capacities.get(sd.sector)<999) && (selection==null || selection.size()==0 || selection.contains(sd.sector))) {
        if (mode.equals(SectorData.comparisonMode[0]))
          sectorsSorted.add(sd.sector);
        else
          sectorsSorted.add(0, sd.sector);
      }
    //System.out.println("Sorted sectors: "+sectorsSorted);
  }

  protected Hashtable<String,Vector<Record>> records=new Hashtable(100000);

  public Hashtable<String,Integer> capacities=new Hashtable(100);

  public int maxStep=0, Nsteps=0;

  protected void readData(String fname) {
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(fname+".csv")))) ;
      String strLine;
      HashSet<String> flights=new HashSet(1000);

      int n=0;
      try {
        br.readLine();
        while ((strLine = br.readLine()) != null) {
          n++;
          String str=strLine.replaceAll("\"","").replaceAll(" ","");
          String[] tokens=str.split(",");
          String flight=tokens[0], sector=tokens[3];
          flights.add(flight);
          sectors.add(sector);
          String key=sector+"_"+tokens[1];
          int step=Integer.valueOf(tokens[1]).intValue();
          if (step>maxStep)
            maxStep=step;
          Vector<Record> vr=records.get(key);

          Record record=new Record();
          record.flight=flight;
          record.delay=Integer.valueOf(tokens[2]).intValue();
          record.FromT=tokens[4];
          record.ToT=tokens[5];
          record.FromN=Integer.valueOf(tokens[6]).intValue();
          record.ToN=Integer.valueOf(tokens[7]).intValue();
          record.FromS=tokens[8];
          record.ToS=(tokens.length==10)?tokens[9]:"";
          if (vr==null) {
            vr=new Vector<Record>(100);
            records.put(key,vr);
          }
          vr.add(record);

          //records.put(sector+"_"+tokens[1],record);
          if (n%100000==0)
            System.out.println("lines processed: "+n+", sectors: "+sectors.size()+", flights: "+flights.size()+", records="+records.size());
        }
        br.close();
        Nsteps=maxStep+1;
      } catch (IOException io) {}
      System.out.println("lines processed: "+n+", sectors: "+sectors.size()+", flights: "+flights.size()+", records="+records.size());
    } catch (FileNotFoundException ex) { System.out.println("problem reading file "+fname+" : "+ex); }
  }

  public Vector<Record> getRecordsForStep (int step) {
    Vector<Record> vr=new Vector(100000,100000);
    String stepAsStr=""+step;
    Set<String> keys=records.keySet();
    for (String key:keys) {
      String tokens[]=key.split("_");
      if (tokens!=null && tokens.length>1 && stepAsStr.equals(tokens[1]))
        for (Record r:records.get(key))
          vr.add(r);
    }
    return vr;
  }

  protected void readCapacities(String fname) {
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(fname+".csv")))) ;
      String strLine;
      try {
        br.readLine();
        while ((strLine = br.readLine()) != null) {
          String str=strLine.replaceAll(" ","");
          String[] tokens=str.split(",");
          String s=tokens[0];
          Integer capacity=Integer.valueOf(tokens[1]);
          capacities.put(s,capacity);
        }
        br.close();
      } catch  (IOException io) {}
    } catch (FileNotFoundException ex) { System.out.println("problem reading file "+fname+" : "+ex); }
  }

  public int Nintervals=70, NintevalsWithHotspots=0, Ishift=20, Iduration=60;
  protected Hashtable<String,Vector<Record>[][]> recsInCellsAll=null;
  //protected Vector<Record> recsInCells[][]=null;
  public boolean[] hasHotspots=null;
  public int iGlobalMax=0, iLocalMax=0; // Max count for all steps Vs. currently dispayed steps

  public void aggregateAll() {
    recsInCellsAll=new Hashtable<>();
    sectorsWithData=new Vector<>(sectors.size());
    iGlobalMax=0;
    int nSectorsProcessed=0;
    System.out.println("* Computing aggregates for "+sectors.size()+" sectors");
    for (String sector:sectors) {
      aggregate(sector,false);
      float capacity=capacities.get(sector);
      if (hotspotRatio==0)
        capacity=1.1f*capacity;
      SectorData sd=new SectorData();
      sd.sector=sector;
      //sd.Nhotspots_all=0;
      HashSet<String> flights=new HashSet<>(100);
      Vector<Record> recsInCells[][]=recsInCellsAll.get(sector);
      for (int i=0; i<recsInCells.length; i++)
        for (int j=0; j<recsInCells[i].length; j++) {
          if (recsInCells[i][j].size()>capacity)
            sd.Nhotspots_all++;
          if (j==0 && recsInCells[i][j].size()>capacity)
            sd.Nhotspots_step0++;
          if (j==recsInCells[i].length-1 && recsInCells[i][j].size()>capacity)
            sd.Nhotspots_stepLast++;
          for (int k = 0; k < recsInCells[i][j].size(); k++) {
            Record rec = recsInCells[i][j].elementAt(k);
            flights.add(rec.flight);
          }
        }
      sd.Nflights=flights.size();
      sectorsWithData.add(sd);
      int n=getMax(getCounts(sector,"CountFlights"));
      if (n>iGlobalMax)
        iGlobalMax=n;
      //System.out.println("Sector="+sector+", max_count="+n);
      nSectorsProcessed++;
      if (nSectorsProcessed%10==0)
        System.out.println("* "+nSectorsProcessed+" sectors ready");
    }
    //System.out.println("* global max="+iGlobalMax);
    sortSectors(SectorData.comparisonMode[2]);
    System.out.println("* Computing aggregates for "+sectors.size()+" sectors ... ready");
  }

  public int stepsInfo[][]=null; // 0: nHotspots; 1: n sectors with hotspots ; 2: total delay; 3..8: nFlightsDelay0,1_4,5_9,10-29,30_59,over60;
  public void calcFeaturesOfSteps() {
    System.out.println("* Computing aggregates for "+Nsteps+" steps");
    if (stepsInfo==null)
      stepsInfo=new int[Nsteps][];
    for (int step=0; step<Nsteps; step++) {
      if (stepsInfo[step]==null)
        stepsInfo[step] = new int[9];
      for (int i=0; i<stepsInfo[step].length; i++)
        stepsInfo[step][i]=0;
      Hashtable<String,Integer> allFlightsAtStep=new Hashtable<>(500);
      for (String sector:sectors) {
        Vector<Record> vr=records.get(sector+"_"+step);
        boolean bSectorHasHotspot=false;
        for (int interval=0; interval<Nintervals; interval++) {
          float capacity=capacities.get(sector);
          if (hotspotRatio==0)
            capacity=1.1f*capacity;
          if (getCount(sector, "CountFlights", interval, step) > capacity) {
            stepsInfo[step][0] += 1;
            bSectorHasHotspot = true;
          }
        }
        if (bSectorHasHotspot)
          stepsInfo[step][1]+=1;
        for (Record r:vr)
          if (!allFlightsAtStep.containsKey(r.flight))
            allFlightsAtStep.put(r.flight,new Integer(r.delay));
      }
      for (String flight:allFlightsAtStep.keySet()) {
        int delay=allFlightsAtStep.get(flight);
        stepsInfo[step][2]+=delay;
        if (delay==0)
          stepsInfo[step][3]++;
        else
        if (delay>=1 && delay<=4)
          stepsInfo[step][4]++;
        else
        if (delay>=5 && delay<=9)
          stepsInfo[step][5]++;
        else
        if (delay>=10 && delay<=29)
          stepsInfo[step][6]++;
        else
        if (delay>=30 && delay<=59)
          stepsInfo[step][7]++;
        else
          stepsInfo[step][8]++;
      }
      if (step>0 && step%10==0)
        System.out.println("* step "+step+" ... ready");
    }
    System.out.println("* Computing aggregates for "+Nsteps+" steps ... ready");

    //for (int i=0; i<Nsteps; i++)
      //System.out.println("step="+i+stepsInfo[i]);

  }

  public void aggregate (String sector) {
    aggregate(sector,true);
  }

  public void aggregate (String sector, boolean toComputeHostops) {
    Vector<Record> recsInCells[][]=recsInCellsAll.get(sector);
    //if (recsInCellsAll!=null)
      //recsInCells=recsInCellsAll.get(sector);
    if (recsInCells==null) {
      recsInCells = new Vector[Nintervals][];
      recsInCellsAll.put(sector,recsInCells);
      for (int i = 0; i < recsInCells.length; i++) {
        recsInCells[i] = new Vector[Nsteps];
        for (int j = 0; j < recsInCells[i].length; j++)
          recsInCells[i][j] = new Vector<Record>(10, 10);
      }
      for (int i = 0; i < Nsteps; i++) {
        String key = sector + "_" + i;
        Vector<Record> vr = records.get(key);
        if (vr==null)
          System.out.println("* panic: key="+key);
        for (int j = 0; j < vr.size(); j++) {
          Record r = vr.get(j);
          for (int k = 0; k < Nintervals; k++) {
            int k1 = k * Ishift, k2 = k1 + Iduration - 1;
            boolean intersect = (hotspotMode<2) ? r.FromN>=k1 && r.FromN<=k2 : Math.max(k1, r.FromN) <= Math.min(k2, r.ToN);
            if (intersect)
              if (hotspotMode==1)
                recsInCells[k][i].add(r);
              else {
                boolean found=false;
                for (int n=0; !found && n<recsInCells[k][i].size(); n++)
                  found=r.flight.equals(recsInCells[k][i].elementAt(n).flight);
                if (!found)
                  recsInCells[k][i].add(r);
              }
          }
        }
      }
    }
    hasHotspots=new boolean[Nintervals];
    float capacity=capacities.get(sector).intValue();
    if (hotspotRatio==0)
      capacity=1.1f*capacity;
    for (int k=0; k<Nintervals; k++) {
      hasHotspots[k]=false;
      for (int i=0; i<Nsteps && !hasHotspots[k]; i++)
        hasHotspots[k]=recsInCells[k][i].size()>capacity;
    }
    NintevalsWithHotspots=0;
    for (int k=0; k<hasHotspots.length; k++)
      if (hasHotspots[k])
        NintevalsWithHotspots++;
  }

/*
  public Vector<int[]> checkEqual () { // for the currently selelted sector
    Vector<int[]> list=new Vector(10,10);
    boolean blist[]=new boolean[Nsteps];
    for (int step=1; step<this.Nsteps; step++) {
      boolean equal = true;
      for (int i = 0; i < this.Nintervals && equal; i++) {
        equal = recsInCells[i][step].size()==recsInCells[i][step - 1].size();
        if (equal)
          for (int k=0; k<recsInCells[i][step].size() && equal; k++)
            equal=recsInCells[i][step].elementAt(k).flight.equals(recsInCells[i][step-1].elementAt(k).flight) &&
                  recsInCells[i][step].elementAt(k).delay==recsInCells[i][step-1].elementAt(k).delay;
      }
      blist[step]=equal;
      //if (step==1000)
        //System.out.println("* step " + step + " is equal to " + (step - 1));
      //if (equal)
        //System.out.println("* " + step + " is equal to " + (step - 1));
      //else
        //System.out.println("* " + step + " is NOT equal to " + (step - 1));
    }
    int N=10;
    for (int step=1; step<Nsteps-N; step++)
      if (blist[step]) {
        // count N of equal steps
        int end=step;
        for (int i=step+1; step<Nsteps-1 && (i<blist.length && blist[i]); i++)
          end=i;
        if (end-step>=N-1) {
          list.addElement(new int[]{step-1,end});
          System.out.println("equal steps:" + (step - 1) + ".." + end);
        }
        step=end+1;
      }
    return list;
  }
*/

  public int getCountHotspots (String sector, int step) {
    int N=0;
    Vector<Record> recsInCells[][]=recsInCellsAll.get(sector);
    float capacity=capacities.get(sector);
    if (hotspotRatio==0)
      capacity=1.1f*capacity;
    for (int interval=0; interval<Nintervals; interval++)
      if (recsInCells[interval][step].size()>capacity)
        N++;
    return N;
  }

  public String getListOfFlightsAsText (String sector, int step, int interval) {
    String s="";
    Vector<Record> recsInCells[][]=recsInCellsAll.get(sector);
    Vector<Record> vr=recsInCells[interval][step];
    for (Record r:vr)
      s+=r.sector+","+r.step+","+r.flight+","+r.delay+","+r.FromS+","+r.FromT+","+r.FromN+","+r.ToS+","+r.ToT+","+r.ToN+"\n";
    return s;
  }

  public Vector<Flight> getFlights() {
    if (allFlights==null)
      return new Vector<>();
    Vector<Flight> vf=new Vector<>(allFlights.size());
    for (String s:allFlights.keySet())
      vf.add(allFlights.get(s));
    return vf;
  }

  public Vector<Flight> getFlights (String sector, int step, Hashtable<String,int[]> flightsTimesInSector) {
    if (allFlights==null)
      return null;
    Hashtable<String,Flight> fl=new Hashtable<>(100);
    Vector<Record> recsInCells[][]=recsInCellsAll.get(sector);
    for (int interval=0; interval<Nintervals; interval++)
      for (Record r:recsInCells[interval][step])
        if (!fl.containsKey(r.flight)) {
          fl.put(r.flight,allFlights.get(r.flight));
          int t[]=new int[2];
          t[0]=r.FromN;
          t[1]=r.ToN;
          flightsTimesInSector.put(r.flight,t);
        }
    Vector<Flight> vf=new Vector<>(fl.size());
    for (String s:fl.keySet())
      vf.add(fl.get(s));
    return vf;
  }

  public Vector<Flight> getFlights (String sector, int interval, int step, Hashtable<String,int[]> flightsTimesInSector) {
    if (allFlights==null)
      return null;
    Vector<Record> recsInCells[][]=recsInCellsAll.get(sector);
    Vector<Flight> vf=new Vector<>(recsInCells[interval][step].size());
    //flightsTimesInSector=new Hashtable<>(recsInCells[interval][step].size());
    for (int i=0; i<recsInCells[interval][step].size(); i++) {
      vf.add(allFlights.get(recsInCells[interval][step].elementAt(i).flight));
      int t[]=new int[2];
      Record r=recsInCells[interval][step].elementAt(i);
      t[0]=r.FromN;
      t[1]=r.ToN;
      flightsTimesInSector.put(r.flight,t);
    }
    return vf;
  }

  public int getCount (String sector, String operation, int step) {
    int N=0;
    Hashtable<String,Integer> flights=new Hashtable<>(100);
    Vector<Record> recsInCells[][]=recsInCellsAll.get(sector);
    for (int interval=0; interval<Nintervals; interval++)
      for (Record r:recsInCells[interval][step])
        if (!flights.containsKey(r.flight))
          flights.put(r.flight,new Integer(r.delay));
    if ("CountFlights".equals(operation))
      N=flights.size();
    if ("CountFlights-noDelay".equals(operation))
      for (String flight:flights.keySet())
        if (flights.get(flight).intValue()==0)
          N++;
    if ("CountFlights-Delay1to4".equals(operation))
      for (String flight:flights.keySet())
        if (flights.get(flight).intValue()>0 && flights.get(flight).intValue()<5)
          N++;
    if ("CountFlights-Delay5to9".equals(operation))
      for (String flight:flights.keySet())
        if (flights.get(flight).intValue()>=5 && flights.get(flight).intValue()<=9)
          N++;
    if ("CountFlights-Delay10to29".equals(operation))
      for (String flight:flights.keySet())
        if (flights.get(flight).intValue()>=10 && flights.get(flight).intValue()<=29)
          N++;
    if ("CountFlights-Delay30to59".equals(operation))
      for (String flight:flights.keySet())
        if (flights.get(flight).intValue()>=30 && flights.get(flight).intValue()<=59)
          N++;
    if ("CountFlights-DelayOver60".equals(operation))
      for (String flight:flights.keySet())
        if (flights.get(flight).intValue()>=60)
          N++;

    return N;
  }

  public int getCount (String sector, String operation, int interval, int step) {
    Vector<Record> recsInCells[][]=recsInCellsAll.get(sector);
    int N=-1;
    if ("CountFlights".equals(operation))
      N=recsInCells[interval][step].size();
    if ("CountFlights-noDelay".equals(operation)) {
      N=0;
      for (int k=0; k<recsInCells[interval][step].size(); k++)
        if (recsInCells[interval][step].elementAt(k).delay==0)
          N++;
    }
    if ("CountFlights-Delay1to4".equals(operation)) {
      N=0;
      for (int k=0; k<recsInCells[interval][step].size(); k++)
        if (recsInCells[interval][step].elementAt(k).delay>0 && recsInCells[interval][step].elementAt(k).delay<5)
          N++;
    }
    if ("CountFlights-Delay5to9".equals(operation)) {
      N=0;
      for (int k=0; k<recsInCells[interval][step].size(); k++)
        if (recsInCells[interval][step].elementAt(k).delay>=5 && recsInCells[interval][step].elementAt(k).delay<=9)
          N++;
    }
    if ("CountFlights-Delay10to29".equals(operation)) {
      N=0;
      for (int k=0; k<recsInCells[interval][step].size(); k++)
        if (recsInCells[interval][step].elementAt(k).delay>=10 && recsInCells[interval][step].elementAt(k).delay<=29)
          N++;
    }
    if ("CountFlights-Delay30to59".equals(operation)) {
      N=0;
      for (int k=0; k<recsInCells[interval][step].size(); k++)
        if (recsInCells[interval][step].elementAt(k).delay>=30 && recsInCells[interval][step].elementAt(k).delay<=59)
          N++;
    }
    if ("CountFlights-DelayOver60".equals(operation)) {
      N=0;
      for (int k=0; k<recsInCells[interval][step].size(); k++)
        if (recsInCells[interval][step].elementAt(k).delay>=60)
          N++;
    }
    return N;
  }

  public int calcMaxForSelectedSteps (int sts[]) {
    iLocalMax=0;
    int i1=0, i2=0;
    //String s=null;
    for (String sector:sectorsSorted)
      for (int interval=0; interval<Nintervals; interval++)
        for (int i=0; i<sts.length; i++) {
          int n=getCount(sector,"CountFlights",interval,sts[i]);
          if (n>iLocalMax) {
            iLocalMax = n;
            //s=sector;
            //i1=interval;
            //i2=sts[i];
          }
        }
    //System.out.println("* max = "+iGlobalMax+" (global), "+iLocalMax+" (local) in sector "+s+" in interval "+i1+" at step "+i2);
    return iLocalMax;
  }

  protected int[][] createCounts() {
    int counts[][]=new int[Nintervals][];
    for (int i=0; i<counts.length; i++) {
      counts[i] = new int[Nsteps];
      for (int j=0; j<counts[i].length; j++)
        counts[i][j]=0;
    }
    return counts;
  }

  public int[][] getCounts (String sector, String operation) {
    Vector<Record> recsInCells[][]=recsInCellsAll.get(sector);;
    int counts[][]=createCounts();
    for (int i=0; i<recsInCells.length; i++)
      for (int j=0; j<recsInCells[i].length; j++) {
        if ("CountFlights".equals(operation)) {
          counts[i][j]=recsInCells[i][j].size();
        }
    }
    return counts;
  }

  public int getMax (int counts[][]) {
    int counts_max=0;
    for (int i=0; i<counts.length; i++)
      for (int j=0; j<counts[i].length; j++)
        if (counts[i][j]>counts_max)
          counts_max=counts[i][j];
    return counts_max;
  }

  public Vector<String> getConnectedSectors (String sector) {
    Vector<Record> recsInCells[][]=recsInCellsAll.get(sector);;
    Vector<String> labels=new Vector(15,5);
    for (int i=0; i<Nintervals; i++)
      for (int j=0; j<recsInCells[0].length; j++)
        for (int k=0; k<recsInCells[i][j].size(); k++) {
          Record r=recsInCells[i][j].elementAt(k);
          String s=r.FromS;
          if (s.length()==0)
            s="NULL";
          if (!labels.contains(s))
            labels.add(s);
          s=r.ToS;
          if (s.length()==0)
            s="NULL";
          if (!labels.contains(s))
            labels.add(s);
        }
    Collections.sort(labels);
    return labels;
  }

  /**
   * Counts distinct values of FromSector/ToSector for a single cell <row,col> or the whole column, if col==-1
   * @param operation
   * @param labels
   * @param row
   * @param col
   * @return
   */
  public int[] getCountsForNominals (String sector, String operation, Vector<String> labels, int row, int col) {
    Vector<Record> recsInCells[][]=recsInCellsAll.get(sector);;
    int out[]=new int[labels.size()];
    for (int i=0; i<out.length; i++)
      out[i]=0;
    if (row==-1)
      for (int i=0; i<Nintervals; i++)
        for (int k=0; k<recsInCells[i][col].size(); k++) {
          Record r=recsInCells[i][col].elementAt(k);
          String s=operation.equals("From")?r.FromS:r.ToS;
          if (s.length()==0)
            s="NULL";
          out[labels.indexOf(s)]++;
        }
    else {
      for (int k=0; k<recsInCells[row][col].size(); k++) {
        Record r=recsInCells[row][col].elementAt(k);
        String s=operation.equals("From")?r.FromS:r.ToS;
        if (s.length()==0)
          s="NULL";
        out[labels.indexOf(s)]++;
      }
    }
    return out;
  }

  public void setHotspotMode (int hotspotMode) {
    this.hotspotMode=hotspotMode;
    aggregateAll();
    calcFeaturesOfSteps();
  }

  public void setHotspotRatio (int hotspotRatio) {
    this.hotspotRatio=hotspotRatio;
    aggregateAll();
    calcFeaturesOfSteps();
  }

  public DataKeeper (String filename_capacities, String filename_data) {
    readCapacities(filename_capacities);
    readData(filename_data);
    aggregateAll();
    calcFeaturesOfSteps();
  }

  protected Hashtable<String, Flight> allFlights=null;
  protected boolean explanationsLoaded=false;
  public Hashtable<String,int[]> attrsInExpl=null;
  public boolean isExplanationsLoaded() { return explanationsLoaded; }

  protected void loadExplanations (TreeSet<Integer> steps, String fn) {
    attrsInExpl=new Hashtable<String, int[]>();
    File f=new File(fn);
    final String pathToData=f.getAbsolutePath().substring(0,f.getAbsolutePath().lastIndexOf("\\")+1);
    SwingWorker worker=new SwingWorker() {
      @Override
      public Boolean doInBackground(){
        TapasDataReader.Readers.readExplanations(pathToData,steps,allFlights,attrsInExpl);
        return !attrsInExpl.isEmpty();
      }
      @Override
      protected void done() {
        if (!attrsInExpl.isEmpty())
          explanationsLoaded=true;
      }
    };
    worker.execute();

  }
  
  protected TreeSet<Integer> decisionSteps=null;
  
  public DataKeeper (String fnCapacities, String fnDecisions, String fnFlightPlans) {
    capacities=TapasDataReader.Readers.readCapacities(fnCapacities);
    decisionSteps=TapasDataReader.Readers.readStepsFromDecisions(fnDecisions);
    Nsteps=decisionSteps.size();
    stepLabels=new String[decisionSteps.size()];
    int n=-1;
    for (Integer i:decisionSteps) {
      n++;
      stepLabels[n] = (i.intValue() == -1) ? "baseline" : "" + i.intValue();
    }
    allFlights=TapasDataReader.Readers.readFlightDelaysFromDecisions(fnDecisions,decisionSteps);
    records=TapasDataReader.Readers.readFlightPlans(fnFlightPlans,allFlights);
    for (String s:records.keySet())
      sectors.add(s.substring(0,s.indexOf("_")));
    aggregateAll();
    calcFeaturesOfSteps();
    loadExplanations(decisionSteps,fnFlightPlans);
  }

  public DataKeeper (String fnCapacities, String fnFlightPlans, String fnSolutions[]) {
    capacities=TapasDataReader.Readers.readCapacities(fnCapacities);
    TreeSet<Integer> steps=new TreeSet();
    steps.add(new Integer(-1)); // step -1 represents the baseline solution
    Nsteps=1+fnSolutions.length;
    stepLabels=new String[Nsteps];
    stepLabels[0]="baseline";
    System.out.println("* loading "+stepLabels[0]+" from "+fnFlightPlans+".csv ...");
    records=TapasDataReader.Readers.readFlightPlans(fnFlightPlans, -1,null);
    for (int i=0; i<fnSolutions.length; i++) {
      int pos=fnSolutions[i].indexOf('=');
      stepLabels[1+i]=fnSolutions[i].substring(0,pos);
      String fn=fnSolutions[i].substring(pos+1);
      System.out.println("* loading "+stepLabels[1+i]+" from "+fn+".csv ...");
      Hashtable<String,Vector<Record>> extraRecords=TapasDataReader.Readers.readSolutionAsStep(fn,i+1);
      for (String str:extraRecords.keySet())
        records.put(str,extraRecords.get(str));
    }
    for (String s:records.keySet())
      sectors.add(s.substring(0,s.indexOf("_")));
    aggregateAll();
    calcFeaturesOfSteps();
    loadExplanations(steps,fnFlightPlans);
  }
  
  public Hashtable<String, Flight> getAllFlights() {
    return allFlights;
  }
  
  public Hashtable<String,Vector<Record>> getFlightPlans() {
    return records;
  }
  
  
  protected FlightViewManager flightViewManager=null;
  
  public void showFlightVariants(String flId){
    if (flightViewManager!=null) {
      flightViewManager.showFlightVariants(flId);
      return;
    }
    flightViewManager=new FlightViewManager(getAllFlights(),getFlightPlans());
    flightViewManager.setCapacities(capacities);
    if (decisionSteps!=null && !decisionSteps.isEmpty())
      flightViewManager.setSolutionSteps(decisionSteps);
    flightViewManager.setIncludeOnlyModifiedFlights(false);
    flightViewManager.showFlightVariants(flId);
  }
}
