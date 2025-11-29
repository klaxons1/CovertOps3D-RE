/**
 * Represents a horizontal screen span belonging to a single sector.
 * Used by the portal renderer to draw floors and ceilings efficiently.
 * Instances are stored in per-row linked lists (240 columns × 288 rows).
 */
public final class RenderSpan {

    /** Leftmost column of the span (inclusive), 0..239 */
    public short startX;

    /** Rightmost column of the span (inclusive), 0..239 */
    public short endX;

    /** ID of the sector that owns this span */
    public short sectorId;

    /** Next span in the same screen row (forms a linked list) */
    public RenderSpan next;

    /**
     * Creates a new render span.
     *
     * @param startX   starting screen column
     * @param endX     ending screen column
     * @param sectorId sector identifier
     */
    public RenderSpan(short startX, short endX, short sectorId) {
        this.startX   = startX;
        this.endX     = endX;
        this.sectorId = sectorId;
        this.next     = null;
    }
}