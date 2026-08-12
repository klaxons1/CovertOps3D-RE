import javax.microedition.lcdui.game.GameCanvas;

/**
 * Handles keycode translation and input state mapping.
 * Extracted from MainGameCanvas god-class to isolate input responsibility.
 * Performance: static, allocation-free, uses only int arithmetic and switch.
 */
public final class InputManager {

    private final GameCanvas canvas;
    private final int keyMappingOffset;

    public InputManager(GameCanvas canvas) {
        this.canvas = canvas;
        // Original logic for detecting Nokia vs other key layouts
        int keyCode8 = Math.abs(canvas.getKeyCode(8));
        this.keyMappingOffset = keyCode8 == 53 ? 5 : keyCode8;
    }

    /** Translates raw keyCode to internal action constant (1..12). */
    public int translateKeyCode(int keyCode) {
        int normalized = (keyCode < 0 ? -keyCode : keyCode) - keyMappingOffset;
        switch (normalized) {
            case 1: return 11; // soft left? mapped to run
            case 2: return 12; // soft right? mapped to back
            default:
                switch (canvas.getGameAction(keyCode)) {
                    case 1: return 1;  // UP -> forward
                    case 2: return 3;  // LEFT -> look up? original mapping
                    case 5: return 4;  // RIGHT -> look down
                    case 6: return 2;  // DOWN -> backward
                    case 8: return 5;  // FIRE -> fire/strafe
                    case 9: return 6;
                    case 10: return 7;
                    case 11: return 8;
                    case 12: return 9;
                    default: return 10;
                }
        }
    }

    /** Handles key pressed, updating GameEngine input flags. */
    public void handleKeyPressed(int keyCode) {
        int action = translateKeyCode(keyCode);
        switch (action) {
            case 1: GameEngine.inputForward = true; return;
            case 2: GameEngine.inputBackward = true; return;
            case 3: GameEngine.inputLookUp = true; return;
            case 4: GameEngine.inputLookDown = true; return;
            case 5:
                GameEngine.inputFire = true;
                GameEngine.inputStrafe = false;
                return;
            case 11: GameEngine.inputRun = true; return;
            case 12: GameEngine.inputBack = true; return;
            default:
                switch (keyCode) {
                    case 48: GameEngine.toggleMapInput = true; return;
                    case 49: GameEngine.useKey = true; return;
                    case 35: // # - Doom god mode toggle
                    case 42: // * - alternate keypad layout
                        GameEngine.toggleGodModeInput = true;
                        return;
                    case 51: GameEngine.selectNextWeapon = true; return;
                    case 53: // also FIRE
                        GameEngine.inputFire = true;
                        GameEngine.inputStrafe = false;
                        return;
                    case 55: GameEngine.inputLeft = true; return;
                    case 57: GameEngine.inputRight = true; return;
                    default: return;
                }
        }
    }

    /** Handles key released. */
    public void handleKeyReleased(int keyCode) {
        int action = translateKeyCode(keyCode);
        switch (action) {
            case 1: GameEngine.inputForward = false; return;
            case 2: GameEngine.inputBackward = false; return;
            case 3: GameEngine.inputLookUp = false; return;
            case 4: GameEngine.inputLookDown = false; return;
            case 5: GameEngine.inputStrafe = true; return;
            default:
                switch (keyCode) {
                    case 55: GameEngine.inputLeft = false; return;
                    case 57: GameEngine.inputRight = false; return;
                    default: break;
                }
        }
    }

    /** Clears all input flags (used when resetting level). */
    public static void clearAll() {
        GameEngine.inputForward = false;
        GameEngine.inputBackward = false;
        GameEngine.inputLeft = false;
        GameEngine.inputRight = false;
        GameEngine.inputLookUp = false;
        GameEngine.inputLookDown = false;
        GameEngine.inputFire = false;
        GameEngine.inputStrafe = false;
        GameEngine.inputRun = false;
        GameEngine.inputBack = false;
        GameEngine.useKey = false;
        GameEngine.toggleMapInput = false;
        GameEngine.toggleGodModeInput = false;
        GameEngine.selectNextWeapon = false;
    }
}
