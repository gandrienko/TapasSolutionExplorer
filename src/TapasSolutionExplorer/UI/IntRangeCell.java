package TapasSolutionExplorer.UI;

import TapasUtilities.RangeSlider;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class IntRangeCell extends AbstractCellEditor
    implements TableCellRenderer, TableCellEditor, ChangeListener {
  
  public RangeSlider editSlider =null;
  
  protected JTable editTable=null;
  protected int editRow=-1, editColumn=-1;
  
  public Component getTableCellRendererComponent(JTable table, Object value,
                                                 boolean isSelected,
                                                 boolean hasFocus, int row, int column) {
    return getSliderForTableCell(table,value,isSelected,row,column);
  }
  
  public Component getTableCellEditorComponent(JTable table, Object value,
                                               boolean isSelected, int row, int column) {
    if (editSlider!=null) {
      editSlider.removeChangeListener(this);
      editSlider=null;
      editTable=null;
      editColumn=editRow=0;
    }
    editSlider=getSliderForTableCell(table,value,isSelected,row,column);
    editTable=table;
    editRow=row;
    editColumn=column;
    editSlider.addChangeListener(this);
    return editSlider;
  }
  
  public RangeSlider getSliderForTableCell(JTable table, Object value,
                                           boolean isSelected, int row, int column) {
    if (value==null || !(value instanceof IntSubRange))
      return null;
    
    RangeSlider rSlider=new RangeSlider();
    
    IntSubRange r = (IntSubRange) value;
    rSlider.setMinimum(r.absMin);
    rSlider.setMaximum(r.absMax);
    rSlider.setValue(r.currMin);
    rSlider.setUpperValue(r.currMax);
    
    rSlider.setBackground((isSelected)?table.getSelectionBackground():table.getBackground());
    Rectangle rect=table.getCellRect(row,column,false);
    rSlider.setSize(rect.width, rect.height);
    //System.out.println("Cell ("+row+","+column+") size = "+rect.width+" x "+rect.height+
                           //"; slider size = "+rSlider.getWidth()+" x "+rSlider.getHeight());
    return rSlider;
  }
  
  public Object getCellEditorValue() {
    if (editSlider==null)
      return null;
    IntSubRange r=new IntSubRange();
    r.absMin= editSlider.getMinimum();
    r.absMax= editSlider.getMaximum();
    r.currMin= editSlider.getValue();
    r.currMax= editSlider.getUpperValue();
    return r;
  }
  
  
  public void stateChanged(ChangeEvent e) {
    if (e.getSource().equals(editSlider)) {
      if (editTable!=null)
        editTable.setValueAt(getCellEditorValue(),editRow,editColumn);
    }
  }
}
