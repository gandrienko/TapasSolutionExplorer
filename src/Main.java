import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

public class Main {

  private static void createAndShowGUI (DataKeeper dk) {
    //Create and set up the window.
    JFrame frame = new JFrame("TAPAS Solution Explorer");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    InfoCanvas ic=new InfoCanvas(dk);
    ControlPanel cp=new ControlPanel(dk,ic);
    frame.getContentPane().add(cp, BorderLayout.SOUTH);
    frame.getContentPane().add(ic, BorderLayout.CENTER);

    //Display the window.
    frame.pack();
    frame.setVisible(true);
  }

  public static void main(String[] args) {
    String fname="FlightsInSectors_LECMTLL"; //"FlightsInSectors";  //"FlightsInSectors_LECSASV"; //
    DataKeeper dk=new DataKeeper(fname,"capacities");
    if (!dk.sectors.isEmpty())
      createAndShowGUI(dk);
  }

}
