import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.util.TreeSet;

public class InfoCanvasAll extends InfoCanvasBasics {

  int sts[]=null; // Steps to Show


  public InfoCanvasAll (DataKeeper dk) {
    super(dk);
  }

  public void setSTS (int sts[]) {
    this.sts=sts;
    plotImageValid=false;
    repaint();
  }

  protected void paintSingleStep (int step, Graphics2D g2, int x, int y, int w, int h) {

  }

  public void paintComponent (Graphics g) {
    //super.paintComponent(g);
    //nY=dk.Nintervals;

    prepareImage();
    if (plotImageValid) {
      g.drawImage(plotImage, 0, 0, null);
      return;
    }
    Graphics2D g2 = (Graphics2D) g;
    if (plotImage != null)
      g2 = (Graphics2D) plotImage.getGraphics();

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setColor(Color.white);
    g2.fillRect(0, 0, getWidth(), getHeight());

    FontRenderContext frc = g2.getFontRenderContext();
    GlyphVector gv = g2.getFont().createGlyphVector(frc, "00:00");
    Rectangle bounds = gv.getPixelBounds(null,0,0);
    int lblw=bounds.width+1;
    gv = g2.getFont().createGlyphVector(frc, "W");
    bounds = gv.getPixelBounds(null,0,0);
    int strw=bounds.width+1, strh=bounds.height+1;
    int maxL=0;
    for (String sector:dk.sectors)
      if (sector.length()>maxL)
        maxL=sector.length();

    g2.setColor(Color.BLACK);
    int xx=lblw;
    for (String sector:new TreeSet<String>(dk.sectors)) {
      for (int i=0; i<sector.length(); i++)
        drawCenteredString(sector.substring(i,i+1),xx,i*strh,strw,strh,g2);
      xx+=strw;
    }
    //g2.drawLine(10,10,20,20);

    if (plotImage!=null) {
      //everything has been drawn to the image
      plotImageValid=true;
      // copy the image to the screen
      g.drawImage(plotImage,0, 0,null);
    }
  }

}
