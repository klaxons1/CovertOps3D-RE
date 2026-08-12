import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * Unicode bitmap-font renderer.
 *
 * Font assets originate from Quantum with the author's permission supplied by
 * the project maintainer. The loader and lookup code are purpose-built for
 * this MIDP game: glyph lookup is O(1), there are no per-character objects,
 * and the same renderer is used by menus, HUD and dialogs.
 */
public final class FontRenderer {

    public static final int STYLE_NORMAL = 0;
    public static final int STYLE_SELECTED = 1;
    public static final int STYLE_ACTIVE = 2;

    private static final int ASCII_LIMIT = 128;
    private static final int CYRILLIC_BASE = 0x0400;
    private static final int CYRILLIC_COUNT = 0x60;
    private static final int DEFAULT_SPACE_WIDTH = 6;
    private static final int SMALL_LETTER_SPACING = 1;

    private Image normalImage;
    private Image selectedImage;
    private Image activeImage;
    private String activeImagePath;
    private String normalImagePath;
    private String selectedImagePath;
    private String activeLoadedPath;
    private String loadedCharacters;
    private int sourceImageWidth;
    private int sourceImageHeight;

    private int[] glyphX;
    private int[] glyphWidth;
    private short[] asciiGlyph;
    private short[] cyrillicGlyph;
    private int fallbackGlyph = -1;
    private int glyphHeight;
    private int spaceWidth = DEFAULT_SPACE_WIDTH;
    private String loadedConfigPath;

    /**
     * Loads one Quantum-compatible font strip. The first image row contains
     * opaque black glyph separators; all remaining rows are glyph pixels.
     * The configuration file is UTF-8 and supplies image paths plus CHARS.
     */
    public void loadFont(String configPath) {
        if (configPath == null) return;

        try {
            String config = readResourceText(configPath);
            String normalPath = getConfigValue(config, "IMG");
            String selectedPath = getConfigValue(config, "SELECTED_IMG");
            String activePath = getConfigValue(config, "ACTIVE_IMG");
            String characters = getConfigValue(config, "CHARS");
            String configuredSpace = getConfigValue(config, "SPACE");

            if (normalPath == null || characters == null || characters.length() == 0) {
                throw new IOException("Invalid font config: " + configPath);
            }
            if (selectedPath == null) selectedPath = normalPath;
            if (activePath == null) activePath = normalPath;

            // English and Russian currently use the same Quantum atlas. Keep
            // it resident and only rebuild character lookup tables on a live
            // language switch instead of allocating another pair of images.
            if (normalImage != null && normalPath.equals(normalImagePath)
                    && selectedPath.equals(selectedImagePath)
                    && characters.equals(loadedCharacters)) {
                updateConfiguredSpace(configuredSpace);
                if (!activePath.equals(activeLoadedPath)) {
                    activeImage = null;
                    activeImagePath = activePath;
                    activeLoadedPath = activePath;
                }
                buildGlyphLookup(characters);
                loadedConfigPath = configPath;
                return;
            }

            Image rawNormal = Image.createImage(normalPath);
            Image rawSelected = normalPath.equals(selectedPath)
                    ? rawNormal : Image.createImage(selectedPath);
            int imageWidth = rawNormal.getWidth();
            int imageHeight = rawNormal.getHeight();

            if (imageHeight <= 1 || rawSelected.getWidth() != imageWidth
                    || rawSelected.getHeight() != imageHeight) {
                throw new IOException("Incompatible font images: " + configPath);
            }

            int[] markerRow = new int[imageWidth];
            rawNormal.getRGB(markerRow, 0, imageWidth, 0, 0, imageWidth, 1);
            int[] boundaries = buildBoundaries(markerRow, characters.length(), imageWidth);

            int[] newGlyphX = new int[characters.length()];
            int[] newGlyphWidth = new int[characters.length()];
            for (int glyph = 0; glyph < characters.length(); ++glyph) {
                int width = boundaries[glyph + 1] - boundaries[glyph];
                if (width <= 0) throw new IOException("Invalid glyph width: " + configPath);
                newGlyphX[glyph] = boundaries[glyph];
                newGlyphWidth[glyph] = width;
            }

            int newSpaceWidth = averageWidth(newGlyphWidth);
            if (configuredSpace != null) {
                try {
                    newSpaceWidth = Integer.parseInt(configuredSpace);
                } catch (NumberFormatException ignored) {
                    // Keep the measured value when an optional setting is bad.
                }
            }
            if (newSpaceWidth <= 0) newSpaceWidth = DEFAULT_SPACE_WIDTH;

            // Assign only after every resource and metric has been validated,
            // so changing language cannot leave a half-loaded font behind.
            normalImage = Image.createImage(rawNormal, 0, 1, imageWidth, imageHeight - 1, 0);
            selectedImage = rawSelected == rawNormal ? normalImage
                    : Image.createImage(rawSelected, 0, 1, imageWidth, imageHeight - 1, 0);
            activeImage = normalPath.equals(activePath) ? normalImage : null;
            activeImagePath = activeImage == null ? activePath : null;
            normalImagePath = normalPath;
            selectedImagePath = selectedPath;
            activeLoadedPath = activePath;
            loadedCharacters = characters;
            sourceImageWidth = imageWidth;
            sourceImageHeight = imageHeight;
            glyphX = newGlyphX;
            glyphWidth = newGlyphWidth;
            glyphHeight = imageHeight - 1;
            spaceWidth = newSpaceWidth;
            buildGlyphLookup(characters);
            loadedConfigPath = configPath;
        } catch (Exception e) {
            DebugLogger.logException("FontRenderer.loadFont", e);
        } catch (OutOfMemoryError e) {
            DebugLogger.logOutOfMemory("FontRenderer.loadFont", e);
        }
    }

    /**
     * Compatibility entry point retained for existing callers. New code should
     * call loadFont with a language-specific configuration path.
     */
    public void loadLargeFont(String path) {
        if (path != null && endsWith(path, ".txt")) {
            loadFont(path);
        } else if (normalImage == null) {
            loadFont(TextStrings.getFontConfigPath());
        }
    }

    /** The unified Quantum strip serves both former large and small fonts. */
    public void loadSmallFont(String ignoredPath) {
        if (normalImage == null) {
            loadFont(TextStrings.getFontConfigPath());
        }
    }

    /** Keep the shared font resident; dialogs no longer thrash image memory. */
    public void unloadSmallFont() {
    }

    public boolean isSmallFontLoaded() {
        return normalImage != null;
    }

    public int getLargeCharHeight() {
        return glyphHeight > 0 ? glyphHeight : 12;
    }

    public int getSmallCharHeight() {
        return glyphHeight > 0 ? glyphHeight : 12;
    }

    public int getSmallSpaceWidth() {
        return spaceWidth;
    }

    public void drawLargeString(String text, Graphics graphics, int x, int y) {
        drawLargeString(text, graphics, x, y, STYLE_NORMAL);
    }

    /** Draws a menu/UI string with the normal, selected or active Quantum tint. */
    public void drawLargeString(String text, Graphics graphics, int x, int y, int style) {
        if (text == null) return;
        if (normalImage == null) {
            graphics.drawString(text, x, y, Graphics.TOP | Graphics.LEFT);
            return;
        }

        Image image = getStyleImage(style);
        for (int i = 0; i < text.length(); ++i) {
            char character = text.charAt(i);
            int glyph = findGlyph(character);
            if (glyph < 0) {
                x += character == '\t' ? spaceWidth << 2 : spaceWidth;
            } else {
                int width = glyphWidth[glyph];
                graphics.drawRegion(image, glyphX[glyph], 0, width, glyphHeight,
                        0, x, y, Graphics.TOP | Graphics.LEFT);
                x += width;
            }
        }
    }

    public int getLargeTextWidth(String text) {
        if (text == null) return 0;
        int width = 0;
        for (int i = 0; i < text.length(); ++i) {
            width += getGlyphAdvance(text.charAt(i), 0);
        }
        return width;
    }

    public void drawCenteredNumber(int value, Graphics graphics, int centerX, int y) {
        String text = Integer.toString(value);
        drawLargeString(text, graphics, centerX - (getLargeTextWidth(text) >> 1), y);
    }

    public void drawSmallString(String text, Graphics graphics, int x, int y) {
        if (text == null) return;
        for (int i = 0; i < text.length(); ++i) {
            x += drawSmallChar(text.charAt(i), graphics, x, y);
        }
    }

    /** Draws one glyph and returns the horizontal advance for dialog layout. */
    public int drawSmallChar(char character, Graphics graphics, int x, int y) {
        int glyph = findGlyph(character);
        if (glyph < 0) {
            return character == '\t' ? spaceWidth << 2 : spaceWidth;
        }

        if (normalImage == null) {
            graphics.drawChar(character, x, y, Graphics.TOP | Graphics.LEFT);
            return spaceWidth;
        }

        int width = glyphWidth[glyph];
        graphics.drawRegion(normalImage, glyphX[glyph], 0, width, glyphHeight,
                0, x, y, Graphics.TOP | Graphics.LEFT);
        return width + SMALL_LETTER_SPACING;
    }

    public int getSmallTextWidth(String text) {
        if (text == null) return 0;
        int width = 0;
        for (int i = 0; i < text.length(); ++i) {
            width += getGlyphAdvance(text.charAt(i), SMALL_LETTER_SPACING);
        }
        return width;
    }

    public int getSmallCharWidth(char character) {
        return getGlyphAdvance(character, SMALL_LETTER_SPACING);
    }

    public String getLoadedConfigPath() {
        return loadedConfigPath;
    }

    private Image getStyleImage(int style) {
        if (style == STYLE_SELECTED && selectedImage != null) return selectedImage;
        if (style == STYLE_ACTIVE) {
            ensureActiveImage();
            if (activeImage != null) return activeImage;
        }
        return normalImage;
    }

    /** Active/red glyphs are not needed by ordinary rendering, so load them lazily. */
    private void ensureActiveImage() {
        if (activeImage != null || activeImagePath == null) return;
        try {
            Image raw = Image.createImage(activeImagePath);
            if (raw.getWidth() != sourceImageWidth || raw.getHeight() != sourceImageHeight) {
                throw new IOException("Incompatible active font image");
            }
            activeImage = Image.createImage(raw, 0, 1, sourceImageWidth, sourceImageHeight - 1, 0);
        } catch (Exception e) {
            DebugLogger.logException("FontRenderer.activeImage", e);
            activeImagePath = null;
        } catch (OutOfMemoryError e) {
            DebugLogger.logOutOfMemory("FontRenderer.activeImage", e);
            activeImagePath = null;
        }
    }

    private int getGlyphAdvance(char character, int spacing) {
        int glyph = findGlyph(character);
        if (glyph < 0) {
            return (character == '\t' ? spaceWidth << 2 : spaceWidth) + spacing;
        }
        return glyphWidth[glyph] + spacing;
    }

    private int findGlyph(char character) {
        if (character == ' ') return -1;
        int glyph = -1;
        if (character < ASCII_LIMIT && asciiGlyph != null) {
            glyph = asciiGlyph[character] - 1;
        } else if (character >= CYRILLIC_BASE && character < CYRILLIC_BASE + CYRILLIC_COUNT
                && cyrillicGlyph != null) {
            glyph = cyrillicGlyph[character - CYRILLIC_BASE] - 1;
        }
        return glyph >= 0 ? glyph : fallbackGlyph;
    }

    private void buildGlyphLookup(String characters) {
        asciiGlyph = new short[ASCII_LIMIT];
        cyrillicGlyph = new short[CYRILLIC_COUNT];
        fallbackGlyph = -1;

        for (int glyph = 0; glyph < characters.length(); ++glyph) {
            char character = characters.charAt(glyph);
            if (character < ASCII_LIMIT) {
                asciiGlyph[character] = (short)(glyph + 1);
            } else if (character >= CYRILLIC_BASE && character < CYRILLIC_BASE + CYRILLIC_COUNT) {
                cyrillicGlyph[character - CYRILLIC_BASE] = (short)(glyph + 1);
            }
            if (character == '?') fallbackGlyph = glyph;
        }
        if (fallbackGlyph < 0 && glyphWidth.length > 0) fallbackGlyph = 0;
    }

    private static int[] buildBoundaries(int[] markerRow, int glyphCount, int imageWidth)
            throws IOException {
        int[] boundaries = new int[glyphCount + 1];
        boundaries[0] = 0;
        int markerCount = 0;

        for (int x = 0; x < markerRow.length; ++x) {
            if (markerRow[x] == 0xFF000000) {
                markerCount++;
                if (markerCount < glyphCount) boundaries[markerCount] = x;
            }
        }
        if (markerCount != glyphCount - 1) {
            throw new IOException("Font marker count mismatch");
        }
        boundaries[glyphCount] = imageWidth;
        return boundaries;
    }

    private void updateConfiguredSpace(String configuredSpace) {
        if (configuredSpace == null) return;
        try {
            int value = Integer.parseInt(configuredSpace);
            if (value > 0) spaceWidth = value;
        } catch (NumberFormatException ignored) {
        }
    }

    private static int averageWidth(int[] widths) {
        int total = 0;
        for (int i = 0; i < widths.length; ++i) total += widths[i];
        return widths.length == 0 ? DEFAULT_SPACE_WIDTH : total / widths.length;
    }

    private static boolean endsWith(String text, String suffix) {
        return text.length() >= suffix.length()
                && text.substring(text.length() - suffix.length()).equals(suffix);
    }

    private static String getConfigValue(String config, String key) {
        int lineStart = 0;
        while (lineStart < config.length()) {
            int lineEnd = config.indexOf('\n', lineStart);
            if (lineEnd < 0) lineEnd = config.length();
            String line = config.substring(lineStart, lineEnd).trim();
            lineStart = lineEnd + 1;

            if (line.length() == 0 || line.charAt(0) == '#') continue;
            int equals = line.indexOf('=');
            if (equals < 0) continue;
            if (line.substring(0, equals).trim().equals(key)) {
                return line.substring(equals + 1).trim();
            }
        }
        return null;
    }

    private static String readResourceText(String path) throws IOException {
        InputStream input = FontRenderer.class.getResourceAsStream(path);
        if (input == null) throw new IOException("Font config not found: " + path);

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
