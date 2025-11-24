public final class PhysicsBody extends Transform3D {
   public int velocityX;
   private int velocityZ;
   public int velocityY;
   private int angularVelocity;
   private static int VELOCITY_THRESHOLD = 65;

   public PhysicsBody(int var1, int var2, int var3, int var4) {
      this(var1, var2, var3, var4, 0, 0, 0, 0);
   }

   private PhysicsBody(int var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8) {
      super(var1, var2, var3, var4);
      this.velocityX = var5;
      this.velocityZ = var6;
      this.velocityY = var7;
      this.angularVelocity = var8;
   }

   public final void applyVelocity() {
      this.applyMovement(this.velocityX, this.velocityZ, this.velocityY, this.angularVelocity);
   }

   public final void applyDampedVelocity() {
      this.applyMovement(this.velocityX >> 1, this.velocityZ >> 1, this.velocityY >> 1, this.angularVelocity >> 1);
   }

   public final void scaleVelocity(int var1, int var2, int var3, int var4) {
      this.velocityX = MathUtils.fixedPointMultiply(this.velocityX, var1);
      this.velocityZ = MathUtils.fixedPointMultiply(this.velocityZ, var2);
      this.velocityY = MathUtils.fixedPointMultiply(this.velocityY, var3);
      this.angularVelocity = MathUtils.fixedPointMultiply(this.angularVelocity, var4);
      if (this.velocityX < VELOCITY_THRESHOLD && this.velocityX > -VELOCITY_THRESHOLD) {
         this.velocityX = 0;
      }

      if (this.velocityX < VELOCITY_THRESHOLD && this.velocityX > -VELOCITY_THRESHOLD) {
         this.velocityX = 0;
      }

      if (this.velocityY < VELOCITY_THRESHOLD && this.velocityY > -VELOCITY_THRESHOLD) {
         this.velocityY = 0;
      }

      if (this.angularVelocity < VELOCITY_THRESHOLD && this.angularVelocity > -VELOCITY_THRESHOLD) {
         this.angularVelocity = 0;
      }

   }

   public final void applyHorizontalForce(int var1, int var2) {
      this.applyForce(var1, var2, 0);
   }

   public final void applyForce(int var1, int var2, int var3) {
      int var4 = MathUtils.fastSin(super.rotation);
      int var5 = MathUtils.fastCos(super.rotation);
      this.velocityX += MathUtils.fixedPointMultiply(var5, var1) - MathUtils.fixedPointMultiply(var4, var2);
      this.velocityY += MathUtils.fixedPointMultiply(-var4, var1) - MathUtils.fixedPointMultiply(var5, var2);
      this.angularVelocity += var3;
   }
}
