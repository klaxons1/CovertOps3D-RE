/**
 * Controls elevator movement between different height levels
 * Manages vertical transportation between sectors
 */
public final class ElevatorController {
    public SectorData controlledSector; // The sector that functions as elevator
    public short minHeight;             // Minimum floor height (bottom position)
    public short maxHeight;             // Maximum floor height (top position)
    public short elevatorState;         // Current elevator state

    /**
     * Elevator states:
     * 0 - Stationary/Inactive
     * 1 - Moving upward (floor rising)
     * 2 - Moving downward (floor lowering)
     */
}