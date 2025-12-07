import java.util.Vector;

public final class GameObject {
    public Transform3D transform;
    public int objectType;
    public int detonationTimer;
    private Vector upperBodySpriteIds;
    private Vector lowerBodySpriteIds;
    public int spriteFrameIndex;
    public int health;
    public int stateTimer;
    public int aiState;
    public Point2D projectionData;
    public int screenY;
    public Texture upperBodyTexture;
    public Texture lowerBodyTexture;
    public int upperBodyScreenWidth;
    public int upperBodyScreenHeight;
    public int lowerBodyScreenWidth;
    public int lowerBodyScreenHeight;

    public GameObject(Transform3D position, int var2, int type, int fuseTime) {

        this.transform = position;
        this.projectionData = new Point2D(0, 0);
        this.objectType = type;
        this.detonationTimer = fuseTime;
        this.upperBodySpriteIds = new Vector();
        this.lowerBodySpriteIds = new Vector();
        this.spriteFrameIndex = 0;
        this.screenY = 0;
        this.upperBodyTexture = null;
        this.lowerBodyTexture = null;
        this.upperBodyScreenWidth = 0;
        this.upperBodyScreenHeight = 0;
        this.lowerBodyScreenWidth = 0;
        this.lowerBodyScreenHeight = 0;
        this.health = 0;
        this.stateTimer = 0;
        this.aiState = -1;

        switch(type) {
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

    public final byte getCurrentUpperBodySpriteId() {
        if (this.spriteFrameIndex > -1 && this.spriteFrameIndex < this.upperBodySpriteIds.size()) {
            return ((Byte)this.upperBodySpriteIds.elementAt(this.spriteFrameIndex)).byteValue();
        }
        return 0;
    }

    public final byte getCurrentLowerBodySpriteId() {
        if (this.spriteFrameIndex > -1 && this.spriteFrameIndex < this.lowerBodySpriteIds.size()) {
            return ((Byte)this.lowerBodySpriteIds.elementAt(this.spriteFrameIndex)).byteValue();
        }
        return 0;
    }

    public final void addSpriteFrame(byte var1, byte var2) {
        this.upperBodySpriteIds.addElement(new Byte(var1));
        this.lowerBodySpriteIds.addElement(new Byte(var2));
    }

    public final boolean compareDepth(GameObject var1) {
        return this.projectionData.y < var1.projectionData.y;
    }

    public final boolean projectToScreen() {
        if (this.projectionData.y <= 0) {
            return false;
        } else {
            this.projectionData.x = (int)((long) MathUtils.fixedPointDivide(this.projectionData.x, this.projectionData.y) * 7864320L >> 16);
            this.screenY = MathUtils.fixedPointDivide(this.screenY, this.projectionData.y) * 120;
            return true;
        }
    }

    public final void calculateUpperBodyScreenSize() {
        this.upperBodyScreenWidth = MathUtils.fixedPointDivide(this.upperBodyTexture.width << 16, this.projectionData.y) * 120 - 131072 >> 18;
        this.upperBodyScreenHeight = MathUtils.fixedPointDivide(this.upperBodyTexture.height << 16, this.projectionData.y) * 120 - 131072 >> 18;
    }

    public final void calculateLowerBodyScreenSize() {
        this.lowerBodyScreenWidth = MathUtils.fixedPointDivide(this.lowerBodyTexture.width << 16, this.projectionData.y) * 120 + 65536 >> 17;
        this.lowerBodyScreenHeight = MathUtils.fixedPointDivide(this.lowerBodyTexture.height << 16, this.projectionData.y) * 120 + 65536 >> 17;
    }
}