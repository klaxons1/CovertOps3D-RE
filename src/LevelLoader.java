import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Loads level geometry, objects and texture/sprite atlases.
 * File format is custom binary, little-endian for map, big-endian for TX/SP.
 *
 * Responsibilities split:
 * - parse map file (vertices, walls, objects, sectors, BSP)
 * - remap legacy texture IDs
 * - preload texture/sprite references
 * - load external TX/SP atlases and build palettes
 *
 * This class keeps the original loading performance: no extra allocations
 * in hot loops, direct array writes, reusable buffers.
 */
public final class LevelLoader {

    public static int levelVariant = 0;
    public static GameWorld gameWorld = null;

    private static int resourceLoadState; // 0=empty, 1=map loaded, 2=assets loaded
    private static Sprite[] spriteTable;
    static Texture[] textureTable;

    // Palette indexes are dense in the atlas files. Keeping their resolved
    // data in an array avoids Hashtable/Byte/Integer/String allocation while
    // a level is loading on a very small Java ME heap.
    private static int[][][] paletteCache;
    private static final int NO_PALETTE = -1;

    static Texture defaultErrorTexture;

    // Exact file header magics (big-endian short), verified against the shipped atlases:
    // tx1..tx4 all start with 0x9954, sp1..sp4 all start with 0x9953.
    // (The constant names were swapped in the previous implementation - the same wrong
    // code path then parsed TX entries with the SP header layout, which misaligned the
    // stream and sent skipBytes() into an endless loop = infinite "Loading" screen.)
    private static final int MAGIC_TX_ATLAS = 39252; // 0x9954 - wall/floor texture atlases (tx*)
    private static final int MAGIC_SP_ATLAS = 39251; // 0x9953 - object sprite atlases (sp*)

    private LevelLoader() {}

    // ==================== Lookup helpers ====================

    private static Sprite getSprite(byte spriteId) {
        if (spriteId == 51 || spriteId < 0) {
            // guard against negative ids indexing the table (would kill the whole load)
            return null;
        }
        Sprite sprite = spriteTable[spriteId];
        return (sprite != null && sprite.pixelData != null) ? sprite : null;
    }

    static Texture getTexture(byte textureId) {
        if (textureId == 0) {
            return defaultErrorTexture;
        }
        Texture tex = textureTable[textureId + 128];
        return (tex != null && tex.width > 0) ? tex : defaultErrorTexture;
    }

    static void initResourceArrays() {
        spriteTable = new Sprite[128];
        textureTable = new Texture[256];
        paletteCache = null;
    }

    private static void unloadAllResources() {
        gameWorld = null;
        resourceLoadState = 0;

        for (int i = 0; i < 128; ++i) {
            spriteTable[i] = null;
        }
        for (int i = 0; i < 256; ++i) {
            textureTable[i] = null;
        }
        // Drop all palette references in one operation. This is cheaper than
        // clearing boxed Hashtable entries and makes the previous level
        // collectible before the next one is decoded.
        paletteCache = null;
    }

    // ==================== Map loading ====================

    public static boolean loadMapData(String levelFilePath, boolean shouldLoadObjects) {
        unloadAllResources();

        DataInputStream dataIn = null;
        try {
            InputStream rawStream = LevelLoader.class.getResourceAsStream(levelFilePath);
            if (rawStream == null) {
                throw new IllegalStateException("Missing map: " + levelFilePath);
            }
            dataIn = new DataInputStream(rawStream);

            gameWorld = new GameWorld();
            dataIn.readByte(); // skip version/header byte

            // ---- Vertices ----
            int vertexByteSize = BinaryUtils.readIntLE(dataIn);
            int vertexCount = vertexByteSize / 4;
            Point2D[] vertices = new Point2D[vertexCount];
            for (int i = 0; i < vertexCount; ++i) {
                short rawX = BinaryUtils.readShortLE(dataIn);
                short rawY = BinaryUtils.readShortLE(dataIn);
                vertices[i] = new Point2D(rawX << 16, rawY << 16);
            }
            gameWorld.setVertices(vertices);

            // ---- Wall definitions ----
            int wallDefByteSize = BinaryUtils.readIntLE(dataIn);
            int wallDefCount = wallDefByteSize / 11;
            WallDefinition[] wallDefs = new WallDefinition[wallDefCount];
            for (int i = 0; i < wallDefCount; ++i) {
                short startVertexId = BinaryUtils.readShortLE(dataIn);
                short endVertexId = BinaryUtils.readShortLE(dataIn);
                byte wallFlags = dataIn.readByte();
                byte wallType = dataIn.readByte();
                byte specialType = dataIn.readByte();
                short frontSurfaceId = BinaryUtils.readShortLE(dataIn);
                short backSurfaceId = BinaryUtils.readShortLE(dataIn);
                wallDefs[i] = new WallDefinition(startVertexId, endVertexId,
                        frontSurfaceId, backSurfaceId, wallFlags, wallType, specialType);
            }
            gameWorld.wallDefinitions = wallDefs;

            // ---- Game objects / player starts ----
            int objectsByteSize = BinaryUtils.readIntLE(dataIn);
            if (levelVariant == 0) {
                levelVariant = 1;
            }
            int objectCount = objectsByteSize / 10;
            GameObject[] loadedObjects = shouldLoadObjects ? new GameObject[objectCount] : null;

            for (int objIdx = 0; objIdx < objectCount; ++objIdx) {
                short rawX = BinaryUtils.readShortLE(dataIn);
                short rawZ = BinaryUtils.readShortLE(dataIn);
                short rawAngle = BinaryUtils.readShortLE(dataIn);
                short objectType = BinaryUtils.readShortLE(dataIn);
                short extraParam = BinaryUtils.readShortLE(dataIn);

                // Types 1..4 are player spawn points for different difficulty/variant
                if (objectType >= 1 && objectType <= 4) {
                    if (levelVariant == objectType) {
                        int worldX = rawX << 16;
                        int worldZ = rawZ << 16;
                        int worldAngle = -rawAngle * 1144 + 102943;
                        gameWorld.worldOrigin = new Transform3D(worldX, 0, worldZ, worldAngle);
                    }
                    if (shouldLoadObjects) {
                        loadedObjects[objIdx] = null; // spawn not a real object
                    }
                } else {
                    if (shouldLoadObjects) {
                        int worldX = rawX << 16;
                        int worldZ = rawZ << 16;
                        int worldAngle = -rawAngle * 1144 + 102943;
                        Transform3D transform = new Transform3D(worldX, 0, worldZ, worldAngle);
                        loadedObjects[objIdx] = new GameObject(transform, rawAngle, objectType, extraParam);
                    }
                }
            }
            if (shouldLoadObjects) {
                gameWorld.staticObjects = loadedObjects;
            }

            // ---- Wall surfaces ----
            int wallSurfByteSize = BinaryUtils.readIntLE(dataIn);
            int wallSurfCount = wallSurfByteSize / 8;
            WallSurface[] wallSurfaces = new WallSurface[wallSurfCount];
            for (int i = 0; i < wallSurfCount; ++i) {
                short texOffsetX = BinaryUtils.readShortLE(dataIn);
                short texOffsetY = BinaryUtils.readShortLE(dataIn);
                byte upperTex = remapLegacyTextureId(dataIn.readByte());
                byte lowerTex = remapLegacyTextureId(dataIn.readByte());
                byte mainTex = remapLegacyTextureId(dataIn.readByte());
                byte sectorLinkId = dataIn.readByte();

                wallSurfaces[i] = new WallSurface(upperTex, mainTex, lowerTex, sectorLinkId, texOffsetX, texOffsetY);

                if (upperTex != 0) preloadTexture(upperTex);
                if (lowerTex != 0) preloadTexture(lowerTex);
                if (mainTex != 0) preloadTexture(mainTex);
            }
            gameWorld.wallSurfaces = wallSurfaces;

            // ---- Sector data ----
            int sectorByteSize = BinaryUtils.readIntLE(dataIn);
            int sectorCount = sectorByteSize / 12;
            SectorData[] sectors = new SectorData[sectorCount];
            for (int i = 0; i < sectorCount; ++i) {
                short sectorId = (short)i;
                short floorHeight = BinaryUtils.readShortLE(dataIn);
                short ceilHeight = BinaryUtils.readShortLE(dataIn);
                byte ceilTexId = remapLegacyTextureId(dataIn.readByte());
                byte floorTexId = remapLegacyTextureId(dataIn.readByte());
                short packedLight = BinaryUtils.readShortLE(dataIn);
                short tag = BinaryUtils.readShortLE(dataIn);
                short sectorType = BinaryUtils.readShortLE(dataIn);

                short lightLevel = (short)((packedLight >> 4) & 15);
                sectors[i] = new SectorData(sectorId, floorHeight, ceilHeight,
                        ceilTexId, floorTexId, lightLevel, tag, sectorType);

                if (ceilTexId != 0) preloadSprite(ceilTexId);
                if (floorTexId != 0) preloadSprite(floorTexId);
            }
            gameWorld.sectors = sectors;

            // ---- BSP nodes ----
            int bspByteSize = BinaryUtils.readIntLE(dataIn);
            int bspNodeCount = bspByteSize / 12;
            BSPNode[] bspNodes = new BSPNode[bspNodeCount];
            for (int i = 0; i < bspNodeCount; ++i) {
                short splitX = BinaryUtils.readShortLE(dataIn);
                short splitZ = BinaryUtils.readShortLE(dataIn);
                short normalX = BinaryUtils.readShortLE(dataIn);
                short splitDy = BinaryUtils.readShortLE(dataIn);
                int frontChild = BinaryUtils.readShortLE(dataIn) & 0xFFFF;
                int backChild = BinaryUtils.readShortLE(dataIn) & 0xFFFF;
                bspNodes[i] = new BSPNode(splitX << 16, splitZ << 16,
                        normalX << 16, splitDy << 16, frontChild, backChild);
            }
            gameWorld.bspNodes = bspNodes;

            // ---- BSP sectors (leaves) ----
            int leafByteSize = BinaryUtils.readIntLE(dataIn);
            int leafCount = leafByteSize / 4;
            Sector[] bspSectors = new Sector[leafCount];
            for (int i = 0; i < leafCount; ++i) {
                short wallCount = BinaryUtils.readShortLE(dataIn);
                short wallOffset = BinaryUtils.readShortLE(dataIn);
                bspSectors[i] = new Sector(wallCount, wallOffset);
            }
            gameWorld.bspSectors = bspSectors;
            BSPNode.visibleSectorsList = new Sector[leafCount];

            // ---- Wall segments ----
            int segByteSize = BinaryUtils.readIntLE(dataIn);
            int segCount = segByteSize / 9;
            WallSegment[] wallSegments = new WallSegment[segCount];
            for (int i = 0; i < segCount; ++i) {
                short startVertex = BinaryUtils.readShortLE(dataIn);
                short endVertex = BinaryUtils.readShortLE(dataIn);
                // On disk the definition index precedes the facing byte, and
                // the texture offset comes last (the old local names were
                // reversed, although the constructor arguments happened to
                // put both values in the right fields).
                short wallDefIdx = BinaryUtils.readShortLE(dataIn);
                boolean isFrontFacing = dataIn.readByte() == 0;
                short texOffset = BinaryUtils.readShortLE(dataIn);
                wallSegments[i] = new WallSegment(startVertex, endVertex,
                        wallDefIdx, isFrontFacing, texOffset);
            }
            gameWorld.wallSegments = wallSegments;

            // ---- PVS (potential visibility set) bit matrix ----
            BinaryUtils.readIntLE(dataIn); // unused header
            int sectorTotal = gameWorld.sectors.length;
            boolean[][] visibilityMatrix = new boolean[sectorTotal][sectorTotal];
            int currentRow = 0;
            int currentCol = 0;
            while (currentCol < sectorTotal) {
                int bitPack = dataIn.readByte() & 255;
                for (int bit = 0; bit < 8 && currentCol < sectorTotal; ++bit) {
                    visibilityMatrix[currentRow][currentCol] = (bitPack & (1 << bit)) == (1 << bit);
                    ++currentRow;
                    if (currentRow >= sectorTotal) {
                        currentRow = 0;
                        ++currentCol;
                    }
                }
            }
            for (int i = 0; i < sectorTotal; ++i) {
                gameWorld.sectors[i].visitedFlags = visibilityMatrix[i];
            }

        } catch (Exception loadEx) {
            DebugLogger.logException("LevelLoader.loadMapData", loadEx);
            return false;
        } catch (OutOfMemoryError oom) {
            DebugLogger.logOutOfMemory("LevelLoader.loadMapData", oom);
            return false;
        } finally {
            closeDataInputStream(dataIn);
        }

        resourceLoadState = 1;
        return true;
    }

    // ==================== Legacy texture remap ====================

    private static byte remapLegacyTextureId(byte originalId) {
        // Returns possibly same ID, but for certain legacy IDs also preloads the replacement
        // Keeps original observable behavior (double texture composite cases)
        byte remappedReplacement;
        switch (originalId) {
            case 6:
                return 5;
            case 7: case 8: case 9: case 10: case 11: case 12: case 13: case 14: case 15:
            case 16: case 17: case 24: case 25: case 26: case 27: case 28: case 29:
            case 30: case 31: case 32: case 33: case 34: case 35: case 36: case 37:
            case 38: case 41: case 44: case 45: case 46: case 49:
                return originalId;
            case 18: case 19: case 20: case 21: case 22: case 23:
                return 17;
            case 39: case 40:
                remappedReplacement = 35;
                break;
            case 42: case 43:
                return 41;
            case 47: case 48:
                remappedReplacement = 46;
                break;
            case 50:
                remappedReplacement = 49;
                break;
            default:
                return originalId;
        }
        preloadTexture(remappedReplacement);
        return originalId;
    }

    // ==================== TX/SP atlas loading ====================

    public static boolean loadGameAssets(String texturePathPrefix, int textureFileCount,
                                         String spritePathPrefix, int spriteFileCount) {
        if (resourceLoadState < 1) throw new IllegalStateException("Map not loaded");
        if (resourceLoadState > 1) throw new IllegalStateException("Assets already loaded");

        try {
            int globalPaletteIndex = 0;
            int[] spritePaletteIndexes = createPaletteIndexArray(spriteTable.length);
            int[] texturePaletteIndexes = createPaletteIndexArray(textureTable.length);
            paletteCache = null;

            // ---- Load texture atlases (TX) ----
            for (int fileNumber = 1; fileNumber <= textureFileCount; ++fileNumber) {
                String fullPath = texturePathPrefix + Integer.toString(fileNumber);
                InputStream fileStream = LevelLoader.class.getResourceAsStream(fullPath);
                if (fileStream == null) throw new IllegalStateException("Missing TX: " + fullPath);

                DataInputStream dataIn = new DataInputStream(fileStream);
                try {
                    int magic = BinaryUtils.readShortBE(dataIn) & 0xFFFF;
                    if (magic != MAGIC_TX_ATLAS) throw new IllegalStateException("Bad magic TX: " + magic);

                    int textureEntryCount = BinaryUtils.readShortBE(dataIn) & 0xFFFF;
                    int paletteEntryCount = BinaryUtils.readShortBE(dataIn) & 0xFFFF;
                    BinaryUtils.readIntBE(dataIn); // pixel-data offset; entries are read sequentially
                    boolean[] referencedPalettes = new boolean[paletteEntryCount];

                    // TX entries have a 9-byte header: id, width, height,
                    // paletteOffset, bitDepth. Anchor offsets exist only in SP.
                    for (int entryIdx = 0; entryIdx < textureEntryCount; ++entryIdx) {
                        byte rawId = dataIn.readByte();
                        int width = BinaryUtils.readShortBE(dataIn) & 0xFFFF;
                        int height = BinaryUtils.readShortBE(dataIn) & 0xFFFF;
                        int paletteOffset = BinaryUtils.readShortBE(dataIn) & 0xFFFF;
                        int bitDepth = BinaryUtils.readShortBE(dataIn) & 0xFFFF;
                        int pixelCount = getPixelCount(width, height, fullPath);
                        int packedBytes = getPackedByteCount(pixelCount, bitDepth, fullPath);
                        int paletteIndex = getPaletteIndex(globalPaletteIndex, paletteOffset,
                                paletteEntryCount, fullPath);

                        if (isSpriteRegistered(rawId)) {
                            // Positive-id 64x64 floor/ceiling sprites live in TX.
                            if (width != 64 || height != 64) {
                                throw new IllegalStateException("Sprite must be 64x64");
                            }

                            byte[] packedData = new byte[packedBytes];
                            byte[] unpacked = new byte[pixelCount];
                            dataIn.readFully(packedData, 0, packedBytes);
                            decompressSprite(packedData, 0, unpacked, 0, pixelCount, bitDepth);

                            spriteTable[rawId] = new Sprite(rawId, unpacked);
                            spritePaletteIndexes[rawId] = paletteIndex;
                            referencedPalettes[paletteOffset] = true;
                        } else if (isTextureRegistered(rawId)) {
                            Texture tex = new Texture(rawId, width, height, 0, 0);
                            byte[] packedData = new byte[packedBytes];
                            dataIn.readFully(packedData, 0, packedBytes);

                            byte[] rowBuffer = null;
                            for (int x = 0; x < width; ++x) {
                                if ((x & 1) == 0) rowBuffer = new byte[height];
                                decompressTexture(packedData, x * height, rowBuffer, 0,
                                        height, bitDepth, x & 1);
                                tex.setPixelData(x, rowBuffer);
                            }
                            textureTable[rawId + 128] = tex;
                            texturePaletteIndexes[rawId + 128] = paletteIndex;
                            referencedPalettes[paletteOffset] = true;
                        } else {
                            BinaryUtils.skipBytes(dataIn, packedBytes);
                        }
                    }

                    readReferencedPalettes(dataIn, referencedPalettes, globalPaletteIndex);
                    globalPaletteIndex += paletteEntryCount;
                    DebugLogger.log("LevelLoader", "atlas ok " + fullPath + " entries=" + textureEntryCount);
                } finally {
                    closeDataInputStream(dataIn);
                }
            }

            // ---- Load object-sprite atlases (SP) ----
            for (int fileNumber = 1; fileNumber <= spriteFileCount; ++fileNumber) {
                String fullPath = spritePathPrefix + Integer.toString(fileNumber);
                InputStream fileStream = LevelLoader.class.getResourceAsStream(fullPath);
                if (fileStream == null) throw new IllegalStateException("Missing SP: " + fullPath);

                DataInputStream dataIn = new DataInputStream(fileStream);
                try {
                    int magic = BinaryUtils.readShortBE(dataIn) & 0xFFFF;
                    if (magic != MAGIC_SP_ATLAS) throw new IllegalStateException("Bad magic SP: " + magic);

                    int spriteEntryCount = BinaryUtils.readShortBE(dataIn) & 0xFFFF;
                    int paletteEntryCount = BinaryUtils.readShortBE(dataIn) & 0xFFFF;
                    BinaryUtils.readIntBE(dataIn); // pixel-data offset; entries are read sequentially
                    boolean[] referencedPalettes = new boolean[paletteEntryCount];

                    // SP entries have a 13-byte header: id, width, height,
                    // hOffset, vOffset, paletteOffset, bitDepth.
                    for (int entryIdx = 0; entryIdx < spriteEntryCount; ++entryIdx) {
                        byte rawId = dataIn.readByte();
                        int width = BinaryUtils.readShortBE(dataIn) & 0xFFFF;
                        int height = BinaryUtils.readShortBE(dataIn) & 0xFFFF;
                        short hOffset = BinaryUtils.readShortBE(dataIn);
                        short vOffset = BinaryUtils.readShortBE(dataIn);
                        int paletteOffset = BinaryUtils.readShortBE(dataIn) & 0xFFFF;
                        int bitDepth = BinaryUtils.readShortBE(dataIn) & 0xFFFF;
                        int pixelCount = getPixelCount(width, height, fullPath);
                        int packedBytes = getPackedByteCount(pixelCount, bitDepth, fullPath);
                        int paletteIndex = getPaletteIndex(globalPaletteIndex, paletteOffset,
                                paletteEntryCount, fullPath);

                        if (isSpriteRegistered(rawId)) {
                            if (width != 64 || height != 64) {
                                throw new IllegalStateException("Sprite must be 64x64");
                            }

                            byte[] packedData = new byte[packedBytes];
                            byte[] unpacked = new byte[pixelCount];
                            dataIn.readFully(packedData, 0, packedBytes);
                            decompressSprite(packedData, 0, unpacked, 0, pixelCount, bitDepth);

                            spriteTable[rawId] = new Sprite(rawId, unpacked);
                            spritePaletteIndexes[rawId] = paletteIndex;
                            referencedPalettes[paletteOffset] = true;
                        } else if (isTextureRegistered(rawId)) {
                            // Negative-id object/enemy sprite textures keep their
                            // header anchors; PortalRenderer uses them for billboards.
                            Texture tex = new Texture(rawId, width, height, hOffset, vOffset);
                            byte[] packedData = new byte[packedBytes];
                            dataIn.readFully(packedData, 0, packedBytes);
                            byte[] rowBuffer = null;
                            for (int x = 0; x < width; ++x) {
                                if ((x & 1) == 0) rowBuffer = new byte[height];
                                decompressTexture(packedData, x * height, rowBuffer, 0,
                                        height, bitDepth, x & 1);
                                tex.setPixelData(x, rowBuffer);
                            }
                            textureTable[rawId + 128] = tex;
                            texturePaletteIndexes[rawId + 128] = paletteIndex;
                            referencedPalettes[paletteOffset] = true;
                        } else {
                            BinaryUtils.skipBytes(dataIn, packedBytes);
                        }
                    }

                    readReferencedPalettes(dataIn, referencedPalettes, globalPaletteIndex);
                    globalPaletteIndex += paletteEntryCount;
                    DebugLogger.log("LevelLoader", "atlas ok " + fullPath + " entries=" + spriteEntryCount);
                } finally {
                    closeDataInputStream(dataIn);
                }
            }

            // ---- Link sprites to their palettes ----
            for (int spriteId = 0; spriteId < spriteTable.length; ++spriteId) {
                Sprite spr = spriteTable[spriteId];
                if (spr != null) {
                    if (spr.pixelData == null) {
                        throw new IllegalStateException("Sprite without pixels: " + spriteId);
                    }
                    spr.colorPalettes = getResolvedPalette(spritePaletteIndexes[spriteId],
                            "Sprite", spriteId);
                    spr.buildFlatColors();
                }
            }

            // ---- Composite special textures (overlays) ----
            byte[][] base35Pixels = null, base46Pixels = null, base49Pixels = null;
            Texture tmp;
            if ((tmp = textureTable[163]) != null) base35Pixels = tmp.pixelData;
            if ((tmp = textureTable[174]) != null) base46Pixels = tmp.pixelData;
            if ((tmp = textureTable[177]) != null) base49Pixels = tmp.pixelData;

            for (int texIdx = 0; texIdx < textureTable.length; ++texIdx) {
                Texture tex = textureTable[texIdx];
                if (tex == null) continue;

                byte texType = tex.textureType;
                byte compositeBaseType = 0;
                boolean hasComposite = true;

                switch (texType) {
                    case 35:
                        tex.compositeTexture(base35Pixels, 0, 0, 64, 128, 0, 0, true);
                        hasComposite = false;
                        break;
                    case 39:
                        tex.compositeTexture(base35Pixels, 0, 0, 64, 128, 0, 0, true);
                        tex.compositeTexture(base35Pixels, 0, 128, 64, 18, 0, 0, false);
                        tex.compositeTexture(base35Pixels, 64, 17, 10, 111, 54, 17, false);
                        compositeBaseType = 35;
                        break;
                    case 40:
                        tex.compositeTexture(base35Pixels, 0, 0, 64, 128, 0, 0, true);
                        tex.compositeTexture(base35Pixels, 0, 128, 64, 18, 0, 0, false);
                        tex.compositeTexture(base35Pixels, 74, 17, 10, 111, 0, 17, false);
                        compositeBaseType = 35;
                        break;
                    case 46:
                        tex.compositeTexture(base46Pixels, 0, 0, 64, 128, 0, 0, true);
                        hasComposite = false;
                        break;
                    case 47:
                        tex.compositeTexture(base46Pixels, 0, 0, 64, 128, 0, 0, true);
                        tex.compositeTexture(base46Pixels, 64, 0, 14, 128, 50, 0, false);
                        compositeBaseType = 46;
                        break;
                    case 48:
                        tex.compositeTexture(base46Pixels, 0, 0, 64, 128, 0, 0, true);
                        tex.compositeTexture(base46Pixels, 64, 0, 14, 128, 50, 0, false);
                        tex.compositeTexture(base46Pixels, 78, 0, 20, 24, 19, 43, false);
                        compositeBaseType = 46;
                        break;
                    case 49:
                        tex.compositeTexture(base49Pixels, 0, 0, 64, 128, 0, 0, true);
                        hasComposite = false;
                        break;
                    case 50:
                        tex.compositeTexture(base49Pixels, 0, 0, 64, 128, 0, 0, true);
                        tex.compositeTexture(base49Pixels, 64, 0, 20, 27, 25, 19, false);
                        compositeBaseType = 49;
                        break;
                    default:
                        hasComposite = false;
                        break;
                }

                if (hasComposite) {
                    texType = compositeBaseType;
                }

                if (tex.width != 0) {
                    tex.colorPalettes = getResolvedPalette(texturePaletteIndexes[texType + 128],
                            "Texture", texType);
                }
            }

            // ---- Resolve sector floor/ceiling sprite references ----
            for (int i = 0; i < gameWorld.sectors.length; ++i) {
                SectorData sector = gameWorld.sectors[i];
                sector.floorTexture = getSprite(sector.floorTextureId);
                sector.ceilingTexture = getSprite(sector.ceilingTextureId);
            }

        } catch (Exception ex) {
            DebugLogger.logException("LevelLoader.loadGameAssets", ex);
            return false;
        } catch (OutOfMemoryError oom) {
            DebugLogger.logOutOfMemory("LevelLoader.loadGameAssets", oom);
            return false;
        }

        resourceLoadState = 2;
        return true;
    }

    /** Creates a primitive index table initialized to the missing-palette value. */
    private static int[] createPaletteIndexArray(int length) {
        int[] indexes = new int[length];
        for (int i = 0; i < length; ++i) {
            indexes[i] = NO_PALETTE;
        }
        return indexes;
    }

    /** Validates a per-file palette offset and turns it into a global index. */
    private static int getPaletteIndex(int firstPaletteIndex, int paletteOffset,
                                       int paletteCount, String atlasPath) {
        if (paletteOffset >= paletteCount) {
            throw new IllegalStateException("Bad palette offset in " + atlasPath);
        }
        return firstPaletteIndex + paletteOffset;
    }

    /** Checks dimensions before allocating unpacked pixels or skipping packed data. */
    private static int getPixelCount(int width, int height, String atlasPath) {
        long pixelCount = (long)width * (long)height;
        if (width <= 0 || height <= 0 || pixelCount > Integer.MAX_VALUE) {
            throw new IllegalStateException("Bad dimensions in " + atlasPath);
        }
        return (int)pixelCount;
    }

    private static int getPackedByteCount(int pixelCount, int bitDepth, String atlasPath) {
        long bitCount = (long)pixelCount * (long)bitDepth;
        if (bitDepth <= 0 || bitCount > (long)Integer.MAX_VALUE * 8L) {
            throw new IllegalStateException("Bad bit depth in " + atlasPath);
        }
        return (int)((bitCount + 7L) >> 3);
    }

    /**
     * Reads only the palettes used by preloaded resources in the current
     * atlas. The reference bitmap replaces a Hashtable membership lookup for
     * each palette and avoids all boxed keys in the loading path.
     */
    private static void readReferencedPalettes(DataInputStream dataIn,
                                               boolean[] referencedPalettes,
                                               int firstPaletteIndex) throws IOException {
        ensurePaletteCacheCapacity(firstPaletteIndex + referencedPalettes.length);

        for (int paletteOffset = 0; paletteOffset < referencedPalettes.length; ++paletteOffset) {
            int colorCount = BinaryUtils.readIntBE(dataIn);
            if (colorCount < 0 || colorCount > Integer.MAX_VALUE / 4) {
                throw new IllegalStateException("Bad palette length");
            }

            if (!referencedPalettes[paletteOffset]) {
                BinaryUtils.skipBytes(dataIn, colorCount * 4);
            } else {
                int[] colors = new int[colorCount];
                for (int color = 0; color < colorCount; ++color) {
                    colors[color] = BinaryUtils.readIntBE(dataIn);
                }
                paletteCache[firstPaletteIndex + paletteOffset] = Texture.createColorPalettes(colors);
            }
        }
    }

    private static void ensurePaletteCacheCapacity(int requiredLength) {
        if (requiredLength <= 0) return;
        if (paletteCache != null && paletteCache.length >= requiredLength) return;

        int newLength = paletteCache == null ? 16 : paletteCache.length;
        while (newLength < requiredLength) {
            newLength <<= 1;
        }

        int[][][] expanded = new int[newLength][][];
        if (paletteCache != null) {
            System.arraycopy(paletteCache, 0, expanded, 0, paletteCache.length);
        }
        paletteCache = expanded;
    }

    private static int[][] getResolvedPalette(int paletteIndex, String resourceType, int resourceId) {
        if (paletteIndex < 0 || paletteCache == null
                || paletteIndex >= paletteCache.length || paletteCache[paletteIndex] == null) {
            throw new IllegalStateException(resourceType + " without palette: " + resourceId);
        }
        return paletteCache[paletteIndex];
    }

    private static void closeDataInputStream(DataInputStream dataIn) {
        if (dataIn == null) return;
        try {
            dataIn.close();
        } catch (IOException ignored) {
            // A resource stream has no useful recovery path at this point.
        }
    }

    public static void preloadTexture(byte textureId) {
        if (!isTextureRegistered(textureId)) {
            textureTable[textureId + 128] = new Texture(textureId, 0, 0, 0, 0);
        }
    }

    private static void preloadSprite(byte spriteId) {
        if (spriteId != 51 && !isSpriteRegistered(spriteId)) {
            spriteTable[spriteId] = new Sprite(spriteId);
        }
    }

    private static boolean isSpriteRegistered(int spriteId) {
        return spriteId >= 0 && spriteId < 128 && spriteTable[spriteId] != null;
    }

    private static boolean isTextureRegistered(int textureId) {
        int tableIdx = textureId + 128;
        return tableIdx >= 0 && tableIdx < 256 && textureTable[tableIdx] != null;
    }

    /**
     * Decompresses packed palette indices from MSB-first bitstream.
     * Allocation-free core loop, identical to original for compatibility.
     */
    public static void decompressSprite(byte[] packedData, int startPixelIndex,
                                        byte[] outputIndices, int outputOffset,
                                        int pixelCount, int bitsPerPixel) {
        int totalStartBits = startPixelIndex * bitsPerPixel;
        int currentByteIndex = totalStartBits / 8;
        int currentBitOffset = totalStartBits % 8;
        final int pixelMask = (1 << bitsPerPixel) - 1;
        final int outputEnd = outputOffset + pixelCount;

        for (int dst = outputOffset; dst < outputEnd; ++dst) {
            byte currentByte = packedData[currentByteIndex];
            int extracted;
            int shiftRight = 8 - (bitsPerPixel + currentBitOffset);
            if (shiftRight >= 0) {
                extracted = currentByte >> shiftRight;
            } else {
                int bitsFromNext = -shiftRight;
                extracted = currentByte << bitsFromNext;
                extracted |= (packedData[currentByteIndex + 1] & 0xFF) >> (8 - bitsFromNext);
            }
            if ((currentBitOffset += bitsPerPixel) > 7) {
                currentBitOffset -= 8;
                currentByteIndex++;
            }
            outputIndices[dst] = (byte)(extracted & pixelMask);
        }
    }

    /**
     * Decompresses texture data where destination byte stores two nibbles in two passes.
     * Mode 0 = high nibble, Mode 1 = low nibble combined with existing high.
     */
    private static void decompressTexture(byte[] packedData, int startPixelIndex,
                                          byte[] outputIndices, int outputOffset,
                                          int pixelCount, int bitsPerPixel,
                                          int texturePartMode) {
        int totalStartBits = startPixelIndex * bitsPerPixel;
        int currentByteIndex = totalStartBits / 8;
        int currentBitOffset = totalStartBits % 8;
        final int pixelMask = (1 << bitsPerPixel) - 1;
        final int outputEnd = outputOffset + pixelCount;

        for (int dst = outputOffset; dst < outputEnd; ++dst) {
            byte currentByte = packedData[currentByteIndex];
            int extracted;
            int shiftRight = 8 - (bitsPerPixel + currentBitOffset);
            if (shiftRight >= 0) {
                extracted = currentByte >> shiftRight;
            } else {
                int bitsFromNext = -shiftRight;
                extracted = currentByte << bitsFromNext;
                extracted |= (packedData[currentByteIndex + 1] & 0xFF) >> (8 - bitsFromNext);
            }
            if ((currentBitOffset += bitsPerPixel) > 7) {
                currentBitOffset -= 8;
                currentByteIndex++;
            }
            int paletteIndex = extracted & pixelMask;
            if (texturePartMode == 0) {
                outputIndices[dst] = (byte)(paletteIndex << 4);
            } else {
                outputIndices[dst] = (byte)(outputIndices[dst] | paletteIndex);
            }
        }
    }
}
