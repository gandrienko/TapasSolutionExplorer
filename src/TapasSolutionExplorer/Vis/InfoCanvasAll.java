package TapasSolutionExplorer.Vis;

import TapasDataReader.Flight;
import TapasDataReader.Record;
import TapasSolutionExplorer.Data.SectorData;
import TapasSolutionExplorer.Data.DataKeeper;
import TapasSolutionExplorer.UI.ControlPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.util.HashSet;
import java.util.Hashtable;
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
  protected int highlightedInterval=-1;
  protected boolean bHideSectorsWithUndefinedCapacity=true;
  HashSet<String> selectedSectors=new HashSet<>(dk.getSectors().size());

  protected int hotspotMode=0,  // 0: by entries, 1: by presence
                hotspotRatio=0; // 0: ratio=1.1; 1: ratio=0;

  protected Vector<JFrame> children=new Vector<>(10);

  protected void killChildren() {
    for (JFrame fr:children)
      fr.dispose();
  }

  public InfoCanvasAll (DataKeeper dk) {
    super(dk);
    addMouseListener(this);
    addMouseMotionListener(this);
  }

  public void setHotspotMode (int hotspotMode) {
    this.hotspotMode=hotspotMode;
    killChildren();
    plotImageValid=false;
    repaint();
  }

  public void setHotspotRatio (int hotspotRatio) {
    this.hotspotRatio=hotspotRatio;
    killChildren();
    plotImageValid=false;
    repaint();
  }

  public void setHideSectorsWithUndefinedCapacity (boolean bHideSectorsWithUndefinedCapacity) {
    this.bHideSectorsWithUndefinedCapacity=bHideSectorsWithUndefinedCapacity;
    dk.sortSectors(dk.sectorsWithData.elementAt(0).compMode,selectedSectors);
    plotImageValid=false;
    repaint();
  }

  public void setSTS (int sts[]) {
    this.sts=sts;
    dk.calcMaxForSelectedSteps(sts);
    plotImageValid=false;
    repaint();
  }
  public HashSet<Integer> getSTS() {
    HashSet<Integer> hSTS=new HashSet<>(sts.length);
    for (int i=0; i<sts.length; i++)
      hSTS.add(new Integer(sts[i]));
    return hSTS;
  }

  Vector<CellInfo> cellInfos =null;
  Vector<SectorInfo> sectorInfos=null;

  public String getToolTipText(MouseEvent me) {
    Point p = new Point(me.getX(), me.getY());
    String s="";
    for (SectorInfo si:sectorInfos)
      if (si.r.contains(p)) {
        highlightedSector=si.sector;
        highlightedInterval=-1;
        plotImageValid=false;
        repaint();
        s+=si.sector;
        for (SectorData sd:dk.sectorsWithData)
          if (sd.sector.equals(si.sector)) {
            String lDelays[]={"no delay","1-4 min","5-9 min","10-29 min","30-59 min","over 60 min"};
            int iDelays[][]=new int[sts.length][];
            for (int i=0; i<iDelays.length; i++)
              iDelays[i]=new int[]{dk.getCount(si.sector,"CountFlights-noDelay",sts[i]),
                      dk.getCount(si.sector,"CountFlights-Delay1to4",sts[i]),
                      dk.getCount(si.sector,"CountFlights-Delay5to9",sts[i]),
                      dk.getCount(si.sector,"CountFlights-Delay10to29",sts[i]),
                      dk.getCount(si.sector,"CountFlights-Delay30to59",sts[i]),
                      dk.getCount(si.sector,"CountFlights-DelayOver60",sts[i])};
            s="<html><body style=background-color:rgb(255,255,204)><p align=center>sector=<b>"+si.sector+"</b>, capacity=<b>"+dk.capacities.get(si.sector)+
                    "</b>\n";
            s+="<table border=0 width=100%><tr align=center><td></td><td></td>";
            for (int i=0; i<sts.length; i++)
              s+="<td>Step# "+sts[i]+((dk.stepLabels==null)?"":" ("+dk.stepLabels[sts[i]]+")")+"</td>";
            s+="</tr>";
            s+="<tr align=center><td>Hotspots:</td><td></td>";
            for (int i=0; i<sts.length; i++)
              s+="<td>"+dk.getCountHotspots(si.sector,sts[i])+"</td>";
            s+="</tr>";
            s+="<tr align=center><td>Flights:</td><td></td>";
            for (int i=0; i<sts.length; i++)
              s+="<td>"+dk.getCount(si.sector,"CountFlights",sts[i])+"</td>";
            s+="</tr>";
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
        s+="<table border=0 width=100%><tr align=center><td></td><td></td>";
        for (int i=0; i<sts.length; i++)
          s+="<td>Step# "+sts[i]+((dk.stepLabels==null)?"":" ("+dk.stepLabels[sts[i]]+")")+"</td>";
        s+="</tr>";
        s+="<tr align=center><td>Flights:</td><td></td>";
        for (int i=0; i<sts.length; i++) {
          s+="<td>";
          int demand=dk.getCount(ci.sector,"CountFlights",ci.interval,sts[i]);
          float capacity=dk.capacities.get(ci.sector);
          if (hotspotRatio==0)
            capacity=1.1f*capacity;
          if (demand>capacity)
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
        highlightedInterval=ci.interval;
        plotImageValid=false;
        repaint();
        /* // flights in the cell: dump to the clipboard
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection("Sector="+ci.sector+" step="+ci.step+" interval="+ci.interval+"\n"+dk.getListOfFlightsAsText(ci.sector,ci.step,ci.interval)), null);
        */
        return s;
      }
    highlightedSector=null;
    highlightedInterval=-1;
    plotImageValid=false;
    repaint();
    return s;
  }

  int yy[]=null, y0=0;

  protected int maxNsectorsToDisplay=40;
  public void setMaxNsectorsToDisplay (int maxNsectorsToDisplay) {
    this.maxNsectorsToDisplay=maxNsectorsToDisplay;
    plotImageValid=false;
    repaint();
  }

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
      sectorInfos=new Vector<>(dk.getSectors().size());
    else
      sectorInfos.clear();
    if (cellInfos ==null)
      cellInfos =new Vector<>(dk.getSectors().size()*dk.Nintervals);
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
    for (String sector:dk.getSectorsSorted())
      if (sector.length()>maxL)
        maxL=sector.length();

    int compW=2+lblw+Math.min(maxNsectorsToDisplay,dk.getSectorsSorted().size())*strw,
        compWextra=5; // width of a single component
    //if (compW*sts.length+compWextra*(sts.length-1)>getWidth()) {
      int W=(getWidth()-5*compWextra)/sts.length;
      strw=(W-(lblw-1))/Math.min(maxNsectorsToDisplay,dk.getSectorsSorted().size());
      compW=2+lblw+Math.min(maxNsectorsToDisplay,dk.getSectorsSorted().size())*strw;
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
      drawCenteredString("Step# "+sts[comp]+((dk.stepLabels==null)?"":" ("+dk.stepLabels[sts[comp]]+")"),xx,0,compW,strh,g2);
      g2.drawLine(xx,strh+1,xx+compW,strh+1);
      g2.setColor(Color.GRAY.brighter());
      for (int i=1; i<dk.Nintervals; i++)
        g2.drawLine(xx,yy[i], xx+compW, yy[i]);
      for (int i=1; i<Math.min(maxNsectorsToDisplay,dk.getSectorsSorted().size()); i++)
        g2.drawLine(xx+lblw+1+i*strw, strh+1, xx+lblw+1+i*strw, yy[yy.length-1]+3);
      g2.setColor(Color.GRAY);
      for (int i=0; i<dk.Nintervals; i++) {
        if (i==highlightedInterval) {
          g2.setColor(new Color(0f,1f,1f,0.5f));
          g2.fillRect(xx,yy[i],lblw,yy[i+1]-yy[i]);
        }
        g2.setColor(Color.GRAY);
        drawCenteredString(String.format("%02d", i / 3) + ":" + String.format("%02d", (i % 3) * 20), xx + 1, yy[i], lblw, yy[i + 1] - yy[i], g2);
      }
      xx+=lblw+1;
      g2.setColor(new Color(0f,1f,1f,0.1f));
      g2.fillRect(xx,yy[0],compW-lblw-1,yy[yy.length-1]-yy[0]);
      g2.setColor(Color.BLACK);
      g2.drawRect(xx,yy[0],compW-lblw-1,yy[yy.length-1]-yy[0]);
      int nShownSectors=0;
      for (String sector : dk.getSectorsSorted()) { // new TreeSet<String>(dk.sectors)
        if (nShownSectors==maxNsectorsToDisplay) {
          g2.setColor(Color.black);
          drawCenteredString("...",xx,2+strh,10,strh,g2);
        }
        if (nShownSectors>=maxNsectorsToDisplay)
          continue;
        nShownSectors++;
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
        float capacity=dk.capacities.get(sector);;
        if (hotspotRatio==0)
          capacity=1.1f*capacity;
        for (int i=0; i<dk.Nintervals; i++) {
          int n=dk.getCount(sector,"CountFlights",i,sts[comp]);
          //System.out.println("sector="+sector+", interval="+String.format("%02d", i / 3) + ":" + String.format("%02d", (i % 3) * 20)+
          //        ", step="+sts[comp]+", value="+n);
          int ww=0; //(strw-1)*n/dk.iGlobalMax;
          if (selectedSectors.contains(sector)) {
            g2.setColor(new Color(0f,1f,1f,0.3f));
            g2.fillRect(xx,yy[i],strw-1,yy[i+1]-yy[i]);
          }
          if (i==highlightedInterval || sector.equals(highlightedSector)) {
            g2.setColor(new Color(0f,1f,1f,0.5f));
            g2.fillRect(xx,yy[i],strw-1,yy[i+1]-yy[i]);
          }
          switch (iRenderingMode) {
            case 0:
              //n=dk.getCount(sector,"CountFlights",i,sts[comp]);
              ww=(strw-1)*n/dk.iLocalMax; // dk.iGlobalMax;
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
                ww=(strw-1)*nn[k]/dk.iLocalMax; // dk.iGlobalMax;
                g2.fillRect(xx,yy[i],ww,yy[i+1]-yy[i]);
              }
              break;
          }
          if (n>capacity) {
            g2.setColor(Color.red);
            ww=(int)((strw-1)*capacity/dk.iLocalMax); // dk.iGlobalMax;
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

  protected SectorInfo findSectorInfoByPoint (Point p) {
    for (SectorInfo si:sectorInfos)
      if (si.r.x<=p.x && si.r.x+si.r.width>=p.x) // (si.r.contains(p)) - here we ignore p.y
        return si;
    return null;
  }
  protected CellInfo findCellInfoByPoint (Point p) {
    for (CellInfo ci:cellInfos)
      if (ci.r.contains(p)) // (si.r.contains(p)) - here we ignore p.y
        return ci;
    return null;
  }

  protected String getStepLabel (int step) {
    return (dk.stepLabels==null) ? ""+step : dk.stepLabels[step];
  }
  protected void doPopup (MouseEvent me) {
    //System.out.println("* popup "+me);
    JPopupMenu menu=new JPopupMenu();
    JCheckBoxMenuItem cbitem=new JCheckBoxMenuItem("Sort sectors by name",SectorData.comparisonMode[0].equals(dk.sortMode));
    cbitem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dk.sortSectors(SectorData.comparisonMode[0]);
        plotImageValid=false;
        repaint();
      }
    });
    menu.add(cbitem);
    cbitem=new JCheckBoxMenuItem("Sort sectors by N flights",SectorData.comparisonMode[1].equals(dk.sortMode));
    cbitem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dk.sortSectors(SectorData.comparisonMode[1]);
        plotImageValid=false;
        repaint();
      }
    });
    menu.add(cbitem);
    cbitem=new JCheckBoxMenuItem("Sort sectors by N hotspots (overall)",SectorData.comparisonMode[2].equals(dk.sortMode));
    cbitem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dk.sortSectors(SectorData.comparisonMode[2]);
        plotImageValid=false;
        repaint();
      }
    });
    menu.add(cbitem);
    cbitem=new JCheckBoxMenuItem("Sort sectors by N hotspots @ first step",SectorData.comparisonMode[3].equals(dk.sortMode));
    cbitem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dk.sortSectors(SectorData.comparisonMode[3]);
        plotImageValid=false;
        repaint();
      }
    });
    menu.add(cbitem);
    cbitem=new JCheckBoxMenuItem("Sort sectors by N hotspots @ last step",SectorData.comparisonMode[4].equals(dk.sortMode));
    cbitem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dk.sortSectors(SectorData.comparisonMode[4]);
        plotImageValid=false;
        repaint();
      }
    });
    menu.add(cbitem);
    if (selectedSectors.size()>0) {
      menu.add(new JPopupMenu.Separator());
      JMenuItem item=new JMenuItem("show only selected");
      item.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          dk.sortSectors(dk.sectorsWithData.elementAt(0).compMode,selectedSectors);
          dk.calcMaxForSelectedSteps(sts);
          plotImageValid=false;
          repaint();
        }
      });
      menu.add(item);
      //menu.add(new JPopupMenu.Separator());
      item=new JMenuItem("show all");
      item.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          dk.sortSectors(dk.sectorsWithData.elementAt(0).compMode);
          dk.calcMaxForSelectedSteps(sts);
          plotImageValid=false;
          repaint();
        }
      });
      menu.add(item);
      //menu.add(new JPopupMenu.Separator());
      item=new JMenuItem("clear selection");
      item.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          selectedSectors.clear();
          dk.sortSectors(dk.sectorsWithData.elementAt(0).compMode,selectedSectors);
          dk.calcMaxForSelectedSteps(sts);
          plotImageValid=false;
          repaint();
        }
      });
      menu.add(item);
/*
      JCheckBoxMenuItem cbitem=new JCheckBoxMenuItem("show only selected",dk.sectorsSorted.size()<dk.sectors.size());
      cbitem.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (cbitem.getState())
            dk.sortSectors(dk.sectorsWithData.elementAt(0).compMode,selectedSectors);
          else
            dk.sortSectors(dk.sectorsWithData.elementAt(0).compMode);
          dk.calcMaxForSelectedSteps(sts);
          plotImageValid=false;
          repaint();
        }
      });
      menu.add(cbitem);
*/
    }
    SectorInfo si=findSectorInfoByPoint(me.getPoint());
    String sector=(si==null)?null:si.sector;
    if (sector!=null) {
      menu.add(new JPopupMenu.Separator());
      JMenuItem item=new JMenuItem("Select sectors connected with "+sector);
      item.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          selectedSectors.add(sector);
          Vector<String> connectedSectors=dk.getConnectedSectors(sector);
          for (String cs:connectedSectors)
            selectedSectors.add(cs);
          dk.sortSectors(dk.sectorsWithData.elementAt(0).compMode,selectedSectors);
          dk.calcMaxForSelectedSteps(sts);
          plotImageValid=false;
          repaint();
        }
      });
      menu.add(item);
      menu.add(new JPopupMenu.Separator());
      CellInfo ci=findCellInfoByPoint(me.getPoint());
      if (dk.getFlights()!=null && dk.getFlights().size()>0)
        for (int k=0; k<((ci==null)?2:3); k++) {
          String s=(k==0)?"all flights" : "flights";
          if (k>0)
            s+=" in " + sector +  " at step #" + si.step + " (" + getStepLabel(si.step) + ")";
          if (k>1 && ci!=null)
            s+=", interval " + String.format("%02d", ci.interval / 3) + ":" + String.format("%02d", (ci.interval % 3) * 20) + ".." +
                    String.format("%02d", ci.interval / 3 + 1) + ":" + String.format("%02d", (ci.interval % 3) * 20);
          final String ss=s;
          final int kk=k;
          item = new JMenuItem("Show "+s);
          item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              Hashtable<String,int[]> flightsTimesInSector=new Hashtable<>();
              Vector<Flight> vf = ((kk==0)?dk.getFlights():((kk==1)?dk.getFlights(sector,si.step,flightsTimesInSector):dk.getFlights(sector,ci.interval,ci.step,flightsTimesInSector)));
              if (vf==null || vf.size()==0)
                return;
              JFrame frame = new JFrame("TAPAS Solution Explorer: " + vf.size() + " " + ss);
              frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
              if (flightsTimesInSector.size()==0)
                flightsTimesInSector=null;
              FlightsTable ft = new FlightsTable(dk, vf, flightsTimesInSector, si.step, dk.isExplanationsLoaded());
  /*
              JTable table = ft.getTable();
              if (table != null) {
                JPopupMenu menu = table.getComponentPopupMenu();
                if (menu == null)
                  menu = new JPopupMenu();
                JMenuItem mit = new JMenuItem("Show flight plan variants");
                menu.add(mit);
                mit.addActionListener(new ActionListener() {
                  @Override
                  public void actionPerformed(ActionEvent e) {
                    Point p = table.getMousePosition();
                    int selectedRow =table.rowAtPoint(p)-1;
                    if (selectedRow<0)
                      return;
                    selectedRow =table.convertRowIndexToModel(selectedRow);
                    String flId = vf.elementAt(selectedRow).id;
                    dk.showFlightVariants(flId);
                  }
                });
                table.setComponentPopupMenu(menu);
              }
  */
              frame.getContentPane().add(ft, BorderLayout.CENTER);
              frame.pack();
              frame.setVisible(true);
              children.add(frame);
            }
          });
          menu.add(item);
        }
      menu.add(new JPopupMenu.Separator());
      item=new JMenuItem("Show full history for "+sector);
      item.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          JFrame frame = new JFrame("TAPAS Solution Explorer: sector "+sector);
          frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
          InfoCanvas ic=new InfoCanvas(dk);
          ic.setSector(sector);
          ic.setHotspotRatio(hotspotRatio);
          ControlPanel cp=new ControlPanel(dk,ic,null,sector);
          frame.getContentPane().add(cp, BorderLayout.SOUTH);
          frame.getContentPane().add(ic, BorderLayout.CENTER);
          frame.pack();
          frame.setVisible(true);
          children.add(frame);
        }
      });
      menu.add(item);
      menu.add(new JSeparator());
      int step=(si.step<0)?0:si.step;
      item = new JMenuItem("show details for step "+getStepLabel(step));
      item.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          Vector<Record> vr[]=new Vector[1];
          vr[0]=dk.getRecordsForStep(step);
          String s[]=new String[1];
          s[0]=(dk.stepLabels==null) ? ""+step : dk.stepLabels[step];
          new TapasSectorExplorer.data_manage.Connector(vr,s,dk.capacities,sector);
        }
      });
      menu.add(item);
      if (step>0) {
        item = new JMenuItem("Compare steps " + getStepLabel(0) + " & " + getStepLabel(step));
        item.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Vector<Record> vr[] = new Vector[2];
            vr[0] = dk.getRecordsForStep(0);
            vr[1] = dk.getRecordsForStep(step);
            String s[] = new String[2];
            s[0] = getStepLabel(0);
            s[1] = getStepLabel(step);
            new TapasSectorExplorer.data_manage.Connector(vr, s, dk.capacities, sector);
          }
        });
        menu.add(item);
        item = new JMenuItem("Compare steps " + getStepLabel(step - 1) + " & " + getStepLabel(step));
        item.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Vector<Record> vr[] = new Vector[2];
            vr[0] = dk.getRecordsForStep(step - 1);
            vr[1] = dk.getRecordsForStep(step);
            String s[] = new String[2];
            s[0] = getStepLabel(step - 1);
            s[1] = getStepLabel(step);
            new TapasSectorExplorer.data_manage.Connector(vr, s, dk.capacities, sector);
          }
        });
        menu.add(item);
      }
      if (step<dk.Nsteps-1) {
        menu.add(new JSeparator());
        item = new JMenuItem("Compare steps "+getStepLabel(step)+" & "+getStepLabel(step+1));
        item.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Vector<Record> vr[]=new Vector[2];
            vr[0]=dk.getRecordsForStep(step);
            vr[1]=dk.getRecordsForStep(step+1);
            String s[]=new String[2];
            s[0]=getStepLabel(step);
            s[1]=getStepLabel(step+1);
            new TapasSectorExplorer.data_manage.Connector(vr,s,dk.capacities,sector);
          }
        });
        menu.add(item);
        item = new JMenuItem("Compare steps "+getStepLabel(step)+" & "+getStepLabel(dk.Nsteps-1));
        item.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Vector<Record> vr[]=new Vector[2];
            vr[0]=dk.getRecordsForStep(step);
            vr[1]=dk.getRecordsForStep(dk.Nsteps-1);
            String s[]=new String[2];
            s[0]=getStepLabel(step);
            s[1]=getStepLabel(dk.Nsteps-1);
            new TapasSectorExplorer.data_manage.Connector(vr,s,dk.capacities,sector);
          }
        });
        menu.add(item);
      }
    }
    menu.show(this,me.getX(),me.getY());
  }
  public void mouseEntered (MouseEvent me) {
  }
  public void mouseExited (MouseEvent me) {
    if (highlightedSector!=null) {
      highlightedSector=null;
      highlightedInterval=-1;
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
          return;
        }
      for (CellInfo ci : cellInfos)
        if (ci.r.contains(me.getPoint())) {
          if (selectedSectors.contains(ci.sector))
            selectedSectors.remove(ci.sector);
          else
            selectedSectors.add(ci.sector);
          plotImageValid = false;
          repaint();
          return;
        }
    }
  }
  public void mousePressed (MouseEvent me) {
  }
  public void mouseReleased (MouseEvent me) {
  }

}
