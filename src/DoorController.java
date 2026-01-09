/**
 * Controls door animation and state transitions
 * Manages opening/closing doors by adjusting ceiling height
 */
public final class DoorController {
    public SectorData controlledSector; // The sector that contains the door
    public short targetCeilingHeight;   // Target ceiling height when door is fully open
    public short doorState;             // Current door state (0=closed, 1=opening, 2=closing, etc.)

    /**
     * Door states:
     * 0 - Closed/Inactive
     * 1 - Opening (moving ceiling upward)
     * 2 - Closing (moving ceiling downward)
     * 100+ - Special states (open with timer, locked, etc.)
     */
}