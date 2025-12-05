import java.util.Vector;

public final class GameObject {
    public Transform3D transform;
    public int objectType;
    public int detonationTimer;
    private Vector torsoSpriteIds;
    private Vector legsSpriteIds;
    public int spriteFrameIndex;
    public int health;
    public int stateTimer;
    public int aiState;
    public Point2D screenPos;
    public int screenHeight;
    public Texture torsoTexture;
    public Texture legsTexture;
    public int spriteWidth1;
    public int spriteHeight1;
    public int spriteWidth2;
    public int spriteHeight2;

    public GameObject(Transform3D var1, int var2, int var3, int var4) {

        this.transform = var1;
        this.screenPos = new Point2D(0, 0);
        this.objectType = var3;
        this.detonationTimer = var4;
        this.torsoSpriteIds = new Vector();
        this.legsSpriteIds = new Vector();
        this.spriteFrameIndex = 0;
        this.screenHeight = 0;
        this.torsoTexture = null;
        this.legsTexture = null;
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
                this.health = MainGameCanvas.HP_3001[GameEngine.difficultyLevel];
                this.aiState = 0;
                break;
            case 3002:
                this.health = MainGameCanvas.HP_3002[GameEngine.difficultyLevel];
                this.aiState = 0;
                break;
            case 3003:
                this.health = MainGameCanvas.HP_3003[GameEngine.difficultyLevel];
                this.aiState = 0;
                break;
            case 3004:
                this.health = MainGameCanvas.HP_3004[GameEngine.difficultyLevel];
                this.aiState = 0;
                break;
            case 3005:
                this.health = MainGameCanvas.HP_3005[GameEngine.difficultyLevel];
                this.aiState = 0;
                break;
            case 3006:
                this.health = MainGameCanvas.HP_3006[GameEngine.difficultyLevel];
                this.aiState = 0;
                break;
            default:

        }
    }

    public final int getMovementSpeed() {
        switch(this.objectType) {
            case 3001:
                return MainGameCanvas.SPEED_3001[GameEngine.difficultyLevel];
            case 3002:
                return MainGameCanvas.SPEED_3002[GameEngine.difficultyLevel];
            case 3003:
                return MainGameCanvas.SPEED_3003[GameEngine.difficultyLevel];
            case 3004:
                return MainGameCanvas.SPEED_3004[GameEngine.difficultyLevel];
            case 3005:
                return MainGameCanvas.SPEED_3005[GameEngine.difficultyLevel];
            case 3006:
                return MainGameCanvas.SPEED_3006[GameEngine.difficultyLevel];
            default:
                return 65536;
        }
    }

    public final void addToWorld(GameWorld var1) {
        var1.getSectorAtPoint(this.transform.x, this.transform.z).addDynamicObject(this);
    }

    public final byte getCurrentSprite1() {
        if (this.spriteFrameIndex > -1 && this.spriteFrameIndex < this.torsoSpriteIds.size()) {
            return ((Byte)this.torsoSpriteIds.elementAt(this.spriteFrameIndex)).byteValue();
        }
        return 0;
    }

    public final byte getCurrentSprite2() {
        if (this.spriteFrameIndex > -1 && this.spriteFrameIndex < this.legsSpriteIds.size()) {
            return ((Byte)this.legsSpriteIds.elementAt(this.spriteFrameIndex)).byteValue();
        }
        return 0;
    }

    public final void addSpriteFrame(byte var1, byte var2) {
        this.torsoSpriteIds.addElement(new Byte(var1));
        this.legsSpriteIds.addElement(new Byte(var2));
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
        this.spriteWidth1 = MathUtils.fixedPointDivide(this.torsoTexture.width << 16, this.screenPos.y) * 120 - 131072 >> 18;
        this.spriteHeight1 = MathUtils.fixedPointDivide(this.torsoTexture.height << 16, this.screenPos.y) * 120 - 131072 >> 18;
    }

    public final void calculateSpriteSize2() {
        this.spriteWidth2 = MathUtils.fixedPointDivide(this.legsTexture.width << 16, this.screenPos.y) * 120 + 65536 >> 17;
        this.spriteHeight2 = MathUtils.fixedPointDivide(this.legsTexture.height << 16, this.screenPos.y) * 120 + 65536 >> 17;
    }
}