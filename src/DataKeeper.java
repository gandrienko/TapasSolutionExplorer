import java.io.*;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

public class DataKeeper {

  protected HashSet<String> flights=new HashSet(1000), sectors=new HashSet(50);
  protected Hashtable<String,Vector<Record>> records=new Hashtable(100000);

  public Hashtable<String,Integer> capacities=new Hashtable(100);

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

  int Nintervals=70, Nsteps=1440, Ishift=20, Iduration=60;
  protected Vector<Record> recsInCells[][]=null;

  public void aggregate (String sector) {
    recsInCells=new Vector[Nintervals][];
    for (int i=0; i<recsInCells.length; i++) {
      recsInCells[i] = new Vector[Nsteps];
      for (int j=0; j<recsInCells[i].length; j++)
        recsInCells[i][j]=new Vector<Record>(10,10);
    }
    for (int i=0; i<Nsteps; i++) {
      String key=sector+"_"+i;
      Vector<Record> vr=records.get(key);
      for (int j=0; j<vr.size(); j++) {
        Record r=vr.get(j);
        for (int k=0; k<Nintervals; k++) {
          int k1=k*Ishift, k2=k1+Iduration-1;
          boolean intersect=Math.max(k1,r.FromN)<=Math.min(k2,r.ToN);
          if (intersect)
            recsInCells[k][i].add(r);
        }
      }
    }
  }

  public Vector<int[]> checkEqual () {
    Vector<int[]> list=new Vector(10,10);
    boolean blist[]=new boolean[Nsteps];
    for (int step=1; step<this.Nsteps; step++) {
      boolean equal = true;
      for (int i = 0; i < this.Nintervals && equal; i++) {
        equal = recsInCells[i][step].size()==recsInCells[i][step - 1].size();
        if (equal)
          for (int k=0; k<recsInCells[i][step].size() && equal; k++)
            equal=recsInCells[i][step].elementAt(k).flight.equals(recsInCells[i][step-1].elementAt(k).flight);
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

  public int getCount (String operation, int i,int j) {
    int N=-1;
    if ("CountFlights".equals(operation))
      N=recsInCells[i][j].size();
    if ("CountFlights-noDelay".equals(operation)) {
      N=0;
      for (int k=0; k<recsInCells[i][j].size(); k++)
        if (recsInCells[i][j].elementAt(k).delay==0)
          N++;
    }
    if ("CountFlights-Delay1to4".equals(operation)) {
      N=0;
      for (int k=0; k<recsInCells[i][j].size(); k++)
        if (recsInCells[i][j].elementAt(k).delay>0 && recsInCells[i][j].elementAt(k).delay<5)
          N++;
    }
    if ("CountFlights-Delay5to9".equals(operation)) {
      N=0;
      for (int k=0; k<recsInCells[i][j].size(); k++)
        if (recsInCells[i][j].elementAt(k).delay>=5 && recsInCells[i][j].elementAt(k).delay<=9)
          N++;
    }
    if ("CountFlights-Delay10to29".equals(operation)) {
      N=0;
      for (int k=0; k<recsInCells[i][j].size(); k++)
        if (recsInCells[i][j].elementAt(k).delay>=10 && recsInCells[i][j].elementAt(k).delay<=29)
          N++;
    }
    if ("CountFlights-Delay30to59".equals(operation)) {
      N=0;
      for (int k=0; k<recsInCells[i][j].size(); k++)
        if (recsInCells[i][j].elementAt(k).delay>=30 && recsInCells[i][j].elementAt(k).delay<=59)
          N++;
    }
    if ("CountFlights-DelayOver60".equals(operation)) {
      N=0;
      for (int k=0; k<recsInCells[i][j].size(); k++)
        if (recsInCells[i][j].elementAt(k).delay>=60)
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
  public int[][] getCounts (String operation) {
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

  public DataKeeper (String filename_data, String filename_capacities) {
    readCapacities(filename_capacities);
    readData(filename_data);
  }

}
