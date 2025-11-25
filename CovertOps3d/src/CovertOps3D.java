import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

public class CovertOps3D extends MIDlet {
   public static Display display = null;
   public static MainGameCanvas mainGameCanvas = null;
   private static CovertOps3D instance = null;

   public void startApp() {
      if (mainGameCanvas != null) {
         mainGameCanvas.sub_32d();
      } else {
         display = Display.getDisplay(this);
         mainGameCanvas = new MainGameCanvas();
         mainGameCanvas.sub_71();
         display.setCurrent(mainGameCanvas);
         MainGameCanvas.var_1e5 = this;
         instance = this;
      }
   }

   public void pauseApp() {
      mainGameCanvas.sub_308();
   }

   public void destroyApp(boolean var1) {
      mainGameCanvas.sub_35e();

      while(!mainGameCanvas.var_16f) {
         Thread.yield();
      }

      this.notifyDestroyed();
   }

   public static void exitApplication() {
       instance.destroyApp(true);
   }
}
