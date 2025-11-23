import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

public class CovertOps3D extends MIDlet {
   public static Display var_ee = null;
   public static Class_3aa var_108 = null;
   private static CovertOps3D var_149 = null;

   public void startApp() {
      if (var_108 != null) {
         var_108.sub_32d();
      } else {
         var_ee = Display.getDisplay(this);
         var_108 = new Class_3aa();
         var_108.sub_71();
         var_ee.setCurrent(var_108);
         Class_3aa.var_1e5 = this;
         var_149 = this;
      }
   }

   public void pauseApp() {
      var_108.sub_308();
   }

   public void destroyApp(boolean var1) {
      var_108.sub_35e();

      while(!var_108.var_16f) {
         Thread.yield();
      }

      this.notifyDestroyed();
   }

   public static void sub_24() {
      try {
         var_149.destroyApp(true);
      } catch (MIDletStateChangeException var0) {
      }
   }
}
