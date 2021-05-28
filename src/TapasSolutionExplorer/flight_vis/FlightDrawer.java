package TapasSolutionExplorer.flight_vis;

import java.awt.*;
import java.util.ArrayList;

public class FlightDrawer {
  public static float dash1[] = {6.0f,2.0f};
  public static Stroke thickStroke=new BasicStroke(3);
  public static Stroke dashedStroke = new BasicStroke(1.0f,BasicStroke.CAP_BUTT,
      BasicStroke.JOIN_MITER,10.0f, dash1, 0.0f);
  public static Stroke thickDashedStroke = new BasicStroke(3.0f,BasicStroke.CAP_BUTT,
      BasicStroke.JOIN_MITER,10.0f, dash1, 0.0f);
  public static Color
      lineColour =new Color(128,80,0,150),
      connectLineColor=new Color(128,80,0,100),
      criticalColor=new Color(196,0,0,196),
      highlightColor=Color.yellow,
      highlightBorderColor=new Color(255,255,0,192),
      selectColor=new Color(0,0,0,70),
      selectBorderColor=new Color(0,0,0,192),
      secondSelectColor=new Color(0,0,192,70),
      secondSelectBorderColor=new Color(0,0,192,192);
  /**
   * Flight identifier
   */
  public String flightId=null;
  /**
   * Variant of the flight
   */
  public int variant=0;
  /**
   * Solution (simulation) step
   */
  public int step=0;
  /**
   * Sequence of points representing the path on the screen
   */
  public ArrayList<Point> screenPath=null;
  /**
   * For each path segments, indicates if it needs to be shown as critical
   */
  public ArrayList<Boolean> isSegmentCritical=null;
  
  public Polygon poly=null;
  
  public void clearPath() {
    if (screenPath!=null)
      screenPath.clear();
    poly=null;
    isSegmentCritical=null;
  }
  
  public void addPathSegment (int x1, int x2, int y1, int y2, boolean showAsCritical) {
    if (screenPath==null)
      screenPath=new ArrayList<Point>(50);
    screenPath.add(new Point(x1,y1));
    screenPath.add(new Point(x2,y2));
    if (showAsCritical && isSegmentCritical==null) {
      isSegmentCritical = new ArrayList<Boolean>(25);
      for (int i=0; i<screenPath.size()/2; i++)
        isSegmentCritical.add(false);
    }
    if (isSegmentCritical!=null)
      isSegmentCritical.add(showAsCritical);
  }
  
  public void setSegmentCriticality(int segmIdx,boolean critical) {
    if (critical && isSegmentCritical==null) {
      isSegmentCritical = new ArrayList<Boolean>(25);
      for (int i=0; i<screenPath.size()/2; i++)
        isSegmentCritical.add(false);
    }
    if (isSegmentCritical!=null)
      isSegmentCritical.set(segmIdx,critical);
  }
  
  public void draw (Graphics g) {
    if (screenPath==null || screenPath.isEmpty())
      return;
    Graphics2D g2d=(Graphics2D)g;
    Stroke origStroke=g2d.getStroke();
    
    boolean makePoly=poly==null;
    if (makePoly)
      poly=new Polygon();
    
    for (int k=0; k<screenPath.size(); k+=2) {
      int segmIdx=k/2;
      boolean isCritical=isSegmentCritical!=null && isSegmentCritical.get(segmIdx);
      g2d.setStroke((variant>0)?thickDashedStroke:thickStroke);
      g2d.setColor((isCritical)?criticalColor:lineColour);
      g2d.drawLine(screenPath.get(k).x,screenPath.get(k).y,screenPath.get(k+1).x,screenPath.get(k+1).y);
      g2d.setStroke((variant>0)?dashedStroke:origStroke);
      if (makePoly) {
        poly.addPoint(screenPath.get(k).x - 1, screenPath.get(k).y);
        poly.addPoint(screenPath.get(k+1).x - 1, screenPath.get(k+1).y);
      }
      if (k+2<screenPath.size()) { //draw a connecting line
        g2d.setColor(connectLineColor);
        g2d.drawLine(screenPath.get(k+1).x,screenPath.get(k+1).y,screenPath.get(k+2).x,screenPath.get(k+2).y);
        if (makePoly) {
          poly.addPoint(screenPath.get(k+1).x, screenPath.get(k+1).y);
          poly.addPoint(screenPath.get(k+2).x, screenPath.get(k+2).y);
        }
      }
    }
    g2d.setStroke(origStroke);
    if (makePoly)
      for (int k=screenPath.size()-2; k>=0; k-=2) {
        poly.addPoint(screenPath.get(k+1).x + 1, screenPath.get(k+1).y);
        poly.addPoint(screenPath.get(k).x + 1, screenPath.get(k).y);
        if (k-2>0) {
          poly.addPoint(screenPath.get(k).x, screenPath.get(k).y);
          poly.addPoint(screenPath.get(k-1).x, screenPath.get(k-1).y);
        }
      }
  }
  
  public boolean contains(int x, int y) {
    if (poly==null)
      return false;
    return poly.contains(x,y);
  }
  
  public boolean intersects(int x, int y, int w, int h) {
    if (poly==null)
      return false;
    return poly.intersects(x,y,w,h);
  }
  
  public void drawHighlighted(Graphics g) {
    if (poly==null)
      return;
    RenderingHints rh = new RenderingHints(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);
    Graphics2D g2d=(Graphics2D)g;
    g2d.setRenderingHints(rh);
    if (variant==0) {
      g.setColor(highlightColor);
      g.fillPolygon(poly);
    }
    Stroke origStroke=g2d.getStroke();
    if (variant>0)
      g2d.setStroke(dashedStroke);
    g.setColor(highlightBorderColor);
    g.drawPolygon(poly);
    if (variant>0)
      g2d.setStroke(origStroke);
  }
  
  public void drawSelected(Graphics g, boolean isSecondSelection) {
    if (poly==null)
      return;
    RenderingHints rh = new RenderingHints(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);
    Graphics2D g2d=(Graphics2D)g;
    g2d.setRenderingHints(rh);
    if (variant==0) {
      g.setColor((isSecondSelection)?secondSelectColor:selectColor);
      g.fillPolygon(poly);
    }
    Stroke origStroke=g2d.getStroke();
    if (variant>0)
      g2d.setStroke(dashedStroke);
    g.setColor((isSecondSelection)?secondSelectBorderColor:selectBorderColor);
    g.drawPolygon(poly);
    if (variant>0)
      g2d.setStroke(origStroke);
  }
}
