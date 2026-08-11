import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * Renders HUD, weapon, crosshair, minimap and messages.
 * Extracted from MainGameCanvas to break god-class.
 * Keeps rendering allocation-free; reuses existing Graphics and images.
 */
public final class HudRenderer {

    private final FontRenderer fontRenderer;
    private Image statusBarImage;
    private Image crosshairImage;

    public HudRenderer(FontRenderer fontRenderer) {
        this.fontRenderer = fontRenderer;
    }

    public void setStatusBarImage(Image img) { this.statusBarImage = img; }
    public void setCrosshairImage(Image img) { this.crosshairImage = img; }

    public Image getStatusBarImage() { return statusBarImage; }

    /**
     * Renders world frame via GameEngine, then weapon, HUD and map overlay.
     * @return headBob value for weapon vertical offset
     */
    public int renderFrameWithHud(Graphics graphics, int frameCounter, WeaponManager weaponManager) {
        try {
            int headBob = GameEngine.renderFrame(graphics, frameCounter) >> 15;

            weaponManager.render(graphics, headBob);

            if (DoomGameMode.isActive()) {
                drawDoomStatusBar(graphics, weaponManager);
            } else {
                graphics.drawImage(statusBarImage, 0, PortalRenderer.VIEWPORT_HEIGHT, 0);
                int hudTextY = PortalRenderer.VIEWPORT_HEIGHT
                        + (MainGameCanvas.STATUS_BAR_HEIGHT - fontRenderer.getLargeCharHeight()) / 2;
                fontRenderer.drawCenteredNumber(GameEngine.playerHealth, graphics, 58, hudTextY);
                fontRenderer.drawCenteredNumber(GameEngine.playerArmor, graphics, 138, hudTextY);

                int ammoType = weaponManager.getDisplayAmmoType();
                if (ammoType >= 0) {
                    fontRenderer.drawCenteredNumber(GameEngine.ammoCounts[ammoType], graphics, 218, hudTextY);
                }
            }

            if (weaponManager.getCurrentWeaponId() > 0 && GameEngine.messageTimer == 0 && !MainGameCanvas.mapEnabled) {
                graphics.drawImage(crosshairImage,
                        (PortalRenderer.VIEWPORT_WIDTH - crosshairImage.getWidth()) >> 1,
                        (PortalRenderer.VIEWPORT_HEIGHT - crosshairImage.getHeight()) >> 1, 0);
            }

            if (MainGameCanvas.mapEnabled) {
                graphics.setClip(0, 0, PortalRenderer.VIEWPORT_WIDTH, PortalRenderer.VIEWPORT_HEIGHT);
                LevelLoader.gameWorld.drawMapOnScreen(graphics);
                graphics.setClip(0, 0, PortalRenderer.VIEWPORT_WIDTH, MainGameCanvas.UI_HEIGHT);
            }

            return headBob;
        } catch (Exception e) {
            DebugLogger.logException("HudRenderer", e);
            return 0;
        } catch (OutOfMemoryError e) {
            DebugLogger.logOutOfMemory("HudRenderer", e);
            return 0;
        }
    }

    /** Compact Doom-style status strip; avoids the old CovertOps bar asset. */
    private void drawDoomStatusBar(Graphics graphics, WeaponManager weaponManager) {
        int y = PortalRenderer.VIEWPORT_HEIGHT;
        graphics.setColor(0x191919);
        graphics.fillRect(0, y, PortalRenderer.VIEWPORT_WIDTH, MainGameCanvas.STATUS_BAR_HEIGHT);
        graphics.setColor(0x6A2424);
        graphics.drawLine(0, y, PortalRenderer.VIEWPORT_WIDTH - 1, y);
        int textY = y + (MainGameCanvas.STATUS_BAR_HEIGHT - fontRenderer.getLargeCharHeight()) / 2;

        graphics.setColor(0xD0D0D0);
        graphics.drawString("HP", 3, y + 2, Graphics.TOP | Graphics.LEFT);
        graphics.drawString("ARM", 82, y + 2, Graphics.TOP | Graphics.LEFT);
        graphics.drawString("AMMO", 164, y + 2, Graphics.TOP | Graphics.LEFT);
        fontRenderer.drawCenteredNumber(GameEngine.playerHealth, graphics, 46, textY);
        fontRenderer.drawCenteredNumber(GameEngine.playerArmor, graphics, 126, textY);
        int ammoType = weaponManager.getDisplayAmmoType();
        if (ammoType >= 0) {
            fontRenderer.drawCenteredNumber(GameEngine.ammoCounts[ammoType], graphics, 218, textY);
        }
    }

    /**
     * Draws multi-line message centered (lines separated by '|').
     */
    public void drawMultiLineMessage(Graphics graphics, String message) {
        int lineStart = 0;
        int lineCount = 0;
        int lineEnd;
        do {
            lineEnd = message.indexOf('|', lineStart);
            if (lineEnd == -1) {
                lineEnd = message.length() - 1;
            } else {
                --lineEnd;
            }
            ++lineCount;
        } while ((lineStart = lineEnd + 2) < message.length());

        int menuItemHeight = 23;
        int halfUi = MainGameCanvas.UI_HEIGHT / 2;
        int textY = halfUi - menuItemHeight * lineCount / 2;
        lineStart = 0;

        do {
            lineEnd = message.indexOf('|', lineStart);
            if (lineEnd == -1) {
                lineEnd = message.length() - 1;
            } else {
                --lineEnd;
            }
            String line = message.substring(lineStart, lineEnd + 1);
            int textX = (PortalRenderer.VIEWPORT_WIDTH - fontRenderer.getLargeTextWidth(line)) / 2;
            fontRenderer.drawLargeString(line, graphics, textX, textY);
            textY += menuItemHeight;
        } while ((lineStart = lineEnd + 2) < message.length());
    }
}
