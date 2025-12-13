import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * Класс для рендеринга текста с использованием bitmap-шрифтов.
 * Поддерживает два размера шрифта: большой (для меню) и маленький (для диалогов).
 */
public class FontRenderer {

    // ==================== Константы для большого шрифта ====================

    private static final int LARGE_FONT_CHARS_PER_ROW = 18;
    private static final int LARGE_FONT_CHAR_HEIGHT = 23;
    private static final int LARGE_FONT_SPACE_WIDTH = 4;

    /** Позиции символов в текстуре большого шрифта */
    private static final int[] LARGE_FONT_OFFSETS = {
            1, 11, 22, 31, 42, 52, 62, 70, 82, 91, 101, 112, 120, 130, 142, 151, 161, 171,
            2, 12, 20, 31, 40, 51, 61, 72, 80, 90, 100, 110, 120, 130, 142, 151, 160, 170,
            1, 12, 21, 31, 41, 51, 61, 71, 81, 91, 100, 110, 120, 130, 140, 150, 160, 170
    };

    /** Ширина символов большого шрифта */
    private static final int[] LARGE_FONT_WIDTHS = {
            9, 9, 7, 8, 7, 7, 7, 10, 6, 6, 9, 6, 10, 10, 7, 9, 8, 8,
            7, 6, 10, 8, 10, 9, 8, 7, 4, 4, 4, 8, 4, 4, 7, 4, 0, 0,
            8, 6, 8, 8, 9, 8, 8, 8, 8, 8, 0, 0, 0, 0, 0, 0, 0, 0
    };

    // ==================== Константы для маленького шрифта ====================

    private static final int SMALL_FONT_CHARS_PER_ROW = 26;
    private static final int SMALL_FONT_CHAR_HEIGHT = 10;
    private static final int SMALL_FONT_SPACE_WIDTH = 3;

    /** Позиции символов в текстуре маленького шрифта */
    private static final int[] SMALL_FONT_X = {
            0, 8, 14, 21, 29, 36, 42, 49, 58, 64, 71, 78, 85, 91, 98, 106, 112, 120, 126, 134, 140, 148, 154, 162, 169, 176,
            1, 8, 15, 22, 29, 36, 43, 50, 59, 65, 71, 80, 84, 92, 99, 106, 113, 121, 127, 135, 141, 148, 155, 162, 169, 177,
            1, 9, 15, 22, 29, 36, 43, 50, 57, 64, 71, 77, 85, 92, 99, 105, 112, 121, 127, 133, 140, 147, 154, 161, 168, 175
    };

    /** Ширина символов маленького шрифта */
    private static final int[] SMALL_FONT_WIDTHS = {
            6, 5, 6, 6, 5, 5, 6, 6, 3, 3, 5, 4, 5, 6, 6, 5, 6, 5, 5, 5, 6, 5, 7, 5, 5, 5,
            5, 5, 5, 5, 5, 4, 5, 5, 1, 2, 5, 2, 7, 5, 5, 5, 5, 3, 5, 2, 5, 5, 5, 4, 5, 3,
            4, 3, 4, 4, 5, 4, 4, 4, 4, 4, 1, 2, 1, 4, 1, 2, 4, 3, 2, 7, 0, 0, 0, 0, 0, 0
    };

    // ==================== Изображения шрифтов ====================

    private Image largeFontImage;
    private Image smallFontImage;

    // ==================== Буфер для координат (избегаем создания массивов) ====================

    private final int[] coordsBuffer = new int[2];

    // ==================== Конструктор ====================

    public FontRenderer() {
    }

    // ==================== Инициализация ====================

    /**
     * Загружает изображение большого шрифта
     */
    public void loadLargeFont(String path) {
        try {
            largeFontImage = Image.createImage(path);
        } catch (Exception e) {
        }
    }

    /**
     * Загружает изображение маленького шрифта
     */
    public void loadSmallFont(String path) {
        try {
            smallFontImage = Image.createImage(path);
        } catch (Exception e) {
        }
    }

    /**
     * Выгружает маленький шрифт для освобождения памяти
     */
    public void unloadSmallFont() {
        smallFontImage = null;
    }

    /**
     * Проверяет, загружен ли маленький шрифт
     */
    public boolean isSmallFontLoaded() {
        return smallFontImage != null;
    }

    // ==================== Геттеры констант ====================

    public int getLargeCharHeight() {
        return LARGE_FONT_CHAR_HEIGHT;
    }

    public int getSmallCharHeight() {
        return SMALL_FONT_CHAR_HEIGHT;
    }

    public int getSmallSpaceWidth() {
        return SMALL_FONT_SPACE_WIDTH;
    }

    // ==================== Рендеринг большого шрифта ====================

    /**
     * Рисует строку большим шрифтом
     */
    public void drawLargeString(String text, Graphics graphics, int x, int y) {
        text = text.toLowerCase();

        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);

            if (c == ' ') {
                x += LARGE_FONT_SPACE_WIDTH;
            } else {
                getLargeFontCoordinates(c, coordsBuffer);
                int fontIdx = coordsBuffer[1] * LARGE_FONT_CHARS_PER_ROW + coordsBuffer[0];
                int charWidth = LARGE_FONT_WIDTHS[fontIdx];
                int charX = LARGE_FONT_OFFSETS[fontIdx];
                int charY = coordsBuffer[1] * LARGE_FONT_CHAR_HEIGHT;

                graphics.drawRegion(largeFontImage, charX, charY, charWidth, LARGE_FONT_CHAR_HEIGHT,
                        0, x, y, 20);
                x += charWidth;
            }
        }
    }

    /**
     * Вычисляет ширину строки большим шрифтом
     */
    public int getLargeTextWidth(String text) {
        text = text.toLowerCase();
        int width = 0;

        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            if (c == ' ') {
                width += LARGE_FONT_SPACE_WIDTH;
            } else {
                getLargeFontCoordinates(c, coordsBuffer);
                int charWidth = LARGE_FONT_WIDTHS[coordsBuffer[1] * LARGE_FONT_CHARS_PER_ROW + coordsBuffer[0]];
                width += charWidth;
            }
        }

        return width;
    }

    /**
     * Рисует число по центру указанной позиции (для HUD)
     */
    public void drawCenteredNumber(int value, Graphics graphics, int centerX, int y) {
        String text = Integer.toString(value);
        int offset = getLargeTextWidth(text) / 2;
        drawLargeString(text, graphics, centerX - offset, y);
    }

    // ==================== Рендеринг маленького шрифта ====================

    /**
     * Рисует строку маленьким шрифтом
     */
    public void drawSmallString(String text, Graphics graphics, int x, int y) {
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);

            if (c == ' ') {
                x += SMALL_FONT_SPACE_WIDTH;
            } else {
                getSmallFontCoordinates(c, coordsBuffer);
                int fontIdx = coordsBuffer[1] * SMALL_FONT_CHARS_PER_ROW + coordsBuffer[0];
                int charWidth = SMALL_FONT_WIDTHS[fontIdx];
                int charX = SMALL_FONT_X[fontIdx];
                int charY = coordsBuffer[1] * SMALL_FONT_CHAR_HEIGHT;

                graphics.drawRegion(smallFontImage, charX, charY, charWidth, SMALL_FONT_CHAR_HEIGHT,
                        0, x, y, 20);
                x += charWidth + 1;
            }
        }
    }

    /**
     * Рисует один символ маленьким шрифтом и возвращает его ширину
     */
    public int drawSmallChar(char c, Graphics graphics, int x, int y) {
        if (c == ' ') {
            return SMALL_FONT_SPACE_WIDTH;
        }

        getSmallFontCoordinates(c, coordsBuffer);
        int fontIdx = coordsBuffer[1] * SMALL_FONT_CHARS_PER_ROW + coordsBuffer[0];
        int charWidth = SMALL_FONT_WIDTHS[fontIdx];
        int charX = SMALL_FONT_X[fontIdx];
        int charY = coordsBuffer[1] * SMALL_FONT_CHAR_HEIGHT;

        graphics.drawRegion(smallFontImage, charX, charY, charWidth, SMALL_FONT_CHAR_HEIGHT,
                0, x, y, 20);

        return charWidth + 1;
    }

    /**
     * Вычисляет ширину строки маленьким шрифтом
     */
    public int getSmallTextWidth(String text) {
        int width = 0;

        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            if (c == ' ') {
                width += SMALL_FONT_SPACE_WIDTH;
            } else {
                getSmallFontCoordinates(c, coordsBuffer);
                int charWidth = SMALL_FONT_WIDTHS[coordsBuffer[1] * SMALL_FONT_CHARS_PER_ROW + coordsBuffer[0]];
                width += charWidth + 1;
            }
        }

        return width;
    }

    /**
     * Получает ширину символа маленького шрифта
     */
    public int getSmallCharWidth(char c) {
        if (c == ' ') {
            return SMALL_FONT_SPACE_WIDTH;
        }
        getSmallFontCoordinates(c, coordsBuffer);
        return SMALL_FONT_WIDTHS[coordsBuffer[1] * SMALL_FONT_CHARS_PER_ROW + coordsBuffer[0]] + 1;
    }

    // ==================== Вспомогательные методы для координат символов ====================

    /**
     * Получает координаты символа в текстуре большого шрифта
     * @param c символ
     * @param result массив [x, y] для записи результата
     */
    private void getLargeFontCoordinates(char c, int[] result) {
        result[0] = LARGE_FONT_CHARS_PER_ROW - 1;
        result[1] = 2;

        if (c >= 'a' && c <= 'r') {
            result[0] = c - 'a';
            result[1] = 0;
        } else if (c >= 's' && c <= 'z') {
            result[0] = c - 's';
            result[1] = 1;
        } else if (c >= '0' && c <= '9') {
            result[0] = c - '0';
            result[1] = 2;
        } else {
            result[1] = 1;
            switch (c) {
                case '!': result[0] = 10; break;
                case '\'': result[0] = 15; break;
                case ',': result[0] = 9; break;
                case '.': result[0] = 8; break;
                case '/': result[0] = 14; break;
                case ':': result[0] = 12; break;
                case ';': result[0] = 13; break;
                case '?': result[0] = 11; break;
            }
        }
    }

    /**
     * Получает координаты символа в текстуре маленького шрифта
     * @param c символ
     * @param result массив [x, y] для записи результата
     */
    private void getSmallFontCoordinates(char c, int[] result) {
        result[0] = SMALL_FONT_CHARS_PER_ROW - 1;
        result[1] = 2;

        if (c >= 'A' && c <= 'Z') {
            result[0] = c - 'A';
            result[1] = 0;
        } else if (c >= 'a' && c <= 'z') {
            result[0] = c - 'a';
            result[1] = 1;
        } else if (c >= '0' && c <= '9') {
            result[0] = c - '0';
            result[1] = 2;
        } else {
            result[1] = 2;
            switch (c) {
                case '!': result[0] = 12; break;
                case '\'': result[0] = 18; break;
                case ',': result[0] = 11; break;
                case '-': result[0] = 17; break;
                case '.': result[0] = 10; break;
                case '/': result[0] = 16; break;
                case ':': result[0] = 14; break;
                case ';': result[0] = 15; break;
                case '?': result[0] = 13; break;
                case '@': result[0] = 19; break;
            }
        }
    }
}