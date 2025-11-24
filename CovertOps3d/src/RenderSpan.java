public final class RenderSpan {
   public short startX;
   public short endX;
   public short sectorId;
   public RenderSpan next;

   public RenderSpan(short var1, short var2, short var3) {
      this.startX = var1;
      this.endX = var2;
      this.sectorId = var3;
      this.next = null;
   }
}
