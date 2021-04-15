package TapasSolutionExplorer.Data;

public class SectorData implements Comparable<SectorData> {
  final public static String comparisonMode[]={"name","flights all","hotspots_all","hotspots_step0","hotspots_stepLast"}; //,"flights delayed","flights delayed severely"};
  public String compMode=comparisonMode[0];
  public String sector;
  public Integer Nhotspots_all=0, Nhotspots_step0=0, Nhotspots_stepLast=0, Nflights=0, NflightsWithDelays[];
  public int compareTo (SectorData sd) {
    if (compMode.equals(comparisonMode[0]))
      return sector.compareTo(sd.sector);
    else
    if (compMode.equals(comparisonMode[2]))
      return Nhotspots_all.compareTo(sd.Nhotspots_all);
    else
    if (compMode.equals(comparisonMode[3]))
      return Nhotspots_step0.compareTo(sd.Nhotspots_step0);
    else
    if (compMode.equals(comparisonMode[4]))
      return Nhotspots_stepLast.compareTo(sd.Nhotspots_stepLast);
    //else
    //if (compMode.equals(comparisonMode[2]))
    return Nflights.compareTo(sd.Nflights);
    //else
  }
}
