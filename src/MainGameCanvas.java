import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.game.GameCanvas;

/**
 * Main game canvas - refactored to remove god-class anti-pattern.
 * Now acts as thin coordinator delegating to specialized subsystems.
 */
public class MainGameCanvas extends GameCanvas implements Runnable {

    public static final int UI_HEIGHT = 320;
    public static final int STATUS_BAR_HEIGHT = 32;
    public static final int HALF_UI_HEIGHT = UI_HEIGHT / 2;

    public static final String LEVEL_PATH_PREFIX = "/gamedata/levels/level_";
    public static final String[] LEVEL_FILE_NAMES = new String[]{
            "01a", "01b", "02a", "02b", "04", "05", "06a", "06b", "06c", "07a", "07b", "08a", "08b"
    };

    public static WeaponManager weaponManager;
    public static int weaponSpriteFrame = 0;

    private final SniperMiniGame sniperMiniGame = new SniperMiniGame();
    private SniperGameController sniperController;

    public boolean isGameRunning = false;
    public boolean isGamePaused = false;
    public boolean isGameInitialized = true;
    public boolean areResourcesLoaded = false;

    public static CovertOps3D mainMidlet = null;

    public static int currentLevelId = 0;
    public static int previousLevelId = -1;
    public static int keyMappingOffset;

    public String[] chapterMenuItems;

    public long frameDeltaTime;
    public long accumulatedTime;
    public long lastFrameTime;
    public int frameCounter;

    private final FontRenderer fontRenderer = new FontRenderer();

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

    private InputManager inputManager;
    private HudRenderer hudRenderer;
    private MenuSystem menuSystem;
    private DialogSystem dialogSystem;
    private LevelResourceManager levelResourceManager;

    public MainGameCanvas() {
        super(false);
        this.frameDeltaTime = 0L;
        this.accumulatedTime = 0L;
        this.lastFrameTime = 0L;
        this.frameCounter = 0;

        weaponManager = new WeaponManager();
        keyMappingOffset = Math.abs(this.getKeyCode(8)) == 53 ? 5 : Math.abs(this.getKeyCode(8));

        this.inputManager = new InputManager(this);
        this.hudRenderer = new HudRenderer(fontRenderer);
        this.menuSystem = new MenuSystem(fontRenderer, this);
        this.dialogSystem = new DialogSystem(fontRenderer, this);
        this.levelResourceManager = new LevelResourceManager(this, fontRenderer, weaponManager);

        this.setFullScreenMode(true);
    }

    public void sizeChanged(int width, int height) {}

    private int translateKeyCode(int keyCode) {
        return inputManager.translateKeyCode(keyCode);
    }

    public void keyPressed(int keyCode) {
        inputManager.handleKeyPressed(keyCode);
    }

    public void keyReleased(int keyCode) {
        inputManager.handleKeyReleased(keyCode);
    }

    private void renderHUDAndWeapon(Graphics graphics) {
        try {
            hudRenderer.renderFrameWithHud(graphics, frameCounter, weaponManager);
        } catch (Exception e) {
            DebugLogger.logException("MainGameCanvas.renderHUD", e);
        } catch (OutOfMemoryError e) {
            DebugLogger.logOutOfMemory("MainGameCanvas.renderHUD", e);
        }
    }

    private void drawSplash(Graphics g) {
        try { dialogSystem.drawSplash(g); }
        catch (Exception e) { DebugLogger.logException("drawSplash", e); }
        catch (OutOfMemoryError e) { DebugLogger.logOutOfMemory("drawSplash", e); }
    }
    private void drawGameOver(Graphics g) {
        try { dialogSystem.drawGameOver(g); }
        catch (Exception e) { DebugLogger.logException("drawGameOver", e); }
        catch (OutOfMemoryError e) { DebugLogger.logOutOfMemory("drawGameOver", e); }
    }
    private void drawPleaseWait(Graphics g) {
        try { dialogSystem.drawPleaseWait(g); }
        catch (Exception e) { DebugLogger.logException("drawPleaseWait", e); }
        catch (OutOfMemoryError e) { DebugLogger.logOutOfMemory("drawPleaseWait", e); }
    }
    private void drawMultiLineMessage(Graphics g, String msg) {
        try { dialogSystem.drawMultiLineMessage(g, msg); }
        catch (Exception e) { DebugLogger.logException("drawMultiLine", e); }
        catch (OutOfMemoryError e) { DebugLogger.logOutOfMemory("drawMultiLine", e); }
    }

    private int showMenuScreen(Graphics g, boolean isMain) {
        try {
            return menuSystem.showMenuScreen(g, isMain, null);
        } catch (Exception e) {
            DebugLogger.logException("showMenuScreen", e);
            return 4;
        } catch (OutOfMemoryError e) {
            DebugLogger.logOutOfMemory("showMenuScreen", e);
            return 4;
        }
    }

    private int drawDialogOverlay(Graphics g, int dialogId) {
        try {
            return dialogSystem.drawDialogOverlay(g, dialogId);
        } catch (Exception e) {
            DebugLogger.logException("drawDialogOverlay", e);
            return -1;
        } catch (OutOfMemoryError e) {
            DebugLogger.logOutOfMemory("drawDialogOverlay", e);
            return -1;
        }
    }

    private int runMiniGameSniper(Graphics g, int level) {
        try {
            if (sniperController == null) {
                sniperController = new SniperGameController(this, sniperMiniGame, fontRenderer, hudRenderer.getStatusBarImage());
            }
            return sniperController.runSniperGame(g, level);
        } catch (Exception e) {
            DebugLogger.logException("runMiniGameSniper", e);
            return -1;
        } catch (OutOfMemoryError e) {
            DebugLogger.logOutOfMemory("runMiniGameSniper", e);
            return -1;
        }
    }

    public final void startGameThread() {
        Thread gameThread = new Thread(this);
        this.isGameRunning = true;
        this.isGameInitialized = false;
        gameThread.start();
    }

    public void run() {
        try {
            DebugLogger.log("MainGameCanvas", "run start");
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
            DebugLogger.log("MainGameCanvas", "splash");
            drawSplash(graphics);
            DebugLogger.log("MainGameCanvas", "initResources");
            initializeGameResources();
            DebugLogger.log("MainGameCanvas", "loadSave");
            SaveSystem.loadSaveData();
            SaveSystem.loadSettingsFromRMS();
            this.areResourcesLoaded = true;
            sniperController = new SniperGameController(this, sniperMiniGame, fontRenderer, hudRenderer.getStatusBarImage());

            DebugLogger.log("MainGameCanvas", "show main menu");
            int menuResult = showMenuScreen(graphics, true);

            outerGameLoop:
            while (true) {
                while (true) {
                    do {
                        if (!isGameRunning) {
                            isGameInitialized = true;
                            DebugLogger.log("MainGameCanvas", "isGameRunning false exit");
                            return;
                        }
                    } while (isGamePaused);

                    if (menuResult == 4) {
                        isGameInitialized = true;
                        CovertOps3D.exitApplication();
                        return;
                    }

                    GameEngine.resetPlayerProgress();
                    if (menuResult == 66) {
                        currentLevelId = 0;
                        previousLevelId = -1;
                        if ((menuResult = drawDialogOverlay(graphics, 0)) != -1) {
                            continue;
                        }
                        DebugLogger.log("MainGameCanvas", "load level 0");
                        drawPleaseWait(graphics);
                        loadLevelResources();
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

                accumulatedTime = 0L;
                lastFrameTime = 0L;

                while (isGameRunning) {
                    try {
                        if ((GameEngine.inputRun || GameEngine.inputBack || isGamePaused)
                                && (menuResult = showMenuScreen(graphics, false)) != 32) {
                            break;
                        }

                        boolean needLoad = false;

                        if (GameEngine.levelTransitionState == 1) {
                            switch (currentLevelId) {
                                case 0:
                                case 13:
                                    currentLevelId = 0;
                                    SaveSystem.saveGameState(8);
                                    if ((menuResult = drawDialogOverlay(graphics, 9)) == -1) {
                                        menuResult = showMenuScreen(graphics, true);
                                    }
                                    continue outerGameLoop;
                                case 1: case 3: case 8: case 10: case 11: case 12:
                                case 14: case 15: case 16: case 17: case 18: case 19: case 21:
                                    break;
                                case 2:
                                    SaveSystem.saveGameState(0);
                                    if ((menuResult = drawDialogOverlay(graphics, 1)) != -1) continue outerGameLoop;
                                    break;
                                case 4:
                                case 20:
                                    if (currentLevelId == 4) {
                                        SaveSystem.saveGameState(1);
                                        if ((menuResult = drawDialogOverlay(graphics, 2)) != -1) continue outerGameLoop;
                                        if ((menuResult = runMiniGameSniper(graphics, 0)) == -2) {
                                            drawGameOver(graphics);
                                            menuResult = showMenuScreen(graphics, true);
                                            continue outerGameLoop;
                                        }
                                        if (menuResult != -1) continue outerGameLoop;
                                    } else {
                                        currentLevelId = 4;
                                    }
                                    SaveSystem.saveGameState(2);
                                    if ((menuResult = drawDialogOverlay(graphics, 3)) != -1) continue outerGameLoop;
                                    break;
                                case 5:
                                    SaveSystem.saveGameState(3);
                                    if ((menuResult = drawDialogOverlay(graphics, 4)) != -1) continue outerGameLoop;
                                    break;
                                case 6:
                                case 22:
                                    if (currentLevelId == 6) {
                                        SaveSystem.saveGameState(4);
                                        if ((menuResult = drawDialogOverlay(graphics, 5)) != -1) continue outerGameLoop;
                                        if ((menuResult = runMiniGameSniper(graphics, 1)) == -2) {
                                            drawGameOver(graphics);
                                            menuResult = showMenuScreen(graphics, true);
                                            continue outerGameLoop;
                                        }
                                        if (menuResult != -1) continue outerGameLoop;
                                    } else {
                                        currentLevelId = 6;
                                    }
                                    SaveSystem.saveGameState(5);
                                    if ((menuResult = drawDialogOverlay(graphics, 6)) != -1) continue outerGameLoop;
                                    break;
                                case 7:
                                    SaveSystem.saveGameState(6);
                                    if ((menuResult = drawDialogOverlay(graphics, 7)) != -1) continue outerGameLoop;
                                    break;
                                case 9:
                                    SaveSystem.saveGameState(7);
                                    if ((menuResult = drawDialogOverlay(graphics, 8)) != -1) continue outerGameLoop;
                                    break;
                            }
                            needLoad = true;
                        } else if (GameEngine.levelTransitionState == -1) {
                            needLoad = true;
                        }

                        if (needLoad) {
                            DebugLogger.log("MainGameCanvas", "needLoad levelId=" + currentLevelId);
                            drawPleaseWait(graphics);
                            loadLevelResources();
                            DebugLogger.log("MainGameCanvas", "level loaded ok");
                        }

                        long currentTime = System.currentTimeMillis();
                        frameDeltaTime = currentTime - lastFrameTime;
                        lastFrameTime = currentTime;
                        accumulatedTime += frameDeltaTime;
                        if (accumulatedTime > 600L) accumulatedTime = 600L;

                        while (accumulatedTime >= 50L) {
                            ++frameCounter;
                            if (gameLoopTick()) {
                                GameEngine.damageFlash = false;
                                renderHUDAndWeapon(graphics);
                                flushScreenBuffer();
                                drawGameOver(graphics);
                                menuResult = showMenuScreen(graphics, true);
                                continue outerGameLoop;
                            }
                            accumulatedTime -= 50L;
                        }

                        renderHUDAndWeapon(graphics);
                        if (GameEngine.messageTimer > 0) {
                            drawMultiLineMessage(graphics, GameEngine.messageText);
                        }

                        flushScreenBuffer();
                        HelperUtils.yieldToOtherThreads();
                    } catch (Exception innerEx) {
                        DebugLogger.logException("MainGameCanvas.innerLoop", innerEx);
                    } catch (OutOfMemoryError innerOom) {
                        DebugLogger.logOutOfMemory("MainGameCanvas.innerLoop", innerOom);
                    }
                }
            }
        } catch (Exception ex) {
            DebugLogger.logException("MainGameCanvas.run", ex);
        } catch (OutOfMemoryError oom) {
            DebugLogger.logOutOfMemory("MainGameCanvas.run", oom);
        }
        DebugLogger.log("MainGameCanvas", "run exit - isGameInitialized=true");
        isGameInitialized = true;
    }

    public final boolean gameLoopTick() {
        try {
            if (GameEngine.updateGameLogic()) {
                return true;
            }

            weaponManager.update(GameEngine.ammoCounts, GameEngine.weaponsAvailable);

            if (GameEngine.selectNextWeapon && !weaponManager.isSwitchAnimationActive()) {
                GameEngine.selectNextWeapon = false;
                weaponManager.switchToNext(GameEngine.ammoCounts, GameEngine.weaponsAvailable);
            }

            GameEngine.currentWeapon = weaponManager.getCurrentWeaponId();
            GameEngine.weaponSwitchAnimationActive = weaponManager.isSwitchAnimationActive();
            GameEngine.weaponAnimationState = weaponManager.getAnimationState();
            GameEngine.weaponCooldownTimer = weaponManager.getCooldownTimer();
            weaponSpriteFrame = weaponManager.getSpriteFrame();

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

            Weapon currentWeapon = weaponManager.getCurrentWeapon();
            if (!currentWeapon.getIsAutomatic() || GameEngine.inputStrafe) {
                GameEngine.inputFire = false;
            }

            return false;
        } catch (Exception e) {
            DebugLogger.logException("gameLoopTick", e);
            return false;
        } catch (OutOfMemoryError e) {
            DebugLogger.logOutOfMemory("gameLoopTick", e);
            return false;
        }
    }

    private void initializeGameResources() {
        try {
            levelResourceManager.initializeGameResources(hudRenderer);
            if (hudRenderer.getStatusBarImage() != null) {
                sniperController = new SniperGameController(this, sniperMiniGame, fontRenderer, hudRenderer.getStatusBarImage());
            }
        } catch (Exception e) {
            DebugLogger.logException("initializeGameResources", e);
        } catch (OutOfMemoryError e) {
            DebugLogger.logOutOfMemory("initializeGameResources", e);
        }
    }

    private void loadLevelResources() {
        try {
            levelResourceManager.loadLevelResources();
        } catch (Exception e) {
            DebugLogger.logException("loadLevelResources", e);
        } catch (OutOfMemoryError e) {
            DebugLogger.logOutOfMemory("loadLevelResources", e);
        }
    }

    public final void stopGame() {
        if (!isGamePaused) {
            isGamePaused = true;
            if (HelperUtils.audioManager != null) {
                HelperUtils.audioManager.stopCurrentSound();
            }
        }
    }

    public final void resumeGame() {
        if (isGamePaused) {
            if (HelperUtils.audioManager != null && SaveSystem.musicEnabled == 1 && areResourcesLoaded) {
                HelperUtils.playSound(0, true, 80, 2);
            }
            isGamePaused = false;
        }
    }

    public void showNotify() { resumeGame(); }
    public void hideNotify() { stopGame(); }
    public final void stopGameLoop() { isGameRunning = false; }

    protected void flushScreenBuffer() {
        this.flushGraphics();
    }

    public void flushGraphicsPublic() {
        this.flushGraphics();
    }
}
