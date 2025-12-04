public class HelperUtils {
    public static AudioManager audioManager;

    static void fastArrayFill(Object buffer, int offset, int length) {
        int step = 1;

        while(step < length) {
            System.arraycopy(buffer, offset, buffer, offset + step,
                    (length - step > step) ? step : (length - step));
            step += step;
        }
    }

    static void preloadObjectTextures(GameObject object, byte[] sprites1, byte[] sprites2) {
        for(int i = 0; i < sprites1.length; ++i) {
            byte sprite1 = sprites1[i];
            byte sprite2 = sprites2[i];
            object.addSpriteFrame(sprite1, sprite2);
            if (sprite1 != 0) {
                LevelLoader.preloadTexture(sprite1);
            }
            if (sprite2 != 0) {
                LevelLoader.preloadTexture(sprite2);
            }
        }
    }

    public static void delay(int milliseconds) throws InterruptedException {
        Thread.sleep(milliseconds);
    }

    public static void freeMemory() {
        System.gc();
    }

    public static void playSound(int soundId, boolean loop, int volume, int priority) {
        boolean isMusic = soundId > 0;
        if (!isMusic || SaveSystem.soundEnabled != 0) {
            int loopCount = loop ? -1 : 1;
            audioManager.setVolume(volume);
            audioManager.playSound(soundId, loopCount, priority);
        }
    }

    public static void stopCurrentSound() {
        audioManager.stopCurrentSound();
    }

    public static void vibrateDevice(int duration) {
        if (SaveSystem.vibrationEnabled != 0) {
            try {
                CovertOps3D.display.vibrate(duration);
            } catch (Exception e) {
            } catch (OutOfMemoryError e) {
            }
        }
    }

    static void yieldToOtherThreads() {
        Thread.yield();
    }
}
