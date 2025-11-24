import java.util.Vector;

public final class GameObject {
    public Transform3D transform;
    public int objectType;
    public int spawnDelay;
    private Vector spriteIds1;
    private Vector spriteIds2;
    public int currentState;
    public int health;
    public int stateTimer;
    public int aiState;
    public Point2D screenPos;
    public int screenHeight;
    public Texture texture1;
    public Texture texture2;
    public int spriteWidth1;
    public int spriteHeight1;
    public int spriteWidth2;
    public int spriteHeight2;

    public GameObject(Transform3D var1, int var2, int var3, int var4) {
        // super(); - в Java ME для классов Object вызов super() не обязателен
        this.transform = var1;
        this.screenPos = new Point2D(0, 0);
        this.objectType = var3;
        this.spawnDelay = var4;
        this.spriteIds1 = new Vector();
        this.spriteIds2 = new Vector();
        this.currentState = 0;
        this.screenHeight = 0;
        this.texture1 = null;
        this.texture2 = null;
        this.spriteWidth1 = 0;
        this.spriteHeight1 = 0;
        this.spriteWidth2 = 0;
        this.spriteHeight2 = 0;
        this.health = 0;
        this.stateTimer = 0;
        this.aiState = -1;

        switch(var3) {
            case 10:
            case 12:
                this.aiState = 0;
                break;
            case 3001:
                this.health = MainGameCanvas.var_146a[GameEngine.difficultyLevel];
                this.aiState = 0;
                break;
            case 3002:
                this.health = MainGameCanvas.var_1492[GameEngine.difficultyLevel];
                this.aiState = 0;
                break;
            case 3003:
                this.health = MainGameCanvas.var_139c[GameEngine.difficultyLevel];
                this.aiState = 0;
                break;
            case 3004:
                this.health = MainGameCanvas.var_13e4[GameEngine.difficultyLevel];
                this.aiState = 0;
                break;
            case 3005:
                this.health = MainGameCanvas.var_13fe[GameEngine.difficultyLevel];
                this.aiState = 0;
                break;
            case 3006:
                this.health = MainGameCanvas.var_143e[GameEngine.difficultyLevel];
                this.aiState = 0;
                break;
            default:
                // ничего не делаем
        }
    }

    public final int getMovementSpeed() {
        switch(this.objectType) {
            case 3001:
                return MainGameCanvas.var_1a4a[GameEngine.difficultyLevel];
            case 3002:
                return MainGameCanvas.var_1a74[GameEngine.difficultyLevel];
            case 3003:
                return MainGameCanvas.var_1966[GameEngine.difficultyLevel];
            case 3004:
                return MainGameCanvas.var_19bb[GameEngine.difficultyLevel];
            case 3005:
                return MainGameCanvas.var_19fd[GameEngine.difficultyLevel];
            case 3006:
                return MainGameCanvas.var_1a1b[GameEngine.difficultyLevel];
            default:
                return 65536;
        }
    }

    public final void addToWorld(GameWorld var1) {
        var1.getSectorAtPoint(this.transform.x, this.transform.z).addDynamicObject(this);
    }

    public final byte getCurrentSprite1() {
        if (this.currentState > -1 && this.currentState < this.spriteIds1.size()) {
            return ((Byte)this.spriteIds1.elementAt(this.currentState)).byteValue();
        }
        return 0;
    }

    public final byte getCurrentSprite2() {
        if (this.currentState > -1 && this.currentState < this.spriteIds2.size()) {
            return ((Byte)this.spriteIds2.elementAt(this.currentState)).byteValue();
        }
        return 0;
    }

    public final void addSpriteFrame(byte var1, byte var2) {
        this.spriteIds1.addElement(new Byte(var1));
        this.spriteIds2.addElement(new Byte(var2));
    }

    public final boolean compareDepth(GameObject var1) {
        return this.screenPos.y < var1.screenPos.y;
    }

    public final boolean projectToScreen() {
        if (this.screenPos.y <= 0) {
            return false;
        } else {
            this.screenPos.x = (int)((long) MathUtils.fixedPointDivide(this.screenPos.x, this.screenPos.y) * 7864320L >> 16);
            this.screenHeight = MathUtils.fixedPointDivide(this.screenHeight, this.screenPos.y) * 120;
            return true;
        }
    }

    public final void calculateSpriteSize1() {
        this.spriteWidth1 = MathUtils.fixedPointDivide(this.texture1.width << 16, this.screenPos.y) * 120 - 131072 >> 18;
        this.spriteHeight1 = MathUtils.fixedPointDivide(this.texture1.height << 16, this.screenPos.y) * 120 - 131072 >> 18;
    }

    public final void calculateSpriteSize2() {
        this.spriteWidth2 = MathUtils.fixedPointDivide(this.texture2.width << 16, this.screenPos.y) * 120 + 65536 >> 17;
        this.spriteHeight2 = MathUtils.fixedPointDivide(this.texture2.height << 16, this.screenPos.y) * 120 + 65536 >> 17;
    }
}