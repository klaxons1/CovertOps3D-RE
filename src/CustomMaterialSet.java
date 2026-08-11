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
 */
public final class CustomMaterialSet {

    private static final int SLOT_COUNT = 128;
    private static final int SKY_WIDTH = 64;
    private static final int SKY_HEIGHT = 128;
    private static final int FLAT_SIZE = 64;

    private Texture[] wallTextures = new Texture[SLOT_COUNT];
    private Sprite[] flatTextures = new Sprite[SLOT_COUNT];
    private Texture skyTexture;
    private int wallTextureCount;
    private int flatTextureCount;

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
            } else if (key.equals("sky")) {
                if (materials.skyTexture != null) throw new IOException("Duplicate sky texture");
                materials.skyTexture = loadSkyTexture(texturePath);
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

    /** Installs the manifest sky texture, if present. */
    public void installSkyTexture() {
        if (skyTexture != null) {
            PortalRenderer.setSkyboxTexture(skyTexture);
        }
    }

    public Texture getWallTexture(int slot) {
        return slot > 0 && slot < SLOT_COUNT ? wallTextures[slot] : null;
    }

    public Sprite getFlatTexture(int slot) {
        return slot > 0 && slot < SLOT_COUNT ? flatTextures[slot] : null;
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

    private static String resolvePath(String basePath, String value) {
        return value.charAt(0) == '/' ? value : basePath + value;
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
