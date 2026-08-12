import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Runtime materials for a C3D custom level.
 *
 * Unlike legacy TX/SP atlases, custom levels reference individual indexed BMP
 * resources through a tiny manifest. PNG may be used by the editor, but the
 * JAR-facing format stays BMP4/BMP8 so palette indices remain available for
 * Java ME lighting.
 *
 * Manifest example:
 *   wall.1=textures/brick.bmp
 *   flat.1=textures/floor.bmp
 *   sky=textures/sky.bmp
 *   sprite.1=sprites/doom/imp.bmp
 */
public final class CustomMaterialSet {

    private static final int SLOT_COUNT = 128;
    private static final int SKY_WIDTH = 64;
    private static final int SKY_HEIGHT = 128;
    private static final int FLAT_SIZE = 64;

    private Texture[] wallTextures = new Texture[SLOT_COUNT];
    private Sprite[] flatTextures = new Sprite[SLOT_COUNT];
    // External billboards use negative texture ids so they cannot collide with
    // positive wall slots in LevelLoader.textureTable.
    private Texture[] spriteTextures = new Texture[SLOT_COUNT];
    // Each animation entry points at already-loaded material slots. The target
    // object stays registered in LevelLoader; GameWorld swaps its pixel/palette
    // references at a fixed tick, so PortalRenderer keeps its branch-free path.
    private Texture[][] animatedWallFrames = new Texture[SLOT_COUNT][];
    private Sprite[][] animatedFlatFrames = new Sprite[SLOT_COUNT][];
    private Texture skyTexture;
    private int wallTextureCount;
    private int flatTextureCount;
    private int spriteTextureCount;

    private CustomMaterialSet() {
    }

    /** Loads an external-texture manifest from the JAR. */
    public static CustomMaterialSet load(String manifestPath) throws IOException {
        if (manifestPath == null) throw new NullPointerException();

        CustomMaterialSet materials = new CustomMaterialSet();
        String manifest = readResourceText(manifestPath);
        String basePath = getParentPath(manifestPath);
        int lineStart = 0;

        while (lineStart < manifest.length()) {
            int lineEnd = manifest.indexOf('\n', lineStart);
            if (lineEnd < 0) lineEnd = manifest.length();
            String line = manifest.substring(lineStart, lineEnd).trim();
            lineStart = lineEnd + 1;

            if (line.length() == 0 || line.charAt(0) == '#' || line.charAt(0) == ';') {
                continue;
            }

            int equals = line.indexOf('=');
            if (equals <= 0) throw new IOException("Bad material entry: " + line);
            String key = line.substring(0, equals).trim();
            String value = line.substring(equals + 1).trim();
            if (value.length() == 0) throw new IOException("Empty material path: " + key);
            String texturePath = resolvePath(basePath, value);

            if (startsWith(key, "wall.")) {
                int slot = parseSlot(key.substring(5));
                if (materials.wallTextures[slot] != null) {
                    throw new IOException("Duplicate wall texture slot: " + slot);
                }
                materials.wallTextures[slot] = loadWallTexture(texturePath, (byte)slot);
                materials.wallTextureCount++;
            } else if (startsWith(key, "flat.")) {
                int slot = parseSlot(key.substring(5));
                if (materials.flatTextures[slot] != null) {
                    throw new IOException("Duplicate flat texture slot: " + slot);
                }
                materials.flatTextures[slot] = loadFlatTexture(texturePath, (byte)slot);
                materials.flatTextureCount++;
            } else if (startsWith(key, "sprite.")) {
                int slot = parseSlot(key.substring(7));
                if (materials.spriteTextures[slot] != null) {
                    throw new IOException("Duplicate sprite texture slot: " + slot);
                }
                materials.spriteTextures[slot] = loadSpriteTexture(texturePath, (byte)slot);
                materials.spriteTextureCount++;
            } else if (key.equals("sky")) {
                if (materials.skyTexture != null) throw new IOException("Duplicate sky texture");
                materials.skyTexture = loadSkyTexture(texturePath);
            } else if (startsWith(key, "anim.wall.")) {
                int slot = parseSlot(key.substring(10));
                if (materials.animatedWallFrames[slot] != null) {
                    throw new IOException("Duplicate wall animation slot: " + slot);
                }
                materials.animatedWallFrames[slot] = parseWallAnimation(value,
                        materials.wallTextures, slot);
            } else if (startsWith(key, "anim.flat.")) {
                int slot = parseSlot(key.substring(10));
                if (materials.animatedFlatFrames[slot] != null) {
                    throw new IOException("Duplicate flat animation slot: " + slot);
                }
                materials.animatedFlatFrames[slot] = parseFlatAnimation(value,
                        materials.flatTextures, slot);
            } else {
                throw new IOException("Unknown material key: " + key);
            }
        }

        return materials;
    }

    /** Registers wall textures in the existing renderer's texture table. */
    public void installWallTextures() {
        for (int slot = 1; slot < SLOT_COUNT; ++slot) {
            Texture texture = wallTextures[slot];
            if (texture != null) {
                LevelLoader.registerExternalTexture((byte)slot, texture);
            }
        }
    }

    /** Registers external billboard textures under negative texture ids. */
    public void installSpriteTextures() {
        for (int slot = 1; slot < SLOT_COUNT; ++slot) {
            Texture texture = spriteTextures[slot];
            if (texture != null) {
                LevelLoader.registerExternalTexture((byte)-slot, texture);
            }
        }
    }

    /** Installs the manifest sky texture, if present. */
    public void installSkyTexture() {
        if (skyTexture != null) {
            PortalRenderer.setSkyboxTexture(skyTexture);
        }
    }

    /** Installs compact Doom wall/flat animation references into one world. */
    public void installTextureAnimations(GameWorld world) {
        if (world == null) throw new NullPointerException();
        int wallCount = 0;
        int flatCount = 0;
        for (int slot = 1; slot < SLOT_COUNT; ++slot) {
            if (animatedWallFrames[slot] != null) ++wallCount;
            if (animatedFlatFrames[slot] != null) ++flatCount;
        }
        if (wallCount == 0 && flatCount == 0) return;

        Texture[] wallTargets = new Texture[wallCount];
        Texture[][] wallFrames = new Texture[wallCount][];
        Sprite[] flatTargets = new Sprite[flatCount];
        Sprite[][] flatFrames = new Sprite[flatCount][];
        int wallIndex = 0;
        int flatIndex = 0;
        for (int slot = 1; slot < SLOT_COUNT; ++slot) {
            if (animatedWallFrames[slot] != null) {
                wallTargets[wallIndex] = wallTextures[slot];
                wallFrames[wallIndex++] = animatedWallFrames[slot];
            }
            if (animatedFlatFrames[slot] != null) {
                flatTargets[flatIndex] = flatTextures[slot];
                flatFrames[flatIndex++] = animatedFlatFrames[slot];
            }
        }
        world.installTextureAnimations(wallTargets, wallFrames, flatTargets, flatFrames);
    }

    public Texture getWallTexture(int slot) {
        return slot > 0 && slot < SLOT_COUNT ? wallTextures[slot] : null;
    }

    public Sprite getFlatTexture(int slot) {
        return slot > 0 && slot < SLOT_COUNT ? flatTextures[slot] : null;
    }

    public Texture getSpriteTexture(int slot) {
        return slot > 0 && slot < SLOT_COUNT ? spriteTextures[slot] : null;
    }

    public Texture getSkyTexture() {
        return skyTexture;
    }

    public int getWallTextureCount() {
        return wallTextureCount;
    }

    public int getFlatTextureCount() {
        return flatTextureCount;
    }

    public int getSpriteTextureCount() {
        return spriteTextureCount;
    }

    public int getAnimatedWallCount() {
        return countAnimations(animatedWallFrames);
    }

    public int getAnimatedFlatCount() {
        return countAnimations(animatedFlatFrames);
    }

    private static Texture loadWallTexture(String path, byte slot) throws IOException {
        BMPLoader bmp = BMPLoader.loadBMP(path);
        validatePaletteIndices(bmp, path);

        if (!isPowerOfTwo(bmp.width) || bmp.width < 2) {
            throw new IOException("Wall width must be a power of two: " + path);
        }
        if (bmp.height != 16 && bmp.height != 64 && bmp.height != 128) {
            throw new IOException("Wall height must be 16, 64 or 128: " + path);
        }

        Texture texture = new Texture(slot, bmp.width, bmp.height, 0, 0, first16Colors(bmp.palette));
        for (int x = 0; x < bmp.width; x += 2) {
            byte[] column = new byte[bmp.height];
            for (int y = 0; y < bmp.height; ++y) {
                int high = bmp.indices[y * bmp.width + x] & 15;
                int low = x + 1 < bmp.width ? bmp.indices[y * bmp.width + x + 1] & 15 : 0;
                column[y] = (byte)((high << 4) | low);
            }
            texture.setPixelData(x, column);
        }
        return texture;
    }

    private static Sprite loadFlatTexture(String path, byte slot) throws IOException {
        BMPLoader bmp = BMPLoader.loadBMP(path);
        validatePaletteIndices(bmp, path);
        if (bmp.width != FLAT_SIZE || bmp.height != FLAT_SIZE) {
            throw new IOException("Flat texture must be 64x64: " + path);
        }

        Sprite sprite = new Sprite(slot, bmp.indices);
        sprite.colorPalettes = Texture.createColorPalettes(first16Colors(bmp.palette));
        sprite.buildFlatColors();
        return sprite;
    }

    /**
     * Loads one transparent-index billboard. Sprite rendering accepts arbitrary
     * dimensions, unlike walls which must use a fast power-of-two layout.
     */
    private static Texture loadSpriteTexture(String path, byte slot) throws IOException {
        BMPLoader bmp = BMPLoader.loadBMP(path);
        validatePaletteIndices(bmp, path);
        if (bmp.width < 1 || bmp.height < 1 || bmp.width > 255 || bmp.height > 255) {
            throw new IOException("Sprite dimensions must be 1..255: " + path);
        }

        Texture texture = new Texture((byte)-slot, bmp.width, bmp.height, 0, 0,
                first16Colors(bmp.palette));
        for (int x = 0; x < bmp.width; x += 2) {
            byte[] column = new byte[bmp.height];
            for (int y = 0; y < bmp.height; ++y) {
                int high = bmp.indices[y * bmp.width + x] & 15;
                int low = x + 1 < bmp.width ? bmp.indices[y * bmp.width + x + 1] & 15 : 0;
                column[y] = (byte)((high << 4) | low);
            }
            texture.setPixelData(x, column);
        }
        return texture;
    }

    private static Texture loadSkyTexture(String path) throws IOException {
        Texture sky = loadWallTexture(path, (byte)0);
        if (sky.width != SKY_WIDTH || sky.height != SKY_HEIGHT) {
            throw new IOException("Sky texture must be 64x128: " + path);
        }
        return sky;
    }

    private static void validatePaletteIndices(BMPLoader bmp, String path) throws IOException {
        if (bmp.palette == null || bmp.palette.length < 16) {
            throw new IOException("Texture palette needs at least 16 colors: " + path);
        }
        for (int i = 0; i < bmp.indices.length; ++i) {
            if ((bmp.indices[i] & 0xFF) > 15) {
                throw new IOException("Only palette indices 0..15 are supported: " + path);
            }
        }
    }

    private static int[] first16Colors(int[] source) {
        int[] colors = new int[16];
        System.arraycopy(source, 0, colors, 0, 16);
        return colors;
    }

    private static boolean isPowerOfTwo(int value) {
        return (value & (value - 1)) == 0;
    }

    private static Texture[] parseWallAnimation(String value, Texture[] textures, int targetSlot)
            throws IOException {
        Texture target = textures[targetSlot];
        if (target == null) {
            throw new IOException("Animated wall target must be defined first: " + targetSlot);
        }
        int[] slots = parseAnimationSlots(value);
        Texture[] frames = new Texture[slots.length];
        for (int i = 0; i < slots.length; ++i) {
            Texture frame = textures[slots[i]];
            if (frame == null) {
                throw new IOException("Animated wall frame is missing: " + slots[i]);
            }
            if (frame.width != target.width || frame.height != target.height) {
                throw new IOException("Animated wall frame size differs at slot: " + slots[i]);
            }
            frames[i] = frame;
        }
        return frames;
    }

    private static Sprite[] parseFlatAnimation(String value, Sprite[] sprites, int targetSlot)
            throws IOException {
        if (sprites[targetSlot] == null) {
            throw new IOException("Animated flat target must be defined first: " + targetSlot);
        }
        int[] slots = parseAnimationSlots(value);
        Sprite[] frames = new Sprite[slots.length];
        for (int i = 0; i < slots.length; ++i) {
            Sprite frame = sprites[slots[i]];
            if (frame == null || frame.pixelData == null || frame.pixelData.length != 4096) {
                throw new IOException("Animated flat frame is missing: " + slots[i]);
            }
            frames[i] = frame;
        }
        return frames;
    }

    /** Parses a compact comma-separated material slot list without String.split. */
    private static int[] parseAnimationSlots(String value) throws IOException {
        int count = 1;
        for (int index = 0; index < value.length(); ++index) {
            if (value.charAt(index) == ',') ++count;
        }
        if (count < 2) throw new IOException("Animation needs at least two frames: " + value);
        int[] slots = new int[count];
        int start = 0;
        for (int index = 0; index < count; ++index) {
            int end = value.indexOf(',', start);
            if (end < 0) end = value.length();
            String number = value.substring(start, end).trim();
            if (number.length() == 0) throw new IOException("Empty animation frame: " + value);
            slots[index] = parseSlot(number);
            start = end + 1;
        }
        return slots;
    }

    private static int countAnimations(Object[] animations) {
        int count = 0;
        for (int index = 1; index < animations.length; ++index) {
            if (animations[index] != null) ++count;
        }
        return count;
    }

    private static int parseSlot(String value) throws IOException {
        try {
            int slot = Integer.parseInt(value);
            if (slot <= 0 || slot >= SLOT_COUNT) throw new NumberFormatException();
            return slot;
        } catch (NumberFormatException e) {
            throw new IOException("Material slot must be 1..127: " + value);
        }
    }

    private static boolean startsWith(String text, String prefix) {
        return text.length() >= prefix.length()
                && text.substring(0, prefix.length()).equals(prefix);
    }

    private static String getParentPath(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? "/" : path.substring(0, slash + 1);
    }

    /** Resolves normal C3M relatives, including ../doom-common shared assets. */
    private static String resolvePath(String basePath, String value) {
        String path = value.charAt(0) == '/' ? value : basePath + value;
        int parent;
        // Class.getResourceAsStream does not consistently normalize .. across
        // MIDP implementations, so collapse it here without java.io.File.
        while ((parent = path.indexOf("/../")) >= 0) {
            int previous = path.lastIndexOf('/', parent - 1);
            if (previous < 0) break;
            path = path.substring(0, previous) + path.substring(parent + 3);
        }
        return path;
    }

    private static String readResourceText(String path) throws IOException {
        InputStream input = CustomMaterialSet.class.getResourceAsStream(path);
        if (input == null) throw new IOException("Material manifest not found: " + path);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[256];
        try {
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (count > 0) output.write(buffer, 0, count);
            }
            return new String(output.toByteArray(), "UTF-8");
        } finally {
            try {
                input.close();
            } catch (IOException ignored) {
            }
        }
    }
}
