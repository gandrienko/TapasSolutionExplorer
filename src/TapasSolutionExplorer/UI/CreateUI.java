package TapasSolutionExplorer.UI;

import TapasSolutionExplorer.Data.DataKeeper;
import TapasSolutionExplorer.Vis.InfoCanvasAll;
import TapasSolutionExplorer.Vis.InfoSteps;

import javax.swing.*;
import java.awt.*;

public class CreateUI {

  public CreateUI (DataKeeper dk) {
    //Create and set up the window for all sectors
    JFrame frame = new JFrame("TAPAS Solution Explorer: all sectors");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    InfoCanvasAll icAll=new InfoCanvasAll(dk);
    int sts[]=new int[]{0,dk.Nsteps-1};
    icAll.setSTS(sts);
    InfoSteps is=new InfoSteps(dk,icAll);
    is.setSelectedSteps(icAll.getSTS());
    ControlPanel cp=new ControlPanel(dk,icAll,is,null);
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
    frame.pack();
    frame.setVisible(true);
  }

}
