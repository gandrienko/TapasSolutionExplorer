package TapasSolutionExplorer.flight_vis;

import TapasDataReader.Record;
import TapasSolutionExplorer.Data.FlightInSector;

import TapasUtilities.ItemSelectionManager;
import TapasUtilities.SingleHighlightManager;
import TapasUtilities.RangeSlider;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.TreeSet;
import java.util.Vector;

public class FlightVisPanel extends JPanel implements ChangeListener, ActionListener, ItemListener {
  public static final Color apricot=new Color(255,182,115),
    denim=new Color(21,96,189),
    linkColor=new Color(230,230,255);
  /**
   * Sector sequences for all variants of all flights
   * dimension 0: flights
   * dimension 1: flight variants
   * dimension 2: visited sectors
   */
  public FlightInSector flights[][][]=null;
  /**
   * Solution steps as labelled by the ML model
   */
  protected TreeSet<Integer> solutionSteps =null;
  
  public FlightVariantsShow flShow=null;
  
  protected JLabel flLabel=null;
  protected MosaicLine mosaicLine=null;
  /**
   * Controls for selecting the time range to view
   */
  protected RangeSlider timeFocuser=null;
  protected JTextField tfTStart=null, tfTEnd=null;
  protected JButton bFullRange=null;
  /**
   * Controls for highlighting excesses of capacity
   */
  protected JCheckBox cbHighlightExcess=null;
  protected JTextField tfPercentExcess=null;
  /**
   * Used for setting the aggregation time step
   */
  protected JComboBox chAggrStep=null;
  /**
   * What to count: entries or presence
   */
  protected JComboBox chEntriesOrPresence=null;
  /**
   * Whether to ignore repeated entries
   */
  protected JCheckBox cbIgnoreReEntries =null;
  /**
   * Supports simultaneous highlighting of flight versions and/or corresponding steps
   * in this component and other components
   */
  protected SingleHighlightManager stepHighlighter=null;
  /**
   * Used for passing information about selection of solution steps
   * corresponding to different flight versions.
   */
  protected ItemSelectionManager flightVersionStepSelector =null;
  /**
   * Used for passing information about selection of solution steps for which the
   * FlightVariantsShow shows the dynamics of the demands by time histograms.
   */
  protected ItemSelectionManager showDemandsStepSelector=null;
  
  public FlightVisPanel(FlightInSector flights[][][]) {
    if (flights==null)
      return;
    this.flights=flights;
    flShow=new FlightVariantsShow(flights);
    flShow.addChangeListener(this);
    stepHighlighter=new SingleHighlightManager();
    flShow.setStepHighlighter(stepHighlighter);
    flightVersionStepSelector =new ItemSelectionManager();
    flShow.setFlightVersionStepSelector(flightVersionStepSelector);
    showDemandsStepSelector=new ItemSelectionManager();
    makeInterior();
  }
  
  public FlightVisPanel(FlightVariantsShow flShow) {
    if (flShow == null)
      return;
    this.flShow = flShow;
    flShow.addChangeListener(this);
    stepHighlighter=new SingleHighlightManager();
    flShow.setStepHighlighter(stepHighlighter);
    flightVersionStepSelector =new ItemSelectionManager();
    flShow.setFlightVersionStepSelector(flightVersionStepSelector);
    showDemandsStepSelector=new ItemSelectionManager();
    makeInterior();
  }
  
  protected void makeInterior(){
    //exShow=new ExplanationsPanel();
    //JSplitPane spl=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,flShow,exShow);
    
    setLayout(new BorderLayout());
    //add(spl,BorderLayout.CENTER);
    add(flShow,BorderLayout.CENTER);
    
    flLabel=new JLabel("No flight selected",JLabel.CENTER);
    JPanel p=new JPanel(new GridLayout(0,1));
    p.add(flLabel);
    mosaicLine=new MosaicLine(25,MosaicLine.HORIZONTAL);
    mosaicLine.addChangeListener(this);
    //mosaicLine.getRedrawNotifier().addChangeListener(this);
    mosaicLine.setStepHighlighter(stepHighlighter);
    p.add(mosaicLine);
    add(p,BorderLayout.NORTH);

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
    
    JPanel cp=new JPanel(new GridLayout(0,1));
    add(cp,BorderLayout.SOUTH);
    
    p=new JPanel(new BorderLayout());
    cp.add(p);
    JPanel pp=new JPanel(new FlowLayout(FlowLayout.LEFT,5,2));
    pp.add(new JLabel("Time range:"));
    pp.add(tfTStart);
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
  
    p=new JPanel(new FlowLayout(FlowLayout.CENTER,20,2));
    cp.add(p);
    
    pp=new JPanel(new FlowLayout(FlowLayout.CENTER,5,2));
    p.add(pp);
  
    pp.add(new JLabel("Aggregate"));
    chEntriesOrPresence=new JComboBox();
    pp.add(chEntriesOrPresence);
    chEntriesOrPresence.addItem("entries");
    chEntriesOrPresence.addItem("presence");
    chEntriesOrPresence.setSelectedIndex(0);
    chEntriesOrPresence.addActionListener(this);
  
    pp.add(Box.createRigidArea(new Dimension(10, 0)));
    cbIgnoreReEntries=new JCheckBox("Ignore re-entries",true);
    cbIgnoreReEntries.addItemListener(this);
    pp.add(cbIgnoreReEntries);
  
    pp.add(Box.createRigidArea(new Dimension(20, 0)));
    pp.add(new JLabel("Time step in histograms:"));
    chAggrStep=new JComboBox();
    chAggrStep.addItem(new Integer(1));
    chAggrStep.addItem(new Integer(5));
    chAggrStep.addItem(new Integer(10));
    chAggrStep.addItem(new Integer(15));
    chAggrStep.addItem(new Integer(20));
    chAggrStep.addItem(new Integer(30));
    chAggrStep.addItem(new Integer(60));
    chAggrStep.setSelectedIndex(4);
    chAggrStep.addActionListener(this);
    pp.add(chAggrStep);
    pp.add(new JLabel("minutes"));
  
    pp=new JPanel(new FlowLayout(FlowLayout.CENTER,5,2));
    p.add(pp);
    cbHighlightExcess=new JCheckBox("Highlight excess of sector capacity by over",true);
    pp.add(cbHighlightExcess);
    cbHighlightExcess.addItemListener(this);
    tfPercentExcess=new JTextField("10",4);
    pp.add(tfPercentExcess);
    tfPercentExcess.addActionListener(this);
    pp.add(new JLabel("%"));
  }
  /**
   * Flight plan versions for all solution steps, to be passed to the drawing component
   * for showing histograms of sector loads.
   * The keys of the hashtable consist of sector identifiers and step numbers with underscore between them.
   */

  public void setSolutionSteps(TreeSet<Integer> solutionSteps) {
    this.solutionSteps = solutionSteps;
    if (solutionSteps!=null) {
      mosaicLine.setNTiles(solutionSteps.size());
      ArrayList<Integer> steps=new ArrayList<Integer>(solutionSteps);
      String labels[]=new String[steps.size()];
      for (int i=0; i<steps.size(); i++)
        labels[i]=String.valueOf(i)+" ("+((i==0)?"baseline":steps.get(i).toString())+")";
      mosaicLine.setTileLabels(labels);
    }
  }
  
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
  
  public String getCurrentFlightText(){
    int fIdx=flShow.getShownFlightIdx();
    if (fIdx>=0) {
      String str = "Flight " + flights[fIdx][0][0].flightId + ": " + flights[fIdx].length + " variants";
      int delay = flights[fIdx][0][0].delay;
      for (int i = 1; i < flights[fIdx].length; i++) {
        FlightInSector fSeq[] = flights[fIdx][i];
        if (fSeq!=null && fSeq[0].delay > delay)
          delay = fSeq[0].delay;
      }
      str += "; max delay = " + delay;
      return str;
    }
    else
      return "No flight selected";
  }
  
  public boolean showFlightVariants(String flId) {
    mosaicLine.setMarkedIdx(-1);
    mosaicLine.cancelSelection();
    if (flShow!=null && flShow.showFlightVariants(flId)) {
      int fIdx=flShow.getShownFlightIdx();
      //show indicate the steps of the flight variants in the mosaic line
      if (solutionSteps!=null) {
        Color colors[]=new Color[solutionSteps.size()];
        for (int i=0; i<colors.length; i++)
          colors[i]=apricot;
        for (int i=0; i<flights[fIdx].length; i++)
          if (flights[fIdx][i]!=null)
            colors[flights[fIdx][i][0].step]=denim;
        mosaicLine.setTileColors(colors);
      }
      LocalTime t1=null, t2=null;
      if (!flights[fIdx][0][0].entryNextDay) {
        //adjust the time range
        t1 = flights[fIdx][0][0].entryTime;
        t2 = (flights[fIdx][0][0].exitNextDay)?t1:flights[fIdx][0][flights[fIdx][0].length - 1].exitTime;
        for (int i = 1; i < flights[fIdx].length; i++) {
          FlightInSector fSeq[] = flights[fIdx][i];
          if (fSeq == null)
            continue;
          if (!fSeq[0].entryNextDay && t1.compareTo(fSeq[0].entryTime) > 0)
            t1 = fSeq[0].entryTime;
          if (!fSeq[0].exitNextDay && t2.compareTo(fSeq[fSeq.length - 1].exitTime) < 0)
            t2 = fSeq[fSeq.length - 1].exitTime;
        }
      }
      flLabel.setText(getCurrentFlightText());
      flLabel.setSize(flLabel.getPreferredSize());
      timeFocuser.removeChangeListener(this);
      timeFocuser.setFullRange();
      if (t1!=null && t2!=null) {
        timeFocuser.setValue(Math.max(t1.getHour() * 60 + t1.getMinute() - 15, timeFocuser.getMinimum()));
        timeFocuser.setUpperValue(Math.min(t2.getHour() * 60 + t2.getMinute() + 15, timeFocuser.getMaximum()));
      }
      timeFocuser.addChangeListener(this);
      getTimeRange();
      repaint();
      return true;
    }
    flLabel.setText("No flight selected");
    flLabel.setSize(flLabel.getPreferredSize());
    return false;
  }
  
  public String getShownFlightId() {
    if (flShow==null)
      return null;
    return flShow.getShownFlightId();
  }
  
  public SingleHighlightManager getStepHighlighter() {
    return stepHighlighter;
  }
  
  public ItemSelectionManager getFlightVersionStepSelector() {
    return flightVersionStepSelector;
  }
  
  public ItemSelectionManager getShowDemandsStepSelector() {
    return showDemandsStepSelector;
  }
  
  public void drawLinks(Graphics g) {
    if (g==null)
      return;
    if (mosaicLine!=null && mosaicLine.tileColors!=null &&
            flShow!=null && flShow.getShownFlightId()!=null) {
      Point locThis=this.getLocationOnScreen(),
          locMosaic=mosaicLine.getLocationOnScreen(),
          locFlShow=flShow.getLocationOnScreen();
      int dxMosaic=locMosaic.x-locThis.x, dyMosaic=locMosaic.y-locThis.y;
      int dxFlShow=locFlShow.x-locThis.x, dyFlShow=locFlShow.y-locThis.y;
      g.setColor(linkColor);
      for (int i = 0; i < mosaicLine.nTiles; i++)
        if (mosaicLine.tileColors[i].equals(denim)) {
          int fIdx=flShow.getFlightVersionIdxForStep(i);
          if (fIdx<0)
            continue;
          Point pt=flShow.getPathOriginForFlightVersion(fIdx);
          if (pt==null)
            continue;;
          int x1 = mosaicLine.getXPosForTile(i)+dxMosaic+mosaicLine.tileW/2,
              y1 = mosaicLine.getYPosForTile(i)+dyMosaic+mosaicLine.tileH+1;
          int x2=pt.x+dxFlShow, y2=pt.y+dyFlShow;
          g.drawLine(x1,y1,x2,y2);
        }
    }
  }
  /*
  // To paint over sub-components, it is necessary to redefine method paint(Graphics)
  // rather than paintComponent(Graphics).
  
  public void paint(Graphics g) {
    super.paint(g);
    drawLinks(g);
  }
  */
  
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
      else
        if (tf.equals(tfPercentExcess)) {
          float perc=-1;
          try {
            perc=Float.parseFloat(tf.getText());
          } catch (Exception ex) {}
          if (perc<0) {
            perc=(flShow ==null)?10: flShow.getMinExcessPercent();
            tf.setText(String.valueOf(perc));
          }
          else
            if (flShow !=null)
              flShow.setMinExcessPercent(perc);
        }
    }
    else
      if (ae.getActionCommand().equals("full_time_range"))  {
        if (timeFocuser.getValue()>timeFocuser.getMinimum() ||
                timeFocuser.getUpperValue()<timeFocuser.getMaximum())
          timeFocuser.setFullRange();
      }
      else
        if (ae.getSource().equals(chAggrStep)) {
          if (flShow !=null)
            flShow.setAggregationTimeStep((Integer)chAggrStep.getSelectedItem());
        }
        else
          if (ae.getSource().equals(chEntriesOrPresence)) {
            if (flShow !=null)
              flShow.setToCountEntries(chEntriesOrPresence.getSelectedIndex()==0);
          }
  }
  
  public void itemStateChanged(ItemEvent e) {
    if (e.getSource().equals(cbIgnoreReEntries)) {
      if (flShow !=null)
        flShow.setToIgnoreReEntries(cbIgnoreReEntries.isSelected());
    }
    else
      if (e.getSource().equals(cbHighlightExcess))  {
        if (flShow !=null)
            flShow.setToHighlightCapExcess(cbHighlightExcess.isSelected());
      }
  }
  
  public void stateChanged(ChangeEvent e) {
    if (e.getSource().equals(timeFocuser))
      getTimeRange();
    else
      if (e.getSource().equals(flShow)) {
        String txt=getCurrentFlightText();
        if (flShow.getSelectedVariant()>=0) {
          txt += "; selected: variant " + flShow.getSelectedVariant() +"; step "+
                      flShow.getStepOfSelectedVariant();
        }
        flLabel.setText(txt);
        flLabel.setSize(flLabel.getPreferredSize());
        mosaicLine.setMarkedIdx(flShow.getStepOfSelectedVariant());
      }
      else
        if (e.getSource().equals(mosaicLine)) {
          Integer sel1=mosaicLine.getSelectedIndex(true),
              sel2=mosaicLine.getSelectedIndex(false);
          if (sel1==null)
            showDemandsStepSelector.deselectAll();
          else
            if (sel2==null)
              showDemandsStepSelector.select(sel1,true);
            else {
              ArrayList selList=new ArrayList(2);
              selList.add(sel1);
              selList.add(sel2);
              showDemandsStepSelector.updateSelection(selList);
            }
          if (flShow!=null)
            flShow.selectSolutionSteps(sel1,sel2);
        }
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
