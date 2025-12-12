import java.io.DataInputStream;
import java.io.InputStream;
import java.util.Stack;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.GameCanvas;

public class MainGameCanvas extends GameCanvas implements Runnable {

    // ==================== UI Constants ====================

    /** Total UI height (screen + status bar) */
    private static final int UI_HEIGHT = 320;

    /** Status bar height */
    private static final int STATUS_BAR_HEIGHT = 32;

    /** Half of total UI height */
    private static final int HALF_UI_HEIGHT = UI_HEIGHT / 2;

    // ==================== Fields ====================
    public static WeaponManager weaponManager;
    public static int weaponSpriteFrame = 0;
    private final SniperMiniGame sniperMiniGame = new SniperMiniGame();
    public boolean isGameRunning = false;
    public boolean isGamePaused = false;
    public boolean isGameInitialized = true;
    public boolean areResourcesLoaded = false;
    public static CovertOps3D mainMidlet = null;
    private static final String LEVEL_PATH_PREFIX = "/gamedata/levels/level_";
    private static final String[] LEVEL_FILE_NAMES = new String[]{
            "01a", "01b", "02a", "02b", "04", "05", "06a", "06b", "06c", "07a", "07b", "08a", "08b"
    };

    public static int currentLevelId = 0;
    public static int previousLevelId = -1;
    public static int keyMappingOffset;

    private String[] SETTINGS_MENU_ITEMS;
    private String[] chapterMenuItems;
    private int menuOffsetY;
    private int smallFontCharsPerRow;
    private int menuItemHeight;
    private int menuBoxWidthUnit;
    private int[] fontCharOffsets;
    private int[] fontCharWidths;
    private int[] fontTextureX;
    private int[] fontTextureW;
    private int textLineHeight;
    private int spaceWidth;
    public long frameDeltaTime;
    public long accumulatedTime;
    public long lastFrameTime;
    public int frameCounter;

    private Image statusBarImage;
    private Image crosshairImage;
    private Image largeFontImage;
    private Image smallFontImage;

    private GameObject[] cachedStaticObjects;
    private GameObject[] nextLevelObjects;

    public static boolean mapEnabled = false;

    public static final int[] DAMAGE_3003 = new int[]{10, 15, 20};
    public static final int[] DAMAGE_3004 = new int[]{15, 20, 25};
    public static final int[] DAMAGE_3005 = new int[]{20, 25, 30};
    public static final int[] DAMAGE_3006 = new int[]{25, 30, 40};
    public static final int[] SNIPER_DAMAGE_SMALL = new int[]{1, 2, 3};
    public static final int[] SNIPER_DAMAGE_MEDIUM = new int[]{2, 4, 5};
    public static final int[] SNIPER_DAMAGE_LARGE = new int[]{3, 5, 7};
    public static final int[] HP_3003 = new int[]{50, 100, 150};
    public static final int[] HP_3004 = new int[]{100, 200, 300};
    public static final int[] HP_3005 = new int[]{100, 200, 300};
    public static final int[] HP_3006 = new int[]{100, 200, 300};
    public static final int[] HP_3001 = new int[]{200, 400, 600};
    public static final int[] HP_3002 = new int[]{300, 600, 900};
    public static final int[] ENEMY_STATE_TRANSITION_TIME = new int[]{64, 64, 64};
    public static final int[] ENEMY_ATTACK_DELAY_MIN = new int[]{6, 4, 2};
    public static final int[] ENEMY_ATTACK_DELAY_RANGE = new int[]{32, 22, 12};
    public static final int[] ENEMY_REDETECT_DELAY = new int[]{32, 22, 12};
    public static final int[] ENEMY_SPAWN_DELAY_BASE = new int[]{256, 192, 128};
    public static final int[] ENEMY_SPAWN_DELAY_VARIANCE = new int[]{128, 128, 128};
    public static final int[] ENEMY_ATTACK_DELAY_BASE = new int[]{128, 64, 32};
    public static final int[] ENEMY_ATTACK_DELAY_VARIANCE = new int[]{32, 32, 32};
    public static final int[] SPEED_3003 = new int[]{131072, 196608, 262144};
    public static final int[] SPEED_3004 = new int[]{131072, 196608, 262144};
    public static final int[] SPEED_3005 = new int[]{196608, 262144, 327680};
    public static final int[] SPEED_3006 = new int[]{196608, 262144, 327680};
    public static final int[] SPEED_3001 = new int[]{196608, 262144, 327680};
    public static final int[] SPEED_3002 = new int[]{196608, 262144, 327680};
    public static final int[] ENEMY_STRAFE_CHANCE_DIVISOR = new int[]{4, 3, 2};

    public MainGameCanvas() {
        super(false);
        System.currentTimeMillis();
        this.menuOffsetY = 18;
        this.smallFontCharsPerRow = 26;
        this.menuItemHeight = 23;
        this.menuBoxWidthUnit = 4;
        this.fontCharOffsets = new int[]{1, 11, 22, 31, 42, 52, 62, 70, 82, 91, 101, 112, 120, 130, 142, 151, 161, 171, 2, 12, 20, 31, 40, 51, 61, 72, 80, 90, 100, 110, 120, 130, 142, 151, 160, 170, 1, 12, 21, 31, 41, 51, 61, 71, 81, 91, 100, 110, 120, 130, 140, 150, 160, 170};
        this.fontCharWidths = new int[]{9, 9, 7, 8, 7, 7, 7, 10, 6, 6, 9, 6, 10, 10, 7, 9, 8, 8, 7, 6, 10, 8, 10, 9, 8, 7, 4, 4, 4, 8, 4, 4, 7, 4, 0, 0, 8, 6, 8, 8, 9, 8, 8, 8, 8, 8, 0, 0, 0, 0, 0, 0, 0, 0};
        this.fontTextureX = new int[]{0, 8, 14, 21, 29, 36, 42, 49, 58, 64, 71, 78, 85, 91, 98, 106, 112, 120, 126, 134, 140, 148, 154, 162, 169, 176, 1, 8, 15, 22, 29, 36, 43, 50, 59, 65, 71, 80, 84, 92, 99, 106, 113, 121, 127, 135, 141, 148, 155, 162, 169, 177, 1, 9, 15, 22, 29, 36, 43, 50, 57, 64, 71, 77, 85, 92, 99, 105, 112, 121, 127, 133, 140, 147, 154, 161, 168, 175};
        this.fontTextureW = new int[]{6, 5, 6, 6, 5, 5, 6, 6, 3, 3, 5, 4, 5, 6, 6, 5, 6, 5, 5, 5, 6, 5, 7, 5, 5, 5, 5, 5, 5, 5, 5, 4, 5, 5, 1, 2, 5, 2, 7, 5, 5, 5, 5, 3, 5, 2, 5, 5, 5, 4, 5, 3, 4, 3, 4, 4, 5, 4, 4, 4, 4, 4, 1, 2, 1, 4, 1, 2, 4, 3, 2, 7, 0, 0, 0, 0, 0, 0};
        this.textLineHeight = 10;
        this.spaceWidth = 3;
        this.frameDeltaTime = 0L;
        this.accumulatedTime = 0L;
        this.lastFrameTime = 0L;
        this.frameCounter = 0;
        this.cachedStaticObjects = null;
        this.nextLevelObjects = null;

        weaponManager = new WeaponManager();
        keyMappingOffset = Math.abs(this.getKeyCode(8)) == 53 ? 5 : Math.abs(this.getKeyCode(8));
        this.setFullScreenMode(true);
    }

    public void sizeChanged(int width, int height) {
    }

    private int translateKeyCode(int keyCode) {
        switch((keyCode < 0 ? -keyCode : keyCode) - keyMappingOffset) {
            case 1:
                return 11;
            case 2:
                return 12;
            default:
                switch(this.getGameAction(keyCode)) {
                    case 1:
                        return 1;
                    case 2:
                        return 3;
                    case 3:
                    case 4:
                    case 7:
                    default:
                        return 10;
                    case 5:
                        return 4;
                    case 6:
                        return 2;
                    case 8:
                        return 5;
                    case 9:
                        return 6;
                    case 10:
                        return 7;
                    case 11:
                        return 8;
                    case 12:
                        return 9;
                }
        }
    }

    public void keyPressed(int keyCode) {
        switch(this.translateKeyCode(keyCode)) {
            case 1:
                GameEngine.inputForward = true;
                return;
            case 2:
                GameEngine.inputBackward = true;
                return;
            case 3:
                GameEngine.inputLookUp = true;
                return;
            case 4:
                GameEngine.inputLookDown = true;
                return;
            case 5:
                GameEngine.inputFire = true;
                GameEngine.inputStrafe = false;
                return;
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            default:
                switch(keyCode) {
                    case 48:
                        GameEngine.toggleMapInput = true;
                        return;
                    case 49:
                        GameEngine.useKey = true;
                        return;
                    case 51:
                        GameEngine.selectNextWeapon = true;
                        return;
                    case 53:
                        GameEngine.inputFire = true;
                        GameEngine.inputStrafe = false;
                        return;
                    case 55:
                        GameEngine.inputLeft = true;
                        return;
                    case 57:
                        GameEngine.inputRight = true;
                    case 50:
                    case 52:
                    case 54:
                    case 56:
                    default:
                        return;
                }
            case 11:
                GameEngine.inputRun = true;
                return;
            case 12:
                GameEngine.inputBack = true;
        }
    }

    public void keyReleased(int keyCode) {
        switch(this.translateKeyCode(keyCode)) {
            case 1:
                GameEngine.inputForward = false;
                return;
            case 2:
                GameEngine.inputBackward = false;
                return;
            case 3:
                GameEngine.inputLookUp = false;
                return;
            case 4:
                GameEngine.inputLookDown = false;
                return;
            case 5:
                GameEngine.inputStrafe = true;
                return;
            default:
                switch(keyCode) {
                    case 55:
                        GameEngine.inputLeft = false;
                        return;
                    case 57:
                        GameEngine.inputRight = false;
                    default:
                }
        }
    }

    private void renderHUDAndWeapon(Graphics graphics) {
        try {
            int headBob = GameEngine.renderFrame(graphics, frameCounter) >> 15;

            weaponManager.render(graphics, headBob);

            graphics.drawImage(statusBarImage, 0, PortalRenderer.VIEWPORT_HEIGHT, 0);
            drawHUDNumber(GameEngine.playerHealth, graphics, 58, PortalRenderer.VIEWPORT_HEIGHT + 6);
            drawHUDNumber(GameEngine.playerArmor, graphics, 138, PortalRenderer.VIEWPORT_HEIGHT + 6);

            int ammoType = weaponManager.getDisplayAmmoType();
            if (ammoType >= 0) {
                drawHUDNumber(GameEngine.ammoCounts[ammoType], graphics, 218, PortalRenderer.VIEWPORT_HEIGHT + 6);
            }

            if (weaponManager.getCurrentWeaponId() > 0 && GameEngine.messageTimer == 0 && !mapEnabled) {
                graphics.drawImage(crosshairImage,
                        (PortalRenderer.VIEWPORT_WIDTH - crosshairImage.getWidth()) >> 1,
                        (PortalRenderer.VIEWPORT_HEIGHT - crosshairImage.getHeight()) >> 1, 0);
            }

            if (mapEnabled) {
                graphics.setClip(0, 0, PortalRenderer.VIEWPORT_WIDTH, PortalRenderer.VIEWPORT_HEIGHT);
                LevelLoader.gameWorld.drawMapOnScreen(graphics);
                graphics.setClip(0, 0, PortalRenderer.VIEWPORT_WIDTH, UI_HEIGHT);
            }
        } catch (Exception e) {
        } catch (OutOfMemoryError e) {
        }
    }

    public final void startGameThread() {
        Thread gameThread = new Thread(this);
        this.isGameRunning = true;
        this.isGameInitialized = false;
        gameThread.start();
    }

    public void run() {
        HelperUtils.audioManager = new AudioManager();
        HelperUtils.audioManager.loadSound("/gamedata/sound/0.mid");
        HelperUtils.audioManager.loadSound("/gamedata/sound/1.amr");
        HelperUtils.audioManager.loadSound("/gamedata/sound/2.amr");
        HelperUtils.audioManager.loadSound("/gamedata/sound/3.amr");
        HelperUtils.audioManager.loadSound("/gamedata/sound/4.amr");
        HelperUtils.audioManager.loadSound("/gamedata/sound/5.amr");
        HelperUtils.audioManager.loadSound("/gamedata/sound/6.amr");
        HelperUtils.audioManager.loadSound("/gamedata/sound/7.amr");
        HelperUtils.audioManager.loadSound("/gamedata/sound/8.amr");
        HelperUtils.audioManager.loadSound("/gamedata/sound/9.amr");

        Graphics graphics = this.getGraphics();
        graphics.setClip(0, 0, PortalRenderer.VIEWPORT_WIDTH, UI_HEIGHT);
        this.drawSplash(graphics);
        this.initializeGameResources();
        SaveSystem.loadSaveData();
        SaveSystem.loadSettingsFromRMS();
        this.areResourcesLoaded = true;
        int menuResult = this.showMenuScreen(graphics, true);

        label182:
        while(true) {
            while(true) {
                do {
                    if (!this.isGameRunning) {
                        this.isGameInitialized = true;
                        return;
                    }
                } while(this.isGamePaused);

                if (menuResult == 4) {
                    this.isGameInitialized = true;
                    CovertOps3D.exitApplication();
                    return;
                }

                GameEngine.resetPlayerProgress();
                if (menuResult == 66) {
                    currentLevelId = 0;
                    previousLevelId = -1;
                    if ((menuResult = this.drawDialogOverlay(graphics, 0)) != -1) {
                        continue;
                    }

                    this.drawPleaseWait(graphics);
                    this.loadLevelResources();
                    break;
                }

                int[] levelMap = new int[]{2, 4, 20, 5, 6, 22, 7, 9};
                int chapterIndex = menuResult - 67;
                currentLevelId = levelMap[chapterIndex];
                previousLevelId = -1;
                SaveSystem.loadGameState(chapterIndex);
                GameEngine.levelTransitionState = 1;
                break;
            }

            this.accumulatedTime = 0L;
            this.lastFrameTime = 0L;

            while(this.isGameRunning) {
                try {
                    if ((GameEngine.inputRun || GameEngine.inputBack || this.isGamePaused)
                            && (menuResult = this.showMenuScreen(graphics, false)) != 32) {
                        break;
                    }

                    label170: {
                        MainGameCanvas tempCanvas;
                        if (GameEngine.levelTransitionState == 1) {
                            switch(currentLevelId) {
                                case 0:
                                case 13:
                                    currentLevelId = 0;
                                    SaveSystem.saveGameState(8);
                                    if ((menuResult = this.drawDialogOverlay(graphics, 9)) == -1) {
                                        menuResult = this.showMenuScreen(graphics, true);
                                    }
                                    continue label182;
                                case 1:
                                case 3:
                                case 8:
                                case 10:
                                case 11:
                                case 12:
                                case 14:
                                case 15:
                                case 16:
                                case 17:
                                case 18:
                                case 19:
                                case 21:
                                default:
                                    break;
                                case 2:
                                    SaveSystem.saveGameState(0);
                                    if ((menuResult = this.drawDialogOverlay(graphics, 1)) != -1) {
                                        continue label182;
                                    }
                                    break;
                                case 4:
                                case 20:
                                    if (currentLevelId == 4) {
                                        SaveSystem.saveGameState(1);
                                        if ((menuResult = this.drawDialogOverlay(graphics, 2)) != -1) {
                                            continue label182;
                                        }

                                        if ((menuResult = this.runMiniGameSniper(graphics, 0)) == -2) {
                                            this.drawGameOver(graphics);
                                            menuResult = this.showMenuScreen(graphics, true);
                                            continue label182;
                                        }

                                        if (menuResult != -1) {
                                            continue label182;
                                        }
                                    } else {
                                        currentLevelId = 4;
                                    }

                                    SaveSystem.saveGameState(2);
                                    if ((menuResult = this.drawDialogOverlay(graphics, 3)) != -1) {
                                        continue label182;
                                    }
                                    break;
                                case 5:
                                    SaveSystem.saveGameState(3);
                                    if ((menuResult = this.drawDialogOverlay(graphics, 4)) != -1) {
                                        continue label182;
                                    }
                                    break;
                                case 6:
                                case 22:
                                    if (currentLevelId == 6) {
                                        SaveSystem.saveGameState(4);
                                        if ((menuResult = this.drawDialogOverlay(graphics, 5)) != -1) {
                                            continue label182;
                                        }

                                        if ((menuResult = this.runMiniGameSniper(graphics, 1)) == -2) {
                                            this.drawGameOver(graphics);
                                            menuResult = this.showMenuScreen(graphics, true);
                                            continue label182;
                                        }

                                        if (menuResult != -1) {
                                            continue label182;
                                        }
                                    } else {
                                        currentLevelId = 6;
                                    }

                                    SaveSystem.saveGameState(5);
                                    if ((menuResult = this.drawDialogOverlay(graphics, 6)) != -1) {
                                        continue label182;
                                    }
                                    break;
                                case 7:
                                    SaveSystem.saveGameState(6);
                                    if ((menuResult = this.drawDialogOverlay(graphics, 7)) != -1) {
                                        continue label182;
                                    }
                                    break;
                                case 9:
                                    SaveSystem.saveGameState(7);
                                    if ((menuResult = this.drawDialogOverlay(graphics, 8)) != -1) {
                                        continue label182;
                                    }
                            }

                            tempCanvas = this;
                        } else {
                            if (GameEngine.levelTransitionState != -1) {
                                break label170;
                            }

                            tempCanvas = this;
                        }

                        tempCanvas.drawPleaseWait(graphics);
                        this.loadLevelResources();
                    }

                    long currentTime = System.currentTimeMillis();
                    this.frameDeltaTime = currentTime - this.lastFrameTime;
                    this.lastFrameTime = currentTime;
                    this.accumulatedTime += this.frameDeltaTime;
                    if (this.accumulatedTime > 600L) {
                        this.accumulatedTime = 600L;
                    }

                    while(this.accumulatedTime >= 50L) {
                        ++this.frameCounter;
                        if (this.gameLoopTick()) {
                            GameEngine.damageFlash = false;
                            this.renderHUDAndWeapon(graphics);
                            this.flushScreenBuffer();
                            this.drawGameOver(graphics);
                            menuResult = this.showMenuScreen(graphics, true);
                            continue label182;
                        }

                        this.accumulatedTime -= 50L; //20 fps quick hack
                    }

                    this.renderHUDAndWeapon(graphics);
                    if (GameEngine.messageTimer > 0) {
                        this.drawMultiLineMessage(graphics, GameEngine.messageText);
                    }

                    this.flushScreenBuffer();
                    HelperUtils.yieldToOtherThreads();
                } catch (Exception e) {
                } catch (OutOfMemoryError e) {
                }
            }
        }
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

            for(int i = 0; i < paletteSize; ++i) {
                int r = dataInput.readByte() & 255;
                int g = dataInput.readByte() & 255;
                int b = dataInput.readByte() & 255;
                palette[i] = r << 16 | g << 8 | b;
            }

            dataInput.close();
            result = new int[pixelCount];

            if (flip) {
                for(int y = 0; y < height; ++y) {
                    for(int x = 0; x < width; ++x) {
                        result[y * width + (width - x - 1)] = palette[pixelData[y * width + x] & 255];
                    }
                }
            } else {
                for(int i = 0; i < pixelCount; ++i) {
                    result[i] = palette[pixelData[i] & 255];
                }
            }
        } catch (Exception e) {
        } catch (OutOfMemoryError e) {
        }

        return result;
    }



    private void drawSplash(Graphics graphics) {
        try {
            Image logo = Image.createImage("/gamedata/sprites/logo.png");
            Image splash = Image.createImage("/gamedata/sprites/splash.png");

            int pixelCount = logo.getWidth() * logo.getHeight();
            int[] fadeBuffer = new int[pixelCount];
            int logoX = (PortalRenderer.VIEWPORT_WIDTH - logo.getWidth()) / 2;
            int logoY = (UI_HEIGHT - logo.getHeight()) / 2;

            graphics.setColor(16777215);
            graphics.drawRect(0, 0, PortalRenderer.VIEWPORT_WIDTH, UI_HEIGHT);
            this.flushScreenBuffer();

            long startTime = System.currentTimeMillis();

            while(true) {
                int fadeColor = 16777215;
                int elapsed = (int)(System.currentTimeMillis() - startTime >> 2);

                if (elapsed < 256) {
                    fadeColor |= (255 - elapsed) << 24;
                } else if (elapsed >= 512 && elapsed < 768) {
                    fadeColor |= (elapsed - 512) << 24;
                } else if (elapsed >= 768) {
                    fadeBuffer = new int[pixelCount = splash.getWidth() * splash.getHeight()];
                    startTime = System.currentTimeMillis();

                    while(true) {
                        fadeColor = 16777215;
                        elapsed = (int)(System.currentTimeMillis() - startTime >> 2);

                        if (elapsed < 256) {
                            fadeColor |= (255 - elapsed) << 24;
                        } else if (elapsed >= 768) {
                            return;
                        }

                        fadeBuffer[0] = fadeColor;
                        HelperUtils.fastArrayFill(fadeBuffer, 0, pixelCount);
                        graphics.drawImage(splash, 0, 0, 20);
                        graphics.drawRGB(fadeBuffer, 0, splash.getWidth(), 0, 0,
                                splash.getWidth(), splash.getHeight(), true);
                        this.flushScreenBuffer();
                        HelperUtils.yieldToOtherThreads();
                    }
                }

                fadeBuffer[0] = fadeColor;
                HelperUtils.fastArrayFill(fadeBuffer, 0, pixelCount);
                graphics.drawImage(logo, logoX, logoY, 20);
                graphics.drawRGB(fadeBuffer, 0, logo.getWidth(), logoX, logoY,
                        logo.getWidth(), logo.getHeight(), true);
                this.flushScreenBuffer();
                HelperUtils.yieldToOtherThreads();
            }
        } catch (Exception e) {
        } catch (OutOfMemoryError e) {
        }
    }

    private void drawGameOver(Graphics graphics) {
        try {
            Image splash = Image.createImage("/gamedata/sprites/splash.png");

            int halfScreenBuffer = PortalRenderer.VIEWPORT_WIDTH * HALF_UI_HEIGHT;
            PortalRenderer.screenBuffer[0] = -2130771968;
            HelperUtils.fastArrayFill(PortalRenderer.screenBuffer, 0, halfScreenBuffer);

            graphics.drawRGB(PortalRenderer.screenBuffer, 0, PortalRenderer.VIEWPORT_WIDTH,
                    0, 0, PortalRenderer.VIEWPORT_WIDTH, HALF_UI_HEIGHT, true);
            graphics.drawRGB(PortalRenderer.screenBuffer, 0, PortalRenderer.VIEWPORT_WIDTH,
                    0, HALF_UI_HEIGHT, PortalRenderer.VIEWPORT_WIDTH, HALF_UI_HEIGHT, true);

            String message = TextStrings.MISSION_FAILED_GAME_OVER;
            this.drawMultiLineMessage(graphics, message);
            this.flushScreenBuffer();
            HelperUtils.delay(2000);

            long startTime = System.currentTimeMillis();

            while(true) {
                int fadeColor = 16711680;
                int elapsed = (int)(System.currentTimeMillis() - startTime >> 4);

                if (elapsed < 128) {
                    fadeColor |= (255 - elapsed) << 24;
                } else {
                    fadeColor |= Integer.MIN_VALUE;
                    if (elapsed >= 512) {
                        break;
                    }
                }

                PortalRenderer.screenBuffer[0] = fadeColor;
                HelperUtils.fastArrayFill(PortalRenderer.screenBuffer, 0, halfScreenBuffer);
                graphics.drawImage(splash, 0, 0, 20);
                graphics.drawRGB(PortalRenderer.screenBuffer, 0, PortalRenderer.VIEWPORT_WIDTH,
                        0, 0, PortalRenderer.VIEWPORT_WIDTH, HALF_UI_HEIGHT, true);
                graphics.drawRGB(PortalRenderer.screenBuffer, 0, PortalRenderer.VIEWPORT_WIDTH,
                        0, HALF_UI_HEIGHT, PortalRenderer.VIEWPORT_WIDTH, HALF_UI_HEIGHT, true);
                this.drawMultiLineMessage(graphics, message);
                this.flushScreenBuffer();
                HelperUtils.yieldToOtherThreads();
            }
        } catch (Exception e) {
            return;
        } catch (OutOfMemoryError e) {
        }
    }

    private void drawStripedBackground(Graphics graphics, Image background) {
        this.accumulatedTime = 0L;
        this.lastFrameTime = System.currentTimeMillis();
        int progress = 0;

        do {
            long currentTime = System.currentTimeMillis();
            this.frameDeltaTime = currentTime - this.lastFrameTime;
            this.lastFrameTime = currentTime;
            this.accumulatedTime += this.frameDeltaTime;
            if (this.accumulatedTime > 600L) {
                this.accumulatedTime = 600L;
            }

            while(this.accumulatedTime >= 6L) {
                ++progress;
                this.accumulatedTime -= 6L;
            }

            int displayProgress = progress > UI_HEIGHT ? UI_HEIGHT : progress;
            int column = 0;

            for(int x = 0; x < PortalRenderer.VIEWPORT_WIDTH; x += 10) {
                if ((column & 1) == 0) {
                    graphics.drawRegion(background, x, UI_HEIGHT - displayProgress, 10, displayProgress,
                            0, x, 0, 20);
                } else {
                    graphics.drawRegion(background, x, 0, 10, displayProgress,
                            0, x, UI_HEIGHT - displayProgress, 20);
                }
                ++column;
            }

            this.flushScreenBuffer();
        } while(progress <= UI_HEIGHT);
    }

    private int showMenuScreen(Graphics graphics, boolean isMainMenu) {
        try {
            GameEngine.inputRun = false;
            GameEngine.inputBack = false;
            GameEngine.inputFire = false;
            GameEngine.inputForward = false;
            GameEngine.inputBackward = false;

            Image background = Image.createImage("/gamedata/sprites/bkg.png");
            int menuMode = 0;
            int scrollOffset = 0;
            String[] menuItems = TextStrings.mainMenuItems;

            if (!isMainMenu) {
                menuMode = 32;
                menuItems = TextStrings.pauseMenuItems;
            }

            int firstItem = 0;
            int lastItem = menuItems.length - 2;
            this.drawStripedBackground(graphics, background);

            if (SaveSystem.musicEnabled == 1 && !this.isGamePaused) {
                HelperUtils.playSound(0, true, 80, 2);
            }

            Stack menuStack = new Stack();

            while(true) {
                graphics.drawImage(background, 0, 0, 20);

                int totalItems = menuItems.length - 1;
                int visibleItems = totalItems > 5 ? 5 : totalItems;
                int menuY = UI_HEIGHT - visibleItems * this.menuItemHeight - 3 - this.menuItemHeight;

                if (totalItems > visibleItems && scrollOffset > 0) {
                    int arrowY = menuY + 2 * this.menuItemHeight - 2;
                    graphics.setColor(16115387);
                    graphics.fillTriangle(117, arrowY, 123, arrowY, PortalRenderer.HALF_VIEWPORT_WIDTH, arrowY - 3);
                }

                graphics.setColor(7433570);

                for(int i = 0; i < visibleItems; ++i) {
                    int itemIndex = i;
                    if (scrollOffset > 0 && i > 1) {
                        itemIndex = i + scrollOffset;
                    }

                    String itemText = menuItems[itemIndex];
                    int textX = (PortalRenderer.VIEWPORT_WIDTH - this.getLargeTextWidth(itemText)) / 2;

                    if ((menuMode & 15) == itemIndex) {
                        int boxWidth = this.menuBoxWidthUnit * 30;
                        graphics.fillRoundRect((PortalRenderer.VIEWPORT_WIDTH - boxWidth) / 2, menuY,
                                boxWidth, this.menuItemHeight, 10, 10);
                    }

                    this.drawLargeString(itemText, graphics, textX, menuY);
                    menuY += this.menuItemHeight;
                }

                if (totalItems > visibleItems && scrollOffset < totalItems - 5) {
                    int arrowY = menuY + 1;
                    graphics.setColor(16115387);
                    graphics.fillTriangle(117, arrowY, 123, arrowY, PortalRenderer.HALF_VIEWPORT_WIDTH, arrowY + 3);
                }

                String actionText = menuItems == this.SETTINGS_MENU_ITEMS ? TextStrings.CHANGE :
                        (menuItems == TextStrings.CONFIRMATION_MENU_ITEMS ? TextStrings.YES : TextStrings.SELECT);
                this.drawLargeString(actionText, graphics, 3, UI_HEIGHT - this.menuItemHeight - 3);
                this.drawLargeString(menuItems[totalItems], graphics,
                        PortalRenderer.VIEWPORT_WIDTH - this.getLargeTextWidth(menuItems[totalItems]) - 3,
                        UI_HEIGHT - this.menuItemHeight - 3);
                this.flushScreenBuffer();
                HelperUtils.yieldToOtherThreads();

                Object[] stackData = new Object[0];

                if (GameEngine.inputRun || GameEngine.inputFire) {
                    GameEngine.inputRun = false;
                    GameEngine.inputFire = false;

                    switch(menuMode) {
                        case 0:
                        case 33:
                            stackData = new Object[4];
                            stackData[0] = menuItems;
                            stackData[1] = new Integer(menuMode);
                            stackData[2] = new Integer(firstItem);
                            stackData[3] = new Integer(lastItem);
                            menuStack.push(stackData);
                            menuItems = TextStrings.difficultyMenuItems;
                            menuMode = 18 + GameEngine.difficultyLevel;
                            firstItem = 2;
                            lastItem = menuItems.length - 2;
                            break;

                        case 1:
                        case 34:
                            this.SETTINGS_MENU_ITEMS = new String[6];
                            this.SETTINGS_MENU_ITEMS[0] = TextStrings.SETTINGS;
                            this.SETTINGS_MENU_ITEMS[1] = TextStrings.EMPTY_SPACE;
                            this.SETTINGS_MENU_ITEMS[2] = TextStrings.SOUND + (SaveSystem.soundEnabled == 1 ? TextStrings.ON : TextStrings.OFF);
                            this.SETTINGS_MENU_ITEMS[3] = TextStrings.MUSIC + (SaveSystem.musicEnabled == 1 ? TextStrings.ON : TextStrings.OFF);
                            this.SETTINGS_MENU_ITEMS[4] = TextStrings.VIBRATION + (SaveSystem.vibrationEnabled == 1 ? TextStrings.ON : TextStrings.OFF);
                            this.SETTINGS_MENU_ITEMS[5] = TextStrings.BACK;
                            stackData = new Object[4];
                            stackData[0] = menuItems;
                            stackData[1] = new Integer(menuMode);
                            stackData[2] = new Integer(firstItem);
                            stackData[3] = new Integer(lastItem);
                            menuStack.push(stackData);
                            menuItems = this.SETTINGS_MENU_ITEMS;
                            menuMode = 50;
                            firstItem = 2;
                            lastItem = this.SETTINGS_MENU_ITEMS.length - 2;
                            break;

                        case 2:
                        case 35:
                            this.showScrollingText(graphics, background, TextStrings.HELP, TextStrings.HELP_MENU_ITEMS, false);
                            break;

                        case 3:
                        case 36:
                            this.showScrollingText(graphics, background, TextStrings.ABOUT, TextStrings.ABOUT_MENU_TEXT, true);
                            break;

                        case 4:
                        case 5:
                        case 6:
                        case 7:
                        case 8:
                        case 9:
                        case 10:
                        case 11:
                        case 12:
                        case 13:
                        case 14:
                        case 15:
                        case 16:
                        case 17:
                        case 21:
                        case 22:
                        case 23:
                        case 24:
                        case 25:
                        case 26:
                        case 27:
                        case 28:
                        case 29:
                        case 30:
                        case 31:
                        case 32:
                        case 37:
                        case 38:
                        case 39:
                        case 40:
                        case 41:
                        case 42:
                        case 43:
                        case 44:
                        case 45:
                        case 46:
                        case 47:
                        case 48:
                        case 49:
                        case 53:
                        case 54:
                        case 55:
                        case 56:
                        case 57:
                        case 58:
                        case 59:
                        case 60:
                        case 61:
                        case 62:
                        case 63:
                        case 64:
                        case 65:
                        case 75:
                        case 76:
                        case 77:
                        case 78:
                        case 79:
                        default:
                            HelperUtils.stopCurrentSound();
                            return menuMode;

                        case 18:
                        case 19:
                        case 20:
                            this.chapterMenuItems = new String[TextStrings.CHAPTER_MENU_DATA.length];
                            this.chapterMenuItems[0] = TextStrings.CHAPTER_MENU_DATA[0];
                            this.chapterMenuItems[1] = TextStrings.CHAPTER_MENU_DATA[1];
                            this.chapterMenuItems[2] = TextStrings.CHAPTER_MENU_DATA[2];
                            this.chapterMenuItems[this.chapterMenuItems.length - 1] =
                                    TextStrings.CHAPTER_MENU_DATA[this.chapterMenuItems.length - 1];
                            stackData = new Object[4];
                            stackData[0] = menuItems;
                            stackData[1] = new Integer(menuMode);
                            stackData[2] = new Integer(firstItem);
                            stackData[3] = new Integer(lastItem);
                            menuStack.push(stackData);
                            GameEngine.difficultyLevel = menuMode - 18;
                            SaveSystem.loadSaveData();
                            firstItem = 2;
                            lastItem = this.chapterMenuItems.length - 2;

                            for(int i = 3; i <= lastItem; ++i) {
                                this.chapterMenuItems[i] = SaveSystem.saveData[i - 3] != null
                                        ? TextStrings.CHAPTER_MENU_DATA[i]
                                        : TextStrings.UNAVAILABLE;
                            }

                            menuItems = this.chapterMenuItems;
                            menuMode = 66;
                            break;

                        case 50:
                            SaveSystem.soundEnabled = (byte)(SaveSystem.soundEnabled ^ 1);
                            this.SETTINGS_MENU_ITEMS[2] = TextStrings.SOUND + (SaveSystem.soundEnabled == 1 ? TextStrings.ON : TextStrings.OFF);
                            if (SaveSystem.musicEnabled != 1) {
                                if (SaveSystem.soundEnabled == 1) {
                                    HelperUtils.playSound(1, false, 80, 0);
                                } else {
                                    HelperUtils.stopCurrentSound();
                                }
                            }
                            SaveSystem.saveSettingsToRMS();
                            break;

                        case 51:
                            SaveSystem.musicEnabled = (byte)(SaveSystem.musicEnabled ^ 1);
                            this.SETTINGS_MENU_ITEMS[3] = TextStrings.MUSIC + (SaveSystem.musicEnabled == 1 ? TextStrings.ON : TextStrings.OFF);
                            if (SaveSystem.musicEnabled == 1) {
                                HelperUtils.stopCurrentSound();
                                HelperUtils.playSound(0, true, 80, 2);
                            } else {
                                HelperUtils.stopCurrentSound();
                            }
                            SaveSystem.saveSettingsToRMS();
                            break;

                        case 52:
                            SaveSystem.vibrationEnabled = (byte)(SaveSystem.vibrationEnabled ^ 1);
                            this.SETTINGS_MENU_ITEMS[4] = TextStrings.VIBRATION + (SaveSystem.vibrationEnabled == 1 ? TextStrings.ON : TextStrings.OFF);
                            if (SaveSystem.vibrationEnabled == 1) {
                                HelperUtils.vibrateDevice(100);
                            }
                            SaveSystem.saveSettingsToRMS();
                            break;

                        case 66:
                        case 67:
                        case 68:
                        case 69:
                        case 70:
                        case 71:
                        case 72:
                        case 73:
                        case 74:
                            if (!this.chapterMenuItems[menuMode - 64].equals(TextStrings.UNAVAILABLE)) {
                                HelperUtils.stopCurrentSound();
                                return menuMode;
                            }
                            break;

                        case 80:
                            HelperUtils.stopCurrentSound();
                            return 4;
                    }
                }

                if (GameEngine.inputBack) {
                    GameEngine.inputBack = false;
                    if (menuItems[menuItems.length - 1] != TextStrings.BACK && menuItems[menuItems.length - 1] != "no") {
                        if (menuItems[menuItems.length - 1] == TextStrings.QUIT) {
                            stackData = new Object[4];
                            stackData[0] = menuItems;
                            stackData[1] = new Integer(menuMode);
                            stackData[2] = new Integer(firstItem);
                            stackData[3] = new Integer(lastItem);
                            menuStack.push(stackData);
                            menuItems = TextStrings.CONFIRMATION_MENU_ITEMS;
                            menuMode = 80;
                            firstItem = 0;
                            lastItem = 0;
                        }
                    } else {
                        Object[] popped = (Object[])menuStack.pop();
                        menuItems = (String[])popped[0];
                        menuMode = ((Integer)popped[1]).intValue();
                        firstItem = ((Integer)popped[2]).intValue();
                        lastItem = ((Integer)popped[3]).intValue();
                        scrollOffset = 0;
                    }
                }

                if (GameEngine.inputForward) {
                    int selectedItem = menuMode & 15;
                    --selectedItem;
                    if (selectedItem < firstItem) {
                        selectedItem = firstItem;
                    } else if (selectedItem - scrollOffset < 2) {
                        --scrollOffset;
                    }
                    menuMode = (menuMode & ~15) | selectedItem;
                    GameEngine.inputForward = false;
                }

                if (GameEngine.inputBackward) {
                    int selectedItem = menuMode & 15;
                    ++selectedItem;
                    if (selectedItem > lastItem) {
                        selectedItem = lastItem;
                    } else if (totalItems > visibleItems && selectedItem - scrollOffset > 4) {
                        ++scrollOffset;
                    }
                    menuMode = (menuMode & ~15) | selectedItem;
                    GameEngine.inputBackward = false;
                }
            }
        } catch (Exception e) {
            HelperUtils.stopCurrentSound();
            return 4;
        } catch (OutOfMemoryError e) {
            HelperUtils.stopCurrentSound();
            return 4;
        }
    }

    private void showScrollingText(Graphics graphics, Image background, String title, String[] content, boolean scrolling) {
        GameEngine.inputRun = false;
        GameEngine.inputBack = false;
        GameEngine.inputFire = false;
        GameEngine.inputForward = false;
        GameEngine.inputBackward = false;

        try {
            String version = mainMidlet.getAppProperty("MIDlet-Version");
            this.smallFontImage = Image.createImage("/gamedata/sprites/font_cut.png");

            boolean needsUpdate = true;
            int textY = UI_HEIGHT - this.menuItemHeight;

            int halfScreenBuffer = PortalRenderer.VIEWPORT_WIDTH * HALF_UI_HEIGHT;

            for(int fadeStep = 1; fadeStep <= 8; ++fadeStep) {
                PortalRenderer.screenBuffer[0] = 16777215 | (fadeStep * 268435456);
                HelperUtils.fastArrayFill(PortalRenderer.screenBuffer, 0, halfScreenBuffer);
                graphics.drawImage(background, 0, 0, 20);
                graphics.drawRGB(PortalRenderer.screenBuffer, 0, PortalRenderer.VIEWPORT_WIDTH,
                        0, 0, PortalRenderer.VIEWPORT_WIDTH, HALF_UI_HEIGHT, true);
                graphics.drawRGB(PortalRenderer.screenBuffer, 0, PortalRenderer.VIEWPORT_WIDTH,
                        0, HALF_UI_HEIGHT, PortalRenderer.VIEWPORT_WIDTH, HALF_UI_HEIGHT, true);
                this.flushScreenBuffer();
                HelperUtils.yieldToOtherThreads();
                HelperUtils.delay(50);
            }

            long lastScrollTime = System.currentTimeMillis();

            do {
                if (needsUpdate) {
                    graphics.setClip(0, 0, PortalRenderer.VIEWPORT_WIDTH, UI_HEIGHT);
                    graphics.drawImage(background, 0, 0, 20);
                    graphics.drawRGB(PortalRenderer.screenBuffer, 0, PortalRenderer.VIEWPORT_WIDTH,
                            0, 0, PortalRenderer.VIEWPORT_WIDTH, HALF_UI_HEIGHT, true);
                    graphics.drawRGB(PortalRenderer.screenBuffer, 0, PortalRenderer.VIEWPORT_WIDTH,
                            0, HALF_UI_HEIGHT, PortalRenderer.VIEWPORT_WIDTH, HALF_UI_HEIGHT, true);

                    String backText = TextStrings.BACK;
                    this.drawLargeString(backText, graphics,
                            PortalRenderer.VIEWPORT_WIDTH - this.getLargeTextWidth(backText) - 3,
                            UI_HEIGHT - this.menuItemHeight - 3);
                    this.drawLargeString(title, graphics,
                            (PortalRenderer.VIEWPORT_WIDTH - this.getLargeTextWidth(title)) / 2, 3);

                    graphics.setClip(0, this.menuItemHeight + 6, PortalRenderer.VIEWPORT_WIDTH,
                            UI_HEIGHT - 2 * this.menuItemHeight - 12);

                    int displayY;
                    if (scrolling) {
                        long currentTime = System.currentTimeMillis();
                        int elapsed = (int)(currentTime - lastScrollTime);
                        displayY = textY;
                        int scrollSteps = elapsed / 50 + 1;
                        textY -= scrollSteps;

                        if (textY + content.length * (this.textLineHeight + 2) < 0) {
                            textY = UI_HEIGHT - this.menuItemHeight;
                        }

                        int remainingDelay = scrollSteps * 50 - elapsed;
                        if (remainingDelay > 0) {
                            HelperUtils.delay(remainingDelay);
                        }
                        lastScrollTime = currentTime;
                    } else {
                        displayY = (UI_HEIGHT - (this.textLineHeight + 2) * content.length) / 2;
                    }

                    for(int i = 0; i < content.length; ++i) {
                        String line = content[i];
                        if (i == 0 && scrolling) {
                            line = line + " " + version;
                        }
                        this.drawSmallString(line, graphics,
                                (PortalRenderer.VIEWPORT_WIDTH - this.getSmallTextWidth(line)) / 2, displayY);
                        displayY += this.textLineHeight + 2;
                    }

                    this.flushScreenBuffer();
                }

                needsUpdate = scrolling;
                HelperUtils.yieldToOtherThreads();
            } while(!GameEngine.inputBack);

            GameEngine.inputBack = false;
            graphics.setClip(0, 0, PortalRenderer.VIEWPORT_WIDTH, UI_HEIGHT);

            for(int fadeStep = 8; fadeStep >= 1; --fadeStep) {
                PortalRenderer.screenBuffer[0] = 16777215 | (fadeStep * 268435456);
                HelperUtils.fastArrayFill(PortalRenderer.screenBuffer, 0, halfScreenBuffer);
                graphics.drawImage(background, 0, 0, 20);
                graphics.drawRGB(PortalRenderer.screenBuffer, 0, PortalRenderer.VIEWPORT_WIDTH,
                        0, 0, PortalRenderer.VIEWPORT_WIDTH, HALF_UI_HEIGHT, true);
                graphics.drawRGB(PortalRenderer.screenBuffer, 0, PortalRenderer.VIEWPORT_WIDTH,
                        0, HALF_UI_HEIGHT, PortalRenderer.VIEWPORT_WIDTH, HALF_UI_HEIGHT, true);
                this.flushScreenBuffer();
                HelperUtils.yieldToOtherThreads();
                HelperUtils.delay(50);
            }
        } catch (Exception e) {
        } catch (OutOfMemoryError e) {
        }

        GameEngine.inputRun = false;
        GameEngine.inputBack = false;
        GameEngine.inputFire = false;
        GameEngine.inputForward = false;
        GameEngine.inputBackward = false;
        this.smallFontImage = null;
        graphics.setClip(0, 0, PortalRenderer.VIEWPORT_WIDTH, UI_HEIGHT);
    }



    private int runMiniGameSniper(Graphics graphics, int level) {
        try {
            int bufferSize = PortalRenderer.SCREEN_BUFFER_SIZE;
            byte[] scenePixels = new byte[bufferSize];
            byte[] maskPixels = new byte[bufferSize];
            byte[] sightPixels = new byte[4096];

            int[][] var10 = new int[6][];
            int[][] var11 = new int[6][];
            int[][] var12 = new int[6][];
            int[][] var13 = new int[6][];

            // Используем методы вместо прямого доступа к полям
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
            int[][] enemySpeeds = new int[][]{
                    {1, 1, 1, 0, 0, 2, 2, 2},
                    {1, 1, 1, 1, 1, 1, 1, 1}
            };

            int[] spriteWidths = new int[]{4, 9, 14};
            int[] spriteHeights = new int[]{8, 18, 30};
            int[] hitPoints = new int[]{
                    SNIPER_DAMAGE_SMALL[GameEngine.difficultyLevel],
                    SNIPER_DAMAGE_MEDIUM[GameEngine.difficultyLevel],
                    SNIPER_DAMAGE_LARGE[GameEngine.difficultyLevel]
            };

            int[] screenOffsets = new int[]{0, 0};
            int[][] enemySprites = new int[8][];
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
                var10[i] = loadSpriteRaw("/gamedata/sniperminigame/ot8" + Integer.toString(spriteNum), flip);
                var11[i] = loadSpriteRaw("/gamedata/sniperminigame/ot18" + Integer.toString(spriteNum), flip);
                var12[i] = loadSpriteRaw("/gamedata/sniperminigame/ot30" + Integer.toString(spriteNum), flip);
                var13[i] = loadSpriteRaw("/gamedata/sniperminigame/ss30" + Integer.toString(spriteNum), flip);
            }

            Image sightImage = Image.createImage("/gamedata/sniperminigame/sight.png");

            // Загрузка ресурсов через новый метод
            sniperMiniGame.loadResources(level, PortalRenderer.VIEWPORT_WIDTH, PortalRenderer.VIEWPORT_HEIGHT,
                    scenePixels, maskPixels, sightPixels);

            // Инициализация позиции прицела через метод
            sniperMiniGame.initScopePosition();

            int[] freeSlots = new int[8];
            this.accumulatedTime = 0L;
            this.lastFrameTime = 0L;
            GameEngine.levelTransitionState = 2;

            while (this.isGameRunning) {
                if (GameEngine.levelTransitionState == 1) {
                    return -1;
                }

                int menuResult;
                if ((GameEngine.inputRun || GameEngine.inputBack || this.isGamePaused)
                        && (menuResult = this.showMenuScreen(graphics, false)) != 32) {
                    return menuResult;
                }

                long currentTime = System.currentTimeMillis();
                this.frameDeltaTime = currentTime - this.lastFrameTime;
                this.lastFrameTime = currentTime;
                this.accumulatedTime += this.frameDeltaTime;

                if (this.accumulatedTime > 600L) {
                    this.accumulatedTime = 600L;
                }

                while (this.accumulatedTime >= 40L) {
                    ++this.frameCounter;
                    // Используем новое имя метода
                    if (!sniperMiniGame.updateLogic(enemyStates, enemyTypes, enemyTimers, freeSlots,
                            enemyPositions, startPositions[level], endPositions[level], enemySpeeds[level])) {
                        return -1;
                    }
                    this.accumulatedTime -= 40L;
                }

                int totalDamage = 0;
                for (int i = 0; i < 8; ++i) {
                    if (enemyStates[i] == 4) {
                        totalDamage += hitPoints[enemySpeeds[level][i]];
                    }
                }

                // Используем геттеры вместо прямого доступа
                int[] normalPalette = sniperMiniGame.getPaletteGray();
                int[] regularPalette = sniperMiniGame.getPaletteRegular();

                if (totalDamage > 0) {
                    if (totalDamage > SNIPER_DAMAGE_SMALL[GameEngine.difficultyLevel]) {
                        if (totalDamage > SNIPER_DAMAGE_MEDIUM[GameEngine.difficultyLevel]) {
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

                // Используем геттеры для позиции прицела
                int sightX = sniperMiniGame.getScopePositionX();
                int sightY = sniperMiniGame.getScopePositionY();

                int renderStartY = sightY < 0 ? 0 : sightY;
                int renderEndY = sightY + 64;
                if (renderEndY > PortalRenderer.VIEWPORT_HEIGHT) {
                    renderEndY = PortalRenderer.VIEWPORT_HEIGHT;
                }

                int renderStartX = sightX < 0 ? 0 : sightX;
                int renderEndX = sightX + 64;
                if (renderEndX > PortalRenderer.VIEWPORT_WIDTH) {
                    renderEndX = PortalRenderer.VIEWPORT_WIDTH;
                }

                // Render background
                for (int y = renderStartY; y < renderEndY; ++y) {
                    int rowStart = renderStartX + PortalRenderer.VIEWPORT_WIDTH * y;
                    int rowEnd = renderEndX + PortalRenderer.VIEWPORT_WIDTH * y;
                    for (int idx = rowStart; idx < rowEnd; ++idx) {
                        PortalRenderer.screenBuffer[idx] = regularPalette[scenePixels[idx] & 255];
                    }
                }

                int activeEnemies = level == 0 ? 6 : 8;

                // Render enemies (first pass - behind mask)
                for (int i = 0; i < activeEnemies; ++i) {
                    if (enemyStates[i] > 0) {
                        enemySprites[i] = null;
                        int enemyX = enemyPositions[i];
                        int enemyY = yPositions[level][i];
                        int speedType = enemySpeeds[level][i];
                        int spriteW = spriteWidths[speedType];
                        int spriteH = spriteHeights[speedType];

                        switch (speedType) {
                            case 0:
                                enemySprites[i] = var10[enemyStates[i] - 1];
                                break;
                            case 1:
                                enemySprites[i] = var11[enemyStates[i] - 1];
                                break;
                            case 2:
                                enemySprites[i] = enemyTypes[i] == 0
                                        ? var12[enemyStates[i] - 1]
                                        : var13[enemyStates[i] - 1];
                                break;
                        }

                        PortalRenderer.copyToScreenBuffer(enemySprites[i], spriteW, spriteH, enemyX, enemyY, totalDamage > 0);

                        if (enemyStates[i] == 4) {
                            enemyStates[i] = (enemyPositions[i] & 1) == 1 ? 1 : 5;
                            int respawnDelay = GameEngine.random.nextInt() & Integer.MAX_VALUE;
                            enemyTimers[i] = respawnDelay % ENEMY_SPAWN_DELAY_VARIANCE[GameEngine.difficultyLevel]
                                    + ENEMY_SPAWN_DELAY_BASE[GameEngine.difficultyLevel];
                        }
                    }
                }

                // Render mask overlay
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

                // Render enemies (second pass - in front of mask)
                if (level == 0) {
                    for (int i = 6; i < 8; ++i) {
                        if (enemyStates[i] > 0) {
                            enemySprites[i] = null;
                            int enemyX = enemyPositions[i];
                            int enemyY = yPositions[level][i];
                            int speedType = enemySpeeds[level][i];
                            int spriteW = spriteWidths[speedType];
                            int spriteH = spriteHeights[speedType];

                            enemySprites[i] = enemyTypes[i] == 0
                                    ? var12[enemyStates[i] - 1]
                                    : var13[enemyStates[i] - 1];

                            PortalRenderer.copyToScreenBuffer(enemySprites[i], spriteW, spriteH, enemyX, enemyY, totalDamage > 0);

                            if (enemyStates[i] == 4) {
                                enemyStates[i] = (enemyPositions[i] & 1) == 1 ? 1 : 5;
                                int respawnDelay = GameEngine.random.nextInt() & Integer.MAX_VALUE;
                                enemyTimers[i] = respawnDelay % ENEMY_SPAWN_DELAY_VARIANCE[GameEngine.difficultyLevel]
                                        + ENEMY_SPAWN_DELAY_BASE[GameEngine.difficultyLevel];
                            }
                        }
                    }
                }

                // Render background outside sight
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
                            int idx = rowStart + x;
                            PortalRenderer.screenBuffer[idx] = normalPalette[scenePixels[idx] & 255];
                        }
                    }
                }

                int bottomStart = PortalRenderer.VIEWPORT_WIDTH * renderEndY;
                for (int i = bottomStart; i < bufferSize; ++i) {
                    PortalRenderer.screenBuffer[i] = normalPalette[scenePixels[i] & 255];
                }

                // Handle shooting
                if (GameEngine.inputFire) {
                    int centerX = sightX + 31;
                    int centerY = sightY + 31;
                    int hitColor = 16777215;
                    boolean hitEnemy = false;

                    for (int i = 7; i >= 0; --i) {
                        int enemyX = enemyPositions[i];
                        int enemyY = yPositions[level][i];
                        int speedType = enemySpeeds[level][i];
                        int spriteW = spriteWidths[speedType];
                        int spriteH = spriteHeights[speedType];

                        if ((level == 0 && (i >= 6 || speedType == 0)
                                || maskPixels[centerY * PortalRenderer.VIEWPORT_WIDTH + centerX] == -1)
                                && enemyStates[i] > 0
                                && centerX >= enemyX && centerX <= enemyX + spriteW
                                && centerY >= enemyY && centerY <= enemyY + spriteH) {

                            int spritePixelX = centerX - enemyX;
                            int spritePixelY = centerY - enemyY;

                            if (hitEnemy = level == 0 && speedType == 0 ? true
                                    : enemySprites[i][spriteW * spritePixelY + spritePixelX] != 16711935) {
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
                graphics.drawImage(this.statusBarImage, 0, PortalRenderer.VIEWPORT_HEIGHT, 0);
                this.drawHUDNumber(GameEngine.playerHealth, graphics, 58, PortalRenderer.VIEWPORT_HEIGHT + 6);
                this.drawHUDNumber(GameEngine.playerArmor, graphics, 138, PortalRenderer.VIEWPORT_HEIGHT + 6);
                this.flushScreenBuffer();

                HelperUtils.yieldToOtherThreads();
            }
        } catch (Exception e) {
        } catch (OutOfMemoryError e) {
        }

        return -1;
    }

    private void drawMultiLineMessage(Graphics graphics, String message) {
        int lineStart = 0;
        int lineCount = 0;

        int lineEnd;
        do {
            lineEnd = message.indexOf(124, lineStart);
            if (lineEnd == -1) {
                lineEnd = message.length() - 1;
            } else {
                --lineEnd;
            }
            ++lineCount;
        } while((lineStart = lineEnd + 2) < message.length());

        int textY = HALF_UI_HEIGHT - this.menuItemHeight * lineCount / 2;
        lineStart = 0;

        do {
            lineEnd = message.indexOf(124, lineStart);
            if (lineEnd == -1) {
                lineEnd = message.length() - 1;
            } else {
                --lineEnd;
            }

            String line = message.substring(lineStart, lineEnd + 1);
            int textX = (PortalRenderer.VIEWPORT_WIDTH - this.getLargeTextWidth(line)) / 2;
            this.drawLargeString(line, graphics, textX, textY);
            textY += this.menuItemHeight;
        } while((lineStart = lineEnd + 2) < message.length());
    }

    public final void stopGame() {
        if (!this.isGamePaused) {
            this.isGamePaused = true;
            if (HelperUtils.audioManager != null) {
                HelperUtils.audioManager.stopCurrentSound();
            }
        }
    }

    public final void resumeGame() {
        if (this.isGamePaused) {
            if (HelperUtils.audioManager != null && SaveSystem.musicEnabled == 1 && this.areResourcesLoaded) {
                HelperUtils.playSound(0, true, 80, 2);
            }
            this.isGamePaused = false;
        }
    }

    public void showNotify() {
        this.resumeGame();
    }

    public void hideNotify() {
        this.stopGame();
    }

    public final void stopGameLoop() {
        this.isGameRunning = false;
    }

    public final boolean gameLoopTick() {
        if (GameEngine.updateGameLogic()) {
            return true;
        }

        weaponManager.update(GameEngine.ammoCounts, GameEngine.weaponsAvailable);

        if (GameEngine.selectNextWeapon && !weaponManager.isSwitchAnimationActive()) {
            GameEngine.selectNextWeapon = false;
            weaponManager.switchToNext(GameEngine.ammoCounts, GameEngine.weaponsAvailable);
        }

        // Синхронизация с GameEngine
        GameEngine.currentWeapon = weaponManager.getCurrentWeaponId();
        GameEngine.weaponSwitchAnimationActive = weaponManager.isSwitchAnimationActive();
        GameEngine.weaponAnimationState = weaponManager.getAnimationState();
        GameEngine.weaponCooldownTimer = weaponManager.getCooldownTimer();
        weaponSpriteFrame = weaponManager.getSpriteFrame();

        // Стрельба
        if (GameEngine.inputFire && !weaponManager.isSwitchAnimationActive()) {
            int sectorType = 0;
            if (GameEngine.currentSector != null) {
                sectorType = GameEngine.currentSector.getSectorType();
            }
            weaponManager.fire(GameEngine.ammoCounts, GameEngine.weaponsAvailable,
                    GameEngine.difficultyLevel, currentLevelId, sectorType);
        } else {
            weaponManager.releaseFire(GameEngine.ammoCounts, GameEngine.weaponsAvailable,
                    GameEngine.difficultyLevel);
        }

        // Сброс флага
        Weapon currentWeapon = weaponManager.getCurrentWeapon();
        if (!currentWeapon.getIsAutomatic() || GameEngine.inputStrafe) {
            GameEngine.inputFire = false;
        }

        return false;
    }

    private void initializeGameResources() {
        try {
            statusBarImage = Image.createImage("/gamedata/sprites/bar.png");
            crosshairImage = Image.createImage("/gamedata/sprites/aim.png");
            largeFontImage = Image.createImage("/gamedata/sprites/font.png");

            weaponManager.initialize();

            MathUtils.initializeMathTables();
            GameEngine.initializeEngine();
        } catch (Exception e) {
        } catch (OutOfMemoryError e) {
        }
    }

    private void loadLevelResources() {
        try {
            HelperUtils.freeMemory();
            String fullLevelPath = LEVEL_PATH_PREFIX + LEVEL_FILE_NAMES[currentLevelId];
            if (previousLevelId < currentLevelId) {
                if (previousLevelId > -1) {
                    this.cachedStaticObjects = LevelLoader.gameWorld.staticObjects;
                }

                if (!LevelLoader.loadMapData(LEVEL_PATH_PREFIX + LEVEL_FILE_NAMES[currentLevelId],
                        this.nextLevelObjects == null)) {
                    CovertOps3D.exitApplication();
                }

                if (this.nextLevelObjects != null) {
                    LevelLoader.gameWorld.staticObjects = this.nextLevelObjects;
                    this.nextLevelObjects = null;
                } else {
                    GameEngine.keysCollected[0] = false;
                    GameEngine.keysCollected[1] = false;
                }
            } else {
                this.nextLevelObjects = LevelLoader.gameWorld.staticObjects;
                if (!LevelLoader.loadMapData(fullLevelPath,
                        this.cachedStaticObjects == null)) {
                    CovertOps3D.exitApplication();
                }

                if (this.cachedStaticObjects != null) {
                    LevelLoader.gameWorld.staticObjects = this.cachedStaticObjects;
                    this.cachedStaticObjects = null;
                }
            }

            HelperUtils.freeMemory();
            GameEngine.resetLevelState();

            LevelLoader.preloadTexture((byte)25);

            byte[] var2 = new byte[]{-23, -25, -28, -30, -32, 0, 0};
            byte[] var3 = new byte[]{-24, -26, -29, -31, -33, -34, -27};
            byte[] var4 = new byte[]{-35, -36, -38, -39, -40, 0, 0};
            byte[] var5 = new byte[]{-86, -88, -91, -93, -95, 0, 0};
            byte[] var6 = new byte[]{-87, -89, -92, -94, -96, -97, -90};
            byte[] var7 = new byte[]{-73, -75, -78, -80, -82, 0, 0};
            byte[] var8 = new byte[]{-74, -76, -79, -81, -83, -84, -77};
            byte[] var9 = new byte[]{-2, -1};
            byte[] var10 = new byte[]{-3, 0};
            byte[] var11 = new byte[]{-59, -61, -64, -66, -68, 0, 0};
            byte[] var12 = new byte[]{-60, -62, -65, -67, -69, -70, -63};
            byte[] var13 = new byte[]{-9};
            byte[] var14 = new byte[]{-10};
            byte[] var15 = new byte[]{-4, -6, -11, -13, 0, 0};
            byte[] var16 = new byte[]{-5, -7, -12, -14, -15, -8};

            GameObject[] objects = LevelLoader.gameWorld.staticObjects;

            for(int i = 0; i < objects.length; ++i) {
                GameObject obj = objects[i];
                if (obj != null) {
                    GameObject target;

                    switch(obj.objectType) {
                        case 5:
                        case 13:
                            obj.addSpriteFrame((byte)0, (byte)-53);
                            LevelLoader.preloadTexture((byte)-53);
                            continue;
                        case 10:
                            HelperUtils.preloadObjectTextures(obj, var9, var10);
                            if (LEVEL_FILE_NAMES[currentLevelId] == "06c") {
                                obj.spriteFrameIndex = 1;
                            }
                            continue;
                        case 12:
                            HelperUtils.preloadObjectTextures(obj, var13, var14);
                            continue;
                        case 26:
                            obj.addSpriteFrame((byte)0, (byte)-16);
                            LevelLoader.preloadTexture((byte)-16);
                            continue;
                        case 60:
                            obj.addSpriteFrame((byte)0, (byte)-18);
                            LevelLoader.preloadTexture((byte)-18);
                            continue;
                        case 61:
                            obj.addSpriteFrame((byte)0, (byte)-17);
                            LevelLoader.preloadTexture((byte)-17);
                            continue;
                        case 82:
                            obj.addSpriteFrame((byte)0, (byte)-21);
                            LevelLoader.preloadTexture((byte)-21);
                            continue;
                        case 2001:
                            obj.addSpriteFrame((byte)0, (byte)-19);
                            LevelLoader.preloadTexture((byte)-19);
                            continue;
                        case 2002:
                            obj.addSpriteFrame((byte)0, (byte)-20);
                            LevelLoader.preloadTexture((byte)-20);
                            continue;
                        case 2003:
                            obj.addSpriteFrame((byte)0, (byte)-22);
                            LevelLoader.preloadTexture((byte)-22);
                            continue;
                        case 2004:
                            obj.addSpriteFrame((byte)0, (byte)-43);
                            LevelLoader.preloadTexture((byte)-43);
                            continue;
                        case 2005:
                            obj.addSpriteFrame((byte)0, (byte)-50);
                            LevelLoader.preloadTexture((byte)-50);
                            continue;
                        case 2006:
                            obj.addSpriteFrame((byte)0, (byte)-72);
                            LevelLoader.preloadTexture((byte)-72);
                            continue;
                        case 2007:
                            target = obj;
                            break;
                        case 2008:
                            obj.addSpriteFrame((byte)0, (byte)-54);
                            LevelLoader.preloadTexture((byte)-54);
                            continue;
                        case 2010:
                            obj.addSpriteFrame((byte)0, (byte)-57);
                            LevelLoader.preloadTexture((byte)-57);
                            continue;
                        case 2012:
                            obj.addSpriteFrame((byte)0, (byte)-55);
                            LevelLoader.preloadTexture((byte)-55);
                            continue;
                        case 2013:
                            obj.addSpriteFrame((byte)0, (byte)-49);
                            LevelLoader.preloadTexture((byte)-49);
                            continue;
                        case 2014:
                            obj.addSpriteFrame((byte)0, (byte)-52);
                            LevelLoader.preloadTexture((byte)-52);
                            continue;
                        case 2015:
                            obj.addSpriteFrame((byte)0, (byte)-58);
                            LevelLoader.preloadTexture((byte)-58);
                            continue;
                        case 2024:
                            obj.addSpriteFrame((byte)0, (byte)-85);
                            LevelLoader.preloadTexture((byte)-85);
                            continue;
                        case 2047:
                            obj.addSpriteFrame((byte)0, (byte)-56);
                            LevelLoader.preloadTexture((byte)-56);
                            continue;
                        case 3001:
                            HelperUtils.preloadObjectTextures(obj, var11, var12);
                            LevelLoader.preloadTexture((byte)-57);
                            continue;
                        case 3002:
                            HelperUtils.preloadObjectTextures(obj, var15, var16);
                            LevelLoader.preloadTexture((byte)-56);
                            continue;
                        case 3003:
                            HelperUtils.preloadObjectTextures(obj, var2, var3);
                            LevelLoader.preloadTexture((byte)-48);
                            continue;
                        case 3004:
                            HelperUtils.preloadObjectTextures(obj, var5, var6);
                            LevelLoader.preloadTexture((byte)-54);
                            continue;
                        case 3005:
                            HelperUtils.preloadObjectTextures(obj, var4, var6);
                            LevelLoader.preloadTexture((byte)-48);
                            continue;
                        case 3006:
                            HelperUtils.preloadObjectTextures(obj, var7, var8);
                            LevelLoader.preloadTexture((byte)-54);
                            continue;
                        default:
                            target = obj;
                    }

                    target.addSpriteFrame((byte)0, (byte)-48);
                    LevelLoader.preloadTexture((byte)-48);
                }
            }

            LevelLoader.preloadTexture((byte)-44);
            LevelLoader.preloadTexture((byte)-45);
            LevelLoader.preloadTexture((byte)-46);
            LevelLoader.preloadTexture((byte)-47);
            LevelLoader.preloadTexture((byte)-71);
            LevelLoader.preloadTexture((byte)-51);
            LevelLoader.preloadTexture((byte)-43);

            if (currentLevelId == 10) {
                LevelLoader.preloadTexture((byte)-72);
            }

            HelperUtils.freeMemory();

            if (!LevelLoader.loadGameAssets("/gamedata/textures/tx", 4, "/gamedata/textures/sp", 4)) {
                CovertOps3D.exitApplication();
            }

            GameEngine.changeSkyboxTexture((byte)25);
            HelperUtils.freeMemory();
        } catch (Exception e) {
        } catch (OutOfMemoryError e) {
        }
    }

    private void drawPleaseWait(Graphics graphics) {
        String text = TextStrings.PLEASE_WAIT;
        int textX = (PortalRenderer.VIEWPORT_WIDTH - this.getLargeTextWidth(text)) / 2;
        int textY = HALF_UI_HEIGHT - this.menuItemHeight / 2;

        int halfScreenBuffer = PortalRenderer.VIEWPORT_WIDTH * HALF_UI_HEIGHT;
        PortalRenderer.screenBuffer[0] = Integer.MIN_VALUE;
        HelperUtils.fastArrayFill(PortalRenderer.screenBuffer, 0, halfScreenBuffer);

        graphics.drawRGB(PortalRenderer.screenBuffer, 0, PortalRenderer.VIEWPORT_WIDTH,
                0, 0, PortalRenderer.VIEWPORT_WIDTH, HALF_UI_HEIGHT, true);
        graphics.drawRGB(PortalRenderer.screenBuffer, 0, PortalRenderer.VIEWPORT_WIDTH,
                0, HALF_UI_HEIGHT, PortalRenderer.VIEWPORT_WIDTH, HALF_UI_HEIGHT, true);

        this.drawLargeString(text, graphics, textX, textY);
        this.flushScreenBuffer();
    }

    private int drawDialogOverlay(Graphics graphics, int dialogId) {
        try {
            int menuHeight = this.menuItemHeight + 6;
            Image frameBuffer = Image.createImage(PortalRenderer.VIEWPORT_WIDTH, UI_HEIGHT);
            Image background = Image.createImage("/gamedata/sprites/bkg_cut.png");
            Image playerPortrait = Image.createImage("/gamedata/sprites/player.png");
            Image agentPortrait = dialogId != 0 && dialogId != 9
                    ? Image.createImage(dialogId == 8
                    ? "/gamedata/sprites/ag_hurt.png"
                    : "/gamedata/sprites/ag.png")
                    : null;
            Image doctorPortrait = dialogId == 7
                    ? Image.createImage("/gamedata/sprites/doctor.png")
                    : null;

            this.smallFontImage = Image.createImage("/gamedata/sprites/font_cut.png");

            int textAreaWidth = PortalRenderer.VIEWPORT_WIDTH - playerPortrait.getWidth() - 6;
            Graphics fbGraphics = frameBuffer.getGraphics();
            fbGraphics.setColor(16711680);
            fbGraphics.drawImage(background, 0, 0, 20);
            fbGraphics.drawImage(playerPortrait, 2, 2, 20);

            int agentY = HALF_UI_HEIGHT + 2;
            int doctorY = 2 * (UI_HEIGHT - menuHeight) / 3 + 2;
            int linesPerBox = (316 - menuHeight) / this.textLineHeight;

            int[][] lineBuffers = new int[3][];
            int[] lineIndices = new int[]{0, 0, 0};
            String[] currentText = new String[3];

            if (agentPortrait != null) {
                if (doctorPortrait != null) {
                    agentY = (UI_HEIGHT - menuHeight) / 3 + 2;
                    fbGraphics.drawImage(agentPortrait,
                            PortalRenderer.VIEWPORT_WIDTH - 2 - agentPortrait.getWidth(), agentY, 20);
                    fbGraphics.drawImage(doctorPortrait,
                            PortalRenderer.VIEWPORT_WIDTH - 2 - doctorPortrait.getWidth(), doctorY, 20);
                    linesPerBox = (316 - menuHeight) / (this.textLineHeight * 3);
                    lineBuffers[1] = new int[linesPerBox];
                    lineBuffers[2] = new int[linesPerBox];
                } else {
                    fbGraphics.drawImage(agentPortrait,
                            PortalRenderer.VIEWPORT_WIDTH - 2 - agentPortrait.getWidth(), agentY, 20);
                    linesPerBox = (316 - menuHeight) / (this.textLineHeight * 2);
                    lineBuffers[1] = new int[linesPerBox];
                }
            }

            lineBuffers[0] = new int[linesPerBox];
            this.drawStripedBackground(graphics, frameBuffer);
            this.drawLargeString(TextStrings.BACK, graphics,
                    PortalRenderer.VIEWPORT_WIDTH - this.getLargeTextWidth(TextStrings.BACK) - 3, UI_HEIGHT - this.menuItemHeight - 3);
            this.drawLargeString(TextStrings.PAUSE, graphics, 3, UI_HEIGHT - this.menuItemHeight - 3);

            int[] charIndices = new int[]{0, 0, 0};
            int[] boxStartX = new int[]{0, 0, 0};
            int[] boxStartY = new int[]{0, 0, 0};
            int[] boxEndY = new int[]{0, 0, 0};
            long[] fadeTimers = new long[]{0L, 0L, 0L};
            boolean[] needsFade = new boolean[]{false, false, false};

            for(int lineNum = 0; lineNum < TextStrings.storyText[dialogId].length; ++lineNum) {
                String line = TextStrings.storyText[dialogId][lineNum];
                int textX = playerPortrait.getWidth() + 4;
                int textY = 2;
                byte boxId = 0;

                if (line.startsWith("A")) {
                    textX = 2;
                    textY = agentY;
                    boxId = 1;
                } else if (line.startsWith("M")) {
                    textX = 2;
                    textY = doctorY;
                    boxId = 2;
                }

                boxStartX[boxId] = textX;
                boxStartY[boxId] = textY;
                boxEndY[boxId] = textY + linesPerBox * this.textLineHeight;

                int maxX = textX + textAreaWidth;
                int maxY = textY + linesPerBox * this.textLineHeight;
                int currentX = textX;
                int currentY = textY;

                charIndices[boxId] = 0;
                currentText[boxId] = line;
                HelperUtils.delay(500);

                if (needsFade[boxId]) {
                    needsFade[boxId] = false;
                    int oldColor = graphics.getColor();
                    graphics.setColor(0);
                    graphics.fillRect(boxStartX[boxId], boxStartY[boxId], textAreaWidth, boxEndY[boxId] - boxStartY[boxId]);
                    graphics.setColor(oldColor);
                    graphics.drawRegion(frameBuffer, boxStartX[boxId], boxStartY[boxId],
                            textAreaWidth, boxEndY[boxId] - boxStartY[boxId],
                            0, boxStartX[boxId], boxStartY[boxId], 20);
                }

                lineIndices[boxId] = 0;
                lineBuffers[boxId][lineIndices[boxId]] = 1;

                for(int charPos = 1; charPos < line.length(); ++charPos) {
                    char c = line.charAt(charPos);

                    if (c == ' ') {
                        if (currentX + this.spaceWidth > maxX) {
                            currentX = textX;
                            if (lineIndices[boxId] >= linesPerBox - 1) {
                                this.drawWrappedLine(graphics, frameBuffer, line, lineBuffers[boxId], charPos + 1,
                                        textX, textY, maxY, textAreaWidth);
                            } else {
                                currentY += this.textLineHeight;
                                ++lineIndices[boxId];
                                lineBuffers[boxId][lineIndices[boxId]] = charPos + 1;
                            }
                        } else {
                            int nextSpace = line.indexOf(32, charPos + 1);
                            if (nextSpace == -1) {
                                nextSpace = line.length();
                            }

                            String word = line.substring(charPos, nextSpace);
                            int wordWidth = this.getSmallTextWidth(word);

                            if (currentX + this.spaceWidth + wordWidth > maxX) {
                                currentX = textX;
                                if (lineIndices[boxId] >= linesPerBox - 1) {
                                    this.drawWrappedLine(graphics, frameBuffer, line, lineBuffers[boxId], charPos + 1,
                                            textX, textY, maxY, textAreaWidth);
                                } else {
                                    currentY += this.textLineHeight;
                                    ++lineIndices[boxId];
                                    lineBuffers[boxId][lineIndices[boxId]] = charPos + 1;
                                }
                            } else {
                                currentX += this.spaceWidth;
                            }
                        }
                    } else {
                        int[] fontCoords = this.getFontCoordinates(c);
                        int fontIdx = fontCoords[1] * this.smallFontCharsPerRow + fontCoords[0];
                        int charWidth = this.fontTextureW[fontIdx];
                        int charX = this.fontTextureX[fontIdx];
                        int charY = fontCoords[1] * this.textLineHeight;

                        if (currentX + charWidth + 1 > maxX) {
                            currentX = textX;
                            if (lineIndices[boxId] >= linesPerBox - 1) {
                                this.drawWrappedLine(graphics, frameBuffer, line, lineBuffers[boxId], charPos,
                                        textX, textY, maxY, textAreaWidth);
                            } else {
                                currentY += this.textLineHeight;
                                ++lineIndices[boxId];
                                lineBuffers[boxId][lineIndices[boxId]] = charPos;
                            }
                        }

                        graphics.drawRegion(this.smallFontImage, charX, charY, charWidth, this.textLineHeight,
                                0, currentX, currentY, 20);
                        currentX += charWidth + 1;
                    }

                    this.flushScreenBuffer();
                    HelperUtils.delay(c == ',' ? 300 : (c != '.' && c != '?' && c != '!' ? 50 : 400));

                    long currentTime = System.currentTimeMillis();
                    for(int b = 0; b < 3; ++b) {
                        if (needsFade[b] && currentTime > fadeTimers[b]) {
                            needsFade[b] = false;
                            int oldColor = graphics.getColor();
                            graphics.setColor(0);
                            graphics.fillRect(boxStartX[b], boxStartY[b], textAreaWidth, boxEndY[b] - boxStartY[b]);
                            graphics.setColor(oldColor);
                            graphics.drawRegion(frameBuffer, boxStartX[b], boxStartY[b],
                                    textAreaWidth, boxEndY[b] - boxStartY[b],
                                    0, boxStartX[b], boxStartY[b], 20);
                            lineIndices[b] = 0;
                            currentText[b] = null;
                        }
                    }

                    if (GameEngine.inputRun) {
                        GameEngine.inputRun = false;
                        graphics.drawRegion(frameBuffer, 3, UI_HEIGHT - this.menuItemHeight - 3,
                                this.getLargeTextWidth(TextStrings.PAUSE), this.menuItemHeight,
                                0, 3, UI_HEIGHT - this.menuItemHeight - 3, 20);
                        this.drawLargeString(TextStrings.RESUME, graphics, 3, UI_HEIGHT - this.menuItemHeight - 3);
                        this.flushScreenBuffer();

                        while(!GameEngine.inputRun && !GameEngine.inputBack
                                && !this.isGamePaused && !GameEngine.inputFire) {
                            HelperUtils.yieldToOtherThreads();
                        }
                    }

                    if (GameEngine.inputRun) {
                        graphics.drawRegion(frameBuffer, 3, UI_HEIGHT - this.menuItemHeight - 3,
                                this.getLargeTextWidth(TextStrings.RESUME), this.menuItemHeight,
                                0, 3, UI_HEIGHT - this.menuItemHeight - 3, 20);
                        this.drawLargeString(TextStrings.PAUSE, graphics, 3, UI_HEIGHT - this.menuItemHeight - 3);
                        this.flushScreenBuffer();
                        GameEngine.inputRun = false;
                    }

                    if (GameEngine.inputBack || this.isGamePaused) {
                        GameEngine.inputRun = false;
                        GameEngine.inputBack = false;
                        int menuResult = this.showMenuScreen(graphics, false);
                        if (menuResult != 32) {
                            this.smallFontImage = null;
                            return menuResult;
                        }

                        graphics.drawImage(frameBuffer, 0, 0, 20);
                        this.drawLargeString(TextStrings.BACK, graphics,
                                PortalRenderer.VIEWPORT_WIDTH - this.getLargeTextWidth(TextStrings.BACK) - 3,
                                UI_HEIGHT - this.menuItemHeight - 3);
                        this.drawLargeString(TextStrings.PAUSE, graphics, 3, UI_HEIGHT - this.menuItemHeight - 3);

                        for(int b = 0; b < 3; ++b) {
                            int endChar = b == boxId ? charPos :
                                    (currentText[b] != null ? currentText[b].length() : 0);

                            if (lineBuffers[b] != null) {
                                int startX = 0;
                                int startY = 0;

                                switch(b) {
                                    case 0:
                                        startX = playerPortrait.getWidth() + 4;
                                        startY = 2;
                                        break;
                                    case 1:
                                        startX = 2;
                                        startY = agentY;
                                        break;
                                    case 2:
                                        startX = 2;
                                        startY = doctorY;
                                        break;
                                }

                                for(int lineIdx = 0; lineIdx <= lineIndices[b]; ++lineIdx) {
                                    int lineStart = lineBuffers[b][lineIdx];
                                    int lineEnd = lineIdx + 1 <= lineIndices[b]
                                            ? lineBuffers[b][lineIdx + 1]
                                            : (currentText[b] != null ? currentText[b].length() : 0);

                                    if (endChar + 1 < lineEnd) {
                                        lineEnd = endChar + 1;
                                    }

                                    int renderX = startX;
                                    for(int pos = lineStart; pos < lineEnd; ++pos) {
                                        char ch = currentText[b].charAt(pos);

                                        if (ch == ' ') {
                                            renderX += this.spaceWidth;
                                        } else {
                                            int[] fCoords = this.getFontCoordinates(ch);
                                            int fIdx = fCoords[1] * this.smallFontCharsPerRow + fCoords[0];
                                            int cWidth = this.fontTextureW[fIdx];
                                            int cX = this.fontTextureX[fIdx];
                                            int cY = fCoords[1] * this.textLineHeight;
                                            graphics.drawRegion(this.smallFontImage, cX, cY, cWidth, this.textLineHeight,
                                                    0, renderX, startY, 20);
                                            renderX += cWidth + 1;
                                        }
                                    }
                                    startY += this.textLineHeight;
                                }
                            }
                        }

                        this.flushScreenBuffer();
                    }

                    if (GameEngine.inputFire) {
                        GameEngine.inputFire = false;
                        this.smallFontImage = null;
                        return -1;
                    }

                    HelperUtils.yieldToOtherThreads();
                }

                charIndices[boxId] = 0;
                needsFade[boxId] = true;
                boxStartX[boxId] = textX;
                boxStartY[boxId] = textY;
                boxEndY[boxId] = maxY;
                fadeTimers[boxId] = System.currentTimeMillis() + 5000L;
                HelperUtils.delay(500);
            }

            HelperUtils.delay(5000);
            this.smallFontImage = null;
        } catch (Exception e) {
        } catch (OutOfMemoryError e) {
        }

        return -1;
    }

    private void drawWrappedLine(Graphics graphics, Image frameBuffer, String text, int[] lineStarts,
                                 int startChar, int startX, int startY, int maxY, int width) {

        int height = maxY - startY;
        int oldColor = graphics.getColor();
        graphics.setColor(0);
        graphics.fillRect(startX, startY, width, height);
        graphics.setColor(oldColor);
        graphics.drawRegion(frameBuffer, startX, startY, width, height, 0, startX, startY, 20);

        int renderY = startY;

        for(int lineIdx = 1; lineIdx < lineStarts.length; ++lineIdx) {
            int lineStart = lineStarts[lineIdx];
            lineStarts[lineIdx - 1] = lineStart;
            int lineEnd = lineIdx + 1 < lineStarts.length
                    ? lineStarts[lineIdx + 1]
                    : startChar;

            if (lineIdx > 1) {
                int oldColor2 = graphics.getColor();
                graphics.setColor(0);
                graphics.fillRect(startX, renderY, width, this.textLineHeight);
                graphics.setColor(oldColor2);
                graphics.drawRegion(frameBuffer, startX, renderY, width, this.textLineHeight,
                        0, startX, renderY, 20);
            }

            int renderX = startX;
            for(int charIdx = lineStart; charIdx < lineEnd; ++charIdx) {
                char c = text.charAt(charIdx);

                if (c == ' ') {
                    renderX += this.spaceWidth;
                } else {
                    int[] coords = this.getFontCoordinates(c);
                    int fontIdx = coords[1] * this.smallFontCharsPerRow + coords[0];
                    int charW = this.fontTextureW[fontIdx];
                    int charX = this.fontTextureX[fontIdx];
                    int charY = coords[1] * this.textLineHeight;
                    graphics.drawRegion(this.smallFontImage, charX, charY, charW, this.textLineHeight,
                            0, renderX, renderY, 20);
                    renderX += charW + 1;
                }
            }
            renderY += this.textLineHeight;
        }

        lineStarts[lineStarts.length - 1] = startChar;
    }

    private void drawHUDNumber(int value, Graphics graphics, int x, int y) {
        String text = Integer.toString(value);
        int centerOffset = this.getLargeTextWidth(text) / 2;
        this.drawLargeString(text, graphics, x - centerOffset, y);
    }

    private int getLargeTextWidth(String text) {
        text = text.toLowerCase();
        int width = 0;

        for(int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            if (c == ' ') {
                width += this.menuBoxWidthUnit;
            } else {
                int[] coords = this.getFontCharCoordinates(c);
                int charWidth = this.fontCharWidths[coords[1] * this.menuOffsetY + coords[0]];
                width += charWidth;
            }
        }

        return width;
    }

    private int getSmallTextWidth(String text) {
        int width = 0;

        for(int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            if (c == ' ') {
                width += this.spaceWidth;
            } else {
                int[] coords = this.getFontCoordinates(c);
                int charWidth = this.fontTextureW[coords[1] * this.smallFontCharsPerRow + coords[0]];
                width += charWidth + 1;
            }
        }

        return width;
    }

    private int[] getFontCharCoordinates(char c) {
        int[] coords = new int[]{this.menuOffsetY - 1, 2};

        if (c >= 'a' && c <= 'r') {
            coords[0] = c - 97;
            coords[1] = 0;
        } else if (c >= 's' && c <= 'z') {
            coords[0] = c - 115;
            coords[1] = 1;
        } else if (c >= '0' && c <= '9') {
            coords[0] = c - 48;
            coords[1] = 2;
        } else {
            coords[1] = 1;
            switch(c) {
                case '!':
                    coords[0] = 10;
                    break;
                case '\'':
                    coords[0] = 15;
                    break;
                case ',':
                    coords[0] = 9;
                    break;
                case '.':
                    coords[0] = 8;
                    break;
                case '/':
                    coords[0] = 14;
                    break;
                case ':':
                    coords[0] = 12;
                    break;
                case ';':
                    coords[0] = 13;
                    break;
                case '?':
                    coords[0] = 11;
                    break;
                default:
                    return coords;
            }
        }

        return coords;
    }

    private int[] getFontCoordinates(char character) {
        int[] coords = new int[]{this.smallFontCharsPerRow - 1, 2};

        if (character >= 'A' && character <= 'Z') {
            coords[0] = character - 65;
            coords[1] = 0;
        } else if (character >= 'a' && character <= 'z') {
            coords[0] = character - 97;
            coords[1] = 1;
        } else if (character >= '0' && character <= '9') {
            coords[0] = character - 48;
            coords[1] = 2;
        } else {
            coords[1] = 2;
            switch(character) {
                case '!':
                    coords[0] = 12;
                    break;
                case '\'':
                    coords[0] = 18;
                    break;
                case ',':
                    coords[0] = 11;
                    break;
                case '-':
                    coords[0] = 17;
                    break;
                case '.':
                    coords[0] = 10;
                    break;
                case '/':
                    coords[0] = 16;
                    break;
                case ':':
                    coords[0] = 14;
                    break;
                case ';':
                    coords[0] = 15;
                    break;
                case '?':
                    coords[0] = 13;
                    break;
                case '@':
                    coords[0] = 19;
                    break;
                default:
                    return coords;
            }
        }

        return coords;
    }

    private void drawLargeString(String text, Graphics graphics, int x, int y) {
        text = text.toLowerCase();

        for(int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);

            if (c == ' ') {
                x += this.menuBoxWidthUnit;
            } else {
                int[] coords = this.getFontCharCoordinates(c);
                int fontIdx = coords[1] * this.menuOffsetY + coords[0];
                int charWidth = this.fontCharWidths[fontIdx];
                int charX = this.fontCharOffsets[fontIdx];
                int charY = coords[1] * this.menuItemHeight;
                graphics.drawRegion(this.largeFontImage, charX, charY, charWidth, this.menuItemHeight,
                        0, x, y, 20);
                x += charWidth;
            }
        }
    }

    private void drawSmallString(String text, Graphics graphics, int x, int y) {
        for(int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);

            if (c == ' ') {
                x += this.spaceWidth;
            } else {
                int[] coords = this.getFontCoordinates(c);
                int fontIdx = coords[1] * this.smallFontCharsPerRow + coords[0];
                int charWidth = this.fontTextureW[fontIdx];
                int charX = this.fontTextureX[fontIdx];
                int charY = coords[1] * this.textLineHeight;
                graphics.drawRegion(this.smallFontImage, charX, charY, charWidth, this.textLineHeight,
                        0, x, y, 20);
                x += charWidth + 1;
            }
        }
    }

    private void flushScreenBuffer() {
        this.flushGraphics();
    }

}