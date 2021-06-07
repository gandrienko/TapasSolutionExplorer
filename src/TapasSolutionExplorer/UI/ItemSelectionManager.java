package TapasSolutionExplorer.UI;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.ArrayList;
import java.util.TreeSet;

public class ItemSelectionManager {
  public TreeSet selected=null;
  
  protected ArrayList<ChangeListener> changeListeners=null;
  
  public void addChangeListener(ChangeListener l) {
    if (changeListeners==null)
      changeListeners=new ArrayList(5);
    if (!changeListeners.contains(l))
      changeListeners.add(l);
  }
  
  public void removeChangeListener(ChangeListener l) {
    if (l!=null && changeListeners!=null)
      changeListeners.remove(l);
  }
  
  public void notifyChange(){
    if (changeListeners==null || changeListeners.isEmpty())
      return;
    ChangeEvent e=new ChangeEvent(this);
    for (ChangeListener l:changeListeners)
      l.stateChanged(e);
  }
  
  public void select(Object obj) {
    if (obj==null)
      return;
    if (selected==null)
      selected=new TreeSet();
    if (!selected.contains(obj)) {
      selected.add(obj);
      notifyChange();
    }
  }
  
  public void deselect(Object obj) {
    if (obj==null || selected==null || selected.isEmpty() || !selected.contains(obj))
      return;
    selected.remove(obj);
    notifyChange();
  }
  
  public void deselectAll() {
    if (selected==null || selected.isEmpty())
      return;
    selected.clear();
    notifyChange();
  }
  
  public boolean hasSelection() {
    return selected!=null && !selected.isEmpty();
  }
  
  public boolean isSelected(Object obj) {
    return obj!=null && selected!=null && selected.contains(obj);
  }
  
  public TreeSet getSelected(){
    return selected;
  }
  
}
