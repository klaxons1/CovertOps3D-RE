/**
 * Render span management system for efficient surface rendering.
 * Handles span merging and optimization for floor/ceiling rendering.
 */
public final class RenderUtils {

    private RenderSpan[] renderSpans;
    private RenderSpan freeList = null;

    public RenderUtils() {
        this.renderSpans = new RenderSpan[PortalRenderer.SCREEN_HEIGHT];
        this.resetRenderer();
    }

    /**
     * Resets the renderer and rebuilds the free list.
     */
    public final void resetRenderer() {
        int screenHeight = PortalRenderer.SCREEN_HEIGHT;

        for (int scanline = 0; scanline < screenHeight; ++scanline) {
            if (this.renderSpans[scanline] != null) {
                RenderSpan currentSpan = this.renderSpans[scanline];

                // Add all spans from this scanline to free list
                while (true) {
                    RenderSpan span = currentSpan;
                    if (currentSpan.next == null) {
                        span.next = this.freeList;
                        this.freeList = this.renderSpans[scanline];
                        break;
                    }
                    currentSpan = span.next;
                }
            }

            this.renderSpans[scanline] = null;
        }
    }

    /**
     * Adds a render span, merging with adjacent spans when possible.
     *
     * @param startX   Starting X coordinate of the span
     * @param endX     Ending X coordinate of the span
     * @param sectorId ID of the sector this span belongs to
     * @param scanline The scanline where this span is rendered
     */
    public final void addRenderSpan(short startX, short endX, short sectorId, int scanline) {
        RenderSpan currentSpan = this.renderSpans[scanline];

        while (true) {
            RenderSpan span = currentSpan;
            RenderSpan newSpan;

            // If no span exists for this scanline, create new one
            if (currentSpan == null) {
                if (this.freeList != null) {
                    // Reuse span from free list
                    newSpan = this.freeList;
                    this.freeList = this.freeList.next;
                    newSpan.startX = startX;
                    newSpan.endX = endX;
                    newSpan.sectorId = sectorId;
                } else {
                    // Create new span if free list is empty
                    newSpan = new RenderSpan(startX, endX, sectorId);
                }

                newSpan.next = this.renderSpans[scanline];
                this.renderSpans[scanline] = newSpan;
                return;
            }

            // Check if we can merge with existing span of same sector
            if (span.sectorId == sectorId) {
                RenderSpan prevSpan;

                // Merge with right side: current span ends where new span starts
                if (span.endX == startX - 1) {
                    span.endX = endX;
                    RenderSpan searchSpan = this.renderSpans[scanline];

                    // Check if we can merge with next span to the right
                    for (prevSpan = null; searchSpan != null; searchSpan = searchSpan.next) {
                        if (searchSpan.sectorId == sectorId && searchSpan.startX == endX + 1) {
                            span.endX = searchSpan.endX;

                            // Remove the merged span from linked list
                            if (prevSpan != null) {
                                prevSpan.next = searchSpan.next;
                            } else {
                                this.renderSpans[scanline] = searchSpan.next;
                            }

                            // Add merged span to free list
                            searchSpan.next = this.freeList;
                            this.freeList = searchSpan;
                            return;
                        }
                        prevSpan = searchSpan;
                    }
                    return;
                }

                // Merge with left side: current span starts where new span ends
                if (span.startX == endX + 1) {
                    span.startX = startX;
                    RenderSpan searchSpan = this.renderSpans[scanline];

                    // Check if we can merge with previous span to the left
                    for (prevSpan = null; searchSpan != null; searchSpan = searchSpan.next) {
                        if (searchSpan.sectorId == sectorId && searchSpan.endX == startX - 1) {
                            span.startX = searchSpan.startX;

                            // Remove the merged span from linked list
                            if (prevSpan != null) {
                                prevSpan.next = searchSpan.next;
                            } else {
                                this.renderSpans[scanline] = searchSpan.next;
                            }

                            // Add merged span to free list
                            searchSpan.next = this.freeList;
                            this.freeList = searchSpan;
                            return;
                        }
                        prevSpan = searchSpan;
                    }
                    return;
                }
            }

            currentSpan = span.next;
        }
    }

    /**
     * Renders all accumulated spans for floor and ceiling surfaces.
     *
     * @param cameraX     Camera X position for texture mapping
     * @param cameraY     Camera Y position for texture mapping
     * @param cameraZ     Camera Z position for texture mapping
     * @param cameraAngle Camera angle for texture mapping
     */
    public final void renderAllSpans(int cameraX, int cameraY, int cameraZ, int cameraAngle) {
        RenderSpan currentSpan;
        RenderSpan span;
        SectorData sector;

        int halfScreenHeight = PortalRenderer.HALF_SCREEN_HEIGHT;
        int screenHeight = PortalRenderer.SCREEN_HEIGHT;

        // Render floor spans (scanlines 0 to halfScreenHeight-1)
        for (int scanline = 0; scanline < halfScreenHeight; ++scanline) {
            currentSpan = this.renderSpans[scanline];

            while (true) {
                span = currentSpan;
                if (currentSpan == null) {
                    break;
                }

                sector = LevelLoader.gameWorld.sectors[span.sectorId];
                PortalRenderer.drawFlatSurface(
                        span.startX, span.endX, scanline,
                        sector.floorTexture.pixelData,
                        sector.floorTexture.colorPalettes,
                        sector.lightLevel,
                        cameraX, cameraY, cameraZ,
                        sector.floorOffsetX,
                        cameraAngle
                );
                currentSpan = span.next;
            }
        }

        // Render ceiling spans (scanlines halfScreenHeight to screenHeight-1)
        for (int scanline = halfScreenHeight; scanline < screenHeight; ++scanline) {
            currentSpan = this.renderSpans[scanline];

            while (true) {
                span = currentSpan;
                if (currentSpan == null) {
                    break;
                }

                sector = LevelLoader.gameWorld.sectors[span.sectorId];
                PortalRenderer.drawFlatSurface(
                        span.startX, span.endX, scanline,
                        sector.ceilingTexture.pixelData,
                        sector.ceilingTexture.colorPalettes,
                        sector.lightLevel,
                        cameraX, cameraY, cameraZ,
                        sector.ceilingOffsetX,
                        cameraAngle
                );
                currentSpan = span.next;
            }
        }
    }
}