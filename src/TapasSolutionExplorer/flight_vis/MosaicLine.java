package TapasSolutionExplorer.flight_vis;

import TapasSolutionExplorer.UI.ChangeNotifier;
import TapasSolutionExplorer.UI.SingleHighlightManager;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Draws a horizontal or vertical line consisting of squares that may differ in their colors.
 * The squares may also have text labels, which are shown when the mouse cursor is pointing on the squares.
 * Supports selection of one or more squares by mouse-clicking.
 * Notifies listeners (implementing ChangeListener interface) about selections.
 */

public class MosaicLine extends JPanel implements ChangeListener {
  public static final int HORIZONTAL=0, VERTICAL=1;
  public static final float mm=((float)Toolkit.getDefaultToolkit().getScreenResolution())/25.4f;
  public static Color TileBorderColor=Color.gray,
      markColor=new Color(255,255,255,160),
      markBorderColor=new Color(255,255,255,192),
      highlightColor=new Color(255,255,0,160),
      highlightBorderColor=new Color(255,255,0,192);
  public static float dash[]={2f,1.0f};
  public static Stroke thickStroke=new BasicStroke(1.5f);
  public static Stroke thickDashedStroke = new BasicStroke(1.5f,BasicStroke.CAP_BUTT,
      BasicStroke.JOIN_MITER,10.0f, dash, 0.0f);
  
  public int orientation=HORIZONTAL;
  public int tileSize=Math.round(mm*3.5f);
  public int nTiles=0;
  public Color tileColors[]=null;
  public String tileLabels[]=null;
  
  public int hlIdx=-1, selIdx=-1, selIdx2=-1, markedIdx=-1;
  /**
   * Position of the first drawn tile (upper left corner)
   */
  protected int tileX0=-1, tileY0=-1;
  /**
   * Actual sizes of the drawn tiles
   */
  protected int tileW=0, tileH=0;
  /**
   * Used to speed up redrawing
   */
  protected BufferedImage off_Image=null;
  protected boolean off_Valid=false;
  
  protected ArrayList<ChangeListener> changeListeners=null;
  /**
   * Supports simultaneous highlighting of flight versions and/or corresponding steps
   * in this component and other components
   */
  protected SingleHighlightManager stepHighlighter=null;
  
  public MosaicLine(int nTiles, int orientation) {
    this.nTiles=nTiles; this.orientation=orientation;
    Dimension prefSize=(orientation==HORIZONTAL)?new Dimension(tileSize*nTiles+6,tileSize+6):
                           new Dimension(tileSize+6,tileSize*nTiles+6);
    setPreferredSize(prefSize);
    setBackground(Color.lightGray);
    
    ToolTipManager.sharedInstance().registerComponent(this);
    ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
    
    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount()>1)
          return;
        int idx=getTileIdxAtPosition(e.getX(),e.getY());
        if (idx<0)
          return;
        if (e.getButton()==MouseEvent.BUTTON1)
          selIdx=(idx==selIdx)?-1:idx;
        else
          selIdx2=(idx==selIdx2)?-1:idx;
        if (selIdx<0)
          selIdx=selIdx2;
        if (selIdx2==selIdx)
          selIdx2=-1;
        notifyChange();
        redraw();
      }
  
      @Override
      public void mouseExited(MouseEvent e) {
        clearHighlighting();
      }
    });
    
    addMouseMotionListener(new MouseAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        int idx=getTileIdxAtPosition(e.getX(),e.getY());
        if (idx<0)
          clearHighlighting();
        else
          highightStep(idx);
      }
    });
  }
  
  public void addChangeListener(ChangeListener l) {
    if (changeListeners==null)
      changeListeners=new ArrayList(5);
    if (!changeListeners.contains(l))
      changeListeners.add(l);
  }
  
  public void removeChangeListener(ChangeListener l) {
    if (l!=null && changeListeners!=null)
      changeListeners.remove(l);
  }
  
  public void notifyChange(){
    if (changeListeners==null || changeListeners.isEmpty())
      return;
    ChangeEvent e=new ChangeEvent(this);
    for (ChangeListener l:changeListeners)
      l.stateChanged(e);
  }
  
  public void setOrientation(int orientation) {
    if (this.orientation == orientation)
      return;
    this.orientation = orientation;
    fullRedraw();
  }
  
  public void setTileSize(int tileSize) {
    if (this.tileSize == tileSize)
      return;
    this.tileSize = tileSize;
    fullRedraw();
  }
  
  public void setNTiles(int nTiles) {
    if (this.nTiles == nTiles)
      return;
    this.nTiles = nTiles;
    fullRedraw();
  }
  
  public void setTileColors(Color[] tileColors) {
    this.tileColors = tileColors;
    fullRedraw();
  }
  
  public void setTileColor(int idx, Color color) {
    if (idx<0 || idx>=nTiles)
      return;
    if (tileColors==null) {
      tileColors=new Color[nTiles];
      for (int i=0; i<nTiles; i++)
        tileColors[i]=(i%2==0)?Color.orange:Color.blue;
    }
    tileColors[idx]=color;
  }
  
  public void setTileLabels(String[] tileLabels) {
    this.tileLabels = tileLabels;
  }
  
  public void setTileLabel(int idx, String label) {
    if (idx<0 || idx>=nTiles)
      return;
    if (tileLabels==null) {
      tileLabels=new String[nTiles];
      for (int i=0; i<nTiles; i++)
        tileLabels[i]=Integer.toString(i);
    }
    tileLabels[idx]=label;
  }
  
  public SingleHighlightManager getStepHighlighter() {
    return stepHighlighter;
  }
  
  public void setStepHighlighter(SingleHighlightManager stepHighlighter) {
    this.stepHighlighter = stepHighlighter;
    if (stepHighlighter!=null)
      stepHighlighter.addChangeListener(this);
  }
  
  public void highightStep(int idx){
    if (stepHighlighter!=null)
      stepHighlighter.highlight(new Integer(idx));
    else
      if (hlIdx!=idx) {
        hlIdx=idx;
        redraw();
      }
  }
  
  public void clearHighlighting(){
    if (stepHighlighter!=null)
      stepHighlighter.clearHighlighting();
    else
      if (hlIdx>=0) {
        hlIdx=-1;
        redraw();
      }
  }
  
  public void stateChanged(ChangeEvent e) {
    if (e.getSource().equals(stepHighlighter))
      if (stepHighlighter.getHighlighted()==null) {
        if (hlIdx>=0) {
          hlIdx=-1;
          redraw();
        }
      }
      else {
        int idx=((Integer)stepHighlighter.getHighlighted()).intValue();
        if (idx>=0 && idx<nTiles && idx!=hlIdx) {
          hlIdx=idx;
          redraw();
        }
      }
  }
  
  public void setMarkedIdx(int idx) {
    if (markedIdx==idx)
      return;
    markedIdx=idx;
    redraw();
  }
  
  protected void cancelSelection() {
    if (selIdx >=0 || selIdx2>=0) {
      selIdx =-1; selIdx2=-1;
      notifyChange();
      redraw();
    }
  }
  
  public int getSelectedIndex(boolean primary) {
    return (primary)?selIdx:selIdx2;
  }
  
  public int getTileIdxAtPosition(int x, int y) {
    int idx=(orientation==HORIZONTAL)?(x-tileX0)/tileW:(y-tileY0)/tileH;
    if (idx<0 || idx>=nTiles)
      return -1;
    return idx;
  }
  
  public String getToolTipText(MouseEvent me) {
    if (!isShowing())
      return null;
    if (me.getButton() != MouseEvent.NOBUTTON)
      return null;
    int idx=getTileIdxAtPosition(me.getX(),me.getY());
    if (idx<0)
      return null;
    if (tileLabels!=null && idx<tileLabels.length)
      return tileLabels[idx];
    return Integer.toString(idx);
  }
  
  public void redraw() {
    if (isShowing())
      paintComponent(getGraphics());
  }
  
  public void fullRedraw() {
    off_Valid=false;
    redraw();;
  }
  
  public int getXPosForTile(int idx) {
    if (orientation==HORIZONTAL)
      return tileX0+idx*tileW;
    return tileX0;
  }
  
  public int getYPosForTile(int idx) {
    if (orientation==HORIZONTAL)
      return tileY0;
    return tileY0+idx*tileH;
  }
  
  protected void drawMarked(Graphics g) {
    if (markedIdx<0)
      return;
    int x=getXPosForTile(markedIdx), y=getYPosForTile(markedIdx);
    Graphics2D g2d=(Graphics2D)g;
    g2d.setColor(markColor);
    g2d.fillRect(x,y,tileW,tileH);
    Stroke stroke=g2d.getStroke();
    g2d.setStroke(thickStroke);
    g2d.setColor(markBorderColor);
    g2d.drawRect(x,y,tileW,tileH);
    g2d.setStroke(stroke);
  }
  
  protected void drawSelected(Graphics g) {
    if (selIdx<0 && selIdx2<0)
      return;
    Graphics2D g2d=(Graphics2D)g;
    Stroke stroke=g2d.getStroke();
    if (selIdx>=0) {
      g2d.setStroke(thickStroke);
      g2d.setColor(Color.darkGray);
      g2d.drawRect(getXPosForTile(selIdx),getYPosForTile(selIdx),tileW,tileH);
    }
    if (selIdx2>=0) {
      g2d.setStroke(thickDashedStroke);
      g2d.setColor(Color.darkGray);
      g2d.drawRect(getXPosForTile(selIdx2),getYPosForTile(selIdx2),tileW,tileH);
    }
    g2d.setStroke(stroke);
  }
  
  protected void drawHighlighted(Graphics g) {
    if (hlIdx<0)
      return;
    int x=getXPosForTile(hlIdx), y=getYPosForTile(hlIdx);
    Graphics2D g2d=(Graphics2D)g;
    g2d.setColor(highlightColor);
    g2d.fillRect(x,y,tileW,tileH);
    g2d.setColor(highlightBorderColor);
    g2d.drawRect(x,y,tileW,tileH);
  }
  
  public void paintComponent(Graphics gr) {
    if (nTiles<1)
      return;
    int w=getWidth(), h=getHeight();
    if (w<1 || h<1)
      return;
    if (off_Image!=null && off_Valid) {
      if (off_Image.getWidth()!=w || off_Image.getHeight()!=h) {
        off_Image = null; off_Valid=false;
      }
      else {
        gr.drawImage(off_Image,0,0,null);
        drawMarked(gr);
        drawSelected(gr);
        drawHighlighted(gr);
        return;
      }
    }
  
    if (off_Image==null || off_Image.getWidth()!=w || off_Image.getHeight()!=h)
      off_Image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = off_Image.createGraphics();
    
    g.setColor(getBackground());
    g.fillRect(0,0,w+1,h+1);
    
    tileW=(orientation==HORIZONTAL)?Math.min(tileSize,w/nTiles):Math.min(tileSize,w);
    if (tileW<3) tileW=3;
    tileH=(orientation==HORIZONTAL)?Math.min(tileSize,h):Math.min(tileSize,h/nTiles);
    if (tileH<3) tileH=3;
    
    int x=(orientation==HORIZONTAL)?(w-nTiles*tileW)/2:(w-tileW)/2,
        y=(orientation==HORIZONTAL)?(h-tileH)/2:(h-nTiles*tileH)/2;
    tileX0=x; tileY0=y;
    
    for (int i=0; i<nTiles; i++) {
      if (tileColors!=null && i<tileColors.length)
        g.setColor(tileColors[i]);
      else
        g.setColor((i%2==0)?Color.orange:Color.blue);
      g.fillRect(x,y,tileW,tileH);
      g.setColor(TileBorderColor);
      g.drawRect(x,y,tileW,tileH);
      if (orientation==HORIZONTAL)
        x+=tileW;
      else
        y+=tileH;
    }
    off_Valid=true;
    gr.drawImage(off_Image,0,0,null);
    drawMarked(gr);
    drawSelected(gr);
    drawHighlighted(gr);
  }
}
