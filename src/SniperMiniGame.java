import java.io.DataInputStream;
import java.io.InputStream;

public class SniperMiniGame {

    // Приватные поля с геттерами
    private int[] paletteRegular;
    private int[] paletteGray;
    private int[] paletteRedTint;
    private int[] paletteGrayRed;

    private int scopeVelocityX;
    private int scopeVelocityY;
    private int scopeWobbleIndex;
    private int enemyUpdateCounter;
    private int enemySpawnTimer;
    private int activeEnemyCount;
    private int lastInputDirection;
    private int scopePositionX;
    private int scopePositionY;

    public SniperMiniGame() {
        reset();
    }

    /**
     * Сброс состояния для новой игры
     */
    public void reset() {
        this.paletteRegular = null;
        this.paletteGray = null;
        this.paletteRedTint = null;
        this.paletteGrayRed = null;
        this.scopeVelocityX = 0;
        this.scopeVelocityY = 0;
        this.scopeWobbleIndex = 0;
        this.enemyUpdateCounter = 0;
        this.enemySpawnTimer = 0;
        this.activeEnemyCount = 0;
        this.lastInputDirection = 0;
        this.scopePositionX = 0;
        this.scopePositionY = 0;
    }

    /**
     * Инициализация позиции прицела перед началом уровня
     */
    public void initScopePosition() {
        this.scopeVelocityX = 0;
        this.scopeVelocityY = 0;
        this.scopePositionX = PortalRenderer.HALF_VIEWPORT_WIDTH - 32;
        this.scopePositionY = PortalRenderer.HALF_VIEWPORT_HEIGHT - 32;
    }

    /**
     * Сброс счётчиков врагов
     */
    public void resetEnemyCounters() {
        this.enemySpawnTimer = 0;
        this.enemyUpdateCounter = 0;
        this.activeEnemyCount = 0;
    }

    // ==================== Геттеры ====================

    public int[] getPaletteRegular() { return paletteRegular; }
    public int[] getPaletteGray() { return paletteGray; }
    public int[] getPaletteRedTint() { return paletteRedTint; }
    public int[] getPaletteGrayRed() { return paletteGrayRed; }

    public int getScopePositionX() { return scopePositionX; }
    public int getScopePositionY() { return scopePositionY; }

    // ==================== Загрузка ресурсов ====================

    public void loadResources(int level, int width, int height,
                              byte[] pixels, byte[] mask, byte[] sight) {
        try {
            String basePath = "/" + (level == 0
                    ? "gamedata/sniperminigame/ss1"
                    : "gamedata/sniperminigame/ss2");

            InputStream stream = getClass().getResourceAsStream(basePath);
            DataInputStream dataInput = new DataInputStream(stream);
            dataInput.skipBytes(1);
            byte compression = dataInput.readByte();
            short imageWidth = dataInput.readShort();
            short imageHeight = dataInput.readShort();

            if (imageWidth != width || imageHeight != height) {
                throw new IllegalStateException("Image dimensions mismatch");
            }

            short paletteSize = dataInput.readShort();
            int pixelCount = width * height;

            int compressedSize = dataInput.readInt();
            byte[] compressed = new byte[compressedSize];
            dataInput.readFully(compressed, 0, compressedSize);
            LevelLoader.decompressSprite(compressed, 0, pixels, 0, pixelCount, compression);

            initPalettes(paletteSize, dataInput);
            dataInput.close();

            loadSightMask(sight);
            loadSceneMask(basePath + "_mask", mask, pixels, pixelCount);

        } catch (Exception e) {
            // Log error
        } catch (OutOfMemoryError e) {
            // Handle OOM
        }
    }

    private void initPalettes(int paletteSize, DataInputStream dataInput)
            throws java.io.IOException {
        this.paletteRegular = new int[paletteSize];
        this.paletteGray = new int[paletteSize];
        this.paletteRedTint = new int[paletteSize];
        this.paletteGrayRed = new int[paletteSize];

        for (int i = 0; i < paletteSize; ++i) {
            int r = dataInput.readByte() & 255;
            int g = dataInput.readByte() & 255;
            int b = dataInput.readByte() & 255;

            this.paletteRegular[i] = (r << 16) | (g << 8) | b;
            this.paletteRedTint[i] = this.paletteRegular[i] | 0xFF0000;

            int gray = (r + g + b) / 3;
            gray = gray + (96 - (gray >> 2));
            if (gray > 255) gray = 255;

            this.paletteGray[i] = (gray << 16) | (gray << 8) | gray;
            this.paletteGrayRed[i] = this.paletteGray[i] | 0xFF0000;
        }
    }

    private void loadSightMask(byte[] sight) throws java.io.IOException {
        InputStream stream = getClass().getResourceAsStream("/gamedata/sniperminigame/sight");
        DataInputStream dataInput = new DataInputStream(stream);
        dataInput.skipBytes(8);
        int compressedSize = dataInput.readInt();
        byte[] compressed = new byte[compressedSize];
        dataInput.readFully(compressed, 0, compressedSize);
        dataInput.close();
        LevelLoader.decompressSprite(compressed, 0, sight, 0, 4096, 1);
    }

    private void loadSceneMask(String path, byte[] mask, byte[] pixels, int pixelCount)
            throws java.io.IOException {
        InputStream stream = getClass().getResourceAsStream(path);
        DataInputStream dataInput = new DataInputStream(stream);
        dataInput.skipBytes(8);
        int compressedSize = dataInput.readInt();
        byte[] compressed = new byte[compressedSize];
        dataInput.readFully(compressed, 0, compressedSize);
        dataInput.close();
        LevelLoader.decompressSprite(compressed, 0, mask, 0, pixelCount, 1);

        for (int i = 0; i < pixelCount; ++i) {
            mask[i] = (mask[i] == 0) ? -1 : pixels[i];
        }
    }

    // ==================== Игровая логика ====================

    public boolean updateLogic(int[] states, int[] types, int[] timers,
                               int[] freeSlots, int[] positions,
                               int[] starts, int[] ends, int[] speeds) {

        if (!updateEnemySpawning(states, types, timers, freeSlots, positions, starts, ends)) {
            return false;
        }

        ++this.enemySpawnTimer;
        ++this.enemyUpdateCounter;

        updateEnemyAnimation(states, speeds);
        updateEnemyMovement(states, positions, starts, ends, speeds);
        updateEnemyAttacks(states, timers);
        updateScopePosition();

        return true;
    }

    private boolean updateEnemySpawning(int[] states, int[] types, int[] timers,
                                        int[] freeSlots, int[] positions,
                                        int[] starts, int[] ends) {
        if (this.enemySpawnTimer < 200) {
            return true;
        }

        this.enemySpawnTimer = 0;
        int freeCount = 0;

        for (int i = 0; i < 8; ++i) {
            if (states[i] == 0) {
                freeSlots[freeCount++] = i;
            }
        }

        if (this.activeEnemyCount < 20) {
            if (freeCount > 0) {
                spawnEnemy(states, types, timers, freeSlots, positions, starts, ends, freeCount);
            }
        } else if (freeCount == 8) {
            return false; // Уровень пройден
        }

        return true;
    }

    private void spawnEnemy(int[] states, int[] types, int[] timers,
                            int[] freeSlots, int[] positions,
                            int[] starts, int[] ends, int freeCount) {
        int enemyType = GameEngine.random.nextInt() & 1;
        int slotIndex = freeSlots[(GameEngine.random.nextInt() & 7) % freeCount];

        types[slotIndex] = enemyType;
        int spawnDelay = GameEngine.random.nextInt() & Integer.MAX_VALUE;
        timers[slotIndex] = spawnDelay % MainGameCanvas.ENEMY_SPAWN_DELAY_VARIANCE[GameEngine.difficultyLevel]
                + MainGameCanvas.ENEMY_SPAWN_DELAY_BASE[GameEngine.difficultyLevel];
        positions[slotIndex] = starts[slotIndex];
        states[slotIndex] = (starts[slotIndex] > ends[slotIndex]) ? 1 : 5;
        ++this.activeEnemyCount;
    }

    private void updateEnemyAnimation(int[] states, int[] speeds) {
        if (this.enemyUpdateCounter % 10 != 0) return;

        for (int i = 0; i < 8; ++i) {
            switch (states[i]) {
                case 1: states[i] = 2; break;
                case 2: states[i] = 1; break;
                case 5: states[i] = 6; break;
                case 6: states[i] = 5; break;
            }
        }
    }

    private void updateEnemyMovement(int[] states, int[] positions,
                                     int[] starts, int[] ends, int[] speeds) {
        for (int i = 0; i < 8; ++i) {
            if (speeds[i] == 0) continue;

            boolean shouldMove = (speeds[i] == 1 && this.enemyUpdateCounter % 7 == 0)
                    || (speeds[i] == 2 && this.enemyUpdateCounter % 5 == 0);

            if (!shouldMove) continue;

            if (states[i] == 1 || states[i] == 2) {
                moveEnemyLeft(states, positions, starts, ends, i);
            } else if (states[i] == 5 || states[i] == 6) {
                moveEnemyRight(states, positions, starts, ends, i);
            }
        }
    }

    private void moveEnemyLeft(int[] states, int[] positions,
                               int[] starts, int[] ends, int i) {
        int newPos = positions[i] - 1;
        int targetPos = (starts[i] < ends[i]) ? starts[i] : ends[i];
        if (newPos < targetPos) {
            newPos = targetPos;
            states[i] = 5;
        }
        positions[i] = newPos;
    }

    private void moveEnemyRight(int[] states, int[] positions,
                                int[] starts, int[] ends, int i) {
        int newPos = positions[i] + 1;
        int targetPos = (starts[i] > ends[i]) ? starts[i] : ends[i];
        if (newPos > targetPos) {
            newPos = targetPos;
            states[i] = 1;
        }
        positions[i] = newPos;
    }

    private void updateEnemyAttacks(int[] states, int[] timers) {
        for (int i = 0; i < 8; ++i) {
            int state = states[i];
            if (state == 0) continue;

            if (timers[i] <= 0) {
                switch (state) {
                    case 1: case 2: case 5: case 6:
                        states[i] = 3;
                        int delay = GameEngine.random.nextInt() & Integer.MAX_VALUE;
                        timers[i] = delay % MainGameCanvas.ENEMY_ATTACK_DELAY_VARIANCE[GameEngine.difficultyLevel]
                                + MainGameCanvas.ENEMY_ATTACK_DELAY_BASE[GameEngine.difficultyLevel];
                        break;
                    case 3:
                        states[i] = 4;
                        timers[i] = 1;
                        break;
                }
            }
            --timers[i];
        }
    }

    private void updateScopePosition() {
        // Wobble effect
        final int[] deltaX = {0, 0, -1, 1, -1, 1, -1, 1};
        final int[] deltaY = {-1, 1, 0, 0, -1, 1, 1, -1};

        int direction = this.scopeWobbleIndex;
        this.scopeWobbleIndex = (direction + 1) & 7;
        this.scopePositionX += deltaX[direction];
        this.scopePositionY += deltaY[direction];

        // Input handling
        processInput();
        applyVelocity();
        clampScopePosition();
        applyFriction();
    }

    private void processInput() {
        byte inputDirection = 0;

        if (GameEngine.inputLookUp) {
            if (this.lastInputDirection == 3) --this.scopeVelocityX;
            inputDirection = 3;
        }
        if (GameEngine.inputLookDown) {
            if (this.lastInputDirection == 4) ++this.scopeVelocityX;
            inputDirection = 4;
        }
        if (GameEngine.inputForward) {
            if (this.lastInputDirection == 1) --this.scopeVelocityY;
            inputDirection = 1;
        }
        if (GameEngine.inputBackward) {
            if (this.lastInputDirection == 2) ++this.scopeVelocityY;
            inputDirection = 2;
        }

        this.lastInputDirection = inputDirection;
    }

    private void applyVelocity() {
        this.scopePositionX += this.scopeVelocityX >> 2;
        this.scopePositionY += this.scopeVelocityY >> 2;
    }

    private void clampScopePosition() {
        int maxX = PortalRenderer.VIEWPORT_WIDTH - 32;
        int maxY = PortalRenderer.VIEWPORT_HEIGHT - 32;

        if (this.scopePositionX > maxX) {
            this.scopePositionX = maxX;
            this.scopeVelocityX = 0;
        }
        if (this.scopePositionX < -31) {
            this.scopePositionX = -31;
            this.scopeVelocityX = 0;
        }
        if (this.scopePositionY > maxY) {
            this.scopePositionY = maxY;
            this.scopeVelocityY = 0;
        }
        if (this.scopePositionY < -31) {
            this.scopePositionY = -31;
            this.scopeVelocityY = 0;
        }
    }

    private void applyFriction() {
        if (this.lastInputDirection != 0) return;

        if (this.scopeVelocityX > 0) --this.scopeVelocityX;
        if (this.scopeVelocityX < 0) ++this.scopeVelocityX;
        if (this.scopeVelocityY > 0) --this.scopeVelocityY;
        if (this.scopeVelocityY < 0) ++this.scopeVelocityY;
    }
}