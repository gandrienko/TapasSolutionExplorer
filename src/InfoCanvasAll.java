import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.util.HashSet;
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

public class InfoCanvasAll extends InfoCanvasBasics implements MouseListener, MouseMotionListener {

  int sts[]=null; // Steps to Show
  protected String highlightedSector=null;
  HashSet<String> selectedSectors=new HashSet<>(dk.sectors.size());

  public InfoCanvasAll (DataKeeper dk) {
    super(dk);
    addMouseListener(this);
    addMouseMotionListener(this);
  }

  public void setSTS (int sts[]) {
    this.sts=sts;
    plotImageValid=false;
    repaint();
  }

  Vector<CellInfo> cellInfos =null;
  Vector<SectorInfo> sectorInfos=null;

  public String getToolTipText(MouseEvent me) {
    Point p = new Point(me.getX(), me.getY());
    String s="";
    for (SectorInfo si:sectorInfos)
      if (si.r.contains(p)) {
        highlightedSector=si.sector;
        plotImageValid=false;
        repaint();
        s+=si.sector;
        for (SectorData sd:dk.sectorsWithData)
          if (sd.sector.equals(si.sector)) {
            s+=", nfl = "+sd.Nflights+", Nhotspots = "+sd.Nhotspots_all+" (all), "+sd.Nhotspots_step0+" (step 0), "+sd.Nhotspots_stepLast+" (last step)";
          }
        return s;
      }
    for (CellInfo ci: cellInfos)
      if (ci.r.contains(p)) {
        String lDelays[]={"no delay","1-4 min","5-9 min","10-29 min","30-59 min","over 60 min"};
        int iDelays[][]=new int[sts.length][];
        for (int i=0; i<iDelays.length; i++)
          iDelays[i]=new int[]{dk.getCount(ci.sector,"CountFlights-noDelay",ci.interval,sts[i]),
                               dk.getCount(ci.sector,"CountFlights-Delay1to4",ci.interval,sts[i]),
                               dk.getCount(ci.sector,"CountFlights-Delay5to9",ci.interval,sts[i]),
                               dk.getCount(ci.sector,"CountFlights-Delay10to29",ci.interval,sts[i]),
                               dk.getCount(ci.sector,"CountFlights-Delay30to59",ci.interval,sts[i]),
                               dk.getCount(ci.sector,"CountFlights-DelayOver60",ci.interval,sts[i])};
        s="<html><body style=background-color:rgb(255,255,204)><p align=center>sector=<b>"+ci.sector+"</b>, capacity=<b>"+dk.capacities.get(ci.sector)+
                "</b>, interval=<b>[" +
                String.format("%02d",ci.interval/3)+":"+String.format("%02d",(ci.interval%3)*20)+".."+
                String.format("%02d",ci.interval/3+1)+":"+String.format("%02d",(ci.interval%3)*20)+
                ")</b>\n";
        s+="<table border=0 width=100%><tr align=center><td></td><td></td>";;
        for (int i=0; i<sts.length; i++)
          s+="<td>Step "+sts[i]+"</td>";
        s+="</tr>";
        s+="<tr align=center><td>Flights:</td><td></td>";
        for (int i=0; i<sts.length; i++) {
          s+="<td>";
          int demand=dk.getCount(ci.sector,"CountFlights",ci.interval,sts[i]);
          if (demand>dk.capacities.get(ci.sector))
            s+="<font color=red>"+demand+"</font> (+"+Math.round(demand*100f/dk.capacities.get(ci.sector)-100)+"%)";
          else
            s+=demand;
          s+="</td>";
        }
        s+="</tr><tr align=center><td>Delays:</td><td></td></tr>";
        for (int i=0; i<lDelays.length; i++) {
          s+="<tr align=right><td>"+lDelays[i]+"</td>";
          int rgb=255-64-32*i;
          s+="<td style=background-color:rgb("+rgb+","+rgb+","+rgb+")>.</td>\n";
          for (int j=0; j<iDelays.length; j++)
            s+="<td>"+iDelays[j][i]+"</td>";
          s+="</tr>";
        }
        s+="</table>\n";
        s+="</body></html>";

        highlightedSector=ci.sector;
        plotImageValid=false;
        repaint();
        //s+=ci.sector+", "+ci.interval;
        return s;
      }
    highlightedSector=null;
    plotImageValid=false;
    repaint();
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

    if (sectorInfos==null)
      sectorInfos=new Vector<>(dk.sectors.size());
    else
      sectorInfos.clear();
    if (cellInfos ==null)
      cellInfos =new Vector<>(dk.sectors.size()*dk.Nintervals);
    else
      cellInfos.clear();

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
    for (String sector:dk.sectorsSorted)
      if (sector.length()>maxL)
        maxL=sector.length();

    int compW=2+lblw+dk.sectorsSorted.size()*strw, compWextra=5; // width of a single component
    //if (compW*sts.length+compWextra*(sts.length-1)>getWidth()) {
      int W=(getWidth()-5*compWextra)/sts.length;
      strw=(W-(lblw-1))/dk.sectorsSorted.size();
      compW=2+lblw+dk.sectorsSorted.size()*strw;
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
      for (int i=1; i<dk.sectorsSorted.size(); i++)
        g2.drawLine(xx+lblw+1+i*strw, strh+1, xx+lblw+1+i*strw, yy[yy.length-1]+3);
      g2.setColor(Color.GRAY);
      for (int i=0; i<dk.Nintervals; i++)
        drawCenteredString(String.format("%02d", i / 3) + ":" + String.format("%02d", (i % 3) * 20),xx+1,yy[i],lblw,yy[i+1]-yy[i],g2);
      xx+=lblw+1;
      g2.setColor(new Color(0f,1f,1f,0.1f));
      g2.fillRect(xx,yy[0],compW-lblw-1,yy[yy.length-1]-yy[0]);
      g2.setColor(Color.BLACK);
      g2.drawRect(xx,yy[0],compW-lblw-1,yy[yy.length-1]-yy[0]);
      for (String sector : dk.sectorsSorted) { // new TreeSet<String>(dk.sectors)
        if (sector.equals(highlightedSector))
          g2.setColor(Color.black);
        else
          g2.setColor(Color.GRAY);
        for (int i = 0; i < sector.length(); i++)
          drawCenteredString(sector.substring(i, i + 1), xx, 2+(i+1) * strh, strw, strh, g2);
        SectorInfo si=new SectorInfo();
        si.sector=sector;
        si.step=sts[comp];
        si.r=new Rectangle(xx,0,strw,yy[0]);
        sectorInfos.add(si);
        Integer cap=dk.capacities.get(sector);
        int capacity=0;
        if (cap!=null)
          capacity=cap.intValue();
        for (int i=0; i<dk.Nintervals; i++) {
          int n=dk.getCount(sector,"CountFlights",i,sts[comp]);
          //System.out.println("sector="+sector+", interval="+String.format("%02d", i / 3) + ":" + String.format("%02d", (i % 3) * 20)+
          //        ", step="+sts[comp]+", value="+n);
          int ww=0; //(strw-1)*n/dk.iGlobalMax;
          if (selectedSectors.contains(sector)) {
            g2.setColor(new Color(0f,1f,1f,0.3f));
            g2.fillRect(xx,yy[i],strw-1,yy[i+1]-yy[i]);
          }
          if (sector.equals(highlightedSector)) {
            g2.setColor(new Color(0f,1f,1f,0.5f));
            g2.fillRect(xx,yy[i],strw-1,yy[i+1]-yy[i]);
          }
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
          cellInfos.add(ci);
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

  protected void doPopup (MouseEvent me) {
    //System.out.println("* popup "+me);
    JPopupMenu menu=new JPopupMenu();
    JMenuItem item=new JMenuItem("Sort sectors by name");
    item.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dk.sortSectors(SectorData.comparisonMode[0]);
        plotImageValid=false;
        repaint();
      }
    });
    menu.add(item);
    item=new JMenuItem("Sort sectors by N flights");
    item.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dk.sortSectors(SectorData.comparisonMode[1]);
        plotImageValid=false;
        repaint();
      }
    });
    menu.add(item);
    item=new JMenuItem("Sort sectors by N hotspots (overall)");
    item.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dk.sortSectors(SectorData.comparisonMode[2]);
        plotImageValid=false;
        repaint();
      }
    });
    menu.add(item);
    item=new JMenuItem("Sort sectors by N hotspots @ step 0");
    item.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dk.sortSectors(SectorData.comparisonMode[3]);
        plotImageValid=false;
        repaint();
      }
    });
    menu.add(item);
    item=new JMenuItem("Sort sectors by N hotspots @ last step");
    item.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dk.sortSectors(SectorData.comparisonMode[4]);
        plotImageValid=false;
        repaint();
      }
    });
    menu.add(item);
    if (selectedSectors.size()>0) {
      menu.add(new JPopupMenu.Separator());
      item=new JMenuItem("clear selection");
      item.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          selectedSectors.clear();
          dk.sortSectors(dk.sectorsWithData.elementAt(0).compMode,selectedSectors);
          plotImageValid=false;
          repaint();
        }
      });
      menu.add(item);
      JCheckBoxMenuItem cbitem=new JCheckBoxMenuItem("show only selected",dk.sectorsSorted.size()<dk.sectors.size());
      cbitem.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (cbitem.getState())
            dk.sortSectors(dk.sectorsWithData.elementAt(0).compMode,selectedSectors);
          else
            dk.sortSectors(dk.sectorsWithData.elementAt(0).compMode);
          plotImageValid=false;
          repaint();
        }
      });
      menu.add(cbitem);
    }
    menu.show(this,me.getX(),me.getY());
  }
  public void mouseEntered (MouseEvent me) {
  }
  public void mouseExited (MouseEvent me) {
    if (highlightedSector!=null) {
      highlightedSector=null;
      plotImageValid=false;
      repaint();
    }
  }
  public void mouseMoved (MouseEvent me) {
  }
  public void mouseDragged (MouseEvent me) {
  }
  public void mouseClicked (MouseEvent me) {
    if (me.getButton() == MouseEvent.BUTTON3)
      doPopup(me);
    if (me.getButton() == MouseEvent.BUTTON1) {
      for (SectorInfo si : sectorInfos)
        if (si.r.contains(me.getPoint())) {
          if (selectedSectors.contains(si.sector))
            selectedSectors.remove(si.sector);
          else
            selectedSectors.add(si.sector);
          plotImageValid = false;
          repaint();
        }
      for (CellInfo ci : cellInfos)
        if (ci.r.contains(me.getPoint())) {
          if (selectedSectors.contains(ci.sector))
            selectedSectors.remove(ci.sector);
          else
            selectedSectors.add(ci.sector);
          plotImageValid = false;
          repaint();
        }
    }
  }
  public void mousePressed (MouseEvent me) {
  }
  public void mouseReleased (MouseEvent me) {
  }

}
