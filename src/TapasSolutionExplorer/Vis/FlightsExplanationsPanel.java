package TapasSolutionExplorer.Vis;

import TapasDataReader.ExTreeReconstructor;
import TapasDataReader.Explanation;
import TapasDataReader.ExplanationItem;
import TapasDataReader.Flight;
import TapasExplTreeViewer.ui.ExTreePanel;
import TapasUtilities.RenderLabelBarChart;
import TapasUtilities.RenderLabel_ValueInSubinterval;


import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.*;

public class FlightsExplanationsPanel extends JPanel {

  protected JTable tableList=null,
         tableExpl=null;
  protected JLabel lblExplTitle=null;
  protected FlightsSingleExplTableModel tableExplModel=null;
  protected int selectedRow=-1;

  protected ExTreeReconstructor exTreeReconstructor=null;
  protected ExTreePanel exTreePanel=null;
  protected JFrame frame=null;

  public FlightsExplanationsPanel (Hashtable<String,int[]> attrsInExpl, Vector<Flight> vf,
                                   int decisionSteps[], int minStep, int maxStep,
                                   boolean bShowZeroActions) {
    super();

    // compute table statistics
    HashSet<String> features=new HashSet<>(10);
    int maxNcond=0, maxNfeatures=0;
    
    if (minStep<0)
      minStep=0;
    if (maxStep<0 || maxStep>=decisionSteps.length)
      maxStep=decisionSteps.length-1;
    
    for (Flight f:vf)
      for (int step=minStep; step<=maxStep && step<f.expl.length; step++)
        if (f.expl==null || f.expl[step]==null)
          ; //System.out.println("* no explanation: flight "+f.id+", step="+step);
        else
          if (f.expl[step].action>0 || bShowZeroActions) {
            ExplanationItem[] e=f.expl[step].eItems,
                              ee=Explanation.getExplItemsCombined(e);
            maxNcond=Math.max(maxNcond,e.length);
            maxNfeatures=Math.max(maxNfeatures,ee.length);
            for (int i=0; i<ee.length; i++)
              features.add(ee[i].attr);
          }
    //System.out.println("* N distinct features = "+features.size());
    ArrayList<String> list = new ArrayList<>(features);
    Collections.sort(list);
    //for (String s:list)
      //System.out.println(s);

    frame = new JFrame("Explanations for " + ((vf.size()==1) ?
                                                         vf.elementAt(0).id :
                                                         vf.size() + " flights") + " at steps ["+minStep+".."+maxStep+"]");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    JCheckBox cbExplCombine=new JCheckBox("Combine intervals",false);
    JCheckBox cbExplAsInt=new JCheckBox("Integer intervals",false);
    lblExplTitle=new JLabel("",JLabel.CENTER);

    Hashtable<String, Flight> hFlights=new Hashtable<>(vf.size());
    for (Flight f:vf)
      hFlights.put(f.id,f);
    exTreeReconstructor=new ExTreeReconstructor();
    exTreeReconstructor.setAttrMinMaxValues(attrsInExpl);
    int stepMinMax[]=null, actionMinMax[]=null;
    if (decisionSteps!=null && (minStep>0 || maxStep<decisionSteps.length-1)) {
      stepMinMax=new int[2];
      stepMinMax[0]=decisionSteps[minStep];
      stepMinMax[1]=decisionSteps[maxStep];
    }
    if (!bShowZeroActions) {
      actionMinMax=new int[2];
      actionMinMax[0]=1;
      actionMinMax[1]=Integer.MAX_VALUE;
    }
    if (!exTreeReconstructor.reconstructExTree(hFlights,actionMinMax,stepMinMax))
      System.out.println("Failed to reconstruct the explanation tree!");
    exTreePanel=new ExTreePanel(exTreeReconstructor.topNodes);

    FlightsListOfExplTableModel tableListModel=new FlightsListOfExplTableModel(vf,attrsInExpl,list,minStep,maxStep,bShowZeroActions);
    tableExplModel=new FlightsSingleExplTableModel(attrsInExpl);

    tableList=new JTable(tableListModel){
      public String getToolTipText(MouseEvent e) {
        String s = "";
        java.awt.Point p = e.getPoint();
        int rowIndex = rowAtPoint(p), colIndex = columnAtPoint(p);
        if (rowIndex>=0) {
          int realRowIndex = convertRowIndexToModel(rowIndex), realColIndex=convertColumnIndexToModel(colIndex);
          if (realColIndex<5)
            return "";
          String fea=list.get(realColIndex-5);
          //Explanation expl = vf.elementAt(tableListModel.rowFlNs[realRowIndex]).expl[tableListModel.rowFlSteps[realRowIndex]];
          s="<html><body style=background-color:rgb(255,255,204)>"; //"<p align=center><b>"+fea+"</b></p>\n";
          s+=tableListModel.getFeatureExplanation(fea)+"\n";
          Explanation expl = vf.elementAt(tableListModel.rowFlNs[realRowIndex]).expl[tableListModel.rowFlSteps[realRowIndex]];
          ExplanationItem eItems[]=expl.eItems;
          if (cbExplCombine.isSelected())
            eItems=Explanation.getExplItemsCombined(eItems);
          if (cbExplAsInt.isSelected())
            eItems=Explanation.getExplItemsAsIntegeres(eItems,attrsInExpl);
          int n=-1;
          for (int i=0; n==-1 && i<eItems.length; i++)
            if (fea.equals(eItems[i].attr))
              n=i;
          if (n==-1)
            return "";
            //s += "<tr><td>feature "+fea+" is not used in this explanation</td></tr>\n";
          else {
            if (eItems[n].sector!=null && !eItems[n].sector.equals("null"))
              s+="<p align=center style=\"margin: 10px\">Sector = <b>"+eItems[n].sector+"</b></p>\n";
            s+="<table>\n";
            s+="<tr><td>Value</td><td>" + getFloatAsString(eItems[n].value) + "</td></tr>\n";
            s+="<tr><td>Condition min..max</td><td>["+getFloatAsString((float)eItems[n].interval[0])+
                  " .. "+getFloatAsString((float)eItems[n].interval[1])+"]</td></tr>\n";
            int minmax[]=attrsInExpl.get(eItems[n].attr);
            s+="<tr><td>Global min..max</td><td>["+minmax[0]+" .. "+minmax[1]+"]</td></tr>\n";
            s+="</table>\n";
          }
          s+="</body></html>";
        }
        return s;
      }
    };
    tableList.setTableHeader(new FlightsListOfExplTableHeader(tableList.getColumnModel(),tableListModel,list));

    tableList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        selectedRow=tableList.getSelectedRow();
        if (selectedRow>=0) {
          int row=tableList.convertRowIndexToModel(selectedRow);
          Explanation expl = vf.elementAt(tableListModel.rowFlNs[row]).expl[tableListModel.rowFlSteps[row]];
          tableExpl.getColumnModel().getColumn(0).setCellRenderer(new RenderLabelBarChart(0, expl.eItems.length));
          setExpl(attrsInExpl, expl, cbExplCombine.isSelected(), cbExplAsInt.isSelected());
        }
      }
    });
    tableList.setPreferredScrollableViewportSize(new Dimension(1200, 500));
    tableList.setFillsViewportHeight(true);
    tableList.setAutoCreateRowSorter(true);
    tableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    tableList.setRowSelectionAllowed(true);
    tableList.setColumnSelectionAllowed(false);
    DefaultTableCellRenderer centerRenderer=new DefaultTableCellRenderer();
    centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
    tableList.getColumnModel().getColumn(1).setCellRenderer(new RenderLabelBarChart(minStep,maxStep));
    tableList.getColumnModel().getColumn(2).setCellRenderer(new RenderLabelBarChart(0,10));
    tableList.getColumnModel().getColumn(3).setCellRenderer(new RenderLabelBarChart(0,maxNcond));
    tableList.getColumnModel().getColumn(4).setCellRenderer(new RenderLabelBarChart(0,maxNfeatures));
    for (int i=0; i<list.size(); i++)
      tableList.getColumnModel().getColumn(5+i).setCellRenderer(new RenderLabel_ValueInSubinterval());
    JScrollPane scrollPaneList = new JScrollPane(tableList);
    scrollPaneList.setOpaque(true);

    tableExpl=new JTable(tableExplModel) {
      public String getToolTipText(MouseEvent e) {
        String s = "";
        java.awt.Point p = e.getPoint();
        int rowIndex = rowAtPoint(p);
        if (rowIndex>=0) {
          int realRowIndex = convertRowIndexToModel(rowIndex);
          s="<html><body style=background-color:rgb(255,255,204)>\n"; //"<p align=center><b>"+tableExplModel.eItems[realRowIndex].attr+"</b></p>\n";
          s+=tableListModel.getFeatureExplanation(tableExplModel.eItems[realRowIndex].attr)+"\n";
          if (tableExplModel.eItems[realRowIndex].sector!=null && !tableExplModel.eItems[realRowIndex].sector.equals("null"))
            s+="<p align=center style=\"margin: 10px\">Sector = <b>"+tableExplModel.eItems[realRowIndex].sector+"</b></p>\n";
          s+="<table>\n";
          s+="<tr><td>Value</td><td>"+getFloatAsString(tableExplModel.eItems[realRowIndex].value)+"</td></tr>\n";
          s+="<tr><td>Condition min..max</td><td>["+getFloatAsString((float)tableExplModel.eItems[realRowIndex].interval[0])+
                  " .. "+getFloatAsString((float)tableExplModel.eItems[realRowIndex].interval[1])+"]</td></tr>\n";
          int minmax[]=attrsInExpl.get(tableExplModel.eItems[realRowIndex].attr);
          s+="<tr><td>Global min..max</td><td>["+minmax[0]+" .. "+minmax[1]+"]</td></tr>\n";
          s+="</table>\n";
          s+="</body></html>";
        }
        return s;
      }
    };
    tableExpl.setPreferredScrollableViewportSize(new Dimension(200, 500));
    tableExpl.setFillsViewportHeight(true);
    tableExpl.setAutoCreateRowSorter(true);
    tableExpl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    tableExpl.setRowSelectionAllowed(true);
    tableExpl.setColumnSelectionAllowed(false);
    tableExpl.getColumnModel().getColumn(3).setCellRenderer(new RenderLabel_ValueInSubinterval());
    JScrollPane scrollPaneExpl = new JScrollPane(tableExpl);
    scrollPaneExpl.setOpaque(true);
    
    tableExpl.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (exTreePanel!=null) {
          selectedRow = tableExpl.getSelectedRow();
          if (selectedRow >= 0) {
            int row = tableExpl.convertRowIndexToModel(selectedRow);
            exTreePanel.highlightExplanationItem(tableExplModel.action,tableExplModel.eItems,row);
          }
        }
      }
    });

    JPanel pExpl=new JPanel(new BorderLayout());
    pExpl.add(scrollPaneExpl,BorderLayout.CENTER);
    pExpl.add(lblExplTitle,BorderLayout.NORTH);

    JSplitPane splitPaneVleft=new JSplitPane(JSplitPane.VERTICAL_SPLIT,scrollPaneList,new DynamicQueryPanel(tableListModel,new int[]{0,1,2,3,4}));
    splitPaneVleft.setOneTouchExpandable(true);
    splitPaneVleft.setDividerLocation(500);
    JSplitPane splitPaneVright=new JSplitPane(JSplitPane.VERTICAL_SPLIT,pExpl,exTreePanel);
    splitPaneVright.setOneTouchExpandable(true);
    splitPaneVright.setDividerLocation(500);
    Dimension minimumSize = new Dimension(100, 300);
    pExpl.setMinimumSize(minimumSize);

    JSplitPane splitPane=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,splitPaneVleft,splitPaneVright);
    splitPane.setOneTouchExpandable(true);
    splitPane.setDividerLocation(1000);
    minimumSize = new Dimension(100, 300);
    scrollPaneList.setMinimumSize(minimumSize);
    scrollPaneExpl.setMinimumSize(minimumSize);

    cbExplCombine.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (selectedRow>=0) {
          updateExTreePanel(splitPaneVright,cbExplCombine.isSelected(),cbExplAsInt.isSelected());
          int row=tableList.convertRowIndexToModel(selectedRow);
          Explanation expl=vf.elementAt(tableListModel.rowFlNs[row]).expl[tableListModel.rowFlSteps[row]];
          setExpl(attrsInExpl,expl,cbExplCombine.isSelected(),cbExplAsInt.isSelected());
        }
      }
    });
    cbExplAsInt.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        tableListModel.setbValuesAreInteger(cbExplAsInt.isSelected());
        if (selectedRow>=0) {
          updateExTreePanel(splitPaneVright,cbExplCombine.isSelected(),cbExplAsInt.isSelected());
          int row=tableList.convertRowIndexToModel(selectedRow);
          Explanation expl=vf.elementAt(tableListModel.rowFlNs[row]).expl[tableListModel.rowFlSteps[row]];
          setExpl(attrsInExpl,expl,cbExplCombine.isSelected(),cbExplAsInt.isSelected());
        }
      }
    });

    frame.getContentPane().add(splitPane, BorderLayout.CENTER);
    JPanel controlPanel=new JPanel(new FlowLayout());
    controlPanel.add(cbExplCombine);
    controlPanel.add(cbExplAsInt);
    frame.getContentPane().add(controlPanel, BorderLayout.SOUTH);
    frame.pack();
    Dimension size=Toolkit.getDefaultToolkit().getScreenSize();
    frame.setLocation((size.width-frame.getWidth())/2,(size.height-frame.getHeight())/2);
    frame.setVisible(true);
  }
  
  public JFrame getFrame() {
    return frame;
  }

  public void updateExTreePanel (JSplitPane splitPaneV, boolean bCombine, boolean bInt) {
    if (bCombine)
      if (bInt)
        splitPaneV.setBottomComponent(exTreePanel=new ExTreePanel(exTreeReconstructor.topNodesIntExCombined));
      else
        splitPaneV.setBottomComponent(exTreePanel=new ExTreePanel(exTreeReconstructor.topNodesExCombined));
    else
      if (bInt)
        splitPaneV.setBottomComponent(exTreePanel=new ExTreePanel(exTreeReconstructor.topNodesInt));
      else
        splitPaneV.setBottomComponent(exTreePanel=new ExTreePanel(exTreeReconstructor.topNodes));
  }

  public String getFloatAsString (float f) {
    if (f==Math.round(f))
      return ""+(int)f;
    else
      return ""+f;
  }
  protected void setExpl (Hashtable<String,int[]> attrsInExpl, Explanation expl, boolean bCombine, boolean bInt) {
    lblExplTitle.setText(expl.FlightID+" @ "+expl.step+", action="+expl.action);
    ExplanationItem eItems[]=expl.eItems;
    if (bCombine)
      eItems=Explanation.getExplItemsCombined(eItems);
    if (bInt)
      eItems=Explanation.getExplItemsAsIntegeres(eItems,attrsInExpl);
    tableExplModel.setExpl(eItems);
    tableExplModel.action=expl.action;
    if (exTreePanel!=null)
      exTreePanel.expandExplanation(expl.action,eItems);
  }

  class FlightsListOfExplTableHeader extends JTableHeader {
    FlightsListOfExplTableModel dataModel=null;
    ArrayList<String> list=null;
    public FlightsListOfExplTableHeader(TableColumnModel model, FlightsListOfExplTableModel dataModel, ArrayList<String> list) {
      super(model);
      this.dataModel=dataModel;
      this.list=list;
    }
    public String getToolTipText(MouseEvent e) {
      java.awt.Point p = e.getPoint();
      int index = columnModel.getColumnIndexAtX(p.x);
      int realIndex = columnModel.getColumn(index).getModelIndex();
      if (realIndex>=5) {
        String fea=list.get(realIndex - 5);
        String s="<html><body style=background-color:rgb(255,255,204)>";
        s+=dataModel.getFeatureExplanation(fea);
        s+="</body></html>";
        return s;
      }
      else
        return "";
    }
  }
  class FlightsListOfExplTableModel extends AbstractTableModel {
    Vector<Flight> vf = null;
    ArrayList<String> listOfFeatures=null;
    Hashtable<String,int[]> attrsInExpl=null;
    int minStep, maxStep;
    boolean bShowZeroActions, bValuesAreInteger=false;
    public int rowFlNs[] = null;
    public int rowFlSteps[] = null;

    public FlightsListOfExplTableModel(Vector<Flight> vf, Hashtable<String,int[]> attrsInExpl, ArrayList<String> listOfFeatures, int minStep, int maxStep, boolean bShowZeroActions) {
      this.vf = vf;
      this.attrsInExpl=attrsInExpl;
      this.listOfFeatures=listOfFeatures;
      this.minStep = minStep;
      this.maxStep = maxStep;
      this.bShowZeroActions = bShowZeroActions;
      calcNexpl();
    }
    public void setbValuesAreInteger (boolean bValuesAreInteger) {
      this.bValuesAreInteger=bValuesAreInteger;
      int selRow=tableList.getSelectedRow();
      fireTableDataChanged();
      if (selRow>=0)
        tableList.setRowSelectionInterval(selRow,selRow);
    }
    protected void calcNexpl() {
      int n = 0;
      for (Flight f : vf)
        for (int step = minStep; step <= maxStep; step++)
          if (f.expl[step] != null && (f.expl[step].action > 0 || bShowZeroActions))
            n++;
      rowFlNs = new int[n];
      rowFlSteps = new int[n];
      n = 0;
      for (int i=0; i<vf.size(); i++) {
        Flight f=vf.elementAt(i);
        for (int step = minStep; step <= maxStep; step++)
          if (f.expl[step] != null && (f.expl[step].action > 0 || bShowZeroActions)) {
            rowFlNs[n] = i;
            rowFlSteps[n] = step;
            n++;
          }
      }
    }
    private String columnNames[] = {"Flight ID", "Step", "Action", "N conditions", "N features"};
    public String getColumnName(int col) {
      return ((col<columnNames.length) ? columnNames[col] : listOfFeatures.get(col-columnNames.length));
    }
    public int getColumnCount() {
      return columnNames.length + listOfFeatures.size();
    }
    public int getRowCount() {
      return rowFlNs.length;
    }
    public Class getColumnClass(int c) {
      return (getValueAt(0, c) == null) ? null : getValueAt(0, c).getClass();
    }
    public Object getValueAt(int row, int col) {
      Flight f=vf.elementAt(rowFlNs[row]);
      switch (col) {
        case 0:
          return f.id;
        case 1:
          return rowFlSteps[row];
        case 2:
          return f.expl[rowFlSteps[row]].action;
        case 3:
          return f.expl[rowFlSteps[row]].eItems.length;
        case 4:
          HashSet<String> features=new HashSet<>(10);
          for (int i=0; i<f.expl[rowFlSteps[row]].eItems.length; i++)
            features.add(f.expl[rowFlSteps[row]].eItems[i].attr);
          return features.size();
        default:
          ExplanationItem e[]=f.expl[rowFlSteps[row]].eItems,
                          ee[]=Explanation.getExplItemsCombined(e);
          if (bValuesAreInteger)
            ee=Explanation.getExplItemsAsIntegeres(ee,attrsInExpl);
          int n=-1;
          for (int i=0; n==-1 && i<ee.length; i++)
            if (ee[i].attr.equals(listOfFeatures.get(col-columnNames.length)))
              n=i;
          if (n==-1)
            return new float[]{-1,0,1,0,1};
          else {
            //System.out.println("row="+row+", col="+col+", Ncond="+e.length+", Nfeatures="+ee.length+", featureN="+n+" "+ee[n].attr);
            float v1=attrsInExpl.get(ee[n].attr)[0], v2=attrsInExpl.get(ee[n].attr)[1],
                  v3=(float)ee[n].interval[0], v4=(float)ee[n].interval[1];
            if (v3==Float.NEGATIVE_INFINITY)
              v3=v1;
            if (v4==Float.POSITIVE_INFINITY)
              v4=v2;
            return new float[]{ee[n].value,v1,v2,v3,v4};
          }
      }
      //return 0;
    }
    public String getFeatureExplanation (String fea) {
      String s="<p align=center><b>"+fea+"</b></p>\n<p style=\"margin: 10px\">";
      if (fea.startsWith("NumberOfDe"))
        s+="Delay the flight has accumulated<br>up to this step.";
      if (fea.startsWith("NumberOfH"))
        s+="Total number of hotspots the flight participates in.";
      if (fea.startsWith("PeriodOfH"))
        s+="The period in which the corresponding sector has hotspot.";
      if (fea.startsWith("SectorOfH"))
        s+="The Nth hotspot the flight participates in.<br>Sectors here are presented with an id subjective to the flight, <br>corresponding to the sequence of sectors crossed by the trajectory. <br>Specifically, number 0 in this list means that this flight participates in <br>a hotspot on the first sector it crosses, 1 at the second sector of its flight plan etc.";
      if (fea.startsWith("DurationInS"))
        s+="Duration (in minutes) the flight remains in Nth sector it crosses, <br>with respect to the sequence of sectors crossed by the trajectory.";
      if (fea.startsWith("TakeOffM"))
        s+="The minute of day the flight takes off given the delay (CTOT).";
      s+="</p>";
      return s;
    }
  }

  class FlightsSingleExplTableModel extends AbstractTableModel {
    protected Hashtable<String,int[]> attrsInExpl=null;
    public int action=-1;
    public ExplanationItem eItems[]=null;
    public FlightsSingleExplTableModel (Hashtable<String,int[]> attrsInExpl) {
      this.attrsInExpl=attrsInExpl;
    }
    public void setExpl (ExplanationItem eItems[]) {
      this.eItems=eItems;
      fireTableDataChanged();
    }
    private String columnNames[] = {"Level", "Feature", /* "Value", "min", "max", "interval_min", "interval-max",*/ "Sector", "Value"};
    public String getColumnName(int col) {
      return columnNames[col];
    }
    public int getColumnCount() {
      return columnNames.length;
    }
    public int getRowCount() {
      return (eItems==null)?0:eItems.length;
    }
    public Class getColumnClass(int c) {
      return (getValueAt(0, c) == null) ? null : getValueAt(0, c).getClass();
    }
    public Object getValueAt(int row, int col) {
      switch (col) {
        case 0:
          return row;
        case 1:
          return eItems[row].attr;
/*
        case 2:
          return eItems[row].value;
        case 3:
          int minmax[]=attrsInExpl.get(eItems[row].attr);
          return minmax[0];
        case 4:
          minmax=attrsInExpl.get(eItems[row].attr);
          return minmax[1];
        case 5:
          return eItems[row].interval[0];
        case 6:
          return eItems[row].interval[1];
*/
        case 3:
          float v1=attrsInExpl.get(eItems[row].attr)[0], v2=attrsInExpl.get(eItems[row].attr)[1],
                v3=(float)eItems[row].interval[0], v4=(float)eItems[row].interval[1];
          if (v3==Float.NEGATIVE_INFINITY)
            v3=v1;
          if (v4==Float.POSITIVE_INFINITY)
            v4=v2;
          return new float[]{eItems[row].value,v1,v2,v3,v4};
        case 2:
          return ("null".equals(eItems[row].sector))?"":eItems[row].sector;
      }
      return 0;
    }

  }

}
