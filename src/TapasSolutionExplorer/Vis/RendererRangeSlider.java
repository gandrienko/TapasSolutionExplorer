package TapasSolutionExplorer.Vis;

import TapasUtilities.RangeSlider;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class RendererRangeSlider extends RangeSlider implements TableCellRenderer {

  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    if (value==null)
      return null;
    if (isSelected)
      setBackground(table.getSelectionBackground());
    else
      setBackground(table.getBackground());
    Rectangle r=table.getCellRect(row,column,true);
    setSize(r.width,r.height);
    setValues((int[])value);
    return this;
  }

  public void setValues (int values[]) {
    setMinimum(values[0]);
    setMaximum(values[1]);
    setValue(values[2]);
    setUpperValue(values[3]);
  }
}
