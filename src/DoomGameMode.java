/**
 * Doom-specific player profile layered on the existing fixed-point engine.
 *
 * The renderer/BSP/collision core stays shared; this class is the migration
 * boundary that prevents CovertOps campaign inventory from leaking into Doom
 * C3B levels. It deliberately contains only primitive arrays for CLDC 1.1.
 */
public final class DoomGameMode {

    // Must match converter slots: 3 actor families x 7 state frames = 1..21,
    // plus four in-between death frames each = 22..33; rocket/plasma/BFG/imp
    // projectiles then occupy 34..37. BFG now points at the real BFS1 ball,
    // not the BFUG pickup patch.
    public static final byte ROCKET_SPRITE = -34;
    public static final byte PLASMA_SPRITE = -35;
    public static final byte BFG_SPRITE = -36;
    public static final byte IMP_FIREBALL_SPRITE = -37;

    /** Three 20 Hz simulation ticks per genuine Doom death pose. */
    public static final int ACTOR_DEATH_FRAME_TICKS = 3;
    /** Five 20 Hz ticks matches Doom's classic eight-tic material cadence. */
    public static final int TEXTURE_ANIMATION_TICKS = 5;
    /** Doom damage floors apply their percentage-like damage about once a second. */
    public static final int FLOOR_DAMAGE_TICKS = 20;

    // Must match the compact C3D sector types emitted by doom_wad_core.py.
    public static final int SECTOR_DAMAGE_5 = 705;
    public static final int SECTOR_DAMAGE_10 = 710;
    public static final int SECTOR_DAMAGE_20 = 720;

    /** Custom entity type = DOOM_ITEM_BASE + original Doom thing type. */
    public static final int DOOM_ITEM_BASE = 9000;
    public static final int DOOM_BARREL_TYPE = DOOM_ITEM_BASE + 2035;

    private static boolean active;
    private static boolean godMode;

    private DoomGameMode() {
    }

    public static boolean isDoomLevelPath(String levelPath) {
        return levelPath != null && levelPath.indexOf("/custom/doom-") >= 0;
    }

    public static void setActive(boolean enabled) {
        active = enabled;
        // A cheat must never leak into a legacy CovertOps level. It remains
        // enabled across a Doom E1M1 -> E1M2 transition because both loads set
        // enabled=true.
        if (!enabled) godMode = false;
    }

    public static boolean isActive() {
        return active;
    }

    /** Toggles the Doom-only invulnerability cheat; false outside Doom maps. */
    public static boolean toggleGodMode() {
        if (!active) return false;
        godMode = !godMode;
        return true;
    }

    public static boolean isGodMode() {
        return active && godMode;
    }

    /** A fresh game always starts without an inherited cheat state. */
    public static void resetGodMode() {
        godMode = false;
    }

    /** Returns the fixed Doom damage-floor amount for a C3D sector type. */
    public static int getFloorDamage(int sectorType) {
        switch (sectorType) {
            case SECTOR_DAMAGE_5:
                return 5;
            case SECTOR_DAMAGE_10:
                return 10;
            case SECTOR_DAMAGE_20:
                return 20;
            default:
                return 0;
        }
    }

    /** Gives every Doom weapon immediately, as requested for the conversion sandbox. */
    public static void configurePlayerLoadout(WeaponManager weaponManager) {
        GameEngine.playerHealth = 100;
        GameEngine.playerArmor = 0;
        for (int i = 0; i < GameEngine.keysCollected.length; ++i) {
            GameEngine.keysCollected[i] = false;
        }

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
            case 5: // blue keycard
                GameEngine.keysCollected[0] = true;
                return true;
            case 6: // yellow keycard
                GameEngine.keysCollected[2] = true;
                return true;
            case 13: // red keycard
                GameEngine.keysCollected[1] = true;
                return true;
            case 8: // backpack: compact engine has no ammo-capacity table
                GameEngine.ammoCounts[WeaponFactory.PISTOL] += 10;
                GameEngine.ammoCounts[WeaponFactory.SHOTGUN] += 4;
                GameEngine.ammoCounts[WeaponFactory.ROCKET_LAUNCHER] += 1;
                GameEngine.ammoCounts[WeaponFactory.PLASMA_RIFLE] += 20;
                return true;
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
