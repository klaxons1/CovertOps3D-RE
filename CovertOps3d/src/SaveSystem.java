import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

public class SaveSystem {
    public static byte soundEnabled = 1;
    public static byte musicEnabled = 1;
    public static byte vibrationEnabled = 1;
    public static byte gameProgressFlags = 0;
    public static byte[][] saveData;

    public static void loadSaveData() {
       try {
          String var0;
          label33: {
             var0 = "data";
             StringBuffer var10000;
             String var10001;
             if (GameEngine.difficultyLevel == 0) {
                var10000 = (new StringBuffer()).append(var0);
                var10001 = "e";
             } else {
                if (GameEngine.difficultyLevel != 2) {
                   break label33;
                }

                var10000 = (new StringBuffer()).append(var0);
                var10001 = "h";
             }

             var0 = var10000.append(var10001).toString();
          }

          RecordStore var1 = RecordStore.openRecordStore(var0, true);
          saveData = new byte[9][];
          int var2;
          if ((var2 = var1.getNumRecords()) > 9) {
             var2 = 9;
          }

          for(int var3 = 0; var3 < var2; ++var3) {
             saveData[var3] = var1.getRecord(var3 + 1);
          }

          var1.closeRecordStore();
       } catch (RecordStoreException var4) {
       } catch (OutOfMemoryError var5) {
       }
    }

    public static void loadGameState(int var0) {
       GameEngine.playerHealth = saveData[var0][0];
       GameEngine.playerArmor = saveData[var0][1];

       for(int var1 = 0; var1 < 9; ++var1) {
          GameEngine.weaponsAvailable[var1] = saveData[var0][2 + var1] == 1;
          GameEngine.ammoCounts[var1] = (saveData[var0][11 + var1] & 255) + ((saveData[var0][20 + var1] & 255) << 8);
       }

       GameEngine.currentWeapon = saveData[var0][29];
       GameEngine.pendingWeaponSwitch = GameEngine.currentWeapon;
    }

    public static void saveGameState(int var0) {
       saveData[var0] = new byte[30];
       saveData[var0][0] = (byte) GameEngine.playerHealth;
       saveData[var0][1] = (byte) GameEngine.playerArmor;

       for(int var1 = 0; var1 < 9; ++var1) {
          saveData[var0][2 + var1] = (byte)(GameEngine.weaponsAvailable[var1] ? 1 : 0);
          saveData[var0][11 + var1] = (byte)(GameEngine.ammoCounts[var1] & 255);
          saveData[var0][20 + var1] = (byte)(GameEngine.ammoCounts[var1] >> 8 & 255);
       }

       saveData[var0][29] = (byte) GameEngine.currentWeapon;
       writeSaveData();
    }

    public static void writeSaveData() {
       try {
          String var0;
          label39: {
             var0 = "data";
             StringBuffer var10000;
             String var10001;
             if (GameEngine.difficultyLevel == 0) {
                var10000 = (new StringBuffer()).append(var0);
                var10001 = "e";
             } else {
                if (GameEngine.difficultyLevel != 2) {
                   break label39;
                }

                var10000 = (new StringBuffer()).append(var0);
                var10001 = "h";
             }

             var0 = var10000.append(var10001).toString();
          }

          RecordStore var1;
          int var2 = (var1 = RecordStore.openRecordStore(var0, true)).getNumRecords();

          for(int var3 = 0; var3 < 9; ++var3) {
             int var4 = saveData[var3] == null ? 0 : saveData[var3].length;
             if (var2 > var3) {
                var1.setRecord(var3 + 1, saveData[var3], 0, var4);
             } else if (var4 > 0) {
                var1.addRecord(saveData[var3], 0, var4);
             }
          }

          var1.closeRecordStore();
       } catch (RecordStoreException var5) {
       } catch (OutOfMemoryError var6) {
       }
    }

    public static void loadSettingsFromRMS() {
       try {
          RecordStore var0 = RecordStore.openRecordStore("settings", true);
          Object var1 = null;
          if (var0.getNumRecords() > 0) {
             byte[] var5;
             soundEnabled = (var5 = var0.getRecord(1))[0];
             musicEnabled = var5[1];
             vibrationEnabled = var5[2];
             gameProgressFlags = var5[3];
          }

          var0.closeRecordStore();
       } catch (RecordStoreException var3) {
       } catch (OutOfMemoryError var4) {
       }
    }

    public static void saveSettingsToRMS() {
       try {
          RecordStore var0;
          int var1 = (var0 = RecordStore.openRecordStore("settings", true)).getNumRecords();
          byte[] var2;
          (var2 = new byte[16])[0] = soundEnabled;
          var2[1] = musicEnabled;
          var2[2] = vibrationEnabled;
          var2[3] = gameProgressFlags;
          if (var1 > 0) {
             var0.setRecord(1, var2, 0, 16);
          } else {
             var0.addRecord(var2, 0, 16);
          }

          var0.closeRecordStore();
       } catch (RecordStoreException var3) {
       } catch (OutOfMemoryError var4) {
       }
    }
}
