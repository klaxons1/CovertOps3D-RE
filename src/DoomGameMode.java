/**
 * Doom-specific player profile layered on the existing fixed-point engine.
 *
 * The renderer/BSP/collision core stays shared; this class is the migration
 * boundary that prevents CovertOps campaign inventory from leaking into Doom
 * C3B levels. It deliberately contains only primitive arrays for CLDC 1.1.
 */
public final class DoomGameMode {

    // Must match converter slots: enemy sprites 1..3, then projectiles 4..6.
    public static final byte ROCKET_SPRITE = -4;
    public static final byte PLASMA_SPRITE = -5;
    public static final byte BFG_SPRITE = -6;
    public static final byte IMP_FIREBALL_SPRITE = -7;

    /** Custom entity type = DOOM_ITEM_BASE + original Doom thing type. */
    public static final int DOOM_ITEM_BASE = 9000;
    public static final int DOOM_BARREL_TYPE = DOOM_ITEM_BASE + 2035;

    private static boolean active;

    private DoomGameMode() {
    }

    public static boolean isDoomLevelPath(String levelPath) {
        return levelPath != null && levelPath.indexOf("/custom/doom-") >= 0;
    }

    public static void setActive(boolean enabled) {
        active = enabled;
    }

    public static boolean isActive() {
        return active;
    }

    /** Gives every Doom weapon immediately, as requested for the conversion sandbox. */
    public static void configurePlayerLoadout(WeaponManager weaponManager) {
        GameEngine.playerHealth = 100;
        GameEngine.playerArmor = 0;
        GameEngine.keysCollected[0] = false;
        GameEngine.keysCollected[1] = false;

        for (int i = 0; i < GameEngine.weaponsAvailable.length; ++i) {
            GameEngine.weaponsAvailable[i] = true;
            GameEngine.ammoCounts[i] = 0;
        }

        // IDs match WeaponFactory's Doom mapping. Keep generous sandbox ammo
        // while pickups and authentic Doom ammo limits are migrated next.
        GameEngine.ammoCounts[WeaponFactory.PISTOL] = 200;
        GameEngine.ammoCounts[WeaponFactory.SHOTGUN] = 50;
        GameEngine.ammoCounts[WeaponFactory.ROCKET_LAUNCHER] = 50;
        GameEngine.ammoCounts[WeaponFactory.PLASMA_RIFLE] = 300;
        GameEngine.ammoCounts[WeaponFactory.BFG9000] = 300;
        GameEngine.currentWeapon = WeaponFactory.PISTOL;
        GameEngine.pendingWeaponSwitch = WeaponFactory.PISTOL;

        if (weaponManager != null) {
            weaponManager.activateDoomLoadout();
        }
    }

    /** Applies the common Doom pickup things and returns true when consumed. */
    public static boolean collectDoomItem(int objectType) {
        if (!active || objectType < DOOM_ITEM_BASE) return false;
        int thingType = objectType - DOOM_ITEM_BASE;
        switch (thingType) {
            case 2007: // clip
                GameEngine.ammoCounts[WeaponFactory.PISTOL] += 10;
                return true;
            case 2048: // box of bullets
                GameEngine.ammoCounts[WeaponFactory.PISTOL] += 50;
                return true;
            case 2008: // shells
                GameEngine.ammoCounts[WeaponFactory.SHOTGUN] += 4;
                return true;
            case 2049: // box of shells
                GameEngine.ammoCounts[WeaponFactory.SHOTGUN] += 20;
                return true;
            case 2010: // rocket
                GameEngine.ammoCounts[WeaponFactory.ROCKET_LAUNCHER] += 1;
                return true;
            case 2046: // box of rockets
                GameEngine.ammoCounts[WeaponFactory.ROCKET_LAUNCHER] += 5;
                return true;
            case 2047: // energy cell
                GameEngine.ammoCounts[WeaponFactory.PLASMA_RIFLE] += 20;
                return true;
            case 17: // cell pack
                GameEngine.ammoCounts[WeaponFactory.PLASMA_RIFLE] += 100;
                return true;
            case 2011: // stimpack
                if (GameEngine.playerHealth >= 100) return false;
                GameEngine.playerHealth += 10;
                if (GameEngine.playerHealth > 100) GameEngine.playerHealth = 100;
                return true;
            case 2012: // medikit
                if (GameEngine.playerHealth >= 100) return false;
                GameEngine.playerHealth += 25;
                if (GameEngine.playerHealth > 100) GameEngine.playerHealth = 100;
                return true;
            case 2013: // soulsphere
                GameEngine.playerHealth += 100;
                if (GameEngine.playerHealth > 200) GameEngine.playerHealth = 200;
                return true;
            case 2014: // health bonus
                if (GameEngine.playerHealth < 200) ++GameEngine.playerHealth;
                return true;
            case 2015: // armor bonus
                if (GameEngine.playerArmor < 200) ++GameEngine.playerArmor;
                return true;
            case 2018: // green armor
                if (GameEngine.playerArmor < 100) GameEngine.playerArmor = 100;
                return true;
            case 2019: // blue armor
                if (GameEngine.playerArmor < 200) GameEngine.playerArmor = 200;
                return true;
            case 2001: // shotgun
                GameEngine.weaponsAvailable[WeaponFactory.SHOTGUN] = true;
                GameEngine.ammoCounts[WeaponFactory.SHOTGUN] += 8;
                return true;
            case 2002: // chaingun
                GameEngine.weaponsAvailable[WeaponFactory.CHAINGUN] = true;
                GameEngine.ammoCounts[WeaponFactory.PISTOL] += 20;
                return true;
            case 2003: // rocket launcher
                GameEngine.weaponsAvailable[WeaponFactory.ROCKET_LAUNCHER] = true;
                GameEngine.ammoCounts[WeaponFactory.ROCKET_LAUNCHER] += 2;
                return true;
            case 2004: // plasma rifle
                GameEngine.weaponsAvailable[WeaponFactory.PLASMA_RIFLE] = true;
                GameEngine.ammoCounts[WeaponFactory.PLASMA_RIFLE] += 40;
                return true;
            case 2005: // chainsaw
                GameEngine.weaponsAvailable[WeaponFactory.CHAINSAW] = true;
                return true;
            case 2006: // BFG9000
                GameEngine.weaponsAvailable[WeaponFactory.BFG9000] = true;
                GameEngine.ammoCounts[WeaponFactory.BFG9000] += 40;
                return true;
            default:
                return false;
        }
    }

    public static boolean isSolidDoomProp(int objectType) {
        return objectType == DOOM_BARREL_TYPE;
    }
}
