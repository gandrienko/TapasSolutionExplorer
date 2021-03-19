import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;

public class InfoCanvas extends JPanel {

  DataKeeper dk=null;
  String sector="";

  boolean bDoubleSpaceForHotspots=false;

  public void setbDoubleSpaceForHotspots (boolean bDoubleSpaceForHotspots) {
    this.bDoubleSpaceForHotspots = bDoubleSpaceForHotspots;
    plotImageValid=false;
    repaint();
  }

  int x0=0, y0=0, h=10, w=1, yy[]=null;

  public InfoCanvas (DataKeeper dk) {
    this.dk=dk;
    setPreferredSize(new Dimension(1500, 1200));
    setBorder(BorderFactory.createLineBorder(Color.BLUE,1));
    ToolTipManager.sharedInstance().registerComponent(this);
  }

  public void setSector (String sector) {
    this.sector=sector;
    plotImageValid=false;
    repaint();
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


  @Override
  public String getToolTipText(MouseEvent me) {
    Point p = new Point(me.getX(),me.getY());
    if (p.x>=x0 && p.x<x0+dk.Nsteps*w && p.y>=yy[0] && p.y<yy[yy.length-1]) {
      Integer cap=dk.capacities.get(sector);
      int capacity=0;
      if (cap!=null)
        capacity=cap.intValue();
      int x1=(p.x-x0)/w, y1=-1;
      for (int i=0; i<yy.length-1; i++)
        if (p.y>=yy[i] && p.y<=yy[i+1])
          y1=i;
      if (y1==-1)
        return "error";
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

  int nY=0;

  public void paintComponent (Graphics g) {
    //System.out.println("* paint...");
    super.paintComponent(g);
    nY=dk.Nintervals;
    prepareImage();
    if (plotImageValid) {
      g.drawImage(plotImage,0, 0,null);
      return;
    }
    Graphics2D g2 = (Graphics2D) g;
    if (plotImage!=null)
      g2=(Graphics2D)plotImage.getGraphics();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setColor(Color.white);
    g2.fillRect(0,0,getWidth(),getHeight());

    FontRenderContext frc = g2.getFontRenderContext();
    String str="1439";
    GlyphVector gv = g2.getFont().createGlyphVector(frc, str);
    Rectangle bounds = gv.getPixelBounds(null,0,0);
    x0=5+bounds.width;
    y0=2+bounds.height;
    if (bDoubleSpaceForHotspots)
      h=Math.max(10,(getHeight()-y0)/(dk.Nintervals+dk.NintevalsWithHotspots));
    else
      h=Math.max(10,(getHeight()-y0)/dk.Nintervals);
    if (yy==null)
      yy=new int[dk.Nintervals+1];
    w=Math.max(1,(getWidth()-x0)/dk.Nsteps);
    yy[0]=y0;
    for (int i=0; i<yy.length-1; i++)
      if (bDoubleSpaceForHotspots && dk.hasHotspots[i])
        yy[i+1]=yy[i]+2*h;
      else
        yy[i+1]=yy[i]+h;
    g2.setColor(Color.GRAY.brighter());
    for (int i=1; i<dk.Nintervals; i++)
      g2.drawLine(x0-3,yy[i], x0+w*dk.Nsteps+3, yy[i]);
    for (int i=1; i<dk.Nsteps; i+=60)
      g2.drawLine(x0+i*w, yy[0]-3, x0+i*w, yy[yy.length-1]+3);
    g2.setColor(Color.GRAY);
    for (int i=0; i<dk.Nintervals; i++)
      g2.drawString(String.format("%02d", i / 3) + ":" + String.format("%02d", (i % 3) * 20), 1, yy[i] + g2.getFontMetrics().getAscent());
    for (int i=0; i<dk.Nsteps; i+=60)
      g2.drawString(""+i,x0+i*w, g2.getFontMetrics().getAscent()-1);
    g2.setColor(new Color(0f,1f,1f,0.1f));
    g2.fillRect(x0,yy[0],w*dk.Nsteps, yy[yy.length-1]-yy[0]);
    g2.setColor(Color.GRAY);
    g2.drawRect(x0,yy[0],w*dk.Nsteps, yy[yy.length-1]-yy[0]);
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
          int hh=(yy[i+1]-yy[i]-2)*counts[i][j]/counts_max;
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
            int rgb=255-64-32*k;
            g2.setColor(new Color(rgb,rgb,rgb));
            hh=(yy[i+1]-yy[i]-2)*n[k]/counts_max;
            g2.drawLine(x0+j, yy[i+1]-hh, x0+j, yy[i+1]);

          }
          //g2.setColor(Color.darkGray);
          //g2.drawLine(x0+j, y0+(i+1)*h-hh, x0+j, y0+(i+1)*h-hh);
          //g2.drawLine(x0+j, y0+(i+1)*h, x0+j, y0+(i+1)*h);
          if (capacity<counts[i][j]) {
            hh = (yy[i+1]-yy[i] - 2) * capacity / counts_max;
            g2.setColor(Color.red);
            g2.drawLine(x0+j, yy[i+1]-hh, x0+j, yy[i+1]-hh);
          }
        }

    if (plotImage!=null) {
      //everything has been drawn to the image
      plotImageValid=true;
      // copy the image to the screen
      g.drawImage(plotImage,0, 0,null);
    }
    //System.out.println("* paint...done");
  }

  /**
   * If the specified Component  is included in a Frame (directly or
   * indirectly) or is a Frame itself, returns this Frame. Otherwise
   * returns a reference to an invisible "dummy" frame (may be needed for
   * creation of dialogs and popup windows)
   */
  public static JFrame getFrame (Component c){
    while (c!=null && !(c instanceof JFrame))
      c=c.getParent();
    return (JFrame)c;
  }

}
