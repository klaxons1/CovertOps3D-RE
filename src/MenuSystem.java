import java.util.Stack;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * Handles main menu / pause menu navigation and rendering.
 * Extracted from MainGameCanvas.
 * Keeps logic identical but isolated for SRP.
 */
public final class MenuSystem {

    private static final int MENU_ITEM_HEIGHT = 23;
    private static final int UI_HEIGHT = 320;

    private static final int SETTINGS_SOUND_MODE = 50;
    private static final int SETTINGS_MUSIC_MODE = 51;
    private static final int SETTINGS_VIBRATION_MODE = 52;
    private static final int SETTINGS_FLOORS_MODE = 53;
    private static final int SETTINGS_SKY_MODE = 54;
    private static final int SETTINGS_MUZZLE_LIGHT_MODE = 55;
    private static final int SETTINGS_SCREEN_EFFECTS_MODE = 56;
    private static final int SETTINGS_ITEM_COUNT = 10;

    private final FontRenderer fontRenderer;
    private final MainGameCanvas canvas; // for flushGraphics and timing fields access
    private String[] settingsMenuItems;

    public MenuSystem(FontRenderer fontRenderer, MainGameCanvas canvas) {
        this.fontRenderer = fontRenderer;
        this.canvas = canvas;
    }

    /** Refreshes the persistent visual/performance settings shown in the menu. */
    private void updateSettingsMenuItems() {
        if (settingsMenuItems == null || settingsMenuItems.length != SETTINGS_ITEM_COUNT) {
            settingsMenuItems = new String[SETTINGS_ITEM_COUNT];
        }

        settingsMenuItems[0] = TextStrings.SETTINGS;
        settingsMenuItems[1] = TextStrings.EMPTY_SPACE;
        settingsMenuItems[2] = TextStrings.SOUND
                + (SaveSystem.soundEnabled == 1 ? TextStrings.ON : TextStrings.OFF);
        settingsMenuItems[3] = TextStrings.MUSIC
                + (SaveSystem.musicEnabled == 1 ? TextStrings.ON : TextStrings.OFF);
        settingsMenuItems[4] = TextStrings.VIBRATION
                + (SaveSystem.vibrationEnabled == 1 ? TextStrings.ON : TextStrings.OFF);
        settingsMenuItems[5] = TextStrings.FLOORS
                + (SaveSystem.texturedFlatsEnabled == 1 ? TextStrings.TEXTURED : TextStrings.FLAT);
        settingsMenuItems[6] = TextStrings.SKY
                + (SaveSystem.texturedSkyEnabled == 1 ? TextStrings.TEXTURED : TextStrings.SOLID);
        settingsMenuItems[7] = TextStrings.MUZZLE_LIGHT
                + (SaveSystem.muzzleLightingEnabled == 1 ? TextStrings.ON : TextStrings.OFF);
        settingsMenuItems[8] = TextStrings.SCREEN_EFFECTS
                + (SaveSystem.screenEffectsEnabled == 1 ? TextStrings.ON : TextStrings.OFF);
        settingsMenuItems[9] = TextStrings.BACK;
    }

    public void drawStripedBackground(Graphics graphics, Image background) {
        canvas.accumulatedTime = 0L;
        canvas.lastFrameTime = System.currentTimeMillis();
        int progress = 0;

        do {
            long currentTime = System.currentTimeMillis();
            canvas.frameDeltaTime = currentTime - canvas.lastFrameTime;
            canvas.lastFrameTime = currentTime;
            canvas.accumulatedTime += canvas.frameDeltaTime;
            if (canvas.accumulatedTime > 600L) {
                canvas.accumulatedTime = 600L;
            }
            while (canvas.accumulatedTime >= 6L) {
                ++progress;
                canvas.accumulatedTime -= 6L;
            }
            int displayProgress = progress > UI_HEIGHT ? UI_HEIGHT : progress;
            int column = 0;
            for (int x = 0; x < PortalRenderer.VIEWPORT_WIDTH; x += 10) {
                if ((column & 1) == 0) {
                    graphics.drawRegion(background, x, UI_HEIGHT - displayProgress, 10, displayProgress,
                            0, x, 0, 20);
                } else {
                    graphics.drawRegion(background, x, 0, 10, displayProgress,
                            0, x, UI_HEIGHT - displayProgress, 20);
                }
                ++column;
            }
            canvas.flushGraphicsPublic();
        } while (progress <= UI_HEIGHT);
    }

    public int showMenuScreen(Graphics graphics, boolean isMainMenu, long[] timingState) {
        // timingState unused, kept for compatibility; actual timing uses canvas fields
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
            drawStripedBackground(graphics, background);

            if (SaveSystem.musicEnabled == 1 && !canvas.isGamePaused) {
                HelperUtils.playSound(0, true, 80, 2);
            }

            Stack menuStack = new Stack();

            while (true) {
                graphics.drawImage(background, 0, 0, 20);

                int totalItems = menuItems.length - 1;
                int visibleItems = totalItems > 5 ? 5 : totalItems;
                int menuY = UI_HEIGHT - visibleItems * MENU_ITEM_HEIGHT - 3 - MENU_ITEM_HEIGHT;

                if (totalItems > visibleItems && scrollOffset > 0) {
                    int arrowY = menuY + 2 * MENU_ITEM_HEIGHT - 2;
                    graphics.setColor(16115387);
                    graphics.fillTriangle(117, arrowY, 123, arrowY, PortalRenderer.HALF_VIEWPORT_WIDTH, arrowY - 3);
                }

                graphics.setColor(7433570);

                for (int i = 0; i < visibleItems; ++i) {
                    int itemIndex = i;
                    if (scrollOffset > 0 && i > 1) {
                        itemIndex = i + scrollOffset;
                    }
                    String itemText = menuItems[itemIndex];
                    int textX = (PortalRenderer.VIEWPORT_WIDTH - fontRenderer.getLargeTextWidth(itemText)) / 2;

                    if ((menuMode & 15) == itemIndex) {
                        int boxWidth = 4 * 30;
                        graphics.fillRoundRect((PortalRenderer.VIEWPORT_WIDTH - boxWidth) / 2, menuY,
                                boxWidth, MENU_ITEM_HEIGHT, 10, 10);
                    }

                    fontRenderer.drawLargeString(itemText, graphics, textX, menuY);
                    menuY += MENU_ITEM_HEIGHT;
                }

                if (totalItems > visibleItems && scrollOffset < totalItems - 5) {
                    int arrowY = menuY + 1;
                    graphics.setColor(16115387);
                    graphics.fillTriangle(117, arrowY, 123, arrowY, PortalRenderer.HALF_VIEWPORT_WIDTH, arrowY + 3);
                }

                String actionText = menuItems == settingsMenuItems ? TextStrings.CHANGE :
                        (menuItems == TextStrings.CONFIRMATION_MENU_ITEMS ? TextStrings.YES : TextStrings.SELECT);
                fontRenderer.drawLargeString(actionText, graphics, 3, UI_HEIGHT - MENU_ITEM_HEIGHT - 3);
                fontRenderer.drawLargeString(menuItems[totalItems], graphics,
                        PortalRenderer.VIEWPORT_WIDTH - fontRenderer.getLargeTextWidth(menuItems[totalItems]) - 3,
                        UI_HEIGHT - MENU_ITEM_HEIGHT - 3);
                canvas.flushGraphicsPublic();
                HelperUtils.yieldToOtherThreads();

                Object[] stackData;

                if (GameEngine.inputRun || GameEngine.inputFire) {
                    GameEngine.inputRun = false;
                    GameEngine.inputFire = false;

                    switch (menuMode) {
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
                            updateSettingsMenuItems();
                            stackData = new Object[4];
                            stackData[0] = menuItems;
                            stackData[1] = new Integer(menuMode);
                            stackData[2] = new Integer(firstItem);
                            stackData[3] = new Integer(lastItem);
                            menuStack.push(stackData);
                            menuItems = settingsMenuItems;
                            menuMode = 50;
                            firstItem = 2;
                            lastItem = settingsMenuItems.length - 2;
                            break;

                        case 2:
                        case 35:
                            new DialogSystem(fontRenderer, canvas).showScrollingText(graphics, background, TextStrings.HELP, TextStrings.HELP_MENU_ITEMS, false);
                            break;

                        case 3:
                        case 36:
                            new DialogSystem(fontRenderer, canvas).showScrollingText(graphics, background, TextStrings.ABOUT, TextStrings.ABOUT_MENU_TEXT, true);
                            break;

                        case 18:
                        case 19:
                        case 20:
                            String[] chapterMenuItems = new String[TextStrings.CHAPTER_MENU_DATA.length];
                            chapterMenuItems[0] = TextStrings.CHAPTER_MENU_DATA[0];
                            chapterMenuItems[1] = TextStrings.CHAPTER_MENU_DATA[1];
                            chapterMenuItems[2] = TextStrings.CHAPTER_MENU_DATA[2];
                            chapterMenuItems[chapterMenuItems.length - 1] =
                                    TextStrings.CHAPTER_MENU_DATA[TextStrings.CHAPTER_MENU_DATA.length - 1];
                            stackData = new Object[4];
                            stackData[0] = menuItems;
                            stackData[1] = new Integer(menuMode);
                            stackData[2] = new Integer(firstItem);
                            stackData[3] = new Integer(lastItem);
                            menuStack.push(stackData);
                            GameEngine.difficultyLevel = menuMode - 18;
                            SaveSystem.loadSaveData();
                            firstItem = 2;
                            lastItem = chapterMenuItems.length - 2;

                            for (int i = 3; i <= lastItem; ++i) {
                                chapterMenuItems[i] = SaveSystem.saveData[i - 3] != null
                                        ? TextStrings.CHAPTER_MENU_DATA[i]
                                        : TextStrings.UNAVAILABLE;
                            }
                            // Store chapter menu into canvas for later usage? Use static holder
                            canvas.chapterMenuItems = chapterMenuItems;
                            menuItems = chapterMenuItems;
                            menuMode = 66;
                            break;

                        case SETTINGS_SOUND_MODE:
                            SaveSystem.soundEnabled = (byte)(SaveSystem.soundEnabled ^ 1);
                            if (SaveSystem.musicEnabled != 1) {
                                if (SaveSystem.soundEnabled == 1) {
                                    HelperUtils.playSound(1, false, 80, 0);
                                } else {
                                    HelperUtils.stopCurrentSound();
                                }
                            }
                            updateSettingsMenuItems();
                            SaveSystem.saveSettingsToRMS();
                            break;

                        case SETTINGS_MUSIC_MODE:
                            SaveSystem.musicEnabled = (byte)(SaveSystem.musicEnabled ^ 1);
                            if (SaveSystem.musicEnabled == 1) {
                                HelperUtils.stopCurrentSound();
                                HelperUtils.playSound(0, true, 80, 2);
                            } else {
                                HelperUtils.stopCurrentSound();
                            }
                            updateSettingsMenuItems();
                            SaveSystem.saveSettingsToRMS();
                            break;

                        case SETTINGS_VIBRATION_MODE:
                            SaveSystem.vibrationEnabled = (byte)(SaveSystem.vibrationEnabled ^ 1);
                            if (SaveSystem.vibrationEnabled == 1) {
                                HelperUtils.vibrateDevice(100);
                            }
                            updateSettingsMenuItems();
                            SaveSystem.saveSettingsToRMS();
                            break;

                        case SETTINGS_FLOORS_MODE:
                            SaveSystem.texturedFlatsEnabled = (byte)(SaveSystem.texturedFlatsEnabled ^ 1);
                            updateSettingsMenuItems();
                            SaveSystem.saveSettingsToRMS();
                            break;

                        case SETTINGS_SKY_MODE:
                            SaveSystem.texturedSkyEnabled = (byte)(SaveSystem.texturedSkyEnabled ^ 1);
                            updateSettingsMenuItems();
                            SaveSystem.saveSettingsToRMS();
                            break;

                        case SETTINGS_MUZZLE_LIGHT_MODE:
                            SaveSystem.muzzleLightingEnabled = (byte)(SaveSystem.muzzleLightingEnabled ^ 1);
                            updateSettingsMenuItems();
                            SaveSystem.saveSettingsToRMS();
                            break;

                        case SETTINGS_SCREEN_EFFECTS_MODE:
                            SaveSystem.screenEffectsEnabled = (byte)(SaveSystem.screenEffectsEnabled ^ 1);
                            updateSettingsMenuItems();
                            SaveSystem.saveSettingsToRMS();
                            break;

                        case 66: case 67: case 68: case 69: case 70: case 71: case 72: case 73: case 74:
                            if (!canvas.chapterMenuItems[menuMode - 64].equals(TextStrings.UNAVAILABLE)) {
                                HelperUtils.stopCurrentSound();
                                return menuMode;
                            }
                            break;

                        case 80:
                            HelperUtils.stopCurrentSound();
                            return 4;

                        default:
                            if (menuMode <= 17 || (menuMode >= 21 && menuMode <= 65) || menuMode >= 75) {
                                HelperUtils.stopCurrentSound();
                                return menuMode;
                            }
                            break;
                    }
                }

                if (GameEngine.inputBack) {
                    GameEngine.inputBack = false;
                    if (menuItems[menuItems.length - 1] != TextStrings.BACK && !menuItems[menuItems.length - 1].equals("no")) {
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
                    int selected = menuMode & 15;
                    --selected;
                    if (selected < firstItem) selected = firstItem;
                    else if (selected - scrollOffset < 2) --scrollOffset;
                    menuMode = (menuMode & ~15) | selected;
                    GameEngine.inputForward = false;
                }

                if (GameEngine.inputBackward) {
                    int selected = menuMode & 15;
                    ++selected;
                    if (selected > lastItem) selected = lastItem;
                    else if (totalItems > visibleItems && selected - scrollOffset > 4) ++scrollOffset;
                    menuMode = (menuMode & ~15) | selected;
                    GameEngine.inputBackward = false;
                }
            }
        } catch (Exception e) {
            DebugLogger.logException("MenuSystem", e);
            HelperUtils.stopCurrentSound();
            return 4;
        } catch (OutOfMemoryError e) {
            DebugLogger.logOutOfMemory("MenuSystem", e);
            HelperUtils.stopCurrentSound();
            return 4;
        }
    }
}
