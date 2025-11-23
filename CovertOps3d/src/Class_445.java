import java.util.Vector;

public final class Class_445 {
   public Class_71 var_c;
   public int var_24;
   public int var_f7;
   private Vector var_146;
   private Vector var_18b;
   public int var_1ce;
   public int var_243;
   public int var_296;
   public int var_2cf;
   public Class_13c var_2f3;
   public int var_319;
   public Class_318 var_363;
   public Class_318 var_399;
   public int var_3e0;
   public int var_404;
   public int var_41b;
   public int var_440;

   public Class_445(Class_71 var1, int var2, int var3, int var4) {
      Class_445 var10000;
      label21: {
         super();
         this.var_c = var1;
         this.var_2f3 = new Class_13c(0, 0);
         this.var_24 = var3;
         this.var_f7 = var4;
         this.var_146 = new Vector();
         this.var_18b = new Vector();
         this.var_1ce = 0;
         this.var_319 = 0;
         this.var_363 = null;
         this.var_399 = null;
         this.var_3e0 = 0;
         this.var_404 = 0;
         this.var_41b = 0;
         this.var_440 = 0;
         this.var_243 = 0;
         this.var_296 = 0;
         this.var_2cf = -1;
         int[] var10001;
         switch(var3) {
         case 10:
            var10000 = this;
            break label21;
         case 12:
            var10000 = this;
            break label21;
         case 3001:
            var10000 = this;
            var10001 = Class_3aa.var_146a;
            break;
         case 3002:
            var10000 = this;
            var10001 = Class_3aa.var_1492;
            break;
         case 3003:
            var10000 = this;
            var10001 = Class_3aa.var_139c;
            break;
         case 3004:
            var10000 = this;
            var10001 = Class_3aa.var_13e4;
            break;
         case 3005:
            var10000 = this;
            var10001 = Class_3aa.var_13fe;
            break;
         case 3006:
            var10000 = this;
            var10001 = Class_3aa.var_143e;
            break;
         default:
            return;
         }

         var10000.var_243 = var10001[Class_29e.var_c79];
         var10000 = this;
      }

      var10000.var_2cf = 0;
   }

   public final int sub_27() {
      switch(this.var_24) {
      case 3001:
         return Class_3aa.var_1a4a[Class_29e.var_c79];
      case 3002:
         return Class_3aa.var_1a74[Class_29e.var_c79];
      case 3003:
         return Class_3aa.var_1966[Class_29e.var_c79];
      case 3004:
         return Class_3aa.var_19bb[Class_29e.var_c79];
      case 3005:
         return Class_3aa.var_19fd[Class_29e.var_c79];
      case 3006:
         return Class_3aa.var_1a1b[Class_29e.var_c79];
      default:
         return 65536;
      }
   }

   public final void sub_57(Class_3e6 var1) {
      var1.sub_e0(this.var_c.var_49, this.var_c.var_ba).sub_119(this);
   }

   public final byte sub_a9() {
      return this.var_1ce > -1 && this.var_1ce < this.var_146.size() ? (Byte)this.var_146.elementAt(this.var_1ce) : 0;
   }

   public final byte sub_b7() {
      return this.var_1ce > -1 && this.var_1ce < this.var_18b.size() ? (Byte)this.var_18b.elementAt(this.var_1ce) : 0;
   }

   public final void sub_ea(byte var1, byte var2) {
      this.var_146.addElement(new Byte(var1));
      this.var_18b.addElement(new Byte(var2));
   }

   public final boolean sub_11b(Class_445 var1) {
      return this.var_2f3.var_83 < var1.var_2f3.var_83;
   }

   public final boolean sub_172() {
      if (this.var_2f3.var_83 <= 0) {
         return false;
      } else {
         this.var_2f3.var_2b = (int)((long)Class_48.sub_3c(this.var_2f3.var_2b, this.var_2f3.var_83) * 7864320L >> 16);
         this.var_319 = Class_48.sub_3c(this.var_319, this.var_2f3.var_83) * 120;
         return true;
      }
   }

   public final void sub_194() {
      this.var_3e0 = Class_48.sub_3c(this.var_363.var_11 << 16, this.var_2f3.var_83) * 120 - 131072 >> 18;
      this.var_404 = Class_48.sub_3c(this.var_363.var_6f << 16, this.var_2f3.var_83) * 120 - 131072 >> 18;
   }

   public final void sub_1ee() {
      this.var_41b = Class_48.sub_3c(this.var_399.var_11 << 16, this.var_2f3.var_83) * 120 + 65536 >> 17;
      this.var_440 = Class_48.sub_3c(this.var_399.var_6f << 16, this.var_2f3.var_83) * 120 + 65536 >> 17;
   }
}
