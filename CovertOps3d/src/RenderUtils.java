public final class RenderUtils {
   private RenderSpan[] renderSpans = new RenderSpan[288];
   private RenderSpan freeList = null;

   public RenderUtils() {
      this.resetRenderer();
   }

   public final void resetRenderer() {
      for(int var1 = 0; var1 < 288; ++var1) {
         if (this.renderSpans[var1] != null) {
            RenderSpan var10000 = this.renderSpans[var1];

            while(true) {
               RenderSpan var2 = var10000;
               if (var10000.next == null) {
                  var2.next = this.freeList;
                  this.freeList = this.renderSpans[var1];
                  break;
               }

               var10000 = var2.next;
            }
         }

         this.renderSpans[var1] = null;
      }

   }

   public final void addRenderSpan(short var1, short var2, short var3, int var4) {
      RenderSpan var10000 = this.renderSpans[var4];

      while(true) {
         RenderSpan var5 = var10000;
         RenderSpan var6;
         if (var10000 == null) {
            if (this.freeList != null) {
               var6 = this.freeList;
               this.freeList = this.freeList.next;
               var6.startX = var1;
               var6.endX = var2;
               var6.sectorId = var3;
            } else {
               var6 = new RenderSpan(var1, var2, var3);
            }

            var6.next = this.renderSpans[var4];
            this.renderSpans[var4] = var6;
            return;
         }

         if (var5.sectorId == var3) {
            RenderSpan var7;
            if (var5.endX == var1 - 1) {
               var5.endX = var2;
               var6 = this.renderSpans[var4];

               for(var7 = null; var6 != null; var6 = var6.next) {
                  if (var6.sectorId == var3 && var6.startX == var2 + 1) {
                     var5.endX = var6.endX;
                     if (var7 != null) {
                        var7.next = var6.next;
                     } else {
                        this.renderSpans[var4] = var6.next;
                     }

                     var6.next = this.freeList;
                     this.freeList = var6;
                     return;
                  }

                  var7 = var6;
               }

               return;
            }

            if (var5.startX == var2 + 1) {
               var5.startX = var1;
               var6 = this.renderSpans[var4];

               for(var7 = null; var6 != null; var6 = var6.next) {
                  if (var6.sectorId == var3 && var6.endX == var1 - 1) {
                     var5.startX = var6.startX;
                     if (var7 != null) {
                        var7.next = var6.next;
                     } else {
                        this.renderSpans[var4] = var6.next;
                     }

                     var6.next = this.freeList;
                     this.freeList = var6;
                     return;
                  }

                  var7 = var6;
               }

               return;
            }
         }

         var10000 = var5.next;
      }
   }

   public final void renderAllSpans(int var1, int var2, int var3, int var4) {
      RenderSpan var10000;
      int var5;
      RenderSpan var6;
      SectorData var7;
      for(var5 = 0; var5 < 144; ++var5) {
         var10000 = this.renderSpans[var5];

         while(true) {
            var6 = var10000;
            if (var10000 == null) {
               break;
            }

            var7 = GameEngine.var_505.sectors[var6.sectorId];
            GameEngine.sub_430(var6.startX, var6.endX, var5, var7.floorTexture.pixelData, var7.floorTexture.colorPalettes, var7.lightLevel, var1, var2, var3, var7.floorOffsetX, var4);
            var10000 = var6.next;
         }
      }

      for(var5 = 144; var5 < 288; ++var5) {
         var10000 = this.renderSpans[var5];

         while(true) {
            var6 = var10000;
            if (var10000 == null) {
               break;
            }

            var7 = GameEngine.var_505.sectors[var6.sectorId];
            GameEngine.sub_430(var6.startX, var6.endX, var5, var7.ceilingTexture.pixelData, var7.ceilingTexture.colorPalettes, var7.lightLevel, var1, var2, var3, var7.ceilingOffsetX, var4);
            var10000 = var6.next;
         }
      }

   }
}
