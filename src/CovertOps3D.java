import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

public class CovertOps3D extends MIDlet {
   public static Display display = null;
   public static MainGameCanvas mainGameCanvas = null;
   private static CovertOps3D instance = null;

   public void startApp() {
      if (mainGameCanvas != null) {
         mainGameCanvas.resumeGame();
      } else {
         display = Display.getDisplay(this);
         mainGameCanvas = new MainGameCanvas();
         mainGameCanvas.startGameThread();
         display.setCurrent(mainGameCanvas);
         MainGameCanvas.mainMidlet = this;
         instance = this;
      }
   }

   public void pauseApp() {
      mainGameCanvas.stopGame();
   }

   public void destroyApp(boolean var1) {
      mainGameCanvas.stopGameLoop();

      while(!mainGameCanvas.isGameInitialized) {
         Thread.yield();
      }

      this.notifyDestroyed();
   }

   public static void exitApplication() {
       instance.destroyApp(true);
   }
}
