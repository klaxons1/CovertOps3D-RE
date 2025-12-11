import javax.microedition.lcdui.Image;

public class Weapon {

    // ==================== Идентификация ====================
    public int id;
    public String name;

    // ==================== Спрайты ====================
    private String[] spritePaths;
    private Image[] sprites;
    private int spriteCount;
    private boolean centered;

    // ==================== Характеристики ====================
    private int[] damage;
    private int[] cooldown;

    // ==================== Боеприпасы ====================
    private int ammoType;
    private boolean usesSharedAmmo;

    // ==================== Флаги ====================
    private boolean isAutomatic;
    private boolean isProjectile;
    private boolean isThrowable;
    private boolean consumeOnUse;

    // ==================== Конструктор ====================

    public Weapon(int id, String name, int spriteCount, boolean centered,
                  int[] damage, int[] cooldown, int ammoType, boolean usesSharedAmmo,
                  boolean isAutomatic, boolean isProjectile, boolean isThrowable,
                  boolean consumeOnUse) {
        this.id = id;
        this.name = name;
        this.spriteCount = spriteCount;
        this.centered = centered;
        this.damage = damage;
        this.cooldown = cooldown;
        this.ammoType = ammoType;
        this.usesSharedAmmo = usesSharedAmmo;
        this.isAutomatic = isAutomatic;
        this.isProjectile = isProjectile;
        this.isThrowable = isThrowable;
        this.consumeOnUse = consumeOnUse;
        this.spritePaths = new String[spriteCount];
        this.sprites = new Image[spriteCount];
    }

    public void setSpritePath(int index, String path) {
        if (index >= 0 && index < spriteCount) {
            spritePaths[index] = path;
        }
    }

    // ==================== Загрузка ресурсов ====================

    public void loadSprites() {
        for (int i = 0; i < spriteCount; i++) {
            if (spritePaths[i] != null) {
                try {
                    sprites[i] = Image.createImage(spritePaths[i]);
                } catch (Exception e) {
                    sprites[i] = null;
                }
            }
        }
    }

    public void unloadSprites() {
        for (int i = 0; i < spriteCount; i++) {
            sprites[i] = null;
        }
    }

    // ==================== Геттеры ====================

    public Image getSprite(int frame) {
        if (frame >= 0 && frame < spriteCount && sprites[frame] != null) {
            return sprites[frame];
        }
        return sprites[0];
    }

    public int getSpriteCount() {
        return spriteCount;
    }

    public boolean isCentered() {
        return centered;
    }

    public int getDamage(int difficulty) {
        return damage[difficulty];
    }

    public int getCooldown(int difficulty) {
        return cooldown[difficulty];
    }

    public int getAmmoType() {
        return ammoType;
    }

    public boolean getUsesSharedAmmo() {
        return usesSharedAmmo;
    }

    public boolean getIsAutomatic() {
        return isAutomatic;
    }

    public boolean getIsProjectile() {
        return isProjectile;
    }

    public boolean getIsThrowable() {
        return isThrowable;
    }

    public boolean getConsumeOnUse() {
        return consumeOnUse;
    }

    public int getWidth() {
        if (sprites[0] != null) {
            return sprites[0].getWidth();
        }
        return 0;
    }

    public int getHeight() {
        if (sprites[0] != null) {
            return sprites[0].getHeight();
        }
        return 0;
    }

    public boolean requiresAmmo() {
        return ammoType >= 0;
    }

    public int getEffectiveAmmoType() {
        if (usesSharedAmmo) {
            return 1; // Luger ammo
        }
        return ammoType;
    }
}