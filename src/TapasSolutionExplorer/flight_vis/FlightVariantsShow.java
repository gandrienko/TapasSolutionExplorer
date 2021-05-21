package TapasSolutionExplorer.flight_vis;

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

public class FlightVariantsShow extends JPanel implements MouseListener, MouseMotionListener {
  public static final int secondsInDay =86400, minutesInDay=1440;
  public static float dash1[] = {10.0f,5.0f};
  public static Stroke dashedStroke = new BasicStroke(1.0f,BasicStroke.CAP_BUTT,
      BasicStroke.JOIN_MITER,10.0f, dash1, 0.0f);
  public static Color
      sectorColor =new Color(128,70,0, 196),
      sectorBkgColor =new Color(255,165,0,30);
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
  /**
   * The sequence of sectors visited by the flight variants;
   * includes all sectors present in at least one variant.
   */
  protected ArrayList<String> sectorSequence=null;
  /**
   * Used for drawing and calculating positions for times
   */
  protected int tMarg=10, tWidth=0;
  protected int yMarg=0, plotH=0,
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
   * Index of highlighted variant
   */
  protected int hlIdx=-1;
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
  }
  
  public boolean showFlightVariants(String flId) {
    hlIdx=-1;
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
    hlIdx=-1;
    if (flights==null || shownFlightIdx<0 || sectorSequence==null || sectorSequence.isEmpty()) {
      flightDrawers=null;
      return;
    }
    if (flightDrawers==null || flightDrawers.length!=flights[shownFlightIdx].length) {
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
  
    int secH=(plotH-(sectorSequence.size()-1)*vSpace)/sectorSequence.size();
  
    int y=yMarg;
    for (int i=0; i<sectorSequence.size(); i++) {
      g.setColor(sectorBkgColor);
      g.fillRect(0,y,w,secH);
      g.setColor(sectorColor);
      g.drawLine(0,y,w,y);
      g.drawLine(0,y+secH,w,y+secH);
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
    if (hlIdx>=0)
      flightDrawers[hlIdx].drawHighlighted(gr);
  }
  
  public void redraw() {
    if (isShowing())
      paintComponent(getGraphics());
  }
  
  //---------------------- MouseListener ----------------------------------------
  
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
  
  public void mouseClicked(MouseEvent e) {}
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

