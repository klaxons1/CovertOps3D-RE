public final class Class_21c {
   private short var_20;
   public short var_80;
   public short var_10b;
   public boolean var_125;
   public Class_1e1 var_131;
   private Class_8e var_185;
   public short var_1d1;

   public Class_21c(short var1, short var2, short var3, boolean var4, short var5) {
      this.var_80 = var1;
      this.var_10b = var2;
      this.var_20 = var3;
      this.var_125 = var4;
      this.var_1d1 = var5;
   }

   public final void sub_35(Class_3e6 var1) {
      this.var_131 = var1.var_210[this.var_20 & '\uffff'];
      Class_21c var10000;
      Class_8e var10001;
      if (this.var_125) {
         var10000 = this;
         var10001 = this.var_131.var_e5;
      } else {
         var10000 = this;
         var10001 = this.var_131.var_133;
      }

      var10000.var_185 = var10001;
   }

   public final Class_30a sub_d2() {
      return this.var_185.var_13d;
   }
}
