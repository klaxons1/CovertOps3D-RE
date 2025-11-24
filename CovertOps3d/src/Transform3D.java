public class Transform3D {
   public int x;
   public int y;
   public int z;
   public int rotation;

   public Transform3D(int var1, int var2, int var3, int var4) {
      this.x = var1;
      this.y = var2;
      this.z = var3;
      this.rotation = var4;
   }

   public final void applyMovement(int var1, int var2, int var3, int var4) {
      this.x += var1;
      this.y += var2;
      this.z += var3;
      Transform3D var10000 = this;
      int var10001 = this.rotation;
      int var10002 = var4;

      while(true) {
         var10000.rotation = var10001 + var10002;
         if (this.rotation >= 0) {
            while(this.rotation >= 411775) {
               this.rotation -= 411775;
            }

            return;
         }

         var10000 = this;
         var10001 = this.rotation;
         var10002 = 411775;
      }
   }

   public final void copyFrom(Transform3D var1) {
      this.setPosition(var1.x, var1.y, var1.z, var1.rotation);
   }

   public final void setPosition(int var1, int var2, int var3, int var4) {
      this.x = var1;
      this.y = var2;
      this.z = var3;
      this.rotation = var4;
   }

   public final void moveRelative(int var1, int var2) {
      int var3 = MathUtils.fastSin(this.rotation);
      int var4 = MathUtils.fastCos(this.rotation);
      this.x += MathUtils.fixedPointMultiply(var4, var1) - MathUtils.fixedPointMultiply(var3, var2);
      this.z += MathUtils.fixedPointMultiply(-var3, var1) - MathUtils.fixedPointMultiply(var4, var2);
   }
}
