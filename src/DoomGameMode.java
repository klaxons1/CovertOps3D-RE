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
}
