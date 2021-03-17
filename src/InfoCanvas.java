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
    Graphics2D g2 = (Graphics2D) g;
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
    g.setColor(Color.darkGray);
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
          g.setColor(Color.gray);
          g.drawLine(x0+j, y0+(i+1)*h-hh, x0+j, y0+(i+1)*h);
          g.setColor(Color.darkGray);
          g.drawLine(x0+j, y0+(i+1)*h-hh, x0+j, y0+(i+1)*h-hh);
          g.drawLine(x0+j, y0+(i+1)*h, x0+j, y0+(i+1)*h);
          if (capacity<counts[i][j]) {
            hh = (h - 2) * capacity / counts_max;
            g.setColor(Color.red);
            g.drawLine(x0+j, y0+(i+1)*h-hh, x0+j, y0+(i+1)*h-hh);
          }
        }
  }
}
