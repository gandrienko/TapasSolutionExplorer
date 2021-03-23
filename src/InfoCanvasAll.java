public class InfoCanvasAll extends InfoCanvasBasics {

  int sts[]=null; // Steps to Show


  public InfoCanvasAll (DataKeeper dk) {
    super(dk);
  }

  public void setSTS (int sts[]) {
    this.sts=sts;
    plotImageValid=false;
    repaint();
  }



}
