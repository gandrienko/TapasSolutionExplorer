import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ControlPanel extends JPanel implements ActionListener {

  protected DataKeeper dk=null;
  protected InfoCanvas ic=null;

  protected JComboBox JCsectors=null;

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
  }

  public void actionPerformed (ActionEvent ae) {
    if (ae.getSource().equals(JCsectors)) {
      String sector=(String)JCsectors.getSelectedItem();
      System.out.println("* aggregating for "+sector);
      dk.aggregate(sector);
      //dk.checkEqual();
      dk.getCounts("CountFlights");
      ic.setSector(sector);
      ic.repaint();
    }
  }

}
