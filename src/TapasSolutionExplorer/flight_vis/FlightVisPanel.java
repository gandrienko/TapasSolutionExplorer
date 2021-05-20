package TapasSolutionExplorer.flight_vis;

import TapasUtilities.RangeSlider;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FlightVisPanel extends JPanel implements ChangeListener, ActionListener {
  public FlightVariantsShow flShow=null;
  
  protected JLabel flLabel=null;
  /**
   * Controls for selecting the time range to view
   */
  protected RangeSlider timeFocuser=null;
  protected JTextField tfTStart=null, tfTEnd=null;
  protected JButton bFullRange=null;
  
  public FlightVisPanel(FlightVariantsShow flShow) {
    if (flShow==null)
      return;
    this.flShow=flShow;
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
