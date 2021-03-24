import javax.swing.*;
import java.awt.*;

public class InfoCanvasBasics extends JPanel {

  static public String RenderingModes[]={"none","delays","sectors (from)","sectors (to)"};
  int iRenderingMode=1;

  DataKeeper dk=null;

  boolean bDoubleSpaceForHotspots=false;

  public void setbDoubleSpaceForHotspots (boolean bDoubleSpaceForHotspots) {
    this.bDoubleSpaceForHotspots = bDoubleSpaceForHotspots;
    plotImageValid=false;
    repaint();
  }

  public InfoCanvasBasics (DataKeeper dk) {
    this.dk=dk;
    setPreferredSize(new Dimension(1500, 900));
    setBorder(BorderFactory.createLineBorder(Color.BLUE,1));
    ToolTipManager.sharedInstance().registerComponent(this);
  }

  public void setRenderingModes (String renderingMode) {
    int k=-1;
    for (int i=0; i<RenderingModes.length && k==-1; i++)
      if (RenderingModes[i].equals(renderingMode))
        k=i;
    if (k!=-1 && iRenderingMode!=k) {
      iRenderingMode=k;
      plotImageValid=false;
      repaint();
    }
  }

  /**
   * The image with the whole plot, which is used for the optimisation of the drawing
   * (helps in case of many objects).
   */
  protected Image plotImage=null;
  /**
   * Indicates whether the full image is valid
   */
  protected boolean plotImageValid=false;

  public void prepareImage () {
    Dimension size=getSize();
    if (size==null) return;
    int w=size.width, h=size.height;
    if (w<20 || h<20) return;
    int y0=0;
    if (plotImage!=null)
      if (plotImage.getWidth(null)!=w || plotImage.getHeight(null)!=h) {
        plotImage=null;
        plotImageValid=false;
      }
    if (plotImage==null && w>0 && h>0) {
      plotImage=createImage(w,h);
      plotImageValid=false;
    }
  }

  public void drawCenteredString(String s, int x0, int y0, int w, int h, Graphics g) {
    FontMetrics fm = g.getFontMetrics();
    int x = (w - fm.stringWidth(s)) / 2;
    int y = (fm.getAscent() + (h - (fm.getAscent() + fm.getDescent())) / 2);
    g.drawString(s, x0+x, y0+y);
  }

}
