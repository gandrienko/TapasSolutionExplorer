import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.util.TreeSet;
import java.util.Vector;

class CellInfo {
  Rectangle r;
  String sector;
  int step, interval;
}

class SectorInfo {
  Rectangle r;
  String sector;
  int step;
}

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

  Vector<CellInfo> cells=null;
  Vector<SectorInfo> sectors=null;

  public String getToolTipText(MouseEvent me) {
    Point p = new Point(me.getX(), me.getY());
    String s="";
    for (SectorInfo sector:sectors)
      if (sector.r.contains(p)) {
        s+=sector.sector;
        return s;
      }
    for (CellInfo cell:cells)
      if (cell.r.contains(p)) {
        s+=cell.sector+", "+cell.interval;
        return s;
      }
    return s;
  }

  int yy[]=null, y0=0;

  public void paintComponent (Graphics g) {
    super.paintComponent(g);

    prepareImage();
    if (plotImageValid) {
      g.drawImage(plotImage, 0, 0, null);
      return;
    }
    Graphics2D g2 = (Graphics2D) g;
    if (plotImage != null)
      g2 = (Graphics2D) plotImage.getGraphics();

    if (sectors==null)
      sectors=new Vector<>(dk.sectors.size());
    else
      sectors.clear();
    if (cells==null)
      cells=new Vector<>(dk.sectors.size()*dk.Nintervals);
    else
      cells.clear();

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

    int compW=2+lblw+dk.sectors.size()*strw, compWextra=5; // width of a single component
    //if (compW*sts.length+compWextra*(sts.length-1)>getWidth()) {
      int W=(getWidth()-5*compWextra)/sts.length;
      strw=(W-(lblw-1))/dk.sectors.size();
      compW=2+lblw+dk.sectors.size()*strw;
    //}

    y0=3+(maxL+1)*strh;
    int h=(getHeight()-(y0+1))/dk.Nintervals;
    if (yy==null)
      yy=new int[dk.Nintervals+1];
    yy[0]=y0;
    for (int i=0; i<yy.length-1; i++)
      yy[i+1]=yy[i]+h;

    for (int comp=0; comp<sts.length; comp++) {
      g2.setColor(Color.BLACK);
      int xx = (compW+compWextra)*comp;
      drawCenteredString("step "+sts[comp],xx,0,compW,strh,g2);
      g2.drawLine(xx,strh+1,xx+compW,strh+1);
      g2.setColor(Color.GRAY.brighter());
      for (int i=1; i<dk.Nintervals; i++)
        g2.drawLine(xx,yy[i], xx+compW, yy[i]);
      for (int i=1; i<dk.sectors.size(); i++)
        g2.drawLine(xx+lblw+1+i*strw, strh+1, xx+lblw+1+i*strw, yy[yy.length-1]+3);
      g2.setColor(Color.GRAY);
      for (int i=0; i<dk.Nintervals; i++)
        drawCenteredString(String.format("%02d", i / 3) + ":" + String.format("%02d", (i % 3) * 20),xx+1,yy[i],lblw,yy[i+1]-yy[i],g2);
      xx+=lblw+1;
      g2.setColor(new Color(0f,1f,1f,0.1f));
      g2.fillRect(xx,yy[0],compW-lblw-1,yy[yy.length-1]-yy[0]);
      g2.setColor(Color.BLACK);
      g2.drawRect(xx,yy[0],compW-lblw-1,yy[yy.length-1]-yy[0]);
      for (String sector : new TreeSet<String>(dk.sectors)) {
        g2.setColor(Color.GRAY);
        for (int i = 0; i < sector.length(); i++)
          drawCenteredString(sector.substring(i, i + 1), xx, 2+(i+1) * strh, strw, strh, g2);
        SectorInfo si=new SectorInfo();
        si.sector=sector;
        si.step=sts[comp];
        si.r=new Rectangle(xx,0,strw,yy[0]);
        sectors.add(si);
        Integer cap=dk.capacities.get(sector);
        int capacity=0;
        if (cap!=null)
          capacity=cap.intValue();
        for (int i=0; i<dk.Nintervals; i++) {
          int n=dk.getCount(sector,"CountFlights",i,sts[comp]);
          //System.out.println("sector="+sector+", interval="+String.format("%02d", i / 3) + ":" + String.format("%02d", (i % 3) * 20)+
          //        ", step="+sts[comp]+", value="+n);
          int ww=0; //(strw-1)*n/dk.iGlobalMax;
          switch (iRenderingMode) {
            case 0:
              //n=dk.getCount(sector,"CountFlights",i,sts[comp]);
              ww=(strw-1)*n/dk.iGlobalMax;
              g2.setColor(Color.gray);
              g2.fillRect(xx,yy[i],ww,yy[i+1]-yy[i]);
              break;
            case 1:
              int nn[]=new int[6];
              nn[0]=dk.getCount(sector,"CountFlights-noDelay",i,sts[comp]);
              nn[1]=nn[0]+dk.getCount(sector,"CountFlights-Delay1to4",i,sts[comp]);
              nn[2]=nn[1]+dk.getCount(sector,"CountFlights-Delay5to9",i,sts[comp]);
              nn[3]=nn[2]+dk.getCount(sector,"CountFlights-Delay10to29",i,sts[comp]);
              nn[4]=nn[3]+dk.getCount(sector,"CountFlights-Delay30to59",i,sts[comp]);
              nn[5]=nn[4]+dk.getCount(sector,"CountFlights-DelayOver60",i,sts[comp]);
              for (int k=nn.length-1; k>=0; k--) {
                int rgb=255-64-32*k;
                g2.setColor(new Color(rgb,rgb,rgb));
                ww=(strw-1)*nn[k]/dk.iGlobalMax;
                g2.fillRect(xx,yy[i],ww,yy[i+1]-yy[i]);
              }
              break;
          }
          g2.setColor(Color.gray);
          g2.fillRect(xx,yy[i],ww,yy[i+1]-yy[i]);
          if (n>capacity) {
            g2.setColor(Color.red);
            ww=(strw-1)*capacity/dk.iGlobalMax;
            g2.drawLine(xx+ww,yy[i],xx+ww,yy[i+1]);
          }
          CellInfo ci=new CellInfo();
          ci.sector=sector;
          ci.step=sts[comp];
          ci.interval=i;
          ci.r=new Rectangle(xx,yy[i],strw,yy[i+1]-yy[i]);
          cells.add(ci);
        }
        xx += strw;
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
