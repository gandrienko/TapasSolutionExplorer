package TapasSolutionExplorer.flight_vis;

import TapasDataReader.Record;
import TapasSolutionExplorer.Data.FlightConstructor;
import TapasSolutionExplorer.Data.FlightInSector;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

public class FlightVariantsShow extends JPanel implements MouseListener, MouseMotionListener {
  public static final int secondsInDay =86400, minutesInDay=1440;
  public static float dash1[] = {10.0f,5.0f};
  public static Stroke dashedStroke = new BasicStroke(1.0f,BasicStroke.CAP_BUTT,
      BasicStroke.JOIN_MITER,10.0f, dash1, 0.0f);
  public static Color
      sectorColor =new Color(128,70,0, 196),
      sectorBkgColor =new Color(255,165,0,30),
      capacityColor=new Color(128, 0, 0, 128);
  public static float alphaMin=0.075f, alphaMax=0.5f;
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
   * Flight plan versions for all solution steps.
   * The keys of the hashtable consist of sector identifiers and step numbers with underscore between them.
   */
  protected Hashtable<String, Vector<Record>> flightPlans=null;
  /**
   * Capacities of the sectors (max acceptable N of flights per hour)
   */
  protected Hashtable<String,Integer> capacities=null;
  /**
   * Index of the currently shown flight
   */
  protected int shownFlightIdx=-1;
  /**
   * The sequence of sectors visited by the flight variants;
   * includes all sectors present in at least one variant.
   */
  protected ArrayList<String> sectorSequence=null;
  /**
   * Time step, in minutes, for aggregating flights in sectors
   */
  public int tStepAggregates=20;
  /**
   * Whether to count entries (true) or presence (false)
   */
  public boolean toCountEntries=true;
  /**
   * Whether to ignore re-entries when counting entries
   */
  public boolean toIgnoreReEntries=true;
  /**
   * Whether to highlight excess of capacity
   */
  public boolean toHighlightCapExcess=true;
  /**
   * Threshold for the capacity excess to highlight, in percents
   */
  public float minExcessPercent=10;
  /**
   * Index of highlighted and selected variants.
   * When two variants are selected, the second is compared with the first.
   */
  protected int hlIdx=-1, selIdx=-1, selIdx2=-1;
  /**
   * Hourly counts of sector entries or occupancies for the solution steps
   * corresponding to the selected flight variants.
   * Dimension 0: sectors, dimension 1: time steps.
   */
  protected int hourlyCounts[][]=null, hourlyCounts2[][]=null;
  /**
   * Used for drawing and calculating positions for times
   */
  protected int tMarg=10, tWidth=0;
  protected int yMarg=0, plotH=0, secH=0,
      vSpace=Math.round(6f*Toolkit.getDefaultToolkit().getScreenResolution()/25.4f);
  /**
   * Time range to show (minutes of the day)
   */
  public int minuteStart=0, minuteEnd=minutesInDay;
  /**
   * Time range length in minutes and in seconds
   */
  public int tLengthMinutes =minutesInDay, tLengthSeconds=tLengthMinutes*60;
  /**
   * Each element draws a line or polygon representing a single flight
   */
  protected FlightDrawer flightDrawers[]=null;
  /**
   * Used to speed up redrawing
   */
  protected BufferedImage off_Image=null;
  protected boolean off_Valid=false;
  
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
    setPreferredSize(new Dimension(Math.round(0.6f*size.width), Math.round(0.6f*size.height)));
    addMouseListener(this);
    addMouseMotionListener(this);
    ToolTipManager.sharedInstance().registerComponent(this);
    ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
  }
  /**
   * Flight plan versions for all solution steps, to be used for showing histograms of sector loads.
   * The keys of the hashtable consist of sector identifiers and step numbers with underscore between them.
   */
  
  public void setFlightPlans(Hashtable<String, Vector<Record>> flightPlans) {
    this.flightPlans = flightPlans;
  }
  /**
   * Capacities of the sectors (max acceptable N of flights per hour)
   */
  public void setCapacities(Hashtable<String, Integer> capacities) {
    this.capacities = capacities;
  }
  
  public boolean showFlightVariants(String flId) {
    hlIdx=-1; selIdx=-1; selIdx2=-1;
    hourlyCounts=null; hourlyCounts2=null;
    if (flId==null || flightIndex==null)
      return  false;
    Integer ii=flightIndex.get(flId);
    if (ii==null)
      return false;
    shownFlightIdx=ii;
    System.out.println("Show flight #"+shownFlightIdx+", id="+flId+", "+flights[shownFlightIdx].length+" variants");
    if (sectorSequence==null)
      sectorSequence=new ArrayList<String>(20);
    else
      sectorSequence.clear();
    //take the sector sequence from the first variant
    FlightInSector fSeq[]=flights[shownFlightIdx][0];
    for (int i=0; i<fSeq.length; i++)
      if (!sectorSequence.contains(fSeq[i].sectorId))
        sectorSequence.add(fSeq[i].sectorId);
    //now go through the remaining variants and try to merge their sequences
    for (int v=1; v<flights[shownFlightIdx].length; v++) {
      fSeq=flights[shownFlightIdx][v];
      int idxs[]=new int[fSeq.length]; //indexes of the sectors present in the existing sequence or -1 if absent
      int currIdx=0, nMatch=0;
      for (int i=0; i<fSeq.length; i++) {
        idxs[i] = -1;
        for (int j=currIdx; j<sectorSequence.size() && idxs[i]<0; j++)
          if (fSeq[i].sectorId.equals(sectorSequence.get(j))) {
            idxs[i]=j;
            currIdx=j+1;
            ++nMatch;
          }
      }
      if (nMatch==fSeq.length)
        continue;
      //try to match from the opposite side
      currIdx=sectorSequence.size()-1;
      int nMatch2=0;
      int idxs2[]=new int[fSeq.length];
      for (int i=fSeq.length-1; i>=0; i--) {
        idxs2[i]=-1;
        for (int j=currIdx; j>=0 && idxs2[i]<0; j--)
          if (fSeq[i].sectorId.equals(sectorSequence.get(j))) {
            idxs[i]=j;
            currIdx=j-1;
            ++nMatch2;
          }
      }
      if (nMatch2>nMatch) {
        idxs=idxs2;
        nMatch=nMatch2;
      }
      if (nMatch==fSeq.length)
        continue;
      int lastIdx=-1;
      for (int i=0; i<fSeq.length; i++) {
        int idx=sectorSequence.indexOf(fSeq[i].sectorId);
        if (idx >= 0)
          lastIdx=idx;
        else {
          int iIns = -1;
          for (int j = i + 1; j < idxs.length && iIns < 0; j++)
            if (idxs[j] >= 0 && idxs[j] > lastIdx)
              iIns = idxs[j];
          if (iIns < 0) {
            sectorSequence.add(fSeq[i].sectorId);
            lastIdx = sectorSequence.size() - 1;
          }
          else {
            sectorSequence.add(iIns, fSeq[i].sectorId);
            for (int j = i + 1; j < idxs.length; j++)
              if (idxs[j] >= iIns) ++idxs[j];
            lastIdx = iIns;
          }
        }
      }
    }
    System.out.println("Sector sequence consists of "+sectorSequence.size()+" sectors");
    off_Valid=false;
    redraw();
    return !sectorSequence.isEmpty();
  }
  
  public int getShownFlightIdx() {
    return shownFlightIdx;
  }
  
  public void setTimeRange(int minute1, int minute2) {
    if ((minute1!=minuteStart || minute2!=minuteEnd) && minute2-minute1>=60) {
      minuteStart=minute1; minuteEnd=minute2;
      tLengthMinutes =minuteEnd-minuteStart;
      tLengthSeconds=tLengthMinutes*60;
      off_Valid = false;
      //redraw();
      repaint();
    }
  }
  
  public int getMinuteStart() {
    return minuteStart;
  }
  public int getMinuteEnd() {
    return minuteEnd;
  }
  
  public int getXPos(LocalTime t, int width) {
    if (t==null)
      return -1;
    //int tSinceMidnight=t.getHour()*3600+t.getMinute()*60+t.getSecond();
    //return Math.round(((float)width)*tSinceMidnight/ secondsInDay);
    if (t.getSecond()>0) {
      int tSinceStart = (t.getHour() * 60 + t.getMinute() - minuteStart) * 60 + t.getSecond();
      return Math.round(((float) width) * tSinceStart / tLengthSeconds);
    }
    int tSinceStart = t.getHour() * 60 + t.getMinute() - minuteStart;
    return Math.round(((float) width) * tSinceStart / tLengthMinutes);
  }
  
  public int getXPos(int minute, int width) {
    return Math.round(((float)width)*(minute-minuteStart)/tLengthMinutes);
  }
  
  public int getMinuteOfDayForXPos(int xPos, int width) {
    if (xPos<0)
      return -1;
    return Math.round((float)xPos/width*tLengthMinutes)+minuteStart;
  }
  
  public LocalTime getTimeForXPos(int xPos, int width) {
    if (xPos<0)
      return null;
    int secOfDay=minuteStart*60+Math.round((float)xPos/width*tLengthSeconds);
    int h=secOfDay/3600, mm=secOfDay%3600, m=mm/60, s=mm%60;
    if (h>23)
      return LocalTime.of(0,0,0);
    return LocalTime.of(h,m,s);
  }
  
  protected void createFlightDrawers(){
    if (flights==null || shownFlightIdx<0 || sectorSequence==null || sectorSequence.isEmpty()) {
      flightDrawers=null;
      hlIdx=-1; selIdx=-1; selIdx2=-1;
      return;
    }
    if (flightDrawers==null || flightDrawers.length!=flights[shownFlightIdx].length) {
      hlIdx=-1; selIdx=-1; selIdx2=-1;
      flightDrawers=new FlightDrawer[flights[shownFlightIdx].length];
      for (int i=0; i<flightDrawers.length; i++) {
        flightDrawers[i] = new FlightDrawer();
        FlightInSector fSeq[]=flights[shownFlightIdx][i];
        flightDrawers[i].flightId = fSeq[0].flightId;
        flightDrawers[i].variant=i;
        flightDrawers[i].step=fSeq[0].step;
      }
    }
  }
  
  protected void setupFlightDrawers(int y0) {
    if (sectorSequence==null || sectorSequence.isEmpty())
      return;
    createFlightDrawers();
    if (flightDrawers==null)
      return;
    int secH=(plotH-(sectorSequence.size()-1)*vSpace)/sectorSequence.size();
    for (int i=0; i<flights[shownFlightIdx].length; i++) {
      flightDrawers[i].clearPath();
      FlightInSector fSeq[]=flights[shownFlightIdx][i];
      for (int j=0; j<fSeq.length; j++) {
        int sIdx=sectorSequence.indexOf(fSeq[j].sectorId);
        if (sIdx<0)  //must not happen!
          continue;
        int y=y0+(secH+vSpace)*sIdx;
        flightDrawers[i].addPathSegment(getXPos(fSeq[j].entryTime,tWidth)+tMarg,
            getXPos(fSeq[j].exitTime,tWidth)+tMarg,y,y+secH);
      }
    }
  }
  
  public int getFlightIdxAtPosition(int x, int y){
    if (flightDrawers==null)
      return -1;
    for (int i=0; i<flightDrawers.length; i++)
      if (flightDrawers[i].contains(x,y))
        return i;
    return -1;
  }
  
  public void paintComponent(Graphics gr) {
    int w=getWidth(), h=getHeight();
    if (w<10 || h<10)
      return;
    if (off_Image!=null && off_Valid) {
      if (off_Image.getWidth()!=w || off_Image.getHeight()!=h) {
        off_Image = null; off_Valid=false;
      }
      else {
        gr.drawImage(off_Image,0,0,null);
        if (selIdx>=0)
          flightDrawers[selIdx].drawSelected(gr,false);
        if (selIdx2>=0)
          flightDrawers[selIdx2].drawSelected(gr,true);
        if (hlIdx>=0)
          flightDrawers[hlIdx].drawHighlighted(gr);
        return;
      }
    }

    if (sectorSequence==null || sectorSequence.isEmpty())
      return;
    
    if (off_Image==null || off_Image.getWidth()!=w || off_Image.getHeight()!=h)
      off_Image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = off_Image.createGraphics();
    
    yMarg=g.getFontMetrics().getHeight();
    plotH=h-2*yMarg;
    int asc=g.getFontMetrics().getAscent();
    
    g.setColor(Color.lightGray);
    g.fillRect(0,0,w,h);
    
    tWidth=w-2*tMarg;
    
    Color tickColor=new Color(90,90,90, 90);
    Stroke origStroke=g.getStroke();
    g.setStroke(dashedStroke);
    for (int i=0; i<=24; i++) {
      int x=tMarg+getXPos(i*60,tWidth);
      if (x<0 || x>w)
        continue;
      g.setColor(tickColor);
      g.drawLine(x,yMarg,x,yMarg+plotH);
      String str=String.format("%02d:00",i);
      int sw=g.getFontMetrics().stringWidth(str);
      g.setColor(Color.darkGray);
      g.drawString(str,x-sw/2,yMarg-2);
      g.drawString(str,x-sw/2,h-yMarg+asc);
    }
    g.setStroke(origStroke);
    
    RenderingHints rh = new RenderingHints(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHints(rh);
  
    secH=(plotH-(sectorSequence.size()-1)*vSpace)/sectorSequence.size();
  
    int y=yMarg;
    for (int i=0; i<sectorSequence.size(); i++) {
      g.setColor(sectorBkgColor);
      g.fillRect(0,y,w,secH);
      g.setColor(sectorColor);
      g.drawLine(0,y,w,y);
      g.drawLine(0,y+secH,w,y+secH);
      if (hourlyCounts!=null && hourlyCounts[i]!=null)
        if (hourlyCounts2!=null && hourlyCounts2[i]!=null)
          compareSectorVisitAggregates(g,sectorSequence.get(i),hourlyCounts[i],hourlyCounts2[i],y);
      else
        showSectorVisitAggregates(g,sectorSequence.get(i),hourlyCounts[i],y);
      g.setColor(sectorColor);
      g.drawString(sectorSequence.get(i),5,y+5+asc);
      y+=secH+vSpace;
    }

    setupFlightDrawers(yMarg);
    
    if (flightDrawers!=null)
      for (int i=0; i<flightDrawers.length; i++)
        flightDrawers[i].draw(g);
    
    off_Valid=true;
    gr.drawImage(off_Image,0,0,null);
    if (selIdx>=0)
      flightDrawers[selIdx].drawSelected(gr,false);
    if (selIdx2>=0)
      flightDrawers[selIdx2].drawSelected(gr,true);
    if (hlIdx>=0)
      flightDrawers[hlIdx].drawHighlighted(gr);
  }
  
  protected void showSectorVisitAggregates(Graphics g, String sectorId,
                                           int counts[],
                                           int y0) {
    if (counts==null || sectorId==null)
      return;
    int max=0;
    for (int j=0; j<counts.length; j++)
        if (max<counts[j])
          max=counts[j];
    if (max<=0) return;
    
    Integer capacity=(capacities==null)?null:capacities.get(sectorId);
  
    float capToHighlight=Float.NaN;
    
    if (capacity!=null && capacity<999) {
      if (toHighlightCapExcess && max>capacity)
        capToHighlight=(100+minExcessPercent)*capacity/100;
      max = Math.max(max, capacity);
    }
    
    int nOverlap=Math.round(60/tStepAggregates)-1;
    float overlapRatio=nOverlap/59;
    float alpha=alphaMax-overlapRatio*(alphaMax-alphaMin);
  
    int maxBH=secH-2;
    for (int j = 0; j < counts.length; j++)
      if (counts[j] > 0) {
        int t=j*tStepAggregates;
        int x1 = tMarg+getXPos(t, tWidth), x2 = tMarg+getXPos(t +60, tWidth);
        float ratio=Math.min(((float) counts[j]) / max,1);
        int bh=Math.round(ratio * maxBH);
        if (!Float.isNaN(capToHighlight) && counts[j] > capToHighlight)
          g.setColor(new Color(ratio,0,0,alpha));
        else
          g.setColor(new Color(1-ratio,1-ratio,1-ratio,alpha));
        g.fillRect(x1, y0 + secH -1 - bh, x2 - x1 + 1, bh);
        g.drawRect(x1, y0 + secH -1 - bh, x2 - x1 + 1, bh);
      }
    if (capacity!=null && capacity<max) {
      g.setColor(capacityColor);
      int cy=y0+secH-Math.round(((float)capacity) / max * maxBH);
      g.drawLine(0,cy,tWidth,cy);
    }
  }
  
  protected void compareSectorVisitAggregates(Graphics g, String sectorId,
                                              int counts1[], int counts2[], int y0) {
    if (sectorId==null)
      return;
    if (counts1==null || counts2==null) {
      showSectorVisitAggregates(g,sectorId,(counts1!=null)?counts1:counts2,y0);
    }
    int diff[]=new int[counts1.length];
    int minDiff=Integer.MAX_VALUE, maxDiff=Integer.MIN_VALUE, max2=0;
    for (int j=0; j<diff.length; j++) {
      diff[j]=counts2[j]-counts1[j];
      if (maxDiff < diff[j])
        maxDiff = diff[j];
      if (minDiff>diff[j])
        minDiff=diff[j];
      if (max2<counts2[j])
        max2=counts2[j];
    }
    if (minDiff>=maxDiff) return;
    int absMaxDiff=Math.max(Math.abs(minDiff),Math.abs(maxDiff));
    
    Integer capacity=(capacities==null)?null:capacities.get(sectorId);
    
    float capToHighlight=Float.NaN;
    
    if (capacity!=null && capacity<999) {
      if (toHighlightCapExcess && max2>capacity)
        capToHighlight=(100+minExcessPercent)*capacity/100;
      absMaxDiff = Math.max(absMaxDiff, capacity);
    }
    
    int nOverlap=Math.round(60/tStepAggregates)-1;
    float overlapRatio=nOverlap/59;
    float alpha=alphaMax-overlapRatio*(alphaMax-alphaMin);
    
    int maxBH=secH-2;
    int yAxis=(maxDiff<=0)?1:(minDiff>=0)?maxBH+1:Math.round(((float)maxBH)/(maxDiff-minDiff)*maxDiff);
    yAxis+=y0;
    g.setColor(new Color(255,255,255,128));
    g.drawLine(0,yAxis,tWidth,yAxis);

    for (int j = 0; j < diff.length; j++)
      if (diff[j] != 0) {
        int t=j*tStepAggregates;
        int x1 = tMarg+getXPos(t, tWidth), x2 = tMarg+getXPos(t +60, tWidth);
        int bh=Math.round(((float) diff[j]) / (maxDiff-minDiff) * maxBH);
        if (!Float.isNaN(capToHighlight) && counts2[j] > capToHighlight) {
          float ratio=((float) diff[j]) / capacity, rAbs=Math.min(1f,Math.abs(ratio));
          g.setColor(new Color(rAbs, 0, 0, alpha));
        }
        else {
          float ratio=((float) diff[j]) / absMaxDiff, rAbs=Math.min(1f,Math.abs(ratio));
          g.setColor(new Color(1 - rAbs, 1 - rAbs, 1 - rAbs, alpha));
        }
        g.fillRect(x1, (bh>0)?yAxis - bh:yAxis, x2 - x1 + 1, Math.abs(bh));
        g.drawRect(x1, (bh>0)?yAxis - bh:yAxis, x2 - x1 + 1, Math.abs(bh));
      }
  }
  
  public void redraw() {
    if (isShowing())
      paintComponent(getGraphics());
  }
  
  protected int getSectorIdx(int yPos) {
    if (yPos<=yMarg || yPos>=yMarg+plotH)
      return -1;
    int sIdx=(yPos-yMarg)/(secH+vSpace);
    if (sIdx<0 || yPos>yMarg+sIdx*(secH+vSpace)+secH) //the mouse is in a vertical space between sectors
      return -1;
    return sIdx;
  }
  
  protected void selectVariant1(int vIdx) {
    if (selIdx==vIdx || selIdx2==vIdx)
      return;
    selIdx=vIdx;
    if (selIdx<0)
      hourlyCounts=null;
    else
    if (flightPlans!=null && !flightPlans.isEmpty())  {
      int solutionStep=flightDrawers[selIdx].step;
      hourlyCounts=new int[sectorSequence.size()][];
      for (int i=0; i<sectorSequence.size(); i++)
        hourlyCounts[i]= FlightConstructor.getHourlyCountsOfSectorEntries(flightPlans,
            sectorSequence.get(i),solutionStep,tStepAggregates,toIgnoreReEntries);
      off_Valid=false;
    }
    redraw();
  }
  
  protected void selectVariant2(int vIdx){
    if (selIdx2==vIdx || selIdx==vIdx)
      return;
    selIdx2=vIdx;
    if (selIdx2<0)
      hourlyCounts2=null;
    else
      if (flightPlans!=null && !flightPlans.isEmpty())  {
        int solutionStep=flightDrawers[selIdx2].step;
        hourlyCounts2=new int[sectorSequence.size()][];
        for (int i=0; i<sectorSequence.size(); i++)
          hourlyCounts2[i]= FlightConstructor.getHourlyCountsOfSectorEntries(flightPlans,
              sectorSequence.get(i),solutionStep,tStepAggregates,toIgnoreReEntries);
        off_Valid=false;
      }
    redraw();
  }
  
  protected void cancelSelection() {
    if (selIdx>=0 || selIdx2>=0) {
      selIdx=-1; selIdx2=-1;
      hourlyCounts=hourlyCounts2=null;
      off_Valid=false;
      redraw();
    }
  }
  
  public String getTextForFlightVersion(int fIdx, int xPos, int yPos) {
    if (fIdx<0)
      return null;
    FlightInSector fSeq[]=flights[shownFlightIdx][fIdx];
    int sIdx=getSectorIdx(yPos), sIdxInFlight=-1;
    if (sIdx>=0) { //check if this flight variant goes through this sector
      String sectorId=sectorSequence.get(sIdx);
      LocalTime t=getTimeForXPos(xPos-tMarg,tWidth);
      if (t!=null)
        for (int i=0; i<fSeq.length && sIdxInFlight<0; i++)
          if (sectorId.equals(fSeq[i].sectorId) &&
                  t.compareTo(fSeq[i].entryTime)>=0 && t.compareTo(fSeq[i].exitTime)<=0)
            sIdxInFlight=i;
    }
    FlightInSector fSel1[] = (selIdx >= 0) ? flights[shownFlightIdx][selIdx] : null;
    FlightInSector fSel2[] = (selIdx2 >= 0) ? flights[shownFlightIdx][selIdx2] : null;

    String str="<html><body style=background-color:rgb(255,255,204)><b>"+fSeq[0].flightId+"</b>";
    str += "<table border=0 cellmargin=3 cellpadding=3 cellspacing=3>";
    
    if (fSel1!=null || fSel2!=null) {
      str+="<tr><td> </td><td>At cursor</td>";
      if (fSel1!=null)
        str+="<td>Selection 1</td><td>Difference</td>";
      if (fSel2!=null)
        str+="<td>Selection 2</td><td>Difference</td>";
      str+="</tr>";
    }

    str+="<tr><td>Version N</td><td>"+fIdx+"</td>";
    if (fSel1!=null)
      str+="<td>"+selIdx+"</td><td>-</td>";
    if (fSel2!=null)
      str+="<td>"+selIdx2+"</td><td>-</td>";
    str+="</tr>";
    str+="<tr><td>Solution step</td><td>"+fSeq[0].step+"</td>";
    if (fSel1!=null) {
      int diff=fSeq[0].step - fSel1[0].step;
      String sDiff=((diff>0)?"+":"")+diff;
      str += "<td>" + fSel1[0].step + "</td><td>" + sDiff + "</td>";
    }
    if (fSel2!=null) {
      int diff=fSeq[0].step - fSel2[0].step;
      String sDiff=((diff>0)?"+":"")+diff;
      str += "<td>" + fSel2[0].step + "</td><td>" + sDiff + "</td>";
    }
    str+="</tr>";
    str+="<tr><td>Delay</td><td>"+fSeq[0].delay+"</td>";
    if (fSel1!=null) {
      int diff=fSeq[0].delay - fSel1[0].delay;
      String sDiff=((diff>0)?"+":"")+diff;
      str += "<td>" + fSel1[0].delay + "</td><td>" + sDiff + "</td>";
    }
    if (fSel2!=null) {
      int diff=fSeq[0].delay - fSel2[0].delay;
      String sDiff=((diff>0)?"+":"")+diff;
      str += "<td>" + fSel2[0].delay + "</td><td>" + sDiff + "</td>";
    }
    str+="</tr>";
    
    if (sIdxInFlight>=0) {
      FlightInSector fs=fSeq[sIdxInFlight];
      str+="<tr><td> </td><td>Sector</td><td>Entry time</td><td>Exit time</td></tr>";
      str+="<tr><td>At cursor</td><td>"+fs.sectorId+"</td><td>"+fs.entryTime+"</td><td>"+fs.exitTime+"</td></tr>";
      if (fs.prevSectorId!=null) {
        str += "<tr><td>Previous</td><td>" + fs.prevSectorId + "</td>";
        if (sIdxInFlight>0) {
          FlightInSector fs2=fSeq[sIdxInFlight-1];
          str+="<td>"+fs2.entryTime+"</td><td>"+fs2.exitTime+"</td></tr>";
        }
        str+="</tr>";
      }
      if (fs.nextSectorId!=null) {
        str += "<tr><td>Next</td><td>" + fs.nextSectorId + "</td>";
        if (sIdxInFlight+1<fSeq.length) {
          FlightInSector fs2=fSeq[sIdxInFlight+1];
          str+="<td>"+fs2.entryTime+"</td><td>"+fs2.exitTime+"</td></tr>";
        }
        str+="</tr>";
      }
    }
    str+="</table>";
    str+="</body></html>";
  
    str+="</table>";
    str+="</body></html>";
    return str;
  }
  
  /**
   * @param binWidth - always in minutes
   */
  public int getMinuteOfDayBinIndex(int minuteOfDay, int binWidth){
    if (minuteOfDay<0 || binWidth<=0)
      return -1;
    return minuteOfDay/binWidth;
  }
  
  public static LocalTime[] getTimeBinRange(int binIdx, int binWidth) {
    if (binWidth<=0 || binIdx<0)
      return null;
    LocalTime tt[]=new LocalTime[2];
    int m=binIdx*binWidth;
    tt[0]=LocalTime.of((m/60)%24,m%60,0);
    m+=59;
    tt[1]=LocalTime.of((m/60)%24,m%60,59);
    return tt;
  }
  
  public String getTextForSector(int sectorIdx, int xPos) {
    if (sectorIdx<0)
      return null;
    String sectorId=sectorSequence.get(sectorIdx);
    String txt="<html><body style=background-color:rgb(255,255,204)>"+
                   "<font size=4><center>Sector "+sectorId+"</center></font>";
    Integer capacity=(capacities==null)?null:capacities.get(sectorId);
    if (capacity!=null)
      txt+="<center>Sector capacity = "+capacity+"</center>";
    LocalTime t=getTimeForXPos(xPos-tMarg,tWidth);
    txt+="<center>Time = "+t+"</center>";

    if (hourlyCounts!=null && hourlyCounts[sectorIdx]!=null) {
      int m=getMinuteOfDayForXPos(xPos-tMarg,tWidth);
      int idx=getMinuteOfDayBinIndex(m,tStepAggregates);
      LocalTime tt[]=getTimeBinRange(idx,tStepAggregates);
      if (tt!=null) {
        FlightInSector fSel1[] = (selIdx >= 0) ? flights[shownFlightIdx][selIdx] : null;
        FlightInSector fSel2[] = (selIdx2 >= 0) ? flights[shownFlightIdx][selIdx2] : null;
  
        txt += "<table border=0 cellmargin=3 cellpadding=3 cellspacing=3>";
        
        String aggrName=(toCountEntries)?"entries":"occupancy";
        if (hourlyCounts2 == null) {
          txt += "<tr><td>Time bin</td><td>#"+idx+"</td><td>"+tt[0]+":00</td><td>.."+tt[1]+"</td></tr>";
          txt += "<tr><td>Solution step:</td><td>" + fSel1[0].step + "</td></tr>";
          txt += "<tr><td>Hourly sector "+aggrName+":</td><td>" + hourlyCounts[sectorIdx][idx]+"</td></tr>";
          if (capacity!=null && hourlyCounts[sectorIdx][idx]>capacity) {
            int diff=hourlyCounts[sectorIdx][idx]-capacity;
            float percent=100f*diff/capacity;
            txt+="<tr><td>Excess of capacity:</td><td>"+diff+"</td><td>flights</td><td>("+
                     String.format("%.2f", percent)+"%)</td></tr>";
          }
        }
        else {
          txt += "<tr><td>Solution steps:</td><td>" + fSel1[0].step +"</td><td>" + fSel2[0].step + "</td></tr>";
          int diff=hourlyCounts2[sectorIdx][idx]-hourlyCounts[sectorIdx][idx];
          String diffStr=((diff>0)?"+":"")+diff;
          txt += "<tr><td>Time bin</td><td>#"+idx+"</td><td>"+tt[0]+":00</td><td>.."+tt[1]+"</td></tr>";
          txt += "<tr><td>Hourly sector "+aggrName+":</td><td>" + hourlyCounts[sectorIdx][idx] +"</td><td>" +
                     hourlyCounts2[sectorIdx][idx] +"</td><td>" + diffStr +"</td></tr>";
          if (capacity!=null && (hourlyCounts[sectorIdx][idx]>capacity || hourlyCounts2[sectorIdx][idx]>capacity)) {
            int diffCap=Math.max(0,hourlyCounts[sectorIdx][idx]-capacity),
                diffCap2=Math.max(0,hourlyCounts2[sectorIdx][idx]-capacity);
            diff=diffCap2-diffCap;
            diffStr=((diff>0)?"+":"")+diff;
            txt += "<tr><td>Excess of capacity (count):</td><td>" + diffCap +"</td><td>" + diffCap2+
                       "</td><td>" +diffStr + "</td></tr>";
            float percent=(diffCap<=0)?0:100f*diffCap/capacity, percent2=(diffCap2<=0)?0:100f*diffCap2/capacity;
            float fDiff=percent2-percent;
            diffStr=((diff>0)?"+":"")+String.format("%.2f", fDiff);
            txt+="<tr><td>Excess of capacity (percent):</td><td>"+String.format("%.2f", percent) +
                     "%</td><td>"+String.format("%.2f", percent2) +"%</td><td>"+diffStr+"%</td></tr>";
          }
        }
        txt += "</table>";
      }
    }
    txt+="</body></html>";
    return txt;
  }
  
  //---------------------- MouseListener ----------------------------------------
  public String getToolTipText(MouseEvent me) {
    if (!isShowing())
      return null;
    if (me.getButton()!=MouseEvent.NOBUTTON)
      return null;
    int fIdx= getFlightIdxAtPosition(me.getX(),me.getY());
    if (fIdx>=0)
      return getTextForFlightVersion(fIdx,me.getX(),me.getY());
    int sIdx=getSectorIdx(me.getY());
    if (sIdx>=0)
      return getTextForSector(sIdx,me.getX());
    return null;
  }
  
  public void mouseMoved(MouseEvent me) {
    if (!isShowing())
      return;
    if (me.getButton()!=MouseEvent.NOBUTTON)
      return;
    int fIdx= getFlightIdxAtPosition(me.getX(),me.getY());
    if (fIdx==hlIdx)
      return;
    hlIdx=fIdx;
    redraw();
  }
  
  public void mouseClicked(MouseEvent e) {
    if (e.getClickCount()>1) {
      cancelSelection();
      return;
    }
    int fIdx= getFlightIdxAtPosition(e.getX(),e.getY());
    if (fIdx<0)
      return;
    if (e.getButton()==MouseEvent.BUTTON1)
      selectVariant1(fIdx);
    else
      selectVariant2(fIdx);
  }
  
  public void mousePressed(MouseEvent e) {}
  public void mouseReleased(MouseEvent e) {}
  public void mouseEntered(MouseEvent e) {
    if (hlIdx>=0) {
      hlIdx=-1;
      redraw();
    }
  }
  public void mouseExited(MouseEvent e) {
    if (hlIdx>=0) {
      hlIdx=-1;
      redraw();
    }
  }
  public void mouseDragged(MouseEvent e) {}
}

