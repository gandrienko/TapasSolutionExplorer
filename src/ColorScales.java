import javafx.scene.paint.Color;

public class ColorScales {

  /**
   * source: https://stackoverflow.com/questions/470690/how-to-automatically-generate-n-distinct-colors
   */
  public static final Color[] KELLY_COLORS = {
          Color.web("0xFFB300"),    // Vivid Yellow
          Color.web("0x803E75"),    // Strong Purple
          Color.web("0xFF6800"),    // Vivid Orange
          Color.web("0xA6BDD7"),    // Very Light Blue
          Color.web("0xC10020"),    // Vivid Red
          Color.web("0xCEA262"),    // Grayish Yellow
          Color.web("0x817066"),    // Medium Gray

          Color.web("0x007D34"),    // Vivid Green
          Color.web("0xF6768E"),    // Strong Purplish Pink
          Color.web("0x00538A"),    // Strong Blue
          Color.web("0xFF7A5C"),    // Strong Yellowish Pink
          Color.web("0x53377A"),    // Strong Violet
          Color.web("0xFF8E00"),    // Vivid Orange Yellow
          Color.web("0xB32851"),    // Strong Purplish Red
          Color.web("0xF4C800"),    // Vivid Greenish Yellow
          Color.web("0x7F180D"),    // Strong Reddish Brown
          Color.web("0x93AA00"),    // Vivid Yellowish Green
          Color.web("0x593315"),    // Deep Yellowish Brown
          Color.web("0xF13A13"),    // Vivid Reddish Orange
          Color.web("0x232C16"),    // Dark Olive Green
  };

  public static java.awt.Color getKellyColor(int idx) {
    javafx.scene.paint.Color fx = KELLY_COLORS[idx];
    java.awt.Color awtColor=new java.awt.Color((float) fx.getRed(), (float) fx.getGreen(), (float) fx.getBlue(), (float) fx.getOpacity());
    return awtColor;
  }
  public static int[] getKellyColorAsRGB(int idx) {
    javafx.scene.paint.Color fx = KELLY_COLORS[idx];
    int rgb[]=new int[3];
    rgb[0]=(int)Math.round(255*fx.getRed());
    rgb[1]=(int)Math.round(255*fx.getGreen());
    rgb[2]=(int)Math.round(255*fx.getBlue());
    return rgb;
  }

}
