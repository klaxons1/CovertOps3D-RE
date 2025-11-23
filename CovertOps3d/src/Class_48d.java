import java.io.InputStream;
import java.util.Vector;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.control.VolumeControl;

public final class Class_48d {
   private int var_4b = 0;
   private Vector var_8b = new Vector();
   private VolumeControl var_9f;

   public final void sub_3e(String var1) {
      Player var2 = null;

      try {
         InputStream var3 = this.getClass().getResourceAsStream(var1);
         int var4;
         if ((var4 = var1.indexOf(".")) == -1) {
            return;
         }

         String var5;
         if ((var5 = new String(var1.getBytes(), var4, var1.length() - var4)).compareTo(".mid") == 0) {
            var2 = Manager.createPlayer(var3, "audio/midi");
         } else {
            if (var5.compareTo(".amr") != 0) {
               return;
            }

            var2 = Manager.createPlayer(var3, "audio/amr");
         }

         var2.realize();
         var2.prefetch();
         var2.setLoopCount(1);
         this.var_9f = (VolumeControl)var2.getControl("VolumeControl");
      } catch (Exception var6) {
         System.err.println(var6.getMessage());
         var6.printStackTrace();
         if (var2 != null) {
            var2.close();
         }
      }

      this.var_8b.addElement(new Class_12e(this, var2, 0));
   }

   public final void sub_72(int var1, int var2, int var3) {
      try {
         Class_12e var4;
         Player var5;
         if (this.var_4b > -1 && (var5 = (var4 = (Class_12e)this.var_8b.elementAt(this.var_4b)).var_36) != null) {
            Class_12e var6;
            Class_12e.sub_3e(var6 = (Class_12e)this.var_8b.elementAt(var1), var3);
            if (var5.getState() == 400) {
               if (var4.var_6c > var6.var_6c) {
                  return;
               }

               var5.stop();
               var5.setMediaTime(0L);
            }

            Player var7;
            if ((var7 = var6.var_36) != null) {
               var7.setLoopCount(var2);
               var7.start();
               this.var_4b = var1;
            }
         }

         System.err.print("Exec player:");
         System.err.println(this.var_8b);
      } catch (Exception var8) {
         System.err.println(var8.getMessage());
         var8.printStackTrace();
      }
   }

   public final void sub_b8() {
      try {
         Player var2;
         if (this.var_4b > -1 && (var2 = ((Class_12e)this.var_8b.elementAt(this.var_4b)).var_36) != null) {
            var2.stop();
            var2.setMediaTime(0L);
         }

      } catch (Exception var3) {
         System.err.println(var3.getMessage());
         var3.printStackTrace();
      }
   }

   public final void sub_e2(int var1) {
      if (this.var_9f != null) {
         this.var_9f.setLevel(var1);
      }

   }
}
