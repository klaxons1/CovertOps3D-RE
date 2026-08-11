import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

public class SaveSystem {

    // ==================== Settings Fields ====================
    public static byte soundEnabled = 1;
    public static byte musicEnabled = 1;
    public static byte vibrationEnabled = 1;

    // Renderer and visual settings. Defaults preserve the full-quality
    // original presentation; faster modes are explicit player choices.
    public static byte texturedFlatsEnabled = 1;
    public static byte texturedSkyEnabled = 1;
    public static byte muzzleLightingEnabled = 1;
    public static byte screenEffectsEnabled = 1;

    public static byte gameProgressFlags = 0;

    // ==================== Save Data Fields ====================
    /**
     * Array holding save data for each level/chapter.
     * Index corresponds to chapter ID.
     */
    public static byte[][] saveData;

    // ==================== Constants ====================
    private static final String STORE_SETTINGS = "settings";
    private static final String STORE_DATA_PREFIX = "data";

    private static final int SAVE_SLOTS_COUNT = 9;
    private static final int SAVE_RECORD_SIZE = 30;
    private static final int SETTINGS_RECORD_SIZE = 16;

    // Settings record layout. The version marker prevents an old record's
    // unused zero bytes from accidentally disabling new visual defaults.
    private static final int SETTINGS_OFF_SOUND = 0;
    private static final int SETTINGS_OFF_MUSIC = 1;
    private static final int SETTINGS_OFF_VIBRATION = 2;
    private static final int SETTINGS_OFF_PROGRESS = 3;
    private static final int SETTINGS_OFF_TEXTURED_FLATS = 4;
    private static final int SETTINGS_OFF_TEXTURED_SKY = 5;
    private static final int SETTINGS_OFF_MUZZLE_LIGHT = 6;
    private static final int SETTINGS_OFF_SCREEN_EFFECTS = 7;
    private static final int SETTINGS_OFF_VERSION = SETTINGS_RECORD_SIZE - 1;
    private static final byte SETTINGS_VERSION = 2;

    // Byte Offsets in Save Record
    private static final int OFF_HEALTH = 0;
    private static final int OFF_ARMOR = 1;
    private static final int OFF_WEAPONS = 2;       // 9 bytes (1 per weapon)
    private static final int OFF_AMMO_LOW = 11;     // 9 bytes (Low byte of ammo count)
    private static final int OFF_AMMO_HIGH = 20;    // 9 bytes (High byte of ammo count)
    private static final int OFF_CURRENT_WEAPON = 29;

    /**
     * Loads save data for the current difficulty level from RMS.
     */
    public static void loadSaveData() {
        try {
            String storeName = getRecordStoreName();
            RecordStore store = RecordStore.openRecordStore(storeName, true);

            saveData = new byte[SAVE_SLOTS_COUNT][];
            int recordsFound = store.getNumRecords();

            if (recordsFound > SAVE_SLOTS_COUNT) {
                recordsFound = SAVE_SLOTS_COUNT;
            }

            for (int i = 0; i < recordsFound; ++i) {
                // Record IDs start at 1
                saveData[i] = store.getRecord(i + 1);
            }

            store.closeRecordStore();
        } catch (RecordStoreException e) {
            // Log or handle error silently
        } catch (OutOfMemoryError e) {
            // Handle OOM
        }
    }

    /**
     * Restores game state (Health, Ammo, Weapons) from the loaded save data buffer.
     * @param slotIndex The chapter/level index to load from.
     */
    public static void loadGameState(int slotIndex) {
        if (saveData[slotIndex] == null) return;

        byte[] data = saveData[slotIndex];

        GameEngine.playerHealth = data[OFF_HEALTH];
        GameEngine.playerArmor = data[OFF_ARMOR];

        for (int i = 0; i < 9; ++i) {
            // Restore available weapons
            GameEngine.weaponsAvailable[i] = data[OFF_WEAPONS + i] == 1;

            // Restore ammo (combine Low and High bytes)
            int lowByte = data[OFF_AMMO_LOW + i] & 0xFF;
            int highByte = data[OFF_AMMO_HIGH + i] & 0xFF;
            GameEngine.ammoCounts[i] = lowByte + (highByte << 8);
        }

        GameEngine.currentWeapon = data[OFF_CURRENT_WEAPON];
        GameEngine.pendingWeaponSwitch = GameEngine.currentWeapon;
    }

    /**
     * Captures current game state and saves it to memory buffer and RMS.
     * @param slotIndex The chapter/level index to save to.
     */
    public static void saveGameState(int slotIndex) {
        saveData[slotIndex] = new byte[SAVE_RECORD_SIZE];
        byte[] data = saveData[slotIndex];

        data[OFF_HEALTH] = (byte) GameEngine.playerHealth;
        data[OFF_ARMOR] = (byte) GameEngine.playerArmor;

        for (int i = 0; i < 9; ++i) {
            data[OFF_WEAPONS + i] = (byte) (GameEngine.weaponsAvailable[i] ? 1 : 0);

            // Split ammo count (int) into two bytes
            int ammo = GameEngine.ammoCounts[i];
            data[OFF_AMMO_LOW + i] = (byte) (ammo & 0xFF);
            data[OFF_AMMO_HIGH + i] = (byte) ((ammo >> 8) & 0xFF);
        }

        data[OFF_CURRENT_WEAPON] = (byte) GameEngine.currentWeapon;

        writeSaveDataToRMS();
    }

    /**
     * Writes the in-memory save buffers to persistent storage (RMS).
     */
    public static void writeSaveDataToRMS() {
        try {
            String storeName = getRecordStoreName();
            RecordStore store = RecordStore.openRecordStore(storeName, true);
            int existingRecords = store.getNumRecords();

            for (int i = 0; i < SAVE_SLOTS_COUNT; ++i) {
                byte[] data = saveData[i];
                int length = (data == null) ? 0 : data.length;

                // Update existing record or add new one
                if (existingRecords > i) {
                    store.setRecord(i + 1, data, 0, length);
                } else if (length > 0) {
                    store.addRecord(data, 0, length);
                }
            }

            store.closeRecordStore();
        } catch (RecordStoreException e) {
            e.printStackTrace();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads global settings (Sound, Music, Vibration) from RMS.
     */
    public static void loadSettingsFromRMS() {
        // Older records do not contain the version marker, so start from the
        // full-quality defaults before selectively reading a newer layout.
        texturedFlatsEnabled = 1;
        texturedSkyEnabled = 1;
        muzzleLightingEnabled = 1;
        screenEffectsEnabled = 1;

        try {
            RecordStore store = RecordStore.openRecordStore(STORE_SETTINGS, true);

            if (store.getNumRecords() > 0) {
                byte[] data = store.getRecord(1);
                if (data.length > SETTINGS_OFF_PROGRESS) {
                    soundEnabled = normalizeToggle(data[SETTINGS_OFF_SOUND]);
                    musicEnabled = normalizeToggle(data[SETTINGS_OFF_MUSIC]);
                    vibrationEnabled = normalizeToggle(data[SETTINGS_OFF_VIBRATION]);
                    gameProgressFlags = data[SETTINGS_OFF_PROGRESS];
                }

                if (data.length > SETTINGS_OFF_VERSION
                        && data[SETTINGS_OFF_VERSION] >= SETTINGS_VERSION) {
                    texturedFlatsEnabled = normalizeToggle(data[SETTINGS_OFF_TEXTURED_FLATS]);
                    texturedSkyEnabled = normalizeToggle(data[SETTINGS_OFF_TEXTURED_SKY]);
                    muzzleLightingEnabled = normalizeToggle(data[SETTINGS_OFF_MUZZLE_LIGHT]);
                    screenEffectsEnabled = normalizeToggle(data[SETTINGS_OFF_SCREEN_EFFECTS]);
                }
            }

            store.closeRecordStore();
        } catch (RecordStoreException e) {
            // Settings load failed, using defaults
        } catch (OutOfMemoryError e) {
        }
    }

    /**
     * Saves global settings to RMS.
     */
    public static void saveSettingsToRMS() {
        try {
            RecordStore store = RecordStore.openRecordStore(STORE_SETTINGS, true);
            int records = store.getNumRecords();

            byte[] data = new byte[SETTINGS_RECORD_SIZE];
            data[SETTINGS_OFF_SOUND] = soundEnabled;
            data[SETTINGS_OFF_MUSIC] = musicEnabled;
            data[SETTINGS_OFF_VIBRATION] = vibrationEnabled;
            data[SETTINGS_OFF_PROGRESS] = gameProgressFlags;
            data[SETTINGS_OFF_TEXTURED_FLATS] = texturedFlatsEnabled;
            data[SETTINGS_OFF_TEXTURED_SKY] = texturedSkyEnabled;
            data[SETTINGS_OFF_MUZZLE_LIGHT] = muzzleLightingEnabled;
            data[SETTINGS_OFF_SCREEN_EFFECTS] = screenEffectsEnabled;
            data[SETTINGS_OFF_VERSION] = SETTINGS_VERSION;

            if (records > 0) {
                store.setRecord(1, data, 0, SETTINGS_RECORD_SIZE);
            } else {
                store.addRecord(data, 0, SETTINGS_RECORD_SIZE);
            }

            store.closeRecordStore();
        } catch (RecordStoreException e) {
        } catch (OutOfMemoryError e) {
        }
    }

    /** Treat every non-zero persisted value as enabled. */
    private static byte normalizeToggle(byte value) {
        return value == 0 ? (byte)0 : (byte)1;
    }

    /**
     * Helper to construct RecordStore name based on difficulty.
     * "data" -> Normal
     * "datae" -> Easy
     * "datah" -> Hard
     */
    private static String getRecordStoreName() {
        String suffix = "";
        if (GameEngine.difficultyLevel == 0) { // Easy
            suffix = "e";
        } else if (GameEngine.difficultyLevel == 2) { // Hard
            suffix = "h";
        }
        // Normal difficulty uses just "data"
        return STORE_DATA_PREFIX + suffix;
    }
}
