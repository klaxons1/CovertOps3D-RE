import java.util.Vector;

public final class Class_110 {
   private short var_62;
   private short var_b7;
   public Class_21c[] var_dc;
   public static short[] var_130;
   public static short[] var_144;
   public Vector var_193;
   public boolean[] var_1b6;

   public Class_110(short var1, short var2) {
      this.var_62 = var1;
      this.var_b7 = var2;
      this.var_193 = new Vector();
   }

   public static boolean sub_33() {
      for(int var0 = 0; var0 < 240; ++var0) {
         if (var_144[var0] < var_130[var0]) {
            return false;
         }
      }

      return true;
   }

   public static void sub_85() {
      if (var_144 == null) {
         var_144 = new short[240];
         var_130 = new short[240];
      }

      for(int var0 = 0; var0 < 240; ++var0) {
         var_144[var0] = 0;
         var_130[var0] = 287;
      }

   }

   public final Class_30a sub_da() {
      return this.var_dc[0].sub_d2();
   }

   public final void sub_fc() {
      this.var_193.removeAllElements();
   }

   public final void sub_119(Class_445 var1) {
      this.var_193.addElement(var1);
   }

   public final void sub_148(Class_3e6 var1) {
      this.var_dc = new Class_21c[this.var_62 & '\uffff'];

      for(int var2 = 0; var2 < this.var_dc.length; ++var2) {
         this.var_dc[var2] = var1.var_433[(this.var_b7 & '\uffff') + var2];
      }

   }

   public final boolean[] sub_19b() {
      this.var_1b6 = this.var_dc[0].sub_d2().var_1dd;
      return this.var_1b6;
   }
}
