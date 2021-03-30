import javafx.scene.control.SplitPane;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

public class Main {

  private static void createAndShowGUI (DataKeeper dk) {
    //Create and set up the window for a single sector.
    JFrame frame = new JFrame("TAPAS Solution Explorer: single sector");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    InfoCanvas ic=new InfoCanvas(dk);
    ControlPanel cp=new ControlPanel(dk,ic);
    frame.getContentPane().add(cp, BorderLayout.SOUTH);
    frame.getContentPane().add(ic, BorderLayout.CENTER);

    //Display the window.
    frame.pack();
    frame.setVisible(true);

    //Create and set up the window for a single sector.
    frame = new JFrame("TAPAS Solution Explorer: all sectors");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    InfoCanvasAll icAll=new InfoCanvasAll(dk);
    InfoSteps is=new InfoSteps(dk);
    cp=new ControlPanel(dk,icAll);
    frame.getContentPane().add(cp, BorderLayout.SOUTH);
    JSplitPane splitPane=new JSplitPane(JSplitPane.VERTICAL_SPLIT,icAll,is);
    splitPane.setOneTouchExpandable(true);
    splitPane.setDividerLocation(0.1);
    icAll.setMinimumSize(new Dimension(1000,750));
    is.setMinimumSize(new Dimension(1000,123));
    frame.getContentPane().add(splitPane, BorderLayout.CENTER);


    //Display the window.
    frame.pack();
    frame.setVisible(true);
  }

  public static void main(String[] args) {
    String fname="v20210304-all-25steps"; //"FlightsInSectors_4"; //"FlightsInSectors_LECMTLL"; // "FlightsInSectors_LECSASV"; //"FlightsInSectors";  //
    String fname_c="capacities";
    if (args.length==2) {
      fname_c=args[0];
      fname=args[1];
    }
    DataKeeper dk=new DataKeeper(fname,fname_c);
    if (!dk.sectors.isEmpty())
      createAndShowGUI(dk);
  }

}
