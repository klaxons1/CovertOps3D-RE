public class WeaponFactory {

    // ==================== ID оружия ====================
    public static final int FIST = 0;
    public static final int LUGER = 1;
    public static final int MAUSER = 2;
    public static final int RIFLE = 3;
    public static final int STEN = 4;
    public static final int PANZERFAUST = 5;
    public static final int DYNAMITE = 6;
    public static final int SONIC = 7;
    public static final int WEAPON_COUNT = 8;

    // ==================== Создание оружия ====================

    public static Weapon createFist() {
        int[] damage = new int[]{5, 5, 5};
        int[] cooldown = new int[]{4, 4, 4};

        Weapon w = new Weapon(FIST, "Fist", 2, true,
                damage, cooldown, -1, false,
                false, false, false, false);

        w.setSpritePath(0, "/gamedata/sprites/fist_a.png");
        w.setSpritePath(1, "/gamedata/sprites/fist_b.png");
        return w;
    }

    public static Weapon createLuger() {
        int[] damage = new int[]{25, 25, 25};
        int[] cooldown = new int[]{6, 6, 6};

        Weapon w = new Weapon(LUGER, "Luger", 2, true,
                damage, cooldown, LUGER, false,
                false, false, false, false);

        w.setSpritePath(0, "/gamedata/sprites/luger_a.png");
        w.setSpritePath(1, "/gamedata/sprites/luger_b.png");
        return w;
    }

    public static Weapon createMauser() {
        int[] damage = new int[]{30, 30, 30};
        int[] cooldown = new int[]{8, 8, 8};

        Weapon w = new Weapon(MAUSER, "Mauser", 2, false,
                damage, cooldown, MAUSER, false,
                false, false, false, false);

        w.setSpritePath(0, "/gamedata/sprites/mauser_a.png");
        w.setSpritePath(1, "/gamedata/sprites/mauser_b.png");
        return w;
    }

    public static Weapon createRifle() {
        int[] damage = new int[]{25, 25, 25};
        int[] cooldown = new int[]{3, 3, 3};

        Weapon w = new Weapon(RIFLE, "Rifle", 2, false,
                damage, cooldown, LUGER, true,
                true, false, false, false);

        w.setSpritePath(0, "/gamedata/sprites/m40_a.png");
        w.setSpritePath(1, "/gamedata/sprites/m40_b.png");
        return w;
    }

    public static Weapon createSten() {
        int[] damage = new int[]{25, 25, 25};
        int[] cooldown = new int[]{2, 2, 2};

        Weapon w = new Weapon(STEN, "Sten", 2, false,
                damage, cooldown, LUGER, true,
                true, false, false, false);

        w.setSpritePath(0, "/gamedata/sprites/sten_a.png");
        w.setSpritePath(1, "/gamedata/sprites/sten_b.png");
        return w;
    }

    public static Weapon createPanzerfaust() {
        int[] damage = new int[]{150, 150, 150};
        int[] cooldown = new int[]{1, 1, 1};

        Weapon w = new Weapon(PANZERFAUST, "Panzerfaust", 3, false,
                damage, cooldown, PANZERFAUST, false,
                false, true, false, true);

        w.setSpritePath(0, "/gamedata/sprites/panzerfaust_a.png");
        w.setSpritePath(1, "/gamedata/sprites/panzerfaust_b.png");
        w.setSpritePath(2, "/gamedata/sprites/panzerfaust_c.png");
        return w;
    }

    public static Weapon createDynamite() {
        int[] damage = new int[]{100, 100, 100};
        int[] cooldown = new int[]{0, 0, 0};

        Weapon w = new Weapon(DYNAMITE, "Dynamite", 1, true,
                damage, cooldown, DYNAMITE, false,
                false, false, true, true);

        w.setSpritePath(0, "/gamedata/sprites/dynamite.png");
        return w;
    }

    public static Weapon createSonic() {
        int[] damage = new int[]{100, 100, 100};
        int[] cooldown = new int[]{10, 10, 10};

        Weapon w = new Weapon(SONIC, "Sonic", 2, true,
                damage, cooldown, SONIC, false,
                false, true, false, false);

        w.setSpritePath(0, "/gamedata/sprites/sonic_a.png");
        w.setSpritePath(1, "/gamedata/sprites/sonic_b.png");
        return w;
    }

    public static Weapon[] createAllWeapons() {
        Weapon[] weapons = new Weapon[WEAPON_COUNT];
        weapons[FIST] = createFist();
        weapons[LUGER] = createLuger();
        weapons[MAUSER] = createMauser();
        weapons[RIFLE] = createRifle();
        weapons[STEN] = createSten();
        weapons[PANZERFAUST] = createPanzerfaust();
        weapons[DYNAMITE] = createDynamite();
        weapons[SONIC] = createSonic();
        return weapons;
    }
}