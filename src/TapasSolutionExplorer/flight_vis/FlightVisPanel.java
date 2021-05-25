package TapasSolutionExplorer.flight_vis;

import TapasDataReader.Record;
import TapasSolutionExplorer.Data.FlightInSector;

import TapasUtilities.RangeSlider;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalTime;
import java.util.Hashtable;
import java.util.Vector;

public class FlightVisPanel extends JPanel implements ChangeListener, ActionListener {
  /**
   * Sector sequences for all variants of all flights
   * dimension 0: flights
   * dimension 1: flight variants
   * dimension 2: visited sectors
   */
  public FlightInSector flights[][][]=null;
  
  public FlightVariantsShow flShow=null;
  
  protected JLabel flLabel=null;
  /**
   * Controls for selecting the time range to view
   */
  protected RangeSlider timeFocuser=null;
  protected JTextField tfTStart=null, tfTEnd=null;
  protected JButton bFullRange=null;
  
  public FlightVisPanel(FlightInSector flights[][][]) {
    if (flights==null)
      return;
    this.flights=flights;
    flShow=new FlightVariantsShow(flights);
    makeInterior();
  }
  
  public FlightVisPanel(FlightVariantsShow flShow) {
    if (flShow == null)
      return;
    this.flShow = flShow;
    makeInterior();
  }
  
  protected void makeInterior(){
    setLayout(new BorderLayout());
    add(flShow,BorderLayout.CENTER);
    flLabel=new JLabel("No flight selected",JLabel.CENTER);
    add(flLabel,BorderLayout.NORTH);

    timeFocuser=new RangeSlider();
    timeFocuser.setPreferredSize(new Dimension(240,timeFocuser.getPreferredSize().height));
    timeFocuser.setMinimum(0);
    timeFocuser.setMaximum(FlightVariantsShow.minutesInDay);
    timeFocuser.setValue(0);
    timeFocuser.setUpperValue(FlightVariantsShow.minutesInDay);
    timeFocuser.addChangeListener(this);
    tfTStart=new JTextField("00:00");
    tfTEnd=new JTextField("24:00");
    tfTStart.addActionListener(this);
    tfTEnd.addActionListener(this);
    JPanel pp=new JPanel(new FlowLayout(FlowLayout.LEFT,5,2));
    pp.add(new JLabel("Time range:"));
    pp.add(tfTStart);
    Panel p=new Panel(new BorderLayout());
    add(p,BorderLayout.SOUTH);
    p.add(pp,BorderLayout.WEST);
    p.add(timeFocuser,BorderLayout.CENTER);
    pp=new JPanel(new FlowLayout(FlowLayout.LEFT,5,2));
    p.add(pp,BorderLayout.EAST);
    pp.add(tfTEnd);

    bFullRange=new JButton("Full range");
    bFullRange.setActionCommand("full_time_range");
    bFullRange.addActionListener(this);
    bFullRange.setEnabled(false);
    pp.add(bFullRange);
  }
  /**
   * Flight plan versions for all solution steps, to be passed to the drawing component
   * for showing histograms of sector loads.
   * The keys of the hashtable consist of sector identifiers and step numbers with underscore between them.
   */
  
  public void setFlightPlans(Hashtable<String, Vector<Record>> flightPlans) {
    if (flShow!=null)
      flShow.setFlightPlans(flightPlans);
  }
  /**
   * Capacities of the sectors (max acceptable N of flights per hour)
   */
  public void setCapacities(Hashtable<String, Integer> capacities) {
    if (flShow!=null)
      flShow.setCapacities(capacities);
  }
  
  public boolean showFlightVariants(String flId) {
    if (flShow!=null && flShow.showFlightVariants(flId)) {
      //adjust the time range
      int fIdx=flShow.getShownFlightIdx();
      String str="Flight "+flId+": "+flights[fIdx].length+" variants";
      LocalTime t1=flights[fIdx][0][0].entryTime, t2=flights[fIdx][0][flights[fIdx][0].length-1].exitTime;
      int delay=flights[fIdx][0][0].delay;
      for (int i=1; i<flights[fIdx].length; i++) {
        FlightInSector fSeq[]=flights[fIdx][i];
        if (fSeq[0].delay>delay) delay=fSeq[0].delay;
        if (t1.compareTo(fSeq[0].entryTime)>0)
          t1=fSeq[0].entryTime;
        if (t2.compareTo(fSeq[fSeq.length-1].exitTime)<0)
          t2=fSeq[fSeq.length-1].exitTime;
      }
      str+="; delay = "+delay;
      flLabel.setText(str);
      flLabel.setSize(flLabel.getPreferredSize());
      timeFocuser.removeChangeListener(this);
      timeFocuser.setFullRange();
      timeFocuser.setValue(Math.max(t1.getHour() * 60 + t1.getMinute()-15,timeFocuser.getMinimum()));
      timeFocuser.setUpperValue(Math.min(t2.getHour() * 60 + t2.getMinute()+15,timeFocuser.getMaximum()));
      timeFocuser.addChangeListener(this);
      getTimeRange();
      return true;
    }
    flLabel.setText("No flight selected");
    flLabel.setSize(flLabel.getPreferredSize());
    return false;
  }
  
  public void actionPerformed (ActionEvent ae) {
    if (ae.getSource() instanceof JTextField) {
      JTextField tf = (JTextField) ae.getSource();
      if (tf.equals(tfTStart) || tf.equals(tfTEnd)) {
        String txt = tf.getText();
        int idx = txt.indexOf(':');
        int h = -1, m = -1;
        try {
          h = Integer.parseInt((idx < 0) ? txt : txt.substring(0, idx));
        } catch (Exception ex) {
        }
        if (h >= 0 && idx > 0)
          try {
            m = Integer.parseInt(txt.substring(idx + 1));
          } catch (Exception ex) {
          }
        if (h < 0 || m < 0 || h > 24 || m > 59) {
          int val = (tf.equals(tfTStart)) ? timeFocuser.getValue() : timeFocuser.getUpperValue();
          tf.setText(String.format("%02d:%02d", val / 60, val % 60));
        }
        else {
          m += h * 60;
          boolean ok = (tf.equals(tfTStart)) ? m < timeFocuser.getUpperValue() : m > timeFocuser.getValue();
          if (!ok) {
            int val = (tf.equals(tfTStart)) ? timeFocuser.getValue() : timeFocuser.getUpperValue();
            tf.setText(String.format("%02d:%02d", val / 60, val % 60));
          }
          else
            if (tf.equals(tfTStart))
              timeFocuser.setValue(m);
            else
              timeFocuser.setUpperValue(m);
        }
      }
    }
    else
      if (ae.getActionCommand().equals("full_time_range"))  {
        if (timeFocuser.getValue()>timeFocuser.getMinimum() ||
                timeFocuser.getUpperValue()<timeFocuser.getMaximum())
          timeFocuser.setFullRange();
      }
  }
  
  public void stateChanged(ChangeEvent e) {
    if (e.getSource().equals(timeFocuser))
      getTimeRange();
  }
  
  protected void getTimeRange() {
    int m1=timeFocuser.getValue(), m2=timeFocuser.getUpperValue();
    if (m2-m1<60) {
      if (flShow !=null)
        if (m1== flShow.getMinuteStart()) {
          m2=m1+60;
          if (m2>FlightVariantsShow.minutesInDay) {
            m2=FlightVariantsShow.minutesInDay;
            m1=m2-60;
          }
        }
        else {
          m1=m2-60;
          if (m1<0) {
            m1=0; m2=60;
          }
        }
      else {
        m2=m1+60;
        if (m2>FlightVariantsShow.minutesInDay) {
          m2=FlightVariantsShow.minutesInDay;
          m1=m2-60;
        }
      }
      timeFocuser.setValue(m1);
      timeFocuser.setUpperValue(m2);
    }
    tfTStart.setText(String.format("%02d:%02d",m1/60,m1%60));
    tfTEnd.setText(String.format("%02d:%02d",m2/60,m2%60));
    flShow.setTimeRange(m1,m2);
    bFullRange.setEnabled(m1>timeFocuser.getMinimum() || m2<timeFocuser.getMaximum());
  }
}
