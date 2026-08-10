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

    // ==================== BMP-спрайты (палитра зависит от сектора) ====================
    private boolean[] bmpFrame;        // frame loaded from an indexed BMP
    private byte[][] bmpPixels;        // palette indices, w*h per frame
    private int[] bmpWidth;
    private int[] bmpHeight;
    private int[][][] bmpShadeTables;  // per frame: 16 light levels x palette ARGB
    private int[] bmpShadeLevel;       // light level baked into sprites[i] (-1 = none)
    private int[] bmpArgbBuffer;       // reusable pixel buffer (max frame area)

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
        this.bmpFrame = new boolean[spriteCount];
        this.bmpPixels = new byte[spriteCount][];
        this.bmpWidth = new int[spriteCount];
        this.bmpHeight = new int[spriteCount];
        this.bmpShadeTables = new int[spriteCount][][];
        this.bmpShadeLevel = new int[spriteCount];
        for (int i = 0; i < spriteCount; i++) {
            bmpShadeLevel[i] = -1;
        }
    }

    public void setSpritePath(int index, String path) {
        if (index >= 0 && index < spriteCount) {
            spritePaths[index] = path;
        }
    }

    // ==================== Загрузка ресурсов ====================

    public void loadSprites() {
        for (int i = 0; i < spriteCount; i++) {
            if (spritePaths[i] == null) {
                continue;
            }
            if (tryLoadBmpSprite(i)) {
                continue;
            }
            try {
                sprites[i] = Image.createImage(spritePaths[i]);
            } catch (Exception e) {
                DebugLogger.logException("Weapon.loadSprites "+name, e);
                sprites[i] = null;
            }
        }
    }

    /**
     * Loads the frame from an indexed BMP (must sit next to the configured
     * PNG path, e.g. luger_a.png -> luger_a.bmp). BMP sprites keep their
     * palette separate from the pixels, so the image is re-tinted with the
     * light level of the sector the player is standing in, exactly like the
     * world textures. Palette index 0 is treated as transparent.
     *
     * @return true if the frame was loaded from a BMP
     */
    private boolean tryLoadBmpSprite(int index) {
        String path = spritePaths[index];
        String lower = path.toLowerCase();

        String bmpPath;
        if (lower.endsWith(".bmp")) {
            bmpPath = path;
        } else if (lower.endsWith(".png")) {
            bmpPath = path.substring(0, path.length() - 4) + ".bmp";
        } else {
            return false;
        }

        BMPLoader bmp;
        try {
            bmp = BMPLoader.loadBMP(bmpPath);
        } catch (java.io.IOException notFound) {
            if (bmpPath == path) {
                DebugLogger.logException("Weapon.loadSprites bmp "+name, notFound);
            }
            return false; // no BMP next to the PNG - use the PNG
        } catch (RuntimeException badFile) {
            DebugLogger.logException("Weapon.loadSprites bmp "+name, badFile);
            return false;
        }

        bmpPixels[index] = bmp.indices;
        bmpWidth[index] = bmp.width;
        bmpHeight[index] = bmp.height;

        int[] rgb = new int[bmp.palette.length];
        for (int c = 0; c < rgb.length; c++) {
            rgb[c] = bmp.palette[c] & 0xFFFFFF;
        }
        // Same shading curve as the world textures (16 brightness levels)
        bmpShadeTables[index] = Texture.createColorPalettes(rgb);

        bmpFrame[index] = true;
        bmpShadeLevel[index] = -1;
        DebugLogger.log("Weapon", "bmp sprite " + bmpPath + " " + bmp.width + "x" + bmp.height + "x" + bmp.bitDepth);
        return true;
    }

    public void unloadSprites() {
        for (int i = 0; i < spriteCount; i++) {
            sprites[i] = null;
            bmpShadeLevel[i] = -1; // rebuild with fresh sector light on next draw
        }
    }

    /**
     * Rebuilds the RGB image of a BMP frame for the given sector light level.
     * Only re-renders when the light level actually changed.
     */
    private Image getBmpSprite(int frame, int lightLevel) {
        if (lightLevel < 0) lightLevel = 0;
        if (lightLevel > 15) lightLevel = 15;

        if (sprites[frame] == null || bmpShadeLevel[frame] != lightLevel) {
            int w = bmpWidth[frame];
            int h = bmpHeight[frame];
            byte[] idx = bmpPixels[frame];
            int[] palette = bmpShadeTables[frame][lightLevel];

            int pixels = w * h;
            if (bmpArgbBuffer == null || bmpArgbBuffer.length < pixels) {
                bmpArgbBuffer = new int[pixels];
            }
            int[] argb = bmpArgbBuffer;
            for (int p = 0; p < pixels; p++) {
                int colorIndex = idx[p] & 0xFF;
                // index 0 = transparency, everything else is shaded by sector light
                argb[p] = (colorIndex == 0) ? 0 : palette[colorIndex];
            }
            // createRGBImage copies the data, the buffer can be reused for frames
            sprites[frame] = Image.createRGBImage(argb, w, h, true);
            bmpShadeLevel[frame] = lightLevel;
        }
        return sprites[frame];
    }

    // ==================== Геттеры ====================

    public Image getSprite(int frame) {
        return getSprite(frame, 8); // legacy callers get the neutral (unshaded) variant
    }

    /**
     * Returns the sprite for the frame; BMP sprites are tinted with the
     * sector light level (0..16, clamped to the 16 shading levels).
     */
    public Image getSprite(int frame, int lightLevel) {
        if (frame >= 0 && frame < spriteCount) {
            if (bmpFrame[frame]) {
                return getBmpSprite(frame, lightLevel);
            }
            if (sprites[frame] != null) {
                return sprites[frame];
            }
        }
        if (bmpFrame[0]) {
            return getBmpSprite(0, lightLevel);
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
        if (bmpFrame[0]) {
            return bmpWidth[0];
        }
        if (sprites[0] != null) {
            return sprites[0].getWidth();
        }
        return 0;
    }

    public int getHeight() {
        if (bmpFrame[0]) {
            return bmpHeight[0];
        }
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