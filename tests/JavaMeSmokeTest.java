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
        testLanguageSwitch();
        testExternalMaterials();
        testExternalEntities();
        testDoomLoadout();
        testC3BLevel();
        testC3BFrameRenders();
        testDoomE1M1Level();
        testRendererFastPaths();
        testFlatSpriteColors();
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

    private static void testLanguageSwitch() {
        TextStrings.setLanguage(TextStrings.LANGUAGE_RUSSIAN);
        assertEquals("russian menu", "новая игра", TextStrings.mainMenuItems[0]);
        assertEquals("russian font config", "/gamedata/font/ru_font.txt",
                TextStrings.getFontConfigPath());
        assertEquals("russian language name", "русский", TextStrings.getLanguageName());

        TextStrings.setLanguage(TextStrings.LANGUAGE_ENGLISH);
        assertEquals("english menu", "new game", TextStrings.mainMenuItems[0]);
        assertEquals("english font config", "/gamedata/font/en_font.txt",
                TextStrings.getFontConfigPath());

        // Keep the test process aligned with the application's first-run default.
        TextStrings.setLanguage(TextStrings.LANGUAGE_RUSSIAN);
    }

    private static void testExternalMaterials() throws Exception {
        LevelLoader.initResourceArrays();
        CustomMaterialSet materials = CustomMaterialSet.load("/gamedata/custom/demo/materials.c3m");
        assertEquals("custom wall material count", 1, materials.getWallTextureCount());
        assertEquals("custom flat material count", 1, materials.getFlatTextureCount());

        Texture wall = materials.getWallTexture(1);
        assertTrue("custom wall loaded", wall != null && wall.width == 64 && wall.height == 128
                && wall.pixelData != null && wall.colorPalettes != null);
        Sprite flat = materials.getFlatTexture(1);
        assertTrue("custom flat loaded", flat != null && flat.pixelData != null
                && flat.colorPalettes != null && flat.flatColors != null);
        Texture sky = materials.getSkyTexture();
        assertTrue("custom sky loaded", sky != null && sky.width == 64 && sky.height == 128);

        materials.installWallTextures();
        assertTrue("custom wall installed", LevelLoader.getTexture((byte)1) == wall);
        materials.installSkyTexture();
    }

    private static void testExternalEntities() throws Exception {
        CustomEntitySet entities = CustomEntitySet.load("/gamedata/custom/demo/entities.ini");
        assertEquals("custom entity count", 1, entities.getEntityCount());
    }

    private static void testDoomLoadout() {
        DoomGameMode.setActive(true);
        DoomGameMode.configurePlayerLoadout(null);
        for (int i = 0; i < WeaponFactory.WEAPON_COUNT; ++i) {
            assertTrue("Doom weapon available " + i, GameEngine.weaponsAvailable[i]);
        }
        assertEquals("Doom current weapon", WeaponFactory.PISTOL, GameEngine.currentWeapon);
        assertEquals("Doom bullets", 200, GameEngine.ammoCounts[WeaponFactory.PISTOL]);
        assertEquals("Doom shells", 50, GameEngine.ammoCounts[WeaponFactory.SHOTGUN]);
    }

    private static void testC3BLevel() {
        MathUtils.initializeMathTables();
        assertTrue("C3B level load", CustomLevelLoader.load(
                "/gamedata/custom/demo/level.c3b", true));
        GameWorld world = LevelLoader.gameWorld;
        assertTrue("C3B world", world != null && world.vertices.length == 4
                && world.bspNodes.length == 1 && world.bspSectors.length == 2);
        world.initializeWorld();
        assertTrue("C3B external spawn", world.worldOrigin != null
                && world.worldOrigin.x == 0 && world.worldOrigin.z == 0);
        assertEquals("C3B external static objects", 0, world.staticObjects.length);
        assertTrue("C3B wall material", LevelLoader.getTexture((byte)1).width == 64);
        // PortalRenderer's inherited field names are vertically inverted.
        // CustomLevelLoader adapts exactly once so C3D floor material renders
        // below the horizon and C3D ceiling sky renders above it.
        assertTrue("C3B floor material", world.sectors[0].ceilingTexture != null
                && world.sectors[0].ceilingTexture.flatColors != null);
        assertTrue("C3B ceiling sky", world.sectors[0].floorTexture == null
                && world.sectors[0].floorTextureId == 51);
        assertTrue("C3B explicit leaf sector", world.getRootBSPNode()
                .findSectorAtPoint(0, 0) == world.sectors[0]);
    }

    /**
     * Loads the demo through the actual game setup and renders one frame.
     * This catches C3D wall-winding mistakes: the BSP can still be valid, but
     * PortalRenderer rejects every right-to-left projected front segment and
     * otherwise leaves the frame buffer black.
     */
    private static void testC3BFrameRenders() {
        GameEngine.initializeEngine();
        assertTrue("C3B frame level load", CustomLevelLoader.load(
                "/gamedata/custom/demo/level.c3b", true));
        GameEngine.resetLevelState();

        int playerY = -((GameEngine.currentSector.floorHeight
                + GameWorld.PLAYER_HEIGHT_OFFSET) << 16);
        PortalRenderer.renderWorld(GameEngine.player.x, playerY,
                GameEngine.player.z, GameEngine.player.rotation);

        int coloredPixels = 0;
        for (int i = 0; i < PortalRenderer.screenBuffer.length; ++i) {
            if ((PortalRenderer.screenBuffer[i] & 0x00FFFFFF) != 0) {
                ++coloredPixels;
            }
        }
        assertTrue("C3B frame contains world pixels", coloredPixels > 1024);
    }

    /**
     * Loads the compact WAD conversion rather than the 10 MB source WAD. It
     * exercises a real-sized custom map: composite Doom wall BMPs, flats,
     * explicit BSP leaf sectors and player starts from entities.ini.
     */
    private static void testDoomE1M1Level() {
        LevelLoader.levelVariant = 0;
        assertTrue("Doom E1M1 C3B load", CustomLevelLoader.load(
                "/gamedata/custom/doom-e1m1/level.c3b", true));
        GameWorld world = LevelLoader.gameWorld;
        assertTrue("Doom E1M1 geometry", world != null && world.wallDefinitions.length == 452
                && world.sectors.length == 83 && world.bspNodes.length > 100);
        assertTrue("Doom E1M1 spawn", world.worldOrigin != null);
        assertEquals("Doom E1M1 enemy count", 29, world.staticObjects.length);
        int doomDoorWalls = 0;
        for (int i = 0; i < world.wallDefinitions.length; ++i) {
            if (world.wallDefinitions[i].getWallType() == 1) ++doomDoorWalls;
        }
        assertEquals("Doom E1M1 door triggers", 8, doomDoorWalls);
        world.initializeWorld();
        int doomVisiblePairs = 0;
        for (int sector = 0; sector < world.sectors.length; ++sector) {
            assertTrue("Doom E1M1 PVS diagonal", !world.sectors[sector].visitedFlags[sector]);
            for (int from = 0; from < world.sectors.length; ++from) {
                if (!world.sectors[sector].visitedFlags[from]) ++doomVisiblePairs;
            }
        }
        assertEquals("Doom E1M1 symmetric reject PVS", 6021, doomVisiblePairs);
        assertTrue("Doom E1M1 wall texture", LevelLoader.getTexture((byte)1) != null
                && LevelLoader.getTexture((byte)1).pixelData != null);
        assertTrue("Doom E1M1 enemy sprite texture", LevelLoader.getTexture((byte)-1) != null
                && LevelLoader.getTexture((byte)-1).pixelData != null);
        assertTrue("Doom E1M1 enemy sprite frame", world.staticObjects[0]
                .getCurrentLowerBodySpriteId() != 0);
        assertTrue("Doom E1M1 flat texture", world.sectors[0].ceilingTexture != null
                && world.sectors[0].ceilingTexture.flatColors != null);
        assertTrue("Doom E1M1 spawn sector", world.getSectorDataAtPoint(
                world.worldOrigin.x, world.worldOrigin.z) != null);
    }

    /** Verifies the MascotME-inspired bulk clear and unrolled opaque-flat path. */
    private static void testRendererFastPaths() {
        Sector.resetClipArrays();
        Sector.floorClip[7] = 123;
        Sector.ceilingClip[7] = 0;
        Sector.resetClipArrays();
        assertEquals("clip floor reset", 0, Sector.floorClip[7]);
        assertEquals("clip ceiling reset", PortalRenderer.MAX_VIEWPORT_Y, Sector.ceilingClip[7]);

        int viewportWidth = PortalRenderer.VIEWPORT_WIDTH;
        PortalRenderer.screenBuffer = new int[PortalRenderer.SCREEN_BUFFER_SIZE];
        PortalRenderer.reciprocalTable = new int[PortalRenderer.VIEWPORT_HEIGHT + 1];
        PortalRenderer.angleCorrectionTable = new int[viewportWidth];
        for (int i = 1; i < PortalRenderer.reciprocalTable.length; ++i) {
            PortalRenderer.reciprocalTable[i] = 65536 / i;
        }
        for (int i = 0; i < viewportWidth; ++i) {
            PortalRenderer.angleCorrectionTable[i] = (i - PortalRenderer.HALF_VIEWPORT_WIDTH) << 8;
        }
        PortalRenderer.gunFireLighting = false;

        byte[] pixels = new byte[4096];
        for (int i = 0; i < pixels.length; ++i) {
            pixels[i] = (byte)(i & 15);
        }
        int[][] palettes = new int[16][16];
        for (int light = 0; light < palettes.length; ++light) {
            for (int color = 0; color < palettes[light].length; ++color) {
                palettes[light][color] = 0xff000000 | (light << 16) | color;
            }
        }

        int startColumn = 5;
        int endColumn = 230;
        int row = 200;
        int sinAngle = 42000;
        int cosAngle = 49000;
        int cameraX = 123456;
        int heightOffset = 1310720;
        int cameraZ = -654321;
        PortalRenderer.drawFlatSurface(startColumn, endColumn, row, pixels, palettes,
                8, sinAngle, cosAngle, cameraX, heightOffset, cameraZ);

        int rowFromCenter = row - PortalRenderer.HALF_VIEWPORT_HEIGHT;
        int perspective = PortalRenderer.reciprocalTable[rowFromCenter];
        int scaledPerspective = (heightOffset * perspective) >> 8;
        int effectiveLight = 8 - (scaledPerspective >> 14);
        if (effectiveLight < 0) effectiveLight = 0;
        else if (effectiveLight > 15) effectiveLight = 15;
        int startAngle = PortalRenderer.angleCorrectionTable[startColumn];
        int endAngle = PortalRenderer.angleCorrectionTable[endColumn];
        int textureU = ((sinAngle + (cosAngle * startAngle >> 14)) * scaledPerspective - cameraX) >> 6;
        int textureV = (cosAngle - (sinAngle * startAngle >> 14)) * scaledPerspective - cameraZ;
        int angleDelta = (endAngle - startAngle)
                * PortalRenderer.reciprocalTable[endColumn - startColumn + 1] >> 16;
        int textureStepU = ((cosAngle * angleDelta >> 14) * scaledPerspective) >> 6;
        int textureStepV = (-sinAngle * angleDelta >> 14) * scaledPerspective;
        int rowOffset = row * viewportWidth;

        for (int column = startColumn; column <= endColumn; ++column) {
            int expected = palettes[effectiveLight][pixels[
                    ((textureU & 16515072) + (textureV & 1056964608)) >> 18]];
            assertEquals("opaque flat " + column, expected,
                    PortalRenderer.screenBuffer[rowOffset + column]);
            textureU += textureStepU;
            textureV += textureStepV;
        }

        int[] flatColors = new int[16];
        for (int light = 0; light < flatColors.length; ++light) {
            flatColors[light] = 0xff000000 | (light << 8) | light;
        }
        PortalRenderer.drawFlatColorSpan(startColumn, endColumn, row,
                flatColors, 8, heightOffset);
        for (int column = startColumn; column <= endColumn; ++column) {
            assertEquals("fast flat color " + column, flatColors[effectiveLight],
                    PortalRenderer.screenBuffer[rowOffset + column]);
        }
    }

    private static void testFlatSpriteColors() {
        Sprite sprite = new Sprite((byte)1);
        sprite.pixelData = new byte[]{0, 1, 0, 1};
        sprite.colorPalettes = new int[16][2];
        for (int level = 0; level < sprite.colorPalettes.length; ++level) {
            sprite.colorPalettes[level][0] = 0xff102030;
            sprite.colorPalettes[level][1] = 0xff304050;
        }
        sprite.buildFlatColors();
        assertEquals("flat sprite colors", 16, sprite.flatColors.length);
        assertEquals("flat sprite average", 0xff203040, sprite.flatColors[8]);
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
                        sector.floorTexture != null && sector.floorTexture.colorPalettes != null
                        && sector.floorTexture.flatColors != null);
            }
            if (sector.ceilingTextureId != 0 && sector.ceilingTextureId != 51) {
                assertTrue("ceiling sprite " + levelName + "/" + index,
                        sector.ceilingTexture != null && sector.ceilingTexture.colorPalettes != null
                        && sector.ceilingTexture.flatColors != null);
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

    private static void assertEquals(String name, String expected, String actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
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
