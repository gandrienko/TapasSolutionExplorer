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
    ControlPanel cp=new ControlPanel(dk,ic,null);
    frame.getContentPane().add(cp, BorderLayout.SOUTH);
    frame.getContentPane().add(ic, BorderLayout.CENTER);

    //Display the window.
    frame.pack();
    frame.setVisible(true);

    //Create and set up the window for a single sector.
    frame = new JFrame("TAPAS Solution Explorer: all sectors");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    InfoCanvasAll icAll=new InfoCanvasAll(dk);
    int sts[]=new int[]{0,dk.Nsteps-1};
    icAll.setSTS(sts);
    InfoSteps is=new InfoSteps(dk,icAll);
    is.setSelectedSteps(icAll.getSTS());
    cp=new ControlPanel(dk,icAll,is);
    frame.getContentPane().add(cp, BorderLayout.SOUTH);

    JPanel p=new JPanel(new BorderLayout());
    p.add(icAll,BorderLayout.CENTER);
    p.add(is,BorderLayout.SOUTH);

    //JSplitPane splitPane=new JSplitPane(JSplitPane.VERTICAL_SPLIT,icAll,is);
    //splitPane.setOneTouchExpandable(true);
    //splitPane.setDividerLocation(0.1);
    //icAll.setMinimumSize(new Dimension(1000,750));
    //is.setMinimumSize(new Dimension(1000,123));
    //frame.getContentPane().add(splitPane, BorderLayout.CENTER);
    frame.getContentPane().add(p, BorderLayout.CENTER);

    //Display the window.
    frame.pack();
    frame.setVisible(true);
  }

  public static void main(String[] args) {
    DataKeeper dk=null;
    if (args.length==2)
      dk=new DataKeeper(args[0],args[1]);
    else
      if (args.length==3)
        dk=new DataKeeper(args[0],args[1],args[2]);
      else {
        String fnCapacities="C:\\CommonGISprojects\\tracks-avia\\TAPAS\\ATFCM-20210331\\0_delays\\scenario_20190801_capacities",
               fnDecisions="C:\\CommonGISprojects\\tracks-avia\\TAPAS\\ATFCM-20210331\\0_delays\\scenario_20190801_exp0_decisions",
               fnFlightPlans="C:\\CommonGISprojects\\tracks-avia\\TAPAS\\ATFCM-20210331\\0_delays\\scenario_20190801_exp0_baseline_flight_plans";
        dk=new DataKeeper(fnCapacities,fnDecisions,fnFlightPlans);
      }
    if (dk!=null && !dk.sectors.isEmpty())
      createAndShowGUI(dk);
  }

}
