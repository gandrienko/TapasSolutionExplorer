import TapasSolutionExplorer.Data.DataKeeper;
import TapasSolutionExplorer.UI.CreateUI;

import java.io.*;
import java.util.Hashtable;
import java.util.Map;

public class Main {

  public static void main(String[] args) {
    DataKeeper dk=null;
    if (args!=null && args.length==1) {
      String parFileName=args[0];
  
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
      dk=new DataKeeper(fnCapacities,fnDecisions,fnFlightPlans);
    }
    else
    if (args.length>=3 && args[2].indexOf('=')>0) {
      String s[]=new String[args.length-2];
      for (int i=0; i<s.length; i++)
        s[i]=args[i+2];
      dk=new DataKeeper(args[0],args[1],s); // fnCapacities,fnDecisions,fnFlightPlans);
    }
    else
    if (args.length==2)
      dk=new DataKeeper(args[0],args[1]);
    else
      if (args.length==3)
        dk=new DataKeeper(args[0],args[1],args[2]);
      else {
/*
        String fnCapacities="C:\\CommonGISprojects\\tracks-avia\\TAPAS\\ATFCM-20210331\\0_delays\\scenario_20190801_capacities",
                fnDecisions="C:\\CommonGISprojects\\tracks-avia\\TAPAS\\ATFCM-20210331\\0_delays\\scenario_20190801_exp0_decisions",
                fnFlightPlans="C:\\CommonGISprojects\\tracks-avia\\TAPAS\\ATFCM-20210331\\0_delays\\scenario_20190801_exp0_baseline_flight_plans";
*/
        String fnCapacities="C:\\CommonGISprojects\\tracks-avia\\TAPAS\\AIML-20210531\\20190704\\0_delays\\scenario_20190704_capacities",
                fnDecisions="C:\\CommonGISprojects\\tracks-avia\\TAPAS\\AIML-20210531\\20190704\\0_delays\\scenario_20190704_exp0_decisions",
                fnFlightPlans="C:\\CommonGISprojects\\tracks-avia\\TAPAS\\AIML-20210531\\20190704\\0_delays\\scenario_20190704_exp0_baseline_flight_plans";
        dk=new DataKeeper(fnCapacities,fnDecisions,fnFlightPlans);
      }
    if (dk!=null && !dk.getSectors().isEmpty())
      new CreateUI(dk);
  }

}
