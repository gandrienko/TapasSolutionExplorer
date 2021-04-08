import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashSet;
import java.util.TreeSet;

public class InfoSteps extends JPanel implements MouseListener {

  DataKeeper dk=null;
  HashSet<Integer> selectedSteps=null;
  InfoCanvasAll icAll=null;

  public int sts_animation=-1;

  public InfoSteps (DataKeeper dk, InfoCanvasAll icAll) {
    this.dk=dk;
    this.icAll=icAll;
    ToolTipManager.sharedInstance().registerComponent(this);
    addMouseListener(this);
  }

  public void setSelectedSteps (HashSet<Integer> selectedSteps) {
    this.selectedSteps=selectedSteps;
  }

  int w,W,x0;
  int h=15;
  int yy[]=new int[]{3,20,40};

  public Dimension getPreferredSize() {
    return new Dimension(1000,123);
  }

  @Override
  public String getToolTipText(MouseEvent me) {
    int x=me.getX(), y=me.getY();
    String s="";
    String lDelays[]={"no delay","1-4 min","5-9 min","10-29 min","30-59 min","over 60 min"};
    if (x>=x0 && x<=x0+W) {
      int step = (x - x0) / w;
      if (step>dk.Nsteps-1)
        step=dk.Nsteps-1;
      s="<html><body style=background-color:rgb(255,255,204)>\n<table border=0 width=100%><tr align=center><td>Step# "+step+((dk.stepLabels==null)?"":" ("+dk.stepLabels[step]+")")+"</td><td></td><td>count</td><td>%</td></tr>";
      //s+="<tr align=right><td>step</td><td></td><td>"+step+"</td></tr>\n";
      s+="<tr align=right><td>N hotspots</td><td></td><td>"+dk.stepsInfo[step][0]+"</td></tr>\n";
      s+="<tr align=right><td>N sectors with hotspots</td><td></td><td>"+dk.stepsInfo[step][1]+"</td></tr>\n";
      s+="<tr align=right><td>total delay</td><td></td><td>"+dk.stepsInfo[step][2]+"</td></tr>\n";
      int sum=0;
      for (int i=3; i<dk.stepsInfo[step].length; i++)
        sum+=dk.stepsInfo[step][i];
      s+="<tr align=right><td>Flights: total</td><td></td><td>"+sum+"</td><td>100 %</td></tr>\n";
      for (int i=3; i<dk.stepsInfo[step].length; i++) {
        int rgb=(i==3)? 255: 255-64-32*(i-4);
        s += "<tr align=right><td>" + lDelays[i-3] + "</td><td style=background-color:rgb("+rgb+","+rgb+","+rgb+")>.</td><td>" +
                dk.stepsInfo[step][i] + "</td><td>" +
                dk.stepsInfo[step][i] * 100 / sum + " %</td></tr>\n";
      }
      s+="</table>\n</body></html>";
    }
    else {
      s="<html><body><b>Point</b> on step to get info<p><b>Click</b> on step to select/deselect its presentation in the panel above</body></html>";
    }
    return s;
  }

  public void paintComponent (Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setColor(Color.white);
    g2.fillRect(0,0,getWidth(),getHeight());

    w=(getWidth()-5)/dk.Nsteps;
    W=w*dk.Nsteps;
    x0=(getWidth()-W)/2;
    int h=15;
    int yy[]=new int[]{3,20,40,60};

    g2.setColor(Color.black);
    g2.drawRect(x0,yy[0],W,h);
    g2.drawRect(x0,yy[1],W,h);
    g2.drawRect(x0,yy[2],W,h);
    g2.drawRect(x0,yy[3],W,h*4);

    int min[]=new int[3], max[]=new int[3], maxNdelayed=0;
    for (int i=0; i<min.length; i++) {
      min[i]=-1; max[i]=-1;
    }
    for (int step=0; step<dk.stepsInfo.length; step++) {
      for (int i = 0; i < min.length; i++) {
        if (min[i] == -1 || dk.stepsInfo[step][i] < min[i])
          min[i] = dk.stepsInfo[step][i];
        if (max[i] == -1 || dk.stepsInfo[step][i] > max[i])
          max[i] = dk.stepsInfo[step][i];
      }
      int nDelayed=0;
      for (int i=min.length+1; i<dk.stepsInfo[step].length; i++)
        nDelayed+=dk.stepsInfo[step][i];
      if (nDelayed>maxNdelayed)
        maxNdelayed=nDelayed;
    }

    for (int step=0; step<dk.stepsInfo.length; step++) {
      if (selectedSteps.contains(new Integer(step))) {
        g2.setColor(Color.cyan);
        g2.drawRect(x0+step*w,0,w,getHeight()-1);
        g2.drawRect(x0+step*w,yy[2]-3,w,4);
        g2.drawRect(x0+step*w,yy[3]-3,w,4);
      }
      for (int i=0; i<min.length; i++) {
        float f=(dk.stepsInfo[step][i]-min[i])*1f/(max[i]-min[i]);
        g2.setColor(new Color((float)(0.2f+0.4*f),0f,0f,0.5f+f/2));
        g2.fillRect(x0+step*w,yy[i],w,h);
      }
      float ff[]=new float[dk.stepsInfo[step].length-4];
      ff[0]=dk.stepsInfo[step][min.length+1];
      for (int i=1; i<ff.length; i++)
        ff[i]=ff[i-1]+dk.stepsInfo[step][min.length+1+i];
      for (int i=ff.length-1; i>=0; i--) {
        int hh=Math.round(h*4*ff[i]/maxNdelayed);
        int rgb=255-64-32*i;
        g2.setColor(new Color(rgb,rgb,rgb));
        g2.fillRect(x0+step*w,yy[min.length]+4*h-hh,w,hh);
      }
    }
  }

  protected HashSet<Integer> selectedStepsBeforeAnimation=null;
  public void animationStart() {
    selectedStepsBeforeAnimation=(HashSet<Integer>)selectedSteps.clone();
    sts_animation=0;
  }
  public void animationNextStep() {
    HashSet<Integer> sts=(HashSet<Integer>)selectedStepsBeforeAnimation.clone();
    for (int i=sts_animation+1; sts.contains(new Integer(i)) && i<dk.Nsteps; i++)
      sts_animation=i;
    if (sts_animation+1<dk.Nsteps) {
      sts_animation++;
      System.out.println("* step="+sts_animation);
      sts.add(new Integer(sts_animation));
    }
    else
      sts_animation=-1;
    repaint();
    icAll.setSTS(getSortedArrayFromHashSet(sts));
  }
  public void animationStop() {
    if (sts_animation!=-1) {
      HashSet<Integer> sts=(HashSet<Integer>)selectedStepsBeforeAnimation.clone();
      sts_animation=-1;
      repaint();
      icAll.setSTS(getSortedArrayFromHashSet(sts));
    }
    selectedStepsBeforeAnimation=null;
  }

  protected int[] getSortedArrayFromHashSet (HashSet<Integer> hs) {
    int out[]=new int[hs.size()], n=0;
    TreeSet<Integer> treeSet = new TreeSet<Integer>(hs);
    for (Integer s:treeSet) {
      out[n] = s.intValue();
      n++;
    }
    return out;
  }

  public void mouseClicked (MouseEvent me) {
    int x=me.getX(), y=me.getY();
    if (x>=x0 && x<=x0+W) {
      int step = (x - x0) / w;
      if (step > dk.Nsteps - 1)
        step = dk.Nsteps - 1;
      boolean updateNeeded=false;
      if (selectedSteps.contains(new Integer(step))) { // remove, if not the only one
        if (selectedSteps.size()>1) {
          selectedSteps.remove(new Integer(step));
          updateNeeded=true;
        }
      }
      else { // add
        selectedSteps.add(new Integer(step));
        updateNeeded=true;
      }
      if (updateNeeded) {
        repaint();
        icAll.setSTS(getSortedArrayFromHashSet(selectedSteps));
      }
    }
  }
  public void mouseEntered (MouseEvent me) {}
  public void mouseExited (MouseEvent me) {}
  public void mousePressed (MouseEvent me) {}
  public void mouseReleased (MouseEvent me) {}

}
