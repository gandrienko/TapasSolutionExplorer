import java.io.*;
import java.util.*;

public class DataKeeper {

  protected HashSet<String> flights=new HashSet(1000), sectors=new HashSet(50);
  public Vector<SectorData> sectorsWithData=new Vector<>(50);
  Vector<String> sectorsSorted=null;

  public void sortSectors (String mode) {
    sortSectors(mode,null);
  }
  public void sortSectors (String mode, HashSet<String> selection) {
    for (SectorData sd:sectorsWithData)
      sd.compMode=mode;
    Collections.sort(sectorsWithData);
    sectorsSorted=new Vector<String>(sectors.size());
    for (SectorData sd:sectorsWithData)
      if (selection==null || selection.size()==0 || selection.contains(sd.sector)) {
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
    } catch (FileNotFoundException ex) {System.out.println("fee");}
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
    } catch (FileNotFoundException ex) {System.out.println("fee");}
  }

  int Nintervals=70, NintevalsWithHotspots=0, Ishift=20, Iduration=60;
  protected Hashtable<String,Vector<Record>[][]> recsInCellsAll=null;
  //protected Vector<Record> recsInCells[][]=null;
  public boolean[] hasHotspots=null;
  public int iGlobalMax=0;

  public void aggregateAll() {
    recsInCellsAll=new Hashtable<>();
    sectorsWithData=new Vector<>(sectors.size());
    iGlobalMax=0;
    for (String sector:sectors) {
      aggregate(sector,false);
      int capacity=capacities.get(sector);
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
    }
    //System.out.println("* global max="+iGlobalMax);
    sortSectors(SectorData.comparisonMode[0]);
  }

  public int stepsInfo[][]=null; // 0: nHotspots; 1: nFlights; 2..7: nFlightsDelay0,1_4,5_9,10-29,30_59,over60;
  public void calcFeaturesOfSteps() {
    stepsInfo=new int[Nsteps][];
    for (int step=0; step<Nsteps; step++) {
      stepsInfo[step] = new int[8];
      for (int i=0; i<stepsInfo[step].length; i++)
        stepsInfo[step][i]=0;
      Hashtable<String,Integer> allFlightsAtStep=new Hashtable<>(500);
      for (String sector:sectors) {
        Vector<Record> vr=records.get(sector+"_"+step);
        for (int interval=0; interval<Nintervals; interval++)
        if (getCount(sector,"CountFlights",interval,step)>capacities.get(sector))
          stepsInfo[step][0]+=1;
        for (Record r:vr)
          if (!allFlightsAtStep.containsKey(r.flight))
            allFlightsAtStep.put(r.flight,new Integer(r.delay));
      }
      stepsInfo[step][1]=allFlightsAtStep.size();
      for (String flight:allFlightsAtStep.keySet()) {
        int delay=allFlightsAtStep.get(flight);
        if (delay==0)
          stepsInfo[step][2]++;
        else
        if (delay>=1 && delay <=4)
          stepsInfo[step][3]++;
        else
        if (delay>=5 && delay<=9)
          stepsInfo[step][4]++;
        else
        if (delay>=10 && delay<=29)
          stepsInfo[step][5]++;
        else
        if (delay>=30 && delay<=59)
          stepsInfo[step][6]++;
        else
          stepsInfo[step][7]++;
      }
    }
    for (int i=0; i<Nsteps; i++)
      System.out.println("step="+i+stepsInfo[i]);

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
        for (int j = 0; j < vr.size(); j++) {
          Record r = vr.get(j);
          for (int k = 0; k < Nintervals; k++) {
            int k1 = k * Ishift, k2 = k1 + Iduration - 1;
            boolean intersect = Math.max(k1, r.FromN) <= Math.min(k2, r.ToN);
            if (intersect)
              recsInCells[k][i].add(r);
          }
        }
      }
    }
    hasHotspots=new boolean[Nintervals];
    int capacity=capacities.get(sector).intValue();
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
    int capacity=capacities.get(sector);
    for (int interval=0; interval<Nintervals; interval++)
      if (recsInCells[interval][step].size()>capacity)
        N++;
    return N;
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

  public DataKeeper (String filename_data, String filename_capacities) {
    readCapacities(filename_capacities);
    readData(filename_data);
    aggregateAll();
    calcFeaturesOfSteps();
  }

}
