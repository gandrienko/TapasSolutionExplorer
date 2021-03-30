import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.HashSet;

public class InfoSteps extends JPanel {

  DataKeeper dk=null;
  HashSet<Integer> selectedSteps=null;

  public InfoSteps (DataKeeper dk) {
    this.dk=dk;
    ToolTipManager.sharedInstance().registerComponent(this);
  }

  public void setSelectedSteps (HashSet<Integer> selectedSteps) {
    this.selectedSteps=selectedSteps;
  }

  int w,W,x0;
  int h=15;
  int yy[]=new int[]{3,20,40};

  @Override
  public String getToolTipText(MouseEvent me) {
    int x=me.getX(), y=me.getY();
    String s="";
    String lDelays[]={"no delay","1-4 min","5-9 min","10-29 min","30-59 min","over 60 min"};
    if (x>=x0 && x<=x0+W) {
      int step = (x - x0) / w;
      if (step>dk.Nsteps-1)
        step=dk.Nsteps-1;
      s="<html><body style=background-color:rgb(255,255,204)>\n<table border=0 width=100%><tr align=center><td>Step = "+step+"</td><td></td><td>count</td><td>%</td></tr>";
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
      for (int i=0; i<min.length; i++) {
        float f=(dk.stepsInfo[step][i]-min[i])*1f/(max[i]-min[i]);
        g2.setColor(new Color((float)(0.2f+0.4*f),0f,0f,0.5f+f/2));
        g2.fillRect(x0+step*w,yy[i],w,h);
      }
      //float sum=dk.stepsInfo[step][2];
      //for (int i=3; i<dk.stepsInfo[step].length; i++)
        //sum+=dk.stepsInfo[step][i];
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
}
