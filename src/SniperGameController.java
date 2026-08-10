import java.io.DataInputStream;
import java.io.InputStream;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * Handles sniper minigame loop, rendering and logic.
 * Extracted from MainGameCanvas to isolate sniper-specific responsibilities.
 * Performance: reuses pre-allocated arrays, avoids per-frame allocations.
 */
public final class SniperGameController {

    private final MainGameCanvas canvas;
    private final SniperMiniGame sniperMiniGame;
    private final FontRenderer fontRenderer;
    private final Image statusBarImage;

    public SniperGameController(MainGameCanvas canvas, SniperMiniGame sniperMiniGame,
                                FontRenderer fontRenderer, Image statusBarImage) {
        this.canvas = canvas;
        this.sniperMiniGame = sniperMiniGame;
        this.fontRenderer = fontRenderer;
        this.statusBarImage = statusBarImage;
    }

    private static int[] loadSpriteRaw(String path, boolean flip) {
        int[] result = null;
        try {
            InputStream stream = (new Object()).getClass().getResourceAsStream(path);
            DataInputStream dataInput = new DataInputStream(stream);
            dataInput.skipBytes(1);
            byte compression = dataInput.readByte();
            short width = dataInput.readShort();
            short height = dataInput.readShort();
            short paletteSize = dataInput.readShort();

            int pixelCount = width * height;
            byte[] pixelData = new byte[pixelCount];

            int compressedSize = dataInput.readInt();
            byte[] compressed = new byte[compressedSize];
            dataInput.readFully(compressed, 0, compressedSize);
            LevelLoader.decompressSprite(compressed, 0, pixelData, 0, pixelCount, compression);

            int[] palette = new int[paletteSize];
            for (int i = 0; i < paletteSize; ++i) {
                int r = dataInput.readByte() & 255;
                int g = dataInput.readByte() & 255;
                int b = dataInput.readByte() & 255;
                palette[i] = r << 16 | g << 8 | b;
            }
            dataInput.close();
            result = new int[pixelCount];
            if (flip) {
                for (int y = 0; y < height; ++y) {
                    for (int x = 0; x < width; ++x) {
                        result[y * width + (width - x - 1)] = palette[pixelData[y * width + x] & 255];
                    }
                }
            } else {
                for (int i = 0; i < pixelCount; ++i) {
                    result[i] = palette[pixelData[i] & 255];
                }
            }
        } catch (Exception e) {
            DebugLogger.logException("SniperGameController.java", e);
        } catch (OutOfMemoryError e) {
            DebugLogger.logOutOfMemory("SniperGameController.java", e);
        }
        return result;
    }

    public int runSniperGame(Graphics graphics, int level) {
        try {
            int bufferSize = PortalRenderer.SCREEN_BUFFER_SIZE;
            byte[] scenePixels = new byte[bufferSize];
            byte[] maskPixels = new byte[bufferSize];
            byte[] sightPixels = new byte[4096];

            int[][] smallSprites = new int[6][];
            int[][] mediumSprites = new int[6][];
            int[][] largeSprites = new int[6][];
            int[][] largeAltSprites = new int[6][];

            sniperMiniGame.resetEnemyCounters();

            int[][] startPositions = new int[][]{
                    {84, 147, 197, 132, 147, 155, 77, 155},
                    {63, 177, 89, 149, 104, 132, 84, 146}
            };
            int[][] endPositions = new int[][]{
                    {147, 84, 164, 147, 132, 160, 155, 77},
                    {75, 162, 149, 89, 132, 104, 90, 152}
            };
            int[][] yPositions = new int[][]{
                    {145, 145, 102, 84, 84, 144, 151, 151},
                    {108, 105, 111, 111, 160, 160, 152, 152}
            };
            int[][] enemySpeedTypes = new int[][]{
                    {1, 1, 1, 0, 0, 2, 2, 2},
                    {1, 1, 1, 1, 1, 1, 1, 1}
            };

            int[] spriteWidths = new int[]{4, 9, 14};
            int[] spriteHeights = new int[]{8, 18, 30};
            int[] hitPoints = new int[]{
                    MainGameCanvas.SNIPER_DAMAGE_SMALL[GameEngine.difficultyLevel],
                    MainGameCanvas.SNIPER_DAMAGE_MEDIUM[GameEngine.difficultyLevel],
                    MainGameCanvas.SNIPER_DAMAGE_LARGE[GameEngine.difficultyLevel]
            };

            int[] screenOffsets = new int[]{0, 0};
            int[][] enemySpriteRefs = new int[8][];
            int[] enemyStates = new int[8];
            int[] enemyTimers = new int[8];
            int[] enemyPositions = new int[8];
            int[] enemyTypes = new int[8];

            for (int i = 0; i < 8; ++i) {
                startPositions[level][i] -= screenOffsets[level];
                endPositions[level][i] -= screenOffsets[level];
                yPositions[level][i] -= screenOffsets[level];
            }

            for (int i = 0; i < 6; ++i) {
                boolean flip = i > 3;
                int spriteNum = flip ? i - 3 : i + 1;
                smallSprites[i] = loadSpriteRaw("/gamedata/sniperminigame/ot8" + spriteNum, flip);
                mediumSprites[i] = loadSpriteRaw("/gamedata/sniperminigame/ot18" + spriteNum, flip);
                largeSprites[i] = loadSpriteRaw("/gamedata/sniperminigame/ot30" + spriteNum, flip);
                largeAltSprites[i] = loadSpriteRaw("/gamedata/sniperminigame/ss30" + spriteNum, flip);
            }

            Image sightImage = Image.createImage("/gamedata/sniperminigame/sight.png");

            sniperMiniGame.loadResources(level, PortalRenderer.VIEWPORT_WIDTH, PortalRenderer.VIEWPORT_HEIGHT,
                    scenePixels, maskPixels, sightPixels);

            sniperMiniGame.initScopePosition();

            int[] freeSlots = new int[8];
            canvas.accumulatedTime = 0L;
            canvas.lastFrameTime = 0L;
            GameEngine.levelTransitionState = 2;

            while (canvas.isGameRunning) {
                if (GameEngine.levelTransitionState == 1) {
                    return -1;
                }

                int menuResult;
                if ((GameEngine.inputRun || GameEngine.inputBack || canvas.isGamePaused)
                        && (menuResult = new MenuSystem(fontRenderer, canvas).showMenuScreen(graphics, false, null)) != 32) {
                    return menuResult;
                }

                long currentTime = System.currentTimeMillis();
                canvas.frameDeltaTime = currentTime - canvas.lastFrameTime;
                canvas.lastFrameTime = currentTime;
                canvas.accumulatedTime += canvas.frameDeltaTime;
                if (canvas.accumulatedTime > 600L) canvas.accumulatedTime = 600L;

                while (canvas.accumulatedTime >= 40L) {
                    ++canvas.frameCounter;
                    if (!sniperMiniGame.updateLogic(enemyStates, enemyTypes, enemyTimers, freeSlots,
                            enemyPositions, startPositions[level], endPositions[level], enemySpeedTypes[level])) {
                        return -1;
                    }
                    canvas.accumulatedTime -= 40L;
                }

                int totalDamage = 0;
                for (int i = 0; i < 8; ++i) {
                    if (enemyStates[i] == 4) {
                        totalDamage += hitPoints[enemySpeedTypes[level][i]];
                    }
                }

                int[] normalPalette = sniperMiniGame.getPaletteGray();
                int[] regularPalette = sniperMiniGame.getPaletteRegular();

                if (totalDamage > 0) {
                    if (totalDamage > MainGameCanvas.SNIPER_DAMAGE_SMALL[GameEngine.difficultyLevel]) {
                        if (totalDamage > MainGameCanvas.SNIPER_DAMAGE_MEDIUM[GameEngine.difficultyLevel]) {
                            HelperUtils.playSound(2, false, 100, 0);
                        } else {
                            HelperUtils.playSound(2, false, 80, 0);
                        }
                    } else {
                        HelperUtils.playSound(2, false, 60, 0);
                    }
                    HelperUtils.vibrateDevice(totalDamage * 10);
                    if (GameEngine.applyDamage(totalDamage)) {
                        return -2;
                    } else {
                        normalPalette = sniperMiniGame.getPaletteGrayRed();
                        regularPalette = sniperMiniGame.getPaletteRedTint();
                    }
                }

                int sightX = sniperMiniGame.getScopePositionX();
                int sightY = sniperMiniGame.getScopePositionY();

                int renderStartY = sightY < 0 ? 0 : sightY;
                int renderEndY = sightY + 64;
                if (renderEndY > PortalRenderer.VIEWPORT_HEIGHT) renderEndY = PortalRenderer.VIEWPORT_HEIGHT;
                int renderStartX = sightX < 0 ? 0 : sightX;
                int renderEndX = sightX + 64;
                if (renderEndX > PortalRenderer.VIEWPORT_WIDTH) renderEndX = PortalRenderer.VIEWPORT_WIDTH;

                for (int y = renderStartY; y < renderEndY; ++y) {
                    int rowStart = renderStartX + PortalRenderer.VIEWPORT_WIDTH * y;
                    int rowEnd = renderEndX + PortalRenderer.VIEWPORT_WIDTH * y;
                    for (int idx = rowStart; idx < rowEnd; ++idx) {
                        PortalRenderer.screenBuffer[idx] = regularPalette[scenePixels[idx] & 255];
                    }
                }

                int activeEnemies = level == 0 ? 6 : 8;
                for (int i = 0; i < activeEnemies; ++i) {
                    if (enemyStates[i] > 0) {
                        enemySpriteRefs[i] = null;
                        int enemyX = enemyPositions[i];
                        int enemyY = yPositions[level][i];
                        int speedType = enemySpeedTypes[level][i];
                        int spriteW = spriteWidths[speedType];
                        int spriteH = spriteHeights[speedType];

                        switch (speedType) {
                            case 0: enemySpriteRefs[i] = smallSprites[enemyStates[i] - 1]; break;
                            case 1: enemySpriteRefs[i] = mediumSprites[enemyStates[i] - 1]; break;
                            case 2: enemySpriteRefs[i] = enemyTypes[i] == 0 ? largeSprites[enemyStates[i] - 1] : largeAltSprites[enemyStates[i] - 1]; break;
                        }

                        PortalRenderer.copyToScreenBuffer(enemySpriteRefs[i], spriteW, spriteH, enemyX, enemyY, totalDamage > 0);

                        if (enemyStates[i] == 4) {
                            enemyStates[i] = (enemyPositions[i] & 1) == 1 ? 1 : 5;
                            int respawnDelay = GameEngine.random.nextInt() & Integer.MAX_VALUE;
                            enemyTimers[i] = respawnDelay % MainGameCanvas.ENEMY_SPAWN_DELAY_VARIANCE[GameEngine.difficultyLevel]
                                    + MainGameCanvas.ENEMY_SPAWN_DELAY_BASE[GameEngine.difficultyLevel];
                        }
                    }
                }

                for (int y = renderStartY; y < renderEndY; ++y) {
                    int rowStart = renderStartX + PortalRenderer.VIEWPORT_WIDTH * y;
                    int rowEnd = renderEndX + PortalRenderer.VIEWPORT_WIDTH * y;
                    for (int idx = rowStart; idx < rowEnd; ++idx) {
                        int maskValue = maskPixels[idx] & 255;
                        if (maskValue != 255) {
                            PortalRenderer.screenBuffer[idx] = regularPalette[maskValue];
                        }
                    }
                }

                if (level == 0) {
                    for (int i = 6; i < 8; ++i) {
                        if (enemyStates[i] > 0) {
                            int enemyX = enemyPositions[i];
                            int enemyY = yPositions[level][i];
                            int speedType = enemySpeedTypes[level][i];
                            int spriteW = spriteWidths[speedType];
                            int spriteH = spriteHeights[speedType];
                            enemySpriteRefs[i] = enemyTypes[i] == 0 ? largeSprites[enemyStates[i] - 1] : largeAltSprites[enemyStates[i] - 1];
                            PortalRenderer.copyToScreenBuffer(enemySpriteRefs[i], spriteW, spriteH, enemyX, enemyY, totalDamage > 0);
                            if (enemyStates[i] == 4) {
                                enemyStates[i] = (enemyPositions[i] & 1) == 1 ? 1 : 5;
                                int respawnDelay = GameEngine.random.nextInt() & Integer.MAX_VALUE;
                                enemyTimers[i] = respawnDelay % MainGameCanvas.ENEMY_SPAWN_DELAY_VARIANCE[GameEngine.difficultyLevel]
                                        + MainGameCanvas.ENEMY_SPAWN_DELAY_BASE[GameEngine.difficultyLevel];
                            }
                        }
                    }
                }

                int topBufferSize = PortalRenderer.VIEWPORT_WIDTH * renderStartY;
                for (int i = 0; i < topBufferSize; ++i) {
                    PortalRenderer.screenBuffer[i] = normalPalette[scenePixels[i] & 255];
                }

                int sightOffsetY = renderStartY - sightY;
                for (int y = renderStartY; y < renderEndY; ++y, ++sightOffsetY) {
                    int rowStart = PortalRenderer.VIEWPORT_WIDTH * y;
                    int leftEnd = rowStart + renderStartX;
                    for (int idx = rowStart; idx < leftEnd; ++idx) {
                        PortalRenderer.screenBuffer[idx] = normalPalette[scenePixels[idx] & 255];
                    }
                    int rightStart = renderEndX + rowStart;
                    int rowEnd = rowStart + PortalRenderer.VIEWPORT_WIDTH;
                    for (int idx = rightStart; idx < rowEnd; ++idx) {
                        PortalRenderer.screenBuffer[idx] = normalPalette[scenePixels[idx] & 255];
                    }
                    int sightRowOffset = 64 * sightOffsetY;
                    int sightOffsetX = renderStartX - sightX;
                    for (int x = renderStartX; x < renderEndX; ++x, ++sightOffsetX) {
                        if (sightPixels[sightRowOffset + sightOffsetX] == 0) {
                            int bufIdx = rowStart + x;
                            PortalRenderer.screenBuffer[bufIdx] = normalPalette[scenePixels[bufIdx] & 255];
                        }
                    }
                }

                int bottomStart = PortalRenderer.VIEWPORT_WIDTH * renderEndY;
                for (int i = bottomStart; i < bufferSize; ++i) {
                    PortalRenderer.screenBuffer[i] = normalPalette[scenePixels[i] & 255];
                }

                if (GameEngine.inputFire) {
                    int centerX = sightX + 31;
                    int centerY = sightY + 31;
                    int hitColor = 16777215;
                    boolean hitEnemy = false;

                    for (int i = 7; i >= 0; --i) {
                        int enemyX = enemyPositions[i];
                        int enemyY = yPositions[level][i];
                        int speedType = enemySpeedTypes[level][i];
                        int spriteW = spriteWidths[speedType];
                        int spriteH = spriteHeights[speedType];

                        if ((level == 0 && (i >= 6 || speedType == 0) || maskPixels[centerY * PortalRenderer.VIEWPORT_WIDTH + centerX] == -1)
                                && enemyStates[i] > 0
                                && centerX >= enemyX && centerX <= enemyX + spriteW
                                && centerY >= enemyY && centerY <= enemyY + spriteH) {

                            int spritePixelX = centerX - enemyX;
                            int spritePixelY = centerY - enemyY;

                            if (hitEnemy = level == 0 && speedType == 0 ? true
                                    : enemySpriteRefs[i][spriteW * spritePixelY + spritePixelX] != 16711935) {
                                HelperUtils.playSound(7, false, 100, 1);
                                enemyStates[i] = 0;
                                hitColor = 16711680;
                                break;
                            }
                        }
                    }

                    if (!hitEnemy) {
                        HelperUtils.playSound((GameEngine.random.nextInt() & 1) == 0 ? 2 : 6, false, 100, 1);
                    }

                    PortalRenderer.screenBuffer[PortalRenderer.VIEWPORT_WIDTH * centerY + centerX] = hitColor;
                    GameEngine.inputFire = false;
                }

                graphics.drawRGB(PortalRenderer.screenBuffer, 0, PortalRenderer.VIEWPORT_WIDTH,
                        0, 0, PortalRenderer.VIEWPORT_WIDTH, PortalRenderer.VIEWPORT_HEIGHT, false);
                graphics.drawImage(sightImage, sightX, sightY, 20);
                graphics.drawImage(statusBarImage, 0, PortalRenderer.VIEWPORT_HEIGHT, 0);
                fontRenderer.drawCenteredNumber(GameEngine.playerHealth, graphics, 58, PortalRenderer.VIEWPORT_HEIGHT + 6);
                fontRenderer.drawCenteredNumber(GameEngine.playerArmor, graphics, 138, PortalRenderer.VIEWPORT_HEIGHT + 6);
                canvas.flushGraphicsPublic();

                HelperUtils.yieldToOtherThreads();
            }
        } catch (Exception e) {
            DebugLogger.logException("SniperGameController.java", e);
        } catch (OutOfMemoryError e) {
            DebugLogger.logOutOfMemory("SniperGameController.java", e);
        }
        return -1;
    }
}
