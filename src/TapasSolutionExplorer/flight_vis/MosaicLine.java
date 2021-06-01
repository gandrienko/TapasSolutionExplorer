package TapasSolutionExplorer.flight_vis;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * Draws a horizontal or vertical line consisting of squares that may differ in their colors.
 * The squares may also have text labels, which are shown when the mouse cursor is pointing on the squares.
 * Supports selection of one or more squares by mouse-clicking.
 * Notifies listeners (implementing ItemStateListener interface) about selections.
 */

public class MosaicLine extends JPanel {
  public static final int HORIZONTAL=0, VERTICAL=1;
  public static final float mm=((float)Toolkit.getDefaultToolkit().getScreenResolution())/25.4f;
  public static Color TileBorderColor=Color.gray;
  
  public int orientation=HORIZONTAL;
  public int tileSize=Math.round(mm*3.5f);
  public int nTiles=0;
  public Color tileColors[]=null;
  public String tileLabels[]=null;
  
  public int hlIdx=-1, selIdx=-1;
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
  
  public MosaicLine(int nTiles, int orientation) {
    this.nTiles=nTiles; this.orientation=orientation;
    Dimension prefSize=(orientation==HORIZONTAL)?new Dimension(tileSize*nTiles,tileSize):
                           new Dimension(tileSize,tileSize*nTiles);
    ToolTipManager.sharedInstance().registerComponent(this);
    ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
    
    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        super.mouseClicked(e);
      }
  
      @Override
      public void mouseExited(MouseEvent e) {
        super.mouseExited(e);
      }
  
      @Override
      public void mouseMoved(MouseEvent e) {
        super.mouseMoved(e);
      }
    });
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
        //todo: mark selected and highlighted items
        return;
      }
    }
  
    if (off_Image==null || off_Image.getWidth()!=w || off_Image.getHeight()!=h)
      off_Image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = off_Image.createGraphics();
    
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
    //todo: mark selected and highlighted items
  }
}
