import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * Handles splash, game over, please wait and story dialog rendering.
 * Separated from MainGameCanvas for SRP and readability.
 */
public final class DialogSystem {

    private static final int MENU_ITEM_HEIGHT = 23;
    private static final int UI_HEIGHT = 320;
    private static final int HALF_UI_HEIGHT = UI_HEIGHT / 2;

    private final FontRenderer fontRenderer;
    private final MainGameCanvas canvas;

    public DialogSystem(FontRenderer fontRenderer, MainGameCanvas canvas) {
        this.fontRenderer = fontRenderer;
        this.canvas = canvas;
    }

    public void drawSplash(Graphics graphics) {
        try {
            Image logo = Image.createImage("/gamedata/sprites/logo.png");
            Image splash = Image.createImage("/gamedata/sprites/splash.png");

            int pixelCount = logo.getWidth() * logo.getHeight();
            int[] fadeBuffer = new int[pixelCount];
            int logoX = (PortalRenderer.VIEWPORT_WIDTH - logo.getWidth()) / 2;
            int logoY = (UI_HEIGHT - logo.getHeight()) / 2;

            graphics.setColor(16777215);
            graphics.drawRect(0, 0, PortalRenderer.VIEWPORT_WIDTH, UI_HEIGHT);
            canvas.flushGraphicsPublic();

            long startTime = System.currentTimeMillis();

            while (true) {
                int fadeColor = 16777215;
                int elapsed = (int)(System.currentTimeMillis() - startTime >> 2);

                if (elapsed < 256) {
                    fadeColor |= (255 - elapsed) << 24;
                } else if (elapsed >= 512 && elapsed < 768) {
                    fadeColor |= (elapsed - 512) << 24;
                } else if (elapsed >= 768) {
                    fadeBuffer = new int[pixelCount = splash.getWidth() * splash.getHeight()];
                    startTime = System.currentTimeMillis();

                    while (true) {
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
                        canvas.flushGraphicsPublic();
                        HelperUtils.yieldToOtherThreads();
                    }
                }

                fadeBuffer[0] = fadeColor;
                HelperUtils.fastArrayFill(fadeBuffer, 0, pixelCount);
                graphics.drawImage(logo, logoX, logoY, 20);
                graphics.drawRGB(fadeBuffer, 0, logo.getWidth(), logoX, logoY,
                        logo.getWidth(), logo.getHeight(), true);
                canvas.flushGraphicsPublic();
                HelperUtils.yieldToOtherThreads();
            }
        } catch (Exception e) {
        } catch (OutOfMemoryError e) {
        }
    }

    public void drawGameOver(Graphics graphics) {
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
            drawMultiLineMessage(graphics, message);
            canvas.flushGraphicsPublic();
            HelperUtils.delay(2000);

            long startTime = System.currentTimeMillis();

            while (true) {
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
                drawMultiLineMessage(graphics, message);
                canvas.flushGraphicsPublic();
                HelperUtils.yieldToOtherThreads();
            }
        } catch (Exception e) {
            return;
        } catch (OutOfMemoryError e) {
        }
    }

    public void drawPleaseWait(Graphics graphics) {
        String text = TextStrings.PLEASE_WAIT;
        int textX = (PortalRenderer.VIEWPORT_WIDTH - fontRenderer.getLargeTextWidth(text)) / 2;
        int textY = HALF_UI_HEIGHT - MENU_ITEM_HEIGHT / 2;

        int halfScreenBuffer = PortalRenderer.VIEWPORT_WIDTH * HALF_UI_HEIGHT;
        PortalRenderer.screenBuffer[0] = Integer.MIN_VALUE;
        HelperUtils.fastArrayFill(PortalRenderer.screenBuffer, 0, halfScreenBuffer);

        graphics.drawRGB(PortalRenderer.screenBuffer, 0, PortalRenderer.VIEWPORT_WIDTH,
                0, 0, PortalRenderer.VIEWPORT_WIDTH, HALF_UI_HEIGHT, true);
        graphics.drawRGB(PortalRenderer.screenBuffer, 0, PortalRenderer.VIEWPORT_WIDTH,
                0, HALF_UI_HEIGHT, PortalRenderer.VIEWPORT_WIDTH, HALF_UI_HEIGHT, true);

        fontRenderer.drawLargeString(text, graphics, textX, textY);
        canvas.flushGraphicsPublic();
    }

    public void showScrollingText(Graphics graphics, Image background, String title, String[] content, boolean isScrollable) {
        GameEngine.inputRun = false;
        GameEngine.inputBack = false;
        GameEngine.inputFire = false;
        GameEngine.inputForward = false;
        GameEngine.inputBackward = false;

        try {
            String version = MainGameCanvas.mainMidlet.getAppProperty("MIDlet-Version");
            fontRenderer.loadSmallFont("/gamedata/sprites/font_cut.png");

            int textY = UI_HEIGHT - MENU_ITEM_HEIGHT;
            int halfScreenBuffer = PortalRenderer.VIEWPORT_WIDTH * HALF_UI_HEIGHT;

            // fade in
            for (int fadeStep = 1; fadeStep <= 8; ++fadeStep) {
                PortalRenderer.screenBuffer[0] = 16777215 | (fadeStep * 268435456);
                HelperUtils.fastArrayFill(PortalRenderer.screenBuffer, 0, halfScreenBuffer);
                graphics.drawImage(background, 0, 0, 20);
                graphics.drawRGB(PortalRenderer.screenBuffer, 0, PortalRenderer.VIEWPORT_WIDTH,
                        0, 0, PortalRenderer.VIEWPORT_WIDTH, HALF_UI_HEIGHT, true);
                graphics.drawRGB(PortalRenderer.screenBuffer, 0, PortalRenderer.VIEWPORT_WIDTH,
                        0, HALF_UI_HEIGHT, PortalRenderer.VIEWPORT_WIDTH, HALF_UI_HEIGHT, true);
                canvas.flushGraphicsPublic();
                HelperUtils.yieldToOtherThreads();
                HelperUtils.delay(50);
            }

            long lastScrollTime = System.currentTimeMillis();
            boolean needsUpdate = true;

            do {
                if (needsUpdate) {
                    graphics.setClip(0, 0, PortalRenderer.VIEWPORT_WIDTH, UI_HEIGHT);
                    graphics.drawImage(background, 0, 0, 20);
                    graphics.drawRGB(PortalRenderer.screenBuffer, 0, PortalRenderer.VIEWPORT_WIDTH,
                            0, 0, PortalRenderer.VIEWPORT_WIDTH, HALF_UI_HEIGHT, true);
                    graphics.drawRGB(PortalRenderer.screenBuffer, 0, PortalRenderer.VIEWPORT_WIDTH,
                            0, HALF_UI_HEIGHT, PortalRenderer.VIEWPORT_WIDTH, HALF_UI_HEIGHT, true);

                    String backText = TextStrings.BACK;
                    fontRenderer.drawLargeString(backText, graphics,
                            PortalRenderer.VIEWPORT_WIDTH - fontRenderer.getLargeTextWidth(backText) - 3,
                            UI_HEIGHT - MENU_ITEM_HEIGHT - 3);
                    fontRenderer.drawLargeString(title, graphics,
                            (PortalRenderer.VIEWPORT_WIDTH - fontRenderer.getLargeTextWidth(title)) / 2, 3);

                    graphics.setClip(0, MENU_ITEM_HEIGHT + 6, PortalRenderer.VIEWPORT_WIDTH,
                            UI_HEIGHT - 2 * MENU_ITEM_HEIGHT - 12);

                    int displayY;
                    if (isScrollable) {
                        long currentTime = System.currentTimeMillis();
                        int elapsed = (int)(currentTime - lastScrollTime);
                        displayY = textY;
                        int scrollSteps = elapsed / 50 + 1;
                        textY -= scrollSteps;
                        if (textY + content.length * (fontRenderer.getSmallCharHeight() + 2) < 0) {
                            textY = UI_HEIGHT - MENU_ITEM_HEIGHT;
                        }
                        int remainingDelay = scrollSteps * 50 - elapsed;
                        if (remainingDelay > 0) HelperUtils.delay(remainingDelay);
                        lastScrollTime = currentTime;
                    } else {
                        displayY = (UI_HEIGHT - (fontRenderer.getSmallCharHeight() + 2) * content.length) / 2;
                    }

                    for (int i = 0; i < content.length; ++i) {
                        String line = content[i];
                        if (i == 0 && isScrollable) line = line + " " + version;
                        fontRenderer.drawSmallString(line, graphics,
                                (PortalRenderer.VIEWPORT_WIDTH - fontRenderer.getSmallTextWidth(line)) / 2, displayY);
                        displayY += fontRenderer.getSmallCharHeight() + 2;
                    }

                    canvas.flushGraphicsPublic();
                }
                needsUpdate = isScrollable;
                HelperUtils.yieldToOtherThreads();
            } while (!GameEngine.inputBack);

            GameEngine.inputBack = false;
            graphics.setClip(0, 0, PortalRenderer.VIEWPORT_WIDTH, UI_HEIGHT);

            for (int fadeStep = 8; fadeStep >= 1; --fadeStep) {
                PortalRenderer.screenBuffer[0] = 16777215 | (fadeStep * 268435456);
                HelperUtils.fastArrayFill(PortalRenderer.screenBuffer, 0, halfScreenBuffer);
                graphics.drawImage(background, 0, 0, 20);
                graphics.drawRGB(PortalRenderer.screenBuffer, 0, PortalRenderer.VIEWPORT_WIDTH,
                        0, 0, PortalRenderer.VIEWPORT_WIDTH, HALF_UI_HEIGHT, true);
                graphics.drawRGB(PortalRenderer.screenBuffer, 0, PortalRenderer.VIEWPORT_WIDTH,
                        0, HALF_UI_HEIGHT, PortalRenderer.VIEWPORT_WIDTH, HALF_UI_HEIGHT, true);
                canvas.flushGraphicsPublic();
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
        fontRenderer.unloadSmallFont();
        graphics.setClip(0, 0, PortalRenderer.VIEWPORT_WIDTH, UI_HEIGHT);
    }

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

        int textY = HALF_UI_HEIGHT - MENU_ITEM_HEIGHT * lineCount / 2;
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
            textY += MENU_ITEM_HEIGHT;
        } while ((lineStart = lineEnd + 2) < message.length());
    }

    public int drawDialogOverlay(Graphics graphics, int dialogId) {
        try {
            int menuHeight = MENU_ITEM_HEIGHT + 6;

            Image frameBuffer = Image.createImage(PortalRenderer.VIEWPORT_WIDTH, UI_HEIGHT);
            Image background = Image.createImage("/gamedata/sprites/bkg_cut.png");
            Image playerPortrait = Image.createImage("/gamedata/sprites/player.png");
            Image agentPortrait = null;
            if (dialogId != 0 && dialogId != 9) {
                agentPortrait = Image.createImage(dialogId == 8
                        ? "/gamedata/sprites/ag_hurt.png"
                        : "/gamedata/sprites/ag.png");
            }
            Image doctorPortrait = (dialogId == 7)
                    ? Image.createImage("/gamedata/sprites/doctor.png")
                    : null;

            fontRenderer.loadSmallFont("/gamedata/sprites/font_cut.png");
            int smallH = fontRenderer.getSmallCharHeight();
            int smallSpace = fontRenderer.getSmallSpaceWidth();

            int textAreaWidth = PortalRenderer.VIEWPORT_WIDTH - playerPortrait.getWidth() - 6;
            Graphics fbGraphics = frameBuffer.getGraphics();
            fbGraphics.setColor(16711680);
            fbGraphics.drawImage(background, 0, 0, 20);
            fbGraphics.drawImage(playerPortrait, 2, 2, 20);

            int agentY = HALF_UI_HEIGHT + 2;
            int doctorY = 2 * (UI_HEIGHT - menuHeight) / 3 + 2;
            int linesPerBox = (316 - menuHeight) / smallH;

            int[][] lineBuffers = new int[3][];
            int[] lineIndices = new int[]{0, 0, 0};
            String[] currentText = new String[3];

            if (agentPortrait != null) {
                if (doctorPortrait != null) {
                    agentY = (UI_HEIGHT - menuHeight) / 3 + 2;
                    fbGraphics.drawImage(agentPortrait, PortalRenderer.VIEWPORT_WIDTH - 2 - agentPortrait.getWidth(), agentY, 20);
                    fbGraphics.drawImage(doctorPortrait, PortalRenderer.VIEWPORT_WIDTH - 2 - doctorPortrait.getWidth(), doctorY, 20);
                    linesPerBox = (316 - menuHeight) / (smallH * 3);
                    lineBuffers[1] = new int[linesPerBox];
                    lineBuffers[2] = new int[linesPerBox];
                } else {
                    fbGraphics.drawImage(agentPortrait, PortalRenderer.VIEWPORT_WIDTH - 2 - agentPortrait.getWidth(), agentY, 20);
                    linesPerBox = (316 - menuHeight) / (smallH * 2);
                    lineBuffers[1] = new int[linesPerBox];
                }
            }
            lineBuffers[0] = new int[linesPerBox];
            new MenuSystem(fontRenderer, canvas).drawStripedBackground(graphics, frameBuffer);
            fontRenderer.drawLargeString(TextStrings.BACK, graphics,
                    PortalRenderer.VIEWPORT_WIDTH - fontRenderer.getLargeTextWidth(TextStrings.BACK) - 3, UI_HEIGHT - MENU_ITEM_HEIGHT - 3);
            fontRenderer.drawLargeString(TextStrings.PAUSE, graphics, 3, UI_HEIGHT - MENU_ITEM_HEIGHT - 3);

            int[] charIndices = new int[]{0, 0, 0};
            int[] boxStartX = new int[]{0, 0, 0};
            int[] boxStartY = new int[]{0, 0, 0};
            int[] boxEndY = new int[]{0, 0, 0};
            long[] fadeTimers = new long[]{0L, 0L, 0L};
            boolean[] needsFade = new boolean[]{false, false, false};

            for (int lineNum = 0; lineNum < TextStrings.storyText[dialogId].length; ++lineNum) {
                String line = TextStrings.storyText[dialogId][lineNum];
                int textX = playerPortrait.getWidth() + 4;
                int textY = 2;
                byte boxId = 0;

                if (line.startsWith("A")) {
                    textX = 2; textY = agentY; boxId = 1;
                } else if (line.startsWith("M")) {
                    textX = 2; textY = doctorY; boxId = 2;
                }

                boxStartX[boxId] = textX;
                boxStartY[boxId] = textY;
                boxEndY[boxId] = textY + linesPerBox * smallH;

                int maxX = textX + textAreaWidth;
                int maxY = textY + linesPerBox * smallH;
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
                            textAreaWidth, boxEndY[boxId] - boxStartY[boxId], 0, boxStartX[boxId], boxStartY[boxId], 20);
                }

                lineIndices[boxId] = 0;
                lineBuffers[boxId][lineIndices[boxId]] = 1;

                for (int charPos = 1; charPos < line.length(); ++charPos) {
                    char c = line.charAt(charPos);

                    if (c == ' ') {
                        if (currentX + smallSpace > maxX) {
                            currentX = textX;
                            if (lineIndices[boxId] >= linesPerBox - 1) {
                                drawWrappedLine(graphics, frameBuffer, line, lineBuffers[boxId], charPos + 1, textX, textY, maxY, textAreaWidth);
                            } else {
                                currentY += smallH;
                                ++lineIndices[boxId];
                                lineBuffers[boxId][lineIndices[boxId]] = charPos + 1;
                            }
                        } else {
                            int nextSpace = line.indexOf(32, charPos + 1);
                            if (nextSpace == -1) nextSpace = line.length();
                            String word = line.substring(charPos, nextSpace);
                            int wordWidth = fontRenderer.getSmallTextWidth(word);

                            if (currentX + smallSpace + wordWidth > maxX) {
                                currentX = textX;
                                if (lineIndices[boxId] >= linesPerBox - 1) {
                                    drawWrappedLine(graphics, frameBuffer, line, lineBuffers[boxId], charPos + 1, textX, textY, maxY, textAreaWidth);
                                } else {
                                    currentY += smallH;
                                    ++lineIndices[boxId];
                                    lineBuffers[boxId][lineIndices[boxId]] = charPos + 1;
                                }
                            } else {
                                currentX += smallSpace;
                            }
                        }
                    } else {
                        int charWidth = fontRenderer.getSmallCharWidth(c);
                        if (currentX + charWidth > maxX) {
                            currentX = textX;
                            if (lineIndices[boxId] >= linesPerBox - 1) {
                                drawWrappedLine(graphics, frameBuffer, line, lineBuffers[boxId], charPos, textX, textY, maxY, textAreaWidth);
                            } else {
                                currentY += smallH;
                                ++lineIndices[boxId];
                                lineBuffers[boxId][lineIndices[boxId]] = charPos;
                            }
                        }
                        int drawnWidth = fontRenderer.drawSmallChar(c, graphics, currentX, currentY);
                        currentX += drawnWidth;
                    }

                    canvas.flushGraphicsPublic();
                    HelperUtils.delay(c == ',' ? 300 : (c != '.' && c != '?' && c != '!' ? 50 : 400));

                    long currentTime = System.currentTimeMillis();
                    for (int b = 0; b < 3; ++b) {
                        if (needsFade[b] && currentTime > fadeTimers[b]) {
                            needsFade[b] = false;
                            int oldColor = graphics.getColor();
                            graphics.setColor(0);
                            graphics.fillRect(boxStartX[b], boxStartY[b], textAreaWidth, boxEndY[b] - boxStartY[b]);
                            graphics.setColor(oldColor);
                            graphics.drawRegion(frameBuffer, boxStartX[b], boxStartY[b],
                                    textAreaWidth, boxEndY[b] - boxStartY[b], 0, boxStartX[b], boxStartY[b], 20);
                            lineIndices[b] = 0;
                            currentText[b] = null;
                        }
                    }

                    if (GameEngine.inputRun) {
                        GameEngine.inputRun = false;
                        graphics.drawRegion(frameBuffer, 3, UI_HEIGHT - MENU_ITEM_HEIGHT - 3,
                                fontRenderer.getLargeTextWidth(TextStrings.PAUSE), MENU_ITEM_HEIGHT,
                                0, 3, UI_HEIGHT - MENU_ITEM_HEIGHT - 3, 20);
                        fontRenderer.drawLargeString(TextStrings.RESUME, graphics, 3, UI_HEIGHT - MENU_ITEM_HEIGHT - 3);
                        canvas.flushGraphicsPublic();
                        while (!GameEngine.inputRun && !GameEngine.inputBack && !canvas.isGamePaused && !GameEngine.inputFire) {
                            HelperUtils.yieldToOtherThreads();
                        }
                    }

                    if (GameEngine.inputRun) {
                        graphics.drawRegion(frameBuffer, 3, UI_HEIGHT - MENU_ITEM_HEIGHT - 3,
                                fontRenderer.getLargeTextWidth(TextStrings.RESUME), MENU_ITEM_HEIGHT,
                                0, 3, UI_HEIGHT - MENU_ITEM_HEIGHT - 3, 20);
                        fontRenderer.drawLargeString(TextStrings.PAUSE, graphics, 3, UI_HEIGHT - MENU_ITEM_HEIGHT - 3);
                        canvas.flushGraphicsPublic();
                        GameEngine.inputRun = false;
                    }

                    if (GameEngine.inputBack || canvas.isGamePaused) {
                        GameEngine.inputRun = false;
                        GameEngine.inputBack = false;
                        int menuResult = new MenuSystem(fontRenderer, canvas).showMenuScreen(graphics, false, null);
                        if (menuResult != 32) {
                            fontRenderer.unloadSmallFont();
                            return menuResult;
                        }

                        graphics.drawImage(frameBuffer, 0, 0, 20);
                        fontRenderer.drawLargeString(TextStrings.BACK, graphics,
                                PortalRenderer.VIEWPORT_WIDTH - fontRenderer.getLargeTextWidth(TextStrings.BACK) - 3,
                                UI_HEIGHT - MENU_ITEM_HEIGHT - 3);
                        fontRenderer.drawLargeString(TextStrings.PAUSE, graphics, 3, UI_HEIGHT - MENU_ITEM_HEIGHT - 3);

                        for (int b = 0; b < 3; ++b) {
                            int endChar = b == boxId ? charPos : (currentText[b] != null ? currentText[b].length() : 0);
                            if (lineBuffers[b] != null) {
                                int startX = 0; int startY = 0;
                                switch (b) {
                                    case 0: startX = playerPortrait.getWidth() + 4; startY = 2; break;
                                    case 1: startX = 2; startY = agentY; break;
                                    case 2: startX = 2; startY = doctorY; break;
                                }
                                for (int lineIdx = 0; lineIdx <= lineIndices[b]; ++lineIdx) {
                                    int lineStart = lineBuffers[b][lineIdx];
                                    int lineEnd = lineIdx + 1 <= lineIndices[b] ? lineBuffers[b][lineIdx + 1] : endChar;
                                    if (endChar + 1 < lineEnd) lineEnd = endChar + 1;
                                    int renderX = startX;
                                    for (int pos = lineStart; pos < lineEnd; ++pos) {
                                        char ch = currentText[b].charAt(pos);
                                        if (ch == ' ') {
                                            renderX += smallSpace;
                                        } else {
                                            renderX += fontRenderer.drawSmallChar(ch, graphics, renderX, startY);
                                        }
                                    }
                                    startY += smallH;
                                }
                            }
                        }
                        canvas.flushGraphicsPublic();
                    }

                    if (GameEngine.inputFire) {
                        GameEngine.inputFire = false;
                        fontRenderer.unloadSmallFont();
                        return -1;
                    }
                    HelperUtils.yieldToOtherThreads();
                }

                charIndices[boxId] = 0;
                needsFade[boxId] = true;
                boxStartX[boxId] = textX; boxStartY[boxId] = textY; boxEndY[boxId] = maxY;
                fadeTimers[boxId] = System.currentTimeMillis() + 5000L;
                HelperUtils.delay(500);
            }

            HelperUtils.delay(5000);
            fontRenderer.unloadSmallFont();
        } catch (Exception e) {
        } catch (OutOfMemoryError e) {
        }
        return -1;
    }

    private void drawWrappedLine(Graphics graphics, Image frameBuffer, String text, int[] lineStarts,
                                 int startChar, int startX, int startY, int maxY, int width) {

        int smallCharHeight = fontRenderer.getSmallCharHeight();
        int smallSpaceWidth = fontRenderer.getSmallSpaceWidth();

        int height = maxY - startY;
        int oldColor = graphics.getColor();
        graphics.setColor(0);
        graphics.fillRect(startX, startY, width, height);
        graphics.setColor(oldColor);
        graphics.drawRegion(frameBuffer, startX, startY, width, height, 0, startX, startY, 20);

        int renderY = startY;

        for (int lineIdx = 1; lineIdx < lineStarts.length; ++lineIdx) {
            int lineStart = lineStarts[lineIdx];
            lineStarts[lineIdx - 1] = lineStart;
            int lineEnd = lineIdx + 1 < lineStarts.length
                    ? lineStarts[lineIdx + 1]
                    : startChar;

            if (lineIdx > 1) {
                int oldColor2 = graphics.getColor();
                graphics.setColor(0);
                graphics.fillRect(startX, renderY, width, smallCharHeight);
                graphics.setColor(oldColor2);
                graphics.drawRegion(frameBuffer, startX, renderY, width, smallCharHeight,
                        0, startX, renderY, 20);
            }

            int renderX = startX;
            for (int charIdx = lineStart; charIdx < lineEnd; ++charIdx) {
                char c = text.charAt(charIdx);
                if (c == ' ') {
                    renderX += smallSpaceWidth;
                } else {
                    renderX += fontRenderer.drawSmallChar(c, graphics, renderX, renderY);
                }
            }
            renderY += smallCharHeight;
        }

        lineStarts[lineStarts.length - 1] = startChar;
    }
}
