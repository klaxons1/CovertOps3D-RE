import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Desktop smoke test for the Java ME-compatible core.
 *
 * It intentionally uses Java 1.3 syntax and APIs only.  The CI job compiles
 * it with the same CLDC/MIDP boot class path as the MIDlet, then runs it on
 * the JDK solely to exercise resource decoding.
 */
public final class JavaMeSmokeTest {

    private static final String[] LEVEL_FILES = new String[]{
            "01a", "01b", "02a", "02b", "04", "05", "06a",
            "06b", "06c", "07a", "07b", "08a", "08b"
    };

    private JavaMeSmokeTest() { }

    public static void main(String[] args) throws Exception {
        testBinaryUtils();
        testRotationNormalization();
        testPaletteLighting();
        testAllShippedLevels();
        System.out.println("Java ME smoke test: OK");
    }

    private static void testBinaryUtils() throws Exception {
        DataInputStream bigEndian = new DataInputStream(new ByteArrayInputStream(new byte[]{
                0x12, 0x34, (byte)0x89, (byte)0xab, (byte)0xcd, (byte)0xef
        }));
        assertEquals("short BE", 0x1234, BinaryUtils.readShortBE(bigEndian) & 0xffff);
        assertEquals("int BE", 0x89abcdef, BinaryUtils.readIntBE(bigEndian));

        DataInputStream littleEndian = new DataInputStream(new ByteArrayInputStream(new byte[]{
                0x34, 0x12, (byte)0xef, (byte)0xcd, (byte)0xab, (byte)0x89
        }));
        assertEquals("short LE", 0x1234, BinaryUtils.readShortLE(littleEndian) & 0xffff);
        assertEquals("int LE", 0x89abcdef, BinaryUtils.readIntLE(littleEndian));

        DataInputStream nonSkipping = new DataInputStream(new NonSkippingInputStream(new byte[]{
                1, 2, 3, 4
        }));
        BinaryUtils.skipBytes(nonSkipping, 3);
        assertEquals("skip fallback", 4, nonSkipping.read());

        try {
            BinaryUtils.readShortBE(new DataInputStream(new ByteArrayInputStream(new byte[]{1})));
            throw new RuntimeException("Truncated big-endian value was accepted");
        } catch (EOFException expected) {
            // Expected: readUnsignedByte() must never silently turn EOF into data.
        }

        try {
            BinaryUtils.skipBytes(new DataInputStream(new NonSkippingInputStream(new byte[]{1})), 2);
            throw new RuntimeException("Truncated skip was accepted");
        } catch (EOFException expected) {
            // Expected.
        }
    }

    private static void testRotationNormalization() {
        final int fullCircle = 411775;

        Transform3D transform = new Transform3D(0, 0, 0, fullCircle - 1);
        transform.applyMovement(0, 0, 0, 2);
        assertEquals("single positive wrap", 1, transform.rotation);

        transform.setPosition(0, 0, 0, 5);
        transform.applyMovement(0, 0, 0, fullCircle * 5 + 7);
        assertEquals("multiple positive wraps", 12, transform.rotation);

        transform.setPosition(0, 0, 0, 5);
        transform.applyMovement(0, 0, 0, -fullCircle * 5 - 7);
        assertEquals("multiple negative wraps", fullCircle - 2, transform.rotation);
    }

    private static void testPaletteLighting() {
        // Warm, highlight-rich color: it makes a hard additive clip obvious.
        int[][] palettes = Texture.createColorPalettes(new int[]{0x00dc6e28});
        assertEquals("palette rows", 16, palettes.length);
        assertEquals("neutral authored color", 0xffdc6e28, palettes[8][0]);
        assertEquals("linear shadow exposure", 0xff4e270e, palettes[0][0]);
        assertEquals("soft highlight rolloff", 0xfff5b04d, palettes[15][0]);

        assertTrue("shadow is darker", (palettes[0][0] & 0x00ffffff)
                < (palettes[8][0] & 0x00ffffff));
        assertTrue("highlight preserves red detail", ((palettes[15][0] >> 16) & 0xff) < 255);
    }

    private static void testAllShippedLevels() {
        MathUtils.initializeMathTables();
        LevelLoader.initResourceArrays();

        for (int levelIndex = 0; levelIndex < LEVEL_FILES.length; ++levelIndex) {
            String levelName = LEVEL_FILES[levelIndex];
            LevelLoader.levelVariant = 0;

            assertTrue("map " + levelName, LevelLoader.loadMapData(
                    "/gamedata/levels/level_" + levelName, true));
            GameWorld world = LevelLoader.gameWorld;
            assertTrue("world " + levelName, world != null && world.sectors != null
                    && world.sectors.length > 0 && world.bspNodes.length > 0);

            // This follows the normal map setup path without constructing a
            // Canvas. It catches wrong segment field order and invalid BSP links.
            world.initializeWorld();

            // The regular level manager preloads the object sprites it needs.
            // Preload the complete negative-ID range here so every shipped SP
            // atlas entry and every palette path is covered by the smoke test.
            for (int textureId = -127; textureId < 0; ++textureId) {
                LevelLoader.preloadTexture((byte)textureId);
            }

            assertTrue("assets " + levelName, LevelLoader.loadGameAssets(
                    "/gamedata/textures/tx", 4,
                    "/gamedata/textures/sp", 4));
            assertTexturesHavePalettes(levelName);
            assertSectorSpritesHavePalettes(world, levelName);
        }
    }

    private static void assertTexturesHavePalettes(String levelName) {
        for (int index = 0; index < LevelLoader.textureTable.length; ++index) {
            Texture texture = LevelLoader.textureTable[index];
            if (texture != null && texture.width > 0) {
                assertTrue("texture pixels " + levelName + "/" + index,
                        texture.pixelData != null);
                assertTrue("texture palette " + levelName + "/" + index,
                        texture.colorPalettes != null && texture.colorPalettes.length == 16);
            }
        }
    }

    private static void assertSectorSpritesHavePalettes(GameWorld world, String levelName) {
        for (int index = 0; index < world.sectors.length; ++index) {
            SectorData sector = world.sectors[index];
            if (sector.floorTextureId != 0 && sector.floorTextureId != 51) {
                assertTrue("floor sprite " + levelName + "/" + index,
                        sector.floorTexture != null && sector.floorTexture.colorPalettes != null);
            }
            if (sector.ceilingTextureId != 0 && sector.ceilingTextureId != 51) {
                assertTrue("ceiling sprite " + levelName + "/" + index,
                        sector.ceilingTexture != null && sector.ceilingTexture.colorPalettes != null);
            }
        }
    }

    private static void assertTrue(String name, boolean value) {
        if (!value) {
            throw new RuntimeException("Assertion failed: " + name);
        }
    }

    private static void assertEquals(String name, int expected, int actual) {
        if (expected != actual) {
            throw new RuntimeException("Assertion failed: " + name
                    + ", expected=" + expected + ", actual=" + actual);
        }
    }

    /** Input stream that has data but deliberately does not implement skip(). */
    private static final class NonSkippingInputStream extends InputStream {
        private final byte[] data;
        private int position;

        NonSkippingInputStream(byte[] data) {
            this.data = data;
        }

        public int read() {
            if (position >= data.length) {
                return -1;
            }
            return data[position++] & 0xff;
        }

        public long skip(long count) throws IOException {
            return 0L;
        }
    }
}
