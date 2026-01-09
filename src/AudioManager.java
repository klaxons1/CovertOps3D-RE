import java.io.InputStream;
import java.util.Vector;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.control.VolumeControl;

public final class AudioManager {
    private int currentSoundIndex = -1;
    private Vector soundPlayers = new Vector();
    private VolumeControl volumeControl;

    /**
     * Inner class to hold Player instance and its priority.
     * Merged SoundPlayer into AudioManager to save space and reduce class count.
     */
    private static class SoundItem {
        public Player player;
        public int priority; // Formerly 'soundId'

        public SoundItem(Player player, int priority) {
            this.player = player;
            this.priority = priority;
        }
    }

    /**
     * Loads an audio file from resources.
     * Supported formats: .mid, .amr, .mp3, .wav
     *
     * @param resourcePath Path to the audio file (e.g., "/music.mp3").
     */
    public final void loadSound(String resourcePath) {
        Player player = null;

        try {
            InputStream inputStream = getClass().getResourceAsStream(resourcePath);
            int dotIndex = resourcePath.lastIndexOf('.');

            if (dotIndex == -1) {
                System.err.println("Error: No extension found in file name: " + resourcePath);
                return;
            }

            // Detect format
            String extension = resourcePath.substring(dotIndex).toLowerCase();
            String contentType = null;

            if (extension.equals(".mid")) {
                contentType = "audio/midi";
            } else if (extension.equals(".amr")) {
                contentType = "audio/amr";
            } else if (extension.equals(".mp3")) {
                contentType = "audio/mpeg";
            } else if (extension.equals(".wav")) {
                contentType = "audio/x-wav";
            } else {
                System.err.println("Unsupported format: " + extension);
                return;
            }

            // Create and prepare player
            player = Manager.createPlayer(inputStream, contentType);
            player.realize();
            player.prefetch();
            player.setLoopCount(1);

            // Grab volume control (usually global for the device mixer)
            if (player.getControl("VolumeControl") != null) {
                this.volumeControl = (VolumeControl) player.getControl("VolumeControl");
            }

        } catch (Exception e) {
            System.err.println("Error loading sound: " + e.getMessage());
            e.printStackTrace();
            if (player != null) {
                player.close();
                player = null;
            }
        }

        // Add new sound wrapper to the vector
        this.soundPlayers.addElement(new SoundItem(player, 0));
    }

    /**
     * Plays a specific sound.
     *
     * @param index     The index of the sound in the loaded vector.
     * @param loopCount Number of loops (-1 for infinite).
     * @param priority  Priority of this playback (higher value = more important).
     */
    public final void playSound(int index, int loopCount, int priority) {
        try {
            SoundItem currentItem = null;
            Player currentPlayer = null;

            // 1. Get the currently playing sound (if any)
            if (this.currentSoundIndex > -1 && this.currentSoundIndex < this.soundPlayers.size()) {
                currentItem = (SoundItem) this.soundPlayers.elementAt(this.currentSoundIndex);
                currentPlayer = currentItem.player;
            }

            // Check index bounds
            if (index < 0 || index >= this.soundPlayers.size()) return;

            // 2. Get the new sound item
            SoundItem newItem = (SoundItem) this.soundPlayers.elementAt(index);

            // Update the priority for this specific playback call
            newItem.priority = priority;

            // 3. Handle interruption logic
            if (currentPlayer != null && currentPlayer.getState() == Player.STARTED) {
                // If current sound is more important than the new one, do nothing (exit)
                if (currentItem.priority > newItem.priority) {
                    return;
                }

                // Stop current sound
                currentPlayer.stop();
                currentPlayer.setMediaTime(0L);
            }

            // 4. Start the new sound
            Player newPlayer = newItem.player;
            if (newPlayer != null) {
                newPlayer.setLoopCount(loopCount);
                newPlayer.start();
                this.currentSoundIndex = index;
            }

            // Debug output
            // System.out.println("Playing sound index: " + index);

        } catch (Exception e) {
            System.err.println("Error playing sound: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Stops the currently active sound.
     */
    public final void stopCurrentSound() {
        try {
            if (this.currentSoundIndex > -1 && this.currentSoundIndex < this.soundPlayers.size()) {
                SoundItem item = (SoundItem) this.soundPlayers.elementAt(this.currentSoundIndex);
                Player player = item.player;
                if (player != null) {
                    player.stop();
                    player.setMediaTime(0L);
                }
            }
        } catch (Exception e) {
            System.err.println("Error stopping sound: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sets the volume for the audio manager.
     * @param level Volume level (0-100)
     */
    public final void setVolume(int level) {
        if (this.volumeControl != null) {
            this.volumeControl.setLevel(level);
        }
    }
}