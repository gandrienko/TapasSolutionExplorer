import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class ControlPanel extends JPanel implements ActionListener, ItemListener {

  protected DataKeeper dk=null;
  protected InfoCanvas ic=null;

  protected JComboBox JCsectors=null;
  protected JCheckBox JCBemthHotspots=null;

  public ControlPanel (DataKeeper dk, InfoCanvas ic) {
    this.dk=dk;
    this.ic=ic;
    setLayout(new BorderLayout());
    JCsectors=new JComboBox(dk.sectors.toArray());
    JCsectors.addActionListener(this);
    if (JCsectors.getItemCount()>0) {
      String sector=(String)JCsectors.getSelectedItem();
      System.out.println("* aggregating for "+sector);
      dk.aggregate(sector);
      //dk.checkEqual();
      dk.getCounts("CountFlights");
      ic.setSector(sector);
    }
    add(JCsectors,BorderLayout.WEST);
    JCBemthHotspots=new JCheckBox("more space for "+dk.NintevalsWithHotspots+" intevals with hotspots", false);
    JCBemthHotspots.addItemListener(this);
    JCBemthHotspots.setToolTipText("time intervals that have hotspots will be given double screen space (height)");
    add(JCBemthHotspots,BorderLayout.EAST);
    ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
  }

  public void actionPerformed (ActionEvent ae) {
    if (ae.getSource().equals(JCsectors)) {
      String sector=(String)JCsectors.getSelectedItem();
      System.out.println("* aggregating for "+sector);
      dk.aggregate(sector);
      //dk.checkEqual();
      dk.getCounts("CountFlights");
      JCBemthHotspots.setText("more space for "+dk.NintevalsWithHotspots+" intevals with hotspots");
      ic.setSector(sector);
      ic.repaint();
    }
  }

  public void itemStateChanged (ItemEvent ie) {
    if (ie.getSource().equals(JCBemthHotspots)) {
      //
    }
  }

}
