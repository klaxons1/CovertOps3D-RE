import javax.microedition.media.Player;

public final class SoundPlayer {
   public Player player;
   public int soundId = 0;

   public SoundPlayer(AudioManager var1, Player var2, int var3) {
      this.player = var2;
      this.soundId = var3;
   }

   public static int sub_3e(SoundPlayer var0, int var1) {
      return var0.soundId = var1;
   }
}
