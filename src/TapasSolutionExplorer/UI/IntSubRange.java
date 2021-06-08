package TapasSolutionExplorer.UI;

/**
 * A data structure to represent a selected subrange of a range of integer values
 */
public class IntSubRange {
  /**
   * The whole range
   */
  public int absMin=0, absMax=0;
  /**
   * The currently selected subrange
   */
  public int currMin=0, currMax=0;
  
  public IntSubRange() {}
  
  public IntSubRange(int absMin, int absMax, int currMin, int currMax) {
    this.absMin=absMin; this.absMax=absMax; this.currMin=currMin; this.currMax=currMax;
  }
}
