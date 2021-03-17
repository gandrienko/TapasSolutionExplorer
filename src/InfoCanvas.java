import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;

public class InfoCanvas extends JPanel {

  DataKeeper dk=null;
  String sector="";

  int x0=0, y0=0, h=10, w=1;

  public InfoCanvas (DataKeeper dk) {
    this.dk=dk;
    setPreferredSize(new Dimension(1500, 1200));
    setBorder(BorderFactory.createLineBorder(Color.BLUE,1));
    ToolTipManager.sharedInstance().registerComponent(this);
  }

  public void setSector (String sector) {
    this.sector=sector;
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
      if (plotImage.getWidth(null)!=w ||
              plotImage.getHeight(null)!=h) {
        plotImage=null;
        plotImageValid=false;
      }
    if (plotImage==null && w>0 && h>0) {
      plotImage=createImage(w,h);
      plotImageValid=false;
    }
  }


  @Override
  public String getToolTipText(MouseEvent me) {
    Point p = new Point(me.getX(),me.getY());
    if (p.x>=x0 && p.x<x0+nX*w && p.y>=y0 && p.y<y0+h*nY) {
      Integer cap=dk.capacities.get(sector);
      int capacity=0;
      if (cap!=null)
        capacity=cap.intValue();
      int x1=(p.x-x0)/w, y1=(p.y-y0)/h;
      String out="<html><body>sector=<b>"+sector+"</b>, capacity="+capacity+"<br>step=<b>" + x1 + "</b>, interval=<b>[" +
                    String.format("%02d",y1/3)+":"+String.format("%02d",(y1%3)*20)+".."+
                    String.format("%02d",y1/3+1)+":"+String.format("%02d",(y1%3)*20)+
                    ")</b>, Nflights=<b>";
      int demand=dk.getCount("CountFlights",y1,x1);
      if (demand>capacity) {
        out+="<font color=red>"+demand+"</font> (+"+Math.round(demand*100f/capacity-100)+"%)";
      }
      else
        out+=demand;
      out+="</b>";
      out+="<table border=0.5>";
      int n=dk.getCount("CountFlights-noDelay",y1,x1);
      out+="<tr align=right><td>Flights w/out delays</td><td>"+n+"</td></tr>";
      n=dk.getCount("CountFlights-Delay1to4",y1,x1);
      out+="<tr align=right><td>delays 1..4 min</td><td>"+n+"</td></tr>";
      n=dk.getCount("CountFlights-Delay5to9",y1,x1);
      out+="<tr align=right><td>delays 5..9 min</td><td>"+n+"</td></tr>";
      n=dk.getCount("CountFlights-Delay10to29",y1,x1);
      out+="<tr align=right><td>delays 10..29 min</td><td>"+n+"</td></tr>";
      n=dk.getCount("CountFlights-Delay30to59",y1,x1);
      out+="<tr align=right><td>delays 30..59 min</td><td>"+n+"</td></tr>";
      n=dk.getCount("CountFlights-DelayOver60",y1,x1);
      out+="<tr align=right><td>delays over 60 min</td><td>"+n+"</td></tr>";
      out+="</table>";
      out+="</body></html>";
      return out;
    }
    else
      return super.getToolTipText();
  }

  int nX=1440, nY=70;

  public void paintComponent (Graphics g) {
    super.paintComponent(g);
    prepareImage();
    if (plotImageValid) {
      g.drawImage(plotImage,0, 0,null);
      return;
    }
    Graphics2D g2 = (Graphics2D) g;
    if (plotImage!=null)
      g2=(Graphics2D)plotImage.getGraphics();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    FontRenderContext frc = g2.getFontRenderContext();
    String str="1439";
    GlyphVector gv = g2.getFont().createGlyphVector(frc, str);
    Rectangle bounds = gv.getPixelBounds(null,0,0);
    x0=5+bounds.width;
    y0=2+bounds.height;
    h=Math.max(10,(getHeight()-y0)/nY);
    w=Math.max(1,(getWidth()-x0)/nX);
    g2.setColor(Color.GRAY);
    for (int i=0; i<nY; i++)
      g2.drawString(String.format("%02d",i/3)+":"+String.format("%02d",(i%3)*20), 1, y0+i*h+g2.getFontMetrics().getAscent());
    for (int i=0; i<nX; i+=60)
      g2.drawString(""+i,x0+i*w, g2.getFontMetrics().getAscent()-1);
    g2.setColor(Color.GRAY.brighter());
    for (int i=1; i<nY; i++)
      g2.drawLine(x0-3,y0+i*h, x0+w*nX+3, y0+i*h);
    for (int i=1; i<nX; i+=60)
      g2.drawLine(x0+i*w, y0-3, x0+i*w, y0+nY*h+3);
    g2.setColor(Color.GRAY);
    g2.drawRect(x0,y0,w*nX, h*nY);
    //draw values
    g2.setColor(Color.darkGray);
    int counts[][]=dk.getCounts("CountFlights"),
        counts_max=dk.getMax(counts);
    Integer cap=dk.capacities.get(sector);
    int capacity=0;
    if (cap!=null)
      capacity=cap.intValue();

    for (int i=0; i<counts.length; i++)
      for (int j=0; j<counts[i].length; j++)
        if (counts[i][j]>0) {
          int hh=(h-2)*counts[i][j]/counts_max;
          //g2.setColor(Color.gray);
          //g2.drawLine(x0+j, y0+(i+1)*h-hh, x0+j, y0+(i+1)*h);
          int n[]=new int[6];
          n[0]=dk.getCount("CountFlights-noDelay",i,j);
          n[1]=n[0]+dk.getCount("CountFlights-Delay1to4",i,j);
          n[2]=n[1]+dk.getCount("CountFlights-Delay5to9",i,j);
          n[3]=n[2]+dk.getCount("CountFlights-Delay10to29",i,j);
          n[4]=n[3]+dk.getCount("CountFlights-Delay30to59",i,j);
          n[5]=n[4]+dk.getCount("CountFlights-DelayOver60",i,j);
          for (int k=5; k>=0; k--) {
            int rgb=255-32*k;
            g2.setColor(new Color(rgb,rgb,rgb));
            hh=(h-2)*n[k]/counts_max;
            g2.drawLine(x0+j, y0+(i+1)*h-hh, x0+j, y0+(i+1)*h);

          }
          //g2.setColor(Color.darkGray);
          //g2.drawLine(x0+j, y0+(i+1)*h-hh, x0+j, y0+(i+1)*h-hh);
          //g2.drawLine(x0+j, y0+(i+1)*h, x0+j, y0+(i+1)*h);
          if (capacity<counts[i][j]) {
            hh = (h - 2) * capacity / counts_max;
            g2.setColor(Color.red);
            g2.drawLine(x0+j, y0+(i+1)*h-hh, x0+j, y0+(i+1)*h-hh);
          }
        }
    if (plotImage!=null) {
      //everything has been drawn to the image
      plotImageValid=true;
      // copy the image to the screen
      g.drawImage(plotImage,0, 0,null);
    }
  }

}
