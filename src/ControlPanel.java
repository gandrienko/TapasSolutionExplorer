import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.TreeSet;

public class ControlPanel extends JPanel implements ActionListener {

  protected DataKeeper dk=null;
  protected InfoCanvasBasics ic=null;
  protected InfoSteps is=null;

  public JComboBox JCsectors=null;
  protected JComboBox JCrenderingMode=null, JChotspotsMode=null, JChotspotsRatio=null;
  protected JCheckBox JCBemthHotspots=null, JCBhideSectorsWithUndefinedCapacity=null;

  protected Timer timer=null;
  protected JButton bstart=null, bstop=null;
  private final int animationDelay = 1000; // 2sec

  public static String[] hotspotModes={"by entries (distinct flights)","by entries (all)","by presence (distinct)"}, hotspotRatios={"demand > 1.1 x capacity","demand > 1 x capacity"};

  public ControlPanel (DataKeeper dk, InfoCanvasBasics ic, InfoSteps is, String sector) {
    this.dk=dk;
    this.ic=ic;
    this.is=is;
    setLayout(new BorderLayout());
    if (ic instanceof InfoCanvas){
      dk.aggregate(sector);
      //dk.checkEqual();
      dk.getCounts(sector,"CountFlights");
      ((InfoCanvas)ic).setSector(sector);
      /*
      TreeSet<String> treeSet = new TreeSet<String>(dk.sectors);
      JCsectors = new JComboBox(treeSet.toArray());
      JCsectors.addActionListener(this);
      if (JCsectors.getItemCount() > 0) {
        String sector = (String) JCsectors.getSelectedItem();
        System.out.println("* aggregating for " + sector);
        dk.aggregate(sector);
        //dk.checkEqual();
        dk.getCounts(sector,"CountFlights");
        ((InfoCanvas)ic).setSector(sector);
      }
      JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));
      p.add(new JLabel("Sector:"));
      p.add(JCsectors);
      add(p,BorderLayout.WEST);
      */
    }
    else { // InfoCanvasAll
      JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
      bstart=new JButton("Animation: start");
      bstop=new JButton("Animation: stop");
      bstop.setEnabled(false);
      ActionListener al=this;
      bstart.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          bstart.setEnabled(false);
          bstop.setEnabled(true);
          timer=new Timer(animationDelay,al);
          is.animationStart();
          timer.start();
        }
      });
      bstop.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          bstart.setEnabled(true);
          bstop.setEnabled(false);
          timer.stop();
          is.animationStop();
        }
      });
      p.add(bstart); p.add(bstop);
      add(p,BorderLayout.WEST);
      p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
      p.add(new JLabel("hotspots:"));
      JChotspotsMode=new JComboBox(hotspotModes);
      JChotspotsMode.addActionListener(this);
      p.add(JChotspotsMode);
      JChotspotsRatio=new JComboBox(hotspotRatios);
      JChotspotsRatio.addActionListener(this);
      p.add(JChotspotsRatio);
      p.add(new JLabel("   "));
      JCBhideSectorsWithUndefinedCapacity=new JCheckBox("hide sectors with undefined capacity",true);
      JCBhideSectorsWithUndefinedCapacity.addItemListener(new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent e) {
          dk.bHideSectorsWithUndefinedCapacity=JCBhideSectorsWithUndefinedCapacity.isSelected();
          ((InfoCanvasAll)ic).setHideSectorsWithUndefinedCapacity(JCBhideSectorsWithUndefinedCapacity.isSelected());
        }
      });
      p.add(JCBhideSectorsWithUndefinedCapacity);
      add(p,BorderLayout.EAST);
    }
    JCrenderingMode=new JComboBox(InfoCanvas.RenderingModes);
    if (ic instanceof InfoCanvasAll) {
      JCrenderingMode.removeItemAt(2);
      JCrenderingMode.removeItemAt(2);
    }
    JCrenderingMode.setSelectedIndex(1);
    JCrenderingMode.addActionListener(this);
    JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    p.add(new JLabel("colors for: "));
    p.add(JCrenderingMode);
    add(p, BorderLayout.CENTER);
    if (ic instanceof InfoCanvas) {
      JCBemthHotspots = new JCheckBox("more space for " + dk.NintevalsWithHotspots + " intevals with hotspots", false);
      JCBemthHotspots.addItemListener(new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent e) {
          ic.setbDoubleSpaceForHotspots(JCBemthHotspots.isSelected());
        }
      });
      JCBemthHotspots.setToolTipText("time intervals that have hotspots will be given double screen space (height)");
      add(JCBemthHotspots, BorderLayout.EAST);
    }
    ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
  }

  public void actionPerformed (ActionEvent ae) {
    if (ae.getSource().equals(JCsectors) && ic instanceof InfoCanvas) {
      String sector=(String)JCsectors.getSelectedItem();
      System.out.println("* aggregating for "+sector);
      dk.aggregate(sector);
      //dk.checkEqual();
      dk.getCounts(sector,"CountFlights");
      JCBemthHotspots.setText("more space for "+dk.NintevalsWithHotspots+" intevals with hotspots");
      ((InfoCanvas)ic).setSector(sector);
    }
    if (ae.getSource().equals(JCrenderingMode))
      ic.setRenderingModes((String)JCrenderingMode.getSelectedItem());
    if (ae.getSource().equals(JChotspotsMode)) {
      dk.setHotspotMode(JChotspotsMode.getSelectedIndex());
      ((InfoCanvasAll)ic).setHotspotMode(JChotspotsMode.getSelectedIndex());
    }
    if (ae.getSource().equals(JChotspotsRatio)) {
      dk.setHotspotRatio(JChotspotsRatio.getSelectedIndex());
      ((InfoCanvasAll)ic).setHotspotRatio(JChotspotsRatio.getSelectedIndex());
    }
    if (ae.getSource() instanceof Timer && is!=null) {
      is.animationNextStep();
      if (is.sts_animation==-1) {
        timer.stop();
        bstart.setEnabled(true);
        bstop.setEnabled(false);
      }
    }
  }

}
