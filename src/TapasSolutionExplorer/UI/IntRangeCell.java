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
  
  public RangeSlider rSlider=null;
  
  protected JTable editTable=null;
  protected int editRow=-1, editColumn=-1;
  
  public Component getTableCellRendererComponent(JTable table, Object value,
                                                 boolean isSelected,
                                                 boolean hasFocus, int row, int column) {
    return getSliderForTableCell(table,value,isSelected,row,column);
  }
  
  public Component getTableCellEditorComponent(JTable table, Object value,
                                               boolean isSelected, int row, int column) {
    editTable=table;
    editRow=row;
    editColumn=column;
    getSliderForTableCell(table,value,isSelected,row,column);
    rSlider.addChangeListener(this);
    return rSlider;
  }
  
  public RangeSlider getSliderForTableCell(JTable table, Object value,
                                           boolean isSelected, int row, int column) {
    if (value==null || !(value instanceof IntSubRange))
      return null;
    
    if (rSlider==null)
      rSlider = new RangeSlider();
    
    rSlider.removeChangeListener(this);
    
    IntSubRange r = (IntSubRange) value;
    rSlider.setMinimum(r.absMin);
    rSlider.setMaximum(r.absMax);
    rSlider.setValue(r.currMin);
    rSlider.setUpperValue(r.currMax);
    
    rSlider.setBackground((isSelected)?table.getSelectionBackground():table.getBackground());
    Rectangle rect=table.getCellRect(row,column,true);
    rSlider.setSize(rect.width,rect.height);
    return rSlider;
  }
  
  public Object getCellEditorValue() {
    IntSubRange r=new IntSubRange();
    r.absMin=rSlider.getMinimum();
    r.absMax=rSlider.getMaximum();
    r.currMin=rSlider.getValue();
    r.currMax=rSlider.getUpperValue();
    return r;
  }
  
  
  public void stateChanged(ChangeEvent e) {
    if (e.getSource().equals(rSlider)) {
      if (editTable!=null)
        editTable.setValueAt(getCellEditorValue(),editRow,editColumn);
    }
  }
}
