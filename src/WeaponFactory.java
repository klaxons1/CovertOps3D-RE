/** Doom weapon definitions for the C3D runtime path.
 *
 * IDs intentionally remain 0..7 so the compact CLDC arrays and save layout
 * stay fixed-size. Legacy aliases below keep low-level compatibility code
 * compiling while the gameplay layer migrates to Doom names.
 */
public final class WeaponFactory {

    public static final int FIST = 0;
    public static final int PISTOL = 1;
    public static final int SHOTGUN = 2;
    public static final int CHAINGUN = 3;
    public static final int ROCKET_LAUNCHER = 4;
    public static final int PLASMA_RIFLE = 5;
    public static final int BFG9000 = 6;
    public static final int CHAINSAW = 7;
    public static final int WEAPON_COUNT = 8;

    // Compatibility names. Gameplay code should use the Doom names above.
    public static final int LUGER = PISTOL;
    public static final int MAUSER = SHOTGUN;
    public static final int RIFLE = CHAINGUN;
    public static final int STEN = ROCKET_LAUNCHER;
    public static final int PANZERFAUST = PLASMA_RIFLE;
    public static final int DYNAMITE = BFG9000;
    public static final int SONIC = CHAINSAW;

    private static final String HUD_PREFIX = "/gamedata/custom/doom-e1m1/hud/";

    private WeaponFactory() {
    }

    public static Weapon createFist() {
        return create(FIST, "Fist", "fist", new int[]{8, 8, 8},
                new int[]{4, 4, 4}, -1, false);
    }

    public static Weapon createPistol() {
        return create(PISTOL, "Pistol", "pistol", new int[]{15, 15, 15},
                new int[]{5, 5, 5}, PISTOL, false);
    }

    public static Weapon createShotgun() {
        return create(SHOTGUN, "Shotgun", "shotgun", new int[]{70, 70, 70},
                new int[]{10, 10, 10}, SHOTGUN, false);
    }

    public static Weapon createChaingun() {
        return create(CHAINGUN, "Chaingun", "chaingun", new int[]{15, 15, 15},
                new int[]{2, 2, 2}, PISTOL, true);
    }

    public static Weapon createRocketLauncher() {
        return create(ROCKET_LAUNCHER, "Rocket", "rocket", new int[]{120, 120, 120},
                new int[]{10, 10, 10}, ROCKET_LAUNCHER, false);
    }

    public static Weapon createPlasmaRifle() {
        return create(PLASMA_RIFLE, "Plasma", "plasma", new int[]{25, 25, 25},
                new int[]{2, 2, 2}, PLASMA_RIFLE, true);
    }

    public static Weapon createBfg9000() {
        return create(BFG9000, "BFG9000", "bfg", new int[]{240, 240, 240},
                new int[]{16, 16, 16}, BFG9000, false);
    }

    public static Weapon createChainsaw() {
        return create(CHAINSAW, "Chainsaw", "chainsaw", new int[]{20, 20, 20},
                new int[]{1, 1, 1}, -1, true);
    }

    private static Weapon create(int id, String name, String hudName, int[] damage,
                                 int[] cooldown, int ammoType, boolean automatic) {
        Weapon weapon = new Weapon(id, name, 2, true, damage, cooldown, ammoType,
                false, automatic, false, false, false);
        weapon.setSpritePath(0, HUD_PREFIX + hudName + "_a.bmp");
        weapon.setSpritePath(1, HUD_PREFIX + hudName + "_b.bmp");
        return weapon;
    }

    public static Weapon[] createAllWeapons() {
        Weapon[] weapons = new Weapon[WEAPON_COUNT];
        weapons[FIST] = createFist();
        weapons[PISTOL] = createPistol();
        weapons[SHOTGUN] = createShotgun();
        weapons[CHAINGUN] = createChaingun();
        weapons[ROCKET_LAUNCHER] = createRocketLauncher();
        weapons[PLASMA_RIFLE] = createPlasmaRifle();
        weapons[BFG9000] = createBfg9000();
        weapons[CHAINSAW] = createChainsaw();
        return weapons;
    }
}
