set fnCapacities="C:\\CommonGISprojects\\tracks-avia\\TAPAS\\ATFCM-20210331\\0_delays\\scenario_20190801_capacities" 
set fnDecisions="C:\\CommonGISprojects\\tracks-avia\\TAPAS\\ATFCM-20210331\\0_delays\\scenario_20190801_exp0_decisions" 
set fnFlightPlans="C:\\CommonGISprojects\\tracks-avia\\TAPAS\\ATFCM-20210331\\0_delays\\scenario_20190801_exp0_baseline_flight_plans"
\jdk8u144x64\bin\java -Xms1024m -Xmx14g -cp TapasSolutionExplorer.jar Main %fnCapacities% %fnDecisions% %fnFlightPlans% 
pause