import TapasSolutionExplorer.Data.DataKeeper;
import TapasSolutionExplorer.UI.CreateUI;

public class Main {

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
    if (dk!=null && !dk.getSectors().isEmpty())
      new CreateUI(dk);
  }

}
