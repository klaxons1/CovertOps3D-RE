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



    public static void delay(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
        }
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
