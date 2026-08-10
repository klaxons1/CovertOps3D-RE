/**
 * Collision detection and resolution subsystem extracted from GameWorld god-class.
 * Handles AABB checks, wall collision, line segment intersection, and sector passability.
 * All methods are allocation-free and use fixed-point math for performance.
 */
public final class CollisionSystem {

    static final int COLLISION_RADIUS = 655360; // 10 units fixed-point
    static final int OBJECT_COLLISION_RADIUS = 1310720; // 20 units
    static final int PICKUP_RADIUS = 1966080; // 30 units

    private static final int MIN_WALL_HEIGHT = 16;
    private static final int MIN_CEILING_CLEARANCE = 50;

    private CollisionSystem() {}

    static boolean isPointOnLeftSideOfLine(Point2D point, Point2D lineStart, Point2D lineEnd) {
        if (lineStart.x == lineEnd.x) {
            return lineStart.y - lineEnd.y > 0 ? point.x <= lineStart.x : point.x > lineStart.x;
        }
        if (lineStart.y == lineEnd.y) {
            return lineStart.x - lineEnd.x < 0 ? point.y <= lineStart.y : point.y > lineStart.y;
        }
        int deltaX = point.x - lineStart.x;
        int deltaY = point.y - lineStart.y;
        long cross = (long)(lineStart.y - lineEnd.y) * (long)deltaX;
        return (long)(lineStart.x - lineEnd.x) * (long)deltaY >= cross;
    }

    public static boolean doLineSegmentsIntersect(int x1, int y1, int x2, int y2,
                                                  int x3, int y3, int x4, int y4) {
        long denominator = (long)(y4 - y3) * (long)(x2 - x1) - (long)(x4 - x3) * (long)(y2 - y1);
        if (denominator == 0L) return false;

        long numeratorA = (long)(x4 - x3) * (long)(y1 - y3) - (long)(y4 - y3) * (long)(x1 - x3);
        long numeratorB = (long)(x2 - x1) * (long)(y1 - y3) - (long)(y2 - y1) * (long)(x1 - x3);

        if (denominator > 0L) {
            return numeratorA >= 0L && numeratorA <= denominator
                    && numeratorB >= 0L && numeratorB <= denominator;
        } else {
            return numeratorA <= 0L && numeratorA >= denominator
                    && numeratorB <= 0L && numeratorB >= denominator;
        }
    }

    public static boolean doesLineIntersectCircle(int lineX1, int lineY1, int lineX2, int lineY2,
                                                  int circleX, int circleY, int radius) {
        return doLineSegmentsIntersect(lineX1, lineY1, lineX2, lineY2,
                circleX - radius, circleY - radius, circleX + radius, circleY - radius)
                || doLineSegmentsIntersect(lineX1, lineY1, lineX2, lineY2,
                circleX + radius, circleY - radius, circleX + radius, circleY + radius)
                || doLineSegmentsIntersect(lineX1, lineY1, lineX2, lineY2,
                circleX + radius, circleY + radius, circleX - radius, circleY + radius)
                || doLineSegmentsIntersect(lineX1, lineY1, lineX2, lineY2,
                circleX - radius, circleY + radius, circleX - radius, circleY - radius);
    }

    static boolean isWallPassableForSector(SectorData currentSector, WallDefinition wall) {
        SectorData backSector = wall.backSurface.linkedSector;
        SectorData frontSector = wall.frontSurface.linkedSector;

        if (backSector != currentSector) {
            if (backSector.floorHeight - currentSector.floorHeight > MIN_WALL_HEIGHT) return true;
            short maxFloor = backSector.floorHeight;
            if (currentSector.floorHeight > maxFloor) maxFloor = currentSector.floorHeight;
            if (backSector.ceilingHeight - maxFloor < MIN_CEILING_CLEARANCE) return true;
        }
        if (frontSector != currentSector) {
            if (frontSector.floorHeight - currentSector.floorHeight > MIN_WALL_HEIGHT) return true;
            short maxFloor = frontSector.floorHeight;
            if (currentSector.floorHeight > maxFloor) maxFloor = currentSector.floorHeight;
            if (frontSector.ceilingHeight - maxFloor < MIN_CEILING_CLEARANCE) return true;
        }
        return false;
    }

    static boolean isWallSolid(WallDefinition wall) {
        SectorData backSector = wall.backSurface.linkedSector;
        SectorData frontSector = wall.frontSurface.linkedSector;

        if (backSector.ceilingHeight - backSector.floorHeight <= 0) return true;
        if (frontSector.ceilingHeight - frontSector.floorHeight <= 0) return true;

        return backSector.floorHeight >= frontSector.ceilingHeight
                || frontSector.floorHeight >= backSector.ceilingHeight;
    }

    static boolean isWallHeightBlocking(int projectileHeight, WallDefinition wall) {
        SectorData backSector = wall.backSurface.linkedSector;
        SectorData frontSector = wall.frontSurface.linkedSector;
        return backSector.ceilingHeight <= projectileHeight
                || backSector.floorHeight >= projectileHeight
                || frontSector.ceilingHeight <= projectileHeight
                || frontSector.floorHeight >= projectileHeight;
    }

    /**
     * Resolves collision with a wall using AABB.
     * Modifies collisionTestPoint in place, returns true if collision occurred.
     */
    static boolean resolveWallCollision(Point2D collisionPoint, WallDefinition wall, Point2D[] vertices) {
        Point2D startVertex = vertices[wall.startVertexId & 0xFFFF];
        Point2D endVertex = vertices[wall.endVertexId & 0xFFFF];

        int wallDeltaX = endVertex.x - startVertex.x;
        int wallDeltaY = endVertex.y - startVertex.y;
        int wallCenterX = startVertex.x + (wallDeltaX >> 1);
        int wallCenterY = startVertex.y + (wallDeltaY >> 1);
        int wallHalfExtentX = wallDeltaX >= 0 ? wallDeltaX >> 1 : -(wallDeltaX >> 1);
        int wallHalfExtentY = wallDeltaY >= 0 ? wallDeltaY >> 1 : -(wallDeltaY >> 1);

        int relativeToCenterX = collisionPoint.x - wallCenterX;
        int relativeToCenterY = collisionPoint.y - wallCenterY;
        int absRelativeX = relativeToCenterX >= 0 ? relativeToCenterX : -relativeToCenterX;
        int absRelativeY = relativeToCenterY >= 0 ? relativeToCenterY : -relativeToCenterY;

        int overlapX = wallHalfExtentX + COLLISION_RADIUS - absRelativeX;
        if (overlapX <= 0) return false;
        int overlapY = wallHalfExtentY + COLLISION_RADIUS - absRelativeY;
        if (overlapY <= 0) return false;

        int correctionX, correctionY;
        if (overlapX < overlapY) {
            correctionX = relativeToCenterX < 0 ? -overlapX : overlapX;
            correctionY = 0;
        } else {
            correctionX = 0;
            correctionY = relativeToCenterY < 0 ? -overlapY : overlapY;
        }

        return adjustCollisionPoint(collisionPoint, correctionX, correctionY, startVertex, endVertex,
                wall.normalVector, wallCenterX, wallCenterY, wallHalfExtentX, wallHalfExtentY);
    }

    private static boolean adjustCollisionPoint(Point2D collisionPoint, int correctionX, int correctionY,
                                                Point2D wallStart, Point2D wallEnd, Point2D wallNormal,
                                                int wallCenterX, int wallCenterY, int wallHalfExtentX, int wallHalfExtentY) {

        int normalX, normalY;
        if (isPointOnLeftSideOfLine(collisionPoint, wallStart, wallEnd)) {
            normalX = -wallNormal.x;
            normalY = -wallNormal.y;
        } else {
            normalX = wallNormal.x;
            normalY = wallNormal.y;
        }

        int penetration;
        if (wallHalfExtentX > wallHalfExtentY) {
            if (normalY >= 0) {
                penetration = wallCenterY + wallHalfExtentY - (collisionPoint.y - COLLISION_RADIUS);
            } else {
                penetration = -((wallCenterY - wallHalfExtentY) - (collisionPoint.y + COLLISION_RADIUS));
            }
        } else {
            if (normalX >= 0) {
                penetration = wallCenterX + wallHalfExtentX - (collisionPoint.x - COLLISION_RADIUS);
            } else {
                penetration = -((wallCenterX - wallHalfExtentX) - (collisionPoint.x + COLLISION_RADIUS));
            }
        }

        if (penetration <= 0) return false;

        int relativeX, relativeY;
        if (normalX >= 0) {
            relativeX = (collisionPoint.x - COLLISION_RADIUS) - (wallCenterX + wallHalfExtentX);
        } else {
            relativeX = (collisionPoint.x + COLLISION_RADIUS) - (wallCenterX - wallHalfExtentX);
        }
        if (normalY >= 0) {
            relativeY = (collisionPoint.y - COLLISION_RADIUS) - (wallCenterY - wallHalfExtentY);
        } else {
            relativeY = (collisionPoint.y + COLLISION_RADIUS) - (wallCenterY + wallHalfExtentY);
        }

        int dotProduct = (int)((long)relativeX * (long)normalX + (long)relativeY * (long)normalY >> 16);
        if (dotProduct >= 0) return false;

        int pushX = (int)((long)normalX * (long)(-dotProduct) >> 16);
        int pushY = (int)((long)normalY * (long)(-dotProduct) >> 16);
        int pushMagnitude = (pushX >= 0 ? pushX : -pushX) + (pushY >= 0 ? pushY : -pushY);
        int correctionMagnitude = (correctionX >= 0 ? correctionX : -correctionX) + (correctionY >= 0 ? correctionY : -correctionY);

        if (correctionMagnitude < pushMagnitude) {
            collisionPoint.x += correctionX;
            collisionPoint.y += correctionY;
        } else {
            collisionPoint.x += pushX;
            collisionPoint.y += pushY;
        }

        return true;
    }
}
