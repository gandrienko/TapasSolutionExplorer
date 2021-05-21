import TapasDataReader.Flight;
import TapasDataReader.Record;
import TapasSolutionExplorer.Data.FlightConstructor;
import TapasSolutionExplorer.Data.FlightInSector;
import TapasSolutionExplorer.UI.TableMouseListener;
import TapasSolutionExplorer.Vis.FlightVariantsTableModel;
import TapasSolutionExplorer.flight_vis.FlightVariantsShow;
import TapasSolutionExplorer.flight_vis.FlightViewManager;
import TapasSolutionExplorer.flight_vis.FlightVisPanel;
import TapasUtilities.RangeSlider;
import TapasUtilities.RenderLabelBarChart;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;

public class SeeFlight {
  
  public static void main(String[] args) {
    String parFileName=(args!=null && args.length>0)?args[0]:"params.txt";
  
    String path=null;
    Hashtable<String,String> fNames=new Hashtable<String,String>(10);
    try {
      BufferedReader br = new BufferedReader(
          new InputStreamReader(
              new FileInputStream(new File(parFileName)))) ;
      String strLine;
      try {
        while ((strLine = br.readLine()) != null) {
          String str=strLine.replaceAll("\"","").replaceAll(" ","");
          String[] tokens=str.split("=");
          if (tokens==null || tokens.length<2)
            continue;
          String parName=tokens[0].trim().toLowerCase();
          if (parName.equals("path") || parName.equals("data_path"))
            path=tokens[1].trim();
          else
            fNames.put(parName,tokens[1].trim());
        }
      } catch (IOException io) {
        System.out.println(io);
      }
    } catch (IOException io) {
      System.out.println(io);
    }
    if (path!=null) {
      for (Map.Entry<String,String> e:fNames.entrySet()) {
        String fName=e.getValue();
        if (!fName.startsWith("\\") && !fName.contains(":\\")) {
          fName=path+fName;
          fNames.put(e.getKey(),fName);
        }
      }
    }
    else
      path="";
  
    String fnDecisions=fNames.get("decisions");
    if (fnDecisions==null) {
      System.out.println("No decisions file name in the parameters!");
      return;
    }
    System.out.println("Decisions file name = "+fnDecisions);
    String fnFlightPlans=fNames.get("flight_plans");
    if (fnFlightPlans==null) {
      System.out.println("No flight plans file name in the parameters!");
      return;
    }
    System.out.println("Flight plans file name = "+fnFlightPlans);
    String fnCapacities=fNames.get("sector_capacities");
    if (fnCapacities==null) {
      System.out.println("No capacities file name in the parameters!");
      //return;
    }
    System.out.println("Capacities file name = "+fnCapacities);
  
    TreeSet<Integer> steps=TapasDataReader.Readers.readStepsFromDecisions(fnDecisions);
    if (steps==null || steps.isEmpty()) {
      System.out.println("Failed to read the decision steps from file "+fnDecisions+" !");
      return;
    }
    System.out.println("Got "+steps.size()+" decision steps!");
    Hashtable<String, Flight> flights=
        TapasDataReader.Readers.readFlightDelaysFromDecisions(fnDecisions,steps);
    if (flights==null || flights.isEmpty()) {
      System.out.println("Failed to read flight data from file "+fnDecisions+" !");
      return;
    }
    System.out.println("Got "+flights.size()+" flights from the decisions file!");
    Hashtable<String, Vector<Record>> records=TapasDataReader.Readers.readFlightPlans(fnFlightPlans,flights);
    if (records==null || records.isEmpty()) {
      System.out.println("Failed to read flight plans from file "+fnFlightPlans+" !");
      return;
    }
    System.out.println("Got "+flights.size()+" flight plans!");
    
    //for the flights that were changed, the numbers of the simulation steps corresponding to the plan changes
    Hashtable<String, int[]> flightSteps=new Hashtable<String, int[]>(flights.size());
    for (Flight fl:flights.values()) {
      if (fl.delays==null || fl.delays.length<2)
        continue;
      if (fl.delays[fl.delays.length-1]<1) //this flight was never delayed
        continue;
      ArrayList<Integer> fStepList=new ArrayList<Integer>(20);
      int iPrev=0;
      for (int i=1; i<fl.delays.length; i++)
        if (fl.delays[i]>fl.delays[iPrev]) {
          //store the step in which the delay was increased
          fStepList.add(i);
          iPrev=i;
        }
      if (!fStepList.isEmpty()) {
        int fSteps[]=new int[fStepList.size()];
        for (int i=0; i<fStepList.size(); i++)
          fSteps[i]=fStepList.get(i);
        flightSteps.put(fl.id,fSteps);
      }
    }
    if (flightSteps.isEmpty()) {
      System.out.println("Failed to get steps of change for any flight!");
      return;
    }
    System.out.println("Got the steps of changes for "+flightSteps.size()+" flights!");
  
    Dimension size=Toolkit.getDefaultToolkit().getScreenSize();
  
    FlightVariantsTableModel tModel=new FlightVariantsTableModel(flightSteps,flights);
    JTable table=new JTable(tModel);
    table.setPreferredScrollableViewportSize(new Dimension(Math.round(size.width * 0.35f), Math.round(size.height * 0.4f)));
    table.setFillsViewportHeight(true);
    table.setAutoCreateRowSorter(true);
    for (int i=0; i<tModel.getColumnCount(); i++)
      table.getColumnModel().getColumn(i).setCellRenderer(tModel.getRendererForColumn(i));
  
    JScrollPane scrollPane = new JScrollPane(table);
  
    JPanel pAll=new JPanel(new BorderLayout());
    pAll.add(scrollPane,BorderLayout.CENTER);
    JPanel cp=new JPanel(new BorderLayout());
    cp.setBorder(BorderFactory.createLineBorder(Color.black));
    pAll.add(cp,BorderLayout.SOUTH);
    RangeSlider stepFocuser=new RangeSlider();
    cp.add(stepFocuser,BorderLayout.CENTER);
    stepFocuser.setMinimum(0);
    stepFocuser.setMaximum(tModel.maxStep);
    stepFocuser.setValue(0);
    stepFocuser.setUpperValue(tModel.maxStep);
  
    JTextField tfStart=new JTextField("0",4);
    JTextField tfEnd=new JTextField(String.valueOf(tModel.maxStep),4);
    JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,5,0));
    p.add(new JLabel("Steps:"));
    p.add(tfStart);
    cp.add(p,BorderLayout.WEST);
    p=new JPanel(new FlowLayout(FlowLayout.LEFT,5,0));
    p.add(tfEnd);
    JButton b=new JButton("Full range");
    b.setEnabled(false);
    p.add(b);
    cp.add(p,BorderLayout.EAST);
  
    stepFocuser.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        int low=stepFocuser.getValue(), high=stepFocuser.getUpperValue();
        tfStart.setText(""+low);
        tfEnd.setText(""+high);
        tModel.setStepsToShow(low,high);
        b.setEnabled(low>stepFocuser.getMinimum() || high<stepFocuser.getMaximum());
      }
    });
    b.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        stepFocuser.setFullRange();
      }
    });
    tfStart.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          int n=Integer.valueOf(tfStart.getText()).intValue();
          if (n>=0 && n<=tModel.maxStep && n<=stepFocuser.getUpperValue()) {
            stepFocuser.setValue(n);
            tModel.setStepsToShow(stepFocuser.getValue(),stepFocuser.getUpperValue());
          }
        } catch (NumberFormatException nfe) {
          tfStart.setText(""+stepFocuser.getValue());
        }
      }
    });
    tfEnd.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          int n=Integer.valueOf(tfEnd.getText()).intValue();
          if (n>=0 && n<=tModel.maxStep && n>=stepFocuser.getValue()) {
            stepFocuser.setUpperValue(n);
            tModel.setStepsToShow(stepFocuser.getValue(),stepFocuser.getUpperValue());
          }
        } catch (NumberFormatException nfe) {
          tfEnd.setText(""+stepFocuser.getUpperValue());
        }
      }
    });
    
    FlightViewManager flightViewManager=new FlightViewManager(flights,records);
    flightViewManager.setIncludeOnlyModifiedFlights(true);
  
    JPopupMenu menu=new JPopupMenu();
    JMenuItem mit=new JMenuItem("Show flight plan variants");
    menu.add(mit);
    mit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int selectedRow =table.convertRowIndexToModel(table.getSelectedRow());
        String flId=tModel.flightIds[selectedRow];
        flightViewManager.showFlightVariants(flId);
      }
    });
    table.setComponentPopupMenu(menu);
    table.addMouseListener(new TableMouseListener(table));
  
    JFrame fr = new JFrame("Flights (" + flightSteps.size() + ")");
    fr.getContentPane().add(pAll, BorderLayout.CENTER);
    fr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    //Display the window.
    fr.pack();
    fr.setLocation(30, 30);
    fr.setVisible(true);
  }
}
