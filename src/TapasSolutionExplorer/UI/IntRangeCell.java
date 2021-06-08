package TapasSolutionExplorer.UI;

import TapasUtilities.RangeSlider;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class IntRangeCell extends AbstractCellEditor
    implements TableCellRenderer, TableCellEditor {
  public RangeSlider rSlider=null;
  
  public IntRangeCell() {
    rSlider=new RangeSlider();
  }
  
  public Component getTableCellRendererComponent(JTable table, Object value,
                                                 boolean isSelected,
                                                 boolean hasFocus, int row, int column) {
    if (value==null || !(value instanceof IntSubRange))
      return null;
    IntSubRange r=(IntSubRange)value;
    rSlider.setMinimum(r.absMin);
    rSlider.setMaximum(r.absMax);
    rSlider.setValue(r.currMin);
    rSlider.setUpperValue(r.currMax);
    rSlider.setBackground((isSelected)?table.getSelectionBackground():table.getBackground());
    return rSlider;
  }
  
  public Component getTableCellEditorComponent(JTable table, Object value,
                                               boolean isSelected, int row, int column) {
    if (value==null || !(value instanceof IntSubRange))
      return null;
    IntSubRange r=(IntSubRange)value;
    rSlider.setMinimum(r.absMin);
    rSlider.setMaximum(r.absMax);
    rSlider.setValue(r.currMin);
    rSlider.setUpperValue(r.currMax);
    rSlider.setBackground((isSelected)?table.getSelectionBackground():table.getBackground());
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
  
}
